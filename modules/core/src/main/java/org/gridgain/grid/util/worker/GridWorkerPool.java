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

package org.gridgain.grid.util.worker;

import org.gridgain.grid.compute.*;
import org.gridgain.grid.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.typedef.internal.*;

import java.util.*;
import java.util.concurrent.*;

/**
 * Pool of runnable workers. This class automatically takes care of
 * error handling that has to do with executing a runnable task and
 * ensures that all tasks are finished when stop occurs.
 */
public class GridWorkerPool {
    /** */
    private final Executor exec;

    /** */
    private final GridLogger log;

    /** */
    private final Collection<GridWorker> workers = new GridConcurrentHashSet<>();

    /**
     * @param exec Executor service.
     * @param log Logger to use.
     */
    public GridWorkerPool(Executor exec, GridLogger log) {
        assert exec != null;
        assert log != null;

        this.exec = exec;
        this.log = log;
    }

    /**
     * Schedules runnable task for execution.
     *
     * @param w Runnable task.
     * @throws GridException Thrown if any exception occurred.
     */
    @SuppressWarnings({"CatchGenericClass", "ProhibitedExceptionThrown"})
    public void execute(final GridWorker w) throws GridException {
        workers.add(w);

        try {
            exec.execute(new Runnable() {
                @Override public void run() {
                    try {
                        w.run();
                    }
                    finally {
                        workers.remove(w);
                    }
                }
            });
        }
        catch (RejectedExecutionException e) {
            workers.remove(w);

            throw new GridComputeExecutionRejectedException("Failed to execute worker due to execution rejection.", e);
        }
        catch (RuntimeException e) {
            workers.remove(w);

            throw new GridException("Failed to execute worker due to runtime exception.", e);
        }
        catch (Error e) {
            workers.remove(w);

            throw e;
        }
    }

    /**
     * Waits for all workers to finish.
     *
     * @param cancel Flag to indicate whether workers should be cancelled
     *      before waiting for them to finish.
     */
    public void join(boolean cancel) {
        if (cancel)
            U.cancel(workers);

        // Record current interrupted status of calling thread.
        boolean interrupted = Thread.interrupted();

        try {
            U.join(workers, log);
        }
        finally {
            // Reset interrupted flag on calling thread.
            if (interrupted)
                Thread.currentThread().interrupt();
        }
    }
}
