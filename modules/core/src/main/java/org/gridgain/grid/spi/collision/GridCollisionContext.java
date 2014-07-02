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

import java.util.*;

/**
 * Context for resolving collisions. This context contains collections of
 * waiting jobs, active jobs, and held jobs. If continuations are not used
 * (see {@link GridComputeJobContinuation}), then collection of held jobs will
 * always be empty. {@link GridCollisionSpi} will manipulate these lists
 * to make sure that only allowed number of jobs are running in parallel or
 * waiting to be executed.
 * @since 3.5
 */
public interface GridCollisionContext {
    /**
     * Gets ordered collection of collision contexts for jobs that are currently executing.
     * It can be empty but never {@code null}.
     *
     * @return Ordered number of collision contexts for currently executing jobs.
     */
    public Collection<GridCollisionJobContext> activeJobs();

    /**
     * Gets ordered collection of collision contexts for jobs that are currently waiting
     * for execution. It can be empty but never {@code null}. Note that a newly
     * arrived job, if any, will always be represented by the last item in this list.
     * <p>
     * This list is guaranteed not to change while
     * {@link GridCollisionSpi#onCollision(GridCollisionContext)} method is being executed.
     *
     * @return Ordered collection of collision contexts for waiting jobs.
     */
    public Collection<GridCollisionJobContext> waitingJobs();

    /**
     * Gets collection of jobs that are currently in {@code held} state. Job can enter
     * {@code held} state by calling {@link GridComputeJobContinuation#holdcc()} method at
     * which point job will release all resources and will get suspended. If
     * {@link GridComputeJobContinuation job continuations} are not used, then this list
     * will always be empty, but never {@code null}.
     *
     * @return Collection of jobs that are currently in {@code held} state.
     */
    public Collection<GridCollisionJobContext> heldJobs();
}
