/* 
 Copyright (C) GridGain Systems. All Rights Reserved.
 
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0
 
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.ggfs.hadoop.v2;

import org.apache.commons.logging.*;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.fs.Options;
import org.apache.hadoop.fs.permission.*;
import org.apache.hadoop.security.AccessControlException;
import org.apache.hadoop.util.*;
import org.gridgain.grid.*;
import org.gridgain.grid.ggfs.*;
import org.gridgain.grid.kernal.ggfs.hadoop.*;
import org.gridgain.grid.kernal.processors.ggfs.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.grid.util.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import static org.gridgain.grid.ggfs.GridGgfs.*;
import static org.gridgain.grid.ggfs.GridGgfsConfiguration.*;
import static org.gridgain.grid.ggfs.GridGgfsMode.*;
import static org.gridgain.grid.ggfs.hadoop.GridGgfsHadoopParameters.*;

/**
 * {@code GGFS} Hadoop 2.x file system driver over file system API. To use
 * {@code GGFS} as Hadoop file system, you should configure this class
 * in Hadoop's {@code core-site.xml} as follows:
 * <pre name="code" class="xml">
 *  &lt;property&gt;
 *      &lt;name&gt;fs.default.name&lt;/name&gt;
 *      &lt;value&gt;ggfs://ipc&lt;/value&gt;
 *  &lt;/property&gt;
 *
 *  &lt;property&gt;
 *      &lt;name&gt;fs.ggfs.impl&lt;/name&gt;
 *      &lt;value&gt;org.gridgain.grid.ggfs.hadoop.GridGgfsHadoopFileSystem&lt;/value&gt;
 *  &lt;/property&gt;
 * </pre>
 * You should also add GridGain JAR and all libraries to Hadoop classpath. To
 * do this, add following lines to {@code conf/hadoop-env.sh} script in Hadoop
 * distribution:
 * <pre name="code" class="bash">
 * export GRIDGAIN_HOME=/path/to/GridGain/distribution
 * export HADOOP_CLASSPATH=$GRIDGAIN_HOME/gridgain*.jar
 *
 * for f in $GRIDGAIN_HOME/libs/*.jar; do
 *  export HADOOP_CLASSPATH=$HADOOP_CLASSPATH:$f;
 * done
 * </pre>
 * <h1 class="header">Data vs Clients Nodes</h1>
 * Hadoop needs to use its FileSystem remotely from client nodes as well as directly on
 * data nodes. Client nodes are responsible for basic file system operations as well as
 * accessing data nodes remotely. Usually, client nodes are started together
 * with {@code job-submitter} or {@code job-scheduler} processes, while data nodes are usually
 * started together with Hadoop {@code task-tracker} processes.
 * <p>
 * For sample client and data node configuration refer to {@code config/hadoop/default-config-client.xml}
 * and {@code config/hadoop/default-config.xml} configuration files in GridGain installation.
 */
public class GridGgfsHadoopFileSystem extends AbstractFileSystem implements Closeable {
    /** Endpoint type: shared memory. */
    private static final String IPC_SHMEM = "shmem";

    /** Endpoint type: loopback. */
    private static final String IPC_TCP = "tcp";

    /** GGFS scheme name. */
    private static final String GGFS_SCHEME = "ggfs";

    /** Logger. */
    private static final Log LOG = LogFactory.getLog(GridGgfsHadoopFileSystem.class);

    /** Busy lock. */
    private final GridBusyLock busyLock = new GridBusyLock();

    /** Ensures that close routine is invoked at most once. */
    private final AtomicBoolean closeGuard = new AtomicBoolean();

    /** Grid remote client. */
    private GridGgfsHadoop rmtClient;

    /** Working directory. */
    private GridGgfsPath workingDir = DFLT_WORKING_DIR;

    /** Authority. */
    private String uriAuthority;

    /** Client logger. */
    private GridGgfsHadoopLogger clientLog;

    /** Server block size. */
    private long grpBlockSize;

    /** Default replication factor. */
    private short dfltReplication;

    /** Secondary URI string. */
    private URI secondaryUri;

    /** Mode resolver. */
    private GridGgfsModeResolver modeRslvr;

    /** Secondary file system instance. */
    private AbstractFileSystem secondaryFs;

