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

package org.gridgain.loadtests.direct.multisplit;

import org.gridgain.grid.*;
import org.gridgain.grid.compute.*;
import org.gridgain.grid.resources.*;

import java.util.*;

/**
 * Load test task.
 */
public class GridLoadTestTask extends GridComputeTaskAdapter<Integer, Integer> {
    /** Injected job context. */
    @GridTaskSessionResource
    private GridComputeTaskSession ctx;

    /** */
    @SuppressWarnings("unused")
    @GridInstanceResource
    private Grid grid;

    /** {@inheritDoc} */
    @Override public Map<? extends GridComputeJob, GridNode> map(List<GridNode> subgrid, Integer arg) throws GridException {
        assert arg != null;
        assert arg > 1;

        Map<GridLoadTestJob, GridNode> map = new HashMap<>(subgrid.size());

        Iterator<GridNode> iter = subgrid.iterator();

        Collection<UUID> assigned = new ArrayList<>(subgrid.size());

        for (int i = 0; i < arg; i++) {
            // Recycle iterator.
            if (!iter.hasNext())
                iter = subgrid.iterator();

            GridNode node = iter.next();

            assigned.add(node.id());

            map.put(new GridLoadTestJob(arg - 1), node);
        }

        ctx.setAttribute("nodes", assigned);

        return map;
    }

    /** {@inheritDoc} */
    @Override public Integer reduce(List<GridComputeJobResult> results) throws GridException {
        assert results != null;

        int retVal = 0;

        for (GridComputeJobResult res : results) {
            assert res.getException() == null : "Load test jobs can never fail: " + ctx;
            assert res.getData() != null;

            retVal += (Integer)res.getData();
        }

        return retVal;
    }
}
