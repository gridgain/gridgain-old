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

package org.gridgain.grid.spi.loadbalancing.adaptive;

import org.gridgain.grid.*;
import org.gridgain.grid.compute.*;
import org.gridgain.testframework.*;
import org.gridgain.testframework.junits.spi.*;

import java.util.*;

/**
 * Tests adaptive load balancing SPI.
 */
@GridSpiTest(spi = GridAdaptiveLoadBalancingSpi.class, group = "Load Balancing SPI")
public class GridAdaptiveLoadBalancingSpiSelfTest extends GridSpiAbstractTest<GridAdaptiveLoadBalancingSpi> {
    /** {@inheritDoc} */
    @Override protected GridSpiTestContext initSpiContext() throws Exception {
        GridSpiTestContext ctx = super.initSpiContext();

        ctx.setLocalNode(new GridTestNode(UUID.randomUUID()));

        return ctx;
    }

    /**
     * @return {@code True} if node weights should be considered.
     */
    @GridSpiTestConfig
    public GridAdaptiveLoadProbe getLoadProbe() {
        return new GridAdaptiveLoadProbe() {
            @Override public double getLoad(GridNode node, int jobsSentSinceLastUpdate) {
                boolean isFirstTime = node.attribute("used") == null;

                assert isFirstTime ? jobsSentSinceLastUpdate == 0 : jobsSentSinceLastUpdate > 0;

                return (Double)node.attribute("load");
            }
        };
    }

    /**
     * @throws Exception If failed.
     */
    @SuppressWarnings({"ObjectEquality"})
    public void testSingleNodeZeroWeight() throws Exception {
        GridTestNode node = (GridTestNode)getSpiContext().nodes().iterator().next();

        node.addAttribute("load", 0d);

        List<GridNode> nodes = Collections.singletonList((GridNode)node);

        GridComputeTaskSession ses = new GridTestTaskSession(GridUuid.randomUuid());

        GridTestNode pick1 = (GridTestNode)getSpi().getBalancedNode(ses, nodes, new GridTestJob());

        pick1.setAttribute("used", true);

        assert nodes.contains(pick1);

        // Verify that same instance is returned every time.
        GridNode pick2 = getSpi().getBalancedNode(ses, nodes, new GridTestJob());

        assert pick1 == pick2;
    }

    /**
     * @throws Exception If failed.
     */
    @SuppressWarnings({"ObjectEquality"})
    public void testSingleNodeSameSession() throws Exception {
        GridTestNode node = (GridTestNode)getSpiContext().nodes().iterator().next();

        node.addAttribute("load", 1d);

        List<GridNode> nodes = Collections.singletonList((GridNode)node);

        GridComputeTaskSession ses = new GridTestTaskSession(GridUuid.randomUuid());

        GridTestNode pick1 = (GridTestNode)getSpi().getBalancedNode(ses, nodes, new GridTestJob());

        pick1.setAttribute("used", true);

        assert nodes.contains(pick1);

        // Verify that same instance is returned every time.
        GridNode pick2 = getSpi().getBalancedNode(ses, nodes, new GridTestJob());

        assert pick1 == pick2;
    }

    /**
     * @throws Exception If failed.
     */
    @SuppressWarnings({"ObjectEquality"})
    public void testSingleNodeDifferentSession() throws Exception {
        GridTestNode node = (GridTestNode)getSpiContext().nodes().iterator().next();

        node.addAttribute("load", 2d);

        List<GridNode> nodes = Collections.singletonList((GridNode)node);

        GridTestNode pick1 = (GridTestNode)getSpi().getBalancedNode(new GridTestTaskSession(GridUuid.randomUuid()),
            nodes, new GridTestJob());

        pick1.setAttribute("used", true);

        assert nodes.contains(pick1);

        // Verify that same instance is returned every time.
        GridNode pick2 = getSpi().getBalancedNode(new GridTestTaskSession(GridUuid.randomUuid()), nodes,
            new GridTestJob());

        assert pick1 == pick2;
    }
}
