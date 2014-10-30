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

package org.gridgain.loadtests.direct.stealing;

import org.gridgain.grid.*;
import org.gridgain.grid.compute.*;
import org.gridgain.grid.loadtest.*;
import org.gridgain.grid.spi.collision.jobstealing.*;
import org.gridgain.grid.spi.discovery.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.failover.jobstealing.*;
import org.gridgain.testframework.*;
import org.gridgain.testframework.config.*;
import org.gridgain.testframework.junits.common.*;

import java.util.*;
import java.util.concurrent.atomic.*;

/**
 *
 */
@GridCommonTest(group = "Load Test")
public class GridStealingLoadTest extends GridCommonAbstractTest {
    /** */
    public GridStealingLoadTest() {
        super(false);
    }

    /**
     * @return Number of threads for the test.
     */
    private int getThreadCount() {
        return Integer.valueOf(GridTestProperties.getProperty("load.test.threadnum"));
    }

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        startGrids(2);
    }

    /** {@inheritDoc} */
    @Override protected void afterTestsStopped() throws Exception {
        stopAllGrids();
    }

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String name) throws Exception {
        GridConfiguration cfg = super.getConfiguration(name);

        GridJobStealingCollisionSpi colSpi = new GridJobStealingCollisionSpi();

        assert colSpi.getActiveJobsThreshold() == GridJobStealingCollisionSpi.DFLT_ACTIVE_JOBS_THRESHOLD;
        assert colSpi.getWaitJobsThreshold() == GridJobStealingCollisionSpi.DFLT_WAIT_JOBS_THRESHOLD;

        // One job at a time.
        colSpi.setActiveJobsThreshold(5);
        colSpi.setWaitJobsThreshold(0);
        colSpi.setMessageExpireTime(5000);

        GridJobStealingFailoverSpi failSpi = new GridJobStealingFailoverSpi();

        // Verify defaults.
        assert failSpi.getMaximumFailoverAttempts() == GridJobStealingFailoverSpi.DFLT_MAX_FAILOVER_ATTEMPTS;

        GridDiscoverySpi discoSpi = new GridTcpDiscoverySpi();

        cfg.setDiscoverySpi(discoSpi);
        cfg.setCollisionSpi(colSpi);
        cfg.setFailoverSpi(failSpi);

        return cfg;
    }

    /**
     * @throws Exception If failed.
     */
    @SuppressWarnings("unchecked")
    public void testStealingLoad() throws Exception {
        final Grid grid = grid(0);

        assert grid != null;

        assert !grid.forRemotes().nodes().isEmpty() : "Test requires at least 2 nodes.";

        final UUID stealingNodeId = grid.forRemotes().nodes().iterator().next().id();

        info("Set stealing node id to: " + stealingNodeId);

        grid.compute().localDeployTask(GridStealingLoadTestTask.class, GridStealingLoadTestTask.class.getClassLoader());

        final long end = 2 * 60 * 1000 + System.currentTimeMillis();

        info("Test timeout: " + getTestTimeout() + " ms.");
        info("Thread count: " + getThreadCount());

        final GridLoadTestStatistics stats = new GridLoadTestStatistics();

        final AtomicBoolean failed = new AtomicBoolean(false);

        final AtomicInteger stolen = new AtomicInteger(0);

        GridTestUtils.runMultiThreaded(new Runnable() {
            /** {@inheritDoc} */
            @Override public void run() {
                try {
                    while (end - System.currentTimeMillis() > 0) {
                        long start = System.currentTimeMillis();

                        // Pass stealing node id.
                        GridComputeTaskFuture<?> fut = grid.compute().withTimeout(20000).
                            execute(GridStealingLoadTestTask.class.getName(), stealingNodeId);

                        stolen.addAndGet((Integer)fut.get());

                        long taskCnt = stats.onTaskCompleted(fut, 1, System.currentTimeMillis() - start);

                        if (taskCnt % 500 == 0)
                            info("Stats [stats=" + stats.toString() + ", stolen=" + stolen + ']');
                    }
                }
                catch (Throwable e) {
                    error("Load test failed.", e);

                    failed.set(true);
                }
            }
        }, getThreadCount(), "grid-load-test-thread");

        info("Final test statistics: " + stats);

        if (failed.get())
            fail();

        assert stolen.get() != 0: "No jobs were stolen by stealing node.";

        info("Stolen jobs: " + stolen.get());
    }
}
