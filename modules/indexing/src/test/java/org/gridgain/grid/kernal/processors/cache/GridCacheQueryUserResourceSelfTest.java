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

package org.gridgain.grid.kernal.processors.cache;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.cache.query.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.spi.discovery.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.testframework.*;
import org.gridgain.testframework.junits.common.*;

import java.util.*;

import static org.gridgain.grid.cache.GridCacheMode.*;

/**
 * Test runs cache queries with injected user resource.
 */
public class GridCacheQueryUserResourceSelfTest extends GridCommonAbstractTest {
    /** Deploy counter key. */
    private static final String DEPLOY_CNT_KEY = "deployCnt";

    /** Undeploy counter key. */
    private static final String UNDEPLOY_CNT_KEY = "undeployCnt";

    /** Grid count. */
    private static final int GRID_CNT = 2;

    /** Node run count. */
    private static final int RUN_CNT = 2;

    /** VM ip finder for TCP discovery. */
    private static final GridTcpDiscoveryIpFinder IP_FINDER = new GridTcpDiscoveryVmIpFinder(true);

    /** External class loader. */
    private static final ClassLoader extClsLdr;

    /**
     * Initialize external class loader.
     */
    static {
        extClsLdr = getExternalClassLoader();
    }

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        cfg.setDiscoverySpi(discoverySpi());
        cfg.setCacheConfiguration(cacheConfiguration());

        return cfg;
    }

    /**
     * @return Discovery SPI.
     * @throws Exception In case of error.
     */
    private GridDiscoverySpi discoverySpi() throws Exception {
        GridTcpDiscoverySpi spi = new GridTcpDiscoverySpi();

        spi.setMaxMissedHeartbeats(Integer.MAX_VALUE);
        spi.setIpFinder(IP_FINDER);

        return spi;
    }

    /**
     * @return Cache configuration.
     * @throws Exception In case of error.
     */
    private GridCacheConfiguration cacheConfiguration() throws Exception {
        GridCacheConfiguration cfg = defaultCacheConfiguration();

        cfg.setCacheMode(PARTITIONED);
        cfg.setBackups(1);

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected long getTestTimeout() {
        return 30 * 1000; // 30 seconds.
    }

    /**
     * Checks that user resource in cache query reducer is
     * deployed and undeployed correctly.
     *
     * @throws Exception If failed.
     */
    public void testCacheQueryUserResourceDeployment() throws Exception {
        // Start secondary nodes.
        for (int i = 0; i < GRID_CNT; i++)
            startGrid(i);

        try {
            for (int i = 1; i <= RUN_CNT; i++) {
                // Start primary node.
                Grid g = startGrid();

                try {
                    runQuery(g);
                }
                finally {
                    // Stop primary node.
                    stopGrid();
                }

                final int fi = i;

                assertTrue(GridTestUtils.waitForCondition(new PA() {
                    @Override public boolean apply() {
                        for (int i = 0; i < GRID_CNT; i++) {
                            Grid g = grid(i);

                            GridNodeLocalMap<String, Integer> nodeLoc = g.nodeLocalMap();

                            Integer depCnt = nodeLoc.get(DEPLOY_CNT_KEY);
                            Integer undepCnt = nodeLoc.get(UNDEPLOY_CNT_KEY);

                            if (depCnt == null || depCnt != fi)
                                return false;

                            if (undepCnt == null || undepCnt != fi)
                                return false;
                        }

                        return true;
                    }
                }, getTestTimeout()));
            }
        }
        finally {
            // Stop secondary nodes.
            for (int i = 0; i < GRID_CNT; i++)
                stopGrid(i);
        }
    }

    /**
     * Runs {@code SCAN} query with user resource injected into reducer.
     *
     * @param g Grid.
     * @throws Exception In case of error.
     */
    @SuppressWarnings("ConstantConditions")
    private void runQuery(Grid g) throws Exception {
        GridCacheQuery<Map.Entry<Integer, Integer>> q = g.<Integer, Integer>cache(null).queries().createScanQuery(null);

        // We use external class loader here to guarantee that secondary nodes
        // won't load the reducer and user resource from each other.
        Class<?> redCls = extClsLdr.loadClass("org.gridgain.grid.tests.p2p.GridExternalCacheQueryReducerClosure");

        q.projection(g.forRemotes()).execute((GridReducer<Map.Entry<Integer, Integer>, Integer>)redCls.newInstance()).
            get();
    }
}
