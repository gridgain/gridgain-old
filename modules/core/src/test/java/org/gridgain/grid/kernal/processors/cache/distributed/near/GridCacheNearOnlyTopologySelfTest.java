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
import org.gridgain.grid.cache.affinity.consistenthash.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.testframework.*;
import org.gridgain.testframework.junits.common.*;

import java.util.concurrent.*;

import static org.gridgain.grid.cache.GridCacheAtomicityMode.*;
import static org.gridgain.grid.cache.GridCacheMode.*;
import static org.gridgain.grid.cache.GridCacheDistributionMode.*;
import static org.gridgain.grid.cache.GridCachePreloadMode.*;
import static org.gridgain.grid.cache.GridCacheTxConcurrency.*;
import static org.gridgain.grid.cache.GridCacheTxIsolation.*;

/**
 * Near-only cache node startup test.
 */
public class GridCacheNearOnlyTopologySelfTest extends GridCommonAbstractTest {
    /** Shared ip finder. */
    private static final GridTcpDiscoveryIpFinder IP_FINDER = new GridTcpDiscoveryVmIpFinder(true);

    /** Near only flag. */
    private boolean nearOnly;

    /** Use cache flag. */
    private boolean cache = true;

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        if (cache) {
            GridCacheConfiguration cacheCfg = defaultCacheConfiguration();

            cacheCfg.setCacheMode(PARTITIONED);
            cacheCfg.setBackups(1);
            cacheCfg.setDistributionMode(nearOnly ? NEAR_ONLY : NEAR_PARTITIONED);
            cacheCfg.setPreloadMode(SYNC);
            cacheCfg.setAtomicityMode(TRANSACTIONAL);

            cfg.setCacheConfiguration(cacheCfg);
        }

        GridTcpDiscoverySpi discoSpi = new GridTcpDiscoverySpi();

        discoSpi.setIpFinder(IP_FINDER);

        cfg.setDiscoverySpi(discoSpi);

        return cfg;
    }

    /** @throws Exception If failed. */
    public void testStartupFirstOneNode() throws Exception {
        checkStartupNearNode(0, 2);
    }

    /** @throws Exception If failed. */
    public void testStartupLastOneNode() throws Exception {
        checkStartupNearNode(1, 2);
    }

    /** @throws Exception If failed. */
    public void testStartupFirstTwoNodes() throws Exception {
        checkStartupNearNode(0, 3);
    }

    /** @throws Exception If failed. */
    public void testStartupInMiddleTwoNodes() throws Exception {
        checkStartupNearNode(1, 3);
    }

    /** @throws Exception If failed. */
    public void testStartupLastTwoNodes() throws Exception {
        checkStartupNearNode(2, 3);
    }

    /** @throws Exception If failed. */
    public void testKeyMapping() throws Exception {
        try {
            cache = true;

            for (int i = 0; i < 4; i++) {
                nearOnly = i == 0;

                startGrid(i);
            }

            for (int i = 0; i < 100; i++)
                assertFalse("For key: " + i, grid(0).cache(null).affinity().isPrimaryOrBackup(grid(0).localNode(), i));
        }
        finally {
            stopAllGrids();
        }
    }

    /** @throws Exception If failed. */
    public void testKeyMappingOnComputeNode() throws Exception {
        try {
            cache = true;

            for (int i = 0; i < 4; i++) {
                nearOnly = i == 0;

                startGrid(i);
            }

            cache = false;

            Grid compute = startGrid(4);

            for (int i = 0; i < 100; i++) {
                GridNode node = compute.mapKeyToNode(null, i);

                assertFalse("For key: " + i, node.id().equals(compute.localNode().id()));
                assertFalse("For key: " + i, node.id().equals(grid(0).localNode().id()));
            }
        }
        finally {
            stopAllGrids();
        }
    }

    /** @throws Exception If failed. */
    public void testNodeLeave() throws Exception {
        try {
            cache = true;

            for (int i = 0; i < 2; i++) {
                nearOnly = i == 0;

                startGrid(i);
            }

            for (int i = 0; i < 10; i++)
                grid(1).cache(null).put(i, i);

            final GridCache<Object, Object> nearOnly = grid(0).cache(null);

            // Populate near cache.
            for (int i = 0; i < 10; i++) {
                assertEquals(i, nearOnly.get(i));
                assertEquals(i, nearOnly.peek(i));
            }

            // Stop the only dht node.
            stopGrid(1);

            for (int i = 0; i < 10; i++) {
                assertNull(nearOnly.peek(i));

                final int key = i;

                GridTestUtils.assertThrows(log, new Callable<Object>() {
                    @Override public Object call() throws Exception {
                        return nearOnly.get(key);
                    }
                }, GridTopologyException.class, null);
            }

            // Test optimistic transaction.
            GridTestUtils.assertThrows(log, new Callable<Object>() {
                @Override public Object call() throws Exception {
                    try (GridCacheTx tx = nearOnly.txStart(OPTIMISTIC, REPEATABLE_READ)) {
                        nearOnly.putx("key", "val");

                        tx.commit();
                    }

                    return null;
                }
            }, GridTopologyException.class, null);

            // Test pessimistic transaction.
            GridTestUtils.assertThrows(log, new Callable<Object>() {
                @Override public Object call() throws Exception {
                    try (GridCacheTx tx = nearOnly.txStart(PESSIMISTIC, REPEATABLE_READ)) {
                        nearOnly.put("key", "val");

                        tx.commit();
                    }

                    return null;
                }
            }, GridTopologyException.class, null);

        }
        finally {
            stopAllGrids();
        }
    }

    /** @throws Exception If failed. */
    private void checkStartupNearNode(int nearNodeIdx, int totalNodeCnt) throws Exception {
        try {
            cache = true;

            for (int i = 0; i < totalNodeCnt; i++) {
                nearOnly = nearNodeIdx == i;

                startGrid(i);
            }
        }
        finally {
            stopAllGrids();
        }
    }
}
