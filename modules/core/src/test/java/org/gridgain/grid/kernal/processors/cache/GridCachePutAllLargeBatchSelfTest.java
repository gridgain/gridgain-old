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
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.kernal.processors.cache.distributed.dht.*;
import org.gridgain.grid.kernal.processors.cache.distributed.near.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.testframework.junits.common.*;

import java.util.*;

import static org.gridgain.grid.cache.GridCacheDistributionMode.*;
import static org.gridgain.grid.cache.GridCacheMode.*;
import static org.gridgain.grid.cache.GridCacheTxConcurrency.*;
import static org.gridgain.grid.cache.GridCacheTxIsolation.*;

/**
 * Tests putAll method with large number of keys.
 */
public class GridCachePutAllLargeBatchSelfTest extends GridCommonAbstractTest {
    /** Grid count. */
    private static final int GRID_CNT = 4;

    /** */
    private boolean nearEnabled;

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        cfg.setCacheConfiguration(cacheConfiguration(gridName));

        return cfg;
    }

    /**
     * @param gridName Grid name.
     * @return Test cache configuration.
     */
    public GridCacheConfiguration cacheConfiguration(String gridName) {
        GridCacheConfiguration ccfg = defaultCacheConfiguration();

        ccfg.setAtomicityMode(GridCacheAtomicityMode.TRANSACTIONAL);
        ccfg.setBackups(2);
        ccfg.setDistributionMode(nearEnabled ? NEAR_PARTITIONED : PARTITIONED_ONLY);
        ccfg.setCacheMode(PARTITIONED);

        return ccfg;
    }

    /**
     * @throws Exception If failed.
     */
    public void testPutAllPartitioned() throws Exception {
        checkPutAll(false);
    }

    /**
     * @throws Exception If failed.
     */
    public void testPutAllNear() throws Exception {
        checkPutAll(true);
    }

    /**
     * @throws Exception If failed.
     */
    private void checkPutAll(boolean nearEnabled) throws Exception {
        this.nearEnabled = nearEnabled;

        startGrids(GRID_CNT);

        awaitPartitionMapExchange();

        try {
            GridCache<Object, Object> cache = grid(0).cache(null);

            int keyCnt = 200;

            for (int i = 0; i < keyCnt; i++)
                cache.put(i, i);

            // Create readers if near cache is enabled.
            for (int g = 1; g < 2; g++) {
                for (int i = 30; i < 70; i++)
                    grid(g).cache(null).get(i);
            }

            try (GridCacheTx tx = cache.txStart(PESSIMISTIC, REPEATABLE_READ)) {
                Map<Integer, Integer> map = new LinkedHashMap<>();

                for (int i = 0; i < keyCnt; i++)
                    map.put(i, i * i);

                cache.getAll(map.keySet());

                cache.putAll(map);

                tx.commit();
            }

            //  Check that no stale transactions left and all locks are released.
            for (int g = 0; g < GRID_CNT; g++) {
                GridKernal k = (GridKernal)grid(g);

                GridCacheAdapter<Object, Object> cacheAdapter = k.context().cache().internalCache();

                assertEquals(0, cacheAdapter.context().tm().idMapSize());

                for (int i = 0; i < keyCnt; i++) {
                    if (cacheAdapter.isNear()) {
                        GridDhtCacheEntry<Object, Object> entry = ((GridNearCacheAdapter<Object, Object>)cacheAdapter)
                            .dht().peekExx(i);

                        if (entry != null) {
                            assertFalse(entry.lockedByAny());
                            assertTrue(entry.localCandidates().isEmpty());
                            assertTrue(entry.remoteMvccSnapshot().isEmpty());
                        }
                    }

                    GridCacheEntryEx<Object, Object> entry = cacheAdapter.peekEx(i);

                    if (entry != null) {
                        assertFalse(entry.lockedByAny());
                        assertTrue(entry.localCandidates().isEmpty());
                        assertTrue(entry.remoteMvccSnapshot().isEmpty());
                    }
                }
            }

            for (int g = 0; g < GRID_CNT; g++) {
                GridCache<Object, Object> checkCache = grid(g).cache(null);

                GridNode checkNode = grid(g).localNode();

                for (int i = 0; i < keyCnt; i++) {
                    if (checkCache.affinity().isPrimaryOrBackup(checkNode, i))
                        assertEquals(i * i, checkCache.peek(i, F.asList(GridCachePeekMode.PARTITIONED_ONLY)));
                }
            }
        }
        finally {
            stopAllGrids();
        }
    }
}
