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

/**
 * Tests round robin load balancing SPI.
 */
@GridSpiTest(spi = GridRoundRobinLoadBalancingSpi.class, group = "Load Balancing SPI")
public class GridRoundRobinLoadBalancingSpiMultipleNodesSelfTest
    extends GridSpiAbstractTest<GridRoundRobinLoadBalancingSpi> {
    /** {@inheritDoc} */
    @Override protected void spiConfigure(GridRoundRobinLoadBalancingSpi spi) throws Exception {
        super.spiConfigure(spi);

        spi.setPerTask(true);
    }

    /** {@inheritDoc} */
    @Override protected GridSpiTestContext initSpiContext() throws Exception {
        GridSpiTestContext spiCtx = super.initSpiContext();

        spiCtx.createLocalNode();
        spiCtx.createRemoteNodes(10);

        return spiCtx;
    }

    /**
     * @throws Exception If test failed.
     */
    @SuppressWarnings({"ObjectEquality"})
    public void testMultipleNodes() throws Exception {
        List<GridNode> allNodes = (List<GridNode>)getSpiContext().nodes();

        GridComputeTaskSession ses = new GridTestTaskSession(GridUuid.randomUuid());

        // Initialize.
        getSpi().getBalancedNode(ses, allNodes, new GridTestJob());

        List<UUID> orderedNodes = new ArrayList<>(getSpi().getNodeIds(ses));

        // Check the round-robin actually did circle.
        for (int i = 0; i < allNodes.size(); i++) {
            GridNode node = getSpi().getBalancedNode(ses, allNodes, new GridTestJob());

            assert orderedNodes.get(i) == node.id();
        }

        // Double-check.
        for (int i = 0; i < allNodes.size(); i++) {
            GridNode node = getSpi().getBalancedNode(ses, allNodes, new GridTestJob());

            assert orderedNodes.get(i) == node.id();
        }
    }

    /**
     * @throws Exception If test failed.
     */
    @SuppressWarnings({"ObjectEquality"})
    public void testMultipleTasks() throws Exception {
        GridComputeTaskSession ses1 = new GridTestTaskSession(GridUuid.randomUuid());
        GridComputeTaskSession ses2 = new GridTestTaskSession(GridUuid.randomUuid());

        List<GridNode> allNodes = (List<GridNode>)getSpiContext().nodes();

        // Initialize.
        getSpi().getBalancedNode(ses1, allNodes, new GridTestJob());
        getSpi().getBalancedNode(ses2, allNodes, new GridTestJob());

        List<UUID> orderedNodes1 = getSpi().getNodeIds(ses1);
        List<UUID> orderedNodes2 = getSpi().getNodeIds(ses2);

        assert orderedNodes1 != orderedNodes2;

        // Check the round-robin actually did circle.
        for (int i = 0; i < allNodes.size(); i++) {
            GridNode node1 = getSpi().getBalancedNode(ses1, allNodes, new GridTestJob());

            assert orderedNodes1.get(i) == node1.id();

            GridNode node2 = getSpi().getBalancedNode(ses2, allNodes, new GridTestJob());

            assert orderedNodes2.get(i) == node2.id();

            assert orderedNodes1.get(i) == orderedNodes2.get(i);
        }

        // Double-check.
        for (int i = 0; i < allNodes.size(); i++) {
            GridNode node1 = getSpi().getBalancedNode(ses1, allNodes, new GridTestJob());

            assert orderedNodes1.get(i) == node1.id();

            GridNode node2 = getSpi().getBalancedNode(ses2, allNodes, new GridTestJob());

            assert orderedNodes2.get(i) == node2.id();

            assert orderedNodes1.get(i) == orderedNodes2.get(i);
        }

        getSpiContext().triggerEvent(new GridTaskEvent(
            null, null, EVT_TASK_FINISHED, ses1.getId(), null, null, false, null));
        getSpiContext().triggerEvent(new GridTaskEvent(
            null, null, EVT_TASK_FAILED, ses2.getId(), null, null, false, null));
    }
}