    /** Whether custom sequential reads before prefetch value is provided. */
    private boolean seqReadsBeforePrefetchOverride;

    /** Custom-provided sequential reads before prefetch. */
    private int seqReadsBeforePrefetch;

    /** Flag that controls whether file writes should be colocated on data node. */
    private boolean colocateFileWrites;

    /**
     * @param name URI for file system.
     * @param cfg Configuration.
     * @throws URISyntaxException if name has invalid syntax.
     * @throws IOException If initialization failed.
     */
    public GridGgfsHadoopFileSystem(URI name, Configuration cfg) throws URISyntaxException, IOException {
        super(name, "ggfs", true, DFLT_IPC_PORT);

        try {
            initialize(name, cfg);
        }
        catch (IOException e) {
            // Close client if exception occurred.
            if (rmtClient != null)
                rmtClient.close();

            throw e;
        }
    }

    /**
     * Public setter that can be used by direct users of FS or Visor.
     *
     * @param colocateFileWrites Whether all ongoing file writes should be colocated.
     */
    public void colocateFileWrites(boolean colocateFileWrites) {
        this.colocateFileWrites = colocateFileWrites;
    }

    /**
     * Enter busy state.
     *
     * @throws IOException If file system is stopped.
     */
    private void enterBusy() throws IOException {
        if (!busyLock.enterBusy())
            throw new IOException("File system is stopped.");
    }

    /**
     * Leave busy state.
     */
    private void leaveBusy() {
        busyLock.leaveBusy();
    }

    /**
     * @param name URI passed to constructor.
     * @param cfg Configuration passed to constructor.
     * @throws IOException If initialization failed.
     */
    private void initialize(URI name, Configuration cfg) throws IOException {
        enterBusy();

        try {
            if (rmtClient != null)
                throw new IOException("File system is already initialized: " + rmtClient);

            A.notNull(name, "name");
            A.notNull(cfg, "cfg");

            if (!GGFS_SCHEME.equals(name.getScheme()))
                throw new IOException("Illegal file system URI [expected=" + GGFS_SCHEME +
                    "://[name]/[optional_path], actual=" + name + ']');

            uriAuthority = name.getAuthority();

            // Resolve type and port from configuration.
            String type = parameter(cfg, PARAM_GGFS_ENDPOINT_TYPE, uriAuthority, U.isWindows() ?
                IPC_TCP : IPC_SHMEM);

            String host = IPC_SHMEM.equals(type) ? IPC_SHMEM : parameter(cfg, PARAM_GGFS_ENDPOINT_HOST, uriAuthority,
                "127.0.0.1");

            int port = parameter(cfg, PARAM_GGFS_ENDPOINT_PORT, uriAuthority, DFLT_IPC_PORT);

            String endpoint = host + ':' + port;

            rmtClient = new GridGgfsHadoop(LOG, endpoint);

            // Override sequential reads before prefetch if needed.
            seqReadsBeforePrefetch = parameter(cfg, PARAM_GGFS_SEQ_READS_BEFORE_PREFETCH, uriAuthority, 0);

            if (seqReadsBeforePrefetch > 0)
                seqReadsBeforePrefetchOverride = true;

            // In GG replication factor is controlled by data cache affinity.
            // We use replication factor to force the whole file to be stored on local node.
            dfltReplication = (short)cfg.getInt("dfs.replication", 3);

            // Get file colocation control flag.
            colocateFileWrites = parameter(cfg, PARAM_GGFS_COLOCATED_WRITES, uriAuthority, false);

            // Get log directory.
            String logDirCfg = parameter(cfg, PARAM_GGFS_LOG_DIR, uriAuthority, DFLT_GGFS_LOG_DIR);

            File logDirFile = U.resolveGridGainPath(logDirCfg);

            String logDir = logDirFile != null ? logDirFile.getAbsolutePath() : null;

            // Handshake.
            GridGgfsHandshakeResponse handshake;
            GridGgfsPaths paths;

            try {
                handshake = rmtClient.handshake(logDir).get();

                grpBlockSize = handshake.blockSize();

                paths = handshake.secondaryPaths();
            }
            catch (GridException e) {
                throw new IOException("Failed to perform handshake with GGFS.", e);
            }

            Boolean logEnabled = parameter(cfg, PARAM_GGFS_LOG_ENABLED, uriAuthority, false);

            if (handshake.sampling() != null ? handshake.sampling() : logEnabled) {
                // Initiate client logger.
                if (logDir == null)
                    throw new IOException("Failed to resolve log directory: " + logDirCfg);

                Integer batchSize = parameter(cfg, PARAM_GGFS_LOG_BATCH_SIZE, uriAuthority, DFLT_GGFS_LOG_BATCH_SIZE);

                clientLog = GridGgfsHadoopLogger.logger(endpoint, handshake.ggfsName(), logDir, batchSize);
            }
            else
                clientLog = GridGgfsHadoopLogger.disabledLogger();

            modeRslvr = new GridGgfsModeResolver(paths.defaultMode(), paths.pathModes());

            boolean initSecondary = paths.defaultMode() == PROXY;

            if (paths.pathModes() != null) {
                for (T2<GridGgfsPath, GridGgfsMode> pathMode : paths.pathModes()) {
                    GridGgfsMode mode = pathMode.getValue();

                    initSecondary |= mode == PROXY;
                }
            }

            if (initSecondary) {
                if (paths.secondaryConfigurationPath() == null)
                    throw new IOException("Failed to connect to the secondary file system because configuration " +
                        "path is not provided.");

                if (paths.secondaryUri() == null)
                    throw new IOException("Failed to connect to the secondary file system because URI is not " +
                        "provided.");

                String secondaryConfPath = paths.secondaryConfigurationPath();

                try {
                    secondaryUri = new URI(paths.secondaryUri());

                    URL secondaryCfgUrl = U.resolveGridGainUrl(secondaryConfPath);

                    if (secondaryCfgUrl == null)
                        throw new IOException("Failed to resolve secondary file system config URL: " +
                            secondaryConfPath);

                    Configuration conf = new Configuration();

                    conf.addResource(secondaryCfgUrl);

                    String prop = String.format("fs.%s.impl.disable.cache", secondaryUri.getScheme());

                    conf.setBoolean(prop, true);

                    secondaryFs = AbstractFileSystem.get(secondaryUri, conf);
                }
                catch (URISyntaxException ignore) {
                    throw new IOException("Failed to resolve secondary file system URI: " + paths.secondaryUri());
                }
                catch (IOException e) {
                    throw new IOException("Failed to connect to the secondary file system: " +
                        paths.secondaryUri(), e);
                }
            }
        }
        finally {
            leaveBusy();
        }
    }

