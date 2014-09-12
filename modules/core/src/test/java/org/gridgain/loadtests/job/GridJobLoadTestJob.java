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
import org.gridgain.grid.logger.*;
import org.gridgain.grid.resources.*;

import java.util.*;

import static java.lang.Thread.*;

/**
 * Job for load test.
 */
public class GridJobLoadTestJob implements GridComputeJob {
    /** Length of the sequence emitted into session attributes. */
    private static final int EMIT_SEQUENCE_LENGTH = 10;

    /** Flag indicating whether this job should emit sequence into session attributes. */
    private final boolean emitAttrs;

    /** Probability of failure. */
    private final double failProbability;

    /** Duration between job start and random failure check. */
    private final long executionDuration;

    /** Duration between failure check and returning from {@link GridJobLoadTestJob#execute()}. */
    private final int completionDelay;

    /** Logger. */
    @GridLoggerResource
    private GridLogger log;

    /** Local node id. */
    @GridLocalNodeIdResource
    private UUID nodeId;

    /** Job context. */
    @GridJobContextResource
    private GridComputeJobContext cntx;

    /** Task session. */
    @GridTaskSessionResource
    private GridComputeTaskSession taskSes;

    /**
     * @param emitAttrs if {@code true} then this work should emit number sequence into session attribute
     * @param failProbability Probability of failure.
     * @param executionDuration Duration between job start and random failure check.
     * @param completionDelay Duration between failure check and returning from
     * {@link GridJobLoadTestJob#execute()}.
     */
    public GridJobLoadTestJob(boolean emitAttrs, double failProbability, long executionDuration,
        int completionDelay) {
        this.emitAttrs = emitAttrs;
        this.failProbability = failProbability;
        this.executionDuration = executionDuration;
        this.completionDelay = completionDelay;
    }

    /**{@inheritDoc}*/
    @Override public void cancel() {
        Thread.currentThread().interrupt();
    }

    /**{@inheritDoc}*/
    @Override public Integer execute() throws GridException {
        try {
            if (log.isInfoEnabled())
                log.info("Job started " + getJobInfo());

            doJob();

            if (new Random().nextDouble() <= failProbability) {
                if (log.isInfoEnabled())
                    log.info("Failing job " + getJobInfo());

                throw new RuntimeException("Task failure simulation");
            }

            sleep(new Random().nextInt(completionDelay));

            if (log.isInfoEnabled())
                log.info("Job is completing normally " + getJobInfo());
        }
        catch (InterruptedException ignored) {
            if (log.isDebugEnabled())
                log.debug("Job was cancelled " + getJobInfo());

            // Let the method return normally.
        }

        return 1;
    }

    /**
     * Performs job actions, depending on {@code emitAttributes} and {@code executionDuration} attribute values.
     *
     * @throws InterruptedException if task was cancelled during job execution.
     */
    @SuppressWarnings("BusyWait")
    private void doJob() throws InterruptedException {
        if (emitAttrs) {
            for (int i = 0; i < EMIT_SEQUENCE_LENGTH; i++) {
                try {
                    taskSes.setAttribute(String.valueOf(i), i);
                }
                catch (GridException e) {
                    log.error("Set attribute failed.", e);
                }

                sleep(executionDuration);
            }
        }
        else {
            sleep(executionDuration);

            Map<?, ?> attrs = taskSes.getAttributes();

            boolean valMissed = false;

            for (int i = 0; i < EMIT_SEQUENCE_LENGTH; i++) {
                Integer val = (Integer) attrs.get(String.valueOf(i));

                // We shouldn't run in situation when some elements emitted before are missed and the current exists.
                assert ! (valMissed && val != null) :
                    "Inconsistent session attribute set was received [missedAttribute=" + i +
                    ", jobId=" + cntx.getJobId() + ", attrs=" + attrs + ", nodeId=" + nodeId + "]";

                valMissed = (val == null);
            }
        }
    }

    /**
     * Gives job description in standard log format.
     *
     * @return String with current job representation.
     */
    private String getJobInfo() {
        return "[taskId=" + taskSes.getId() + ", jobId=" + cntx.getJobId() + ", nodeId=" + nodeId + "]";
    }
}
