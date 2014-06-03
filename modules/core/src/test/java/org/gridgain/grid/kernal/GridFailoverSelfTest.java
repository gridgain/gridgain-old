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
import org.gridgain.grid.resources.*;
import org.gridgain.grid.spi.failover.always.*;
import org.gridgain.testframework.junits.common.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.*;

/**
 * Failover tests.
 */
@GridCommonTest(group = "Kernal Self")
public class GridFailoverSelfTest extends GridCommonAbstractTest {
    /** Initial node that job has been mapped to. */
    private static final AtomicReference<GridNode> nodeRef = new AtomicReference<>(null);

    /** */
    public GridFailoverSelfTest() {
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
    public void testJobFail() throws Exception {
        try {
            Grid grid1 = startGrid(1);
            Grid grid2 = startGrid(2);

            assert grid1 != null;
            assert grid2 != null;

            Integer res = (Integer)grid1.compute().withTimeout(10000).execute(JobFailTask.class.getName(), "1").get();

            assert res != null;
            assert res == 1;
        }
        finally {
            stopGrid(1);
            stopGrid(2);
        }
    }

    /**
     *
     */
    @GridComputeTaskSessionFullSupport
    private static class JobFailTask implements GridComputeTask<String, Object> {
        /** */
        @GridTaskSessionResource
        private GridComputeTaskSession ses;

        /** {@inheritDoc} */
        @Override public Map<? extends GridComputeJob, GridNode> map(List<GridNode> subgrid, String arg) throws GridException {
            ses.setAttribute("fail", true);

            nodeRef.set(subgrid.get(0));

            return Collections.singletonMap(new GridComputeJobAdapter(arg) {
                /** Local node ID. */
                @GridLocalNodeIdResource
                private UUID locId;

                /** {@inheritDoc} */
                @Override public Serializable execute() throws GridException {
                    boolean fail;

                    try {
                        fail = ses.<String, Boolean>waitForAttribute("fail");
                    }
                    catch (InterruptedException e) {
                        throw new GridException("Got interrupted while waiting for attribute to be set.", e);
                    }

                    if (fail) {
                        ses.setAttribute("fail", false);

                        assert nodeRef.get().id().equals(locId);

                        throw new GridException("Job exception.");
                    }

                    assert !nodeRef.get().id().equals(locId);

                    // This job does not return any result.
                    return Integer.parseInt(this.<String>argument(0));
                }
            }, subgrid.get(0));
        }

        /** {@inheritDoc} */
        @Override public GridComputeJobResultPolicy result(GridComputeJobResult res,
            List<GridComputeJobResult> received) throws GridException {
            if (res.getException() != null && !(res.getException() instanceof GridComputeUserUndeclaredException)) {
                assert res.getNode().id().equals(nodeRef.get().id());

                return GridComputeJobResultPolicy.FAILOVER;
            }

            assert !res.getNode().id().equals(nodeRef.get().id());

            return GridComputeJobResultPolicy.REDUCE;
        }

        /** {@inheritDoc} */
        @Override public Object reduce(List<GridComputeJobResult> results) throws GridException {
            assert results.size() == 1;

            assert nodeRef.get() != null;

            assert !results.get(0).getNode().id().equals(nodeRef.get().id()) :
                "Initial node and result one are the same (should be different).";

            return results.get(0).getData();
        }
    }
}