    /**
     * Get string parameter.
     *
     * @param cfg Configuration.
     * @param name Parameter name.
     * @param authority Authority.
     * @param dflt Default value.
     * @return String value.
     */
    private String parameter(Configuration cfg, String name, String authority, String dflt) {
        return cfg.get(String.format(name, authority), dflt);
    }

    /**
     * Get integer parameter.
     *
     * @param cfg Configuration.
     * @param name Parameter name.
     * @param authority Authority.
     * @param dflt Default value.
     * @return Integer value.
     * @throws IOException In case of parse exception.
     */
    private int parameter(Configuration cfg, String name, String authority, int dflt) throws IOException {
        String name0 = String.format(name, authority);

        try {
            return cfg.getInt(name0, dflt);
        }
        catch (NumberFormatException ignore) {
            throw new IOException("Failed to parse parameter value to integer: " + name0);
        }
    }

    /**
     * Get boolean parameter.
     *
     * @param cfg Configuration.
     * @param name Parameter name.
     * @param authority Authority.
     * @param dflt Default value.
     * @return Boolean value.
     */
    private boolean parameter(Configuration cfg, String name, String authority, boolean dflt) {
        return cfg.getBoolean(String.format(name, authority), dflt);
    }

    /** {@inheritDoc} */
    @Override public void close() throws IOException {
        if (closeGuard.compareAndSet(false, true)) {
            busyLock.block();

            if (rmtClient == null)
                return;

            rmtClient.close();

            if (clientLog.isLogEnabled())
                clientLog.close();

            // Reset initialized resources.
            rmtClient = null;
        }
    }

    /** {@inheritDoc} */
    @Override public int getUriDefaultPort() {
        return DFLT_IPC_PORT;
    }

