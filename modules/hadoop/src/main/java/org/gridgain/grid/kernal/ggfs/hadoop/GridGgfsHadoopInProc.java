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

import org.apache.commons.logging.*;
import org.gridgain.grid.*;
import org.gridgain.grid.ggfs.*;
import org.gridgain.grid.kernal.processors.ggfs.*;
import org.gridgain.grid.util.lang.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Communication with grid in the same process.
 */
public class GridGgfsHadoopInProc implements GridGgfsHadoopEx {
    /** Target GGFS. */
    private final GridGgfsEx ggfs;

    /** Buffer size. */
    private final int bufSize;

    /** Event listeners. */
    private final Map<GridGgfsHadoopStreamDelegate, GridGgfsHadoopStreamEventListener> lsnrs =
        new ConcurrentHashMap<>();

    /** Logger. */
    private final Log log;

    /**
     * Constructor.
     *
     * @param ggfs Target GGFS.
     */
    public GridGgfsHadoopInProc(GridGgfsEx ggfs, Log log) {
        this.ggfs = ggfs;
        this.log = log;

        bufSize = ggfs.configuration().getBlockSize() * 2;
    }

    /** {@inheritDoc} */
    @Override public GridGgfsHandshakeResponse handshake(String logDir) {
        ggfs.clientLogDirectory(logDir);

        return new GridGgfsHandshakeResponse(ggfs.name(), ggfs.proxyPaths(), ggfs.groupBlockSize(),
            ggfs.globalSampling());
    }

    /** {@inheritDoc} */
    @Override public void close(boolean force) {
        // Perform cleanup.
        for (GridGgfsHadoopStreamEventListener lsnr : lsnrs.values()) {
            try {
                lsnr.onClose();
            }
            catch (GridException e) {
                if (log.isDebugEnabled())
                    log.debug("Failed to notify stream event listener", e);
            }
        }
    }

    /** {@inheritDoc} */
    @Override public GridGgfsFile info(GridGgfsPath path) throws GridException {
        try {
            return ggfs.info(path);
        }
        catch (IllegalStateException e) {
            throw new GridGgfsHadoopCommunicationException("Failed to get file info because Grid is stopping: " + path);
        }
    }

    /** {@inheritDoc} */
    @Override public GridGgfsFile update(GridGgfsPath path, Map<String, String> props) throws GridException {
        try {
            return ggfs.update(path, props);
        }
        catch (IllegalStateException e) {
            throw new GridGgfsHadoopCommunicationException("Failed to update file because Grid is stopping: " + path);
        }
    }

    /** {@inheritDoc} */
    @Override public Boolean setTimes(GridGgfsPath path, long accessTime, long modificationTime) throws GridException {
        try {
            ggfs.setTimes(path, accessTime, modificationTime);

            return true;
        }
        catch (IllegalStateException e) {
            throw new GridGgfsHadoopCommunicationException("Failed to set path times because Grid is stopping: " +
                path);
        }
    }

    /** {@inheritDoc} */
    @Override public Boolean rename(GridGgfsPath src, GridGgfsPath dest) throws GridException {
        try {
            ggfs.rename(src, dest);

            return true;
        }
        catch (IllegalStateException e) {
            throw new GridGgfsHadoopCommunicationException("Failed to rename path because Grid is stopping: " + src);
        }
    }

    /** {@inheritDoc} */
    @Override public Boolean delete(GridGgfsPath path, boolean recursive) throws GridException {
        try {
            return ggfs.delete(path, recursive);
        }
        catch (IllegalStateException e) {
            throw new GridGgfsHadoopCommunicationException("Failed to delete path because Grid is stopping: " + path);
        }
    }

    /** {@inheritDoc} */
    @Override public GridGgfsStatus fsStatus() throws GridException {
        try {
            return ggfs.globalSpace();
        }
        catch (IllegalStateException e) {
            throw new GridGgfsHadoopCommunicationException("Failed to get file system status because Grid is " +
                "stopping.");
        }
    }

