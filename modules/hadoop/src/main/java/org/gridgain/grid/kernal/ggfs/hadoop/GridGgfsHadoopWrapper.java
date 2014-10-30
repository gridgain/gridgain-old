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
import org.apache.hadoop.conf.*;
import org.gridgain.grid.*;
import org.gridgain.grid.ggfs.*;
import org.gridgain.grid.kernal.processors.ggfs.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import static org.gridgain.grid.kernal.ggfs.hadoop.GridGgfsHadoopEndpoint.*;
import static org.gridgain.grid.kernal.ggfs.hadoop.GridGgfsHadoopUtils.*;

/**
 * Wrapper for GGFS server.
 */
public class GridGgfsHadoopWrapper implements GridGgfsHadoop {
    /** Delegate. */
    private final AtomicReference<Delegate> delegateRef = new AtomicReference<>();

    /** Authority. */
    private final String authority;

    /** Connection string. */
    private final GridGgfsHadoopEndpoint endpoint;

    /** Log directory. */
    private final String logDir;

    /** Configuration. */
    private final Configuration conf;

    /** Logger. */
    private final Log log;

    /**
     * Constructor.
     *
     * @param authority Authority (connection string).
     * @param logDir Log directory for server.
     * @param conf Configuration.
     * @param log Current logger.
     */
    public GridGgfsHadoopWrapper(String authority, String logDir, Configuration conf, Log log) throws IOException {
        try {
            this.authority = authority;
            this.endpoint = new GridGgfsHadoopEndpoint(authority);
            this.logDir = logDir;
            this.conf = conf;
            this.log = log;
        }
        catch (GridException e) {
            throw new IOException("Failed to parse endpoint: " + authority, e);
        }
    }

    /** {@inheritDoc} */
    @Override public GridGgfsHandshakeResponse handshake(String logDir) throws IOException {
        return withReconnectHandling(new FileSystemClosure<GridGgfsHandshakeResponse>() {
            @Override public GridGgfsHandshakeResponse apply(GridGgfsHadoopEx hadoop,
                GridGgfsHandshakeResponse hndResp) throws GridException, IOException {
                return hndResp;
            }
        });
    }

    /** {@inheritDoc} */
    @Override public void close(boolean force) {
        Delegate delegate = delegateRef.get();

        if (delegate != null && delegateRef.compareAndSet(delegate, null))
            delegate.close(force);
    }

    /** {@inheritDoc} */
    @Override public GridGgfsFile info(final GridGgfsPath path) throws IOException {
        return withReconnectHandling(new FileSystemClosure<GridGgfsFile>() {
            @Override public GridGgfsFile apply(GridGgfsHadoopEx hadoop, GridGgfsHandshakeResponse hndResp)
                throws GridException, IOException {
                return hadoop.info(path);
            }
        }, path);
    }

    /** {@inheritDoc} */
    @Override public GridGgfsFile update(final GridGgfsPath path, final Map<String, String> props) throws IOException {
        return withReconnectHandling(new FileSystemClosure<GridGgfsFile>() {
            @Override public GridGgfsFile apply(GridGgfsHadoopEx hadoop, GridGgfsHandshakeResponse hndResp)
                throws GridException, IOException {
                return hadoop.update(path, props);
            }
        }, path);
    }

    /** {@inheritDoc} */
    @Override public Boolean setTimes(final GridGgfsPath path, final long accessTime, final long modificationTime)
        throws IOException {
        return withReconnectHandling(new FileSystemClosure<Boolean>() {
            @Override public Boolean apply(GridGgfsHadoopEx hadoop, GridGgfsHandshakeResponse hndResp)
                throws GridException, IOException {
                return hadoop.setTimes(path, accessTime, modificationTime);
            }
        }, path);
    }

    /** {@inheritDoc} */
    @Override public Boolean rename(final GridGgfsPath src, final GridGgfsPath dest) throws IOException {
        return withReconnectHandling(new FileSystemClosure<Boolean>() {
            @Override public Boolean apply(GridGgfsHadoopEx hadoop, GridGgfsHandshakeResponse hndResp)
                throws GridException, IOException {
                return hadoop.rename(src, dest);
            }
        }, src);
    }

