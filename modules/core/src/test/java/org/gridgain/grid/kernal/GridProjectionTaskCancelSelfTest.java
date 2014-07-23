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
import org.gridgain.testframework.*;
import org.gridgain.testframework.junits.common.*;

import java.util.*;
import java.util.concurrent.*;

/**
 * Test for {@link GridCompute#cancelTask(GridUuid)}.
 */
public class GridProjectionTaskCancelSelfTest extends GridCommonAbstractTest {
    /** Number fo nodes to run in this test. */
    private static final int NODES_CNT = 3;

    /** */
    private static volatile CountDownLatch finishJobs;

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        super.beforeTestsStarted();

        startGrids(NODES_CNT);
    }

    /** {@inheritDoc} */
    @Override protected void afterTestsStopped() throws Exception {
        stopAllGrids();

        super.afterTestsStopped();
    }

    /**
     * @throws Exception If failed.
     */
    public void testLocalCancel() throws Exception {
        finishJobs = new CountDownLatch(1);

        final GridComputeTaskFuture<String> fut = grid(0).compute().execute(TestTask.class, null);

        grid(0).compute().cancelTask(fut.getTaskSession().getId());

        finishJobs.countDown();

        GridTestUtils.assertThrows(null, new Callable<Object>() {
            @Override public Object call() throws Exception {
                return fut.get();
            }
        }, GridFutureCancelledException.class, null);

        assert fut.isCancelled();
    }

    /**
     * @throws Exception If failed.
     */
    public void testRemoteCancel() throws Exception {
        finishJobs = new CountDownLatch(1);

        final GridComputeTaskFuture<String> fut = grid(0).compute().execute(TestTask.class, finishJobs);

        grid(1).compute().cancelTask(fut.getTaskSession().getId());

        GridTestUtils.assertThrows(null, new Callable<Object>() {
            @Override public Object call() throws Exception {
                return fut.get();
            }
        }, GridFutureCancelledException.class, null);

        finishJobs.countDown();

        assert fut.isCancelled();
    }

    /**
     * @throws Exception If failed.
     */
    public void testLocalCancelThroughtRemoteNode() throws Exception {
        finishJobs = new CountDownLatch(1);

        final GridComputeTaskFuture<String> fut = grid(0).compute().execute(TestTask.class, finishJobs);

        // Projection without master node.
        GridProjection p = grid(0).forOthers(grid(0).localNode());

        p.compute().cancelTask(fut.getTaskSession().getId());

        GridTestUtils.assertThrows(null, new Callable<Object>() {
            @Override public Object call() throws Exception {
                return fut.get();
            }
        }, GridFutureCancelledException.class, null);

        finishJobs.countDown();

        assert fut.isCancelled();
    }

    /**
     * Test task for this test.
     */
    private static class TestTask extends GridComputeTaskSplitAdapter<Object, String> {
        /** Successful return value. */
        public static final String SUCCESS = "Success";

        /** {@inheritDoc} */
        @Override protected Collection<? extends GridComputeJob> split(int gridSize, final Object arg) throws GridException {
            Collection<GridComputeJob> jobs = new ArrayList<>(NODES_CNT);

            for (int i = 0; i < NODES_CNT; i++)
                jobs.add(new GridComputeJobAdapter() {
                    @Override public Object execute() {
                        try {
                            finishJobs.await();
                        }
                        catch (InterruptedException ignored) {
                            return null;
                        }

                        return SUCCESS;
                    }
                });

            return jobs;
        }

        /** {@inheritDoc} */
        @Override public String reduce(List<GridComputeJobResult> results) throws GridException {
            assert results.size() == NODES_CNT;

            for (GridComputeJobResult r : results)
                assert r.getData().equals(SUCCESS);

            return SUCCESS;
        }
    }
}