    /** {@inheritDoc} */
    @Override public Collection<GridGgfsPath> listPaths(GridGgfsPath path) throws GridException {
        try {
            return ggfs.listPaths(path);
        }
        catch (IllegalStateException e) {
            throw new GridGgfsHadoopCommunicationException("Failed to list paths because Grid is stopping: " + path);
        }
    }

    /** {@inheritDoc} */
    @Override public Collection<GridGgfsFile> listFiles(GridGgfsPath path) throws GridException {
        try {
            return ggfs.listFiles(path);
        }
        catch (IllegalStateException e) {
            throw new GridGgfsHadoopCommunicationException("Failed to list files because Grid is stopping: " + path);
        }
    }

    /** {@inheritDoc} */
    @Override public Boolean mkdirs(GridGgfsPath path, Map<String, String> props) throws GridException {
        try {
            ggfs.mkdirs(path, props);

            return true;
        }
        catch (IllegalStateException e) {
            throw new GridGgfsHadoopCommunicationException("Failed to create directory because Grid is stopping: " +
                path);
        }
    }

    /** {@inheritDoc} */
    @Override public GridGgfsPathSummary contentSummary(GridGgfsPath path) throws GridException {
        try {
            return ggfs.summary(path);
        }
        catch (IllegalStateException e) {
            throw new GridGgfsHadoopCommunicationException("Failed to get content summary because Grid is stopping: " +
                path);
        }
    }

    /** {@inheritDoc} */
    @Override public Collection<GridGgfsBlockLocation> affinity(GridGgfsPath path, long start, long len)
        throws GridException {
        try {
            return ggfs.affinity(path, start, len);
        }
        catch (IllegalStateException e) {
            throw new GridGgfsHadoopCommunicationException("Failed to get affinity because Grid is stopping: " + path);
        }
    }

    /** {@inheritDoc} */
    @Override public GridGgfsHadoopStreamDelegate open(GridGgfsPath path) throws GridException {
        try {
            GridGgfsInputStreamAdapter stream = ggfs.open(path, bufSize);

            return new GridGgfsHadoopStreamDelegate(this, stream, stream.fileInfo().length());
        }
        catch (IllegalStateException e) {
            throw new GridGgfsHadoopCommunicationException("Failed to open file because Grid is stopping: " + path);
        }
    }

    /** {@inheritDoc} */
    @Override public GridGgfsHadoopStreamDelegate open(GridGgfsPath path, int seqReadsBeforePrefetch)
        throws GridException {
        try {
            GridGgfsInputStreamAdapter stream = ggfs.open(path, bufSize, seqReadsBeforePrefetch);

            return new GridGgfsHadoopStreamDelegate(this, stream, stream.fileInfo().length());
        }
        catch (IllegalStateException e) {
            throw new GridGgfsHadoopCommunicationException("Failed to open file because Grid is stopping: " + path);
        }
    }

    /** {@inheritDoc} */
    @Override public GridGgfsHadoopStreamDelegate create(GridGgfsPath path, boolean overwrite, boolean colocate,
        int replication, long blockSize, @Nullable Map<String, String> props) throws GridException {
        try {
            GridGgfsOutputStream stream = ggfs.create(path, bufSize, overwrite,
                colocate ? ggfs.nextAffinityKey() : null, replication, blockSize, props);

            return new GridGgfsHadoopStreamDelegate(this, stream);
        }
        catch (IllegalStateException e) {
            throw new GridGgfsHadoopCommunicationException("Failed to create file because Grid is stopping: " + path);
        }
    }

