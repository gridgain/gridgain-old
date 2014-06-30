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

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static org.gridgain.grid.GridSystemProperties.*;
import static org.gridgain.grid.cache.GridCacheAtomicWriteOrderMode.*;
import static org.gridgain.grid.cache.GridCacheMode.*;
import static org.gridgain.grid.cache.GridCacheDistributionMode.*;
import static org.gridgain.grid.cache.GridCachePreloadMode.*;
import static org.gridgain.grid.cache.GridCacheWriteSynchronizationMode.*;

/**
 *
 */
public abstract class GridCacheValueConsistencyAbstractSelfTest extends GridCacheAbstractSelfTest {
    /** Number of threads for test. */
    private static final int THREAD_CNT = 16;

    /** */
    private String sizePropVal;

    /** {@inheritDoc} */
    @Override protected int gridCount() {
        return 4;
    }

    /** {@inheritDoc} */
    @Override protected long getTestTimeout() {
        return 60000;
    }

    /** {@inheritDoc} */
    @Override protected GridCacheConfiguration cacheConfiguration(String gridName) throws Exception {
        GridCacheConfiguration cCfg = super.cacheConfiguration(gridName);

        cCfg.setCacheMode(PARTITIONED);
        cCfg.setAtomicityMode(atomicityMode());
        cCfg.setAtomicWriteOrderMode(writeOrderMode());
        cCfg.setDistributionMode(distributionMode());
        cCfg.setPreloadMode(SYNC);
        cCfg.setWriteSynchronizationMode(FULL_SYNC);
        cCfg.setBackups(1);

        return cCfg;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        // Need to increase value set in GridAbstractTest
        sizePropVal = System.getProperty(GG_ATOMIC_CACHE_DELETE_HISTORY_SIZE);

        System.setProperty(GG_ATOMIC_CACHE_DELETE_HISTORY_SIZE, "100000");

        super.beforeTestsStarted();
    }

    /** {@inheritDoc} */
    @Override protected void afterTestsStopped() throws Exception {
        super.afterTestsStopped();

        System.setProperty(GG_ATOMIC_CACHE_DELETE_HISTORY_SIZE, sizePropVal != null ? sizePropVal : "");
    }

    /**
     * @return Distribution mode.
     */
    @Override protected GridCacheDistributionMode distributionMode() {
        return PARTITIONED_ONLY;
    }

    /**
     * @return Atomic write order mode.
     */
    protected GridCacheAtomicWriteOrderMode writeOrderMode() {
        return CLOCK;
    }

    /**
     * @return Consistency test iteration count.
     */
    protected abstract int iterationCount();

