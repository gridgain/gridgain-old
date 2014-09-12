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

package org.gridgain.grid.compute;

import org.gridgain.grid.*;

import java.lang.annotation.*;
import java.util.*;

/**
 * This annotation disables caching of task results when attached to {@link GridComputeTask} class
 * being executed. Use it when number of jobs within task grows too big, or jobs themselves
 * are too large to keep in memory throughout task execution. By default all results are cached and passed into
 * {@link GridComputeTask#result(GridComputeJobResult,List) GridComputeTask.result(GridComputeJobResult, List&lt;GridComputeJobResult&gt;)}
 * method or {@link GridComputeTask#reduce(List) GridComputeTask.reduce(List&lt;GridComputeJobResult&gt;)} method.
 * When this annotation is attached to a task class, then this list of job results will always be empty.
 * <p>
 * Note that if this annotation is attached to a task class, then job siblings list is not maintained
 * and always has size of {@code 0}. This is done to make sure that in case if task emits large
 * number of jobs, list of jobs siblings does not grow. This only affects the following methods
 * on {@link GridComputeTaskSession}:
 * <ul>
 * <li>{@link GridComputeTaskSession#getJobSiblings()}</li>
 * <li>{@link GridComputeTaskSession#getJobSibling(GridUuid)}</li>
 * <li>{@link GridComputeTaskSession#refreshJobSiblings()}</li>
 * </ul>
 *
 * Use this annotation when job results are too large to hold in memory and can be discarded
 * after being processed in
 * {@link GridComputeTask#result(GridComputeJobResult, List) GridComputeTask.result(GridComputeJobResult, List&lt;GridComputeJobResult&gt;)}
 * method.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface GridComputeTaskNoResultCache {
    // No-op.
}
