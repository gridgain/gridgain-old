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
import org.gridgain.grid.cache.eviction.lru.*;
import org.gridgain.grid.cache.query.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.kernal.processors.cache.query.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.marshaller.optimized.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.grid.spi.indexing.h2.*;
import org.gridgain.grid.spi.swapspace.file.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.testframework.junits.common.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import static org.gridgain.grid.cache.GridCacheAtomicityMode.*;
import static org.gridgain.grid.cache.GridCacheMode.*;

/**
 * Multi-threaded tests for cache queries.
 */
@SuppressWarnings("StatementWithEmptyBody")
public class GridCacheQueryMultiThreadedSelfTest extends GridCommonAbstractTest {
    /** */
    private static final boolean TEST_INFO = true;

    /** Number of test grids (nodes). Should not be less than 2. */
    private static final int GRID_CNT = 2;

    /** */
    private static GridTcpDiscoveryIpFinder ipFinder = new GridTcpDiscoveryVmIpFinder(true);

    /** */
    private static AtomicInteger idxSwapCnt = new AtomicInteger();

    /** */
    private static AtomicInteger idxUnswapCnt = new AtomicInteger();

    /** */
    private static final long DURATION = 30 * 1000;

    /** Don't start grid by default. */
    public GridCacheQueryMultiThreadedSelfTest() {
        super(false);
    }

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        GridTcpDiscoverySpi disco = new GridTcpDiscoverySpi();

        disco.setIpFinder(ipFinder);

        cfg.setDiscoverySpi(disco);

        cfg.setSwapSpaceSpi(new GridFileSwapSpaceSpi());
        cfg.setMarshaller(new GridOptimizedMarshaller(false));

        GridCacheConfiguration cacheCfg = defaultCacheConfiguration();

        cacheCfg.setCacheMode(PARTITIONED);
        cacheCfg.setAtomicityMode(TRANSACTIONAL);
        cacheCfg.setDistributionMode(GridCacheDistributionMode.NEAR_PARTITIONED);
        cacheCfg.setWriteSynchronizationMode(GridCacheWriteSynchronizationMode.FULL_SYNC);
        cacheCfg.setSwapEnabled(true);
        cacheCfg.setBackups(1);
        cacheCfg.setEvictionPolicy(evictsEnabled() ? new GridCacheLruEvictionPolicy(100) : null);

        if (offheapEnabled() && evictsEnabled())
            cacheCfg.setOffHeapMaxMemory(1000); // Small offheap for evictions.

        cfg.setCacheConfiguration(cacheCfg);

        GridH2IndexingSpi indexing = new GridH2IndexingSpi() {
            @Override public <K> void onSwap(@Nullable String spaceName, String swapSpaceName, K key)
                throws GridSpiException {
                super.onSwap(spaceName, swapSpaceName, key);

                idxSwapCnt.incrementAndGet();
            }

            @Override public <K, V> void onUnswap(@Nullable String spaceName, K key, V val, byte[] valBytes)
                throws GridSpiException {
                super.onUnswap(spaceName, key, val, valBytes);

                idxUnswapCnt.incrementAndGet();
            }
        };

        indexing.setDefaultIndexPrimitiveKey(true);
        indexing.setMaxOffheapRowsCacheSize(128);

        if (offheapEnabled())
            indexing.setMaxOffHeapMemory(0);

        cfg.setIndexingSpi(indexing);

