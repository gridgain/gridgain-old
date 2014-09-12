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

package org.gridgain.grid.kernal.processors.hadoop;

import org.apache.hadoop.conf.*;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.mapreduce.*;
import org.gridgain.grid.hadoop.*;
import org.gridgain.grid.kernal.processors.hadoop.jobtracker.*;
import org.gridgain.grid.util.typedef.internal.*;

import static org.gridgain.grid.hadoop.GridHadoopJobPhase.*;
import static org.gridgain.grid.hadoop.GridHadoopJobState.*;

/**
 * Hadoop utility methods.
 */
public class GridHadoopUtils {
    /** Speculative concurrency on this machine. Mimics default public pool size calculation. */
    public static final int SPECULATIVE_CONCURRENCY = Math.min(8, Runtime.getRuntime().availableProcessors() * 2);

    /** Staging constant. */
    private static final String STAGING_CONSTANT = ".staging";

    /** Step span. */
    private static final long STEP_SPAN = 1000L;

    /** Minimum possible amount of steps giving 50% of remaining progress. */
    private static final int MIN_STEPS_PER_HALF_PROGRESS = 5;

    /** Range of possible amount of steps giving 50% of remaining progress. Gives [5 .. 50] range. */
    private static final int STEPS_PER_HALF_PROGRESS_RANGE = 45;

    /**
     * Convert Hadoop job metadata to job status.
     *
     * @param meta Metadata.
     * @return Status.
     */
    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    public static GridHadoopJobStatus status(GridHadoopJobMetadata meta) {
        GridHadoopDefaultJobInfo jobInfo = (GridHadoopDefaultJobInfo)meta.jobInfo();

        return new GridHadoopJobStatus(
            meta.jobId(),
            meta.phase() == PHASE_COMPLETE ? meta.failCause() == null ? STATE_SUCCEEDED : STATE_FAILED : STATE_RUNNING,
            jobInfo.configuration().getJobName(),
            jobInfo.configuration().getUser(),
            meta.pendingSplits() != null ? meta.pendingSplits().size() : 0,
            meta.pendingReducers() != null ? meta.pendingReducers().size() : 0,
            meta.mapReducePlan().mappers(),
            meta.mapReducePlan().reducers(),
            meta.startTimestamp(),
            meta.setupCompleteTimestamp(),
            meta.mapCompleteTimestamp(),
            meta.phase(),
            SPECULATIVE_CONCURRENCY,
            meta.version()
        );
    }

    /**
     * Convert GG job status to Hadoop job status.
     *
     * @param status GG job status.
     * @return Hadoop job status.
     */
    public static JobStatus status(GridHadoopJobStatus status, Configuration conf) {
        JobID jobId = new JobID(status.jobId().globalId().toString(), status.jobId().localId());

        JobStatus.State state;

        switch (status.jobState()) {
            case STATE_RUNNING:
                state = JobStatus.State.RUNNING;

                break;

            case STATE_SUCCEEDED:
                state = JobStatus.State.SUCCEEDED;

                break;

            case STATE_FAILED:
                state = JobStatus.State.FAILED;

                break;

            default:
                assert status.jobState() == STATE_KILLED;

                state = JobStatus.State.KILLED;
        }

        float setupProgress;
        float mapProgress;
        float reduceProgress;
        float cleanupProgress;

        switch (status.jobPhase()) {
            case PHASE_SETUP:
                setupProgress = setupProgress(status);
                mapProgress = 0.0f;
                reduceProgress = 0.0f;
                cleanupProgress = 0.0f;

                break;

            case PHASE_MAP:
                setupProgress = 1.0f;
                mapProgress = mapProgress(status);
                reduceProgress = 0.0f;
                cleanupProgress = 0.0f;

                break;

            case PHASE_REDUCE:
                setupProgress = 1.0f;
                mapProgress = 1.0f;
                reduceProgress = reduceProgress(status);
                cleanupProgress = 0.0f;

                break;

            case PHASE_CANCELLING:
                // Do not know where cancel occurred => calculate setup/map/reduce progress.
                setupProgress = setupProgress(status);
                mapProgress = mapProgress(status);
                reduceProgress = reduceProgress(status);
                cleanupProgress = 0.0f;

                break;

            default:
                assert status.jobPhase() == PHASE_COMPLETE;

                // Do not know whether this is complete on success or failure => calculate setup/map/reduce progress.
                setupProgress = setupProgress(status);
                mapProgress = mapProgress(status);
                reduceProgress = reduceProgress(status);
                cleanupProgress = 1.0f;
        }

        return new JobStatus(jobId, setupProgress, mapProgress, reduceProgress, cleanupProgress, state,
            JobPriority.NORMAL, status.user(), status.jobName(), jobFile(conf, status.user(), jobId).toString(), "N/A");
    }

