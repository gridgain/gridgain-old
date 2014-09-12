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

package org.gridgain.grid.p2p;

import org.gridgain.grid.*;
import org.gridgain.grid.compute.*;
import org.gridgain.grid.compute.gridify.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.resources.*;

import java.util.*;

/**
 * P2P test task.
 */
public class GridP2PTestTask extends GridComputeTaskAdapter<Object, Integer> {
    /** */
    public static final String TASK_NAME = GridP2PTestTask.class.getName();

    /** */
    @GridLoggerResource
    private GridLogger log;

    /** */
    @GridLocalNodeIdResource
    private UUID nodeId;

    /** {@inheritDoc} */
    @Override public Map<? extends GridComputeJob, GridNode> map(List<GridNode> subgrid, Object arg) throws GridException {
        assert subgrid != null;
        assert !subgrid.isEmpty();

        Integer arg1 = null;

        if (arg instanceof GridifyArgument)
            arg1 = (Integer)((GridifyArgument)arg).getMethodParameters()[0];
        else if (arg instanceof Integer)
            arg1 = (Integer)arg;
        else
            assert false : "Failed to map task (unknown argument type) [type=" + arg.getClass() + ", val=" + arg + ']';

        Map<GridComputeJob, GridNode> map = new HashMap<>(subgrid.size());

        for (GridNode node : subgrid)
            if (!node.id().equals(nodeId))
                map.put(new GridP2PTestJob(arg1), node);

        return map;
    }

    /** {@inheritDoc} */
    @Override public Integer reduce(List<GridComputeJobResult> results) throws GridException {
        assert results.size() == 1 : "Results [received=" + results.size() + ", expected=" + 1 + ']';

        GridComputeJobResult res = results.get(0);

        if (log.isInfoEnabled())
            log.info("Got job result for aggregation: " + res);

        if (res.getException() != null)
            throw res.getException();

        return res.getData();
    }
}
