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

package org.gridgain.grid.kernal.processors.cache.distributed.near;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.cache.query.*;
import org.gridgain.grid.events.*;
import org.gridgain.grid.kernal.processors.cache.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.grid.spi.indexing.h2.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static org.gridgain.grid.cache.GridCacheAtomicityMode.*;
import static org.gridgain.grid.cache.GridCacheMode.*;
import static org.gridgain.grid.cache.GridCacheDistributionMode.*;

/**
 * Test for distributed queries with node restarts.
 */
public class GridCacheQueryNodeRestartSelfTest extends GridCacheAbstractSelfTest {
    /** */
    private static final int GRID_CNT = 3;

    /** */
    private static final int KEY_CNT = 1000;

    /** */
    private static GridTcpDiscoveryIpFinder ipFinder = new GridTcpDiscoveryVmIpFinder(true);

    /** {@inheritDoc} */
    @Override protected int gridCount() {
        return GRID_CNT;
    }

    /** {@inheritDoc} */
    @Override protected long getTestTimeout() {
        return 90 * 1000;
    }

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration c = super.getConfiguration(gridName);

        GridTcpDiscoverySpi disco = new GridTcpDiscoverySpi();

        disco.setIpFinder(ipFinder);

        c.setDiscoverySpi(disco);

        GridCacheConfiguration cc = defaultCacheConfiguration();

        cc.setCacheMode(PARTITIONED);
        cc.setBackups(1);
        cc.setWriteSynchronizationMode(GridCacheWriteSynchronizationMode.FULL_SYNC);
        cc.setAtomicityMode(TRANSACTIONAL);
        cc.setDistributionMode(NEAR_PARTITIONED);

        c.setCacheConfiguration(cc);

        GridH2IndexingSpi idxSpi = new GridH2IndexingSpi();

        idxSpi.setDefaultIndexPrimitiveKey(true);

        c.setIndexingSpi(idxSpi);

        return c;
    }

    /**
     * JUnit.
     *
     * @throws Exception If failed.
     */
    @SuppressWarnings({"TooBroadScope"})
    public void testRestarts() throws Exception {
        int duration = 60 * 1000;
        int qryThreadNum = 10;
        final long nodeLifeTime = 2 * 1000;
        final int logFreq = 20;

        final GridCache<Integer, Integer> cache = grid(0).cache(null);

        assert cache != null;

        for (int i = 0; i < KEY_CNT; i++)
            cache.put(i, i);

        assertEquals(KEY_CNT, cache.size());

        final AtomicInteger qryCnt = new AtomicInteger();

        final AtomicBoolean done = new AtomicBoolean();

        GridFuture<?> fut1 = multithreadedAsync(new CAX() {
            @Override public void applyx() throws GridException {
                while (!done.get()) {
                    GridCacheQuery<Map.Entry<Integer, Integer>> qry =
                        cache.queries().createSqlQuery(Integer.class, "_val >= 0");

                    qry.includeBackups(true);
                    qry.keepAll(true);

                    assertFalse(qry.execute().get().isEmpty());

                    int c = qryCnt.incrementAndGet();

                    if (c % logFreq == 0)
                        info("Executed queries: " + c);
                }
            }
        }, qryThreadNum);

        final AtomicInteger restartCnt = new AtomicInteger();

        CollectingEventListener lsnr = new CollectingEventListener();

        for (int i = 0; i < GRID_CNT; i++)
            grid(i).events().localListen(lsnr, GridEventType.EVT_CACHE_PRELOAD_STOPPED);

        GridFuture<?> fut2 = multithreadedAsync(new Callable<Object>() {
            @SuppressWarnings({"BusyWait"})
            @Override public Object call() throws Exception {
                while (!done.get()) {
                    int idx = GRID_CNT;

                    startGrid(idx);

                    Thread.sleep(nodeLifeTime);

                    stopGrid(idx);

                    int c = restartCnt.incrementAndGet();

                    if (c % logFreq == 0)
                        info("Node restarts: " + c);
                }

                return true;
            }
        }, 1);

        Thread.sleep(duration);

        done.set(true);

        fut1.get();
        fut2.get();

        info("Awaiting preload events [restartCnt=" + restartCnt.get() + ']');

        boolean success = lsnr.awaitEvents(GRID_CNT * 2 * restartCnt.get(), 15000);

        for (int i = 0; i < GRID_CNT; i++)
            grid(i).events().stopLocalListen(lsnr, GridEventType.EVT_CACHE_PRELOAD_STOPPED);

        assert success;
    }

    /** Listener that will wait for specified number of events received. */
    private class CollectingEventListener implements GridPredicate<GridEvent> {
        /** Registered events count. */
        private int evtCnt;

        /** {@inheritDoc} */
        @Override public synchronized boolean apply(GridEvent evt) {
            evtCnt++;

            info("Processed event [evt=" + evt + ", evtCnt=" + evtCnt + ']');

            notifyAll();

            return true;
        }

        /**
         * Waits until total number of events processed is equal or greater then argument passed.
         *
         * @param cnt Number of events to wait.
         * @param timeout Timeout to wait.
         * @return {@code True} if successfully waited, {@code false} if timeout happened.
         * @throws InterruptedException If thread is interrupted.
         */
        public synchronized boolean awaitEvents(int cnt, long timeout) throws InterruptedException {
            long start = U.currentTimeMillis();

            long now = start;

            while (start + timeout > now) {
                if (evtCnt >= cnt)
                    return true;

                wait(start + timeout - now);

                now = U.currentTimeMillis();
            }

            return false;
        }
    }
}