    /** {@inheritDoc} */
    @Override public Boolean delete(final GridGgfsPath path, final boolean recursive) throws IOException {
        return withReconnectHandling(new FileSystemClosure<Boolean>() {
            @Override public Boolean apply(GridGgfsHadoopEx hadoop, GridGgfsHandshakeResponse hndResp)
                throws GridException, IOException {
                return hadoop.delete(path, recursive);
            }
        }, path);
    }

    /** {@inheritDoc} */
    @Override public Collection<GridGgfsBlockLocation> affinity(final GridGgfsPath path, final long start,
        final long len) throws IOException {
        return withReconnectHandling(new FileSystemClosure<Collection<GridGgfsBlockLocation>>() {
            @Override public Collection<GridGgfsBlockLocation> apply(GridGgfsHadoopEx hadoop,
                GridGgfsHandshakeResponse hndResp) throws GridException, IOException {
                return hadoop.affinity(path, start, len);
            }
        }, path);
    }

    /** {@inheritDoc} */
    @Override public GridGgfsPathSummary contentSummary(final GridGgfsPath path) throws IOException {
        return withReconnectHandling(new FileSystemClosure<GridGgfsPathSummary>() {
            @Override public GridGgfsPathSummary apply(GridGgfsHadoopEx hadoop, GridGgfsHandshakeResponse hndResp)
                throws GridException, IOException {
                return hadoop.contentSummary(path);
            }
        }, path);
    }

    /** {@inheritDoc} */
    @Override public Boolean mkdirs(final GridGgfsPath path, final Map<String, String> props) throws IOException {
        return withReconnectHandling(new FileSystemClosure<Boolean>() {
            @Override public Boolean apply(GridGgfsHadoopEx hadoop, GridGgfsHandshakeResponse hndResp)
                throws GridException, IOException {
                return hadoop.mkdirs(path, props);
            }
        }, path);
    }

    /** {@inheritDoc} */
    @Override public Collection<GridGgfsFile> listFiles(final GridGgfsPath path) throws IOException {
        return withReconnectHandling(new FileSystemClosure<Collection<GridGgfsFile>>() {
            @Override public Collection<GridGgfsFile> apply(GridGgfsHadoopEx hadoop,
                GridGgfsHandshakeResponse hndResp) throws GridException, IOException {
                return hadoop.listFiles(path);
            }
        }, path);
    }

    /** {@inheritDoc} */
    @Override public Collection<GridGgfsPath> listPaths(final GridGgfsPath path) throws IOException {
        return withReconnectHandling(new FileSystemClosure<Collection<GridGgfsPath>>() {
            @Override public Collection<GridGgfsPath> apply(GridGgfsHadoopEx hadoop,
                GridGgfsHandshakeResponse hndResp) throws GridException, IOException {
                return hadoop.listPaths(path);
            }
        }, path);
    }

    /** {@inheritDoc} */
    @Override public GridGgfsStatus fsStatus() throws IOException {
        return withReconnectHandling(new FileSystemClosure<GridGgfsStatus>() {
            @Override public GridGgfsStatus apply(GridGgfsHadoopEx hadoop, GridGgfsHandshakeResponse hndResp)
                throws GridException, IOException {
                return hadoop.fsStatus();
            }
        });
    }

    /** {@inheritDoc} */
    @Override public GridGgfsHadoopStreamDelegate open(final GridGgfsPath path) throws IOException {
        return withReconnectHandling(new FileSystemClosure<GridGgfsHadoopStreamDelegate>() {
            @Override public GridGgfsHadoopStreamDelegate apply(GridGgfsHadoopEx hadoop,
                GridGgfsHandshakeResponse hndResp) throws GridException, IOException {
                return hadoop.open(path);
            }
        }, path);
    }

    /** {@inheritDoc} */
    @Override public GridGgfsHadoopStreamDelegate open(final GridGgfsPath path, final int seqReadsBeforePrefetch)
        throws IOException {
        return withReconnectHandling(new FileSystemClosure<GridGgfsHadoopStreamDelegate>() {
            @Override public GridGgfsHadoopStreamDelegate apply(GridGgfsHadoopEx hadoop,
                GridGgfsHandshakeResponse hndResp) throws GridException, IOException {
                return hadoop.open(path, seqReadsBeforePrefetch);
            }
        }, path);
    }

