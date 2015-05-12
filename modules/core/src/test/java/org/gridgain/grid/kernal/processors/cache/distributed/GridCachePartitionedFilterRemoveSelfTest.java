/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gridgain.grid.kernal.processors.cache.distributed;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.spi.discovery.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.testframework.junits.common.*;

import java.io.*;

import static org.gridgain.grid.cache.GridCacheAtomicityMode.*;
import static org.gridgain.grid.cache.GridCacheMode.*;
import static org.gridgain.grid.cache.GridCacheDistributionMode.*;

/**
 * Test filtered put.
 */
public class GridCachePartitionedFilterRemoveSelfTest extends GridCommonAbstractTest {
    /** */
    private static final GridTcpDiscoveryIpFinder IP_FINDER = new GridTcpDiscoveryVmIpFinder(true);

    /**
     * {@inheritDoc}
     */
    @Override
    protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        cfg.setDiscoverySpi(discoverySpi());
        cfg.setCacheConfiguration(cacheConfiguration());

        return cfg;
    }

    /**
     * @return Discovery SPI;
     */
    private GridDiscoverySpi discoverySpi() {
        GridTcpDiscoverySpi spi = new GridTcpDiscoverySpi();

        spi.setIpFinder(IP_FINDER);

        return spi;
    }

    /**
     * @return Cache configuration.
     */
    private GridCacheConfiguration cacheConfiguration() {
        GridCacheConfiguration cfg = defaultCacheConfiguration();

        cfg.setCacheMode(PARTITIONED);
        cfg.setBackups(1);
        cfg.setAtomicityMode(ATOMIC);
        cfg.setDistributionMode(PARTITIONED_ONLY);

        return cfg;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void beforeTestsStarted() throws Exception {
        startGrids(3);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void afterTestsStopped() throws Exception {
        stopGrid();
    }

    /**
     * @throws Exception If failed.
     */
    public void testRemovePrimary() throws Exception {
        checkRemove(true);
    }

    /**
     * @throws Exception If failed.
     */
    public void testRemoveBackup() throws Exception {
        checkRemove(false);
    }

    /**
     * @throws Exception If failed.
     */
    private void checkRemove(final boolean primary) throws Exception {
        GridCache<Object, Object> cache = grid(0).cache(null);

        for (int i = 0; i < 100; i++)
            cache.put(i, i);

        GridPredicate<GridCacheEntry<Object, Object>> p = new GridCacheEntryGridPredicate(primary);

        for (int g = 0; g < 3; g++) {
            GridCache<Object, Object> cache0 = grid(g).cache(null);

            cache0.removeAll(cache0.projection(p).keySet());
        }

        for (int g = 0; g < 3; g++) {
            assertTrue(grid(g).cache(null).isEmpty());
        }
    }

    /**
     *
     */
    private static class GridCacheEntryGridPredicate implements GridPredicate<GridCacheEntry<Object, Object>>, Serializable {
        /** */
        private final boolean primary;

        /**
         * @param primary Primary flag.
         */
        public GridCacheEntryGridPredicate(boolean primary) {
            this.primary = primary;
        }

        /** {@inheritDoc} */
        @Override public boolean apply(GridCacheEntry<Object, Object> e) {
            return primary ? e.primary() : e.backup();
        }
    }
}
