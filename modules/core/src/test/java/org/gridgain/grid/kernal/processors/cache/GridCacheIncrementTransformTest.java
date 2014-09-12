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
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.testframework.*;
import org.gridgain.testframework.junits.common.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import static org.gridgain.grid.cache.GridCacheAtomicWriteOrderMode.*;
import static org.gridgain.grid.cache.GridCacheAtomicityMode.*;
import static org.gridgain.grid.cache.GridCacheMode.*;
import static org.gridgain.grid.cache.GridCacheDistributionMode.*;
import static org.gridgain.grid.cache.GridCachePreloadMode.*;
import static org.gridgain.grid.cache.GridCacheWriteSynchronizationMode.*;

/**
 * Tests cache in-place modification logic with iterative value increment.
 */
public class GridCacheIncrementTransformTest extends GridCommonAbstractTest {
    /** IP finder. */
    private static final GridTcpDiscoveryIpFinder IP_FINDER = new GridTcpDiscoveryVmIpFinder(true);

    /** Number of nodes to test on. */
    private static final int GRID_CNT = 4;

    /** Number of increment iterations. */
    private static final int NUM_ITERS = 5000;

    /** Helper for excluding stopped node from iteration logic. */
    private AtomicReferenceArray<Grid> grids;

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        GridCacheConfiguration cache = new GridCacheConfiguration();

        cache.setCacheMode(PARTITIONED);
        cache.setAtomicityMode(ATOMIC);
        cache.setDistributionMode(PARTITIONED_ONLY);
        cache.setAtomicWriteOrderMode(PRIMARY);
        cache.setWriteSynchronizationMode(FULL_SYNC);
        cache.setBackups(1);
        cache.setPreloadMode(SYNC);

        cfg.setCacheConfiguration(cache);

        GridTcpDiscoverySpi disco = new GridTcpDiscoverySpi();

        disco.setIpFinder(IP_FINDER);

        cfg.setDiscoverySpi(disco);

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        startGrids(GRID_CNT);

        grids = new AtomicReferenceArray<>(GRID_CNT);

        for (int i = 0; i < GRID_CNT; i++)
            grids.set(i, grid(i));

        cache(0).put("key", new TestObject(0));
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        stopAllGrids();

        grids = null;
    }

    /**
     * @throws Exception If failed.
     */
    public void testIncrement() throws Exception {
        testIncrement(false);
    }

    /**
     * @throws Exception If failed.
     */
    public void testIncrementRestart() throws Exception {
        final AtomicBoolean stop = new AtomicBoolean();
        final AtomicReference<Throwable> error = new AtomicReference<>();

        GridFuture<Long> fut = GridTestUtils.runMultiThreadedAsync(new Runnable() {
            @Override public void run() {
                try {
                    Random rnd = new Random();

                    while (!stop.get()) {
                        int idx = -1;

                        Grid grid = null;

                        while (grid == null) {
                            idx = rnd.nextInt(GRID_CNT);

                            grid = grids.getAndSet(idx, null);
                        }

                        stopGrid(idx);

                        assert grids.compareAndSet(idx, null, startGrid(idx));
                    }
                }
                catch (Exception e) {
                    error.set(e);
                }
            }
        }, 1, "restarter");

        try {
            testIncrement(true);

            assertNull(error.get());
        }
        finally {
            stop.set(true);

            fut.get(getTestTimeout());
        }
    }

    /**
     * @param restarts Whether test is running with node restarts.
     * @throws Exception If failed.
     */
    private void testIncrement(boolean restarts) throws Exception {
        Random rnd = new Random();

        for (int i = 0; i < NUM_ITERS; i++) {
            int idx = -1;

            Grid grid = null;

            while (grid == null) {
                idx = rnd.nextInt(GRID_CNT);

                grid = restarts ? grids.getAndSet(idx, null) : grid(idx);
            }

            GridCache <String, TestObject> cache = grid.cache(null);

            assertNotNull(cache);

            TestObject obj = cache.get("key");

            assertNotNull(obj);
            assertEquals(i, obj.val);

            while (true) {
                try {
                    cache.transform("key", new Transformer());

                    break;
                }
                catch (GridCachePartialUpdateException ignored) {
                    // Need to re-check if update actually succeeded.
                    TestObject updated = cache.get("key");

                    if (updated != null && updated.val == i + 1)
                        break;
                }
            }

            if (restarts)
                assert grids.compareAndSet(idx, null, grid);
        }
    }

    /** */
    private static class TestObject implements Serializable {
        /** Value. */
        private int val;

        /**
         * @param val Value.
         */
        private TestObject(int val) {
            this.val = val;
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return "TestObject [val=" + val + ']';
        }
    }

    /** */
    private static class Transformer implements C1<TestObject, TestObject> {
        /** {@inheritDoc} */
        @Override public TestObject apply(TestObject obj) {
            assert obj != null;

            return new TestObject(obj.val + 1);
        }
    }
}
