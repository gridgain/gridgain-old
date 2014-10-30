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

package org.gridgain.grid.kernal.managers.deployment;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.compute.*;
import org.gridgain.grid.kernal.managers.communication.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.communication.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.grid.util.direct.*;
import org.gridgain.testframework.junits.common.*;
import org.jdk8.backport.*;

import java.util.*;
import java.util.concurrent.atomic.*;

import static org.gridgain.grid.cache.GridCacheMode.*;
import static org.gridgain.grid.cache.GridCacheWriteSynchronizationMode.*;

/**
 * Tests message count for different deployment scenarios.
 */
public class GridDeploymentMessageCountSelfTest extends GridCommonAbstractTest {
    /** VM ip finder for TCP discovery. */
    private static GridTcpDiscoveryIpFinder ipFinder = new GridTcpDiscoveryVmIpFinder(true);

    /** Test p2p task. */
    private static final String TEST_TASK = "org.gridgain.grid.tests.p2p.GridSingleSplitTestTask";

    /** Test p2p value. */
    private static final String TEST_VALUE = "org.gridgain.grid.tests.p2p.GridCacheDeploymentTestValue";

    /** SPIs. */
    private Map<String, MessageCountingCommunicationSpi> commSpis = new ConcurrentHashMap8<>();

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        GridTcpDiscoverySpi discoSpi = new GridTcpDiscoverySpi();

        discoSpi.setIpFinder(ipFinder);

        cfg.setDiscoverySpi(discoSpi);

        cfg.setPeerClassLoadingEnabled(true);

        GridCacheConfiguration cacheCfg = defaultCacheConfiguration();

        cacheCfg.setCacheMode(REPLICATED);
        cacheCfg.setWriteSynchronizationMode(FULL_SYNC);

        cfg.setCacheConfiguration(cacheCfg);

        MessageCountingCommunicationSpi commSpi = new MessageCountingCommunicationSpi();

        commSpis.put(gridName, commSpi);

        cfg.setCommunicationSpi(commSpi);

        return cfg;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    public void testTaskDeployment() throws Exception {
        ClassLoader ldr = getExternalClassLoader();

        Class taskCls = ldr.loadClass(TEST_TASK);

        try {
            startGrids(2);

            GridComputeTaskFuture<Object> taskFut = grid(0).compute().execute(taskCls, 2);

            Integer res = (Integer)taskFut.get();

            assertEquals(Integer.valueOf(2), res);

            for (MessageCountingCommunicationSpi spi : commSpis.values()) {
                assertTrue(spi.deploymentMessageCount() > 0);

                spi.resetCount();
            }

            for (int i = 0; i < 10; i++) {
                taskFut = grid(0).compute().execute(taskCls, 2);

                res = (Integer)taskFut.get();

                assertEquals(Integer.valueOf(2), res);
            }

            for (MessageCountingCommunicationSpi spi : commSpis.values())
                assertEquals(0, spi.deploymentMessageCount());
        }
        finally {
            stopAllGrids();
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testCacheValueDeploymentOnPut() throws Exception {
        ClassLoader ldr = getExternalClassLoader();

        Class valCls = ldr.loadClass(TEST_VALUE);

        try {
            startGrids(2);

            GridCache<Object, Object> cache = grid(0).cache(null);

            cache.put("key", valCls.newInstance());

            for (int i = 0; i < 2; i++)
                assertNotNull("For grid: " + i, grid(i).cache(null).peek("key"));

            for (MessageCountingCommunicationSpi spi : commSpis.values()) {
                assertTrue(spi.deploymentMessageCount() > 0);

                spi.resetCount();
            }

            for (int i = 0; i < 10; i++) {
                String key = "key" + i;

                cache.put(key, valCls.newInstance());

                for (int k = 0; k < 2; k++)
                    assertNotNull(grid(k).cache(null).peek(key));
            }

            for (MessageCountingCommunicationSpi spi : commSpis.values())
                assertEquals(0, spi.deploymentMessageCount());
        }
        finally {
            stopAllGrids();
        }
    }

    /**
     *
     */
    private class MessageCountingCommunicationSpi extends GridTcpCommunicationSpi {
        /** */
        private AtomicInteger msgCnt = new AtomicInteger();

        /** {@inheritDoc} */
        @Override public void sendMessage(GridNode node, GridTcpCommunicationMessageAdapter msg)
            throws GridSpiException {
            if (isDeploymentMessage((GridIoMessage)msg))
                msgCnt.incrementAndGet();

            super.sendMessage(node, msg);
        }

        /**
         * @return Number of deployment messages.
         */
        public int deploymentMessageCount() {
            return msgCnt.get();
        }

        /**
         * Resets counter to zero.
         */
        public void resetCount() {
            msgCnt.set(0);
        }

        /**
         * Checks if it is a p2p deployment message.
         *
         * @param msg Message to check.
         * @return {@code True} if this is a p2p message.
         */
        private boolean isDeploymentMessage(GridIoMessage msg) {
            Object origMsg = msg.message();

            boolean dep = (origMsg instanceof GridDeploymentRequest) || (origMsg instanceof GridDeploymentResponse);

            if (dep)
                info(">>> Got deployment message: " + origMsg);

            return dep;
        }
    }
}
