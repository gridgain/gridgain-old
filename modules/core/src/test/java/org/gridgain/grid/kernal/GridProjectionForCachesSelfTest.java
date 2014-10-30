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
import org.gridgain.grid.spi.discovery.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.testframework.junits.common.*;
import org.jetbrains.annotations.*;

import java.util.*;

import static org.gridgain.grid.cache.GridCacheMode.*;

/**
 * Tests for {@link GridProjection#forCache(String, String...)} method.
 */
public class GridProjectionForCachesSelfTest extends GridCommonAbstractTest {
    /** */
    private static final GridTcpDiscoveryIpFinder IP_FINDER = new GridTcpDiscoveryVmIpFinder(true);

    /** */
    private static final String CACHE_NAME = "cache";

    /** */
    private Grid grid;

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        cfg.setDiscoverySpi(discoverySpi());

        if (gridName.equals(getTestGridName(0)))
            cfg.setCacheConfiguration(cacheConfiguration(null));
        else if (gridName.equals(getTestGridName(1)))
            cfg.setCacheConfiguration(cacheConfiguration(CACHE_NAME));
        else if (gridName.equals(getTestGridName(2)) || gridName.equals(getTestGridName(3)))
            cfg.setCacheConfiguration(cacheConfiguration(null), cacheConfiguration(CACHE_NAME));
        else
            cfg.setCacheConfiguration();

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
     * @param cacheName Cache name.
     * @return Cache configuration.
     */
    private GridCacheConfiguration cacheConfiguration(@Nullable String cacheName) {
        GridCacheConfiguration cfg = defaultCacheConfiguration();

        cfg.setName(cacheName);
        cfg.setCacheMode(PARTITIONED);
        cfg.setBackups(1);

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        for (int i = 0; i < 5; i++)
            startGrid(i);
    }

    /** {@inheritDoc} */
    @Override protected void afterTestsStopped() throws Exception {
        stopAllGrids();
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        grid = grid(0);
    }

    /**
     * @throws Exception If failed.
     */
    public void testProjectionForDefaultCache() throws Exception {
        GridProjection prj = grid.forCache(null);

        assert prj != null;
        assert prj.nodes().size() == 3;
        assert prj.nodes().contains(grid(0).localNode());
        assert !prj.nodes().contains(grid(1).localNode());
        assert prj.nodes().contains(grid(2).localNode());
        assert prj.nodes().contains(grid(3).localNode());
        assert !prj.nodes().contains(grid(4).localNode());
    }

    /**
     * @throws Exception If failed.
     */
    public void testProjectionForNamedCache() throws Exception {
        GridProjection prj = grid.forCache(CACHE_NAME);

        assert prj != null;
        assert prj.nodes().size() == 3;
        assert !prj.nodes().contains(grid(0).localNode());
        assert prj.nodes().contains(grid(1).localNode());
        assert prj.nodes().contains(grid(2).localNode());
        assert prj.nodes().contains(grid(3).localNode());
        assert !prj.nodes().contains(grid(4).localNode());
    }

    /**
     * @throws Exception If failed.
     */
    public void testProjectionForBothCaches() throws Exception {
        GridProjection prj = grid.forCache(null, CACHE_NAME);

        assert prj != null;
        assert prj.nodes().size() == 2;
        assert !prj.nodes().contains(grid(0).localNode());
        assert !prj.nodes().contains(grid(1).localNode());
        assert prj.nodes().contains(grid(2).localNode());
        assert prj.nodes().contains(grid(3).localNode());
        assert !prj.nodes().contains(grid(4).localNode());
    }

    /**
     * @throws Exception If failed.
     */
    public void testProjectionForWrongCacheName() throws Exception {
        GridProjection prj = grid.forCache("wrong");

        assert prj != null;
        assert prj.nodes().isEmpty();
    }

    /**
     * @throws Exception If failed.
     */
    public void testProjections() throws Exception {
        GridNode locNode = grid.localNode();
        UUID locId = locNode.id();

        assertNotNull(locId);

        assertEquals(5, grid.nodes().size());

        GridProjection prj = grid.forLocal();

        assertEquals(1, prj.nodes().size());
        assertEquals(locNode, F.first(prj.nodes()));

        prj = grid.forHost(locNode);
        assertEquals(grid.nodes().size(), prj.nodes().size());
        assertTrue(grid.nodes().containsAll(prj.nodes()));
        try {
            grid.forHost(null);
        }
        catch (NullPointerException ignored) {
            // No-op.
        }

        prj = grid.forNode(locNode);
        assertEquals(1, prj.nodes().size());

        prj = grid.forNode(locNode, locNode);
        assertEquals(1, prj.nodes().size());

        try {
            grid.forNode(null);
        }
        catch (NullPointerException ignored) {
            // No-op.
        }

        prj = grid.forNodes(F.asList(locNode));
        assertEquals(1, prj.nodes().size());

        prj = grid.forNodes(F.asList(locNode, locNode));
        assertEquals(1, prj.nodes().size());

        try {
            grid.forNodes(null);
        }
        catch (NullPointerException ignored) {
            // No-op.
        }

        prj = grid.forNodeId(locId);
        assertEquals(1, prj.nodes().size());

        prj = grid.forNodeId(locId, locId);
        assertEquals(1, prj.nodes().size());

        try {
            grid.forNodeId(null);
        }
        catch (NullPointerException ignored) {
            // No-op.
        }

        prj = grid.forNodeIds(F.asList(locId));
        assertEquals(1, prj.nodes().size());

        prj = grid.forNodeIds(F.asList(locId, locId));
        assertEquals(1, prj.nodes().size());

        try {
            grid.forNodeIds(null);
        }
        catch (NullPointerException ignored) {
            // No-op.
        }

        prj = grid.forOthers(locNode);

        assertEquals(4, prj.nodes().size());
        assertFalse(prj.nodes().contains(locNode));

        assertEquals(4, grid.forRemotes().nodes().size());
        assertTrue(prj.nodes().containsAll(grid.forRemotes().nodes()));

        try {
            grid.forOthers((GridNode)null);
        }
        catch (NullPointerException ignored) {
            // No-op.
        }
    }
}
