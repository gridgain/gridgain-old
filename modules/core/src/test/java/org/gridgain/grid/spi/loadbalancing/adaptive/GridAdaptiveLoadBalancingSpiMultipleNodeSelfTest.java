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
import org.gridgain.testframework.*;
import org.gridgain.testframework.junits.spi.*;
import java.util.*;

/**
 * Tests adaptive load balancing SPI.
 */
@GridSpiTest(spi = GridAdaptiveLoadBalancingSpi.class, group = "Load Balancing SPI")
public class GridAdaptiveLoadBalancingSpiMultipleNodeSelfTest extends GridSpiAbstractTest<GridAdaptiveLoadBalancingSpi> {
    /** */
    private static final int RMT_NODE_CNT = 10;

    /** {@inheritDoc} */
    @Override protected GridSpiTestContext initSpiContext() throws Exception {
        GridSpiTestContext ctx = super.initSpiContext();

        for (int i = 0; i < RMT_NODE_CNT; i++) {
            GridTestNode node = new GridTestNode(UUID.randomUUID());

            node.setAttribute("load", (double)(i + 1));

            ctx.addNode(node);
        }

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
    public void testWeights() throws Exception {
        // Seal it.
        List<GridNode> nodes = new ArrayList<>(getSpiContext().remoteNodes());

        int[] cnts = new int[RMT_NODE_CNT];

        // Invoke load balancer a large number of times, so statistics won't lie.
        for (int i = 0; i < 50000; i++) {
            GridTestNode node = (GridTestNode)getSpi().getBalancedNode(new GridTestTaskSession(GridUuid.randomUuid()),
                nodes, new GridTestJob());

            int idx = ((Double)node.attribute("load")).intValue() - 1;

            if (cnts[idx] == 0)
                node.setAttribute("used", true);

            // Increment number of times a node was picked.
            cnts[idx]++;
        }

        info("Node counts: " + Arrays.toString(cnts));

        for (int i = 0; i < cnts.length - 1; i++) {
            assert cnts[i] > cnts[i + 1] : "Invalid node counts for index [idx=" + i + ", cnts[i]=" + cnts[i] +
                ", cnts[i+1]=" + cnts[i + 1] + ']';
        }
    }
}
