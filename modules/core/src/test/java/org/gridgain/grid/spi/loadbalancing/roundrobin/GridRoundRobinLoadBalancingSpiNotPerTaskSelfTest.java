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
import org.gridgain.grid.events.*;
import org.gridgain.testframework.*;
import org.gridgain.testframework.junits.spi.*;

import java.util.*;

import static org.gridgain.grid.events.GridEventType.*;
import static org.gridgain.grid.spi.loadbalancing.roundrobin.GridRoundRobinTestUtils.*;

/**
 * Tests round robin load balancing.
 */
@GridSpiTest(spi = GridRoundRobinLoadBalancingSpi.class, group = "Load Balancing SPI")
public class GridRoundRobinLoadBalancingSpiNotPerTaskSelfTest
    extends GridSpiAbstractTest<GridRoundRobinLoadBalancingSpi> {
    /**
     * @return Per-task configuration parameter.
     */
    @GridSpiTestConfig
    public boolean getPerTask() {
        return false;
    }

    /** {@inheritDoc} */
    @Override protected GridSpiTestContext initSpiContext() throws Exception {
        GridSpiTestContext spiCtx = super.initSpiContext();

        spiCtx.createLocalNode();
        spiCtx.createRemoteNodes(10);

        return spiCtx;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        assert !getSpi().isPerTask() : "Invalid SPI configuration.";
    }

    /**
     * @throws Exception If test failed.
     */
    public void testMultipleNodes() throws Exception {
        List<GridNode> allNodes = (List<GridNode>)getSpiContext().nodes();

        GridComputeTaskSession ses = new GridTestTaskSession();

        List<UUID> orderedNodes = new ArrayList<>(getSpi().getNodeIds(ses));

        assertEquals("Balancer doesn't use all available nodes", orderedNodes.size(), allNodes.size());

        checkCyclicBalancing(getSpi(), allNodes, orderedNodes, ses);
    }

    /**
     * @throws Exception If test failed.
     */
    public void testMultipleTaskSessions() throws Exception {
        GridComputeTaskSession ses1 = new GridTestTaskSession(GridUuid.randomUuid());
        GridComputeTaskSession ses2 = new GridTestTaskSession(GridUuid.randomUuid());

        List<GridNode> allNodes = (List<GridNode>)getSpiContext().nodes();

        List<UUID> orderedNodes = getSpi().getNodeIds(ses1);

        assertEquals("Balancer doesn't use all available nodes", orderedNodes.size(), allNodes.size());

        checkCyclicBalancing(getSpi(), allNodes, orderedNodes, ses1, ses2);

        getSpiContext().triggerEvent(new GridTaskEvent(
            null, null, EVT_TASK_FINISHED, ses1.getId(), null, null, false, null));
        getSpiContext().triggerEvent(new GridTaskEvent(
            null, null, EVT_TASK_FAILED, ses2.getId(), null, null, false, null));
    }

    /**
     * @throws Exception If test failed.
     */
    public void testBalancingOneNode() throws Exception {
        GridComputeTaskSession ses = new GridTestTaskSession();

        List<GridNode> allNodes = (List<GridNode>)getSpiContext().nodes();

        List<GridNode> balancedNode = Arrays.asList(allNodes.get(0));

        GridNode firstNode = getSpi().getBalancedNode(ses, balancedNode, new GridTestJob());
        GridNode secondNode = getSpi().getBalancedNode(ses, balancedNode, new GridTestJob());

        assertEquals(firstNode, secondNode);
    }

    /** */
    public void testNodeNotInTopology() {
        GridComputeTaskSession ses = new GridTestTaskSession();

        GridNode node = new GridTestNode(UUID.randomUUID());

        List<GridNode> notInTop = Arrays.asList(node);

        try {
            getSpi().getBalancedNode(ses, notInTop, new GridTestJob());
        }
        catch (GridException e) {
            assertTrue(e.getMessage().contains("Task topology does not have alive nodes"));
        }
    }
}
