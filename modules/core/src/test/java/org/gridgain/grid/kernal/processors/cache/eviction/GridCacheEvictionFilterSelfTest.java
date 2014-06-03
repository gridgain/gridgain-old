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

package org.gridgain.grid.kernal.processors.cache.eviction;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.cache.affinity.consistenthash.*;
import org.gridgain.grid.cache.eviction.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.testframework.junits.common.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static org.gridgain.grid.cache.GridCacheAtomicityMode.*;
import static org.gridgain.grid.cache.GridCacheMode.*;
import static org.gridgain.grid.cache.GridCacheDistributionMode.*;
import static org.gridgain.grid.cache.GridCachePreloadMode.*;

/**
 * Base class for eviction tests.
 */
public class GridCacheEvictionFilterSelfTest extends GridCommonAbstractTest {
    /** IP finder. */
    private static final GridTcpDiscoveryIpFinder ipFinder = new GridTcpDiscoveryVmIpFinder(true);

    /** Replicated cache. */
    private GridCacheMode mode = REPLICATED;

    /** Near enabled flag. */
    private boolean nearEnabled;

    /** */
    private EvictionFilter filter;

    /** Policy. */
    private GridCacheEvictionPolicy<Object, Object> plc = new GridCacheEvictionPolicy<Object, Object>() {
        @Override public void onEntryAccessed(boolean rmv, GridCacheEntry entry) {
            assert !(entry.peek() instanceof Integer);
        }
    };

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration c = super.getConfiguration(gridName);

        GridCacheConfiguration cc = defaultCacheConfiguration();

        cc.setCacheMode(mode);
        cc.setDistributionMode(nearEnabled ? NEAR_PARTITIONED : PARTITIONED_ONLY);
        cc.setEvictionPolicy(plc);
        cc.setNearEvictionPolicy(plc);
        cc.setEvictSynchronized(false);
        cc.setEvictNearSynchronized(false);
        cc.setSwapEnabled(false);
        cc.setWriteSynchronizationMode(GridCacheWriteSynchronizationMode.FULL_SYNC);
        cc.setEvictionFilter(filter);
        cc.setPreloadMode(SYNC);
        cc.setAtomicityMode(TRANSACTIONAL);

        if (mode == PARTITIONED)
            cc.setBackups(1);

        c.setCacheConfiguration(cc);

        GridTcpDiscoverySpi disco = new GridTcpDiscoverySpi();

        disco.setIpFinder(ipFinder);

        c.setDiscoverySpi(disco);

        return c;
    }

    /** @throws Exception If failed. */
    public void testLocal() throws Exception {
        mode = LOCAL;

        checkEvictionFilter();
    }

    /** @throws Exception If failed. */
    public void testReplicated() throws Exception {
        mode = REPLICATED;

        checkEvictionFilter();
    }

    /** @throws Exception If failed. */
    public void testPartitioned() throws Exception {
        mode = PARTITIONED;
        nearEnabled = true;

        checkEvictionFilter();
    }

    /** @throws Exception If failed. */
    public void testPartitionedNearDisabled() throws Exception {
        mode = PARTITIONED;
        nearEnabled = false;

        checkEvictionFilter();
    }

    /** @throws Exception If failed. */
    @SuppressWarnings("BusyWait")
    private void checkEvictionFilter() throws Exception {
        filter = new EvictionFilter();

        startGridsMultiThreaded(2);

        try {
            Grid g = grid(0);

            GridCache<Object, Object> c = g.cache(null);

            int cnt = 1;

            for (int i = 0; i < cnt; i++)
                c.putx(i, i);

            Map<Object, AtomicInteger> cnts = filter.counts();

            int exp = mode == LOCAL ? 1 : mode == REPLICATED ? 2 : nearEnabled ? 3 : 2;

            for (int j = 0; j < 3; j++) {
                boolean success = true;

                for (int i = 0; i < cnt; i++) {
                    int cnt0 = cnts.get(i).get();

                    success = cnt0 == exp;

                    if (!success) {
                        U.warn(log, "Invalid count for key [key=" + i + ", cnt=" + cnt0 + ", expected=" + exp + ']');

                        break;
                    }
                    else
                        info("Correct count for key [key=" + i + ", cnt=" + cnt0 + ']');
                }

                if (success)
                    break;

                if (j < 2)
                    Thread.sleep(1000);
                else
                    assert false : "Test has not succeeded (see log for details).";
            }
        }
        finally {
            stopAllGrids();
        }
    }

    /**
     * This test case is just to visualize a support issue from client. It does not fail.
     *
     * @throws Exception If failed.
     */
    public void _testPartitionedMixed() throws Exception {
        mode = PARTITIONED;
        nearEnabled = false;

        filter = new EvictionFilter();

        Grid g = startGrid();

        GridCache<Object, Object> cache = g.cache(null);

        try {
            int id = 1;

            cache.putx(id++, 1);
            cache.putx(id++, 2);

            for (int i = id + 1; i < 10; i++) {
                cache.putx(id, id);

                cache.putx(i, String.valueOf(i));
            }

            info(">>>> " + cache.get(1));
            info(">>>> " + cache.get(2));
            info(">>>> " + cache.get(3));
        }
        finally {
            stopGrid();
        }
    }

    /**
     *
     */
    private final class EvictionFilter implements GridCacheEvictionFilter<Object, Object> {
        /** */
        private final ConcurrentMap<Object, AtomicInteger> cnts = new ConcurrentHashMap<>();

        /** {@inheritDoc} */
        @Override public boolean evictAllowed(GridCacheEntry<Object, Object> entry) {
            AtomicInteger i = cnts.get(entry.getKey());

            if (i == null) {
                AtomicInteger old = cnts.putIfAbsent(entry.getKey(), i = new AtomicInteger());

                if (old != null)
                    i = old;
            }

            i.incrementAndGet();

            String grid = entry.projection().gridProjection().grid().name();

            boolean ret = !(entry.peek() instanceof Integer);

            if (!ret)
                info(">>> Not evicting key [grid=" + grid + ", key=" + entry.getKey() + ", cnt=" + i.get() + ']');
            else
                info(">>> Evicting key [grid=" + grid + ", key=" + entry.getKey() + ", cnt=" + i.get() + ']');

            return ret;
        }

        /** @return Counts. */
        ConcurrentMap<Object, AtomicInteger> counts() {
            return cnts;
        }
    }
}