    /** {@inheritDoc} */
    @Override public FsServerDefaults getServerDefaults() throws IOException {
        return new FsServerDefaults(grpBlockSize, (int)grpBlockSize, (int)grpBlockSize, dfltReplication, 64 * 1024,
            false, 0, DataChecksum.Type.NULL);
    }

    /** {@inheritDoc} */
    @Override public boolean setReplication(Path f, short replication)
        throws AccessControlException, FileNotFoundException, UnresolvedLinkException, IOException {
        return mode(f) == PROXY && secondaryFs.setReplication(f, replication);
    }

    /** {@inheritDoc} */
    @Override public void setTimes(Path f, long mtime, long atime)
        throws AccessControlException, FileNotFoundException, UnresolvedLinkException, IOException {
        if (mode(f) == PROXY)
            secondaryFs.setTimes(f, mtime, atime);
        else {
            if (mtime == -1 && atime == -1)
                return;

            try {
                rmtClient.setTimes(convert(f), atime, mtime).get();
            }
            catch (GridException e) {
                throw cast(e);
            }
        }
    }

    /** {@inheritDoc} */
    @Override public FsStatus getFsStatus() throws IOException {
        try {
            GridGgfsStatus status = rmtClient.fsStatus().get();

            return new FsStatus(status.spaceTotal(), status.spaceUsed(), status.spaceTotal() - status.spaceUsed());
        }
        catch (GridException e) {
            throw cast(e);
        }
    }

    /** {@inheritDoc} */
    @Override public void setPermission(Path p, FsPermission perm) throws IOException {
        enterBusy();

        try {
            A.notNull(p, "p");

            if (mode(p) == PROXY)
                secondaryFs.setPermission(toSecondary(p), perm);
            else {
                try {
                    if (rmtClient.update(convert(p), permission(perm)).get() == null)
                        throw new IOException("Failed to set file permission (file not found?)" +
                            " [path=" + p + ", perm=" + perm + ']');
                }
                catch (GridException e) {
                    throw cast(e);
                }
            }
        }
        finally {
            leaveBusy();
        }
    }

    /** {@inheritDoc} */
    @Override public void setOwner(Path p, String usr, String grp) throws IOException {
        A.notNull(p, "p");
        A.notNull(usr, "username");
        A.notNull(grp, "grpName");

        enterBusy();

        try {
            if (mode(p) == PROXY)
                secondaryFs.setOwner(toSecondary(p), usr, grp);
            else if (rmtClient.update(convert(p), F.asMap(PROP_USER_NAME, usr, PROP_GROUP_NAME, grp)).get() == null)
                throw new IOException("Failed to set file permission (file not found?)" +
                    " [path=" + p + ", username=" + usr + ", grpName=" + grp + ']');
        }
        catch (GridException e) {
            throw cast(e);
        }
        finally {
            leaveBusy();
        }
    }

