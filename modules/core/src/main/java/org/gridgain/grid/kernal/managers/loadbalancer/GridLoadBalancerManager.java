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

package org.gridgain.grid.kernal.managers.loadbalancer;

import org.gridgain.grid.compute.*;
import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.cache.affinity.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.kernal.managers.*;
import org.gridgain.grid.kernal.managers.deployment.*;
import org.gridgain.grid.spi.loadbalancing.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * Load balancing manager.
 */
public class GridLoadBalancerManager extends GridManagerAdapter<GridLoadBalancingSpi> {
    /**
     * @param ctx Grid kernal context.
     */
    public GridLoadBalancerManager(GridKernalContext ctx) {
        super(ctx, ctx.config().getLoadBalancingSpi());
    }

    /** {@inheritDoc} */
    @Override public void start() throws GridException {
        startSpi();

        if (log.isDebugEnabled())
            log.debug(startInfo());
    }

    /** {@inheritDoc} */
    @Override public void stop(boolean cancel) throws GridException {
        stopSpi();

        if (log.isDebugEnabled())
            log.debug(stopInfo());
    }

    /**
     * @param ses Task session.
     * @param top Task topology.
     * @param job Job to balance.
     * @return Next balanced node.
     * @throws GridException If anything failed.
     */
    public GridNode getBalancedNode(GridTaskSessionImpl ses, List<GridNode> top, GridComputeJob job)
        throws GridException {
        assert ses != null;
        assert top != null;
        assert job != null;

        // Check cache affinity routing first.
        GridNode affNode = cacheAffinityNode(ses.deployment(), job, top);

        if (affNode != null) {
            if (log.isDebugEnabled())
                log.debug("Found affinity node for the job [job=" + job + ", affNode=" + affNode.id() + "]");

            return affNode;
        }

        return getSpi(ses.getLoadBalancingSpi()).getBalancedNode(ses, top, job);
    }

    /**
     * @param ses Grid task session.
     * @param top Task topology.
     * @return Load balancer.
     */
    @SuppressWarnings("ExternalizableWithoutPublicNoArgConstructor")
    public GridComputeLoadBalancer getLoadBalancer(final GridTaskSessionImpl ses, final List<GridNode> top) {
        assert ses != null;

        // Return value is not intended for sending over network.
        return new GridLoadBalancerAdapter() {
            @Nullable @Override public GridNode getBalancedNode(GridComputeJob job, @Nullable Collection<GridNode> exclNodes)
                throws GridException {
                A.notNull(job, "job");

                if (F.isEmpty(exclNodes))
                    return GridLoadBalancerManager.this.getBalancedNode(ses, top, job);

                List<GridNode> nodes = F.loseList(top, true, exclNodes);

                if (nodes.isEmpty())
                    return null;

                // Exclude list of nodes from topology.
                return GridLoadBalancerManager.this.getBalancedNode(ses, nodes, job);
            }
        };
    }

    /**
     * @param dep Deployment.
     * @param job Grid job.
     * @param nodes Topology nodes.
     * @return Cache affinity node or {@code null} if this job is not routed with cache affinity key.
     * @throws GridException If failed to determine whether to use affinity routing.
     */
    @Nullable private GridNode cacheAffinityNode(GridDeployment dep, GridComputeJob job, Collection<GridNode> nodes)
        throws GridException {
        assert dep != null;
        assert job != null;
        assert nodes != null;

        if (log.isDebugEnabled())
            log.debug("Looking for cache affinity node [job=" + job + "]");

        Object key = dep.annotatedValue(job, GridCacheAffinityKeyMapped.class);

        if (key == null)
            return null;

        String cacheName = (String)dep.annotatedValue(job, GridCacheName.class);

        if (log.isDebugEnabled())
            log.debug("Affinity properties [key=" + key + ", cacheName=" + cacheName + "]");

        try {
            GridNode node = ctx.affinity().mapKeyToNode(cacheName, key);

            if (node == null)
                throw new GridException("Failed to map key to node (is cache with given name started?) [gridName=" +
                    ctx.gridName() + ", key=" + key + ", cacheName=" + cacheName +
                    ", nodes=" + U.toShortString(nodes) + ']');

            if (!nodes.contains(node))
                throw new GridException("Failed to map key to node (projection nodes do not contain affinity node) " +
                    "[gridName=" + ctx.gridName() + ", key=" + key + ", cacheName=" + cacheName +
                    ", nodes=" + U.toShortString(nodes) + ", node=" + U.toShortString(node) + ']');

            return node;
        }
        catch (GridException e) {
            throw new GridException("Failed to map affinity key to node for job [gridName=" + ctx.gridName() +
                ", job=" + job + ']', e);
        }
    }
}
