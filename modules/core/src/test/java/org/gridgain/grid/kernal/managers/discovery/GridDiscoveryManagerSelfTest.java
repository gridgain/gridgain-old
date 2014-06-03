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

package org.gridgain.grid.kernal.managers.discovery;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.testframework.junits.common.*;

import static org.gridgain.grid.cache.GridCacheMode.*;
import static org.gridgain.grid.cache.GridCacheDistributionMode.*;

/**
 *
 */
public class GridDiscoveryManagerSelfTest extends GridCommonAbstractTest {
    /** */
    private static final String CACHE_NAME = "cache";

    /** */
    private static final GridTcpDiscoveryIpFinder IP_FINDER = new GridTcpDiscoveryVmIpFinder(true);

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        stopAllGrids();
    }

    /** {@inheritDoc} */
    @SuppressWarnings("IfMayBeConditional")
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        GridTcpDiscoverySpi disc = new GridTcpDiscoverySpi();

        disc.setIpFinder(IP_FINDER);

        cfg.setDiscoverySpi(disc);

        GridCacheConfiguration ccfg1 = defaultCacheConfiguration();

        ccfg1.setName(CACHE_NAME);

        GridCacheConfiguration ccfg2 = defaultCacheConfiguration();

        ccfg2.setName(null);

        GridCacheDistributionMode distrMode;

        if (gridName.equals(getTestGridName(1)))
            distrMode = NEAR_ONLY;
        else if (gridName.equals(getTestGridName(2)))
            distrMode = NEAR_PARTITIONED;
        else
            distrMode = PARTITIONED_ONLY;

        ccfg1.setCacheMode(PARTITIONED);
        ccfg2.setCacheMode(PARTITIONED);

        ccfg1.setDistributionMode(distrMode);
        ccfg2.setDistributionMode(distrMode);

        cfg.setCacheConfiguration(ccfg1, ccfg2);

        return cfg;
    }

    /**
     * @throws Exception If failed.
     */
    public void testHasNearCache() throws Exception {
        GridKernal g0 = (GridKernal)startGrid(0); // PARTITIONED_ONLY cache.

        assertFalse(g0.context().discovery().hasNearCache(CACHE_NAME, 0));
        assertFalse(g0.context().discovery().hasNearCache(null, 0));

        assertFalse(g0.context().discovery().hasNearCache(CACHE_NAME, 1));
        assertFalse(g0.context().discovery().hasNearCache(null, 1));

        GridKernal g1 = (GridKernal)startGrid(1); // NEAR_ONLY cache.

        assertFalse(g0.context().discovery().hasNearCache(CACHE_NAME, 1));
        assertTrue(g0.context().discovery().hasNearCache(CACHE_NAME, 2));
        assertFalse(g0.context().discovery().hasNearCache(null, 1));
        assertTrue(g0.context().discovery().hasNearCache(null, 2));

        assertTrue(g1.context().discovery().hasNearCache(CACHE_NAME, 2));
        assertTrue(g1.context().discovery().hasNearCache(null, 2));

        GridKernal g2 = (GridKernal)startGrid(2); // NEAR_PARTITIONED cache.

        assertFalse(g0.context().discovery().hasNearCache(CACHE_NAME, 1));
        assertTrue(g0.context().discovery().hasNearCache(CACHE_NAME, 2));
        assertTrue(g0.context().discovery().hasNearCache(CACHE_NAME, 3));
        assertFalse(g0.context().discovery().hasNearCache(null, 1));
        assertTrue(g0.context().discovery().hasNearCache(null, 2));
        assertTrue(g0.context().discovery().hasNearCache(null, 3));

        assertTrue(g1.context().discovery().hasNearCache(CACHE_NAME, 2));
        assertTrue(g1.context().discovery().hasNearCache(CACHE_NAME, 3));
        assertTrue(g1.context().discovery().hasNearCache(null, 2));
        assertTrue(g1.context().discovery().hasNearCache(null, 3));

        assertTrue(g2.context().discovery().hasNearCache(CACHE_NAME, 3));
        assertTrue(g2.context().discovery().hasNearCache(null, 3));

        stopGrid(1);

        assertFalse(g0.context().discovery().hasNearCache(CACHE_NAME, 1));
        assertTrue(g0.context().discovery().hasNearCache(CACHE_NAME, 2));
        assertTrue(g0.context().discovery().hasNearCache(CACHE_NAME, 3));
        assertTrue(g0.context().discovery().hasNearCache(CACHE_NAME, 4));
        assertFalse(g0.context().discovery().hasNearCache(null, 1));
        assertTrue(g0.context().discovery().hasNearCache(null, 2));
        assertTrue(g0.context().discovery().hasNearCache(null, 3));
        assertTrue(g0.context().discovery().hasNearCache(null, 4));

        assertTrue(g2.context().discovery().hasNearCache(CACHE_NAME, 3));
        assertTrue(g2.context().discovery().hasNearCache(CACHE_NAME, 4));
        assertTrue(g2.context().discovery().hasNearCache(null, 3));
        assertTrue(g2.context().discovery().hasNearCache(null, 4));

        stopGrid(2);

        assertFalse(g0.context().discovery().hasNearCache(CACHE_NAME, 1));
        assertTrue(g0.context().discovery().hasNearCache(CACHE_NAME, 2));
        assertTrue(g0.context().discovery().hasNearCache(CACHE_NAME, 3));
        assertTrue(g0.context().discovery().hasNearCache(CACHE_NAME, 4));
        assertFalse(g0.context().discovery().hasNearCache(CACHE_NAME, 5));

        assertFalse(g0.context().discovery().hasNearCache(null, 1));
        assertTrue(g0.context().discovery().hasNearCache(null, 2));
        assertTrue(g0.context().discovery().hasNearCache(null, 3));
        assertTrue(g0.context().discovery().hasNearCache(null, 4));
        assertFalse(g0.context().discovery().hasNearCache(null, 5));
    }
}
