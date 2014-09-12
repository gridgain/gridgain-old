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

package org.gridgain.grid.util.ipc.shmem;

import org.gridgain.grid.*;
import org.gridgain.grid.kernal.processors.resource.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.thread.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.grid.util.ipc.*;
import org.gridgain.grid.util.lang.*;
import org.gridgain.grid.util.tostring.*;
import org.gridgain.grid.util.worker.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.atomic.*;

/**
 * Server shared memory IPC endpoint.
 */
public class GridIpcSharedMemoryServerEndpoint implements GridIpcServerEndpoint {
    /** Troubleshooting public wiki page. */
    public static final String TROUBLESHOOTING_URL = "http://bit.ly/GridGain-Troubleshooting";

    /** IPC error message. */
    public static final String OUT_OF_RESOURCES_MSG = "Failed to allocate shared memory segment " +
        "(for troubleshooting see " + TROUBLESHOOTING_URL + ')';

    /** Default endpoint port number. */
    public static final int DFLT_IPC_PORT = 10500;

    /** Default shared memory space in bytes. */
    public static final int DFLT_SPACE_SIZE = 256 * 1024;

    /**
     * Default token directory. Note that this path is relative to {@code GRIDGAIN_HOME/work} folder
     * if {@code GRIDGAIN_HOME} system or environment variable specified, otherwise it is relative to
     * {@code work} folder under system {@code java.io.tmpdir} folder.
     *
     * @see GridConfiguration#getWorkDirectory()
     */
    public static final String DFLT_TOKEN_DIR_PATH = "ipc/shmem";

    /**
     * Shared memory token file name prefix.
     *
     * Token files are created and stored in the following manner: [tokDirPath]/[nodeId]-[current
     * PID]/gg-shmem-space-[auto_idx]-[other_party_pid]-[size]
     */
    public static final String TOKEN_FILE_NAME = "gg-shmem-space-";

    /** Default lock file name. */
    private static final String LOCK_FILE_NAME = "lock.file";

    /** GC frequency. */
    private static final long GC_FREQ = 10000;

    /** ID generator. */
    private static final AtomicLong tokIdxGen = new AtomicLong();

    /** Port to bind socket to. */
    private int port = DFLT_IPC_PORT;

    /** Prefix. */
    private String tokDirPath = DFLT_TOKEN_DIR_PATH;

    /** Space size. */
    private int size = DFLT_SPACE_SIZE;

    /** Server socket. */
    @GridToStringExclude
    private ServerSocket srvSock;

    /** Token directory. */
    private File tokDir;

    /** Logger. */
    @GridLoggerResource
    private GridLogger log;

    /** Local node ID. */
    @GridLocalNodeIdResource
    private UUID locNodeId;

    /** Grid name. */
    @GridNameResource
    private String gridName;

    /** Flag allowing not to print out of resources warning. */
    private boolean omitOutOfResourcesWarn;

    /** GC worker. */
    private GridWorker gcWorker;

    /** Pid of the current process. */
    private int pid;

    /** Closed flag. */
    private volatile boolean closed;

    /** Spaces opened on with this endpoint. */
    private final Collection<GridIpcSharedMemoryClientEndpoint> endpoints =
        new GridConcurrentHashSet<>();

    /** Use this constructor when dependencies could be injected with {@link GridResourceProcessor#injectGeneric(Object)}. */
    public GridIpcSharedMemoryServerEndpoint() {
        // No-op.
    }

    /**
     * Constructor to set dependencies explicitly.
     *
     * @param log Log.
     * @param locNodeId Node id.
     * @param gridName Grid name.
     */
    public GridIpcSharedMemoryServerEndpoint(GridLogger log, UUID locNodeId, String gridName) {
        this.log = log;
        this.locNodeId = locNodeId;
        this.gridName = gridName;
    }

    /** @param omitOutOfResourcesWarn If {@code true}, out of resources warning will not be printed by server. */
    public void omitOutOfResourcesWarning(boolean omitOutOfResourcesWarn) {
        this.omitOutOfResourcesWarn = omitOutOfResourcesWarn;
    }

