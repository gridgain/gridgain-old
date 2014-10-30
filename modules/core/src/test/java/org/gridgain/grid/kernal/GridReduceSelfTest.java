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
import org.gridgain.grid.lang.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.testframework.junits.common.*;

import java.util.*;

/**
 * Test reduce with long operations.
 */
public class GridReduceSelfTest extends GridCommonAbstractTest {
    /** Number of nodes in the grid. */
    private static final int GRID_CNT = 3;

    /**
     * @throws Exception If failed.
     */
    public void testReduce() throws Exception {
        startGrids(GRID_CNT);

        try {
            Grid grid = grid(0);

            assert grid.nodes().size() == GRID_CNT;

            List<ReducerTestClosure> closures = closures(grid.nodes().size());

            Long res = grid.forLocal().compute().call(closures, new R1<Long, Long>() {
                private long sum;

                @Override public boolean collect(Long e) {
                    info("Got result from closure: " + e);

                    sum += e;

                    // Stop collecting on value 1.
                    return e != 1;
                }

                @Override public Long reduce() {
                    return sum;
                }
            }).get();

            assertEquals((Long)1L, res);

            assertTrue(closures.get(0).isFinished);

            for (int i = 1; i < closures.size(); i++)
                assertFalse("Closure #" + i + " is not interrupted.", closures.get(i).isFinished);
        }
        finally {
            stopAllGrids();
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testReduceAsync() throws Exception {
        startGrids(GRID_CNT);

        try {
            Grid grid = grid(0);

            assert grid.nodes().size() == GRID_CNT;

            List<ReducerTestClosure> closures = closures(grid.nodes().size());

            GridFuture<Long> fut = grid.forLocal().compute().call(closures, new R1<Long, Long>() {
                private long sum;

                @Override
                public boolean collect(Long e) {
                    info("Got result from closure: " + e);

                    sum += e;

                    // Stop collecting on value 1.
                    return e != 1;
                }

                @Override
                public Long reduce() {
                    return sum;
                }
            });

            assertEquals((Long)1L, fut.get());

            assertTrue(closures.get(0).isFinished);

            for (int i = 1; i < closures.size(); i++)
                assertFalse("Closure #" + i + " is not interrupted.", closures.get(i).isFinished);
        }
        finally {
            stopAllGrids();
        }
    }

    /**
     * @param size Number of closures.
     * @return Collection of closures.
     */
    private static List<ReducerTestClosure> closures(int size) {
        assert size > 1;

        List<ReducerTestClosure> cls = new ArrayList<>(size);

        cls.add(new ReducerTestClosure(true)); // Fast closure.

        for (int i = 1; i < size; i++)
            cls.add(new ReducerTestClosure(false)); // Normal closures.

        return cls;
    }

    /**
     * Closure for testing reducer.
     */
    @SuppressWarnings("PackageVisibleField")
    private static class ReducerTestClosure implements GridCallable<Long> {
        /** Logger. */
        @GridLoggerResource
        private GridLogger log;

        /** Test flag to check the thread was interrupted. */
        volatile boolean isFinished;

        /** Fast or normal closure. */
        private boolean fast;

        /**
         * @param fast Fast or normal closure.
         */
        ReducerTestClosure(boolean fast) {
            this.fast = fast;
        }

        /** {@inheritDoc} */
        @Override public Long call() {
            try {
                try {
                    if (fast) {
                        Thread.sleep(500);

                        log.info("Returning 1 from fast closure.");

                        return 1L;
                    }
                    else {
                        Thread.sleep(5000);

                        log.info("Returning 2 from normal closure.");

                        return 2L;
                    }
                }
                finally {
                    isFinished = true;
                }
            }
            catch (InterruptedException ignore) {
                log.info("Returning 0 from interrupted closure.");

                return 0L;
            }
        }
    }
}
