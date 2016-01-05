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

import org.gridgain.grid.*;
import org.gridgain.grid.hadoop.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.kernal.processors.*;

/**
 * Hadoop processor.
 */
public abstract class GridHadoopProcessorAdapter extends GridProcessorAdapter {
    /**
     * @param ctx Kernal context.
     */
    protected GridHadoopProcessorAdapter(GridKernalContext ctx) {
        super(ctx);
    }

    /**
     * @return Hadoop facade.
     */
    public abstract GridHadoop hadoop();

    /**
     * @return Hadoop configuration.
     */
    public abstract GridHadoopConfiguration config();

    /**
     * @return Collection of generated IDs.
     */
    public abstract GridHadoopJobId nextJobId();

    /**
     * Submits job to job tracker.
     *
     * @param jobId Job ID to submit.
     * @param jobInfo Job info to submit.
     * @return Execution future.
     */
    public abstract GridFuture<?> submit(GridHadoopJobId jobId, GridHadoopJobInfo jobInfo);

    /**
     * Gets Hadoop job execution status.
     *
     * @param jobId Job ID to get status for.
     * @return Job execution status.
     * @throws GridException If failed.
     */
    public abstract GridHadoopJobStatus status(GridHadoopJobId jobId) throws GridException;

    /**
     * Returns Hadoop job counters.
     *
     * @param jobId Job ID to get counters for.
     * @return Job counters.
     * @throws GridException If failed.
     */
    public abstract GridHadoopCounters counters(GridHadoopJobId jobId) throws GridException;

    /**
     * Gets Hadoop job finish future.
     *
     * @param jobId Job ID.
     * @return Job finish future or {@code null}.
     * @throws GridException If failed.
     */
    public abstract GridFuture<?> finishFuture(GridHadoopJobId jobId) throws GridException;

    /**
     * Kills job.
     *
     * @param jobId Job ID.
     * @return {@code True} if job was killed.
     * @throws GridException If failed.
     */
    public abstract boolean kill(GridHadoopJobId jobId) throws GridException;
}
