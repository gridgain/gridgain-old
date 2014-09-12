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

package org.gridgain.grid.kernal.ggfs.hadoop;

import org.apache.hadoop.conf.*;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.fs.permission.*;
import org.apache.hadoop.ipc.*;
import org.gridgain.grid.*;
import org.gridgain.grid.ggfs.*;

import java.io.*;
import java.net.*;
import java.util.*;

import org.gridgain.grid.kernal.processors.ggfs.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.jetbrains.annotations.*;

/**
 * Adapter to use any Hadoop file system {@link org.apache.hadoop.fs.FileSystem} as {@link GridGgfsFileSystem}.
 */
public class GridGgfsHadoopFileSystemWrapper implements GridGgfsFileSystem, AutoCloseable {
    /** Property name for path to Hadoop configuration. */
    public static final String SECONDARY_FS_CONFIG_PATH = "SECONDARY_FS_CONFIG_PATH";

    /** Property name for URI of file system. */
    public static final String SECONDARY_FS_URI = "SECONDARY_FS_URI";

    /** Hadoop file system. */
    private final FileSystem fileSys;

    /** Properties of file system */
    private final Map<String, String> props = new HashMap<>();

    /**
     * Constructor.
     *
     * @param uri URI of file system.
     * @param cfgPath Additional path to Hadoop configuration.
     * @throws GridException In case of error.
     */
    public GridGgfsHadoopFileSystemWrapper(@Nullable String uri, @Nullable String cfgPath) throws GridException {
        Configuration cfg = new Configuration();

        if (cfgPath != null)
            cfg.addResource(U.resolveGridGainUrl(cfgPath));

        try {
            fileSys = uri == null ? FileSystem.get(cfg) : FileSystem.get(new URI(uri), cfg);
        }
        catch (IOException | URISyntaxException e) {
            throw new GridException(e);
        }

        uri = fileSys.getUri().toString();

        if (!uri.endsWith("/"))
            uri += "/";

        props.put(SECONDARY_FS_CONFIG_PATH, cfgPath);
        props.put(SECONDARY_FS_URI, uri);
    }

    /**
     * Convert GGFS path into Hadoop path.
     *
     * @param path GGFS path.
     * @return Hadoop path.
     */
    private Path convert(GridGgfsPath path) {
        URI uri = fileSys.getUri();

        return new Path(uri.getScheme(), uri.getAuthority(), path.toString());
    }

    /**
     * Heuristically checks if exception was caused by invalid HDFS version and returns appropriate exception.
     *
     * @param e Exception to check.
     * @param detailMsg Detailed error message.
     * @return Appropriate exception.
     */
    private GridGgfsException handleSecondaryFsError(IOException e, String detailMsg) {
        boolean wrongVer = X.hasCause(e, RemoteException.class) ||
            (e.getMessage() != null && e.getMessage().contains("Failed on local"));

        GridGgfsException ggfsErr = !wrongVer ? cast(detailMsg, e) :
            new GridGgfsInvalidHdfsVersionException("HDFS version you are connecting to differs from local " +
                "version.", e);



        return ggfsErr;
    }

    /**
     * Cast IO exception to GGFS exception.
     *
     * @param e IO exception.
     * @return GGFS exception.
     */
    public static GridGgfsException cast(String msg, IOException e) {
        if (e instanceof FileNotFoundException)
            return new GridGgfsFileNotFoundException(e);
        else if (e instanceof ParentNotDirectoryException)
            return new GridGgfsParentNotDirectoryException(msg, e);
        else if (e instanceof PathIsNotEmptyDirectoryException)
            return new GridGgfsDirectoryNotEmptyException(e);
        else if (e instanceof PathExistsException)
            return new GridGgfsPathAlreadyExistsException(msg, e);
        else
            return new GridGgfsException(msg, e);
    }

    /**
     * Convert Hadoop FileStatus properties to map.
     *
     * @param status File status.
     * @return GGFS attributes.
     */
    private static Map<String, String> properties(FileStatus status) {
        FsPermission perm = status.getPermission();

        if (perm == null)
            perm = FsPermission.getDefault();

        return F.asMap(PROP_PERMISSION, String.format("%04o", perm.toShort()), PROP_USER_NAME, status.getOwner(),
            PROP_GROUP_NAME, status.getGroup());
    }

