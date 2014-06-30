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

package org.gridgain.grid.kernal.managers;

import org.gridgain.grid.*;
import org.gridgain.grid.kernal.managers.checkpoint.*;
import org.gridgain.grid.kernal.managers.collision.*;
import org.gridgain.grid.kernal.managers.communication.*;
import org.gridgain.grid.kernal.managers.deployment.*;
import org.gridgain.grid.kernal.managers.discovery.*;
import org.gridgain.grid.kernal.managers.eventstorage.*;
import org.gridgain.grid.kernal.managers.failover.*;
import org.gridgain.grid.kernal.managers.loadbalancer.*;
import org.gridgain.grid.kernal.managers.swapspace.*;
import org.gridgain.grid.kernal.processors.resource.*;
import org.gridgain.grid.marshaller.optimized.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.checkpoint.sharedfs.*;
import org.gridgain.grid.spi.collision.*;
import org.gridgain.grid.spi.collision.fifoqueue.*;
import org.gridgain.grid.spi.communication.*;
import org.gridgain.grid.spi.communication.tcp.*;
import org.gridgain.grid.spi.deployment.*;
import org.gridgain.grid.spi.deployment.local.*;
import org.gridgain.grid.spi.discovery.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.eventstorage.*;
import org.gridgain.grid.spi.eventstorage.memory.*;
import org.gridgain.grid.spi.failover.always.*;
import org.gridgain.grid.spi.loadbalancing.roundrobin.*;
import org.gridgain.grid.spi.swapspace.*;
import org.gridgain.grid.spi.swapspace.file.*;
import org.gridgain.testframework.junits.*;
import org.gridgain.testframework.junits.common.*;

/**
 * Managers stop test.
 *
 */
public class GridManagerStopSelfTest extends GridCommonAbstractTest {
    /** Kernal context. */
    private GridTestKernalContext ctx;

    /** */
    public GridManagerStopSelfTest() {
        super(/*startGrid*/false);
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        ctx = newContext();

        ctx.config().setPeerClassLoadingEnabled(true);

        ctx.add(new GridResourceProcessor(ctx));

        ctx.start();
    }

    /**
     * @param target Target spi.
     * @throws GridException If injection failed.
     */
    private void injectLogger(GridSpi target) throws GridException {
        ctx.resource().injectBasicResource(
            target,
            GridLoggerResource.class,
            ctx.config().getGridLogger().getLogger(target.getClass())
        );
    }

    /**
     * @throws Exception If failed.
     */
    public void testStopCheckpointManager() throws Exception {
        GridSharedFsCheckpointSpi spi = new GridSharedFsCheckpointSpi();

        injectLogger(spi);

        ctx.config().setCheckpointSpi(spi);

        GridCheckpointManager mgr = new GridCheckpointManager(ctx);

        mgr.stop(true);
    }

    /**
     * @throws Exception If failed.
     */
    public void testStopCollisionManager() throws Exception {
        GridCollisionSpi spi = new GridFifoQueueCollisionSpi();

        injectLogger(spi);

        ctx.config().setCollisionSpi(spi);

        GridCollisionManager mgr = new GridCollisionManager(ctx);

        mgr.stop(true);
    }

    /**
     * @throws Exception If failed.
     */
    public void testStopCommunicationManager() throws Exception {
        GridCommunicationSpi spi = new GridTcpCommunicationSpi();

        injectLogger(spi);

        ctx.config().setCommunicationSpi(spi);
        ctx.config().setMarshaller(new GridOptimizedMarshaller());

        GridIoManager mgr = new GridIoManager(ctx);

        mgr.stop(false);
    }

    /**
     * @throws Exception If failed.
     */
    public void testStopDeploymentManager() throws Exception {
        GridDeploymentSpi spi = new GridLocalDeploymentSpi();

        injectLogger(spi);

        ctx.config().setDeploymentSpi(spi);

        GridDeploymentManager mgr = new GridDeploymentManager(ctx);

        mgr.stop(true);
    }

    /**
     * @throws Exception If failed.
     */
    public void testStopDiscoveryManager() throws Exception {
        GridDiscoverySpi spi = new GridTcpDiscoverySpi();

        injectLogger(spi);

        ctx.config().setDiscoverySpi(spi);

        GridDiscoveryManager mgr = new GridDiscoveryManager(ctx);

        mgr.stop(true);
    }

    /**
     * @throws Exception If failed.
     */
    public void testStopEventStorageManager() throws Exception {
        GridEventStorageSpi spi = new GridMemoryEventStorageSpi();

        injectLogger(spi);

        ctx.config().setEventStorageSpi(spi);

        GridEventStorageManager mgr = new GridEventStorageManager(ctx);

        mgr.stop(true);
    }

    /**
     * @throws Exception If failed.
     */
    public void testStopFailoverManager() throws Exception {
        GridAlwaysFailoverSpi spi = new GridAlwaysFailoverSpi();

        injectLogger(spi);

        ctx.config().setFailoverSpi(spi);

        GridFailoverManager mgr = new GridFailoverManager(ctx);

        mgr.stop(true);
    }

    /**
     * @throws Exception If failed.
     */
    public void testStopLoadBalancingManager() throws Exception {
        GridRoundRobinLoadBalancingSpi spi = new GridRoundRobinLoadBalancingSpi();

        injectLogger(spi);

        ctx.config().setLoadBalancingSpi(spi);

        GridLoadBalancerManager mgr = new GridLoadBalancerManager(ctx);

        mgr.stop(true);
    }

    /**
     * @throws Exception If failed.
     */
    public void testStopSwapSpaceManager() throws Exception {
        GridSwapSpaceSpi spi = new GridFileSwapSpaceSpi();

        injectLogger(spi);

        ctx.config().setSwapSpaceSpi(spi);

        GridSwapSpaceManager mgr = new GridSwapSpaceManager(ctx);

        mgr.stop(true);
    }
}
