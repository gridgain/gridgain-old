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

package org.gridgain.loadtests.direct.stealing;

import org.gridgain.grid.*;
import org.gridgain.grid.compute.*;
import org.gridgain.grid.resources.*;

import java.util.*;

/**
 * Stealing load test task.
 */
public class GridStealingLoadTestTask extends GridComputeTaskAdapter<UUID, Integer> {
    /** */
    @GridTaskSessionResource
    private GridComputeTaskSession taskSes;

    /** */
    private UUID stealingNodeId;

    /** */
    private int stolenJobs;

    /** {@inheritDoc} */
    @Override public Map<? extends GridComputeJob, GridNode> map(List<GridNode> subgrid, UUID arg) throws GridException {
        assert arg != null;
        assert subgrid.size() > 1: "Test requires at least 2 nodes. One with load and another one to steal.";

        int jobsNum = subgrid.size();

        Map<GridStealingLoadTestJob, GridNode> map = new HashMap<>(jobsNum);

        stealingNodeId = arg;

        Iterator<GridNode> iter = subgrid.iterator();

        Collection<UUID> assigned = new ArrayList<>(subgrid.size());

        for (int i = 0; i < jobsNum; i++) {
            GridNode node = null;

            boolean nextNodeFound = false;

            while (iter.hasNext() && !nextNodeFound) {
                node = iter.next();

                // Do not map jobs to the stealing node.
                if (!node.id().equals(stealingNodeId))
                    nextNodeFound = true;

                // Recycle iterator.
                if (!iter.hasNext())
                    iter = subgrid.iterator();
            }

            assert node != null;

            assigned.add(node.id());

            map.put(new GridStealingLoadTestJob(), node);
        }

        taskSes.setAttribute("nodes", assigned);

        return map;
    }

    /** {@inheritDoc} */
    @Override public Integer reduce(List<GridComputeJobResult> results) throws GridException {
        assert results != null;

        for (GridComputeJobResult res : results) {
            if (res.getData() != null && stealingNodeId.equals(res.getData()))
                stolenJobs++;
        }

        return stolenJobs;
    }
}