    /** {@inheritDoc} */
    @Override public boolean exists(GridGgfsPath path) throws GridException {
        try {
            return fileSys.exists(convert(path));
        }
        catch (IOException e) {
            throw handleSecondaryFsError(e, "Failed to check file existence [path=" + path + "]");
        }
    }

    /** {@inheritDoc} */
    @Nullable @Override public GridGgfsFile update(GridGgfsPath path, Map<String, String> props) throws GridException {
        GridGgfsHadoopFSProperties props0 = new GridGgfsHadoopFSProperties(props);

        try {
            if (props0.userName() != null || props0.groupName() != null)
                fileSys.setOwner(convert(path), props0.userName(), props0.groupName());

            if (props0.permission() != null)
                fileSys.setPermission(convert(path), props0.permission());
        }
        catch (IOException e) {
            throw handleSecondaryFsError(e, "Failed to update file properties [path=" + path + "]");
        }

        //Result is not used in case of secondary FS.
        return null;
    }

    /** {@inheritDoc} */
    @Override public void rename(GridGgfsPath src, GridGgfsPath dest) throws GridException {
        // Delegate to the secondary file system.
        try {
            if (!fileSys.rename(convert(src), convert(dest)))
                throw new GridGgfsException("Failed to rename (secondary file system returned false) " +
                    "[src=" + src + ", dest=" + dest + ']');
        }
        catch (IOException e) {
            throw handleSecondaryFsError(e, "Failed to rename file [src=" + src + ", dest=" + dest + ']');
        }
    }

    /** {@inheritDoc} */
    @Override public boolean delete(GridGgfsPath path, boolean recursive) throws GridException {
        try {
            return fileSys.delete(convert(path), recursive);
        }
        catch (IOException e) {
            throw handleSecondaryFsError(e, "Failed to delete file [path=" + path + ", recursive=" + recursive + "]");
        }
    }

    /** {@inheritDoc} */
    @Override public void mkdirs(GridGgfsPath path) throws GridException {
        try {
            if (!fileSys.mkdirs(convert(path)))
                throw new GridException("Failed to make directories [path=" + path + "]");
        }
        catch (IOException e) {
            throw handleSecondaryFsError(e, "Failed to make directories [path=" + path + "]");
        }
    }

    /** {@inheritDoc} */
    @Override public void mkdirs(GridGgfsPath path, @Nullable Map<String, String> props) throws GridException {
        try {
            if (!fileSys.mkdirs(convert(path), new GridGgfsHadoopFSProperties(props).permission()))
                throw new GridException("Failed to make directories [path=" + path + ", props=" + props + "]");
        }
        catch (IOException e) {
            throw handleSecondaryFsError(e, "Failed to make directories [path=" + path + ", props=" + props + "]");
        }
    }

    /** {@inheritDoc} */
    @Override public Collection<GridGgfsPath> listPaths(GridGgfsPath path) throws GridException {
        try {
            FileStatus[] statuses = fileSys.listStatus(convert(path));

            if (statuses == null)
                throw new GridGgfsFileNotFoundException("Failed to list files (path not found): " + path);

            Collection<GridGgfsPath> res = new ArrayList<>(statuses.length);

            for (FileStatus status : statuses)
                res.add(new GridGgfsPath(path, status.getPath().getName()));

            return res;
        }
        catch (FileNotFoundException ignored) {
            throw new GridGgfsFileNotFoundException("Failed to list files (path not found): " + path);
        }
        catch (IOException e) {
            throw handleSecondaryFsError(e, "Failed to list statuses due to secondary file system exception: " + path);
        }
    }

    /** {@inheritDoc} */
    @Override public Collection<GridGgfsFile> listFiles(GridGgfsPath path) throws GridException {
        try {
            FileStatus[] statuses = fileSys.listStatus(convert(path));

            if (statuses == null)
                throw new GridGgfsFileNotFoundException("Failed to list files (path not found): " + path);

            Collection<GridGgfsFile> res = new ArrayList<>(statuses.length);

            for (FileStatus status : statuses) {
                GridGgfsFileInfo fsInfo = status.isDirectory() ? new GridGgfsFileInfo(true, properties(status)) :
                    new GridGgfsFileInfo((int)status.getBlockSize(), status.getLen(), null, null, false,
                    properties(status));

                res.add(new GridGgfsFileImpl(new GridGgfsPath(path, status.getPath().getName()), fsInfo, 1));
            }

            return res;
        }
        catch (FileNotFoundException ignored) {
            throw new GridGgfsFileNotFoundException("Failed to list files (path not found): " + path);
        }
        catch (IOException e) {
            throw handleSecondaryFsError(e, "Failed to list statuses due to secondary file system exception: " + path);
        }
    }

