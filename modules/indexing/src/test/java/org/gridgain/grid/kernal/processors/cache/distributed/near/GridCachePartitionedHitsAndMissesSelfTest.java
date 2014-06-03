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
import org.gridgain.grid.dataload.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.grid.spi.indexing.h2.*;
import org.gridgain.testframework.junits.common.*;

import java.util.*;

import static org.gridgain.grid.cache.GridCacheMode.*;
import static org.gridgain.grid.cache.GridCacheDistributionMode.*;
import static org.gridgain.grid.cache.GridCacheWriteSynchronizationMode.*;

/**
 * Test for issue GG-3997 Total Hits and Misses display wrong value for in-memory database.
 */
public class GridCachePartitionedHitsAndMissesSelfTest extends GridCommonAbstractTest {
    /** Amount of grids to start. */
    private static final int GRID_CNT = 3;

    /** Count of total numbers to generate. */
    private static final int CNT = 2000;

    /** IP Finder. */
    private static final GridTcpDiscoveryIpFinder IP_FINDER = new GridTcpDiscoveryVmIpFinder(true);

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        // IndexingSpi
        GridH2IndexingSpi spi = new GridH2IndexingSpi();
        spi.setName("indexingSpi");
        spi.setDefaultIndexPrimitiveKey(true);
        cfg.setIndexingSpi(spi);

        // DiscoverySpi
        GridTcpDiscoverySpi disco = new GridTcpDiscoverySpi();
        disco.setIpFinder(IP_FINDER);
        cfg.setDiscoverySpi(disco);

        // Cache.
        cfg.setCacheConfiguration(cacheConfiguration(gridName));

        return cfg;
    }

    /**
     * Cache configuration.
     *
     * @param gridName Grid name.
     * @return Cache configuration.
     * @throws Exception In case of error.
     */
    protected GridCacheConfiguration cacheConfiguration(String gridName) throws Exception {
        GridCacheConfiguration cfg = defaultCacheConfiguration();
        cfg.setCacheMode(PARTITIONED);
        cfg.setStartSize(700000);
        cfg.setWriteSynchronizationMode(FULL_ASYNC);
        cfg.setEvictionPolicy(null);
        cfg.setBackups(1);
        cfg.setDefaultTxConcurrency(GridCacheTxConcurrency.PESSIMISTIC);
        cfg.setDefaultTxIsolation(GridCacheTxIsolation.REPEATABLE_READ);
        cfg.setDistributionMode(PARTITIONED_ONLY);
        cfg.setPreloadPartitionedDelay(-1);
        cfg.setBackups(1);

        return cfg;
    }

    /**
     * This test is just a wrapper for org.gridgain.examples1.data.realtime.GridPopularNumbersRealTimeExample
     *
     * @throws Exception If failed.
     */
    public void testHitsAndMisses() throws Exception {
        assert(GRID_CNT > 0);

        startGrids(GRID_CNT);

        try {
            final Grid g = grid(0);

            realTimePopulate(g);

            // Check metrics for the whole cache.
            long hits = 0;
            long misses = 0;

            for (int i = 0; i < GRID_CNT; i++) {
                GridCacheMetrics m = grid(i).cache(null).metrics();

                hits += m.hits();
                misses += m.misses();
            }

            assertEquals(CNT/2, hits);
            assertEquals(CNT/2, misses);
        }
        finally {
            stopAllGrids();
        }
    }

    /**
     * Populates cache with data loader.
     *
     * @param g Grid.
     * @throws GridException If failed.
     */
    private static void realTimePopulate(final Grid g) throws GridException {
        try (GridDataLoader<Integer, Long> ldr = g.dataLoader(null)) {
            // Sets max values to 1 so cache metrics have correct values.
            ldr.perNodeParallelLoadOperations(1);

            // Count closure which increments a count on remote node.
            ldr.updater(new IncrementingUpdater());

            for (int i = 0; i < CNT; i++)
                ldr.addData(i % (CNT / 2), 1L);
        }
    }

    /**
     * Increments value for key.
     */
    private static class IncrementingUpdater implements GridDataLoadCacheUpdater<Integer, Long> {
        /** */
        private static final GridClosure<Long, Long> INC = new GridClosure<Long, Long>() {
            @Override public Long apply(Long e) {
                return e == null ? 1L : e + 1;
            }
        };

        /** {@inheritDoc} */
        @Override public void update(GridCache<Integer, Long> cache,
            Collection<Map.Entry<Integer, Long>> entries) throws GridException {
            for (Map.Entry<Integer, Long> entry : entries)
                cache.transform(entry.getKey(), INC);
        }
    }
}
