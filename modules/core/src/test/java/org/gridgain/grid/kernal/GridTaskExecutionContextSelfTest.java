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
import org.gridgain.grid.compute.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.marshaller.optimized.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.util.lang.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.testframework.*;
import org.gridgain.testframework.junits.common.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Tests for {@code GridProjection.withXXX(..)} methods.
 */
public class GridTaskExecutionContextSelfTest extends GridCommonAbstractTest {
    /** */
    private static final AtomicInteger CNT = new AtomicInteger();

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        cfg.setMarshaller(new GridOptimizedMarshaller(false));

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        startGridsMultiThreaded(2);
    }

    /** {@inheritDoc} */
    @Override protected void afterTestsStopped() throws Exception {
        stopAllGrids();
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        CNT.set(0);
    }

    /**
     * @throws Exception If failed.
     */
    public void testWithName() throws Exception {
        Callable<String> f = new GridCallable<String>() {
            @GridTaskSessionResource
            private GridComputeTaskSession ses;

            @Override public String call() {
                return ses.getTaskName();
            }
        };

        Grid g = grid(0);

        assert "name1".equals(g.compute().withName("name1").call(f).get());
        assert "name2".equals(g.compute().withName("name2").call(f).get());
        assert f.getClass().getName().equals(g.compute().call(f).get());

        assert "name1".equals(g.compute().withName("name1").execute(new TestTask(false), null).get());
        assert "name2".equals(g.compute().withName("name2").execute(new TestTask(false), null).get());
        assert TestTask.class.getName().equals(g.compute().execute(new TestTask(false), null).get());
    }

    /**
     * @throws Exception If failed.
     */
    public void testWithNoFailoverClosure() throws Exception {
        final Runnable r = new GridAbsClosureX() {
            @Override public void applyx() throws GridException {
                CNT.incrementAndGet();

                throw new GridComputeExecutionRejectedException("Expected error.");
            }
        };

        final Grid g = grid(0);

        GridTestUtils.assertThrows(
            log,
            new Callable<Object>() {
                @Override public Object call() throws Exception {
                    g.compute().withNoFailover().run(r).get();

                    return null;
                }
            },
            GridComputeExecutionRejectedException.class,
            "Expected error."
        );

        assertEquals(1, CNT.get());
    }

    /**
     * @throws Exception If failed.
     */
    public void testWithNoFailoverTask() throws Exception {
        final Grid g = grid(0);

        GridTestUtils.assertThrows(
            log,
            new Callable<Object>() {
                @Override public Object call() throws Exception {
                    g.compute().withNoFailover().execute(new TestTask(true), null).get();

                    return null;
                }
            },
            GridComputeExecutionRejectedException.class,
            "Expected error."
        );

        assertEquals(1, CNT.get());
    }

    /**
     * Test task that returns its name.
     */
    private static class TestTask extends GridComputeTaskSplitAdapter<Void, String> {
        /** */
        private final boolean fail;

        /**
         * @param fail Whether to fail.
         */
        private TestTask(boolean fail) {
            this.fail = fail;
        }

        /** {@inheritDoc} */
        @Override protected Collection<? extends GridComputeJob> split(int gridSize, Void arg) throws GridException {
            return F.asSet(new GridComputeJobAdapter() {
                @GridTaskSessionResource
                private GridComputeTaskSession ses;

                @Override public Object execute() throws GridException {
                    CNT.incrementAndGet();

                    if (fail)
                        throw new GridComputeExecutionRejectedException("Expected error.");

                    return ses.getTaskName();
                }
            });
        }

        /** {@inheritDoc} */
        @Override public String reduce(List<GridComputeJobResult> results) throws GridException {
            return F.first(results).getData();
        }
    }
}
