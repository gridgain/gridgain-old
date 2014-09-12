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

package org.gridgain.grid.spi.discovery;

import org.gridgain.grid.*;
import org.gridgain.testframework.junits.common.*;

import java.io.*;
import java.util.*;

/**
 * Grid discovery metrics test.
 */
@GridCommonTest(group = "Utils")
public class GridDiscoveryMetricsHelperSelfTest extends GridCommonAbstractTest {
    /** */
    private static final int METRICS_COUNT = 500;

    /** */
    public GridDiscoveryMetricsHelperSelfTest() {
        super(false /*don't start grid*/);
    }

    /** */
    public void testMetricsSize() {
        byte[] data = new byte[GridDiscoveryMetricsHelper.METRICS_SIZE];

        // Test serialization.
        int off = GridDiscoveryMetricsHelper.serialize(data, 0, createMetrics());

        assert off == GridDiscoveryMetricsHelper.METRICS_SIZE;

        // Test deserialization.
        GridNodeMetrics res = GridDiscoveryMetricsHelper.deserialize(data, 0);

        assert res != null;
    }

    /** */
    public void testSerialization() {
        byte[] data = new byte[GridDiscoveryMetricsHelper.METRICS_SIZE];

        GridNodeMetrics metrics1 = createMetrics();

        // Test serialization.
        int off = GridDiscoveryMetricsHelper.serialize(data, 0, metrics1);

        assert off == GridDiscoveryMetricsHelper.METRICS_SIZE;

        // Test deserialization.
        GridNodeMetrics metrics2 = GridDiscoveryMetricsHelper.deserialize(data, 0);

        assert metrics2 != null;

        assert metrics1.equals(metrics2);
    }

    /**
     * @throws IOException If I/O error occurs.
     */
    public void testMultipleMetricsSerialization() throws IOException {
        Map<UUID, GridNodeMetrics> metrics = new HashMap<>(METRICS_COUNT);

        for (int i = 0; i < METRICS_COUNT; i++)
            metrics.put(UUID.randomUUID(), createMetrics());

        ByteArrayOutputStream bos = new ByteArrayOutputStream(1024 * 1024);

        ObjectOutputStream oos = new ObjectOutputStream(bos);

        oos.writeObject(metrics);

        oos.close();

        info(">>> Size of metrics map <UUID, GridNodeMetrics> in KB [metricsCount=" + METRICS_COUNT +
            ", size=" + bos.size() / 1024.0 + ']');
    }

    /**
     * @return Test metrics.
     */
    private GridNodeMetrics createMetrics() {
        GridDiscoveryMetricsAdapter metrics = new GridDiscoveryMetricsAdapter();

        metrics.setAvailableProcessors(1);
        metrics.setAverageActiveJobs(2);
        metrics.setAverageCancelledJobs(3);
        metrics.setAverageJobExecuteTime(4);
        metrics.setAverageJobWaitTime(5);
        metrics.setAverageRejectedJobs(6);
        metrics.setAverageWaitingJobs(7);
        metrics.setCurrentActiveJobs(8);
        metrics.setCurrentCancelledJobs(9);
        metrics.setCurrentIdleTime(10);
        metrics.setCurrentIdleTime(11);
        metrics.setCurrentJobExecuteTime(12);
        metrics.setCurrentJobWaitTime(13);
        metrics.setCurrentRejectedJobs(14);
        metrics.setCurrentWaitingJobs(15);
        metrics.setCurrentDaemonThreadCount(16);
        metrics.setHeapMemoryCommitted(17);
        metrics.setHeapMemoryInitialized(18);
        metrics.setHeapMemoryMaximum(19);
        metrics.setHeapMemoryUsed(20);
        metrics.setLastUpdateTime(21);
        metrics.setMaximumActiveJobs(22);
        metrics.setMaximumCancelledJobs(23);
        metrics.setMaximumJobExecuteTime(24);
        metrics.setMaximumJobWaitTime(25);
        metrics.setMaximumRejectedJobs(26);
        metrics.setMaximumWaitingJobs(27);
        metrics.setNonHeapMemoryCommitted(28);
        metrics.setNonHeapMemoryInitialized(29);
        metrics.setNonHeapMemoryMaximum(30);
        metrics.setNonHeapMemoryUsed(31);
        metrics.setMaximumThreadCount(32);
        metrics.setStartTime(33);
        metrics.setCurrentCpuLoad(34);
        metrics.setCurrentThreadCount(35);
        metrics.setTotalCancelledJobs(36);
        metrics.setTotalExecutedJobs(37);
        metrics.setTotalIdleTime(38);
        metrics.setTotalRejectedJobs(39);
        metrics.setTotalStartedThreadCount(40);
        metrics.setUpTime(41);
        metrics.setSentMessagesCount(42);
        metrics.setSentBytesCount(43);
        metrics.setReceivedMessagesCount(44);
        metrics.setReceivedBytesCount(45);
        metrics.setOutboundMessagesQueueSize(46);

        return metrics;
    }
}
