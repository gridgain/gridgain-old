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

package org.gridgain.grid.kernal;

import org.gridgain.grid.*;
import org.gridgain.grid.compute.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.spi.failover.*;
import org.gridgain.grid.spi.failover.always.*;
import org.gridgain.testframework.junits.common.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.*;

/**
 * Test failover and custom topology. Topology returns local node if remote node fails.
 */
@GridCommonTest(group = "Kernal Self")
public class GridFailoverCustomTopologySelfTest extends GridCommonAbstractTest {
    /** */
    private final AtomicInteger failCnt = new AtomicInteger(0);

    /** */
    private static final Object mux = new Object();

    /** */
    public GridFailoverCustomTopologySelfTest() {
        super(/*start Grid*/false);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("deprecation")
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        cfg.setNodeId(null);

        cfg.setFailoverSpi(new GridAlwaysFailoverSpi() {
            /** {@inheritDoc} */
            @Override public GridNode failover(GridFailoverContext ctx, List<GridNode> top) {
                failCnt.incrementAndGet();

                return super.failover(ctx, top);
            }
        });

        return cfg;
    }
    /**
     * Tests that failover don't pick local node if it has been excluded from topology.
     *
     * @throws Exception If failed.
     */
    @SuppressWarnings({"WaitNotInLoop", "UnconditionalWait", "unchecked"})
    public void testFailoverTopology() throws Exception {
        try {
            Grid grid1 = startGrid(1);
            Grid grid2 = startGrid(2);

            assert grid1 != null;
            assert grid2 != null;

            grid1.compute().localDeployTask(JobTask.class, JobTask.class.getClassLoader());

            try {
                GridComputeTaskFuture<String> fut;

                synchronized(mux){
                    fut = grid1.compute().execute(JobTask.class, null);

                    mux.wait();
                }

                stopAndCancelGrid(2);

                String res = fut.get();

                info("Task result: " + res);
            }
            catch (GridException e) {
                info("Got unexpected grid exception: " + e);
            }

            info("Failed over: " + failCnt.get());

            assert failCnt.get() == 1 : "Invalid fail over counter [expected=1, actual=" + failCnt.get() + ']';
        }
        finally {
            stopGrid(1);

            // Stopping stopped instance just in case.
            stopGrid(2);
        }
    }

    /** */
    @SuppressWarnings("PublicInnerClass")
    public static class JobTask extends GridComputeTaskAdapter<String, String> {
        /** */
        @GridLoggerResource private GridLogger log;

         /** */
        @GridLocalNodeIdResource private UUID locNodeId;

        /** {@inheritDoc} */
        @Override public Map<? extends GridComputeJob, GridNode> map(List<GridNode> subgrid, String arg) throws GridException {
            assert locNodeId != null;

            if (log.isInfoEnabled())
                log.info("Mapping jobs [subgrid=" + subgrid + ", arg=" + arg + ']');

            GridNode remoteNode = null;

            for (GridNode node : subgrid) {
                if (!node.id().equals(locNodeId))
                    remoteNode = node;
            }

            return Collections.singletonMap(new GridComputeJobAdapter(locNodeId) {
                /** */
               @GridLocalNodeIdResource private UUID nodeId;

                /** {@inheritDoc} */
                @SuppressWarnings("NakedNotify")
                @Override public Serializable execute() throws GridException {
                    assert nodeId != null;

                    if (!nodeId.equals(argument(0))) {
                        try {
                            synchronized(mux) {
                                mux.notifyAll();
                            }

                            Thread.sleep(Integer.MAX_VALUE);
                        }
                        catch (InterruptedException e) {
                            throw new GridComputeExecutionRejectedException("Expected interruption during execution.", e);
                        }
                    }
                    else
                        return "success";

                    throw new GridComputeExecutionRejectedException("Expected exception during execution.");
                }
            }, remoteNode);
        }

        /** {@inheritDoc} */
        @Override public String reduce(List<GridComputeJobResult> results) throws GridException {
            assert results.size() == 1;

            return results.get(0).getData();
        }
    }
}
