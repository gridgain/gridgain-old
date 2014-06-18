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

package org.gridgain.grid.kernal.processors.schedule;

import org.gridgain.grid.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.scheduler.*;

import java.util.concurrent.*;

/**
 * No-op implementation of {@link GridScheduleProcessorAdapter}, throws exception on usage attempt.
 */
public class GridNoopScheduleProcessor extends GridScheduleProcessorAdapter {
    /**
     * @param ctx Kernal context.
     */
    public GridNoopScheduleProcessor(GridKernalContext ctx) {
        super(ctx);
    }

    /** {@inheritDoc} */
    @Override public GridSchedulerFuture<?> schedule(Runnable c, String pattern) {
        throw processorException();
    }

    /** {@inheritDoc} */
    @Override public <R> GridSchedulerFuture<R> schedule(Callable<R> c, String pattern) {
        throw processorException();
    }

    /**
     * @return No-op processor usage exception;
     */
    private GridRuntimeException processorException() {
        return new GridRuntimeException("Current GridGain configuration does not support schedule functionality " +
            "(consider adding gridgain-schedule module to classpath).");
    }
}
