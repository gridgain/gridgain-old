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
import org.gridgain.grid.cache.store.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.marshaller.optimized.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.grid.util.lang.*;
import org.gridgain.testframework.*;
import org.gridgain.testframework.junits.common.*;
import org.jdk8.backport.*;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.concurrent.atomic.*;

import static org.gridgain.grid.cache.GridCacheAtomicityMode.*;
import static org.gridgain.grid.cache.GridCacheMode.*;
import static org.gridgain.grid.cache.GridCacheDistributionMode.*;
import static org.gridgain.grid.cache.GridCacheWriteSynchronizationMode.*;

/**
 * Abstract class for cache tests.
 */
public abstract class GridCacheAbstractSelfTest extends GridCommonAbstractTest {
    /** Test timeout */
    private static final long TEST_TIMEOUT = 30 * 1000;

    /** Store map. */
    protected static final Map<Object, Object> map = new ConcurrentHashMap8<>();

    /** VM ip finder for TCP discovery. */
    private static GridTcpDiscoveryIpFinder ipFinder = new GridTcpDiscoveryVmIpFinder(true);

    /**
     * @return Grids count to start.
     */
    protected abstract int gridCount();

    /** {@inheritDoc} */
    @Override protected long getTestTimeout() {
        return TEST_TIMEOUT;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        int cnt = gridCount();

        assert cnt >= 1 : "At least one grid must be started";

        startGridsMultiThreaded(cnt);
    }

    /** {@inheritDoc} */
    @Override protected void afterTestsStopped() throws Exception {
        stopAllGrids();

        map.clear();
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        assert cache().tx() == null;
        assert cache().isEmpty() : "Cache is not empty: " + cache().entrySet();
        assert cache().keySet().isEmpty() : "Key set is not empty: " + cache().keySet();
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        GridCacheTx tx = cache().tx();

        if (tx != null) {
            tx.close();

            fail("Cache transaction remained after test completion: " + tx);
        }

        for (int i = 0; i < gridCount(); i++) {
            while (true) {
                try {
                    final int fi = i;

                    assertTrue(
                        "Cache is not empty: " + cache(i).entrySet(),
                        GridTestUtils.waitForCondition(
                            // Preloading may happen as nodes leave, so we need to wait.
                            new GridAbsPredicateX() {
                                @Override public boolean applyx() throws GridException {
                                    GridCache<String, Integer> cache = cache(fi);

                                    if (txEnabled())
                                        cache.removeAll();
                                    else
                                        cache.clearAll();

                                    // clearAll() does not remove entries with readers.
                                    if (!cache.isEmpty() && !txEnabled() && CU.isNearEnabled(cache.configuration()))
                                        cache.removeAll();

                                    return cache.isEmpty();
                                }
                            },
                            getTestTimeout()));

                    int primaryKeySize = cache(i).primarySize();
                    int keySize = cache(i).size();
                    int size = cache(i).size();

                    info("Size after [idx=" + i +
                        ", size=" + size +
                        ", size=" + keySize +
                        ", primarySize=" + primaryKeySize +
                        ", keySet=" + cache(i).keySet() + ']');

                    assertEquals("Cache is not empty [idx=" + i + ", entrySet=" + cache(i).entrySet() + ']',
                        0, cache(i).size());

                    break;
                }
                catch (Exception e) {
                    if (X.hasCause(e, GridTopologyException.class)) {
                        info("Got topology exception while tear down (will retry in 1000ms).");

                        U.sleep(1000);
                    }
                    else
                        throw e;
                }
            }

            Iterator<Map.Entry<String, Integer>> it = cache(i).swapIterator();

            while (it.hasNext()) {
                Map.Entry<String, Integer> entry = it.next();

                cache(i).remove(entry.getKey());
            }
        }

        assert cache().tx() == null;
        assert cache().isEmpty() : "Cache is not empty: " + cache().entrySet();
        assert cache().size() == 0 : "Cache is not empty: " + cache().entrySet();
        assert cache().keySet().isEmpty() : "Key set is not empty: " + cache().keySet();

        resetStore();
    }

