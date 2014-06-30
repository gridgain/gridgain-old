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
import org.gridgain.grid.kernal.processors.task.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.messaging.*;
import org.gridgain.testframework.junits.common.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import static org.gridgain.grid.events.GridEventType.*;

/**
 * Grid node metrics self test.
 */
@GridCommonTest(group = "Kernal Self")
public class GridNodeMetricsSelfTest extends GridCommonAbstractTest {
    /** Test message size. */
    private static final int MSG_SIZE = 1024;

    /** Number of messages. */
    private static final int MSG_CNT = 3;

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        startGrid();
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        stopAllGrids();
    }

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        cfg.setCacheConfiguration();
        cfg.setMetricsUpdateFrequency(0);

        return cfg;
    }

    /**
     * @throws Exception If failed.
     */
    public void testSingleTaskMetrics() throws Exception {
        Grid grid = grid();

        grid.compute().execute(new GridTestTask(), "testArg").get();

        // Let metrics update twice.
        final CountDownLatch latch = new CountDownLatch(2);

        grid.events().localListen(new GridPredicate<GridEvent>() {
            @Override public boolean apply(GridEvent evt) {
                assert evt.type() == EVT_NODE_METRICS_UPDATED;

                latch.countDown();

                return true;
            }
        }, EVT_NODE_METRICS_UPDATED);

        // Wait for metrics update.
        latch.await();

        GridNodeMetrics metrics = grid.localNode().metrics();

        info("Node metrics: " + metrics);

        assert metrics.getAverageActiveJobs() > 0;
        assert metrics.getAverageCancelledJobs() == 0;
        assert metrics.getAverageJobExecuteTime() >= 0;
        assert metrics.getAverageJobWaitTime() >= 0;
        assert metrics.getAverageRejectedJobs() == 0;
        assert metrics.getAverageWaitingJobs() == 0;
        assert metrics.getCurrentActiveJobs() == 0;
        assert metrics.getCurrentCancelledJobs() == 0;
        assert metrics.getCurrentJobExecuteTime() == 0;
        assert metrics.getCurrentJobWaitTime() == 0;
        assert metrics.getCurrentWaitingJobs() == 0;
        assert metrics.getMaximumActiveJobs() == 1;
        assert metrics.getMaximumCancelledJobs() == 0;
        assert metrics.getMaximumJobExecuteTime() >= 0;
        assert metrics.getMaximumJobWaitTime() >= 0;
        assert metrics.getMaximumRejectedJobs() == 0;
        assert metrics.getMaximumWaitingJobs() == 0;
        assert metrics.getTotalCancelledJobs() == 0;
        assert metrics.getTotalExecutedJobs() == 1;
        assert metrics.getTotalRejectedJobs() == 0;
        assert metrics.getTotalExecutedTasks() == 1;

        assertTrue("MaximumJobExecuteTime=" + metrics.getMaximumJobExecuteTime() +
            " is less than AverageJobExecuteTime=" + metrics.getAverageJobExecuteTime(),
            metrics.getMaximumJobExecuteTime() >= metrics.getAverageJobExecuteTime());
    }

    /**
     * @throws Exception If failed.
     */
    public void testInternalTaskMetrics() throws Exception {
        Grid grid = grid();

        // Visor task is internal and should not affect metrics.
        grid.compute().withName("visor-test-task").execute(new TestInternalTask(), "testArg").get();

        // Let metrics update twice.
        final CountDownLatch latch = new CountDownLatch(2);

        grid.events().localListen(new GridPredicate<GridEvent>() {
            @Override public boolean apply(GridEvent evt) {
                assert evt.type() == EVT_NODE_METRICS_UPDATED;

                latch.countDown();

                return true;
            }
        }, EVT_NODE_METRICS_UPDATED);

        // Wait for metrics update.
        latch.await();

        GridNodeMetrics metrics = grid.localNode().metrics();

        info("Node metrics: " + metrics);

        assert metrics.getAverageActiveJobs() == 0;
        assert metrics.getAverageCancelledJobs() == 0;
        assert metrics.getAverageJobExecuteTime() == 0;
        assert metrics.getAverageJobWaitTime() == 0;
        assert metrics.getAverageRejectedJobs() == 0;
        assert metrics.getAverageWaitingJobs() == 0;
        assert metrics.getCurrentActiveJobs() == 0;
        assert metrics.getCurrentCancelledJobs() == 0;
        assert metrics.getCurrentJobExecuteTime() == 0;
        assert metrics.getCurrentJobWaitTime() == 0;
        assert metrics.getCurrentWaitingJobs() == 0;
        assert metrics.getMaximumActiveJobs() == 0;
        assert metrics.getMaximumCancelledJobs() == 0;
        assert metrics.getMaximumJobExecuteTime() == 0;
        assert metrics.getMaximumJobWaitTime() == 0;
        assert metrics.getMaximumRejectedJobs() == 0;
        assert metrics.getMaximumWaitingJobs() == 0;
        assert metrics.getTotalCancelledJobs() == 0;
        assert metrics.getTotalExecutedJobs() == 0;
        assert metrics.getTotalRejectedJobs() == 0;
        assert metrics.getTotalExecutedTasks() == 0;

        assertTrue("MaximumJobExecuteTime=" + metrics.getMaximumJobExecuteTime() +
            " is less than AverageJobExecuteTime=" + metrics.getAverageJobExecuteTime(),
            metrics.getMaximumJobExecuteTime() >= metrics.getAverageJobExecuteTime());
    }

    /**
     * @throws Exception If failed.
     */
    public void testIoMetrics() throws Exception {
        Grid grid0 = grid();
        Grid grid1 = startGrid(1);

        Object msg = new TestMessage();

        int size = grid0.configuration().getMarshaller().marshal(msg).length;

        assert size > MSG_SIZE;

        final CountDownLatch latch = new CountDownLatch(MSG_CNT);

        grid0.message().localListen(null, new GridMessagingListenActor<TestMessage>() {
            @Override protected void receive(UUID nodeId, TestMessage rcvMsg) throws Throwable {
                latch.countDown();
            }
        });

        grid1.message().localListen(null, new GridMessagingListenActor<TestMessage>() {
            @Override protected void receive(UUID nodeId, TestMessage rcvMsg) throws Throwable {
                respond(rcvMsg);
            }
        });

        for (int i = 0; i < MSG_CNT; i++)
            grid0.forRemotes().message().send(null, msg);

        latch.await();

        GridNodeMetrics metrics = grid0.localNode().metrics();

        info("Node 0 metrics: " + metrics);

        // Time sync messages are being sent.
        assert metrics.getSentMessagesCount() >= MSG_CNT;
        assert metrics.getSentBytesCount() > size * MSG_CNT;
        assert metrics.getReceivedMessagesCount() >= MSG_CNT;
        assert metrics.getReceivedBytesCount() > size * MSG_CNT;

        metrics = grid1.localNode().metrics();

        info("Node 1 metrics: " + metrics);

        // Time sync messages are being sent.
        assert metrics.getSentMessagesCount() >= MSG_CNT;
        assert metrics.getSentBytesCount() > size * MSG_CNT;
        assert metrics.getReceivedMessagesCount() >= MSG_CNT;
        assert metrics.getReceivedBytesCount() > size * MSG_CNT;
    }

    /**
     * Test message.
     */
    @SuppressWarnings("UnusedDeclaration")
    private static class TestMessage implements Serializable {
        /** */
        private final byte[] arr = new byte[MSG_SIZE];
    }

    /**
     * Test internal task.
     */
    @GridInternal
    private static class TestInternalTask extends GridTestTask {
        // No-op.
    }
}
