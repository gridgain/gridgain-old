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
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.testframework.*;
import org.gridgain.testframework.junits.spi.*;
import java.util.*;

import static org.gridgain.grid.spi.loadbalancing.weightedrandom.GridWeightedRandomLoadBalancingSpi.*;

/**
 * {@link GridWeightedRandomLoadBalancingSpi} self test.
 */
@GridSpiTest(spi = GridWeightedRandomLoadBalancingSpi.class, group = "Load Balancing SPI")
public class GridWeightedRandomLoadBalancingSpiWeightedSelfTest
    extends GridSpiAbstractTest<GridWeightedRandomLoadBalancingSpi> {
    /**
     * @return {@code True} if node weights should be considered.
     */
    @GridSpiTestConfig
    public boolean getUseWeights() {
        return true;
    }

    /**
     * @throws Exception If test failed.
     */
    public void testWeights() throws Exception {
        List<GridNode> nodes = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            GridTestNode node = new GridTestNode(UUID.randomUUID());

            node.addAttribute(U.spiAttribute(getSpi(), NODE_WEIGHT_ATTR_NAME), i + 1);

            nodes.add(node);
        }

        // Seal it.
        nodes = Collections.unmodifiableList(nodes);

        int[] cnts = new int[10];

        // Invoke load balancer a large number of times, so statistics won't lie.
        for (int i = 0; i < 100000; i++) {
            GridNode node = getSpi().getBalancedNode(new GridTestTaskSession(GridUuid.randomUuid()), nodes,
                new GridTestJob());

            int weight = (Integer)node.attribute(U.spiAttribute(getSpi(), NODE_WEIGHT_ATTR_NAME));

            // Increment number of times a node was picked.
            cnts[weight - 1]++;
        }

        for (int i = 0; i < cnts.length - 1; i++) {
            assert cnts[i] < cnts[i + 1] : "Invalid node counts for index [idx=" + i + ", cnts[i]=" + cnts[i] +
                ", cnts[i+1]=" + cnts[i + 1] + ']';
        }

        info("Node counts: " + Arrays.toString(cnts));
    }
}
