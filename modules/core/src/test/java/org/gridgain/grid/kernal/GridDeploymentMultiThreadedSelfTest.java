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

package org.gridgain.grid.kernal;

import org.gridgain.grid.*;
import org.gridgain.grid.compute.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.testframework.*;
import org.gridgain.testframework.junits.common.*;

import java.util.*;
import java.util.concurrent.*;

import static java.util.concurrent.TimeUnit.*;

/**
 * Task deployment tests.
 */
public class GridDeploymentMultiThreadedSelfTest extends GridCommonAbstractTest {
    /** */
    private static final int THREAD_CNT = 20;

    /** */
    private static final int EXEC_CNT = 30000;

    /**
     * @throws Exception If failed.
     */
    public void testDeploy() throws Exception {
        try {
            final Grid grid = startGrid(0);

            grid.compute().localDeployTask(GridDeploymentTestTask.class, GridDeploymentTestTask.class.getClassLoader());

            assert grid.compute().localTasks().get(GridDeploymentTestTask.class.getName()) != null;

            grid.compute().undeployTask(GridDeploymentTestTask.class.getName());

            final CyclicBarrier barrier = new CyclicBarrier(THREAD_CNT, new Runnable() {
                private int iterCnt;

                @Override public void run() {
                    try {
                        grid.compute().undeployTask(GridDeploymentTestTask.class.getName());

                        assert grid.compute().localTasks().get(GridDeploymentTestTask.class.getName()) == null;

                        if (++iterCnt % 100 == 0)
                            info("Iterations count: " + iterCnt);
                    }
                    catch (GridException e) {
                        U.error(log, "Failed to undeploy task message.", e);

                        fail("See logs for details.");
                    }
                }
            });

            GridTestUtils.runMultiThreaded(new Callable<Object>() {
                @Override public Object call() throws Exception {
                    try {
                        for (int i = 0; i < EXEC_CNT; i++) {
                            barrier.await(2000, MILLISECONDS);

                            grid.compute().localDeployTask(GridDeploymentTestTask.class,
                                GridDeploymentTestTask.class.getClassLoader());

                            assert grid.compute().localTasks().get(GridDeploymentTestTask.class.getName()) != null;
                        }
                    }
                    catch (Exception e) {
                        U.error(log, "Test failed.", e);

                        throw e;
                    }
                    finally {
                        info("Thread finished.");
                    }

                    return null;
                }
            }, THREAD_CNT, "grid-load-test-thread");
        }
        finally {
            stopAllGrids();
        }
    }

    /**
     * Test task.
     */
    private static class GridDeploymentTestTask extends GridComputeTaskAdapter<Object, Object> {
        /** {@inheritDoc} */
        @Override public Map<? extends GridComputeJob, GridNode> map(List<GridNode> subgrid, Object arg) throws GridException {
            assert false;

            return Collections.emptyMap();
        }

        /** {@inheritDoc} */
        @Override public Object reduce(List<GridComputeJobResult> results) throws GridException {
            return null;
        }
    }
}