    /** {@inheritDoc} */
    @Override public void start() throws GridException {
        GridIpcSharedMemoryNativeLoader.load();

        pid = GridIpcSharedMemoryUtils.pid();

        if (pid == -1)
            throw new GridIpcEndpointBindException("Failed to get PID of the current process.");

        if (size <= 0)
            throw new GridIpcEndpointBindException("Space size should be positive: " + size);

        String tokDirPath = this.tokDirPath;

        if (F.isEmpty(tokDirPath))
            throw new GridIpcEndpointBindException("Token directory path is empty.");

        tokDirPath = tokDirPath + '/' + locNodeId.toString() + '-' + GridIpcSharedMemoryUtils.pid();

        tokDir = U.resolveWorkDirectory(tokDirPath, false);

        if (port <= 0 || port >= 0xffff)
            throw new GridIpcEndpointBindException("Port value is illegal: " + port);

        try {
            srvSock = new ServerSocket();

            // Always bind to loopback.
            srvSock.bind(new InetSocketAddress("127.0.0.1", port));
        }
        catch (IOException e) {
            // Although empty socket constructor never throws exception, close it just in case.
            U.closeQuiet(srvSock);

            throw new GridIpcEndpointBindException("Failed to bind shared memory IPC endpoint (is port already " +
                "in use?): " + port, e);
        }

        gcWorker = new GcWorker(gridName, "ipc-shmem-gc", log);

        new GridThread(gcWorker).start();

        if (log.isInfoEnabled())
            log.info("IPC shared memory server endpoint started [port=" + port +
                ", tokDir=" + tokDir.getAbsolutePath() + ']');
    }

    /** {@inheritDoc} */
    @SuppressWarnings("ErrorNotRethrown")
    @Override public GridIpcEndpoint accept() throws GridException {
        while (!Thread.currentThread().isInterrupted()) {
            Socket sock = null;

            boolean accepted = false;

            try {
                sock = srvSock.accept();

                accepted = true;

                InputStream inputStream = sock.getInputStream();
                ObjectInputStream in = new ObjectInputStream(inputStream);

                ObjectOutputStream out = new ObjectOutputStream(sock.getOutputStream());

                GridIpcSharedMemorySpace inSpace = null;

                GridIpcSharedMemorySpace outSpace = null;

                boolean err = true;

                try {
                    GridIpcSharedMemoryInitRequest req = (GridIpcSharedMemoryInitRequest)in.readObject();

                    if (log.isDebugEnabled())
                        log.debug("Processing request: " + req);

                    GridPair<String> p = inOutToken(req.pid(), size);

                    String file1 = p.get1();
                    String file2 = p.get2();

                    assert file1 != null;
                    assert file2 != null;

                    // Create tokens.
                    new File(file1).createNewFile();
                    new File(file2).createNewFile();

                    if (log.isDebugEnabled())
                        log.debug("Created token files: " + p);

                    inSpace = new GridIpcSharedMemorySpace(
                        file1,
                        req.pid(),
                        pid,
                        size,
                        true,
                        log);

                    outSpace = new GridIpcSharedMemorySpace(
                        file2,
                        pid,
                        req.pid(),
                        size,
                        false,
                        log);

                    GridIpcSharedMemoryClientEndpoint ret = new GridIpcSharedMemoryClientEndpoint(inSpace, outSpace,
                        log);

                    out.writeObject(new GridIpcSharedMemoryInitResponse(file2, outSpace.sharedMemoryId(),
                        file1, inSpace.sharedMemoryId(), pid, size));

                    err = !in.readBoolean();

                    endpoints.add(ret);

                    return ret;
                }
                catch (UnsatisfiedLinkError e) {
                    throw GridIpcSharedMemoryUtils.linkError(e);
                }
                catch (IOException e) {
                    if (log.isDebugEnabled())
                        log.debug("Failed to process incoming connection " +
                            "(was connection closed by another party):" + e.getMessage());
                }
                catch (ClassNotFoundException e) {
                    U.error(log, "Failed to process incoming connection.", e);
                }
                catch (ClassCastException e) {
                    String msg = "Failed to process incoming connection (most probably, shared memory " +
                        "rest endpoint has been configured by mistake).";

                    LT.warn(log, null, msg);

                    sendErrorResponse(out, e);
                }
                catch (GridIpcOutOfSystemResourcesException e) {
                    if (!omitOutOfResourcesWarn)
                        LT.warn(log, null, OUT_OF_RESOURCES_MSG);

                    sendErrorResponse(out, e);
                }
                catch (GridException e) {
                    LT.error(log, e, "Failed to process incoming shared memory connection.");

                    sendErrorResponse(out, e);
                }
                finally {
                    // Exception has been thrown, need to free system resources.
                    if (err) {
                        if (inSpace != null)
                            inSpace.forceClose();

                        // Safety.
                        if (outSpace != null)
                            outSpace.forceClose();
                    }
                }
            }
            catch (IOException e) {
                if (!Thread.currentThread().isInterrupted() && !accepted)
                    throw new GridException("Failed to accept incoming connection.", e);

                if (!closed)
                    LT.error(log, null, "Failed to process incoming shared memory connection: " + e.getMessage());
            }
            finally {
                U.closeQuiet(sock);
            }
        } // while

        throw new GridInterruptedException("Socket accept was interrupted.");
    }

