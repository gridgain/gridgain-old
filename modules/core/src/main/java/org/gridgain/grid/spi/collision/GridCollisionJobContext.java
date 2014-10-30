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

package org.gridgain.grid.spi.collision;

import org.gridgain.grid.compute.*;

/**
 * This interface defines set of operations that collision SPI implementation can perform on
 * jobs that are either waiting or executing.
 */
public interface GridCollisionJobContext {
    /**
     * Gets current task session associated with this job.
     *
     * @return Grid task session.
     */
    public GridComputeTaskSession getTaskSession();

    /**
     * Gets job context. Use this context to set/get attributes that
     * should be visible only to this job and should not be distributed
     * to other jobs in the grid.
     * <p>
     * Job context travels with job whenever it gets failed-over to another
     * node, so attributes set on the context on one node will be visible
     * on other nodes this job may potentially end up on.
     *
     * @return Job context.
     */
    public GridComputeJobContext getJobContext();

    /**
     * Job for this context.
     *
     * @return Job for this context.
     */
    public GridComputeJob getJob();

    /**
     * Activates the job. If job is already active this is no-op. Collision resolution
     * is handled concurrently, so it may be possible that other threads already activated
     * or cancelled/rejected this job. This method will return {@code true} if it was
     * able to activate the job, and {@code false} otherwise.
     *
     * @return {@code True} if it was possible to activate the job, and
     *      {@code false} otherwise.
     */
    public boolean activate();

    /**
     * Cancels the job. If job was active (executing) method {@link GridComputeJob#cancel()} will
     * be called on the job. If job was in wait state, then it will be {@code rejected}
     * prior to execution and {@link GridComputeJob#cancel()} will not be called.
     * <p>
     * Collision resolution is handled concurrently, so it may be possible that other threads
     * already activated or cancelled/rejected this job. This method will return {@code true}
     * if it was able to cancel/reject the job and {@code false} otherwise.
     *
     * @return {@code True} if it was possible to cancel/reject this job, {@code false}
     *      otherwise.
     */
    public boolean cancel();
}
