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

package org.gridgain.loadtests.job;

import org.gridgain.grid.*;
import org.gridgain.grid.compute.*;

import java.util.*;

/**
 * Runnable with continuous task submission and result checking.
 */
public class GridJobLoadTestSubmitter implements Runnable {
    /** */
    public static final int TIMEOUT = 120000;

    /** Grid where all tasks should be submitted. */
    private final Grid grid;

    /** Params of simulated jobs. */
    private final GridJobLoadTestParams params;

    /** Time to sleep between task submissions. */
    private final long submitDelay;

    /** Submission/cancel ratio. */
    private final int cancelRate;

    /** List of futures for submitted tasks. */
    private final List<GridComputeTaskFuture<Integer>> futures = new LinkedList<>();

    /** Counter to implement fixed submit/cancel ratio. */
    private int iteration;

    /**
     * @param grid Grid where all tasks should be submitted.
     * @param params Params of simulated jobs.
     * @param cancelRate Submission/cancel ratio.
     * @param submitDelay Time to sleep between task submissions.
     */
    public GridJobLoadTestSubmitter(Grid grid, GridJobLoadTestParams params, int cancelRate, long submitDelay) {
        this.grid = grid;
        this.params = params;
        this.cancelRate = cancelRate;
        this.submitDelay = submitDelay;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("BusyWait")
    @Override public void run() {
        while (true) {
            checkCompletion();

            performCancel();

            try {
                Thread.sleep(submitDelay);
            }
            catch (InterruptedException ignored) {
                return;
            }

            futures.add(grid.compute().withTimeout(TIMEOUT).execute(GridJobLoadTestTask.class, params));
        }
    }

    /**
     * Finds completed tasks in the queue and removes them.
     */
    private void checkCompletion() {
        for (Iterator<GridComputeTaskFuture<Integer>> iter = futures.iterator(); iter.hasNext();) {
            GridComputeTaskFuture<Integer> fut = iter.next();

            if (fut.isDone()) {
                try {
                    Integer res = fut.get();

                    assert res == params.getJobsCount() :
                        "Task returned wrong result [taskIs=" + fut.getTaskSession().getId() + ", result=" + res + "]";

                    grid.log().info(">>> Task completed successfully. Task id: " + fut.getTaskSession().getId());
                }
                catch (GridFutureCancelledException ignored) {
                    grid.log().info(">>> Task cancelled: " + fut.getTaskSession().getId());
                }
                catch (GridException e) {
                    grid.log().warning(
                        ">>> Get operation for completed task failed: " + fut.getTaskSession().getId(), e);
                }
                finally {
                    iter.remove();
                }
            }
        }
    }

    /**
     * Cancel a random task when required.
     */
    private void performCancel() {
        iteration++;

        if (iteration % cancelRate == 0) {
            // Here we should have mostly running futures so just pick one.
            GridComputeTaskFuture<Integer> futToCancel = futures.get( new Random().nextInt(futures.size()) );

            try {
                futToCancel.cancel();
                grid.log().info("Task canceled: " + futToCancel.getTaskSession().getId());
            }
            catch (GridException e) {
                grid.log().warning(">>> Future cancellation failed: " + futToCancel.getTaskSession().getId(), e);
            }
        }
    }
}
