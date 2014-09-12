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

package org.gridgain.grid.spi.loadbalancing.roundrobin;

import org.gridgain.grid.*;
import org.gridgain.grid.events.*;
import org.gridgain.grid.kernal.managers.eventstorage.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static org.gridgain.grid.events.GridEventType.*;

/**
 * Load balancer that works in global (not-per-task) mode.
 */
class GridRoundRobinGlobalLoadBalancer {
    /** SPI context. */
    private GridSpiContext ctx;

    /** Listener for node's events. */
    private GridLocalEventListener lsnr;

    /** Logger. */
    private final GridLogger log;

    /** Current snapshot of nodes which participated in load balancing. */
    private volatile GridNodeList nodeList = new GridNodeList(0, null);

    /** Mutex for updating current topology. */
    private final Object mux = new Object();

    /** Barrier for separating initialization callback and load balancing routine. */
    private final CountDownLatch initLatch = new CountDownLatch(1);

    /**
     * @param log Grid logger.
     */
    GridRoundRobinGlobalLoadBalancer(GridLogger log) {
        assert log != null;

        this.log = log;
    }

    /**
     * @param ctx Load balancing context.
     */
    void onContextInitialized(final GridSpiContext ctx) {
        this.ctx = ctx;

        ctx.addLocalEventListener(
            lsnr = new GridLocalEventListener() {
                @Override public void onEvent(GridEvent evt) {
                    assert evt instanceof GridDiscoveryEvent;

                    UUID nodeId = ((GridDiscoveryEvent)evt).eventNode().id();

                    synchronized (mux) {
                        if (evt.type() == EVT_NODE_JOINED) {
                            List<UUID> oldNodes = nodeList.getNodes();

                            if (!oldNodes.contains(nodeId)) {
                                List<UUID> newNodes = new ArrayList<>(oldNodes.size() + 1);

                                newNodes.add(nodeId);

                                for (UUID node : oldNodes)
                                    newNodes.add(node);

                                nodeList = new GridNodeList(0, newNodes);
                            }
                        }
                        else {
                            assert evt.type() == EVT_NODE_LEFT || evt.type() == EVT_NODE_FAILED;

                            List<UUID> oldNodes = nodeList.getNodes();

                            if (oldNodes.contains(nodeId)) {
                                List<UUID> newNodes = new ArrayList<>(oldNodes.size() - 1);

                                for (UUID node : oldNodes)
                                    if (!nodeId.equals(node))
                                        newNodes.add(node);

                                nodeList = new GridNodeList(0, newNodes);
                            }
                        }
                    }
                }
            },
            EVT_NODE_FAILED, EVT_NODE_JOINED, EVT_NODE_LEFT
        );

        synchronized (mux) {
            List<UUID> oldNodes = nodeList.getNodes();

            Collection<UUID> set = oldNodes == null ? new HashSet<UUID>() : new HashSet<>(oldNodes);

            for (GridNode node : ctx.nodes())
                set.add(node.id());

            nodeList = new GridNodeList(0, new ArrayList<>(set));
        }

        initLatch.countDown();
    }

    /** */
    void onContextDestroyed() {
        if (ctx != null)
            ctx.removeLocalEventListener(lsnr);
    }

    /**
     * Gets balanced node for given topology.
     *
     * @param top Topology to pick from.
     * @return Best balanced node.
     * @throws GridException Thrown in case of any error.
     */
    GridNode getBalancedNode(Collection<GridNode> top) throws GridException {
        assert !F.isEmpty(top);

        awaitInitializationCompleted();

        Map<UUID, GridNode> topMap = null;

        GridNode found;

        int misses = 0;

        do {
            GridNodeList nodeList = this.nodeList;

            List<UUID> nodes = nodeList.getNodes();

            int cycleSize = nodes.size();

            if (cycleSize == 0)
                throw new GridException("Task topology does not have any alive nodes.");

            AtomicInteger idx;

            int curIdx, nextIdx;

            do {
                idx = nodeList.getCurrentIdx();

                curIdx = idx.get();

                nextIdx = (idx.get() + 1) % cycleSize;
            }
            while (!idx.compareAndSet(curIdx, nextIdx));

            found = findNodeById(top, nodes.get(nextIdx));

            if (found == null) {
                misses++;

                // For optimization purposes checks balancer can return at least one node with specified
                // request topology only after full cycle (approximately).
                if (misses >= cycleSize) {
                    if (topMap == null) {
                        topMap = new HashMap<>(top.size());

                        for (GridNode node : top)
                            topMap.put(node.id(), node);
                    }

                    checkBalancerNodes(top, topMap, nodes);

                    // Zero miss counter so next topology check will be performed once again after full cycle.
                    misses = 0;
                }
            }
        }
        while (found == null);

        if (log.isDebugEnabled())
            log.debug("Found round-robin node: " + found);

        return found;
    }

    /**
     * Finds node by id. Returns null in case of absence of specified id in request topology.
     *
     * @param top Topology for current request.
     * @param foundNodeId Node id.
     * @return Found node or null in case of absence of specified id in request topology.
     */
    private static GridNode findNodeById(Iterable<GridNode> top, UUID foundNodeId) {
        for (GridNode node : top)
            if (foundNodeId.equals(node.id()))
                return node;

        return null;
    }

    /**
     * Checks if balancer can return at least one node,
     * throw exception otherwise.
     *
     * @param top Topology for current request.
     * @param topMap Topology map.
     * @param nodes Current balanced nodes.
     * @throws GridException If balancer can not return any node.
     */
    private static void checkBalancerNodes(Collection<GridNode> top, Map<UUID, GridNode> topMap, Iterable<UUID> nodes)
        throws GridException {

        boolean contains = false;

        for (UUID nodeId : nodes) {
            if (topMap.get(nodeId) != null) {
                contains = true;

                break;
            }
        }

        if (!contains)
            throw new GridException("Task topology does not have alive nodes: " + top);
    }

    /**
     * Awaits initialization of balancing nodes to be completed.
     *
     * @throws GridException Thrown in case of thread interruption.
     */
    private void awaitInitializationCompleted() throws GridException {
        try {
            if (initLatch.getCount() > 0)
                initLatch.await();
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new GridException("Global balancer was interrupted.", e);
        }
    }

    /**
     * Snapshot of nodes which participated in load balancing.
     */
    private static final class GridNodeList {
        /** Cyclic pointer for selecting next node. */
        private final AtomicInteger curIdx;

        /** Node ids. */
        private final List<UUID> nodes;

        /**
         * @param curIdx Initial index of current node.
         * @param nodes Initial node ids.
         */
        private GridNodeList(int curIdx, List<UUID> nodes) {
            this.curIdx = new AtomicInteger(curIdx);
            this.nodes = nodes;
        }

        /**
         * @return Index of current node.
         */
        private AtomicInteger getCurrentIdx() {
            return curIdx;
        }

        /**
         * @return Node ids.
         */
        private List<UUID> getNodes() {
            return nodes;
        }
    }

    /**
     * THIS METHOD IS USED ONLY FOR TESTING.
     *
     * @return Internal list of nodes.
     */
    List<UUID> getNodeIds() {
        List<UUID> nodes = nodeList.getNodes();

        return Collections.unmodifiableList(nodes);
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridRoundRobinGlobalLoadBalancer.class, this);
    }
}
