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
import org.gridgain.testframework.*;
import org.gridgain.testframework.junits.spi.*;

import java.util.*;

import static org.gridgain.grid.spi.loadbalancing.roundrobin.GridRoundRobinTestUtils.*;

/**
 * Tests round robin load balancing with topology changes.
 */
@GridSpiTest(spi = GridRoundRobinLoadBalancingSpi.class, group = "Load Balancing SPI")
public class GridRoundRobinLoadBalancingSpiTopologyChangeSelfTest
    extends GridSpiAbstractTest<GridRoundRobinLoadBalancingSpi> {
    /**
     * @return Per-task configuration parameter.
     */
    @GridSpiTestConfig
    public boolean getPerTask() { return false; }

    /** {@inheritDoc} */
    @Override protected GridSpiTestContext initSpiContext() throws Exception {
        GridSpiTestContext spiCtx = super.initSpiContext();

        spiCtx.createLocalNode();
        spiCtx.createRemoteNodes(10);

        return spiCtx;
    }

    /**
     * @throws Exception If failed.
     */
    public void testTopologyChange() throws Exception {
        GridComputeTaskSession ses = new GridTestTaskSession(GridUuid.randomUuid());

        // Warm up.
        List<GridNode> allNodes = (List<GridNode>)getSpiContext().nodes();

        List<UUID> orderedNodes = getSpi().getNodeIds(ses);

        checkCyclicBalancing(getSpi(), allNodes, orderedNodes, ses);

        // Remove node.
        UUID doomed = orderedNodes.get(0);

        if (getSpiContext().localNode().id().equals(doomed))
            doomed = orderedNodes.get(1);

        getSpiContext().removeNode(doomed);

        assertTrue(allNodes.remove(new GridTestNode(doomed)));

        orderedNodes = getSpi().getNodeIds(ses);

        assertFalse("Balancer uses removed node", orderedNodes.contains(doomed));

        checkCyclicBalancing(getSpi(), allNodes, orderedNodes, ses);

        // Add node.
        GridNode newNode = new GridTestNode(UUID.randomUUID());

        getSpiContext().addNode(newNode);

        assertTrue(allNodes.add(newNode));

        // Check that new node was added to balancing.
        boolean foundNewNode = false;

        for (int i = 0; i < allNodes.size(); i++) {
            GridNode node = getSpi().getBalancedNode(ses, allNodes, new GridTestJob());
            if (newNode.id().equals(node.id())) {
                foundNewNode = true;
                break;
            }
        }

        assertTrue("Balancer doesn't use added node", foundNewNode);

        orderedNodes = getSpi().getNodeIds(ses);

        checkCyclicBalancing(getSpi(), allNodes, orderedNodes, ses);
    }
}
