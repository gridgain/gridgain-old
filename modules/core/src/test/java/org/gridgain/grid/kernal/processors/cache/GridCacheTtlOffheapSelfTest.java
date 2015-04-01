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

package org.gridgain.grid.kernal.processors.cache;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.cache.eviction.lru.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.testframework.junits.common.*;

/**
 * TTL test with offheap.
 */
public class GridCacheTtlOffheapSelfTest extends GridCommonAbstractTest {
    /** */
    private static final GridTcpDiscoveryIpFinder IP_FINDER = new GridTcpDiscoveryVmIpFinder(true);

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        GridCacheConfiguration cache = new GridCacheConfiguration();

        cache.setCacheMode(GridCacheMode.PARTITIONED);
        cache.setAtomicityMode(GridCacheAtomicityMode.ATOMIC);
        cache.setDistributionMode(GridCacheDistributionMode.PARTITIONED_ONLY);
        cache.setMemoryMode(GridCacheMemoryMode.ONHEAP_TIERED);
        cache.setOffHeapMaxMemory(0);
        cache.setDefaultTimeToLive(2000);
        cache.setEvictionPolicy(new GridCacheLruEvictionPolicy(5));

        cfg.setCacheConfiguration(cache);

        GridTcpDiscoverySpi disco = new GridTcpDiscoverySpi();

        disco.setIpFinder(IP_FINDER);

        cfg.setDiscoverySpi(disco);

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        startGrid();
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        stopGrid();
    }

    /**
     * @throws Exception If failed.
     */
    public void testTtl() throws Exception {
        GridCache<Integer, Integer> cache = cache();

        for (int i = 0; i < 10; i++) {
            GridCacheEntry<Integer, Integer> e = cache.entry(i);

            e.timeToLive(2000);
            e.setx(i);
        }

        assertEquals(5, cache.size());
        assertEquals(5, cache.offHeapEntriesCount());

        Thread.sleep(2500);

        info(cache.size() + " -> " + cache.offHeapEntriesCount());

        assertEquals(0, cache.size());
        assertEquals(0, cache.offHeapEntriesCount());
    }
}
