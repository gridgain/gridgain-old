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

package org.gridgain.grid.hadoop;

import org.gridgain.grid.*;

import java.util.*;

/**
 * Hadoop job.
 */
public interface GridHadoopJob {
    /**
     * Gets job ID.
     *
     * @return Job ID.
     */
    public GridHadoopJobId id();

    /**
     * Gets job information.
     *
     * @return Job information.
     */
    public GridHadoopJobInfo info();

    /**
     * Gets collection of input splits for this job.
     *
     * @return Input splits.
     */
    public Collection<GridHadoopInputSplit> input() throws GridException;

    /**
     * Returns context for task execution.
     *
     * @param info Task info.
     * @return Task Context.
     * @throws GridException If failed.
     */
    public GridHadoopTaskContext getTaskContext(GridHadoopTaskInfo info) throws GridException;

    /**
     * Does all the needed initialization for the job. Will be called on each node where tasks for this job must
     * be executed.
     * <p>
     * If job is running in external mode this method will be called on instance in GridGain node with parameter
     * {@code false} and on instance in external process with parameter {@code true}.
     *
     * @param external If {@code true} then this job instance resides in external process.
     * @param locNodeId Local node ID.
     * @throws GridException If failed.
     */
    public void initialize(boolean external, UUID locNodeId) throws GridException;

    /**
     * Release all the resources.
     * <p>
     * If job is running in external mode this method will be called on instance in GridGain node with parameter
     * {@code false} and on instance in external process with parameter {@code true}.
     *
     * @param external If {@code true} then this job instance resides in external process.
     * @throws GridException If failed.
     */
    public void dispose(boolean external) throws GridException;

    /**
     * Prepare local environment for the task.
     *
     * @param info Task info.
     * @throws GridException If failed.
     */
    public void prepareTaskEnvironment(GridHadoopTaskInfo info) throws GridException;

    /**
     * Cleans up local environment of the task.
     *
     * @param info Task info.
     * @throws GridException If failed.
     */
    public void cleanupTaskEnvironment(GridHadoopTaskInfo info) throws GridException;

    /**
     * Cleans up the job staging directory.
     */
    void cleanupStagingDirectory();
}
