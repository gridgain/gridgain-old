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

package org.gridgain.grid.kernal.processors.cache.eviction;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.cache.eviction.*;
import org.gridgain.grid.cache.eviction.fifo.*;
import org.gridgain.grid.cache.eviction.lru.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.testframework.junits.common.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static org.gridgain.grid.cache.GridCacheMode.*;
import static org.gridgain.grid.cache.GridCacheDistributionMode.*;
import static org.gridgain.grid.cache.GridCacheTxConcurrency.*;
import static org.gridgain.grid.cache.GridCacheTxIsolation.*;
import static org.gridgain.grid.cache.GridCacheWriteSynchronizationMode.*;

/**
 *
 */
public class GridCacheConcurrentEvictionsSelfTest extends GridCommonAbstractTest {
    /** IP finder. */
    private static final GridTcpDiscoveryIpFinder ipFinder = new GridTcpDiscoveryVmIpFinder(true);

    /** Replicated cache. */
    private GridCacheMode mode = REPLICATED;

    /** */
    private GridCacheEvictionPolicy<?, ?> plc;

    /** */
    private GridCacheEvictionPolicy<?, ?> nearPlc;

    /** */
    private int warmUpPutsCnt;

    /** */
    private int iterCnt;

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration c = super.getConfiguration(gridName);

        GridCacheConfiguration cc = defaultCacheConfiguration();

        cc.setCacheMode(mode);

        cc.setSwapEnabled(false);

        cc.setWriteSynchronizationMode(FULL_SYNC);

        cc.setDistributionMode(PARTITIONED_ONLY);

        cc.setEvictionPolicy(plc);
        cc.setNearEvictionPolicy(nearPlc);

        cc.setDefaultTxConcurrency(PESSIMISTIC);
        cc.setDefaultTxIsolation(READ_COMMITTED);

        c.setCacheConfiguration(cc);

        GridTcpDiscoverySpi disco = new GridTcpDiscoverySpi();

        disco.setIpFinder(ipFinder);

        c.setDiscoverySpi(disco);

        return c;
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        super.afterTest();

        plc = null;
        nearPlc = null;
    }

    /**
     * @throws Exception If failed.
     */
    public void testConcurrentPutsFifoLocal() throws Exception {
        mode = LOCAL;
        plc = new GridCacheFifoEvictionPolicy<Object, Object>(1000);
        nearPlc = null;
        warmUpPutsCnt = 100000;
        iterCnt = 100000;

        checkConcurrentPuts();
    }

    /**
     * @throws Exception If failed.
     */
    public void testConcurrentPutsLruLocal() throws Exception {
        mode = LOCAL;
        plc = new GridCacheLruEvictionPolicy<Object, Object>(1000);
        nearPlc = null;
        warmUpPutsCnt = 100000;
        iterCnt = 100000;

        checkConcurrentPuts();
    }

    /**
     * @throws Exception If failed.
     */
    private void checkConcurrentPuts() throws Exception {
        try {
            Grid grid = startGrid(1);

            final GridCache<Integer, Integer> cache = grid.cache(null);

            // Warm up.
            for (int i = 0; i < warmUpPutsCnt; i++) {
                cache.putx(i, i);

                if (i != 0 && i % 1000 == 0)
                    info("Warm up puts count: " + i);
            }

            info("Cache size: " + cache.size());

            cache.removeAll();

            final AtomicInteger idx = new AtomicInteger();

            int threadCnt = 30;

            long start = System.currentTimeMillis();

            GridFuture<?> fut = multithreadedAsync(
                new Callable<Object>() {
                    @Override public Object call() throws Exception {
                        for (int i = 0; i < iterCnt; i++) {
                            int j = idx.incrementAndGet();

                            cache.putx(j, j);

                            if (i != 0 && i % 10000 == 0)
                                // info("Puts count: " + i);
                                info("Stats [putsCnt=" + i + ", size=" + cache.size() + ']');
                        }

                        return null;
                    }
                },
                threadCnt
            );

            fut.get();

            info("Test results [threadCnt=" + threadCnt + ", iterCnt=" + iterCnt + ", cacheSize=" + cache.size() +
                ", duration=" + (System.currentTimeMillis() - start) + ']');
        }
        finally {
            stopAllGrids();
        }
    }
}
