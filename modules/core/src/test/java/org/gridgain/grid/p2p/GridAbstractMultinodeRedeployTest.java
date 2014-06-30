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

package org.gridgain.grid.p2p;

import org.gridgain.grid.*;
import org.gridgain.grid.compute.*;
import org.gridgain.grid.spi.failover.never.*;
import org.gridgain.testframework.*;
import org.gridgain.testframework.config.*;
import org.gridgain.testframework.junits.common.*;

import java.net.*;
import java.util.*;

/**
 * Common test for deploy modes.
 */
abstract class GridAbstractMultinodeRedeployTest extends GridCommonAbstractTest {
    /** Number of iterations. */
    private static final int ITERATIONS = 1000;

    /** Current deployment mode. Used in {@link #getConfiguration(String)}. */
    private GridDeploymentMode depMode;

    /** */
    private static final String TASK_NAME = "org.gridgain.grid.tests.p2p.GridP2PTestTaskExternalPath1";

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        cfg.setDeploymentMode(depMode);

        cfg.setFailoverSpi(new GridNeverFailoverSpi());

        cfg.setNetworkTimeout(10000);

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected long getTestTimeout() {
        return getTestDurationInSeconds() * 2 * 1000;
    }

    /**
     * @return Time for load test in seconds.
     */
    private int getTestDurationInSeconds() {
        return 30;
    }

    /**
     * @return Number of threads for the test.
     */
    private int getThreadCount() {
        return 10;
    }

    /**
     * @param depMode deployment mode.
     * @throws Throwable If task execution failed.
     */
    protected void processTest(GridDeploymentMode depMode) throws Throwable {
        this.depMode = depMode;

        try {
            final Grid grid1 = startGrid(1);
            final Grid grid2 = startGrid(2);
            final Grid grid3 = startGrid(3);

            for (int i = 0; i < ITERATIONS; i++) {
                grid1.compute().localDeployTask(loadTaskClass(), loadTaskClass().getClassLoader());
                grid2.compute().localDeployTask(loadTaskClass(), loadTaskClass().getClassLoader());

                GridComputeTaskFuture<int[]> fut1 = grid1.compute().execute(TASK_NAME, Arrays.<UUID>asList(
                    grid1.localNode().id(),
                    grid2.localNode().id(),
                    grid3.localNode().id()));

                GridComputeTaskFuture<int[]> fut2 = grid2.compute().execute(TASK_NAME, Arrays.<UUID>asList(
                    grid1.localNode().id(),
                    grid2.localNode().id(),
                    grid3.localNode().id()));

                int[] result1 = fut1.get();
                int[] result2 = fut2.get();

                if (result1 == null || result2 == null || result1.length != 2 || result2.length != 2)
                    throw new GridException("Received wrong result.");
            }
        }
        finally {
            stopGrid(1);
            stopGrid(2);
            stopGrid(3);
        }
    }

    /**
     * @return Loaded class.
     * @throws Exception Thrown if any exception occurs.
     */
    @SuppressWarnings({"unchecked"})
    private Class<? extends GridComputeTask<int[], ?>> loadTaskClass() throws Exception {
        return (Class<? extends GridComputeTask<int[], ?>>)new GridTestExternalClassLoader(new URL[]{
            new URL(GridTestProperties.getProperty("p2p.uri.cls"))}).loadClass(TASK_NAME);
    }
}
