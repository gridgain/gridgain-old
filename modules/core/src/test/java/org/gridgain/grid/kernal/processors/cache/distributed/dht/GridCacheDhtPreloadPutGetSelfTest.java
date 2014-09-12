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
import org.gridgain.grid.kernal.processors.cache.distributed.dht.preloader.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.testframework.*;
import org.gridgain.testframework.junits.common.*;
import org.jetbrains.annotations.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static org.gridgain.grid.cache.GridCacheMode.*;
import static org.gridgain.grid.cache.GridCachePreloadMode.*;
import static org.gridgain.grid.cache.GridCacheWriteSynchronizationMode.*;

/**
 * Test cases for partitioned cache {@link GridDhtPreloader preloader}.
 *
 * Forum example <a
 * href="http://www.gridgainsystems.com/jiveforums/thread.jspa?threadID=1449">
 * http://www.gridgainsystems.com/jiveforums/thread.jspa?threadID=1449</a>
 */
public class GridCacheDhtPreloadPutGetSelfTest extends GridCommonAbstractTest {
    /** Key count. */
    private static final int KEY_CNT = 1000;

    /** Iterations count. */
    private static final int ITER_CNT = 10;

    /** Frequency. */
    private static final int FREQUENCY = 100;

    /** Number of key backups. Each test method can set this value as required. */
    private int backups;

    /** Preload mode. */
    private GridCachePreloadMode preloadMode;

    /** IP finder. */
    private GridTcpDiscoveryIpFinder ipFinder = new GridTcpDiscoveryVmIpFinder(true);

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        assert preloadMode != null;

        GridCacheConfiguration cacheCfg = defaultCacheConfiguration();

        cacheCfg.setCacheMode(PARTITIONED);
        cacheCfg.setWriteSynchronizationMode(FULL_SYNC);
        cacheCfg.setPreloadMode(preloadMode);
        cacheCfg.setBackups(backups);

        GridTcpDiscoverySpi disco = new GridTcpDiscoverySpi();

        disco.setIpFinder(ipFinder);

        cfg.setDiscoverySpi(disco);
        cfg.setCacheConfiguration(cacheCfg);

        return cfg;
    }

    /**
     * @throws Exception If failed.
     */
    public void testPutGetAsync0() throws Exception {
        preloadMode = ASYNC;
        backups = 0;

        performTest();
    }

    /**
     * @throws Exception If failed.
     */
    public void testPutGetAsync1() throws Exception {
        preloadMode = ASYNC;
        backups = 1;

        performTest();
    }

    /**
     * @throws Exception If failed.
     */
    public void testPutGetAsync2() throws Exception {
        preloadMode = ASYNC;
        backups = 2;

        performTest();
    }

    /**
     * @throws Exception If failed.
     */
    public void testPutGetSync0() throws Exception {
        preloadMode = SYNC;
        backups = 0;

        performTest();
    }

    /**
     * @throws Exception If failed.
     */
    public void testPutGetSync1() throws Exception {
        preloadMode = SYNC;
        backups = 1;

        performTest();
    }

    /**
     * @throws Exception If failed.
     */
    public void testPutGetSync2() throws Exception {
        preloadMode = SYNC;
        backups = 2;

        performTest();
    }

    /**
     * @throws Exception If failed.
     */
    public void testPutGetNone0() throws Exception {
        preloadMode = NONE;
        backups = 0;

        performTest();
    }

    /**
     * @throws Exception If failed.
     */
    public void testPutGetNone1() throws Exception {
        preloadMode = NONE;
        backups = 1;

        performTest();
    }

    /**
     * @throws Exception If failed.
     */
    public void testPutGetNone2() throws Exception {
        preloadMode = NONE;
        backups = 2;

        performTest();
    }

    /**
     * @throws Exception If test fails.
     */
    private void performTest() throws Exception {
        try {
            final CountDownLatch writeLatch = new CountDownLatch(1);

            final CountDownLatch readLatch = new CountDownLatch(1);

            final AtomicBoolean done = new AtomicBoolean();

            GridFuture fut1 = GridTestUtils.runMultiThreadedAsync(
                new Callable<Object>() {
                    @Nullable @Override public Object call() throws Exception {
                        Grid g2 = startGrid(2);

                        for (int i = 0; i < ITER_CNT; i++) {
                            info("Iteration # " + i);

                            GridCache<Integer, Integer> cache = g2.cache(null);

                            for (int j = 0; j < KEY_CNT; j++) {
                                GridCacheEntry<Integer, Integer> entry = cache.entry(j);

                                assert entry != null;

                                Integer val = entry.getValue();

                                if (j % FREQUENCY == 0)
                                    info("Read entry: " + entry.getKey() + " -> " + val);

                                if (done.get())
                                    assert val != null && val == j;
                            }

                            writeLatch.countDown();

                            readLatch.await();
                        }

                        return null;
                    }
                },
                1,
                "reader"
            );

            GridFuture fut2 = GridTestUtils.runMultiThreadedAsync(
                new Callable<Object>() {
                    @Nullable @Override public Object call() throws Exception {
                        writeLatch.await();

                        Grid g1 = startGrid(1);

                        GridCache<Integer, Integer> cache = g1.cache(null);

                        for (int j = 0; j < KEY_CNT; j++) {
                            cache.put(j, j);

                            if (j % FREQUENCY == 0)
                                info("Stored value in cache: " + j);
                        }

                        done.set(true);

                        for (int j = 0; j < KEY_CNT; j++) {
                            GridCacheEntry<Integer, Integer> entry = cache.entry(j);

                            assert entry != null;

                            Integer val = entry.getValue();

                            if (j % FREQUENCY == 0)
                                info("Read entry: " + entry.getKey() + " -> " + val);

                            assert val != null && val == j;
                        }

                        if (backups > 0)
                            stopGrid(1);

                        readLatch.countDown();

                        return null;
                    }
                },
                1,
                "writer"
            );

            fut1.get();
            fut2.get();
        }
        finally {
            stopAllGrids();
        }
    }
}
