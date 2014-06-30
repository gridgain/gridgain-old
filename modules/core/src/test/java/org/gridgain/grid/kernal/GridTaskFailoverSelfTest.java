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
import org.gridgain.testframework.junits.common.*;

import java.io.*;
import java.util.*;

/**
 * Test for task failover.
 */
@GridCommonTest(group = "Kernal Self")
public class GridTaskFailoverSelfTest extends GridCommonAbstractTest {
    /** Don't change it value. */
    public static final int SPLIT_COUNT = 2;

    /** */
    public GridTaskFailoverSelfTest() {
        super(false);
    }

    /**
     * @throws Exception If test failed.
     */
    @SuppressWarnings("unchecked")
    public void testFailover() throws Exception {
        Grid grid = startGrid();

        try {
            grid.compute().localDeployTask(GridFailoverTestTask.class, GridFailoverTestTask.class.getClassLoader());

            GridComputeTaskFuture<?> fut = grid.compute().execute(GridFailoverTestTask.class.getName(), null);

            assert fut != null;

            fut.get();

            assert false : "Should never be reached due to exception thrown.";
        }
        catch (GridTopologyException e) {
            info("Received correct exception: " + e);
        }
        finally {
            stopGrid();
        }
    }

    /** */
    @SuppressWarnings({"PublicInnerClass"})
    public static class GridFailoverTestTask extends GridComputeTaskSplitAdapter<Serializable, Integer> {
        /** */
        @GridLoggerResource
        private GridLogger log;

        /** {@inheritDoc} */
        @Override public Collection<GridComputeJobAdapter> split(int gridSize, Serializable arg) {
            if (log.isInfoEnabled())
                log.info("Splitting job [job=" + this + ", gridSize=" + gridSize + ", arg=" + arg + ']');

            Collection<GridComputeJobAdapter> jobs = new ArrayList<>(SPLIT_COUNT);

            for (int i = 0; i < SPLIT_COUNT; i++)
                jobs.add(new GridComputeJobAdapter() {
                    @Override public Serializable execute() {
                        if (log.isInfoEnabled())
                            log.info("Computing job [job=" + this + ']');

                        return null;
                    }
                });

            return jobs;
        }

        /** {@inheritDoc} */
        @Override public GridComputeJobResultPolicy result(GridComputeJobResult res, List<GridComputeJobResult> received) throws
            GridException {
            if (res.getException() != null)
                throw res.getException();

            return GridComputeJobResultPolicy.FAILOVER;
        }

        /** {@inheritDoc} */
        @Override public Integer reduce(List<GridComputeJobResult> results) {
            if (log.isInfoEnabled())
                log.info("Reducing job [job=" + this + ", results=" + results + ']');

            int res = 0;

            for (GridComputeJobResult result : results)
                res += (Integer)result.getData();

            return res;
        }
    }
}
