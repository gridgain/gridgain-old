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
import org.gridgain.grid.logger.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.spi.collision.jobstealing.*;
import org.gridgain.grid.spi.failover.jobstealing.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.testframework.*;
import org.gridgain.testframework.junits.common.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.*;

/**
 * Multithreaded job stealing test.
 */
@GridCommonTest(group = "Kernal Self")
public class GridMultithreadedJobStealingSelfTest extends GridCommonAbstractTest {
    /** */
    private Grid grid;

    /** */
    public GridMultithreadedJobStealingSelfTest() {
        super(false /* don't start grid*/);
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        grid = startGridsMultiThreaded(2);
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        grid = null;

        stopAllGrids();
    }

    /**
     * Test 2 jobs on 2 nodes.
     *
     * @throws Exception If test failed.
     */
    public void testTwoJobsMultithreaded() throws Exception {
        final AtomicReference<Exception> fail = new AtomicReference<>(null);

        final AtomicInteger stolen = new AtomicInteger(0);
        final AtomicInteger noneStolen = new AtomicInteger(0);

        int threadsNum = 10;

        GridTestUtils.runMultiThreaded(new Runnable() {
            /** */
            @Override public void run() {
                try {
                    JobStealingResult res = grid.compute().execute(JobStealingTask.class, null).get();

                    info("Task result: " + res);

                    switch(res) {
                        case NONE_STOLEN : {
                            noneStolen.addAndGet(2);
                            break;
                        }
                        case ONE_STOLEN : {
                            noneStolen.addAndGet(1);
                            stolen.addAndGet(1);
                            break;
                        }
                        case BOTH_STOLEN: {
                            stolen.addAndGet(2);
                            break;
                        }
                        default: {
                            assert false : "Result is: " + res;
                        }
                    }
                }
                catch (GridException e) {
                    log.error("Failed to execute task.", e);

                    fail.getAndSet(e);
                }
            }
        }, threadsNum, "JobStealingThread");

        for (Grid g : G.allGrids())
            info("Metrics [nodeId=" + g.localNode().id() +
                ", metrics=" + g.localNode().metrics() + ']');

        assert fail.get() == null : "Test failed with exception: " + fail.get();

        // Total jobs number is threadsNum * 2
        assert stolen.get() + noneStolen.get() == threadsNum * 2 : "Incorrect processed jobs number";

        assert stolen.get() != 0 : "No jobs were stolen.";

        // Under these circumstances we should not have  more than 2 jobs
        // difference.
        assert Math.abs(stolen.get() - noneStolen.get()) <= 2 : "Stats [stolen=" + stolen +
            ", noneStolen=" + noneStolen + ']';
    }

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        GridJobStealingCollisionSpi colSpi = new GridJobStealingCollisionSpi();

        // One job at a time.
        colSpi.setActiveJobsThreshold(1);
        colSpi.setWaitJobsThreshold(0);

        GridJobStealingFailoverSpi failSpi = new GridJobStealingFailoverSpi();

        // Verify defaults.
        assert failSpi.getMaximumFailoverAttempts() == GridJobStealingFailoverSpi.DFLT_MAX_FAILOVER_ATTEMPTS;

        cfg.setCollisionSpi(colSpi);
        cfg.setFailoverSpi(failSpi);

        return cfg;
    }

    /**
     * Job stealing task.
     */
    private static class JobStealingTask extends GridComputeTaskAdapter<Object, JobStealingResult> {
        /** Grid. */
        @GridInstanceResource private Grid grid;

        /** Logger. */
        @GridLoggerResource private GridLogger log;

        /** {@inheritDoc} */
        @SuppressWarnings("ForLoopReplaceableByForEach")
            @Override public Map<? extends GridComputeJob, GridNode> map(List<GridNode> subgrid,
            @Nullable Object arg) throws GridException {
            assert subgrid.size() == 2 : "Invalid subgrid size: " + subgrid.size();

            Map<GridComputeJobAdapter, GridNode> map = new HashMap<>(subgrid.size());

            // Put all jobs onto local node.
            for (int i = 0; i < subgrid.size(); i++)
                map.put(new GridJobStealingJob(2000L), grid.localNode());

            return map;
        }

        /** {@inheritDoc} */
        @Override public JobStealingResult reduce(List<GridComputeJobResult> results) throws GridException {
            assert results.size() == 2;

            for (GridComputeJobResult res : results) {
                log.info("Job result: " + res.getData());
            }

            Object obj0 = results.get(0).getData();

            if (obj0.equals(results.get(1).getData())) {
                if (obj0.equals(grid.name()))
                    return JobStealingResult.NONE_STOLEN;

                return JobStealingResult.BOTH_STOLEN;
            }

            return JobStealingResult.ONE_STOLEN;
        }
    }

    /**
     * Job stealing job.
     */
    private static final class GridJobStealingJob extends GridComputeJobAdapter {
        /** Injected grid. */
        @GridInstanceResource private Grid grid;

        /**
         * @param arg Job argument.
         */
        GridJobStealingJob(Long arg) {
            super(arg);
        }

        /** {@inheritDoc} */
        @Override public Serializable execute() throws GridException {
            try {
                Long sleep = argument(0);

                assert sleep != null;

                Thread.sleep(sleep);
            }
            catch (InterruptedException e) {
                throw new GridException("Job got interrupted.", e);
            }

            return grid.name();
        }
    }

    /**
     * Job stealing result.
     */
    private enum JobStealingResult {
        /** */
        BOTH_STOLEN,

        /** */
        ONE_STOLEN,

        /** */
        NONE_STOLEN
    }
}