        return cfg;
    }

    /** @return {@code true} If offheap enabled. */
    protected boolean offheapEnabled() {
        return false;
    }

    /** @return {@code true} If evictions enabled. */
    protected boolean evictsEnabled() {
        return true;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        super.beforeTest();

        // Clean up all caches.
        for (int i = 0; i < GRID_CNT; i++) {
            GridCache<Object, Object> c = grid(i).cache(null);

            assertEquals(0, c.size());
        }
    }

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        assert GRID_CNT >= 2 : "Constant GRID_CNT must be greater than or equal to 2.";

        startGridsMultiThreaded(GRID_CNT);
    }

    /** {@inheritDoc} */
    @Override protected void afterTestsStopped() throws Exception {
        stopAllGrids();

        if (evictsEnabled()) {
            assertTrue(idxSwapCnt.get() > 0);
            assertTrue(idxUnswapCnt.get() > 0);
        }
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        super.afterTest();

        // Clean up all caches.
        for (int i = 0; i < GRID_CNT; i++) {
            GridCache<Object, Object> c = grid(i).cache(null);

            c.removeAll(F.<GridCacheEntry<Object, Object>>alwaysTrue());

            Iterator<Map.Entry<Object, Object>> it = c.swapIterator();

            while (it.hasNext()) {
                it.next();

                it.remove();
            }

            it = c.offHeapIterator();

            while (it.hasNext()) {
                it.next();

                it.remove();
            }

            assertEquals("Swap keys: " + c.swapKeys(), 0, c.swapKeys());
            assertEquals(0, c.offHeapEntriesCount());
            assertEquals(0, c.size());
        }
    }

    /** {@inheritDoc} */
    @Override protected void info(String msg) {
        if (TEST_INFO)
            super.info(msg);
    }

    /**
     * @param entries Entries.
     * @param g Grid.
     * @return Affinity nodes.
     */
    private Set<UUID> affinityNodes(Iterable<Map.Entry<Integer, Integer>> entries, Grid g) {
        Set<UUID> nodes = new HashSet<>();

        for (Map.Entry<Integer, Integer> entry : entries)
            nodes.add(g.cache(null).affinity().mapKeyToPrimaryAndBackups(entry.getKey()).iterator().next().id());

        return nodes;
    }

    /**
     * JUnit.
     *
     * @throws Exception If failed.
     */
    @SuppressWarnings({"TooBroadScope"})
    public void testMultiThreadedSwapUnswapString() throws Exception {
        int threadCnt = 150;
        final int keyCnt = 2000;
        final int valCnt = 10000;

        final Grid g = grid(0);

        // Put test values into cache.
        final GridCache<Integer, String> c = g.cache(null);

        assertEquals(0, g.cache(null).size());
        assertEquals(0, c.queries().createSqlQuery(String.class, "1 = 1").execute().get().size());
        assertEquals(0, c.queries().createSqlQuery(Long.class, "1 = 1").execute().get().size());

        Random rnd = new Random();

        for (int i = 0; i < keyCnt; i += 1 + rnd.nextInt(3)) {
            c.putx(i, String.valueOf(rnd.nextInt(valCnt)));

            if (evictsEnabled() && rnd.nextBoolean())
                assertTrue(c.evict(i));
        }

        final AtomicBoolean done = new AtomicBoolean();

        GridFuture<?> fut = multithreadedAsync(new CAX() {
            @Override public void applyx() throws GridException {
                Random rnd = new Random();

                while (!done.get()) {
                    switch (rnd.nextInt(5)) {
                        case 0:
                            c.putx(rnd.nextInt(keyCnt), String.valueOf(rnd.nextInt(valCnt)));

                            break;
                        case 1:
                            if (evictsEnabled())
                                c.evict(rnd.nextInt(keyCnt));

                            break;
                        case 2:
                            c.remove(rnd.nextInt(keyCnt));

                            break;
                        case 3:
                            c.get(rnd.nextInt(keyCnt));

                            break;
                        case 4:
                            GridCacheQuery<Map.Entry<Integer, String>> qry = c.queries().createSqlQuery(
                                String.class, "_val between ? and ?");

                            int from = rnd.nextInt(valCnt);

                            GridCacheQueryFuture<Map.Entry<Integer, String>> fut =
                                qry.execute(String.valueOf(from), String.valueOf(from + 250));

                            Collection<Map.Entry<Integer, String>> res = fut.get();

                            for (Map.Entry<Integer, String> ignored : res) {
                                //No-op.
                            }
                    }
                }
            }
        }, threadCnt);

        Thread.sleep(DURATION);

        done.set(true);

        fut.get();
    }

    /**
     * JUnit.
     *
     * @throws Exception If failed.
     */
    @SuppressWarnings({"TooBroadScope"})
    public void testMultiThreadedSwapUnswapLong() throws Exception {
        int threadCnt = 150;
        final int keyCnt = 2000;
        final int valCnt = 10000;

        final Grid g = grid(0);

        // Put test values into cache.
        final GridCache<Integer, Long> c = g.cache(null);

        assertEquals(0, g.cache(null).size());
        assertEquals(0, c.queries().createSqlQuery(String.class, "1 = 1").execute().get().size());
        assertEquals(0, c.queries().createSqlQuery(Long.class, "1 = 1").execute().get().size());

        Random rnd = new Random();

        for (int i = 0; i < keyCnt; i += 1 + rnd.nextInt(3)) {
            c.putx(i, (long)rnd.nextInt(valCnt));

            if (evictsEnabled() && rnd.nextBoolean())
                assertTrue(c.evict(i));
        }

        final AtomicBoolean done = new AtomicBoolean();

        GridFuture<?> fut = multithreadedAsync(new CAX() {
            @Override public void applyx() throws GridException {
                Random rnd = new Random();

                while (!done.get()) {
                    int key = rnd.nextInt(keyCnt);

                    switch (rnd.nextInt(5)) {
                        case 0:
                            c.putx(key, (long)rnd.nextInt(valCnt));

                            break;
                        case 1:
                            if (evictsEnabled())
                                c.evict(key);

                            break;
                        case 2:
                            c.remove(key);

                            break;
                        case 3:
                            c.get(key);

                            break;
                        case 4:
                            GridCacheQuery<Map.Entry<Integer, Long>> qry = c.queries().createSqlQuery(
                                Long.class,
                                "_val between ? and ?");

                            int from = rnd.nextInt(valCnt);

                            GridCacheQueryFuture<Map.Entry<Integer, Long>> f = qry.execute(from, from + 250);

                            Collection<Map.Entry<Integer, Long>> res = f.get();

                            for (Map.Entry<Integer, Long> ignored : res) {
                                //No-op.
                            }
                    }
                }
            }
        }, threadCnt);

        Thread.sleep(DURATION);

        done.set(true);

        fut.get();
    }

    /**
     * JUnit.
     *
     * @throws Exception If failed.
     */
    @SuppressWarnings({"TooBroadScope"})
    public void testMultiThreadedSwapUnswapLongString() throws Exception {
        int threadCnt = 150;
        final int keyCnt = 2000;
        final int valCnt = 10000;

        final Grid g = grid(0);

        // Put test values into cache.
        final GridCache<Integer, Object> c = g.cache(null);

        assertEquals(0, g.cache(null).size());
        assertEquals(0, c.offHeapEntriesCount());
//        assertEquals(0, c.swapKeys());
        assertEquals(0, c.queries().createSqlQuery(String.class, "1 = 1").execute().get().size());
        assertEquals(0, c.queries().createSqlQuery(Long.class, "1 = 1").execute().get().size());

        Random rnd = new Random();

        for (int i = 0; i < keyCnt; i += 1 + rnd.nextInt(3)) {
            c.putx(i, rnd.nextBoolean() ? (long)rnd.nextInt(valCnt) : String.valueOf(rnd.nextInt(valCnt)));

            if (evictsEnabled() && rnd.nextBoolean())
                assertTrue(c.evict(i));
        }

        final AtomicBoolean done = new AtomicBoolean();

        GridFuture<?> fut = multithreadedAsync(new CAX() {
            @Override public void applyx() throws GridException {
                Random rnd = new Random();

                while (!done.get()) {
                    int key = rnd.nextInt(keyCnt);

                    switch (rnd.nextInt(5)) {
                        case 0:
                            c.putx(key, rnd.nextBoolean() ? (long)rnd.nextInt(valCnt) :
                                String.valueOf(rnd.nextInt(valCnt)));

                            break;
                        case 1:
                            if (evictsEnabled())
                                c.evict(key);

                            break;
                        case 2:
                            c.remove(key);

                            break;
                        case 3:
                            c.get(key);

                            break;
                        case 4:
                            GridCacheQuery<Map.Entry<Integer, Object>> qry = c.queries().createSqlQuery(
                                rnd.nextBoolean() ? Long.class : String.class,
                                "_val between ? and ?");

                            int from = rnd.nextInt(valCnt);

                            GridCacheQueryFuture<Map.Entry<Integer, Object>> f = qry.execute(from, from + 250);

                            Collection<Map.Entry<Integer, Object>> res = f.get();

                            for (Map.Entry<Integer, Object> ignored : res) {
                                //No-op.
                            }
                    }
                }
            }
        }, threadCnt);

        Thread.sleep(DURATION);

        done.set(true);

        fut.get();
    }

    /**
     * @throws Exception If failed.
     */
    @SuppressWarnings({"TooBroadScope"})
    public void testMultiThreadedSwapUnswapObject() throws Exception {
        int threadCnt = 50;
        final int keyCnt = 4000;
        final int valCnt = 10000;

        final Grid g = grid(0);

        // Put test values into cache.
        final GridCache<Integer, TestValue> c = g.cache(null);

        assertEquals(0, g.cache(null).size());
        assertEquals(0, c.queries().createSqlQuery(String.class, "1 = 1").execute().get().size());
        assertEquals(0, c.queries().createSqlQuery(Long.class, "1 = 1").execute().get().size());

        Random rnd = new Random();

        for (int i = 0; i < keyCnt; i += 1 + rnd.nextInt(3)) {
            c.putx(i, new TestValue(rnd.nextInt(valCnt)));

            if (evictsEnabled() && rnd.nextBoolean())
                assertTrue(c.evict(i));
        }

        final AtomicBoolean done = new AtomicBoolean();

        GridFuture<?> fut = multithreadedAsync(new CAX() {
            @Override public void applyx() throws GridException {
                Random rnd = new Random();

                while (!done.get()) {
                    int key = rnd.nextInt(keyCnt);

                    switch (rnd.nextInt(5)) {
                        case 0:
                            c.putx(key, new TestValue(rnd.nextInt(valCnt)));

                            break;
                        case 1:
                            if (evictsEnabled())
                                c.evict(key);

                            break;
                        case 2:
                            c.remove(key);

                            break;
                        case 3:
                            c.get(key);

                            break;
                        case 4:
                            GridCacheQuery<Map.Entry<Integer, TestValue>> qry = c.queries().createSqlQuery(
                                Long.class, "TestValue.val between ? and ?");

                            int from = rnd.nextInt(valCnt);

                            GridCacheQueryFuture<Map.Entry<Integer, TestValue>> f = qry.execute(from, from + 250);

                            Collection<Map.Entry<Integer, TestValue>> res = f.get();

                            for (Map.Entry<Integer, TestValue> ignored : res) {
                                //No-op.
                            }
                    }
                }
            }
        }, threadCnt);

        Thread.sleep(DURATION);

        done.set(true);

        fut.get();
    }

    /**
     * JUnit.
     *
     * @throws Exception If failed.
     */
    @SuppressWarnings({"TooBroadScope"})
    public void testMultiThreadedSameQuery() throws Exception {
        int threadCnt = 50;
        final int keyCnt = 10;
        final int logMod = 5000;

        final Grid g = grid(0);

        // Put test values into cache.
        GridCache<Integer, Integer> c = g.cache(null);

        for (int i = 0; i < keyCnt; i++) {
            c.putx(i, i);

            info("Affinity [key=" + i + ", aff=" + c.affinity().mapKeyToPrimaryAndBackups(i).iterator().next().id() + ']');

            assertTrue(c.evict(i));
        }

        final AtomicInteger cnt = new AtomicInteger();

        final AtomicBoolean done = new AtomicBoolean();

        final GridCacheQuery<Map.Entry<Integer, Integer>> qry = c.queries().createSqlQuery(Integer.class, "_val >= 0");

        GridFuture<?> fut = multithreadedAsync(
            new CAX() {
                @Override public void applyx() throws GridException {
                    int iter = 0;

                    while (!done.get() && !Thread.currentThread().isInterrupted()) {
                        iter++;

                        GridCacheQueryFuture<Map.Entry<Integer, Integer>> fut = qry.execute();

                        Collection<Map.Entry<Integer, Integer>> entries = fut.get();

                        assert entries != null;

                        assertEquals("Query results [entries=" + entries + ", aff=" + affinityNodes(entries, g) +
                            ", iteration=" + iter + ']', keyCnt, entries.size());

                        if (cnt.incrementAndGet() % logMod == 0) {
                            GridCacheQueryManager<Object, Object> qryMgr =
                                ((GridKernal)g).internalCache().context().queries();

                            assert qryMgr != null;

                            qryMgr.printMemoryStats();
                        }
                    }
                }
            }, threadCnt);

        Thread.sleep(DURATION);

        info("Finishing test...");

        done.set(true);

        fut.get();
    }

    /**
     * JUnit.
     *
     * @throws Exception If failed.
     */
    @SuppressWarnings({"TooBroadScope"})
    public void testMultiThreadedNewQueries() throws Exception {
        int threadCnt = 50;
        final int keyCnt = 10;
        final int logMod = 5000;

        final Grid g = grid(0);

        // Put test values into cache.
        final GridCache<Integer, Integer> c = g.cache(null);

        for (int i = 0; i < keyCnt; i++) {
            c.putx(i, i);

            assertTrue(c.evict(i));
        }

        final AtomicInteger cnt = new AtomicInteger();

        final AtomicBoolean done = new AtomicBoolean();

        GridFuture<?> fut = multithreadedAsync(new CAX() {
            @Override public void applyx() throws GridException {
                int iter = 0;

                while (!done.get() && !Thread.currentThread().isInterrupted()) {
                    iter++;

                    GridCacheQuery<Map.Entry<Integer, Integer>> qry =
                        c.queries().createSqlQuery(Integer.class, "_val >= 0");

                    GridCacheQueryFuture<Map.Entry<Integer, Integer>> fut = qry.execute();

                    Collection<Map.Entry<Integer, Integer>> entries = fut.get();

                    assert entries != null;

                    assertEquals("Entries count is not as expected on iteration: " + iter, keyCnt, entries.size());

                    if (cnt.incrementAndGet() % logMod == 0) {
                        GridCacheQueryManager<Object, Object> qryMgr =
                            ((GridKernal)g).internalCache().context().queries();

                        assert qryMgr != null;

                        qryMgr.printMemoryStats();
                    }
                }
            }
        }, threadCnt);

        Thread.sleep(DURATION);

        done.set(true);

        fut.get();
    }

    /**
     * JUnit.
     *
     * @throws Exception If failed.
     */
    @SuppressWarnings({"TooBroadScope"})
    public void testMultiThreadedReduceQuery() throws Exception {
        int threadCnt = 50;
        int keyCnt = 10;
        final int logMod = 5000;

        final Grid g = grid(0);

        // Put test values into cache.
        GridCache<Integer, Integer> c = g.cache(null);

        for (int i = 0; i < keyCnt; i++)
            c.putx(i, i);

        final GridCacheQuery<Map.Entry<Integer, Integer>> rdcQry =
            c.queries().createSqlQuery(Integer.class, "_val > 1 and _val < 4");

        rdcQry.includeBackups(true);
        rdcQry.keepAll(true);

        final GridReducer<Map.Entry<Integer, Integer>, Integer> rmtRdc =
            new GridReducer<Map.Entry<Integer, Integer>, Integer>() {
                /** Reducer result. */
                private int res;

                @Override public boolean collect(Map.Entry<Integer, Integer> e) {
                    res += e.getKey();

                    return true;
                }

                @Override public Integer reduce() {
                    return res;
                }
            };

        final AtomicInteger cnt = new AtomicInteger();

        final AtomicBoolean stop = new AtomicBoolean();

        GridFuture<?> fut = multithreadedAsync(new CAX() {
            @Override public void applyx() throws GridException {
                while (!stop.get()) {
                    Collection<Integer> rmtVals = rdcQry.execute(rmtRdc).get();

                    assertEquals(GRID_CNT, rmtVals.size());

                    Iterator<Integer> reduceIter = rmtVals.iterator();

                    assert reduceIter != null;

                    for (int i = 0; i < GRID_CNT; i++) {
                        assert reduceIter.hasNext();

                        assertEquals(Integer.valueOf(5), reduceIter.next());
                    }

                    Collection<Integer> res = rdcQry.execute(rmtRdc).get();

                    int val = F.sumInt(res);

                    int expVal = 5 * GRID_CNT;

                    assertEquals(expVal, val);

                    if (cnt.incrementAndGet() % logMod == 0) {
                        GridCacheQueryManager<Object, Object> qryMgr =
                            ((GridKernal)g).internalCache().context().queries();

                        assert qryMgr != null;

                        qryMgr.printMemoryStats();
                    }
                }
            }
        }, threadCnt);

        Thread.sleep(DURATION);

        stop.set(true);

        fut.get();
    }

    /**
     * JUnit.
     *
     * @throws Exception If failed.
     */
    @SuppressWarnings({"TooBroadScope"})
    public void testMultiThreadedScanQuery() throws Exception {
        int threadCnt = 50;
        final int keyCnt = 500;
        final int logMod = 5000;

        final Grid g = grid(0);

        // Put test values into cache.
        GridCache<Integer, Integer> c = g.cache(null);

        for (int i = 0; i < keyCnt; i++)
            c.putx(i, i);

        final AtomicInteger cnt = new AtomicInteger();

        final AtomicBoolean done = new AtomicBoolean();

        final GridCacheQuery<Map.Entry<Integer, Integer>> qry = c.queries().createScanQuery(null);

        GridFuture<?> fut = multithreadedAsync(
            new CAX() {
                @Override public void applyx() throws GridException {
                    int iter = 0;

                    while (!done.get() && !Thread.currentThread().isInterrupted()) {
                        iter++;

                        GridCacheQueryFuture<Map.Entry<Integer, Integer>> fut = qry.execute();

                        Collection<Map.Entry<Integer, Integer>> entries = fut.get();

                        assert entries != null;

                        assertEquals("Entries count is not as expected on iteration: " + iter, keyCnt, entries.size());

                        if (cnt.incrementAndGet() % logMod == 0) {
                            GridCacheQueryManager<Object, Object> qryMgr =
                                ((GridKernal)g).internalCache().context().queries();

                            assert qryMgr != null;

                            qryMgr.printMemoryStats();
                        }
                    }
                }
            }, threadCnt);

        Thread.sleep(DURATION);

        done.set(true);

        fut.get();
    }

    /**
     * Test value.
     */
    private static class TestValue implements Serializable {
        /** Value. */
        @GridCacheQuerySqlField
        private int val;

        /**
         * @param val Value.
         */
        private TestValue(int val) {
            this.val = val;
        }

        /**
         * @return Value.
         */
        public int value() {
            return val;
        }
    }
}