    /** {@inheritDoc} */
    @Override public GridGgfsHadoopStreamDelegate append(GridGgfsPath path, boolean create,
        @Nullable Map<String, String> props) throws GridException {
        try {
            GridGgfsOutputStream stream = ggfs.append(path, bufSize, create, props);

            return new GridGgfsHadoopStreamDelegate(this, stream);
        }
        catch (IllegalStateException e) {
            throw new GridGgfsHadoopCommunicationException("Failed to append file because Grid is stopping: " + path);
        }
    }

    /** {@inheritDoc} */
    @Override public GridPlainFuture<byte[]> readData(GridGgfsHadoopStreamDelegate delegate, long pos, int len,
        @Nullable byte[] outBuf, int outOff, int outLen) {
        GridGgfsInputStreamAdapter stream = delegate.target();

        try {
            byte[] res = null;

            if (outBuf != null) {
                int outTailLen = outBuf.length - outOff;

                if (len <= outTailLen)
                    stream.readFully(pos, outBuf, outOff, len);
                else {
                    stream.readFully(pos, outBuf, outOff, outTailLen);

                    int remainderLen = len - outTailLen;

                    res = new byte[remainderLen];

                    stream.readFully(pos, res, 0, remainderLen);
                }
            } else {
                res = new byte[len];

                stream.readFully(pos, res, 0, len);
            }

            return new GridPlainFutureAdapter<>(res);
        }
        catch (IllegalStateException | IOException e) {
            GridGgfsHadoopStreamEventListener lsnr = lsnrs.get(delegate);

            if (lsnr != null)
                lsnr.onError(e.getMessage());

            return new GridPlainFutureAdapter<>(e);
        }
    }

    /** {@inheritDoc} */
    @Override public void writeData(GridGgfsHadoopStreamDelegate delegate, byte[] data, int off, int len)
        throws IOException {
        try {
            GridGgfsOutputStream stream = delegate.target();

            stream.write(data, off, len);
        }
        catch (IllegalStateException | IOException e) {
            GridGgfsHadoopStreamEventListener lsnr = lsnrs.get(delegate);

            if (lsnr != null)
                lsnr.onError(e.getMessage());

            if (e instanceof IllegalStateException)
                throw new IOException("Failed to write data to GGFS stream because Grid is stopping.", e);
            else
                throw e;
        }
    }

    /** {@inheritDoc} */
    @Override public void flush(GridGgfsHadoopStreamDelegate delegate) throws IOException {
        try {
            GridGgfsOutputStream stream = delegate.target();

            stream.flush();
        }
        catch (IllegalStateException | IOException e) {
            GridGgfsHadoopStreamEventListener lsnr = lsnrs.get(delegate);

            if (lsnr != null)
                lsnr.onError(e.getMessage());

            if (e instanceof IllegalStateException)
                throw new IOException("Failed to flush data to GGFS stream because Grid is stopping.", e);
            else
                throw e;
        }
    }

    /** {@inheritDoc} */
    @Override public void closeStream(GridGgfsHadoopStreamDelegate desc) throws IOException {
        Closeable closeable = desc.target();

        try {
            closeable.close();
        }
        catch (IllegalStateException e) {
            throw new IOException("Failed to close GGFS stream because Grid is stopping.", e);
        }
    }

    /** {@inheritDoc} */
    @Override public void addEventListener(GridGgfsHadoopStreamDelegate delegate,
        GridGgfsHadoopStreamEventListener lsnr) {
        GridGgfsHadoopStreamEventListener lsnr0 = lsnrs.put(delegate, lsnr);

        assert lsnr0 == null || lsnr0 == lsnr;

        if (log.isDebugEnabled())
            log.debug("Added stream event listener [delegate=" + delegate + ']');
    }

    /** {@inheritDoc} */
    @Override public void removeEventListener(GridGgfsHadoopStreamDelegate delegate) {
        GridGgfsHadoopStreamEventListener lsnr0 = lsnrs.remove(delegate);

        if (lsnr0 != null && log.isDebugEnabled())
            log.debug("Removed stream event listener [delegate=" + delegate + ']');
    }
}
