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

package org.gridgain.grid.kernal.processors.cache;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.kernal.managers.communication.*;
import org.gridgain.grid.kernal.processors.cache.distributed.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.communication.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.grid.util.direct.*;
import org.gridgain.testframework.junits.common.*;
import org.jdk8.backport.*;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static org.gridgain.grid.cache.GridCacheWriteSynchronizationMode.*;

/**
 * Test cases for preload tests.
 */
public class GridCacheReplicatedSynchronousCommitTest extends GridCommonAbstractTest {
    /** */
    private static final int ADDITION_CACHE_NUMBER = 2;

    /** */
    private static final String NO_COMMIT = "no_commit";

    /** */
    private final Collection<TestCommunicationSpi> commSpis = new ConcurrentLinkedDeque8<>();

    /** */
    private static GridTcpDiscoveryIpFinder ipFinder = new GridTcpDiscoveryVmIpFinder(true);

    /**
     *
     */
    public GridCacheReplicatedSynchronousCommitTest() {
        super(false /*start grid. */);
    }

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration c = super.getConfiguration(gridName);

        GridCacheConfiguration cc = defaultCacheConfiguration();

        cc.setCacheMode(GridCacheMode.REPLICATED);

        cc.setWriteSynchronizationMode(FULL_SYNC);

        c.setCacheConfiguration(cc);

        TestCommunicationSpi commSpi = new TestCommunicationSpi(gridName.equals(NO_COMMIT));

        c.setCommunicationSpi(commSpi);

        commSpis.add(commSpi);

        GridTcpDiscoverySpi disco = new GridTcpDiscoverySpi();

        disco.setIpFinder(ipFinder);

        c.setDiscoverySpi(disco);

        return c;
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        super.afterTest();

        commSpis.clear();
    }

    /**
     * @throws Exception If test failed.
     */
    public void testSynchronousCommit() throws Exception {
        try {
            Grid firstGrid = startGrid("1");

            GridCache<Integer, String> firstCache = firstGrid.cache(null);

            for (int i = 0; i < ADDITION_CACHE_NUMBER; i++)
                startGrid(String.valueOf(i + 2));

            firstCache.put(1, "val1");

            int cnt = 0;

            for (TestCommunicationSpi commSpi : commSpis)
                cnt += commSpi.messagesCount();

            assert cnt == ADDITION_CACHE_NUMBER;
        }
        finally {
            stopAllGrids();
        }
    }

    /**
     * @throws Exception If test failed.
     */
    public void testSynchronousCommitNodeLeave() throws Exception {
        try {
            Grid grid1 = startGrid("1");

            startGrid(NO_COMMIT);

            Grid grid3 = startGrid("3");

            GridCache<Integer, String> cache1 = grid1.cache(null);
            GridCache<Integer, String> cache3 = grid3.cache(null);

            GridFuture<?> fut = multithreadedAsync(
                new Callable<Object>() {
                    @Nullable @Override public Object call() throws Exception {
                        Thread.sleep(1000);

                        stopGrid(NO_COMMIT);

                        return null;
                    }
                },
                1);

            cache1.put(1, "val1");

            assert cache3.get(1) != null;

            fut.get();
        }
        finally {
            stopAllGrids();
        }
    }

    /**
     *
     */
    private static class TestCommunicationSpi extends GridTcpCommunicationSpi {
        /** */
        private final AtomicInteger msgCnt = new AtomicInteger();

        /** */
        private boolean noCommit;

        /**
         * @param noCommit Send Commit or not.
         */
        private TestCommunicationSpi(boolean noCommit) {
            this.noCommit = noCommit;
        }

        /**
         * @return Number of transaction finish messages that was sent.
         */
        public int messagesCount() {
            return msgCnt.get();
        }

        /** {@inheritDoc} */
        @Override public void sendMessage(GridNode node, GridTcpCommunicationMessageAdapter msg)
            throws GridSpiException {
            Object obj = ((GridIoMessage)msg).message();

            if (obj instanceof GridDistributedTxFinishResponse) {
                msgCnt.incrementAndGet();

                if (noCommit)
                    return;
            }

            super.sendMessage(node, msg);
        }
    }
}