    /**
     * @param out Output stream.
     * @param err Error cause.
     */
    private void sendErrorResponse(ObjectOutput out, Exception err) {
        try {
            out.writeObject(new GridIpcSharedMemoryInitResponse(err));
        }
        catch (IOException e) {
            U.error(log, "Failed to send error response to client.", e);
        }
    }

    /**
     * @param pid PID of the other party.
     * @param size Size of the space.
     * @return Token pair.
     */
    private GridPair<String> inOutToken(int pid, int size) {
        while (true) {
            long idx = tokIdxGen.get();

            if (tokIdxGen.compareAndSet(idx, idx + 2))
                return F.pair(new File(tokDir, TOKEN_FILE_NAME + idx + "-" + pid + "-" + size).getAbsolutePath(),
                    new File(tokDir, TOKEN_FILE_NAME + (idx + 1) + "-" + pid + "-" + size).getAbsolutePath());
        }
    }

    /** {@inheritDoc} */
    @Override public int getPort() {
        return port;
    }

    /** {@inheritDoc} */
    @Nullable @Override public String getHost() {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code false} as shared memory endpoints can not be used for management.
     */
    @Override public boolean isManagement() {
        return false;
    }

    /**
     * Sets port endpoint will be bound to.
     *
     * @param port Port number.
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Gets token directory path.
     *
     * @return Token directory path.
     */
    public String getTokenDirectoryPath() {
        return tokDirPath;
    }

    /**
     * Sets token directory path.
     *
     * @param tokDirPath Token directory path.
     */
    public void setTokenDirectoryPath(String tokDirPath) {
        this.tokDirPath = tokDirPath;
    }

    /**
     * Gets size of shared memory spaces that are created by the endpoint.
     *
     * @return Size of shared memory space.
     */
    public int getSize() {
        return size;
    }

    /**
     * Sets size of shared memory spaces that are created by the endpoint.
     *
     * @param size Size of shared memory space.
     */
    public void setSize(int size) {
        this.size = size;
    }

