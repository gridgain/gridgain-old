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

import net.sf.json.*;
import org.gridgain.grid.compute.*;
import org.gridgain.grid.*;

import java.util.*;

import static org.gridgain.grid.compute.GridComputeJobResultPolicy.*;

/**
 * Test task summarizes length of all strings in the arguments list.
 * <p>
 * The argument of the task is JSON-serialized array of objects to calculate string length sum of.
 */
public class GridClientHttpTask extends GridComputeTaskSplitAdapter<String, Integer> {
    /** Task delegate. */
    private final GridClientTcpTask delegate = new GridClientTcpTask();

    /** {@inheritDoc} */
    @Override protected Collection<? extends GridComputeJob> split(int gridSize, String arg) throws GridException {
        JSON json = JSONSerializer.toJSON(arg);

        List list = json.isArray() ? JSONArray.toList((JSONArray)json, String.class, new JsonConfig()) : null;

        //noinspection unchecked
        return delegate.split(gridSize, list);
    }

    /** {@inheritDoc} */
    @Override public Integer reduce(List<GridComputeJobResult> results) throws GridException {
        return delegate.reduce(results);
    }

    /** {@inheritDoc} */
    @Override public GridComputeJobResultPolicy result(GridComputeJobResult res, List<GridComputeJobResult> rcvd) throws GridException {
        if (res.getException() != null)
            return FAILOVER;

        return WAIT;
    }
}