    /** {@inheritDoc} */
    @Override public FSDataInputStream open(Path f, int bufSize) throws IOException {
        A.notNull(f, "f");

        enterBusy();

        try {
            GridGgfsPath path = convert(f);
            GridGgfsMode mode = modeRslvr.resolveMode(path);

            if (mode == PROXY) {
                FSDataInputStream is = secondaryFs.open(toSecondary(f), bufSize);

                if (clientLog.isLogEnabled()) {
                    // At this point we do not know file size, so we perform additional request to remote FS to get it.
                    FileStatus status = secondaryFs.getFileStatus(toSecondary(f));

                    long size = status != null ? status.getLen() : -1;

                    long logId = GridGgfsHadoopLogger.nextId();

                    clientLog.logOpen(logId, path, PROXY, bufSize, size);

                    return new FSDataInputStream(new GridGgfsHadoopProxyInputStream(is, clientLog, logId));
                }
                else
                    return is;
            }
            else {
                GridGgfsInputStreamDescriptor desc = seqReadsBeforePrefetchOverride ?
                    rmtClient.open(path, seqReadsBeforePrefetch).get() : rmtClient.open(path).get();

                long logId = -1;

                if (clientLog.isLogEnabled()) {
                    logId = GridGgfsHadoopLogger.nextId();

                    clientLog.logOpen(logId, path, mode, bufSize, desc.length());
                }

                if (LOG.isDebugEnabled())
                    LOG.debug("Opening input stream [thread=" + Thread.currentThread().getName() + ", path=" + path +
                        ", bufSize=" + bufSize + ']');

                GridGgfsHadoopInputStream ggfsIn = new GridGgfsHadoopInputStream(rmtClient, desc.streamId(),
                    desc.length(), bufSize, LOG, clientLog, logId);

                if (LOG.isDebugEnabled())
                    LOG.debug("Opened input stream [path=" + path + ", streamId=" + desc.streamId() + ']');

                return new FSDataInputStream(ggfsIn);
            }
        }
        catch (GridException e) {
            throw cast(e);
        }
        finally {
            leaveBusy();
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("deprecation")
    @Override public FSDataOutputStream createInternal(
        Path f,
        EnumSet<CreateFlag> flag,
        FsPermission perm,
        int bufSize,
        short replication,
        long blockSize,
        Progressable progress,
        Options.ChecksumOpt checksumOpt,
        boolean createParent
    ) throws AccessControlException, FileAlreadyExistsException, ParentNotDirectoryException,
        UnsupportedFileSystemException, UnresolvedLinkException, IOException {
        A.notNull(f, "f");

        enterBusy();

        boolean overwrite = flag.contains(CreateFlag.OVERWRITE);
        boolean append = flag.contains(CreateFlag.APPEND);
        boolean create = flag.contains(CreateFlag.CREATE);

        OutputStream out = null;

        try {
            GridGgfsPath path = convert(f);
            GridGgfsMode mode = modeRslvr.resolveMode(path);

            if (LOG.isDebugEnabled())
                LOG.debug("Opening output stream in create [thread=" + Thread.currentThread().getName() + "path=" +
                    path + ", overwrite=" + overwrite + ", bufSize=" + bufSize + ']');

            if (mode == PROXY) {
                FSDataOutputStream os = secondaryFs.createInternal(toSecondary(f), flag, perm, bufSize,
                    replication, blockSize, progress, checksumOpt, createParent);

                if (clientLog.isLogEnabled()) {
                    long logId = GridGgfsHadoopLogger.nextId();

                    if (append)
                        clientLog.logAppend(logId, path, PROXY, bufSize); // Don't have stream ID.
                    else
                        clientLog.logCreate(logId, path, PROXY, overwrite, bufSize, replication, blockSize);

                    return new FSDataOutputStream(new GridGgfsHadoopProxyOutputStream(os, clientLog, logId));
                }
                else
                    return os;
            }
            else {
                Map<String, String> permMap = permission(perm);

                // Create stream and close it in the 'finally' section if any sequential operation failed.
                Long streamId;

                long logId = -1;

                if (append) {
                    streamId = rmtClient.append(path, create, permMap).get();

                    if (clientLog.isLogEnabled()) {
                        logId = GridGgfsHadoopLogger.nextId();

                        clientLog.logAppend(logId, path, mode, bufSize);
                    }

                    if (LOG.isDebugEnabled())
                        LOG.debug("Opened output stream in append [path=" + path + ", streamId=" + streamId + ']');
                }
                else {
                    streamId = rmtClient.create(path, overwrite, colocateFileWrites, replication, blockSize,
                        permMap).get();

                    if (clientLog.isLogEnabled()) {
                        logId = GridGgfsHadoopLogger.nextId();

                        clientLog.logCreate(logId, path, mode, overwrite, bufSize, replication, blockSize);
                    }

                    if (LOG.isDebugEnabled())
                        LOG.debug("Opened output stream in create [path=" + path + ", streamId=" + streamId + ']');
                }

                assert streamId != null;

                GridGgfsHadoopOutputStream ggfsOut = new GridGgfsHadoopOutputStream(rmtClient, streamId, LOG,
                    clientLog, logId);

                bufSize = Math.max(64 * 1024, bufSize);

                out = new BufferedOutputStream(ggfsOut, bufSize);

                FSDataOutputStream res = new FSDataOutputStream(out, null, 0);

                // Mark stream created successfully.
                out = null;

                return res;
            }
        }
        catch (GridException e) {
            throw cast(e);
        }
        finally {
            // Close if failed during stream creation.
            if (out != null)
                U.closeQuiet(out);

            leaveBusy();
        }
    }

    /** {@inheritDoc} */
    @Override public boolean supportsSymlinks() {
        return false;
    }

    /** {@inheritDoc} */
    @Override public void renameInternal(Path src, Path dst) throws IOException {
        A.notNull(src, "src");
        A.notNull(dst, "dst");

        enterBusy();

        try {
            GridGgfsPath srcPath = convert(src);
            GridGgfsPath dstPath = convert(dst);
            Set<GridGgfsMode> childrenModes = modeRslvr.resolveChildrenModes(srcPath);

            if (childrenModes.contains(PROXY)) {
                if (clientLog.isLogEnabled())
                    clientLog.logRename(srcPath, PROXY, dstPath);

                secondaryFs.renameInternal(toSecondary(src), toSecondary(dst));
            }

            rmtClient.rename(srcPath, dstPath).get();

            if (clientLog.isLogEnabled())
                clientLog.logRename(srcPath, modeRslvr.resolveMode(srcPath), dstPath);
        }
        catch (GridException e) {
            throw cast(e);
        }
        finally {
            leaveBusy();
        }
    }

    /** {@inheritDoc} */
    @Override public boolean delete(Path f, boolean recursive) throws IOException {
        A.notNull(f, "f");

        enterBusy();

        try {
            GridGgfsPath path = convert(f);
            GridGgfsMode mode = modeRslvr.resolveMode(path);
            Set<GridGgfsMode> childrenModes = modeRslvr.resolveChildrenModes(path);

            if (childrenModes.contains(PROXY)) {
                if (clientLog.isLogEnabled())
                    clientLog.logDelete(path, PROXY, recursive);

                return secondaryFs.delete(toSecondary(f), recursive);
            }

            boolean res = rmtClient.delete(path, recursive).get();

            if (clientLog.isLogEnabled())
                clientLog.logDelete(path, mode, recursive);

            return res;
        }
        catch (GridException e) {
            throw cast(e);
        }
        finally {
            leaveBusy();
        }
    }

    /** {@inheritDoc} */
    @Override public void setVerifyChecksum(boolean verifyChecksum) throws AccessControlException, IOException {
        // Checksum has effect for secondary FS only.
        if (secondaryFs != null)
            secondaryFs.setVerifyChecksum(verifyChecksum);
    }

    /** {@inheritDoc} */
    @Override public FileChecksum getFileChecksum(Path f) throws IOException {
        if (mode(f) == PROXY)
            return secondaryFs.getFileChecksum(f);

        return null;
    }

    /** {@inheritDoc} */
    @Override public FileStatus[] listStatus(Path f) throws IOException {
        A.notNull(f, "f");

        enterBusy();

        try {
            GridGgfsPath path = convert(f);
            GridGgfsMode mode = modeRslvr.resolveMode(path);

            if (mode == PROXY) {
                FileStatus[] arr = secondaryFs.listStatus(toSecondary(f));

                if (arr != null) {
                    for (int i = 0; i < arr.length; i++)
                        arr[i] = toPrimary(arr[i]);
                }

                if (clientLog.isLogEnabled()) {
                    String[] fileArr = null;

                    if (arr != null) {
                        fileArr = new String[arr.length];

                        for (int i = 0; i < arr.length; i++)
                            fileArr[i] = arr[i].getPath().toString();
                    }

                    clientLog.logListDirectory(path, PROXY, fileArr);
                }

                return arr;
            }
            else {
                List<GridGgfsFile> files = new ArrayList<>(rmtClient.listFiles(path).get());

                FileStatus[] arr = new FileStatus[files.size()];

                for (int i = 0; i < arr.length; i++)
                    arr[i] = convert(files.get(i));

                if (clientLog.isLogEnabled()) {
                    String[] fileArr = new String[arr.length];

                    if (arr != null) {
                        for (int i = 0; i < arr.length; i++)
                            fileArr[i] = arr[i].getPath().toString();
                    }

                    clientLog.logListDirectory(path, mode, fileArr);
                }

                return arr;
            }
        }
        catch (GridException e) {
            throw cast(e);
        }
        finally {
            leaveBusy();
        }
    }

    /** {@inheritDoc} */
    @Override public void mkdir(Path f, FsPermission perm, boolean createParent) throws IOException {
        A.notNull(f, "f");

        enterBusy();

        try {
            GridGgfsPath path = convert(f);
            GridGgfsMode mode = modeRslvr.resolveMode(path);

            if (mode == PROXY) {
                if (clientLog.isLogEnabled())
                    clientLog.logMakeDirectory(path, PROXY);

                secondaryFs.mkdir(toSecondary(f), perm, createParent);
            }
            else {
                rmtClient.mkdirs(path, permission(perm)).get();

                if (clientLog.isLogEnabled())
                    clientLog.logMakeDirectory(path, mode);
            }
        }
        catch (GridException e) {
            throw cast(e);
        }
        finally {
            leaveBusy();
        }
    }

    /** {@inheritDoc} */
    @Override public FileStatus getFileStatus(Path f) throws IOException {
        A.notNull(f, "f");

        enterBusy();

        try {
            if (mode(f) == PROXY)
                return toPrimary(secondaryFs.getFileStatus(toSecondary(f)));
            else {
                GridGgfsFile info = rmtClient.info(convert(f)).get();

                if (info == null)
                    throw new FileNotFoundException("File not found: " + f);

                return convert(info);
            }
        }
        catch (GridException e) {
            throw cast(e);
        }
        finally {
            leaveBusy();
        }
    }

    /** {@inheritDoc} */
    @Override public BlockLocation[] getFileBlockLocations(Path path, long start, long len) throws IOException {
        A.notNull(path, "path");

        GridGgfsPath ggfsPath = convert(path);

        enterBusy();

        try {
            if (modeRslvr.resolveMode(ggfsPath) == PROXY)
                return secondaryFs.getFileBlockLocations(path, start, len);
            else {
                long now = System.currentTimeMillis();

                List<GridGgfsBlockLocation> affinity = new ArrayList<>(
                    rmtClient.affinity(ggfsPath, start, len).get());

                BlockLocation[] arr = new BlockLocation[affinity.size()];

                for (int i = 0; i < arr.length; i++)
                    arr[i] = convert(affinity.get(i));

                if (LOG.isDebugEnabled())
                    LOG.debug("Fetched file locations [path=" + path + ", fetchTime=" +
                        (System.currentTimeMillis() - now) + ", locations=" + Arrays.asList(arr) + ']');

                return arr;
            }
        }
        catch (GridException e) {
            throw cast(e);
        }
        finally {
            leaveBusy();
        }
    }

    /**
     * Resolve path mode.
     *
     * @param path HDFS path.
     * @return Path mode.
     */
    public GridGgfsMode mode(Path path) {
        return modeRslvr.resolveMode(convert(path));
    }

    /**
     * Convert the given path to path acceptable by the primary file system.
     *
     * @param path Path.
     * @return Primary file system path.
     */
    private Path toPrimary(Path path) {
        return convertPath(path, getUri());
    }

    /**
     * Convert the given path to path acceptable by the secondary file system.
     *
     * @param path Path.
     * @return Secondary file system path.
     */
    private Path toSecondary(Path path) {
        assert secondaryFs != null;
        assert secondaryUri != null;

        return convertPath(path, secondaryUri);
    }

    /**
     * Convert path using the given new URI.
     *
     * @param path Old path.
     * @param newUri New URI.
     * @return New path.
     */
    private Path convertPath(Path path, URI newUri) {
        assert newUri != null;

        if (path != null) {
            URI pathUri = path.toUri();

            try {
                return new Path(new URI(pathUri.getScheme() != null ? newUri.getScheme() : null,
                    pathUri.getAuthority() != null ? newUri.getAuthority() : null, pathUri.getPath(), null, null));
            }
            catch (URISyntaxException e) {
                throw new GridRuntimeException("Failed to construct secondary file system path from the primary file " +
                    "system path: " + path, e);
            }
        }
        else
            return null;
    }

    /**
     * Convert a file status obtained from the secondary file system to a status of the primary file system.
     *
     * @param status Secondary file system status.
     * @return Primary file system status.
     */
    private FileStatus toPrimary(FileStatus status) {
        return status != null ? new FileStatus(status.getLen(), status.isDirectory(), status.getReplication(),
            status.getBlockSize(), status.getModificationTime(), status.getAccessTime(), status.getPermission(),
            status.getOwner(), status.getGroup(), toPrimary(status.getPath())) : null;
    }

    /**
     * Convert GGFS path into Hadoop path.
     *
     * @param path GGFS path.
     * @return Hadoop path.
     */
    private Path convert(GridGgfsPath path) {
        return new Path(GGFS_SCHEME, uriAuthority, path.toString());
    }

    /**
     * Convert Hadoop path into GGFS path.
     *
     * @param path Hadoop path.
     * @return GGFS path.
     */
    @Nullable private GridGgfsPath convert(Path path) {
        if (path == null)
            return null;

        return path.isAbsolute() ? new GridGgfsPath(path.toUri().getPath()) :
            new GridGgfsPath(workingDir, path.toUri().getPath());
    }

    /**
     * Convert GGFS affinity block location into Hadoop affinity block location.
     *
     * @param block GGFS affinity block location.
     * @return Hadoop affinity block location.
     */
    private BlockLocation convert(GridGgfsBlockLocation block) {
        Collection<String> names = block.names();
        Collection<String> hosts = block.hosts();

        return new BlockLocation(
            names.toArray(new String[names.size()]) /* hostname:portNumber of data nodes */,
            hosts.toArray(new String[hosts.size()]) /* hostnames of data nodes */,
            block.start(), block.length()
        ) {
            @Override public String toString() {
                try {
                    return "BlockLocation [offset=" + getOffset() + ", length=" + getLength() +
                        ", hosts=" + Arrays.asList(getHosts()) + ", names=" + Arrays.asList(getNames()) + ']';
                }
                catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    /**
     * Convert GGFS file information into Hadoop file status.
     *
     * @param file GGFS file information.
     * @return Hadoop file status.
     */
    private FileStatus convert(GridGgfsFile file) {
        return new FileStatus(
            file.length(),
            file.isDirectory(),
            dfltReplication,
            file.groupBlockSize(),
            file.modificationTime(),
            file.accessTime(),
            permission(file),
            file.property(PROP_USER_NAME, DFLT_USER_NAME),
            file.property(PROP_GROUP_NAME, "users"),
            convert(file.path())) {
            @Override public String toString() {
                return "FileStatus [path=" + getPath() + ", isDir=" + isDirectory() + ", len=" + getLen() + "]";
            }
        };
    }

    /**
     * Converts {@code GridException} into corresponding {@code IOException} expected to be thrown
     * from public methods of {@code AbstractFileSystem}.
     *
     * @param e GridException to convert.
     * @return IOException.
     */
    @SuppressWarnings("IfMayBeConditional")
    private IOException cast(GridException e) {
        assert e != null;

        if (e instanceof GridGgfsFileNotFoundException)
            return new FileNotFoundException(e.getMessage());

        else if (e instanceof GridGgfsDirectoryNotEmptyException)
            return new PathIsNotEmptyDirectoryException(e.getMessage());

        else if (e instanceof GridGgfsParentNotDirectoryException)
            return new ParentNotDirectoryException(e.getMessage());

        else if (e instanceof GridGgfsPathAlreadyExistsException)
            return new PathExistsException(e.getMessage());

        else
            return new IOException(e);
    }

    /**
     * Convert Hadoop permission into GGFS file attribute.
     *
     * @param perm Hadoop permission.
     * @return GGFS attributes.
     */
    private Map<String, String> permission(FsPermission perm) {
        if (perm == null)
            perm = FsPermission.getDefault();

        return F.asMap(PROP_PERMISSION, String.format("%04o", perm.toShort()));
    }

    /**
     * Convert GGFS file attributes into Hadoop permission.
     *
     * @param file File info.
     * @return Hadoop permission.
     */
    private FsPermission permission(GridGgfsFile file) {
        String perm = file.property(PROP_PERMISSION, null);

        if (perm == null)
            return FsPermission.getDefault();

        try {
            return new FsPermission((short)Integer.parseInt(perm, 8));
        }
        catch (NumberFormatException ignore) {
            return FsPermission.getDefault();
        }
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridGgfsHadoopFileSystem.class, this);
    }
}
