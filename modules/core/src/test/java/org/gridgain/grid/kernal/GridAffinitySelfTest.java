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
import org.gridgain.grid.lang.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.testframework.junits.common.*;

import java.util.*;

import static org.gridgain.grid.cache.GridCacheMode.*;

/**
 * Tests affinity mapping.
 */
public class GridAffinitySelfTest extends GridCommonAbstractTest {
    /** VM ip finder for TCP discovery. */
    private static final GridTcpDiscoveryIpFinder IP_FINDER = new GridTcpDiscoveryVmIpFinder(true);

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        GridTcpDiscoverySpi disco = new GridTcpDiscoverySpi();

        disco.setMaxMissedHeartbeats(Integer.MAX_VALUE);
        disco.setIpFinder(IP_FINDER);

        cfg.setDiscoverySpi(disco);

        if (gridName.endsWith("1"))
            cfg.setCacheConfiguration(); // Empty cache configuration.
        else {
            assert gridName.endsWith("2");

            GridCacheConfiguration cacheCfg = defaultCacheConfiguration();

            cacheCfg.setCacheMode(PARTITIONED);
            cacheCfg.setBackups(1);

            cfg.setCacheConfiguration(cacheCfg);
        }

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        startGridsMultiThreaded(1, 2);
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        stopAllGrids();
    }

    /**
     * @throws GridException If failed.
     */
    public void testAffinity() throws GridException {
        Grid g1 = grid(1);
        Grid g2 = grid(2);

        assert caches(g1).size() == 0;
        assert F.first(caches(g2)).getCacheMode() == PARTITIONED;

        Map<GridNode, Collection<String>> map = g1.mapKeysToNodes(null, F.asList("1"));

        assertNotNull(map);
        assertEquals("Invalid map size: " + map.size(), 1, map.size());
        assertEquals(F.first(map.keySet()), g2.localNode());

        UUID id1 = g1.mapKeyToNode(null, "2").id();

        assertNotNull(id1);
        assertEquals(g2.localNode().id(), id1);

        UUID id2 = g1.mapKeyToNode(null, "3").id();

        assertNotNull(id2);
        assertEquals(g2.localNode().id(), id2);
    }

    /**
     * @param g Grid.
     * @return Non-system caches.
     */
    private Collection<GridCacheConfiguration> caches(Grid g) {
        return F.view(Arrays.asList(g.configuration().getCacheConfiguration()), new GridPredicate<GridCacheConfiguration>() {
            @Override public boolean apply(GridCacheConfiguration c) {
                return c.getName() == null || !c.getName().equals(CU.UTILITY_CACHE_NAME);
            }
        });
    }
}