    /** {@inheritDoc} */
    @Override public GridGgfsHadoopStreamDelegate create(final GridGgfsPath path, final boolean overwrite,
        final boolean colocate, final int replication, final long blockSize, @Nullable final Map<String, String> props)
        throws IOException {
        return withReconnectHandling(new FileSystemClosure<GridGgfsHadoopStreamDelegate>() {
            @Override public GridGgfsHadoopStreamDelegate apply(GridGgfsHadoopEx hadoop,
                GridGgfsHandshakeResponse hndResp) throws GridException, IOException {
                return hadoop.create(path, overwrite, colocate, replication, blockSize, props);
            }
        }, path);
    }

    /** {@inheritDoc} */
    @Override public GridGgfsHadoopStreamDelegate append(final GridGgfsPath path, final boolean create,
        @Nullable final Map<String, String> props) throws IOException {
        return withReconnectHandling(new FileSystemClosure<GridGgfsHadoopStreamDelegate>() {
            @Override public GridGgfsHadoopStreamDelegate apply(GridGgfsHadoopEx hadoop,
                GridGgfsHandshakeResponse hndResp) throws GridException, IOException {
                return hadoop.append(path, create, props);
            }
        }, path);
    }

    /**
     * Execute closure which is not path-specific.
     *
     * @param clo Closure.
     * @return Result.
     * @throws IOException If failed.
     */
    private <T> T withReconnectHandling(FileSystemClosure<T> clo) throws IOException {
        return withReconnectHandling(clo, null);
    }

    /**
     * Execute closure.
     *
     * @param clo Closure.
     * @param path Path for exceptions.
     * @return Result.
     * @throws IOException If failed.
     */
    private <T> T withReconnectHandling(final FileSystemClosure<T> clo, @Nullable GridGgfsPath path)
        throws IOException {
        Exception err = null;

        for (int i = 0; i < 2; i++) {
            Delegate curDelegate = null;

            boolean close = false;
            boolean force = false;

            try {
                curDelegate = delegate();

                assert curDelegate != null;

                close = curDelegate.doomed;

                return clo.apply(curDelegate.hadoop, curDelegate.hndResp);
            }
            catch (GridGgfsHadoopCommunicationException e) {
                if (curDelegate != null && !curDelegate.doomed) {
                    // Try getting rid fo faulty delegate ASAP.
                    delegateRef.compareAndSet(curDelegate, null);

                    close = true;
                    force = true;
                }

                if (log.isDebugEnabled())
                    log.debug("Failed to send message to a server: " + e);

                err = e;
            }
            catch (GridException e) {
                throw GridGgfsHadoopUtils.cast(e, path != null ? path.toString() : null);
            }
            finally {
                if (close) {
                    assert curDelegate != null;

                    curDelegate.close(force);
                }
            }
        }

        throw new IOException("Failed to communicate with GGFS.", err);
    }

