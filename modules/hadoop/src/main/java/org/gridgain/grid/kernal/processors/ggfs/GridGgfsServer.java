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

package org.gridgain.grid.kernal.processors.ggfs;

import org.gridgain.grid.*;
import org.gridgain.grid.kernal.ggfs.common.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.thread.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.grid.util.ipc.*;
import org.gridgain.grid.util.ipc.loopback.*;
import org.gridgain.grid.util.ipc.shmem.*;
import org.gridgain.grid.util.worker.*;
import org.jdk8.backport.*;
import org.jetbrains.annotations.*;

import java.io.*;

import static org.gridgain.grid.spi.GridPortProtocol.*;

/**
 * GGFS server. Handles requests passed from GGFS clients.
 */
public class GridGgfsServer {
    /** GGFS context. */
    private final GridGgfsContext ggfsCtx;

    /** Logger. */
    private final GridLogger log;

    /** GGFS marshaller. */
    private final GridGgfsMarshaller marsh;

    /** Endpoint configuration. */
    private final String endpointCfg;

    /** Server endpoint. */
    private GridIpcServerEndpoint srvEndpoint;

    /** Server message handler. */
    private GridGgfsServerHandler hnd;

    /** Accept worker. */
    private AcceptWorker acceptWorker;

    /** Started client workers. */
    private ConcurrentLinkedDeque8<ClientWorker> clientWorkers = new ConcurrentLinkedDeque8<>();

    /** Flag indicating if this a management endpoint. */
    private final boolean mgmt;

    /**
     * Constructs ggfs server manager.
     * @param ggfsCtx GGFS context.
     * @param endpointCfg Endpoint configuration to start.
     * @param mgmt Management flag - if true, server is intended to be started for Visor.
     */
    public GridGgfsServer(GridGgfsContext ggfsCtx, String endpointCfg, boolean mgmt) {
        assert ggfsCtx != null;
        assert endpointCfg != null;

        this.endpointCfg = endpointCfg;
        this.ggfsCtx = ggfsCtx;
        this.mgmt = mgmt;

        log = ggfsCtx.kernalContext().log(GridGgfsServer.class);

        marsh = new GridGgfsMarshaller();
    }

    /**
     * Starts this server.
     *
     * @throws GridException If failed.
     */
    public void start() throws GridException {
        srvEndpoint = GridIpcServerEndpointDeserializer.deserialize(endpointCfg);

        if (U.isWindows() && srvEndpoint instanceof GridIpcSharedMemoryServerEndpoint)
            throw new GridException(GridIpcSharedMemoryServerEndpoint.class.getSimpleName() +
                " should not be configured on Windows (configure " +
                GridIpcServerTcpEndpoint.class.getSimpleName() + ")");

        if (srvEndpoint instanceof GridIpcServerTcpEndpoint) {
            GridIpcServerTcpEndpoint srvEndpoint0 = (GridIpcServerTcpEndpoint)srvEndpoint;

            srvEndpoint0.setManagement(mgmt);

            if (srvEndpoint0.getHost() == null) {
                if (mgmt) {
                    String locHostName = ggfsCtx.kernalContext().config().getLocalHost();

                    try {
                        srvEndpoint0.setHost(U.resolveLocalHost(locHostName).getHostAddress());
                    }
                    catch (IOException e) {
                        throw new GridException("Failed to resolve local host: " + locHostName, e);
                    }
                }
                else
                    // Bind non-management endpoint to 127.0.0.1 by default.
                    srvEndpoint0.setHost("127.0.0.1");
            }
        }

        ggfsCtx.kernalContext().resource().injectGeneric(srvEndpoint);

        srvEndpoint.start();

        // GridIpcServerEndpoint.getPort contract states return -1 if there is no port to be registered.
        if (srvEndpoint.getPort() >= 0)
            ggfsCtx.kernalContext().ports().registerPort(srvEndpoint.getPort(), TCP, srvEndpoint.getClass());

        hnd = new GridGgfsIpcHandler(ggfsCtx, mgmt);

        // Start client accept worker.
        acceptWorker = new AcceptWorker();
    }

    /**
     * Callback that is invoked when kernal is ready.
     */
    public void onKernalStart() {
        // Accept connections only when grid is ready.
        if (srvEndpoint != null)
            new GridThread(acceptWorker).start();
    }

    /**
     * Stops this server.
     *
     * @param cancel Cancel flag.
     */
    public void stop(boolean cancel) {
        // Skip if did not start.
        if (srvEndpoint == null)
            return;

        // Stop accepting new client connections.
        U.cancel(acceptWorker);

        U.join(acceptWorker, log);

        // Stop server handler, no more requests on existing connections will be processed.
        try {
            hnd.stop();
        }
        catch (GridException e) {
            U.error(log, "Failed to stop GGFS server handler (will close client connections anyway).", e);
        }

        // Stop existing client connections.
        for (ClientWorker worker : clientWorkers)
            U.cancel(worker);

        U.join(clientWorkers, log);

        // GridIpcServerEndpoint.getPort contract states return -1 if there is no port to be registered.
        if (srvEndpoint.getPort() >= 0)
            ggfsCtx.kernalContext().ports().deregisterPort(srvEndpoint.getPort(), TCP, srvEndpoint.getClass());

        try {
            ggfsCtx.kernalContext().resource().cleanupGeneric(srvEndpoint);
        }
        catch (GridException e) {
            U.error(log, "Failed to cleanup server endpoint.", e);
        }
    }

    /**
     * Gets IPC server endpoint.
     *
     * @return IPC server endpoint.
     */
    public GridIpcServerEndpoint getIpcServerEndpoint() {
        return srvEndpoint;
    }

