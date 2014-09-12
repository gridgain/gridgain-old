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
import org.gridgain.grid.util.typedef.*;
import org.gridgain.testframework.junits.common.*;

import java.util.*;

import static org.gridgain.grid.cache.GridCacheAtomicityMode.*;
import static org.gridgain.grid.cache.GridCacheDistributionMode.*;
import static org.gridgain.grid.cache.GridCacheMode.*;
import static org.gridgain.grid.cache.GridCacheWriteSynchronizationMode.*;

/**
 * We have three nodes - A, B and C - and start them in that order. Each node contains NEAR_PARTITIONED transactional
 * cache. Then we immediately put a key which is primary for A, near for B and backup for C. Once key is put, we
 * read it on B. Finally the key is updated again and we ensure that it was updated on the near node B as well. I.e.
 * with this test we ensures that node B is considered as near reader for that key in case put occurred during preload.
 */
public class GridCacheNearReaderPreloadSelfTest extends GridCommonAbstractTest {
    /** Test iterations count. */
    private static final int REPEAT_CNT = 10;

    /** Amopunt of updates on each test iteration. */
    private static final int PUT_CNT = 100;

    /** Cache name. */
    private static final String CACHE_NAME = "cache";

    /** Cache on primary node. */
    private GridCache<Integer, Integer> cache1;

    /** Cache on near node. */
    private GridCache<Integer, Integer> cache2;

    /** Cache on backup node. */
    private GridCache<Integer, Integer> cache3;

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        cache1 = null;
        cache2 = null;
        cache3 = null;

        stopAllGrids(true);
    }

    /**
     * Test.
     *
     * @throws Exception If failed.
     */
    public void testNearReaderPreload() throws Exception {
        for (int i = 0; i < REPEAT_CNT; i++) {
            startUp();

            int key = key();

            for (int j = 0; j < PUT_CNT; j++) {
                cache1.put(key, j);

                checkCaches(key, j);
            }

            stopAllGrids(true);
        }
    }

    /**
     * Startup routine.
     *
     * @throws Exception If failed.
     */
    private void startUp() throws Exception {
        GridTcpDiscoveryIpFinder ipFinder = new GridTcpDiscoveryVmIpFinder(true);

        Grid node1 = G.start(dataNode(ipFinder, "node1"));
        Grid node2 = G.start(dataNode(ipFinder, "node2"));
        Grid node3 = G.start(dataNode(ipFinder, "node3"));

        info("Node 1: " + node1.localNode().id());
        info("Node 2: " + node2.localNode().id());
        info("Node 3: " + node3.localNode().id());

        cache1 = node1.cache(CACHE_NAME);
        cache2 = node2.cache(CACHE_NAME);
        cache3 = node3.cache(CACHE_NAME);
    }

    /**
     * Create configuration for data node.
     *
     * @param ipFinder IP finder.
     * @param gridName Grid name.
     * @return Configuration for data node.
     * @throws GridException If failed.
     */
    private GridConfiguration dataNode(GridTcpDiscoveryIpFinder ipFinder, String gridName)
        throws Exception {
        GridCacheConfiguration ccfg = new GridCacheConfiguration();

        ccfg.setName(CACHE_NAME);
        ccfg.setCacheMode(PARTITIONED);
        ccfg.setAtomicityMode(TRANSACTIONAL);
        ccfg.setDistributionMode(NEAR_PARTITIONED);
        ccfg.setWriteSynchronizationMode(FULL_SYNC);
        ccfg.setBackups(1);

        GridConfiguration cfg = getConfiguration(gridName);

        GridTcpDiscoverySpi spi = new GridTcpDiscoverySpi();

        spi.setIpFinder(ipFinder);

        cfg.setLocalHost("127.0.0.1");
        cfg.setDiscoverySpi(spi);
        cfg.setCacheConfiguration(ccfg);
        cfg.setIncludeProperties();
        cfg.setRestEnabled(false);

        return cfg;
    }

    /**
     * Get key which will be primary for the first node and backup for the third node.
     *
     * @return Key.
     */
    private Integer key() {
        int key = 0;

        while (true) {
            Collection<GridNode> affNodes = cache1.affinity().mapKeyToPrimaryAndBackups(key);

            assert !F.isEmpty(affNodes);

            GridNode primaryNode = F.first(affNodes);

            if (F.eq(primaryNode, cache1.gridProjection().grid().localNode()) &&
                affNodes.contains(cache3.gridProjection().grid().localNode()))
                break;

            key++;
        }

        return key;
    }

    /**
     * Check whether all caches contains expected value for the given key.
     *
     * @param key Key.
     * @param expVal Expected value.
     * @throws Exception If failed.
     */
    private void checkCaches(int key, int expVal) throws Exception {
        checkCache(cache1, key, expVal);
        checkCache(cache2, key, expVal);
        checkCache(cache3, key, expVal);
    }

    /**
     * Check whether provided cache contains expected value for the given key.
     *
     * @param cache Cache.
     * @param key Key.
     * @param expVal Expected value.
     * @throws Exception If failed.
     */
    private void checkCache(GridCacheProjection<Integer, Integer> cache, int key, int expVal) throws Exception {
        GridCacheEntry<Integer, Integer> entry = cache.entry(key);

        assert F.eq(expVal, entry.getValue()) : "Unexpected cache value [key=" + key + ", expected=" + expVal +
            ", actual=" + entry.getValue() + ", primary=" + entry.primary() + ", backup=" + entry.backup() + ']';
    }
}
