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

import org.gridgain.grid.cache.*;
import org.gridgain.testframework.*;

import java.util.concurrent.*;

import static org.gridgain.grid.cache.GridCacheTxConcurrency.*;
import static org.gridgain.grid.cache.GridCacheTxIsolation.*;

/**
 * Multithreaded update test with off heap enabled.
 */
public class GridCacheOffHeapMultiThreadedUpdateSelfTest extends GridCacheOffHeapMultiThreadedUpdateAbstractSelfTest {
    /**
     * @throws Exception If failed.
     */
    public void testTransformTx() throws Exception {
        info(">>> PESSIMISTIC node 0");

        testTransformTx(keyForNode(0), PESSIMISTIC);

        info(">>> OPTIMISTIC node 0");
        testTransformTx(keyForNode(0), OPTIMISTIC);

        if (gridCount() > 1) {
            info(">>> PESSIMISTIC node 1");
            testTransformTx(keyForNode(1), PESSIMISTIC);

            info(">>> OPTIMISTIC node 1");
            testTransformTx(keyForNode(1), OPTIMISTIC);
        }
    }

    /**
     * @param key Key.
     * @param txConcurrency Transaction concurrency.
     * @throws Exception If failed.
     */
    private void testTransformTx(final Integer key, final GridCacheTxConcurrency txConcurrency) throws Exception {
        final GridCache<Integer, Integer> cache = grid(0).cache(null);

        cache.put(key, 0);

        final int THREADS = 5;
        final int ITERATIONS_PER_THREAD = iterations();

        GridTestUtils.runMultiThreaded(new Callable<Void>() {
            @Override public Void call() throws Exception {
                for (int i = 0; i < ITERATIONS_PER_THREAD && !failed; i++) {
                    if (i % 500 == 0)
                        log.info("Iteration " + i);

                    try (GridCacheTx tx = cache.txStart(txConcurrency, REPEATABLE_READ)) {
                        cache.transform(key, new IncClosure());

                        tx.commit();
                    }
                }

                return null;
            }
        }, THREADS, "transform");

        for (int i = 0; i < gridCount(); i++) {
            Integer val = (Integer)grid(i).cache(null).get(key);

            if (txConcurrency == PESSIMISTIC)
                assertEquals("Unexpected value for grid " + i, (Integer)(ITERATIONS_PER_THREAD * THREADS), val);
            else
                assertNotNull("Unexpected value for grid " + i, val);
        }

        if (failed) {
            for (int g = 0; g < gridCount(); g++)
                info("Value for cache [g=" + g + ", val=" + grid(g).cache(null).get(key) + ']');

            assertFalse(failed);
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testPutTx() throws Exception {
        testPutTx(keyForNode(0), PESSIMISTIC);

        // TODO GG-8118.
        //testPutTx(keyForNode(0), OPTIMISTIC);

        if (gridCount() > 1) {
            testPutTx(keyForNode(1), PESSIMISTIC);

            // TODO GG-8118.
            //testPutTx(keyForNode(1), OPTIMISTIC);
        }
    }

    /**
     * @param key Key.
     * @param txConcurrency Transaction concurrency.
     * @throws Exception If failed.
     */
    private void testPutTx(final Integer key, final GridCacheTxConcurrency txConcurrency) throws Exception {
        final GridCache<Integer, Integer> cache = grid(0).cache(null);

        cache.put(key, 0);

        final int THREADS = 5;
        final int ITERATIONS_PER_THREAD = iterations();

        GridTestUtils.runMultiThreaded(new Callable<Void>() {
            @Override public Void call() throws Exception {
                for (int i = 0; i < ITERATIONS_PER_THREAD; i++) {
                    if (i % 500 == 0)
                        log.info("Iteration " + i);

                    try (GridCacheTx tx = cache.txStart(txConcurrency, REPEATABLE_READ)) {
                        Integer val = cache.put(key, i);

                        assertNotNull(val);

                        tx.commit();
                    }
                }

                return null;
            }
        }, THREADS, "put");

        for (int i = 0; i < gridCount(); i++) {
            Integer val = (Integer)grid(i).cache(null).get(key);

            assertNotNull("Unexpected value for grid " + i, val);
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testPutWithFilterTx() throws Exception {
        testPutWithFilterTx(keyForNode(0), PESSIMISTIC);

        // TODO GG-8118.
        //testPutWithFilterTx(keyForNode(0), OPTIMISTIC);

        if (gridCount() > 1) {
            testPutWithFilterTx(keyForNode(1), PESSIMISTIC);

            // TODO GG-8118.
            //testPutWithFilterTx(keyForNode(1), OPTIMISTIC);
        }
    }

    /**
     * @param key Key.
     * @param txConcurrency Transaction concurrency.
     * @throws Exception If failed.
     */
    private void testPutWithFilterTx(final Integer key, final GridCacheTxConcurrency txConcurrency) throws Exception {
        final GridCache<Integer, Integer> cache = grid(0).cache(null);

        cache.put(key, 0);

        final int THREADS = 5;
        final int ITERATIONS_PER_THREAD = iterations();

        GridTestUtils.runMultiThreaded(new Callable<Void>() {
            @Override public Void call() throws Exception {
                for (int i = 0; i < ITERATIONS_PER_THREAD && !failed; i++) {
                    if (i % 500 == 0)
                        log.info("Iteration " + i);

                    try (GridCacheTx tx = cache.txStart(txConcurrency, REPEATABLE_READ)) {
                        cache.putx(key, i, new TestFilter());

                        tx.commit();
                    }
                }

                return null;
            }
        }, THREADS, "putWithFilter");

        for (int i = 0; i < gridCount(); i++) {
            Integer val = (Integer)grid(i).cache(null).get(key);

            assertNotNull("Unexpected value for grid " + i, val);
        }

        assertFalse(failed);
    }

    /**
     * @throws Exception If failed.
     */
    public void testPutxIfAbsentTx() throws Exception {
        testPutxIfAbsentTx(keyForNode(0), PESSIMISTIC);

        // TODO GG-8118.
        //testPutxIfAbsentTx(keyForNode(0), OPTIMISTIC);

        if (gridCount() > 1) {
            testPutxIfAbsentTx(keyForNode(1), PESSIMISTIC);

            // TODO GG-8118.
            //testPutxIfAbsentTx(keyForNode(1), OPTIMISTIC);
        }
    }

    /**
     * @param key Key.
     * @param txConcurrency Transaction concurrency.
     * @throws Exception If failed.
     */
    private void testPutxIfAbsentTx(final Integer key, final GridCacheTxConcurrency txConcurrency) throws Exception {
        final GridCache<Integer, Integer> cache = grid(0).cache(null);

        cache.put(key, 0);

        final int THREADS = 5;
        final int ITERATIONS_PER_THREAD = iterations();

        GridTestUtils.runMultiThreaded(new Callable<Void>() {
            @Override public Void call() throws Exception {
                for (int i = 0; i < ITERATIONS_PER_THREAD && !failed; i++) {
                    if (i % 500 == 0)
                        log.info("Iteration " + i);

                    try (GridCacheTx tx = cache.txStart(txConcurrency, REPEATABLE_READ)) {
                        assertFalse(cache.putxIfAbsent(key, 100));

                        tx.commit();
                    }
                }

                return null;
            }
        }, THREADS, "putxIfAbsent");

        for (int i = 0; i < gridCount(); i++) {
            Integer val = (Integer)grid(i).cache(null).get(key);

            assertEquals("Unexpected value for grid " + i, (Integer)0, val);
        }

        assertFalse(failed);
    }
}
