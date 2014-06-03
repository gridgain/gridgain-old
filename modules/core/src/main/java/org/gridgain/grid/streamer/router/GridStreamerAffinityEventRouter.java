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

package org.gridgain.grid.streamer.router;

import org.gridgain.grid.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.streamer.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.typedef.*;

import java.util.*;

/**
 * Router used to colocate identical streamer events or events with identical affinity
 * key on the same node. Such collocation is often required to perform computations on
 * multiple events together, for example, find number of occurrences of a word in some
 * text. In this case you would collocate identical words together to make sure that
 * you can update their counts.
 * <h1 class="header">Affinity Key</h1>
 * Affinity key for collocation of event together on the same node is specified
 * via {@link AffinityEvent#affinityKey()} method. If event does not implement
 * {@link AffinityEvent} interface, then event itself will be used to determine affinity.
 */
public class GridStreamerAffinityEventRouter extends GridStreamerEventRouterAdapter {
    /** */
    public static final int REPLICA_CNT = 128;

    /**
     * All events that implement this interface will be routed based on key affinity.
     */
    @SuppressWarnings("PublicInnerClass")
    public interface AffinityEvent {
        /**
         * @return Affinity route key for the event.
         */
        public Object affinityKey();
    }

    /** Grid instance. */
    @GridInstanceResource
    private Grid grid;

    /** */
    private final GridConsistentHash<UUID> nodeHash = new GridConsistentHash<>();

    /** */
    private Collection<UUID> addedNodes = new GridConcurrentHashSet<>();

    /** {@inheritDoc} */
    @Override public <T> GridNode route(GridStreamerContext ctx, String stageName, T evt) {
        return node(evt instanceof AffinityEvent ? ((AffinityEvent) evt).affinityKey() :
            evt, ctx);
    }

    /**
     * @param obj Object.
     * @param ctx Context.
     * @return Rich node.
     */
    private GridNode node(Object obj, GridStreamerContext ctx) {
        while (true) {
            Collection<GridNode> nodes = ctx.projection().nodes();

            assert nodes != null;
            assert !nodes.isEmpty();

            int nodesSize = nodes.size();

            if (nodesSize == 1) { // Minor optimization.
                GridNode ret = F.first(nodes);

                assert ret != null;

                return ret;
            }

            final Collection<UUID> lookup = new HashSet<>(nodesSize);

            // Store nodes in map for fast lookup.
            for (GridNode n : nodes)
                // Add nodes into hash circle, if absent.
                lookup.add(resolveNode(n));

            // Cleanup circle.
            if (lookup.size() != addedNodes.size()) {
                Collection<UUID> rmv = null;

                for (Iterator<UUID> iter = addedNodes.iterator(); iter.hasNext(); ) {
                    UUID id = iter.next();

                    if (!lookup.contains(id)) {
                        iter.remove();

                        if (rmv == null)
                            rmv = new ArrayList<>();

                        rmv.add(id);
                    }
                }

                if (!F.isEmpty(rmv))
                    nodeHash.removeNodes(rmv);
            }

            UUID nodeId = nodeHash.node(obj, lookup);

            assert nodeId != null;

            GridNode node = ctx.projection().node(nodeId);

            if (node != null)
                return node;
        }
    }

    /**
     * Add node to hash circle if this is the first node invocation.
     *
     * @param n Node to get info for.
     * @return Node ID.
     */
    private UUID resolveNode(GridNode n) {
        UUID nodeId = n.id();

        if (!addedNodes.contains(nodeId)) {
            addedNodes.add(nodeId);

            nodeHash.addNode(nodeId, REPLICA_CNT);
        }

        return nodeId;
    }
}
