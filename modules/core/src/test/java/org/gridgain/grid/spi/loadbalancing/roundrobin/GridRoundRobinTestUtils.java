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
import org.gridgain.grid.compute.*;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Helper class for balancer tests.
 */
class GridRoundRobinTestUtils {
    /**
     * Performs two full cycles by round robin routine for check correct order.
     *
     * @param spi Load balancing SPI.
     * @param allNodes Topology nodes.
     * @param orderedNodes Balancing nodes.
     * @param ses Task session.
     * @throws GridException If balancer failed.
     */
    static void checkCyclicBalancing(GridRoundRobinLoadBalancingSpi spi, List<GridNode> allNodes,
        List<UUID> orderedNodes, GridComputeTaskSession ses) throws GridException {

        GridNode firstNode = spi.getBalancedNode(ses, allNodes, new GridTestJob());

        int startIdx = firstBalancedNodeIndex(firstNode, orderedNodes);

        // Two full cycles by round robin routine.
        for (int i = 0; i < allNodes.size() * 2; i++) {
            int actualIdx = (startIdx + i + 1) % allNodes.size();

            GridNode nextNode = spi.getBalancedNode(ses, allNodes, new GridTestJob());

            assertEquals("Balancer returns node out of order", nextNode.id(), orderedNodes.get(actualIdx));
        }
    }

    /**
     * Performs two full cycles by round robin routine for check correct order.
     * Switches between two task sessions by turns.
     *
     * @param spi Load balancing SPI.
     * @param allNodes Topology nodes.
     * @param orderedNodes Balancing nodes.
     * @param ses1 First task session.
     * @param ses2 Second task session.
     * @throws GridException If balancer failed.
     */
    static void checkCyclicBalancing(GridRoundRobinLoadBalancingSpi spi, List<GridNode> allNodes,
        List<UUID> orderedNodes, GridComputeTaskSession ses1, GridComputeTaskSession ses2) throws GridException {

        GridNode firstNode = spi.getBalancedNode(ses1, allNodes, new GridTestJob());

        int startIdx = firstBalancedNodeIndex(firstNode, orderedNodes);

        // Two full cycles by round robin routine.
        for (int i = 0; i < allNodes.size() * 2; i++) {
            int actualIdx = (startIdx + i + 1) % allNodes.size();

            GridNode nextNode = spi.getBalancedNode(i % 2 == 0 ? ses1 : ses2, allNodes, new GridTestJob());

            assertEquals("Balancer returns node out of order", nextNode.id(), orderedNodes.get(actualIdx));
        }
    }

    /**
     * @param firstNode First node which was return by balancer.
     * @param orderedNodes Balancing nodes.
     * @return Index of first node which was return by balancer.
     */
    static int firstBalancedNodeIndex(GridNode firstNode, List<UUID> orderedNodes) {
        int startIdx = -1;

        for (int i = 0; i < orderedNodes.size(); i++) {
            if (firstNode.id() == orderedNodes.get(i))
                startIdx = i;
        }

        assertTrue("Can't find position of first balanced node", startIdx >= 0);

        return startIdx;
    }
}
