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
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Multithreaded tests for global load balancer.
 */
@GridSpiTest(spi = GridRoundRobinLoadBalancingSpi.class, group = "Load Balancing SPI")
public class GridRoundRobinLoadBalancingNotPerTaskMultithreadedSelfTest
    extends GridSpiAbstractTest<GridRoundRobinLoadBalancingSpi> {
    /** Thread count. */
    public static final int THREAD_CNT = 8;

    /** Per-thread iteration count. */
    public static final int ITER_CNT = 4_000_000;

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
     *
     * @throws Exception If failed.
     */
    public void testMultipleTaskSessionsMultithreaded() throws Exception {
        final GridRoundRobinLoadBalancingSpi spi = getSpi();

        final List<GridNode> allNodes = (List<GridNode>)getSpiContext().nodes();

        GridTestUtils.runMultiThreaded(new Callable<Object>() {
            @Override public Object call() throws Exception {
                GridComputeTaskSession ses = new GridTestTaskSession(GridUuid.randomUuid());

                Map<UUID, AtomicInteger> nodeCnts = new HashMap<>();

                for (int i = 1; i <= ITER_CNT; i++) {
                    GridNode node = spi.getBalancedNode(ses, allNodes, new GridTestJob());

                    if (!nodeCnts.containsKey(node.id()))
                        nodeCnts.put(node.id(), new AtomicInteger(1));
                    else
                        nodeCnts.get(node.id()).incrementAndGet();
                }

                int predictCnt = ITER_CNT / allNodes.size();

                // Consider +-20% is permissible spread for single node measure.
                int floor = (int)(predictCnt * 0.8);

                double avgSpread = 0;

                for (GridNode n : allNodes) {
                    int curCnt = nodeCnts.get(n.id()).intValue();

                    avgSpread += Math.abs(predictCnt - curCnt);

                    String msg = "Node stats [id=" + n.id() + ", cnt=" + curCnt + ", floor=" + floor +
                        ", predictCnt=" + predictCnt + ']';

                    info(msg);

                    assertTrue(msg, curCnt >= floor);
                }

                avgSpread /= allNodes.size();

                avgSpread = 100.0 * avgSpread / predictCnt;

                info("Average spread for " + allNodes.size() + " nodes is " + avgSpread + " percents");

                // Consider +-10% is permissible average spread for all nodes.
                assertTrue("Average spread is too big: " + avgSpread, avgSpread <= 10);

                return null;
            }
        }, THREAD_CNT, "balancer-test-worker");
    }
}
