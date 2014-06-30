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

package org.gridgain.loadtests.direct.multisplit;

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
 * Multi-splits load test.
 */
@GridCommonTest(group = "Load Test")
public class GridMultiSplitsLoadTest extends GridCommonAbstractTest {
    /** */
    public GridMultiSplitsLoadTest() {
        super(true);
    }


    /** {@inheritDoc} */
    @SuppressWarnings("ConstantConditions")
    @Override public String getTestGridName() {
        return null;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("ConstantConditions")
    @Override protected GridConfiguration getConfiguration() throws Exception {
        GridConfiguration cfg = super.getConfiguration();

        GridCommunicationSpi commSpi = new GridTcpCommunicationSpi();

        cfg.setCommunicationSpi(commSpi);

        GridDiscoverySpi discoSpi = new GridTcpDiscoverySpi();

        cfg.setDiscoverySpi(discoSpi);

        ((ThreadPoolExecutor)cfg.getExecutorService()).prestartAllCoreThreads();

        return cfg;
    }

    /** {@inheritDoc} */
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

        // Warm up.
        grid.compute().withTimeout(5000).execute(GridLoadTestTask.class.getName(), 3).get();

        info("Load test will be executed for '" + getTestDurationInMinutes() + "' mins.");
        info("Thread count: " + getThreadCount());

        final GridLoadTestStatistics stats = new GridLoadTestStatistics();

        GridTestUtils.runMultiThreaded(new Runnable() {
            /** {@inheritDoc} */
            @Override public void run() {
                while (end - System.currentTimeMillis() > 0) {
                    int levels = 3;

                    int exp = factorial(levels);

                    long start = System.currentTimeMillis();

                    try {
                        GridComputeTaskFuture<Integer> fut = grid.compute().execute(GridLoadTestTask.class, levels);

                        int res = fut.get();

                        if (res != exp)
                            fail("Received wrong result [expected=" + exp + ", actual=" + res + ']');

                        long taskCnt = stats.onTaskCompleted(fut, exp, System.currentTimeMillis() - start);

                        if (taskCnt % 500 == 0)
                            info(stats.toString());
                    }
                    catch (GridException e) {
                        error("Failed to execute grid task.", e);

                        fail();
                    }
                }
            }
        }, getThreadCount(), "grid-notaop-load-test");

        info("Final test statistics: " + stats);
    }

    /**
     * Calculates factorial.
     *
     * @param num Factorial to calculate.
     * @return Factorial for the number passed in.
     */
    private int factorial(int num) {
        assert num > 0;

        if (num == 1)
            return 1;

        return num * factorial(num -1);
    }
}
