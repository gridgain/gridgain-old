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
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.testframework.junits.common.*;

import static org.gridgain.grid.cache.GridCacheAtomicityMode.*;
import static org.gridgain.grid.cache.GridCacheMode.*;

/**
 * Tests for {@link GridCacheMvccManager}.
 */
public class GridCacheMvccManagerSelfTest extends GridCommonAbstractTest {
    /** VM ip finder for TCP discovery. */
    private static GridTcpDiscoveryIpFinder ipFinder = new GridTcpDiscoveryVmIpFinder(true);

    /** Cache mode. */
    private GridCacheMode mode;

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        GridTcpDiscoverySpi disco = new GridTcpDiscoverySpi();

        disco.setMaxMissedHeartbeats(Integer.MAX_VALUE);
        disco.setIpFinder(ipFinder);

        cfg.setDiscoverySpi(disco);
        cfg.setCacheConfiguration(cacheConfiguration());

        return cfg;
    }

    /** @return Cache configuration. */
    protected GridCacheConfiguration cacheConfiguration() {
        GridCacheConfiguration cfg = defaultCacheConfiguration();

        cfg.setCacheMode(mode);
        cfg.setWriteSynchronizationMode(GridCacheWriteSynchronizationMode.FULL_SYNC);
        cfg.setAtomicityMode(TRANSACTIONAL);

        return cfg;
    }

    /** @throws Exception If failed. */
    public void testLocalCache() throws Exception {
        mode = LOCAL;

        testCandidates(1);
    }

    /** @throws Exception If failed. */
    public void testReplicatedCache() throws Exception {
        mode = REPLICATED;

        testCandidates(3);
    }

    /** @throws Exception If failed. */
    public void testPartitionedCache() throws Exception {
        mode = PARTITIONED;

        testCandidates(3);
    }

    /**
     * @param gridCnt Grid count.
     * @throws Exception If failed.
     */
    private void testCandidates(int gridCnt) throws Exception {
        try {
            Grid grid = startGridsMultiThreaded(gridCnt);

            GridCache<Integer, Integer> cache = grid.cache(null);

            GridCacheTx tx = cache.txStart();

            cache.put(1, 1);

            tx.commit();

            for (int i = 0; i < gridCnt; i++) {
                assert ((GridKernal)grid(i)).internalCache().context().mvcc().localCandidates().isEmpty();
                assert ((GridKernal)grid(i)).internalCache().context().mvcc().remoteCandidates().isEmpty();
            }
        }
        finally {
            stopAllGrids();
        }
    }
}
