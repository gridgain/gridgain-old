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

package org.gridgain.loadtests.direct.session;

import org.gridgain.grid.*;
import org.gridgain.grid.compute.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.resources.*;

import java.util.*;

/**
 * Session load test task.
 */
public class GridSessionLoadTestTask extends GridComputeTaskAdapter<Integer, Boolean> {
    /** */
    @GridTaskSessionResource
    private GridComputeTaskSession taskSes;

    /** */
    @GridLoggerResource
    private GridLogger log;

    /** */
    private Map<String, Integer> params;

    /** {@inheritDoc} */
    @Override public Map<? extends GridComputeJob, GridNode> map(List<GridNode> subgrid, Integer arg) throws GridException {
        assert taskSes != null;
        assert arg != null;
        assert arg > 0;

        Map<GridSessionLoadTestJob, GridNode> map = new HashMap<>(subgrid.size());

        Iterator<GridNode> iter = subgrid.iterator();

        Random rnd = new Random();

        params = new HashMap<>(arg);

        Collection<UUID> assigned = new ArrayList<>(subgrid.size());

        for (int i = 0; i < arg; i++) {
            // Recycle iterator.
            if (!iter.hasNext())
                iter = subgrid.iterator();

            String paramName = UUID.randomUUID().toString();

            int paramVal = rnd.nextInt();

            taskSes.setAttribute(paramName, paramVal);

            GridNode node = iter.next();

            assigned.add(node.id());

            map.put(new GridSessionLoadTestJob(paramName), node);

            params.put(paramName, paramVal);

            if (log.isDebugEnabled())
                log.debug("Set session attribute [name=" + paramName + ", value=" + paramVal + ']');
        }

        taskSes.setAttribute("nodes", assigned);

        return map;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("BusyWait")
    @Override public Boolean reduce(List<GridComputeJobResult> results) throws GridException {
        assert taskSes != null;
        assert results != null;
        assert params != null;
        assert !params.isEmpty();
        assert results.size() == params.size();

        Map<String, Integer> receivedParams = new HashMap<>();

        boolean allAttrReceived = false;

        int cnt = 0;

        while (!allAttrReceived && cnt++ < 3) {
            allAttrReceived = true;

            for (Map.Entry<String, Integer> entry : params.entrySet()) {
                assert taskSes.getAttribute(entry.getKey()) != null;

                Integer newVal = (Integer)taskSes.getAttribute(entry.getKey());

                assert newVal != null;

                receivedParams.put(entry.getKey(), newVal);

                if (newVal != entry.getValue() + 1)
                    allAttrReceived = false;
            }

            if (!allAttrReceived) {
                try {
                    Thread.sleep(100);
                }
                catch (InterruptedException e) {
                    throw new GridException("Thread interrupted.", e);
                }
            }
        }

        if (log.isDebugEnabled()) {
            for (Map.Entry<String, Integer> entry : receivedParams.entrySet()) {
                log.debug("Received session attr value [name=" + entry.getKey() + ", val=" + entry.getValue()
                    + ", expected=" + (params.get(entry.getKey()) + 1) + ']');
            }
        }

        return allAttrReceived;
    }
}