    /**
     * Gets staging area directory.
     *
     * @param conf Configuration.
     * @param usr User.
     * @return Staging area directory.
     */
    public static Path stagingAreaDir(Configuration conf, String usr) {
        return new Path(conf.get(MRJobConfig.MR_AM_STAGING_DIR, MRJobConfig.DEFAULT_MR_AM_STAGING_DIR)
            + Path.SEPARATOR + usr + Path.SEPARATOR + STAGING_CONSTANT);
    }

    /**
     * Gets job file.
     *
     * @param conf Configuration.
     * @param usr User.
     * @param jobId Job ID.
     * @return Job file.
     */
    public static Path jobFile(Configuration conf, String usr, JobID jobId) {
        return new Path(stagingAreaDir(conf, usr), jobId.toString() + Path.SEPARATOR + MRJobConfig.JOB_CONF_FILE);
    }

    /**
     * Calculate setup progress.
     *
     * @param status Status.
     * @return Setup progress.
     */
    private static float setupProgress(GridHadoopJobStatus status) {
        // Map phase was started => setup had been finished.
        if (status.mapStartTime() > 0)
            return 1.0f;

        return progress(1, 0, 1, status.setupStartTime());
    }

    /**
     * Calculate map progress.
     *
     * @param status Status.
     * @return Map progress.
     */
    private static float mapProgress(GridHadoopJobStatus status) {
        // Reduce phase was started => map had been finished.
        if (status.reduceStartTime() > 0)
            return 1.0f;

        return progress(status.totalMapperCnt(), status.totalMapperCnt() - status.pendingMapperCnt(),
            Math.min(status.totalMapperCnt(), status.concurrencyLevel()), status.mapStartTime());
    }

    /**
     * Calculate reduce progress.
     *
     * @param status Status.
     * @return Reduce progress.
     */
    private static float reduceProgress(GridHadoopJobStatus status) {
        // Reduce has net started yet => no progress.
        if (status.reduceStartTime() == 0)
            return 0.0f;

        return progress(status.totalReducerCnt(), status.totalReducerCnt() - status.pendingReducerCnt(),
            Math.min(status.totalReducerCnt(), status.concurrencyLevel()), status.reduceStartTime());
    }

    /**
     * Calculate progress.
     *
     * @param totalTasks Total tasks.
     * @param completedTasks Completed tasks.
     * @param maxConcurrentTasks Maximum possible number of concurrent tasks.
     * @param startTime Start time.
     * @return Progress.
     */
    private static float progress(int totalTasks, int completedTasks, int maxConcurrentTasks, long startTime) {
        // Fast-path when all tasks are completed.
        if (totalTasks == 0 || totalTasks == completedTasks)
            return 1.0f;
        else {
            assert maxConcurrentTasks <= totalTasks;

            int concurrentTasks = Math.min(totalTasks - completedTasks, maxConcurrentTasks);

            long dur = U.currentTimeMillis() - startTime;

            float speculativeProgress = speculativeProgress(totalTasks, maxConcurrentTasks, dur) * concurrentTasks;

            float res = ((float)completedTasks + speculativeProgress) / totalTasks;

            assert res <= 1.01f; // Assume that > .01f is an algorithm bug, not precision problem.

            if (res > 1.0f)
                res = 1.0f; // Protect from FP precision problems.

            return res;
        }
    }

    /**
     * Calculate speculative progress.
     *
     * @param totalTasks Total tasks.
     * @param maxConcurrentTasks Maximum possible number of concurrent tasks.
     * @param dur Duration.
     * @return Speculative progress.
     */
    private static float speculativeProgress(int totalTasks, int maxConcurrentTasks, long dur) {
        // Determine amount of steps needed to cover 50% of remaining progress.
        // More total tasks => less steps to double the progress.
        float halfRatio = (float)maxConcurrentTasks / totalTasks;

        int halfProgressSteps = MIN_STEPS_PER_HALF_PROGRESS + (int)(STEPS_PER_HALF_PROGRESS_RANGE * halfRatio);

        // Determine amount of step size half events and amount of tail steps with the same size.
        long stepSizeChanges = dur / (halfProgressSteps * STEP_SPAN);

        long tailSteps = dur / STEP_SPAN - stepSizeChanges * halfProgressSteps;

        assert halfProgressSteps * stepSizeChanges + tailSteps == dur / STEP_SPAN;

        // Calculate speculative progress.
        float power2 = (float)Math.pow(0.5, stepSizeChanges);

        return /** Sum of geom. progression 1/2 + 1/4 ... */ 1 - power2 +
            /** Relative progress of tail steps. */ (power2 / 2) * tailSteps / halfProgressSteps;
    }

    /**
     * Constructor.
     */
    private GridHadoopUtils() {
        // No-op.
    }
}
