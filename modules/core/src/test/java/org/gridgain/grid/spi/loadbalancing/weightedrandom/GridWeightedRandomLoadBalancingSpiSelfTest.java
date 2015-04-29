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

package org.gridgain.grid.spi.loadbalancing.weightedrandom;

import org.gridgain.grid.*;
import org.gridgain.testframework.*;
import org.gridgain.testframework.junits.spi.*;
import java.util.*;

/**
 * Weighted random load balancing SPI.
 */
@GridSpiTest(spi = GridWeightedRandomLoadBalancingSpi.class, group = "Load Balancing SPI")
public class GridWeightedRandomLoadBalancingSpiSelfTest extends
    GridSpiAbstractTest<GridWeightedRandomLoadBalancingSpi> {
    /**
     * @throws Exception If failed.
     */
    @SuppressWarnings({"ObjectEquality"})
    public void testSingleNode() throws Exception {
        List<GridNode> nodes = Collections.singletonList((GridNode)new GridTestNode(UUID.randomUUID()));

        GridNode node = getSpi().getBalancedNode(new GridTestTaskSession(), nodes, new GridTestJob());

        assert nodes.contains(node);

        // Verify that same instance is returned every time.
        GridNode balancedNode = getSpi().getBalancedNode(new GridTestTaskSession(), nodes, new GridTestJob());

        assert node == balancedNode;
    }

    /**
     * @throws Exception If failed.
     */
    public void testMultipleNodes() throws Exception {
        List<GridNode> nodes = new ArrayList<>();

        for (int i = 0; i < 10; i++)
            nodes.add(new GridTestNode(UUID.randomUUID()));

        // Seal it.
        nodes = Collections.unmodifiableList(nodes);

        GridNode node = getSpi().getBalancedNode(new GridTestTaskSession(), nodes, new GridTestJob());

        assert node != null;
        assert nodes.contains(node);
    }
}
