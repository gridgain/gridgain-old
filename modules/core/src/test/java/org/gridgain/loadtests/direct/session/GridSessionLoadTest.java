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

package org.gridgain.loadtests.direct.session;

import org.gridgain.grid.*;
import org.gridgain.grid.compute.*;
import org.gridgain.grid.loadtest.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.testframework.*;
import org.gridgain.testframework.config.*;
import org.gridgain.testframework.junits.common.*;

import java.util.concurrent.atomic.*;

/**
 * Session load test.
 */
@GridCommonTest(group = "Load Test")
public class GridSessionLoadTest extends GridCommonAbstractTest {
    /** */
    public GridSessionLoadTest() {
        super(/*start Grid*/true);
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

    /** {@inheritDoc} */
    @Override protected long getTestTimeout() {
        return (getTestDurationInMinutes() + 1) * 60 * 1000;
    }

    /**
     * @throws Exception If failed.
     */
    @SuppressWarnings("unchecked")
    public void testSessionLoad() throws Exception {
        final Grid grid = G.grid(getTestGridName());

        assert grid != null;

        grid.compute().localDeployTask(GridSessionLoadTestTask.class, GridSessionLoadTestTask.class.getClassLoader());

        final long end = getTestDurationInMinutes() * 60 * 1000 + System.currentTimeMillis();

        info("Load test will be executed for '" + getTestDurationInMinutes() + "' mins.");
        info("Thread count: " + getThreadCount());

        final GridLoadTestStatistics stats = new GridLoadTestStatistics();

        final AtomicBoolean failed = new AtomicBoolean(false);

        GridTestUtils.runMultiThreaded(new Runnable() {
            /** {@inheritDoc} */
            @Override public void run() {
                try {
                    while (end - System.currentTimeMillis() > 0) {
                        long start = System.currentTimeMillis();

                        GridComputeTaskFuture<?> fut = grid.compute().withTimeout(10000).
                            execute(GridSessionLoadTestTask.class.getName(), grid.nodes().size());

                        Object res = fut.get();

                        assert (Boolean)res;

                        long taskCnt = stats.onTaskCompleted(fut, 1, System.currentTimeMillis() - start);

                        if (taskCnt % 500 == 0)
                            info(stats.toString());
                    }
                }
                catch (Throwable e) {
                    error("Load test failed.", e);

                    failed.set(true);
                }
            }
        }, getThreadCount(), "grid-load-test-thread");

        info("Final test statistics: " + stats);

        if (failed.get())
            fail();
    }
}
