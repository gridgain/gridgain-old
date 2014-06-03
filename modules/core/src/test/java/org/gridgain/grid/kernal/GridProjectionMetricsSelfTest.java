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
import org.gridgain.grid.events.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.testframework.junits.common.*;

import java.util.*;
import java.util.concurrent.*;

import static org.gridgain.grid.events.GridEventType.*;

/**
 * Tests for projection metrics.
 */
@GridCommonTest(group = "Kernal Self")
public class GridProjectionMetricsSelfTest extends GridCommonAbstractTest {
    /** */
    private static final int NODES_CNT = 4;

    /** */
    private static final int ITER_CNT = 30;

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        for (int i = 0; i < NODES_CNT; i++)
            startGrid(i);
    }

    /** {@inheritDoc} */
    @Override protected void afterTestsStopped() throws Exception {
        stopAllGrids();
    }

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        cfg.setCacheConfiguration();
        cfg.setIncludeProperties();
        cfg.setMetricsUpdateFrequency(0);

        return cfg;
    }

    /**
     * @throws Exception In case of error.
     */
    public void testEmptyProjection() throws Exception {
        try {
            grid(0).forPredicate(F.<GridNode>alwaysFalse()).metrics();

            assert false;
        }
        catch (GridEmptyProjectionException e) {
            info("Caught expected exception: " + e);
        }
    }

    /**
     *
     */
    public void testTaskExecution() {
        for (int i = 0; i < ITER_CNT; i++) {
            info("Starting new iteration: " + i);

            try {
                performTaskExecutionTest();
            }
            catch (Throwable t) {
                error("Iteration failed: " + i, t);

                fail("Test failed (see logs for details).");
            }
        }
    }

    /**
     * @throws Exception In case of error.
     */
    private void performTaskExecutionTest() throws Exception {
        Grid g = grid(0);

        JobFinishLock jobFinishLock = new JobFinishLock();

        MetricsUpdateLock metricsUpdLock = new MetricsUpdateLock();

        try {
            for (Grid g0 : G.allGrids())
                g0.events().localListen(jobFinishLock, EVT_JOB_FINISHED);

            g.compute().execute(new GridTestTask(), "testArg").get();

            // Wait until all nodes fire JOB FINISH event.
            jobFinishLock.await();

            g.events().localListen(metricsUpdLock, EVT_NODE_METRICS_UPDATED);

            // Wait until local node will have updated metrics.
            metricsUpdLock.await();

            GridProjectionMetrics m = g.metrics();

            checkMetrics(m);
        }
        finally {
            for (Grid g0 : G.allGrids())
                g0.events().stopLocalListen(jobFinishLock);

            g.events().stopLocalListen(metricsUpdLock);
        }
    }

    /**
     * @param m Metrics.
     */
    @SuppressWarnings({"FloatingPointEquality"})
    private void checkMetrics(GridProjectionMetrics m) {
        assert m.getTotalNodes() == NODES_CNT;
        assert m.getTotalHosts() == 1;

        assert m.getMinimumActiveJobs() == 0;
        assert m.getMaximumActiveJobs() == 0;
        assert m.getAverageActiveJobs() == 0;

        assert m.getMinimumCancelledJobs() == 0;
        assert m.getMaximumCancelledJobs() == 0;
        assert m.getAverageCancelledJobs() == 0;

        assert m.getMinimumRejectedJobs() == 0;
        assert m.getMaximumRejectedJobs() == 0;
        assert m.getAverageRejectedJobs() == 0;

        assert m.getMinimumWaitingJobs() == 0;
        assert m.getMaximumWaitingJobs() == 0;
        assert m.getAverageWaitingJobs() == 0;

        assert m.getMinimumJobExecuteTime() >= 0;
        assert m.getMaximumJobExecuteTime() >= 0;
        assert m.getAverageJobExecuteTime() >= 0;

        assert m.getAverageJobExecuteTime() >= m.getMinimumJobExecuteTime() &&
            m.getAverageJobExecuteTime() <= m.getMaximumJobExecuteTime();

        assert m.getMinimumJobWaitTime() >= 0;
        assert m.getMaximumJobWaitTime() >= 0;
        assert m.getAverageJobWaitTime() >= 0;

        assert m.getAverageJobWaitTime() >= m.getMinimumJobWaitTime() &&
            m.getAverageJobWaitTime() <= m.getMaximumJobWaitTime();

        assert m.getMinimumDaemonThreadCount() > 0;
        assert m.getMaximumDaemonThreadCount() > 0;
        assert m.getAverageDaemonThreadCount() > 0;

        assert m.getAverageDaemonThreadCount() >= m.getMinimumDaemonThreadCount() &&
            m.getAverageDaemonThreadCount() <= m.getMaximumDaemonThreadCount();

        assert m.getMinimumThreadCount() > 0;
        assert m.getMaximumThreadCount() > 0;
        assert m.getAverageThreadCount() > 0;

        assert m.getAverageThreadCount() >= m.getMinimumThreadCount() &&
            m.getAverageThreadCount() <= m.getMaximumThreadCount();

        assert m.getMinimumIdleTime() >= 0;
        assert m.getMaximumIdleTime() >= 0;
        assert m.getAverageIdleTime() >= 0;
        assert m.getIdleTimePercentage() >= 0;
        assert m.getIdleTimePercentage() <= 1;

        assert m.getAverageIdleTime() >= m.getMinimumIdleTime() && m.getAverageIdleTime() <= m.getMaximumIdleTime();

        assert m.getMinimumBusyTimePercentage() > 0;
        assert m.getMaximumBusyTimePercentage() > 0;
        assert m.getAverageBusyTimePercentage() > 0;

        assert m.getAverageBusyTimePercentage() >= m.getMinimumBusyTimePercentage() &&
            m.getAverageBusyTimePercentage() <= m.getMaximumBusyTimePercentage();

        assert m.getMinimumCpuLoad() >= 0 || m.getMinimumCpuLoad() == -1.0;
        assert m.getMaximumCpuLoad() >= 0 || m.getMaximumCpuLoad() == -1.0;
        assert m.getAverageCpuLoad() >= 0 || m.getAverageCpuLoad() == -1.0;

        assert m.getAverageCpuLoad() >= m.getMinimumCpuLoad() && m.getAverageCpuLoad() <= m.getMaximumCpuLoad();

        assert m.getMinimumHeapMemoryCommitted() > 0;
        assert m.getMaximumHeapMemoryCommitted() > 0;
        assert m.getAverageHeapMemoryCommitted() > 0;

        assert m.getAverageHeapMemoryCommitted() >= m.getMinimumHeapMemoryCommitted() &&
            m.getAverageHeapMemoryCommitted() <= m.getMaximumHeapMemoryCommitted();

        assert m.getMinimumHeapMemoryUsed() > 0;
        assert m.getMaximumHeapMemoryUsed() > 0;
        assert m.getAverageHeapMemoryUsed() > 0;

        assert m.getAverageHeapMemoryUsed() >= m.getMinimumHeapMemoryUsed() &&
            m.getAverageHeapMemoryUsed() <= m.getMaximumHeapMemoryUsed();

        assert m.getMinimumHeapMemoryMaximum() > 0;
        assert m.getMaximumHeapMemoryMaximum() > 0;
        assert m.getAverageHeapMemoryMaximum() > 0;

        assert m.getAverageHeapMemoryMaximum() >= m.getMinimumHeapMemoryMaximum() &&
            m.getAverageHeapMemoryMaximum() <= m.getMaximumHeapMemoryMaximum();

        assert m.getMinimumHeapMemoryInitialized() >= 0;
        assert m.getMaximumHeapMemoryInitialized() >= 0;
        assert m.getAverageHeapMemoryInitialized() >= 0;

        assert m.getAverageHeapMemoryInitialized() >= m.getMinimumHeapMemoryInitialized() &&
            m.getAverageHeapMemoryInitialized() <= m.getMaximumHeapMemoryInitialized();

        assert m.getMinimumNonHeapMemoryCommitted() > 0;
        assert m.getMaximumNonHeapMemoryCommitted() > 0;
        assert m.getAverageNonHeapMemoryCommitted() > 0;

        assert m.getAverageNonHeapMemoryCommitted() >= m.getMinimumNonHeapMemoryCommitted() &&
            m.getAverageNonHeapMemoryCommitted() <= m.getMaximumNonHeapMemoryCommitted();

        assert m.getMinimumNonHeapMemoryUsed() > 0;
        assert m.getMaximumNonHeapMemoryUsed() > 0;
        assert m.getAverageNonHeapMemoryUsed() > 0;

        assert m.getAverageNonHeapMemoryUsed() >= m.getMinimumNonHeapMemoryUsed() &&
            m.getAverageNonHeapMemoryUsed() <= m.getMaximumNonHeapMemoryUsed();

        assert m.getMinimumNonHeapMemoryMaximum() > 0;
        assert m.getMaximumNonHeapMemoryMaximum() > 0;
        assert m.getAverageNonHeapMemoryMaximum() > 0;

        assert m.getAverageNonHeapMemoryMaximum() >= m.getMinimumNonHeapMemoryMaximum() &&
            m.getAverageNonHeapMemoryMaximum() <= m.getMaximumNonHeapMemoryMaximum();

        assert m.getMinimumNonHeapMemoryInitialized() > 0;
        assert m.getMaximumNonHeapMemoryInitialized() > 0;
        assert m.getAverageNonHeapMemoryInitialized() > 0;

        assert m.getAverageNonHeapMemoryInitialized() >= m.getMinimumNonHeapMemoryInitialized() &&
            m.getAverageNonHeapMemoryInitialized() <= m.getMaximumNonHeapMemoryInitialized();

        assert m.getYoungestNodeStartTime() > 0;
        assert m.getOldestNodeStartTime() > 0;

        assert m.getYoungestNodeStartTime() > m.getOldestNodeStartTime();

        assert m.getMinimumUpTime() > 0;
        assert m.getMaximumUpTime() > 0;
        assert m.getAverageUpTime() > 0;

        assert m.getAverageUpTime() >= m.getMinimumUpTime() && m.getAverageUpTime() <= m.getMaximumUpTime();

        assert m.getMinimumCpusPerNode() > 0;
        assert m.getMaximumCpusPerNode() > 0;
        assert m.getAverageCpusPerNode() > 0;

        assert m.getAverageCpusPerNode() == m.getMinimumCpusPerNode() &&
            m.getAverageCpusPerNode() == m.getMaximumCpusPerNode();

        assert m.getMinimumNodesPerHost() == NODES_CNT;
        assert m.getMaximumNodesPerHost() == NODES_CNT;
        assert m.getAverageNodesPerHost() == NODES_CNT;

        assert m.getTotalCpus() > 0;
        assert m.getTotalHosts() == 1;
        assert m.getTotalNodes() == NODES_CNT;
    }

    /**
     *
     */
    private static class JobFinishLock implements GridPredicate<GridEvent> {
        /** Latch. */
        private final CountDownLatch latch = new CountDownLatch(NODES_CNT);

        /** {@inheritDoc} */
        @Override public boolean apply(GridEvent evt) {
            assert evt.type() == EVT_JOB_FINISHED;

            latch.countDown();

            return true;
        }

        /**
         * Waits until all nodes fire EVT_JOB_FINISHED.
         *
         * @throws InterruptedException If interrupted.
         */
        public void await() throws InterruptedException {
            latch.await();
        }
    }

    /**
     *
     */
    private static class MetricsUpdateLock implements GridPredicate<GridEvent> {
        /** Latch. */
        private final CountDownLatch latch = new CountDownLatch(NODES_CNT * 2);

        /** */
        private final Map<UUID, Integer> metricsRcvdCnt = new HashMap<>();

        /** {@inheritDoc} */
        @Override public boolean apply(GridEvent evt) {
            GridDiscoveryEvent discoEvt = (GridDiscoveryEvent)evt;

            Integer cnt = F.addIfAbsent(metricsRcvdCnt, discoEvt.eventNode().id(), 0);

            assert cnt != null;

            if (cnt < 2) {
                latch.countDown();

                metricsRcvdCnt.put(discoEvt.eventNode().id(), ++cnt);
            }

            return true;
        }

        /**
         * Waits until all metrics will be received twice from all nodes in
         * topology.
         *
         * @throws InterruptedException If interrupted.
         */
        public void await() throws InterruptedException {
            latch.await();
        }
    }
}