    /** {@inheritDoc} */
    @Override public GridGgfsReader open(GridGgfsPath path, int bufSize) {
        return new GridGgfsHadoopReader(fileSys, convert(path), bufSize);
    }

    /** {@inheritDoc} */
    @Override public OutputStream create(GridGgfsPath path, boolean overwrite) throws GridException {
        try {
            return fileSys.create(convert(path), overwrite);
        }
        catch (IOException e) {
            throw handleSecondaryFsError(e, "Failed to create file [path=" + path + ", overwrite=" + overwrite + "]");
        }
    }

    /** {@inheritDoc} */
    @Override public OutputStream create(GridGgfsPath path, int bufSize, boolean overwrite, int replication,
        long blockSize, @Nullable Map<String, String> props) throws GridException {
        GridGgfsHadoopFSProperties props0 =
            new GridGgfsHadoopFSProperties(props != null ? props : Collections.<String, String>emptyMap());

        try {
            return fileSys.create(convert(path), props0.permission(), overwrite, bufSize, (short)replication, blockSize,
                null);
        }
        catch (IOException e) {
            throw handleSecondaryFsError(e, "Failed to create file [path=" + path + ", props=" + props +
                ", overwrite=" + overwrite + ", bufSize=" + bufSize + ", replication=" + replication +
                ", blockSize=" + blockSize + "]");
        }
    }

    /** {@inheritDoc} */
    @Override public OutputStream append(GridGgfsPath path, int bufSize, boolean create,
        @Nullable Map<String, String> props) throws GridException {
        try {
            return fileSys.append(convert(path), bufSize);
        }
        catch (IOException e) {
            throw handleSecondaryFsError(e, "Failed to append file [path=" + path + ", bufSize=" + bufSize + "]");
        }
    }

    /** {@inheritDoc} */
    @Override public GridGgfsFile info(final GridGgfsPath path) throws GridException {
        try {
            final FileStatus status = fileSys.getFileStatus(convert(path));

            if (status == null)
                return null;

            final Map<String, String> props = properties(status);

            return new GridGgfsFile() {
                @Override public GridGgfsPath path() {
                    return path;
                }

                @Override public boolean isFile() {
                    return status.isFile();
                }

                @Override public boolean isDirectory() {
                    return status.isDirectory();
                }

                @Override public int blockSize() {
                    return (int)status.getBlockSize();
                }

                @Override public long groupBlockSize() {
                    return status.getBlockSize();
                }

                @Override public long accessTime() {
                    return status.getAccessTime();
                }

                @Override public long modificationTime() {
                    return status.getModificationTime();
                }

                @Override public String property(String name) throws IllegalArgumentException {
                    String val = props.get(name);

                    if (val ==  null)
                        throw new IllegalArgumentException("File property not found [path=" + path + ", name=" + name + ']');

                    return val;
                }

                @Nullable @Override public String property(String name, @Nullable String dfltVal) {
                    String val = props.get(name);

                    return val == null ? dfltVal : val;
                }

                @Override public long length() {
                    return status.getLen();
                }

                /** {@inheritDoc} */
                @Override public Map<String, String> properties() {
                    return props;
                }
            };

        }
        catch (FileNotFoundException ignore) {
            return null;
        }
        catch (IOException e) {
            throw handleSecondaryFsError(e, "Failed to get file status [path=" + path + "]");
        }
    }

    /** {@inheritDoc} */
    @Override public long usedSpaceSize() throws GridException {
        try {
            return fileSys.getContentSummary(new Path(fileSys.getUri())).getSpaceConsumed();
        }
        catch (IOException e) {
            throw handleSecondaryFsError(e, "Failed to get used space size of file system.");
        }
    }

    /** {@inheritDoc} */
    @Nullable @Override public Map<String, String> properties() {
        return props;
    }

    /** {@inheritDoc} */
    @Override public void close() throws GridException {
        try {
            fileSys.close();
        }
        catch (IOException e) {
            throw new GridException(e);
        }
    }
}
