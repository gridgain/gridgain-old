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

package org.gridgain.grid.kernal.processors.affinity;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.cache.affinity.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.testframework.*;
import org.gridgain.testframework.junits.common.*;

import java.util.*;
import java.util.concurrent.*;

import static org.gridgain.grid.cache.GridCacheMode.*;

/**
 * Tests for {@link GridAffinityProcessor}.
 */
@GridCommonTest(group = "Affinity Processor")
public abstract class GridAffinityProcessorAbstractSelfTest extends GridCommonAbstractTest {
    /** Number of grids started for tests. Should not be less than 2. */
    private static final int NODES_CNT = 3;

    /** Cache name. */
    private static final String CACHE_NAME = "cache";

    /** IP finder. */
    private static final GridTcpDiscoveryIpFinder ipFinder = new GridTcpDiscoveryVmIpFinder(true);

    /** Flag to start grid with cache. */
    private boolean withCache;

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        GridTcpDiscoverySpi discoSpi = new GridTcpDiscoverySpi();

        discoSpi.setIpFinder(ipFinder);

        cfg.setDiscoverySpi(discoSpi);

        if (withCache) {
            GridCacheConfiguration cacheCfg = defaultCacheConfiguration();

            cacheCfg.setName(CACHE_NAME);
            cacheCfg.setCacheMode(PARTITIONED);
            cacheCfg.setBackups(1);
            cacheCfg.setAffinity(affinityFunction());

            cfg.setCacheConfiguration(cacheCfg);
        }

        return cfg;
    }

    /**
     * Creates affinity function for test.
     *
     * @return Affinity function.
     */
    protected abstract GridCacheAffinityFunction affinityFunction();

    /** {@inheritDoc} */
    @SuppressWarnings({"ConstantConditions"})
    @Override protected void beforeTestsStarted() throws Exception {
        assert NODES_CNT >= 1;

        withCache = false;

        for (int i = 0; i < NODES_CNT; i++)
            startGrid(i);

        withCache = true;

        for (int i = NODES_CNT; i < 2 * NODES_CNT; i++)
            startGrid(i);
    }

    /** {@inheritDoc} */
    @Override protected void afterTestsStopped() throws Exception {
        stopAllGrids();
    }

    /**
     * Test affinity functions caching and clean up.
     *
     * @throws Exception In case of any exception.
     */
    @SuppressWarnings("AssertEqualsBetweenInconvertibleTypes")
    public void testAffinityProcessor() throws Exception {
        Random rnd = new Random();

        final GridKernal grid1 = (GridKernal)grid(rnd.nextInt(NODES_CNT)); // With cache.
        GridKernal grid2 = (GridKernal)grid(NODES_CNT + rnd.nextInt(NODES_CNT)); // Without cache.

        assertEquals(NODES_CNT * 2, grid1.nodes().size());
        assertEquals(NODES_CNT * 2, grid2.nodes().size());

        GridTestUtils.assertThrows(log, new Callable<Void>() {
            @Override public Void call() throws Exception {
                grid1.cache(CACHE_NAME);

                return null;
            }
        }, IllegalArgumentException.class, null);

        GridCache<Integer, Integer> cache = grid2.cache(CACHE_NAME);

        assertNotNull(cache);

        GridAffinityProcessor affPrc1 = grid1.context().affinity();
        GridAffinityProcessor affPrc2 = grid2.context().affinity();

        // Create keys collection.
        Collection<Integer> keys = new ArrayList<>(1000);

        for (int i = 0; i < 1000; i++)
            keys.add(i);

        //
        // Validate affinity functions collection updated on first call.
        //

        Map<GridNode, Collection<Integer>> node1Map = affPrc1.mapKeysToNodes(CACHE_NAME, keys);
        Map<GridNode, Collection<Integer>> node2Map = affPrc2.mapKeysToNodes(CACHE_NAME, keys);
        Map<GridNode, Collection<Integer>> cacheMap = cache.affinity().mapKeysToNodes(keys);

        assertEquals(cacheMap.size(), node1Map.size());
        assertEquals(cacheMap.size(), node2Map.size());

        for (Map.Entry<GridNode, Collection<Integer>> entry : cacheMap.entrySet()) {
            GridNode node = entry.getKey();

            Collection<Integer> mappedKeys = entry.getValue();

            Collection<Integer> mapped1 = node1Map.get(node);
            Collection<Integer> mapped2 = node2Map.get(node);

            assertTrue(mappedKeys.containsAll(mapped1) && mapped1.containsAll(mappedKeys));
            assertTrue(mappedKeys.containsAll(mapped2) && mapped2.containsAll(mappedKeys));
        }
    }

    /**
     * Test performance of affinity processor.
     *
     * @throws Exception In case of any exception.
     */
    public void testPerformance() throws Exception {
        GridKernal grid = (GridKernal)grid(0);
        GridAffinityProcessor aff = grid.context().affinity();

        int keysSize = 1000000;

        Collection<Integer> keys = new ArrayList<>(keysSize);

        for (int i = 0; i < keysSize; i++)
            keys.add(i);

        long start = System.currentTimeMillis();

        int iterations = 10000000;

        for (int i = 0; i < iterations; i++)
            aff.mapKeyToNode(keys);

        long diff = System.currentTimeMillis() - start;

        info(">>> Map " + keysSize + " keys to " + grid.nodes().size() + " nodes " + iterations + " times in " + diff + "ms.");

        assertTrue(diff < 25000);
    }
}
