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

package org.gridgain.grid.kernal.processors.cache.distributed.dht;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.cache.affinity.consistenthash.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.grid.spi.swapspace.file.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.testframework.junits.common.*;

import static org.gridgain.grid.cache.GridCacheAtomicityMode.*;
import static org.gridgain.grid.cache.GridCacheMode.*;
import static org.gridgain.grid.cache.GridCacheDistributionMode.*;
import static org.gridgain.grid.cache.GridCacheTxIsolation.*;
import static org.gridgain.grid.cache.GridCacheTxConcurrency.*;
import static org.gridgain.grid.cache.GridCacheWriteSynchronizationMode.*;

/**
 * Test ensuring that values are visible inside OPTIMISTIC transaction in co-located cache.
 */
public class GridCacheColocatedOptimisticTransactionSelfTest extends GridCommonAbstractTest {
    /** Grid count. */
    private static final int GRID_CNT = 3;

    /** Cache name. */
    private static final String CACHE = "cache";

    /** Key. */
    private static final Integer KEY = 1;

    /** Value. */
    private static final String VAL = "val";

    /** Shared IP finder. */
    private static final GridTcpDiscoveryIpFinder IP_FINDER = new GridTcpDiscoveryVmIpFinder(true);

    /** Grids. */
    private static Grid[] grids;

    /** Regular caches. */
    private static GridCache<Integer, String>[] caches;

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration c = super.getConfiguration(gridName);

        GridTcpDiscoverySpi disco = new GridTcpDiscoverySpi();

        disco.setIpFinder(IP_FINDER);

        GridCacheConfiguration cc = new GridCacheConfiguration();

        cc.setName(CACHE);
        cc.setCacheMode(PARTITIONED);
        cc.setAtomicityMode(TRANSACTIONAL);
        cc.setDistributionMode(PARTITIONED_ONLY);
        cc.setBackups(1);
        cc.setWriteSynchronizationMode(FULL_SYNC);
        cc.setTxSerializableEnabled(true);
        cc.setSwapEnabled(true);
        cc.setEvictSynchronized(false);
        cc.setEvictNearSynchronized(false);

        c.setDiscoverySpi(disco);
        c.setCacheConfiguration(cc);
        c.setSwapSpaceSpi(new GridFileSwapSpaceSpi());

        return c;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override protected void beforeTest() throws Exception {
        grids = new Grid[GRID_CNT];
        caches = new GridCache[GRID_CNT];

        for (int i = 0; i < GRID_CNT; i++) {
            grids[i] = startGrid(i);

            caches[i] = grids[i].cache(CACHE);
        }
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        stopAllGrids();

        caches = null;
        grids = null;
    }

    /**
     * Perform test.
     *
     * @throws Exception If failed.
     */
    public void testOptimisticTransaction() throws Exception {
        for (GridCache<Integer, String> cache : caches) {
            GridCacheTx tx = cache.txStart(OPTIMISTIC, REPEATABLE_READ);

            try {
                cache.put(KEY, VAL);

                tx.commit();
            }
            finally {
                tx.close();
            }

            for (GridCache<Integer, String> cacheInner : caches) {
                tx = cacheInner.txStart(OPTIMISTIC, REPEATABLE_READ);

                try {
                    assert F.eq(VAL, cacheInner.get(KEY));

                    tx.commit();
                }
                finally {
                    tx.close();
                }
            }

            tx = cache.txStart(OPTIMISTIC, REPEATABLE_READ);

            try {
                cache.remove(KEY);

                tx.commit();
            }
            finally {
                tx.close();
            }
        }
    }
}
