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

package org.gridgain.grid.kernal;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.cache.eviction.lru.*;
import org.gridgain.grid.kernal.processors.cache.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.testframework.junits.common.*;

import java.util.*;
import java.util.concurrent.*;

import static org.gridgain.grid.cache.GridCacheMode.*;

/**
 * Tests explicit lock.
 */
public class GridMultiTxLockSelfTest extends GridCommonAbstractTest {
    /** */
    public static final String CACHE_NAME = "part_cache";

    /** IP finder. */
    private static final GridTcpDiscoveryIpFinder ipFinder = new GridTcpDiscoveryVmIpFinder(true);

    /** */
    private volatile boolean run = true;

    /** {@inheritDoc} */
    @Override protected void afterTestsStopped() throws Exception {
        stopAllGrids();

        assertEquals(0, G.allGrids().size());
    }

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration c = super.getConfiguration(gridName);

        GridTcpDiscoverySpi disco = new GridTcpDiscoverySpi();

        disco.setIpFinder(ipFinder);

        c.setDiscoverySpi(disco);

        GridCacheConfiguration ccfg = new GridCacheConfiguration();

        // Cache configuration copy from BOKU-58
        ccfg.setName(CACHE_NAME);
        ccfg.setDefaultTimeToLive(120000);
        ccfg.setAtomicityMode(GridCacheAtomicityMode.TRANSACTIONAL);
        ccfg.setWriteSynchronizationMode(GridCacheWriteSynchronizationMode.PRIMARY_SYNC);
        ccfg.setQueryIndexEnabled(false);
        ccfg.setBackups(2);
        ccfg.setCacheMode(PARTITIONED);
        ccfg.setStartSize(100000);
        ccfg.setDefaultTxTimeout(3000);
        ccfg.setDefaultLockTimeout(3000);
        ccfg.setNearEvictionPolicy(new GridCacheLruEvictionPolicy(10000));
        ccfg.setEvictNearSynchronized(true);
        ccfg.setEvictionPolicy(new GridCacheLruEvictionPolicy(100000));
        ccfg.setEvictSynchronized(true);
        ccfg.setDistributionMode(GridCacheDistributionMode.PARTITIONED_ONLY);

        c.setCacheConfiguration(ccfg);

        return c;
    }

    /**
     * @throws Exception If failed.
     */
    public void testExplicitLockOneKey() throws Exception {
        checkExplicitLock(1);
    }

    /**
     * @throws Exception If failed.
     */
    public void testExplicitLockManyKeys() throws Exception {
        checkExplicitLock(4);
    }

    /**
     * @throws Exception If failed.
     */
    public void checkExplicitLock(int keys) throws Exception {
        Collection<Thread> threads = new ArrayList<>();

        try {
            // Start grid 1.
            Grid grid1 = startGrid(1);

            threads.add(runCacheOperations(grid1.cache(CACHE_NAME), keys));

            TimeUnit.SECONDS.sleep(3L);

            // Start grid 2.
            Grid grid2 = startGrid(2);

            threads.add(runCacheOperations(grid2.cache(CACHE_NAME), keys));

            TimeUnit.SECONDS.sleep(3L);

            // Start grid 3.
            Grid grid3 = startGrid(3);

            threads.add(runCacheOperations(grid3.cache(CACHE_NAME), keys));

            TimeUnit.SECONDS.sleep(3L);

            // Start grid 4.
            Grid grid4 = startGrid(4);

            threads.add(runCacheOperations(grid4.cache(CACHE_NAME), keys));

            TimeUnit.SECONDS.sleep(3L);

            stopThreads(threads);

            for (int i = 1; i <= 4; i++) {
                GridCacheTxManager<Object, Object> tm = ((GridKernal) grid(i)).internalCache(CACHE_NAME).context().tm();

                assertEquals("txMap is not empty:" + i, 0, tm.idMapSize());
            }
        }
        finally {
            stopAllGrids();
        }
    }

    /**
     * @param threads Thread which will be stopped.
     */
    private void stopThreads(Iterable<Thread> threads) {
        try {
            run = false;

            for (Thread thread : threads)
                thread.join();
        }
        catch (Exception e) {
            U.error(log(), "Couldn't stop threads.", e);
        }
    }

    /**
     * @param cache Cache.
     * @return Running thread.
     */
    private Thread runCacheOperations(final GridCacheProjection<Object,Object> cache, final int keys) {
        Thread t = new Thread() {
            @Override public void run() {
                while (run) {
                    TreeMap<Integer, String> vals = generateValues(keys);

                    try {
                        // Explicit lock.
                        cache.lock(vals.firstKey(), 0);

                        try {
                            // Put or remove.
                            if (ThreadLocalRandom.current().nextDouble(1) < 0.65)
                                cache.putAll(vals);
                            else
                                cache.removeAll(vals.keySet());
                        }
                        catch (Exception e) {
                            U.error(log(), "Failed cache operation.", e);
                        }
                        finally {
                            cache.unlock(vals.firstKey());
                        }
                    }
                    catch (Exception e){
                        U.error(log(), "Failed unlock.", e);
                    }
                }
            }
        };

        t.start();

        return t;
    }

    /**
     * @param cnt Number of keys to generate.
     * @return Map.
     */
    private TreeMap<Integer, String> generateValues(int cnt) {
        TreeMap<Integer, String> res = new TreeMap<>();

        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        while (res.size() < cnt) {
            int key = rnd.nextInt(0, 100);

            res.put(key, String.valueOf(key));
        }

        return res;
    }
}
