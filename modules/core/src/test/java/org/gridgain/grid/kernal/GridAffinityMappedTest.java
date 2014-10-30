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

package org.gridgain.grid.kernal;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.cache.affinity.*;
import org.gridgain.grid.kernal.processors.cache.distributed.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.testframework.junits.common.*;

import java.util.*;

import static org.gridgain.grid.cache.GridCacheMode.*;

/**
 * Tests affinity mapping when {@link GridCacheAffinityKeyMapper} is used.
 */
public class GridAffinityMappedTest extends GridCommonAbstractTest {
    /** VM ip finder for TCP discovery. */
    private static GridTcpDiscoveryIpFinder ipFinder = new GridTcpDiscoveryVmIpFinder(true);

    /**
     *
     */
    public GridAffinityMappedTest() {
        super(false);
    }

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        GridTcpDiscoverySpi disco = new GridTcpDiscoverySpi();
        disco.setMaxMissedHeartbeats(Integer.MAX_VALUE);
        disco.setIpFinder(ipFinder);
        cfg.setDiscoverySpi(disco);

        if (gridName.endsWith("1"))
            cfg.setCacheConfiguration(); // Empty cache configuration.
        else {
            assert gridName.endsWith("2") || gridName.endsWith("3");

            GridCacheConfiguration cacheCfg = defaultCacheConfiguration();

            cacheCfg.setCacheMode(PARTITIONED);
            cacheCfg.setAffinity(new MockCacheAffinityFunction());
            cacheCfg.setAffinityMapper(new MockCacheAffinityKeyMapper());

            cfg.setCacheConfiguration(cacheCfg);
            cfg.setUserAttributes(F.asMap(GridCacheModuloAffinityFunction.IDX_ATTR, gridName.endsWith("2") ? 0 : 1));
        }

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        startGrid(1);
        startGrid(2);
        startGrid(3);
    }

    /** {@inheritDoc} */
    @Override protected void afterTestsStopped() throws Exception {
        stopGrid(1);
        stopGrid(2);
        stopGrid(3);
    }

    /**
     * @throws GridException If failed.
     */
    public void testMappedAffinity() throws GridException {
        Grid g1 = grid(1);
        Grid g2 = grid(2);
        Grid g3 = grid(3);

        assert g1.configuration().getCacheConfiguration().length == 0;
        assert g2.configuration().getCacheConfiguration()[0].getCacheMode() == PARTITIONED;
        assert g3.configuration().getCacheConfiguration()[0].getCacheMode() == PARTITIONED;

        GridNode first = g2.localNode();
        GridNode second = g3.localNode();

        //When MockCacheAfinity and MockCacheAffinityKeyMapper are set to cache configuration we expect the following.
        //Key 0 is mapped to partition 0, first node.
        //Key 1 is mapped to partition 1, second node.
        //key 2 is mapped to partition 0, first node because mapper substitutes key 2 with affinity key 0.
        Map<GridNode, Collection<Integer>> map = g1.mapKeysToNodes(null, F.asList(0));

        assertNotNull(map);
        assertEquals("Invalid map size: " + map.size(), 1, map.size());
        assertEquals(F.first(map.keySet()), first);

        UUID id1 = g1.mapKeyToNode(null, 1).id();

        assertNotNull(id1);
        assertEquals(second.id(),  id1);

        UUID id2 = g1.mapKeyToNode(null, 2).id();

        assertNotNull(id2);
        assertEquals(first.id(),  id2);
    }

    /**
     * Mock affinity implementation that ensures constant key-to-node mapping based on {@link GridCacheModuloAffinityFunction}
     * The partition selection is as follows: 0 maps to partition 0 and any other value maps to partition 1
     */
    private static class MockCacheAffinityFunction extends GridCacheModuloAffinityFunction {
        /**
         * Initializes module affinity with 2 parts and 0 backups
         */
        private MockCacheAffinityFunction() {
            super(2, 0);
        }

        /** {@inheritDoc} */
        @Override public int partition(Object key) {
            return Integer.valueOf(0) == key ? 0 : 1;
        }

        /** {@inheritDoc} */
        @Override public void reset() {
            //no-op
        }
    }

    /**
     * Mock affinity mapper implementation that substitutes values other than 0 and 1 with 0.
     */
    private static class MockCacheAffinityKeyMapper implements GridCacheAffinityKeyMapper {
        /** {@inheritDoc} */
        @Override public Object affinityKey(Object key) {
            return key instanceof Integer ? 1 == (Integer)key ? key : 0 : key;
        }

        /** {@inheritDoc} */
        @Override public void reset() {
            // This mapper is stateless and needs no initialization logic.
        }
    }
}
