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

package org.gridgain.grid.kernal.visor.cmd;

import org.gridgain.grid.*;
import org.gridgain.grid.compute.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.util.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * Base class for Visor tasks intended to query data from a multiple node.
 *
 * @param <A> Task argument type.
 * @param <R> Task result type.
 */
public abstract class VisorMultiNodeTask<A, R, J> implements GridComputeTask<GridBiTuple<Set<UUID>, A>, R> {
    @GridInstanceResource
    protected GridEx g;

    /** Task argument. */
    protected A taskArg;

    /**
     * @param arg Task arg.
     * @return New job.
     */
    protected abstract VisorJob<A, J> job(A arg);

    /** {@inheritDoc} */
    @Nullable @Override public Map<? extends GridComputeJob, GridNode> map(List<GridNode> subgrid,
        @Nullable GridBiTuple<Set<UUID>, A> arg) throws GridException {
        assert arg != null;
        assert arg.get1() != null;

        Set<UUID> nodeIds = arg.get1();
        taskArg = arg.get2();

        Map<GridComputeJob, GridNode> map = new GridLeanMap<>(nodeIds.size());

        for (GridNode node : subgrid)
            if (nodeIds.contains(node.id()))
                map.put(job(taskArg), node);

        return map;
    }

    /** {@inheritDoc} */
    @Override public GridComputeJobResultPolicy result(GridComputeJobResult res,
        List<GridComputeJobResult> rcvd) throws GridException {
        // All Visor tasks should handle exceptions in reduce method.
        return GridComputeJobResultPolicy.WAIT;
    }
}
