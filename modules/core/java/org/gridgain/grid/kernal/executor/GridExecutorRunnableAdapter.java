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

package org.gridgain.grid.kernal.executor;

import java.io.*;
import java.util.concurrent.*;

/**
 * Wraps {@link Runnable} task with predefined result.
 *
 * @author @java.author
 * @version @java.version
 * @param <T> The result type of the {@link Callable} argument.
 */
class GridExecutorRunnableAdapter<T> implements Callable<T>, Serializable {
    /** Returned result. */
    private final T result;

    /** Wrapped task. */
    private final Runnable task;

    /**
     * Creates adapter for runnable command.
     *
     * @param task The runnable task.
     * @param result Returned result.
     */
    GridExecutorRunnableAdapter(Runnable  task, T result) {
        this.task = task;
        this.result = result;
    }

    /** {@inheritDoc} */
    @Override public T call() {
        task.run();

        return result;
    }
}
