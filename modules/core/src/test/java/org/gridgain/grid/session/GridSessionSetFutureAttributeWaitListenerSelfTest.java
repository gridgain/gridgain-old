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

package org.gridgain.grid.session;

import org.gridgain.grid.*;
import org.gridgain.grid.compute.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.testframework.junits.common.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 *
 */
@GridCommonTest(group = "Task Session")
public class GridSessionSetFutureAttributeWaitListenerSelfTest extends GridCommonAbstractTest {
    /** */
    public static final int SPLIT_COUNT = 5;

    /** */
    private static final int WAIT_TIME = 20000;

    /** */
    private static CountDownLatch startSignal;

    /** */
    private static final Object mux = new Object();

    /** */
    private GridTaskSessionAttributeTestListener lsnr = new GridTaskSessionAttributeTestListener();

    /** */
    public GridSessionSetFutureAttributeWaitListenerSelfTest() {
        super(true);
    }

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration c = super.getConfiguration(gridName);

        GridTcpDiscoverySpi discoSpi = new GridTcpDiscoverySpi();

        discoSpi.setIpFinder(new GridTcpDiscoveryVmIpFinder(true));

        c.setDiscoverySpi(discoSpi);

        c.setExecutorService(
            new ThreadPoolExecutor(
                SPLIT_COUNT * 2,
                SPLIT_COUNT * 2,
                0, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>()));

        c.setExecutorServiceShutdown(true);

        return c;
    }

    /**
     * @throws Exception If failed.
     */
    public void testSetAttribute() throws Exception {
        Grid grid = G.grid(getTestGridName());

        grid.compute().localDeployTask(GridTaskSessionTestTask.class, GridTaskSessionTestTask.class.getClassLoader());

        for (int i = 0; i < 1; i++) {
            refreshInitialData();

            GridComputeTaskFuture<?> fut = grid.compute().execute(GridTaskSessionTestTask.class.getName(), null);

            assert fut != null;

            try {
                // Wait until jobs begin execution.
                boolean await = startSignal.await(WAIT_TIME, TimeUnit.MILLISECONDS);

                assert await == true : "Jobs did not start.";

                fut.getTaskSession().addAttributeListener(lsnr, true);

                info("Setting attribute 'testName'.");

                fut.getTaskSession().setAttribute("testName", "testVal");

                Object res = fut.get();

                assert (Integer)res == SPLIT_COUNT : "Invalid result [i=" + i + ", fut=" + fut + ']';

                assert lsnr.getAttributes().size() != 0 : "No attributes found.";
            }
            finally {
                // We must wait for the jobs to be sure that they have completed
                // their execution since they use static variable (shared for the tests).
                fut.get();
            }
        }
    }

    /** */
    private void refreshInitialData() {
        startSignal = new CountDownLatch(SPLIT_COUNT);

        lsnr.reset();
    }

    /**
     *
     */
    @GridComputeTaskSessionFullSupport
    public static class GridTaskSessionTestTask extends GridComputeTaskSplitAdapter<Serializable, Integer> {
        /** */
        @GridLoggerResource private GridLogger log;

        /** */
        @GridTaskSessionResource private GridComputeTaskSession taskSes;

        /** {@inheritDoc} */
        @Override protected Collection<? extends GridComputeJob> split(int gridSize, Serializable arg) throws GridException {
            if (log.isInfoEnabled())
                log.info("Splitting job [job=" + this + ", gridSize=" + gridSize + ", arg=" + arg + ']');

            Collection<GridComputeJob> jobs = new ArrayList<>(SPLIT_COUNT);

            for (int i = 1; i <= SPLIT_COUNT; i++) {
                jobs.add(new GridComputeJobAdapter(i) {
                    @SuppressWarnings({"UnconditionalWait"})
                    public Serializable execute() throws GridException {
                        assert taskSes != null;

                        if (log.isInfoEnabled())
                            log.info("Computing job [job=" + this + ", arg=" + argument(0) + ']');

                        startSignal.countDown();

                        try {
                            if (startSignal.await(WAIT_TIME, TimeUnit.MILLISECONDS) == false)
                                fail();

                            synchronized (mux) {
                                mux.wait(WAIT_TIME);
                            }

                            return 1;
                        }
                        catch (InterruptedException e) {
                            throw new GridException("Failed to wait for listener due to interruption.", e);
                        }
                    }
                });
            }

            return jobs;
        }

        /** {@inheritDoc} */
        @Override public GridComputeJobResultPolicy result(GridComputeJobResult result, List<GridComputeJobResult> received)
            throws GridException {
            if (result.getException() != null)
                throw result.getException();

            return received.size() == SPLIT_COUNT ? GridComputeJobResultPolicy.REDUCE : GridComputeJobResultPolicy.WAIT;
        }

        /** {@inheritDoc} */
        @Override public Integer reduce(List<GridComputeJobResult> results) throws GridException {
            if (log.isInfoEnabled())
                log.info("Reducing job [job=" + this + ", results=" + results + ']');

            if (results.size() < SPLIT_COUNT)
                fail();

            int sum = 0;

            for (GridComputeJobResult result : results) {
                if (result.getData() != null)
                    sum += (Integer)result.getData();
            }

            return sum;
        }
    }

    /**
     *
     */
    private class GridTaskSessionAttributeTestListener implements GridComputeTaskSessionAttributeListener {
        /** */
        private Map<Object, Object> attrs = new HashMap<>();

        /** {@inheritDoc} */
        @SuppressWarnings({"NakedNotify"})
        public void onAttributeSet(Object key, Object val) {
            assert key != null;

            info("Received attribute [name=" + key + ",val=" + val + ']');

            attrs.put(key, val);

            synchronized (mux) {
                mux.notifyAll();
            }
        }

        /**
         * Getter for property 'attrs'.
         *
         * @return Attributes map.
         */
        public Map<Object, Object> getAttributes() {
            return attrs;
        }

        /** */
        public void reset() {
            attrs.clear();
        }
    }
}
