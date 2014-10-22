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

package org.gridgain.grid.tests.p2p;

import org.gridgain.grid.*;
import org.gridgain.grid.compute.*;

import java.io.*;
import java.util.*;

/**
 * Test task for P2P deployment tests.
 */
public class GridSingleSplitTestTask extends GridComputeTaskSplitAdapter<Integer, Integer> {
    /**
     * {@inheritDoc}
     */
    @Override protected Collection<? extends GridComputeJob> split(int gridSize, Integer arg) throws GridException {
        assert gridSize > 0 : "Subgrid cannot be empty.";

        Collection<GridComputeJobAdapter> jobs = new ArrayList<>(gridSize);

        for (int i = 0; i < arg; i++)
            jobs.add(new GridSingleSplitTestJob(1));

        return jobs;
    }

    /**
     * {@inheritDoc}
     */
    @Override public Integer reduce(List<GridComputeJobResult> results) throws GridException {
        int retVal = 0;

        for (GridComputeJobResult res : results) {
            assert res.getException() == null : "Load test jobs can never fail: " + res;

            retVal += (Integer)res.getData();
        }

        return retVal;
    }

    /**
     * Test job for P2P deployment tests.
     */
    @SuppressWarnings("PublicInnerClass")
    public static final class GridSingleSplitTestJob extends GridComputeJobAdapter {
        /**
         * @param args Job arguments.
         */
        public GridSingleSplitTestJob(Integer args) {
            super(args);
        }

        /** {@inheritDoc} */
        @Override public Serializable execute() {
            return new GridSingleSplitTestJobTarget().executeLoadTestJob((Integer)argument(0));
        }
    }
}