    /**
     * Get delegate creating it if needed.
     *
     * @return Delegate.
     */
    private Delegate delegate() throws GridGgfsHadoopCommunicationException {
        Exception err = null;

        // 1. If delegate is set, return it immediately.
        Delegate curDelegate = delegateRef.get();

        if (curDelegate != null)
            return curDelegate;

        // 2. Guess that we are in the same VM.
        if (!parameter(conf, PARAM_GGFS_ENDPOINT_NO_EMBED, authority, false)) {
            GridGgfsEx ggfs = null;

            if (endpoint.grid() == null) {
                try {
                    Grid grid = G.grid();

                    ggfs = (GridGgfsEx)grid.ggfs(endpoint.ggfs());
                }
                catch (Exception e) {
                    err = e;
                }
            }
            else {
                for (Grid grid : G.allGrids()) {
                    try {
                        ggfs = (GridGgfsEx)grid.ggfs(endpoint.ggfs());

                        break;
                    }
                    catch (Exception e) {
                        err = e;
                    }
                }
            }

            if (ggfs != null) {
                GridGgfsHadoopEx hadoop = null;

                try {
                    hadoop = new GridGgfsHadoopInProc(ggfs, log);

                    curDelegate = new Delegate(hadoop, hadoop.handshake(logDir));
                }
                catch (IOException | GridException e) {
                    if (e instanceof GridGgfsHadoopCommunicationException)
                        hadoop.close(true);

                    if (log.isDebugEnabled())
                        log.debug("Failed to connect to in-proc GGFS, fallback to IPC mode.", e);

                    err = e;
                }
            }
        }

        // 3. Try connecting using shmem.
        if (!parameter(conf, PARAM_GGFS_ENDPOINT_NO_LOCAL_SHMEM, authority, false)) {
            if (curDelegate == null && !U.isWindows()) {
                GridGgfsHadoopEx hadoop = null;

                try {
                    hadoop = new GridGgfsHadoopOutProc(endpoint.port(), endpoint.grid(), endpoint.ggfs(), log);

                    curDelegate = new Delegate(hadoop, hadoop.handshake(logDir));
                }
                catch (IOException | GridException e) {
                    if (e instanceof GridGgfsHadoopCommunicationException)
                        hadoop.close(true);

                    if (log.isDebugEnabled())
                        log.debug("Failed to connect to out-proc local GGFS using shmem.", e);

                    err = e;
                }
            }
        }

        // 4. Try local TCP connection.
        boolean skipLocalTcp = parameter(conf, PARAM_GGFS_ENDPOINT_NO_LOCAL_TCP, authority, false);

        if (!skipLocalTcp) {
            if (curDelegate == null) {
                GridGgfsHadoopEx hadoop = null;

                try {
                    hadoop = new GridGgfsHadoopOutProc(LOCALHOST, endpoint.port(), endpoint.grid(), endpoint.ggfs(),
                        log);

                    curDelegate = new Delegate(hadoop, hadoop.handshake(logDir));
                }
                catch (IOException | GridException e) {
                    if (e instanceof GridGgfsHadoopCommunicationException)
                        hadoop.close(true);

                    if (log.isDebugEnabled())
                        log.debug("Failed to connect to out-proc local GGFS using TCP.", e);

                    err = e;
                }
            }
        }

        // 5. Try remote TCP connection.
        if (curDelegate == null && (skipLocalTcp || !F.eq(LOCALHOST, endpoint.host()))) {
            GridGgfsHadoopEx hadoop = null;

            try {
                hadoop = new GridGgfsHadoopOutProc(endpoint.host(), endpoint.port(), endpoint.grid(), endpoint.ggfs(), log);

                curDelegate = new Delegate(hadoop, hadoop.handshake(logDir));
            }
            catch (IOException | GridException e) {
                if (e instanceof GridGgfsHadoopCommunicationException)
                    hadoop.close(true);

                if (log.isDebugEnabled())
                    log.debug("Failed to connect to out-proc remote GGFS using TCP.", e);

                err = e;
            }
        }

        if (curDelegate != null) {
            if (!delegateRef.compareAndSet(null, curDelegate))
                curDelegate.doomed = true;

            return curDelegate;
        }
        else
            throw new GridGgfsHadoopCommunicationException("Failed to connect to GGFS: " + endpoint, err);
    }

    /**
     * File system operation closure.
     */
    private static interface FileSystemClosure<T> {
        /**
         * Call closure body.
         *
         * @param hadoop RPC handler.
         * @param hndResp Handshake response.
         * @return Result.
         * @throws GridException If failed.
         * @throws IOException If failed.
         */
        public T apply(GridGgfsHadoopEx hadoop, GridGgfsHandshakeResponse hndResp) throws GridException, IOException;
    }

    /**
     * Delegate.
     */
    private static class Delegate {
        /** RPC handler. */
        private final GridGgfsHadoopEx hadoop;

        /** Handshake request. */
        private final GridGgfsHandshakeResponse hndResp;

        /** Close guard. */
        private final AtomicBoolean closeGuard = new AtomicBoolean();

        /** Whether this delegate must be closed at the end of the next invocation. */
        private boolean doomed;

        /**
         * Constructor.
         *
         * @param hadoop Hadoop.
         * @param hndResp Handshake response.
         */
        private Delegate(GridGgfsHadoopEx hadoop, GridGgfsHandshakeResponse hndResp) {
            this.hadoop = hadoop;
            this.hndResp = hndResp;
        }

        /**
         * Close underlying RPC handler.
         *
         * @param force Force flag.
         */
        private void close(boolean force) {
            if (closeGuard.compareAndSet(false, true))
                hadoop.close(force);
        }
    }
}