    /**
     * Cleans up cache store.
     */
    protected void resetStore() {
        map.clear();
    }

    /**
     * Put entry to cache store.
     *
     * @param key Key.
     * @param val Value.
     */
    protected void putToStore(Object key, Object val) {
        map.put(key, val);
    }

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        GridTcpDiscoverySpi disco = new GridTcpDiscoverySpi();

        disco.setMaxMissedHeartbeats(Integer.MAX_VALUE);

        disco.setIpFinder(ipFinder);

        if (isDebug())
            disco.setAckTimeout(Integer.MAX_VALUE);

        cfg.setDiscoverySpi(disco);

        cfg.setCacheConfiguration(cacheConfiguration(gridName));

        cfg.setMarshaller(new GridOptimizedMarshaller(false));

        return cfg;
    }

    /**
     * @param gridName Grid name.
     * @return Cache configuration.
     * @throws Exception In case of error.
     */
    protected GridCacheConfiguration cacheConfiguration(String gridName) throws Exception {
        GridCacheConfiguration cfg = defaultCacheConfiguration();

        cfg.setStore(cacheStore());
        cfg.setSwapEnabled(swapEnabled());
        cfg.setCacheMode(cacheMode());
        cfg.setAtomicityMode(atomicityMode());
        cfg.setWriteSynchronizationMode(writeSynchronization());
        cfg.setDistributionMode(distributionMode());

        if (cacheMode() == PARTITIONED)
            cfg.setBackups(1);

        return cfg;
    }

    /**
     * @return Default cache mode.
     */
    protected GridCacheMode cacheMode() {
        return GridCacheConfiguration.DFLT_CACHE_MODE;
    }

    /**
     * @return Cache atomicity mode.
     */
    protected GridCacheAtomicityMode atomicityMode() {
        return TRANSACTIONAL;
    }

    /**
     * @return Partitioned mode.
     */
    protected GridCacheDistributionMode distributionMode() {
        return NEAR_PARTITIONED;
    }

    /**
     * @return Write synchronization.
     */
    protected GridCacheWriteSynchronizationMode writeSynchronization() {
        return FULL_SYNC;
    }

    /**
     * @return Write through storage emulator.
     */
    protected GridCacheStore<?, ?> cacheStore() {
        return new GridCacheStoreAdapter<Object, Object>() {
            @Override public void loadCache(GridBiInClosure<Object, Object> clo,
                Object... args) {
                for (Map.Entry<Object, Object> e : map.entrySet())
                    clo.apply(e.getKey(), e.getValue());
            }

            @Override public Object load(GridCacheTx tx, Object key) {
                return map.get(key);
            }

            @Override public void put(GridCacheTx tx, Object key, @Nullable Object val) {
                map.put(key, val);
            }

            @Override public void remove(GridCacheTx tx, Object key) {
                map.remove(key);
            }
        };
    }

    /**
     * @return {@code true} if swap should be enabled.
     */
    protected boolean swapEnabled() {
        return true;
    }

    /**
     * @return {@code true} if near cache should be enabled.
     */
    protected boolean nearEnabled() {
        return distributionMode() == NEAR_ONLY || distributionMode() == NEAR_PARTITIONED;
    }

    /**
     * @return {@code True} if transactions are enabled.
     */
    protected boolean txEnabled() {
        return true;
    }

    /**
     * @return {@code True} if locking is enabled.
     */
    protected boolean lockingEnabled() {
        return true;
    }

    /**
     * @return {@code True} for partitioned caches.
     */
    protected final boolean partitionedMode() {
        return cacheMode() == PARTITIONED;
    }

    /**
     * @param idx Index of grid.
     * @return Cache instance casted to work with string and integer types for convenience.
     */
    @SuppressWarnings({"unchecked"})
    @Override protected GridCache<String, Integer> cache(int idx) {
        return grid(idx).cache(null);
    }

    /**
     * @return Default cache instance casted to work with string and integer types for convenience.
     */
    @SuppressWarnings({"unchecked"})
    @Override protected GridCache<String, Integer> cache() {
        return cache(0);
    }

    /**
     * @param idx Index of grid.
     * @return Cache context.
     */
    protected GridCacheContext<String, Integer> context(int idx) {
        return ((GridKernal)grid(idx)).<String, Integer>internalCache().context();
    }

    /**
     * @param key Key.
     * @param idx Node index.
     * @return {@code True} if key belongs to node with index idx.
     */
    protected boolean belongs(String key, int idx) {
        return context(idx).cache().affinity().isPrimaryOrBackup(context(idx).localNode(), key);
    }

    /**
     * Filters cache entry projections leaving only ones with keys containing 'key'.
     */
    protected static GridPredicate<GridCacheEntry<String, Integer>> entryKeyFilter =
        new P1<GridCacheEntry<String, Integer>>() {
        @Override public boolean apply(GridCacheEntry<String, Integer> entry) {
            return entry.getKey().contains("key");
        }
    };

    /**
     * Filters cache entry projections leaving only ones with keys not containing 'key'.
     */
    protected static GridPredicate<GridCacheEntry<String, Integer>> entryKeyFilterInv =
        new P1<GridCacheEntry<String, Integer>>() {
        @Override public boolean apply(GridCacheEntry<String, Integer> entry) {
            return !entry.getKey().contains("key");
        }
    };

    /**
     * Filters cache entry projections leaving only ones with values less than 50.
     */
    protected static final GridPredicate<GridCacheEntry<String, Integer>> lt50 =
        new P1<GridCacheEntry<String, Integer>>() {
            @Override public boolean apply(GridCacheEntry<String, Integer> entry) {
                Integer i = entry.peek();

                return i != null && i < 50;
            }
        };

    /**
     * Filters cache entry projections leaving only ones with values greater or equal than 100.
     */
    protected static final GridPredicate<GridCacheEntry<String, Integer>> gte100 =
        new P1<GridCacheEntry<String, Integer>>() {
            @Override public boolean apply(GridCacheEntry<String, Integer> entry) {
                Integer i = entry.peek();

                return i != null && i >= 100;
            }

            @Override public String toString() {
                return "gte100";
            }
        };

    /**
     * Filters cache entry projections leaving only ones with values greater or equal than 200.
     */
    protected static final GridPredicate<GridCacheEntry<String, Integer>> gte200 =
        new P1<GridCacheEntry<String, Integer>>() {
            @Override public boolean apply(GridCacheEntry<String, Integer> entry) {
                Integer i = entry.peek();

                return i != null && i >= 200;
            }

            @Override public String toString() {
                return "gte200";
            }
        };

    /**
     * {@link GridInClosure} for calculating sum.
     */
    @SuppressWarnings({"PublicConstructorInNonPublicClass"})
    protected static final class SumVisitor implements CI1<GridCacheEntry<String, Integer>> {
        /** */
        private final AtomicInteger sum;

        /**
         * @param sum {@link AtomicInteger} instance for accumulating sum.
         */
        public SumVisitor(AtomicInteger sum) {
            this.sum = sum;
        }

        /** {@inheritDoc} */
        @Override public void apply(GridCacheEntry<String, Integer> entry) {
            if (entry.getValue() != null) {
                Integer i = entry.getValue();

                assert i != null : "Value cannot be null for entry: " + entry;

                sum.addAndGet(i);
            }
        }
    }

    /**
     * {@link GridReducer} for calculating sum.
     */
    @SuppressWarnings({"PublicConstructorInNonPublicClass"})
    protected static final class SumReducer implements R1<GridCacheEntry<String, Integer>, Integer> {
        /** */
        private int sum;

        /** */
        public SumReducer() {
            // no-op
        }

        /** {@inheritDoc} */
        @Override public boolean collect(GridCacheEntry<String, Integer> entry) {
            if (entry.getValue() != null) {
                Integer i = entry.getValue();

                assert i != null;

                sum += i;
            }

            return true;
        }

        /** {@inheritDoc} */
        @Override public Integer reduce() {
            return sum;
        }
    }
}
