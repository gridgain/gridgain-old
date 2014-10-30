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

package org.gridgain.grid.session;

import org.gridgain.grid.*;
import org.gridgain.grid.compute.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.spi.failover.always.*;
import org.gridgain.testframework.junits.common.*;

import java.io.*;
import java.util.*;

/**
 * Job failover test.
 */
@GridCommonTest(group = "Task Session")
public class GridSessionJobFailoverSelfTest extends GridCommonAbstractTest {
    /**
     * Default constructor.
     */
    public GridSessionJobFailoverSelfTest() {
        super(/*start Grid*/false);
    }

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        cfg.setFailoverSpi(new GridAlwaysFailoverSpi());

        return cfg;
    }

    /**
     * @throws Exception If failed.
     */
    @SuppressWarnings("unchecked")
    public void testFailoverJobSession() throws Exception {
        try {
            Grid grid1 = startGrid(1);

            startGrid(2);

            grid1.compute().localDeployTask(SessionTestTask.class, SessionTestTask.class.getClassLoader());

            Object res = grid1.compute().execute(SessionTestTask.class.getName(), "1").get();

            assert (Integer)res == 1;
        }
        finally {
            stopGrid(1);
            stopGrid(2);
        }
    }

    /**
     * Session test task implementation.
     */
    @GridComputeTaskSessionFullSupport
    private static class SessionTestTask implements GridComputeTask<String, Object> {
        /** */
        @GridTaskSessionResource private GridComputeTaskSession ses;

        /** */
        private boolean jobFailed;

        /** {@inheritDoc} */
        @Override public Map<? extends GridComputeJob, GridNode> map(List<GridNode> subgrid, String arg) throws GridException {
            ses.setAttribute("fail", true);

            for (int i = 0; i < 10; i++) {
                for (int ii = 0; ii < 10; ii++)
                    ses.setAttribute("test.task.attr." + i, ii);
            }

            return Collections.singletonMap(new GridComputeJobAdapter(arg) {
                /** */
                @GridLocalNodeIdResource
                private UUID locNodeId;

                @Override public Serializable execute() throws GridException {
                    boolean fail;

                    try {
                        fail = (Boolean)ses.waitForAttribute("fail");
                    }
                    catch (InterruptedException e) {
                        throw new GridException("Got interrupted while waiting for attribute to be set.", e);
                    }

                    if (fail) {
                        ses.setAttribute("fail", false);

                        for (int i = 0; i < 10; i++) {
                            for (int ii = 0; ii < 10; ii++)
                                ses.setAttribute("test.job.attr." + i, ii);
                        }

                        throw new GridException("Job exception.");
                    }

                    try {
                        for (int i = 0; i < 10; i++) {
                            boolean attr = ses.waitForAttribute("test.task.attr." + i, 9, 100000);

                            assert attr;
                        }

                        for (int i = 0; i < 10; i++) {
                            boolean attr = ses.waitForAttribute("test.job.attr." + i, 9, 100000);

                            assert attr;
                        }
                    }
                    catch (InterruptedException e) {
                        throw new GridException("Got interrupted while waiting for attribute to be set.", e);
                    }

                    // This job does not return any result.
                    return Integer.parseInt(this.<String>argument(0));
                }
            }, subgrid.get(0));
        }

        /** {@inheritDoc} */
        @Override public GridComputeJobResultPolicy result(GridComputeJobResult res, List<GridComputeJobResult> received)
            throws GridException {
            if (res.getException() != null) {
                assert !jobFailed;

                jobFailed = true;

                return GridComputeJobResultPolicy.FAILOVER;
            }

            return GridComputeJobResultPolicy.REDUCE;
        }

        /** {@inheritDoc} */
        @Override public Object reduce(List<GridComputeJobResult> results) throws GridException {
            assert results.size() == 1;

            return results.get(0).getData();
        }
    }
}
