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
import org.gridgain.grid.cache.*;
import org.gridgain.grid.cache.affinity.*;
import org.gridgain.grid.cache.affinity.consistenthash.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.testframework.*;
import org.gridgain.testframework.junits.common.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static org.gridgain.grid.cache.GridCacheMode.*;

/**
 * Tests multiple parallel jobs execution.
 */
@GridCommonTest(group = "Kernal Self")
public class GridMultipleJobsSelfTest extends GridCommonAbstractTest {
    /** */
    private static final int LOG_MOD = 100;

    /** */
    private static final int TEST_TIMEOUT = 60 * 1000;

    /** IP finder. */
    private static final GridTcpDiscoveryIpFinder ipFinder = new GridTcpDiscoveryVmIpFinder(true);

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        startGrid(1);
        startGrid(2);

        assertEquals(2, grid(1).nodes().size());
    }

    /** {@inheritDoc} */
    @Override protected void afterTestsStopped() throws Exception {
        stopGrid(1);
        stopGrid(2);

        assertEquals(0, G.allGrids().size());
    }

    /** {@inheritDoc} */
    @Override protected long getTestTimeout() {
        return TEST_TIMEOUT;
    }

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration c = super.getConfiguration(gridName);

        GridTcpDiscoverySpi disco = new GridTcpDiscoverySpi();

        disco.setIpFinder(ipFinder);

        c.setDiscoverySpi(disco);

        if (getTestGridName(1).equals(gridName))
            c.setCacheConfiguration(/* no configured caches */);
        else {
            GridCacheConfiguration cc = defaultCacheConfiguration();

            cc.setCacheMode(PARTITIONED);
            cc.setBackups(1);

            c.setCacheConfiguration(cc);
        }

        return c;
    }

    /**
     * @throws Exception If test failed.
     */
    public void testNotAffinityJobs() throws Exception {
        /* =========== Test properties =========== */
        int jobsNum = 5000;
        int threadNum = 10;

        runTest(jobsNum, threadNum, NotAffinityJob.class);
    }

    /**
     * @throws Exception If test failed.
     */
    public void testAffinityJobs() throws Exception {
        /* =========== Test properties =========== */
        int jobsNum = 5000;
        int threadNum = 10;

        runTest(jobsNum, threadNum, AffinityJob.class);
    }

    /**
     * @param jobsNum Number of jobs.
     * @param threadNum Number of threads.
     * @param jobCls Job class.
     * @throws Exception If failed.
     */
    private void runTest(final int jobsNum, int threadNum, final Class<? extends GridCallable<Boolean>> jobCls)
        throws Exception {
        final Grid grid1 = grid(1);

        final CountDownLatch latch = new CountDownLatch(jobsNum);

        final AtomicInteger jobsCnt = new AtomicInteger();

        final AtomicInteger resCnt = new AtomicInteger();

        GridTestUtils.runMultiThreaded(new CAX() {
            @Override public void applyx() throws GridException {
                while (true) {
                    int cnt = jobsCnt.incrementAndGet();

                    if (cnt > jobsNum)
                        break;

                    GridCallable<Boolean> job;

                    try {
                        job = jobCls.newInstance();
                    }
                    catch (Exception e) {
                        throw new GridException("Could not instantiate a job.", e);
                    }

                    GridFuture<Boolean> fut = grid1.compute().call(job);

                    if (cnt % LOG_MOD == 0)
                        X.println("Submitted jobs: " + cnt);

                    fut.listenAsync(new CIX1<GridFuture<Boolean>>() {
                        @Override public void applyx(GridFuture<Boolean> f) throws GridException {
                            try {
                                assert f.get();
                            }
                            finally {
                                latch.countDown();

                                long cnt = resCnt.incrementAndGet();

                                if (cnt % LOG_MOD == 0)
                                    X.println("Results count: " + cnt);
                            }
                        }
                    });
                }
            }
        }, threadNum, "TEST-THREAD");

        latch.await();
    }

    /**
     * Test not affinity job.
     */
    @SuppressWarnings({"PublicInnerClass"})
    public static class NotAffinityJob implements GridCallable<Boolean> {
        /** */
        private static AtomicInteger cnt = new AtomicInteger();

        /** {@inheritDoc} */
        @Override public Boolean call() throws Exception {
            int c = cnt.incrementAndGet();

            if (c % LOG_MOD == 0)
                X.println("Executed jobs: " + c);

            Thread.sleep(10);

            return true;
        }
    }

    /**
     * Test affinity routed job.
     */
    @SuppressWarnings({"PublicInnerClass"})
    public static class AffinityJob implements GridCallable<Boolean> {
        /** */
        private static AtomicInteger cnt = new AtomicInteger();

        /** {@inheritDoc} */
        @Override public Boolean call() throws Exception {
            int c = cnt.incrementAndGet();

            if (c % LOG_MOD == 0)
                X.println("Executed affinity jobs: " + c);

            Thread.sleep(10);

            return true;
        }

        /**
         * @return Affinity key.
         */
        @GridCacheAffinityKeyMapped
        public String affinityKey() {
            return "key";
        }
    }
}
