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

package org.gridgain.loadtests.direct.redeploy;

import org.gridgain.grid.*;
import org.gridgain.grid.compute.*;
import org.gridgain.grid.loadtest.*;
import org.gridgain.grid.spi.communication.*;
import org.gridgain.grid.spi.communication.tcp.*;
import org.gridgain.grid.spi.discovery.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.testframework.*;
import org.gridgain.testframework.config.*;
import org.gridgain.testframework.junits.common.*;

import java.util.concurrent.*;

/**
 * Single splits redeploy load test.
 */
@GridCommonTest(group = "Load Test")
public class GridSingleSplitsRedeployLoadTest extends GridCommonAbstractTest {
    /** Load test task type ID. */
    public static final String TASK_NAME = "org.gridgain.grid.tests.p2p.GridSingleSplitTestTask";

    /** */
    public GridSingleSplitsRedeployLoadTest() {
        super(true);
    }

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration() throws Exception {
        GridConfiguration cfg = super.getConfiguration();

        GridCommunicationSpi commSpi = new GridTcpCommunicationSpi();

        cfg.setCommunicationSpi(commSpi);

        GridDiscoverySpi discoSpi = new GridTcpDiscoverySpi();

        cfg.setDiscoverySpi(discoSpi);

        ((ThreadPoolExecutor)cfg.getExecutorService()).prestartAllCoreThreads();

        cfg.setDeploymentMode(GridDeploymentMode.CONTINUOUS);

        return cfg;
    }

    /**
     * @return Test timeout.
     */
    @Override protected long getTestTimeout() {
        return (getTestDurationInMinutes() + 1) * 60 * 1000;
    }

    /**
     * @return Time for load test in minutes.
     */
    private int getTestDurationInMinutes() {
        return Integer.valueOf(GridTestProperties.getProperty("load.test.duration"));
    }

    /**
     * @return Number of threads for the test.
     */
    private int getThreadCount() {
        //return 1;
        return Integer.valueOf(GridTestProperties.getProperty("load.test.threadnum"));
    }

    /**
     * Load test grid.
     *
     * @throws Exception If task execution failed.
     */
    public void testLoad() throws Exception {
        final Grid grid = G.grid(getTestGridName());

        final long end = getTestDurationInMinutes() * 60 * 1000 + System.currentTimeMillis();

        grid.compute().localDeployTask(loadTaskClass(), loadTaskClass().getClassLoader());

        info("Load test will be executed for '" + getTestDurationInMinutes() + "' mins.");
        info("Thread count: " + getThreadCount());

        final GridLoadTestStatistics stats = new GridLoadTestStatistics();

        new Thread(new Runnable() {
            /** {@inheritDoc} */
            @SuppressWarnings("BusyWait")
            @Override public void run() {
                try {
                    while (end - System.currentTimeMillis() > 0) {
                        Class<? extends GridComputeTask<?, ?>> cls = loadTaskClass();

                        // info("Deploying class: " + cls);

                        grid.compute().localDeployTask(cls, cls.getClassLoader());

                        Thread.sleep(1000);
                    }
                }
                catch (Exception e) {
                    error("Failed to deploy grid task.", e);

                    fail();
                }
            }

        },  "grid-notaop-deploy-load-test").start();


        GridTestUtils.runMultiThreaded(new Runnable() {
            /** {@inheritDoc} */
            @Override public void run() {
                try {
                    int levels = 3;

                    while (end - System.currentTimeMillis() > 0) {
                        long start = System.currentTimeMillis();

                        // info("Executing task: " + TASK_NAME);

                        GridComputeTaskFuture<Integer> fut = grid.compute().execute(TASK_NAME, levels);

                        int res = fut.get();

                        if (res != levels)
                            fail("Received wrong result [expected=" + levels + ", actual=" + res + ']');

                        long taskCnt = stats.onTaskCompleted(fut, levels, System.currentTimeMillis() - start);

                        if (taskCnt % 500 == 0)
                            info(stats.toString());
                    }
                }
                catch (GridException e) {
                    error("Failed to execute grid task.", e);

                    fail();
                }
            }
        }, getThreadCount(), "grid-notaop-load-test");

        info("Final test statistics: " + stats);
    }

    /**
     * @return Loaded task class.
     * @throws Exception If failed.
     */
    @SuppressWarnings({"unchecked"})
    private Class<? extends GridComputeTask<?, ?>> loadTaskClass() throws Exception {
        return (Class<? extends GridComputeTask<?, ?>>)getExternalClassLoader().loadClass(TASK_NAME);
    }
}
