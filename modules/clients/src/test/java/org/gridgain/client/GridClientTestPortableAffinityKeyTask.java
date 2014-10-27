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

import org.gridgain.grid.*;
import org.gridgain.grid.compute.*;
import org.gridgain.grid.portables.*;
import org.gridgain.grid.resources.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * Task used to test portable affinity key.
 */
public class GridClientTestPortableAffinityKeyTask extends GridComputeTaskAdapter<Object, Boolean> {
    /** */
    @GridInstanceResource
    private Grid grid;

    /** {@inheritDoc} */
    @Nullable @Override public Map<? extends GridComputeJob, GridNode> map(List<GridNode> gridNodes,
        @Nullable final Object arg) throws GridException {
        for (GridNode node : gridNodes) {
            if (node.isLocal())
                return Collections.singletonMap(new GridComputeJobAdapter() {
                    @Override public Object execute() throws GridException {
                        return executeJob(arg);
                    }
                }, node);
        }

        throw new GridException("Failed to find local node in task topology: " + gridNodes);
    }

    /** {@inheritDoc} */
    @Nullable @Override public Boolean reduce(List<GridComputeJobResult> results) throws GridException {
        return results.get(0).getData();
    }

    /**
     * @param arg Argument.
     * @return Execution result.
     * @throws GridException If failed.
     */
     protected Boolean executeJob(Object arg) throws GridException {
        Collection args = (Collection)arg;

        Iterator<Object> it = args.iterator();

        assert args.size() == 3 : args.size();

        GridPortableObject obj = (GridPortableObject)it.next();

        String cacheName = (String)it.next();

        String expAffKey = (String)it.next();

        Object affKey = grid.cache(cacheName).affinity().affinityKey(obj);

        if (!expAffKey.equals(affKey))
            throw new GridException("Unexpected affinity key: " + affKey);

        if (!grid.cache(cacheName).affinity().mapKeyToNode(obj).isLocal())
            throw new GridException("Job is not run on primary node.");

        return true;
    }
}
