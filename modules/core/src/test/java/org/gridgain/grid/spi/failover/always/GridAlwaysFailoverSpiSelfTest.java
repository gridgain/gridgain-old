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

package org.gridgain.grid.spi.failover.always;

import org.gridgain.grid.*;
import org.gridgain.grid.compute.*;
import org.gridgain.grid.spi.failover.*;
import org.gridgain.testframework.*;
import org.gridgain.testframework.junits.spi.*;

import java.util.*;

import static org.gridgain.grid.spi.failover.always.GridAlwaysFailoverSpi.*;

/**
 * Always-failover SPI test.
 */
@GridSpiTest(spi = GridAlwaysFailoverSpi.class, group = "Failover SPI")
public class GridAlwaysFailoverSpiSelfTest extends GridSpiAbstractTest<GridAlwaysFailoverSpi> {
    /**
     * @throws Exception If failed.
     */
    public void testSingleNode() throws Exception {
        GridAlwaysFailoverSpi spi = getSpi();

        List<GridNode> nodes = new ArrayList<>();

        GridNode node = new GridTestNode(UUID.randomUUID());

        nodes.add(node);

        node = spi.failover(new GridFailoverTestContext(new GridTestTaskSession(), new GridTestJobResult(node)), nodes);

        assert node == null;
    }

    /**
     * @throws Exception If test failed.
     */
    @SuppressWarnings("unchecked")
    public void testTwoNodes() throws Exception {
        GridAlwaysFailoverSpi spi = getSpi();

        List<GridNode> nodes = new ArrayList<>();

        nodes.add(new GridTestNode(UUID.randomUUID()));
        nodes.add(new GridTestNode(UUID.randomUUID()));

        GridComputeJobResult jobRes = new GridTestJobResult(nodes.get(0));

        GridNode node = spi.failover(new GridFailoverTestContext(new GridTestTaskSession(), jobRes), nodes);

        assert node != null;
        assert node.equals(nodes.get(1));

        checkFailedNodes(jobRes, 1);
    }

    /**
     * @throws Exception If failed.
     */
    public void testMaxAttempts() throws Exception {
        GridAlwaysFailoverSpi spi = getSpi();

        spi.setMaximumFailoverAttempts(1);

        List<GridNode> nodes = new ArrayList<>();

        nodes.add(new GridTestNode(UUID.randomUUID()));
        nodes.add(new GridTestNode(UUID.randomUUID()));

        GridComputeJobResult jobRes = new GridTestJobResult(nodes.get(0));

        // First attempt.
        GridNode node = spi.failover(new GridFailoverTestContext(new GridTestTaskSession(), jobRes), nodes);

        assert node != null;
        assert node.equals(nodes.get(1));

        checkFailedNodes(jobRes, 1);

        // Second attempt (exceeds default max attempts of 1).
        node = spi.failover(new GridFailoverTestContext(new GridTestTaskSession(), jobRes), nodes);

        assert node == null;

        checkFailedNodes(jobRes, 1);
    }

    /**
     * @param res Job result.
     * @param cnt Failure count.
     */
    @SuppressWarnings("unchecked")
    private void checkFailedNodes(GridComputeJobResult res, int cnt) {
        Collection<UUID> failedNodes =
            (Collection<UUID>)res.getJobContext().getAttribute(FAILED_NODE_LIST_ATTR);

        assert failedNodes != null;
        assert failedNodes.size() == cnt;
    }
}