    /** {@inheritDoc} */
    @Override public void close() {
        closed = true;

        U.closeQuiet(srvSock);

        if (gcWorker != null) {
            U.cancel(gcWorker);

            // This method may be called from already interrupted thread.
            // Need to ensure cleaning on close.
            boolean interrupted = Thread.interrupted();

            try {
                U.join(gcWorker);
            }
            catch (GridInterruptedException e) {
                U.warn(log, "Interrupted when stopping GC worker.", e);
            }
            finally {
                if (interrupted)
                    Thread.currentThread().interrupt();
            }
        }
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridIpcSharedMemoryServerEndpoint.class, this);
    }

    /**
     * Sets configuration properties from the map.
     *
     * @param endpointCfg Map of properties.
     * @throws GridException If invalid property name or value.
     */
    public void setupConfiguration(Map<String, String> endpointCfg) throws GridException {
        for (Map.Entry<String,String> e : endpointCfg.entrySet()) {
            try {
                switch (e.getKey()) {
                    case "type":
                    case "host":
                    case "management":
                        //Ignore these properties
                        break;

                    case "port":
                        setPort(Integer.parseInt(e.getValue()));
                        break;

                    case "size":
                        setSize(Integer.parseInt(e.getValue()));
                        break;

                    case "tokenDirectoryPath":
                        setTokenDirectoryPath(e.getValue());
                        break;

                    default:
                        throw new GridException("Invalid property '" + e.getKey() + "' of " + getClass().getSimpleName());
                }
            }
            catch (Throwable t) {
                if (t instanceof GridException)
                    throw t;

                throw new GridException("Invalid value '" + e.getValue() + "' of the property '" + e.getKey() + "' in " +
                        getClass().getSimpleName(), t);
            }
        }
    }

    /**
     *
     */
    private class GcWorker extends GridWorker {
        /**
         * @param gridName Grid name.
         * @param name Name.
         * @param log Log.
         */
        protected GcWorker(@Nullable String gridName, String name, GridLogger log) {
            super(gridName, name, log);
        }

        /** {@inheritDoc} */
        @Override protected void body() throws InterruptedException, GridInterruptedException {
            if (log.isDebugEnabled())
                log.debug("GC worker started.");

            File workTokDir = tokDir.getParentFile();

            assert workTokDir != null;

            while (!isCancelled()) {
                U.sleep(GC_FREQ);

                if (log.isDebugEnabled())
                    log.debug("Starting GC iteration.");

                RandomAccessFile lockFile = null;

                FileLock lock = null;

                try {
                    lockFile = new RandomAccessFile(new File(workTokDir, LOCK_FILE_NAME), "rw");

                    lock = lockFile.getChannel().lock();

                    if (lock != null)
                        processTokenDirectory(workTokDir);
                    else if (log.isDebugEnabled())
                        log.debug("Token directory is being processed concurrently: " + workTokDir.getAbsolutePath());
                }
                catch (OverlappingFileLockException ignored) {
                    if (log.isDebugEnabled())
                        log.debug("Token directory is being processed concurrently: " + workTokDir.getAbsolutePath());
                }
                catch (IOException e) {
                    U.error(log, "Failed to process directory: " + workTokDir.getAbsolutePath(), e);
                }
                finally {
                    U.releaseQuiet(lock);
                    U.closeQuiet(lockFile);
                }

                // Process spaces created by this endpoint.
                if (log.isDebugEnabled())
                    log.debug("Processing local spaces.");

                for (GridIpcSharedMemoryClientEndpoint e : endpoints) {
                    if (log.isDebugEnabled())
                        log.debug("Processing endpoint: " + e);

                    if (!e.checkOtherPartyAlive()) {
                        endpoints.remove(e);

                        if (log.isDebugEnabled())
                            log.debug("Removed endpoint: " + e);
                    }
                }
            }
        }

        /** @param workTokDir Token directory (common for multiple nodes). */
        private void processTokenDirectory(File workTokDir) {
            for (File f : workTokDir.listFiles()) {
                if (!f.isDirectory()) {
                    if (!f.getName().equals(LOCK_FILE_NAME)) {
                        if (log.isDebugEnabled())
                            log.debug("Unexpected file: " + f.getName());
                    }

                    continue;
                }

                if (f.equals(tokDir)) {
                    if (log.isDebugEnabled())
                        log.debug("Skipping own token directory: " + tokDir.getName());

                    continue;
                }

                String name = f.getName();

                int pid;

                try {
                    pid = Integer.parseInt(name.substring(name.lastIndexOf('-') + 1));
                }
                catch (NumberFormatException ignored) {
                    if (log.isDebugEnabled())
                        log.debug("Failed to parse file name: " + name);

                    continue;
                }

                // Is process alive?
                if (GridIpcSharedMemoryUtils.alive(pid)) {
                    if (log.isDebugEnabled())
                        log.debug("Skipping alive node: " + pid);

                    continue;
                }

                if (log.isDebugEnabled())
                    log.debug("Possibly stale token folder: " + f);

                // Process each token under stale token folder.
                File[] shmemToks = f.listFiles();

                if (shmemToks == null)
                    // Although this is strange, but is reproducible sometimes on linux.
                    return;

                int rmvCnt = 0;

                try {
                    for (File f0 : shmemToks) {
                        if (log.isDebugEnabled())
                            log.debug("Processing token file: " + f0.getName());

                        if (f0.isDirectory()) {
                            if (log.isDebugEnabled())
                                log.debug("Unexpected directory: " + f0.getName());
                        }

                        // Token file format: gg-shmem-space-[auto_idx]-[other_party_pid]-[size]
                        String[] toks = f0.getName().split("-");

                        if (toks.length != 6) {
                            if (log.isDebugEnabled())
                                log.debug("Unrecognized token file: " + f0.getName());

                            continue;
                        }

                        int pid0;
                        int size;

                        try {
                            pid0 = Integer.parseInt(toks[4]);
                            size = Integer.parseInt(toks[5]);
                        }
                        catch (NumberFormatException ignored) {
                            if (log.isDebugEnabled())
                                log.debug("Failed to parse file name: " + name);

                            continue;
                        }

                        if (GridIpcSharedMemoryUtils.alive(pid0)) {
                            if (log.isDebugEnabled())
                                log.debug("Skipping alive process: " + pid0);

                            continue;
                        }

                        if (log.isDebugEnabled())
                            log.debug("Possibly stale token file: " + f0);

                        GridIpcSharedMemoryUtils.freeSystemResources(f0.getAbsolutePath(), size);

                        if (f0.delete()) {
                            if (log.isDebugEnabled())
                                log.debug("Deleted file: " + f0.getName());

                            rmvCnt++;
                        }
                        else if (!f0.exists()) {
                            if (log.isDebugEnabled())
                                log.debug("File has been concurrently deleted: " + f0.getName());

                            rmvCnt++;
                        }
                        else if (log.isDebugEnabled())
                            log.debug("Failed to delete file: " + f0.getName());
                    }
                }
                finally {
                    // Assuming that no new files can appear, since
                    if (rmvCnt == shmemToks.length) {
                        U.delete(f);

                        if (log.isDebugEnabled())
                            log.debug("Deleted empty token directory: " + f.getName());
                    }
                }
            }
        }
    }
}
