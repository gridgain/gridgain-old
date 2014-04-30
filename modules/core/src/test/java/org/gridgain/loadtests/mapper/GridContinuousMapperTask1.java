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

package org.gridgain.loadtests.mapper;

import org.gridgain.grid.*;
import org.gridgain.grid.compute.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.util.typedef.*;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Test task.
 */
@SuppressWarnings("TransientFieldNotInitialized")
@GridComputeTaskNoResultCache
public class GridContinuousMapperTask1 extends GridComputeTaskAdapter<Integer, Integer> {
    /** Job ID generator. */
    private final transient AtomicInteger jobIdGen = new AtomicInteger();

    /** Mapper. */
    @GridTaskContinuousMapperResource
    private GridComputeTaskContinuousMapper mapper;

    /** Grid. */
    @GridInstanceResource
    private Grid g;

    /** Blocking queue. */
    private final transient LinkedBlockingQueue<Integer> queue = new LinkedBlockingQueue<>(10);

    /** Sent jobs count. */
    private final transient AtomicInteger sentJobs = new AtomicInteger();

    /** Maximum number of executions. */
    private transient int maxExecs;

    /** Worker thread. */
    private transient Thread t = new Thread("mapper-worker") {
        @Override public void run() {
            try {
                while (!Thread.currentThread().isInterrupted())
                    queue.put(jobIdGen.getAndIncrement());
            }
            catch (InterruptedException ignore) {
                // No-op.
            }
        }
    };

    /**
     * Sends job to node.
     *
     * @param n Node.
     * @throws GridException If failed.
     */
    private void sendJob(GridNode n) throws GridException {
        try {
            int jobId = queue.take();

            sentJobs.incrementAndGet();

            mapper.send(new GridComputeJobAdapter(jobId) {
                @GridInstanceResource
                private Grid g;

                @Override public Object execute() {
                    Integer jobId = argument(0);

                    X.println(">>> Received job for ID: " + jobId);

                    return g.cache("replicated").peek(jobId);
                }
            }, n);
        }
        catch (InterruptedException e) {
            throw new GridException(e);
        }
    }

    /** {@inheritDoc} */
    @Override public Map<? extends GridComputeJob, GridNode> map(List<GridNode> subgrid, @Nullable Integer arg)
        throws GridException {
        maxExecs = arg;

        // Start worker thread.
        t.start();

        if (g.nodes().size() == 1)
            sendJob(g.localNode());
        else
            for (GridNode n : g.forRemotes().nodes())
                sendJob(n);

        return null;
    }

    /** {@inheritDoc} */
    @Override public GridComputeJobResultPolicy result(GridComputeJobResult res, List<GridComputeJobResult> rcvd) throws GridException {
        if (res.getException() != null)
            throw new GridException(res.getException());

        TestObject o = res.getData();

        assert o != null;

        X.println("Received job result from node [resId=" + o.getId() + ", node=" + res.getNode().id() + ']');

        if (sentJobs.get() < maxExecs)
            sendJob(res.getNode());

        return GridComputeJobResultPolicy.WAIT;
    }

    /** {@inheritDoc} */
    @Override public Integer reduce(List<GridComputeJobResult> results) throws GridException {
        X.println(">>> Reducing task...");

        t.interrupt();

        try {
            t.join();
        }
        catch (InterruptedException e) {
            throw new GridException(e);
        }

        return null;
    }
}
