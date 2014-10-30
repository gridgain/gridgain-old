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

package org.gridgain.client;

import org.gridgain.grid.compute.*;
import org.gridgain.grid.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.util.typedef.*;

import java.util.*;

import static org.gridgain.grid.compute.GridComputeJobResultPolicy.*;

/**
 * Stop node task, applicable arguments:
 * <ul>
 *     <li>node id (as string) to stop or</li>
 *     <li>node type (see start nodes task).</li>
 * </ul>
 */
public class GridClientStopNodeTask extends GridComputeTaskSplitAdapter<String, Integer> {
    /** */
    @GridLoggerResource
    private transient GridLogger log;

    /** */
    @GridInstanceResource
    private transient Grid grid;

    /** {@inheritDoc} */
    @Override protected Collection<? extends GridComputeJob> split(int gridSize, String arg) throws GridException {
        Collection<GridComputeJob> jobs = new ArrayList<>();

        for (int i = 0; i < gridSize; i++)
            jobs.add(new StopJob(arg));

        return jobs;
    }

    /** {@inheritDoc} */
    @Override public GridComputeJobResultPolicy result(GridComputeJobResult res, List<GridComputeJobResult> rcvd) throws GridException {
        GridComputeJobResultPolicy superRes = super.result(res, rcvd);

        // Deny failover.
        if (superRes == FAILOVER)
            superRes = WAIT;

        return superRes;
    }

    /** {@inheritDoc} */
    @Override public Integer reduce(List<GridComputeJobResult> results) throws GridException {
        int stoppedCnt = 0;

        for (GridComputeJobResult res : results)
            if (!res.isCancelled())
                stoppedCnt+=(Integer)res.getData();

        return stoppedCnt;
    }

    /**
     * Stop node job it is executed on.
     */
    private static class StopJob extends GridComputeJobAdapter {
        /** */
        private final String gridType;

        /** */
        @GridLoggerResource
        private GridLogger log;

        /** */
        @GridInstanceResource
        private Grid grid;

        /** */
        private StopJob(String gridType) {
            this.gridType = gridType;
        }

        /** {@inheritDoc} */
        @Override public Object execute() {
            log.info(">>> Stop node [nodeId=" + grid.localNode().id() + ", name='" + grid.name() + "']");

            String prefix = GridClientStartNodeTask.getConfig(gridType).getGridName() + " (";

            if (!grid.name().startsWith(prefix)) {
                int stoppedCnt = 0;

                for (Grid g : G.allGrids())
                    if (g.name().startsWith(prefix)) {
                        try {
                            log.info(">>> Grid stopping [nodeId=" + g.localNode().id() + ", name='" + g.name() + "']");

                            G.stop(g.name(), true);

                            stoppedCnt++;
                        }
                        catch (IllegalStateException e) {
                            log.warning("Failed to stop grid.", e);
                        }
                    }

                return stoppedCnt;
            }

            return 0;
        }
    }
}
