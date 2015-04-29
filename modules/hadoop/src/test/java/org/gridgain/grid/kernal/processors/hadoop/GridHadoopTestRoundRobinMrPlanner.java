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

package org.gridgain.grid.kernal.processors.hadoop;

import org.gridgain.grid.*;
import org.gridgain.grid.hadoop.*;
import org.gridgain.grid.kernal.processors.hadoop.planner.GridHadoopDefaultMapReducePlan;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * Round-robin mr planner.
 */
public class GridHadoopTestRoundRobinMrPlanner implements GridHadoopMapReducePlanner {
    /** {@inheritDoc} */
    @Override public GridHadoopMapReducePlan preparePlan(GridHadoopJob job, Collection<GridNode> top,
        @Nullable GridHadoopMapReducePlan oldPlan) throws GridException {
        if (top.isEmpty())
            throw new IllegalArgumentException("Topology is empty");

        // Has at least one element.
        Iterator<GridNode> it = top.iterator();

        Map<UUID, Collection<GridHadoopInputSplit>> mappers = new HashMap<>();

        for (GridHadoopInputSplit block : job.input()) {
            GridNode node = it.next();

            Collection<GridHadoopInputSplit> nodeBlocks = mappers.get(node.id());

            if (nodeBlocks == null) {
                nodeBlocks = new ArrayList<>();

                mappers.put(node.id(), nodeBlocks);
            }

            nodeBlocks.add(block);

            if (!it.hasNext())
                it = top.iterator();
        }

        int[] rdc = new int[job.info().reducers()];

        for (int i = 0; i < rdc.length; i++)
            rdc[i] = i;

        return new GridHadoopDefaultMapReducePlan(mappers, Collections.singletonMap(it.next().id(), rdc));
    }
}
