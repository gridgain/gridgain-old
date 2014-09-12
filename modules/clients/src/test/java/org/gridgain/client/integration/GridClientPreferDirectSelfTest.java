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

package org.gridgain.client.integration;

import org.gridgain.client.*;
import org.gridgain.client.balancer.*;
import org.gridgain.grid.*;
import org.gridgain.grid.compute.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.testframework.junits.common.*;

import java.util.*;

import static org.gridgain.client.integration.GridClientAbstractMultiNodeSelfTest.*;

/**
 *
 */
public class GridClientPreferDirectSelfTest extends GridCommonAbstractTest {
    /** VM ip finder for TCP discovery. */
    private static final GridTcpDiscoveryIpFinder IP_FINDER = new GridTcpDiscoveryVmIpFinder(true);

    /** */
    private static final int NODES_CNT = 6;

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        startGrids(NODES_CNT);
    }

    /** {@inheritDoc} */
    @Override protected void afterTestsStopped() throws Exception {
        info("Stopping grids.");

        stopAllGrids();
    }

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration c = super.getConfiguration(gridName);

        GridTcpDiscoverySpi disco = new GridTcpDiscoverySpi();

        disco.setIpFinder(IP_FINDER);

        c.setDiscoverySpi(disco);

        c.setLocalHost(HOST);

        assert c.getClientConnectionConfiguration() == null;

        GridClientConnectionConfiguration clientCfg = new GridClientConnectionConfiguration();

        clientCfg.setRestTcpPort(REST_TCP_PORT_BASE);

        c.setClientConnectionConfiguration(clientCfg);

        return c;
    }

    /**
     * @throws Exception If failed.
     */
    public void testRandomBalancer() throws Exception {
        GridClientRandomBalancer b = new GridClientRandomBalancer();

        b.setPreferDirectNodes(true);

        executeTest(b);
    }

    /**
     * @throws Exception If failed.
     */
    public void testRoundRobinBalancer() throws Exception {
        GridClientRoundRobinBalancer b = new GridClientRoundRobinBalancer();

        b.setPreferDirectNodes(true);

        executeTest(b);
    }

    /**
     * @throws Exception If failed.
     */
    @SuppressWarnings("TypeMayBeWeakened")
    private void executeTest(GridClientLoadBalancer b) throws Exception {
        try (GridClient c = client(b)) {
            Set<String> executions = new HashSet<>();

            for (int i = 0; i < NODES_CNT * 10; i++)
                executions.add(
                    c.compute().<String>execute(TestTask.class.getName(), null));

            assertEquals(NODES_CNT / 2, executions.size());

            for (int i = 0; i < NODES_CNT / 2; i++)
                executions.contains(grid(i).localNode().id().toString());
        }
    }

    /**
     * @param b Balancer.
     * @return Client.
     * @throws Exception If failed.
     */
    private GridClient client(GridClientLoadBalancer b) throws Exception {
        GridClientConfiguration cfg = new GridClientConfiguration();

        cfg.setBalancer(b);

        cfg.setTopologyRefreshFrequency(TOP_REFRESH_FREQ);

        Collection<String> rtrs = new ArrayList<>(3);

        for (int i = 0; i < NODES_CNT / 2; i++)
            rtrs.add(HOST + ':' + (REST_TCP_PORT_BASE + i));

        cfg.setRouters(rtrs);

        return GridClientFactory.start(cfg);
    }

    /**
     * Test task. Returns Id of the node that has split the task,
     */
    private static class TestTask extends GridComputeTaskSplitAdapter<Object, String> {
        @GridInstanceResource
        private Grid grid;

        /** Count of tasks this job was split to. */
        private int gridSize;

        /** {@inheritDoc} */
        @Override protected Collection<? extends GridComputeJob> split(int gridSize, Object arg)
            throws GridException {
            Collection<GridComputeJobAdapter> jobs = new ArrayList<>(gridSize);

            this.gridSize = gridSize;

            for (int i = 0; i < gridSize; i++) {
                jobs.add(new GridComputeJobAdapter() {
                    @Override public Object execute() {
                        try {
                            Thread.sleep(100);
                        }
                        catch (InterruptedException ignored) {
                            Thread.currentThread().interrupt();
                        }

                        return "OK";
                    }
                });
            }

            return jobs;
        }

        /** {@inheritDoc} */
        @Override public String reduce(List<GridComputeJobResult> results) throws GridException {
            int sum = 0;

            for (GridComputeJobResult res : results) {
                assertNotNull(res.getData());

                sum += 1;
            }

            assert gridSize == sum;

            return grid.localNode().id().toString();
        }
    }
}
