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

package org.gridgain.grid.compute.gridify.aop;

import org.gridgain.grid.compute.*;
import org.gridgain.grid.compute.gridify.*;
import org.gridgain.grid.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.util.lang.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.grid.util.gridify.*;
import java.util.*;

/**
 * Default gridify task which simply executes a method on remote node.
 * <p>
 * See {@link Gridify} documentation for more information about execution of
 * {@code gridified} methods.
 * @see Gridify
 */
public class GridifyDefaultTask extends GridComputeTaskAdapter<GridifyArgument, Object>
    implements GridPeerDeployAware {
    /** */
    private static final long serialVersionUID = 0L;

    /** Deploy class. */
    @SuppressWarnings({"TransientFieldNotInitialized"})
    private final transient Class<?> p2pCls;

    /** Class loader. */
    @SuppressWarnings({"TransientFieldNotInitialized"})
    private final transient ClassLoader clsLdr;

    /** Grid instance. */
    @GridInstanceResource
    private Grid grid;

    /** Load balancer. */
    @GridLoadBalancerResource
    private GridComputeLoadBalancer balancer;

    /**
     * Creates gridify default task with given deployment class.
     *
     * @param cls Deployment class for peer-deployment.
     */
    public GridifyDefaultTask(Class<?> cls) {
        assert cls != null;

        p2pCls = cls;

        clsLdr = U.detectClassLoader(cls);
    }

    /** {@inheritDoc} */
    @Override public Class<?> deployClass() {
        return p2pCls;
    }

    /** {@inheritDoc} */
    @Override public ClassLoader classLoader() {
        return clsLdr;
    }

    /** {@inheritDoc} */
    @Override public Map<? extends GridComputeJob, GridNode> map(List<GridNode> subgrid, GridifyArgument arg) throws GridException {
        assert !subgrid.isEmpty() : "Subgrid should not be empty: " + subgrid;

        assert grid != null : "Grid instance could not be injected";
        assert balancer != null : "Load balancer could not be injected";

        GridComputeJob job = new GridifyJobAdapter(arg);

        GridNode node = balancer.getBalancedNode(job, Collections.<GridNode>singletonList(grid.localNode()));

        if (node != null) {
            // Give preference to remote nodes.
            return Collections.singletonMap(job, node);
        }

        return Collections.singletonMap(job, balancer.getBalancedNode(job, null));
    }

    /** {@inheritDoc} */
    @Override public final Object reduce(List<GridComputeJobResult> results) throws GridException {
        assert results.size() == 1;

        GridComputeJobResult res = results.get(0);

        if (res.getException() != null)
            throw res.getException();

        return res.getData();
    }
}
