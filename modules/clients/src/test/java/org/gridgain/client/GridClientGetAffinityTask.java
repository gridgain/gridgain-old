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
import org.gridgain.grid.resources.*;
import org.gridgain.grid.util.typedef.internal.*;

import java.util.*;

import static org.gridgain.grid.compute.GridComputeJobResultPolicy.*;

/**
 * Get affinity for task argument.
 */
public class GridClientGetAffinityTask extends GridTaskSingleJobSplitAdapter<String, Integer> {
    /** Grid. */
    @GridInstanceResource
    private transient Grid grid;

    /** {@inheritDoc} */
    @Override protected Object executeJob(int gridSize, String arg) throws GridException {
        A.notNull(arg, "task argument");

        String[] split = arg.split(":", 2);

        A.ensure(split.length == 2, "Task argument should have format 'cacheName:affinityKey'.");

        String cacheName = split[0];
        String affKey = split[1];

        if ("null".equals(cacheName))
            cacheName = null;

        GridNode node = grid.mapKeyToNode(cacheName, affKey);

        return node.id().toString();
    }

    /** {@inheritDoc} */
    @Override public GridComputeJobResultPolicy result(GridComputeJobResult res, List<GridComputeJobResult> rcvd) throws GridException {
        if (res.getException() != null)
            return FAILOVER;

        return WAIT;
    }
}
