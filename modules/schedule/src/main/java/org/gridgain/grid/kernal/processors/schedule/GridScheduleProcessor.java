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

import it.sauronsoftware.cron4j.*;
import org.gridgain.grid.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.kernal.processors.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.scheduler.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.typedef.*;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.concurrent.*;

/**
 * Schedules cron-based execution of grid tasks and closures.
 */
public class GridScheduleProcessor extends GridScheduleProcessorAdapter {
    /** Cron scheduler. */
    private Scheduler sched;

    /** Schedule futures. */
    private Set<GridSchedulerFuture<?>> schedFuts = new GridConcurrentHashSet<>();

    /**
     * @param ctx Kernal context.
     */
    public GridScheduleProcessor(GridKernalContext ctx) {
        super(ctx);
    }

    /** {@inheritDoc} */
    @Override public GridSchedulerFuture<?> schedule(final Runnable c, String pattern) {
        assert c != null;
        assert pattern != null;

        GridScheduleFutureImpl<Object> fut = new GridScheduleFutureImpl<>(sched, ctx, pattern);

        fut.schedule(new GridCallable<Object>() {
            @Nullable @Override public Object call() {
                c.run();

                return null;
            }
        });

        return fut;
    }

    /** {@inheritDoc} */
    @Override public <R> GridSchedulerFuture<R> schedule(Callable<R> c, String pattern) {
        assert c != null;
        assert pattern != null;

        GridScheduleFutureImpl<R> fut = new GridScheduleFutureImpl<>(sched, ctx, pattern);

        fut.schedule(c);

        return fut;
    }

    /**
     *
     * @return Future objects of currently scheduled active(not finished) tasks.
     */
    public Collection<GridSchedulerFuture<?>> getScheduledFutures() {
        return Collections.unmodifiableList(new ArrayList<>(schedFuts));
    }

    /**
     * Removes future object from the collection of scheduled futures.
     *
     * @param fut Future object.
     */
    void onDescheduled(GridSchedulerFuture<?> fut) {
        assert fut != null;

        schedFuts.remove(fut);
    }

    /**
     * Adds future object to the collection of scheduled futures.
     *
     * @param fut Future object.
     */
    void onScheduled(GridSchedulerFuture<?> fut) {
        assert fut != null;

        schedFuts.add(fut);
    }

    /** {@inheritDoc} */
    @Override public void start() throws GridException {
        sched = new Scheduler();

        sched.start();
    }

    /** {@inheritDoc} */
    @Override public void stop(boolean cancel) throws GridException {
        if (sched.isStarted())
            sched.stop();

        sched = null;
    }

    /** {@inheritDoc} */
    @Override public void printMemoryStats() {
        X.println(">>>");
        X.println(">>> Schedule processor memory stats [grid=" + ctx.gridName() + ']');
        X.println(">>>   schedFutsSize: " + schedFuts.size());
    }
}