    /**
     * @throws Exception If failed.
     */
    public void testPutRemove() throws Exception {
        awaitPartitionMapExchange();

        GridCache<String, Integer> cache = cache();

        int keyCnt = 10;

        for (int i = 0; i < keyCnt; i++)
            cache.put("key" + i, i);

        for (int g = 0; g < gridCount(); g++) {
            GridCache<String, Integer> cache0 = cache(g);
            GridNode locNode = grid(g).localNode();

            for (int i = 0; i < keyCnt; i++) {
                String key = "key" + i;

                if (cache.affinity().mapKeyToPrimaryAndBackups(key).contains(locNode)) {
                    info("Node is reported as affinity node for key [key=" + key + ", nodeId=" + locNode.id() + ']');

                    assertEquals((Integer)i, cache0.peek(key));
                }
                else {
                    info("Node is reported as NOT affinity node for key [key=" + key +
                        ", nodeId=" + locNode.id() + ']');

                    if (distributionMode() == NEAR_PARTITIONED && cache == cache0)
                        assertEquals((Integer)i, cache0.peek(key));
                    else
                        assertNull(cache0.peek(key));
                }

                assertEquals((Integer)i, cache0.get(key));
            }
        }

        info("Removing values from cache.");

        for (int i = 0; i < keyCnt; i++)
            assertEquals((Integer)i, cache.remove("key" + i));

        for (int g = 0; g < gridCount(); g++) {
            GridCache<String, Integer> cache0 = cache(g);

            for (int i = 0; i < keyCnt; i++) {
                String key = "key" + i;

                assertNull(cache0.peek(key));

                assertNull(cache0.get(key));
            }
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testPutRemoveAll() throws Exception {
        awaitPartitionMapExchange();

        GridCache<String, Integer> cache = cache();

        int keyCnt = 10;

        for (int i = 0; i < keyCnt; i++) {
            info("Putting value to cache: " + i);

            cache.put("key" + i, i);
        }

        for (int g = 0; g < gridCount(); g++) {
            GridCache<String, Integer> cache0 = cache(g);
            GridNode locNode = grid(g).localNode();

            for (int i = 0; i < keyCnt; i++) {
                String key = "key" + i;

                if (cache.affinity().mapKeyToPrimaryAndBackups(key).contains(grid(g).localNode())) {
                    info("Node is reported as affinity node for key [key=" + key + ", nodeId=" + locNode.id() + ']');

                    assertEquals((Integer)i, cache0.peek(key));
                }
                else {
                    info("Node is reported as NOT affinity node for key [key=" + key +
                        ", nodeId=" + locNode.id() + ']');

                    if (distributionMode() == NEAR_PARTITIONED && cache == cache0)
                        assertEquals((Integer)i, cache0.peek(key));
                    else
                        assertNull(cache0.peek(key));
                }

                assertEquals((Integer)i, cache0.get(key));
            }
        }

        for (int g = 0; g < gridCount(); g++) {
            info(">>>> Removing all values form cache: " + g);

            cache(g).removeAll();
        }

        info(">>>> Starting values check");

        for (int g = 0; g < gridCount(); g++) {
            GridCache<String, Integer> cache0 = cache(g);

            for (int i = 0; i < keyCnt; i++) {
                String key = "key" + i;

                assertNull(cache0.peek(key));
                assertNull(cache0.get(key));
            }
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testPutRemoveConsistencyMultithreaded() throws Exception {
        final int range = 10000;

        final int iterCnt = iterationCount();

        final AtomicInteger iters = new AtomicInteger();

        multithreadedAsync(new Callable<Object>() {
            @Override public Object call() throws Exception {
                Random rnd = new Random();

                while (true) {
                    int i = iters.getAndIncrement();

                    if (i >= iterCnt)
                        break;

                    int g = rnd.nextInt(gridCount());

                    Grid grid = grid(g);

                    GridCache<Object, Object> cache = grid.cache(null);

                    int k = rnd.nextInt(range);

                    boolean rmv = rnd.nextBoolean();

                    if (!rmv)
                        cache.put(k, Thread.currentThread().getId());
                    else
                        cache.remove(k);

                    if (i > 0 && i % 5000 == 0)
                        info("Completed: " + i);
                }

                return null;
            }
        }, THREAD_CNT).get();

        int present = 0;
        int absent = 0;

        for (int i = 0; i < range; i++) {
            Long firstVal = null;

            for (int g = 0; g < gridCount(); g++) {
                Long val = (Long)grid(g).cache(null).peek(i);

                if (firstVal == null && val != null)
                    firstVal = val;

                assert val == null || firstVal.equals(val) : "Invalid value detected [val=" + val +
                    ", firstVal=" + firstVal + ']';
            }

            if (firstVal == null)
                absent++;
            else
                present++;
        }

        info("Finished check [present=" + present + ", absent=" + absent + ']');

        info("Checking keySet consistency");

        for (int g = 0; g < gridCount(); g++)
            checkKeySet(grid(g));
    }

    /**
     * @param g Grid to check.
     */
    private void checkKeySet(Grid g) {
        GridCache<Object, Object> cache = g.cache(null);

        Set<Object> keys = cache.keySet();

        int cacheSize = cache.size();
        int keySetSize = keys.size();

        int itSize = 0;

        for (Object ignored : keys)
            itSize++;

        int valsSize = cache.values().size();

        info("cacheSize=" + cacheSize + ", keysSize=" + keySetSize + ", valsSize=" + valsSize +
            ", itSize=" + itSize + ']');

        assertEquals("cacheSize vs itSize", cacheSize, itSize);
        assertEquals("cacheSize vs keySeySize", cacheSize, keySetSize);
    }
}
