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

package org.gridgain.loadtests.client;

import org.gridgain.client.*;
import org.gridgain.grid.util.typedef.internal.*;

/**
 * Makes a long run to ensure stability and absence of memory leaks.
 */
public class GridClientTcpSslLoadTest extends GridClientTcpSslMultiThreadedSelfTest {
    /** Test duration. */
    private static final long TEST_RUN_TIME = 8 * 60 * 60 * 1000;

    /** Statistics output interval. */
    private static final long STATISTICS_PRINT_INTERVAL = 5 * 60 * 1000;

    /** Time to let connections closed by idle. */
    private static final long RELAX_INTERVAL = 60 * 1000;

    /**
     * @throws Exception If failed.
     */
    public void testLongRun() throws Exception {
        long start = System.currentTimeMillis();

        long lastPrint = start;

        do {
            clearCaches();

            testMultithreadedTaskRun();

            testMultithreadedCachePut();

            long now = System.currentTimeMillis();

            if (now - lastPrint > STATISTICS_PRINT_INTERVAL) {
                info(">>>>>>> Running test for " + ((now - start) / 1000) + " seconds.");

                lastPrint = now;
            }

            // Let idle check work.
            U.sleep(RELAX_INTERVAL);
        }
        while (System.currentTimeMillis() - start < TEST_RUN_TIME);
    }

    /** {@inheritDoc} */
    @Override protected int topologyRefreshFrequency() {
        return 5000;
    }

    /** {@inheritDoc} */
    @Override protected int maxConnectionIdleTime() {
        return topologyRefreshFrequency() / 5;
    }

    /**
     * Clears caches on all nodes.
     */
    @SuppressWarnings("ConstantConditions")
    private void clearCaches() {
        for (int i = 0; i < NODES_CNT; i++)
            grid(i).cache(PARTITIONED_CACHE_NAME).clearAll();
    }
}
