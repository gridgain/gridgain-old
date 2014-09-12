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
import org.gridgain.grid.ggfs.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.thread.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.grid.util.ipc.*;
import org.gridgain.grid.util.worker.*;

import java.util.*;
import java.util.concurrent.*;

import static org.gridgain.grid.ggfs.GridGgfsConfiguration.*;

/**
 * GGFS server manager.
 */
public class GridGgfsServerManager extends GridGgfsManager {
    /** IPC server rebind interval. */
    private static final long REBIND_INTERVAL = 3000;

    /** Collection of servers to maintain. */
    private Collection<GridGgfsServer> srvrs;

    /** Server port binders. */
    private BindWorker bindWorker;

    /** Kernal start latch. */
    private CountDownLatch kernalStartLatch = new CountDownLatch(1);

    /** {@inheritDoc} */
    @Override protected void start0() throws GridException {
        GridGgfsConfiguration ggfsCfg = ggfsCtx.configuration();
        Map<String,String> cfg = ggfsCfg.getIpcEndpointConfiguration();

        if (F.isEmpty(cfg)) {
            // Set default configuration.
            cfg = new HashMap<>();

            cfg.put("type", U.isWindows() ? "tcp" : "shmem");
            cfg.put("port", String.valueOf(DFLT_IPC_PORT));
        }

        if (ggfsCfg.isIpcEndpointEnabled())
            bind(cfg, /*management*/false);

        if (ggfsCfg.getManagementPort() >= 0) {
            cfg = new HashMap<>();

            cfg.put("type", "tcp");
            cfg.put("port", String.valueOf(ggfsCfg.getManagementPort()));

            bind(cfg, /*management*/true);
        }

        if (bindWorker != null)
            new GridThread(bindWorker).start();
    }

    /**
     * Tries to start server endpoint with specified configuration. If failed, will print warning and start a thread
     * that will try to periodically start this endpoint.
     *
     * @param endpointCfg Endpoint configuration to start.
     * @param mgmt {@code True} if endpoint is management.
     * @throws GridException If failed.
     */
    private void bind(final Map<String,String> endpointCfg, final boolean mgmt) throws GridException {
        if (srvrs == null)
            srvrs = new ConcurrentLinkedQueue<>();

        GridGgfsServer ipcSrv = new GridGgfsServer(ggfsCtx, endpointCfg, mgmt);

        try {
            ipcSrv.start();

            srvrs.add(ipcSrv);
        }
        catch (GridIpcEndpointBindException ignored) {
            int port = ipcSrv.getIpcServerEndpoint().getPort();

            String portMsg = port != -1 ? " Failed to bind to port (is port already in use?): " + port : "";

            U.warn(log, "Failed to start GGFS " + (mgmt ? "management " : "") + "endpoint " +
                "(will retry every " + (REBIND_INTERVAL / 1000) + "s)." +
                portMsg);

            if (bindWorker == null)
                bindWorker = new BindWorker();

            bindWorker.addConfiguration(endpointCfg, mgmt);
        }
    }

    /**
     * @return Collection of active endpoints.
     */
    public Collection<GridIpcServerEndpoint> endpoints() {
        return F.viewReadOnly(srvrs, new C1<GridGgfsServer, GridIpcServerEndpoint>() {
            @Override public GridIpcServerEndpoint apply(GridGgfsServer e) {
                return e.getIpcServerEndpoint();
            }
        });
    }

    /** {@inheritDoc} */
    @Override protected void onKernalStart0() throws GridException {
        if (!F.isEmpty(srvrs)) {
            for (GridGgfsServer srv : srvrs)
                srv.onKernalStart();
        }

        kernalStartLatch.countDown();
    }

    /** {@inheritDoc} */
    @Override protected void stop0(boolean cancel) {
        // Safety.
        kernalStartLatch.countDown();

        if (bindWorker != null) {
            bindWorker.cancel();

            U.join(bindWorker, log);
        }

        if (!F.isEmpty(srvrs)) {
            for (GridGgfsServer srv : srvrs)
                srv.stop(cancel);
        }
    }

    /**
     * Bind worker.
     */
    @SuppressWarnings("BusyWait")
    private class BindWorker extends GridWorker {
        /** Configurations to bind. */
        private Collection<GridBiTuple<Map<String, String>, Boolean>> bindCfgs = new LinkedList<>();

        /**
         * Constructor.
         */
        private BindWorker() {
            super(ggfsCtx.kernalContext().gridName(), "bind-worker", ggfsCtx.kernalContext().log());
        }

        /**
         * Adds configuration to bind on. Should not be called after thread start.
         *
         * @param cfg Configuration.
         * @param mgmt Management flag.
         */
        public void addConfiguration(Map<String, String> cfg, boolean mgmt) {
            bindCfgs.add(F.t(cfg, mgmt));
        }

        /** {@inheritDoc} */
        @Override protected void body() throws InterruptedException {
            kernalStartLatch.await();

            while (!isCancelled()) {
                Thread.sleep(REBIND_INTERVAL);

                Iterator<GridBiTuple<Map<String, String>, Boolean>> it = bindCfgs.iterator();

                while (it.hasNext()) {
                    GridBiTuple<Map<String, String>, Boolean> cfg = it.next();

                    GridGgfsServer ipcSrv = new GridGgfsServer(ggfsCtx, cfg.get1(), cfg.get2());

                    try {
                        ipcSrv.start();

                        ipcSrv.onKernalStart();

                        srvrs.add(ipcSrv);

                        it.remove();
                    }
                    catch (GridException e) {
                        if (GridWorker.log.isDebugEnabled())
                            GridWorker.log.debug("Failed to bind GGFS endpoint [cfg=" + cfg + ", err=" + e.getMessage() + ']');
                    }
                }

                if (bindCfgs.isEmpty())
                    break;
            }
        }
    }
}