    /**
     * Client reader thread.
     */
    private class ClientWorker extends GridWorker {
        /** Connected client endpoint. */
        private GridIpcEndpoint endpoint;

        /** Data output stream. */
        private final GridGgfsDataOutputStream out;

        /** Client session object. */
        private GridGgfsClientSession ses;

        /** Queue node for fast unlink. */
        private ConcurrentLinkedDeque8.Node<ClientWorker> node;

        /**
         * Creates client worker.
         *
         * @param idx Worker index for worker thread naming.
         * @param endpoint Connected client endpoint.
         * @throws GridException If endpoint output stream cannot be obtained.
         */
        protected ClientWorker(GridIpcEndpoint endpoint, int idx) throws GridException {
            super(ggfsCtx.kernalContext().gridName(), "ggfs-client-worker-" + idx, log);

            this.endpoint = endpoint;

            ses = new GridGgfsClientSession();

            out = new GridGgfsDataOutputStream(new BufferedOutputStream(endpoint.outputStream()));
        }

        /** {@inheritDoc} */
        @Override protected void body() throws InterruptedException, GridInterruptedException {
            try {
                GridGgfsDataInputStream dis = new GridGgfsDataInputStream(endpoint.inputStream());

                byte[] hdr = new byte[GridGgfsMarshaller.HEADER_SIZE];

                while (!Thread.currentThread().isInterrupted()) {
                    dis.readFully(hdr);

                    final long reqId = U.bytesToLong(hdr, 0);

                    int ordinal = U.bytesToInt(hdr, 8);

                    final GridGgfsIpcCommand cmd = GridGgfsIpcCommand.valueOf(ordinal);

                    GridGgfsMessage msg = marsh.unmarshall(cmd, hdr, dis);

                    GridFuture<GridGgfsMessage> fut = hnd.handleAsync(ses, msg, dis);

                    // If fut is null, no response is required.
                    if (fut != null) {
                        if (fut.isDone()) {
                            GridGgfsMessage res;

                            try {
                                res = fut.get();
                            }
                            catch (GridException e) {
                                res = new GridGgfsControlResponse();

                                ((GridGgfsControlResponse)res).error(e);
                            }

                            try {
                                synchronized (out) {
                                    // Reuse header.
                                    GridGgfsMarshaller.fillHeader(hdr, reqId, res.command());

                                    marsh.marshall(res, hdr, out);

                                    out.flush();
                                }
                            }
                            catch (IOException | GridException e) {
                                shutdown0(e);
                            }
                        }
                        else {
                            fut.listenAsync(new CIX1<GridFuture<GridGgfsMessage>>() {
                                @Override public void applyx(GridFuture<GridGgfsMessage> fut) {
                                    GridGgfsMessage res;

                                    try {
                                        res = fut.get();
                                    }
                                    catch (GridException e) {
                                        res = new GridGgfsControlResponse();

                                        ((GridGgfsControlResponse)res).error(e);
                                    }

                                    try {
                                        synchronized (out) {
                                            byte[] hdr = GridGgfsMarshaller.createHeader(reqId, res.command());

                                            marsh.marshall(res, hdr, out);

                                            out.flush();
                                        }
                                    }
                                    catch (IOException | GridException e) {
                                        shutdown0(e);
                                    }
                                }
                            });
                        }
                    }
                }
            }
            catch (EOFException ignored) {
                // Client closed connection.
            }
            catch (GridException | IOException e) {
                if (!isCancelled())
                    U.error(log, "Failed to read data from client (will close connection)", e);
            }
            finally {
                onFinished();
            }
        }

        /**
         * @param node Node in queue for this worker.
         */
        public void node(ConcurrentLinkedDeque8.Node<ClientWorker> node) {
            this.node = node;
        }

        /** {@inheritDoc} */
        @Override public void cancel() {
            super.cancel();

            shutdown0(null);
        }

        /**
         * @param e Optional exception occurred while stopping this
         */
        private void shutdown0(@Nullable Throwable e) {
            if (!isCancelled()) {
                if (e != null)
                    U.error(log, "Stopping client reader due to exception: " + endpoint, e);
            }

            U.closeQuiet(out);

            endpoint.close();
        }

        /**
         * Final resource cleanup.
         */
        private void onFinished() {
            // Second close is no-op, if closed manually.
            U.closeQuiet(out);

            endpoint.close();

            // Finally, remove from queue.
            if (clientWorkers.unlinkx(node))
                hnd.onClosed(ses);
        }
    }

    /**
     * Accept worker.
     */
    private class AcceptWorker extends GridWorker {
        /** Accept index. */
        private int acceptCnt;

        /**
         * Creates accept worker.
         */
        protected AcceptWorker() {
            super(ggfsCtx.kernalContext().gridName(), "ggfs-accept-worker", log);
        }

        /** {@inheritDoc} */
        @Override protected void body() throws InterruptedException, GridInterruptedException {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    GridIpcEndpoint client = srvEndpoint.accept();

                    if (log.isDebugEnabled())
                        log.debug("GGFS client connected [ggfsName=" + ggfsCtx.kernalContext().gridName() +
                            ", client=" + client + ']');

                    ClientWorker worker = new ClientWorker(client, acceptCnt++);

                    GridThread workerThread = new GridThread(worker);

                    ConcurrentLinkedDeque8.Node<ClientWorker> node = clientWorkers.addx(worker);

                    worker.node(node);

                    workerThread.start();
                }
            }
            catch (GridException e) {
                if (!isCancelled())
                    U.error(log, "Failed to accept client IPC connection (will shutdown accept thread).", e);
            }
            finally {
                srvEndpoint.close();
            }
        }

        /** {@inheritDoc} */
        @Override public void cancel() {
            super.cancel();

            srvEndpoint.close();
        }
    }
}
