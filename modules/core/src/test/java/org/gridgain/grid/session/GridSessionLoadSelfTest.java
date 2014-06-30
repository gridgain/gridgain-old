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
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.testframework.*;
import org.gridgain.testframework.junits.common.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Task session load self test.
 */
@GridCommonTest(group = "Task Session")
public class GridSessionLoadSelfTest extends GridCommonAbstractTest {
    /** */
    private static final int THREAD_CNT = 40;

    /** */
    private static final int EXEC_CNT = 10;

    /** */
    private boolean locMarsh;

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration c = super.getConfiguration(gridName);

        c.setMarshalLocalJobs(locMarsh);
        c.setPeerClassLoadingEnabled(false);

        return c;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        startGrids(2);
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        stopAllGrids();
    }

    /**
     * @throws Exception If failed.
     */
    public void testSessionLoad() throws Exception {
        locMarsh = true;

        checkSessionLoad();
    }

    /**
     * @throws Exception If failed.
     */
    public void testSessionLoadNoLocalMarshalling() throws Exception {
        locMarsh = false;

        checkSessionLoad();
    }

    /**
     * @throws Exception If failed.
     */
    private void checkSessionLoad() throws Exception {
        final Grid grid = grid(1);

        assert grid != null;
        assert grid.nodes().size() == 2;

        info("Thread count: " + THREAD_CNT);

        GridTestUtils.runMultiThreaded(new Callable<Object>() {
            @Override public Object call() throws Exception {
                try {
                    for (int i = 0; i < EXEC_CNT; i++)
                        assert grid.compute().withName("task-name").execute(SessionLoadTestTask.class,
                            grid.nodes().size() * 2).get(20000);
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

    /**
     *
     */
    @GridComputeTaskSessionFullSupport
    private static class SessionLoadTestTask extends GridComputeTaskAdapter<Integer, Boolean> {
        /** */
        @GridTaskSessionResource
        private GridComputeTaskSession taskSes;

        /** */
        @GridLoggerResource
        private GridLogger log;

        /** */
        private Map<String, Integer> params;

        /** {@inheritDoc} */
        @Override public Map<? extends GridComputeJob, GridNode> map(List<GridNode> subgrid, Integer arg)
            throws GridException {
            assert taskSes != null;
            assert arg != null;
            assert arg > 1;

            Map<SessionLoadTestJob, GridNode> map = new HashMap<>(subgrid.size());

            Iterator<GridNode> iter = subgrid.iterator();

            Random rnd = new Random();

            params = new HashMap<>(arg);

            for (int i = 0; i < arg; i++) {
                // Recycle iterator.
                if (!iter.hasNext())
                    iter = subgrid.iterator();

                String paramName = UUID.randomUUID().toString();

                int paramVal = rnd.nextInt();

                taskSes.setAttribute(paramName, paramVal);

                map.put(new SessionLoadTestJob(paramName), iter.next());

                params.put(paramName, paramVal);

                if (log.isDebugEnabled())
                    log.debug("Set session attribute [name=" + paramName + ", value=" + paramVal + ']');
            }

            return map;
        }

        /** {@inheritDoc} */
        @Override public Boolean reduce(List<GridComputeJobResult> results) throws GridException {
            assert taskSes != null;
            assert results != null;
            assert params != null;
            assert !params.isEmpty();
            assert results.size() == params.size();

            if (log.isDebugEnabled())
                log.debug("Reducing: " + params);

            Map<String, Integer> receivedParams = new HashMap<>();

            boolean allAttrReceived = false;

            for (int i = 0; i < 3 && !allAttrReceived; i++) {
                allAttrReceived = true;

                for (Map.Entry<String, Integer> entry : params.entrySet()) {
                    Serializable attr = taskSes.getAttribute(entry.getKey());

                    assert attr != null;

                    int newVal = (Integer)attr;

                    receivedParams.put(entry.getKey(), newVal);

                    // New value is expected to be +1 to argument value.
                    if (newVal != entry.getValue() + 1)
                        allAttrReceived = false;
                }

                if (!allAttrReceived)
                    U.sleep(1000);
            }

            if (log.isDebugEnabled()) {
                for (Map.Entry<String, Integer> entry : receivedParams.entrySet())
                    log.debug("Received session attribute value [name=" + entry.getKey() + ", val=" + entry.getValue()
                        + ", expected=" + (params.get(entry.getKey()) + 1) + ']');
            }

            return allAttrReceived;
        }
    }

    /**
     *
     */
    private static class SessionLoadTestJob extends GridComputeJobAdapter {
        /** */
        @GridTaskSessionResource
        private GridComputeTaskSession taskSes;

        /** */
        @GridLoggerResource
        private GridLogger log;

        /**
         * @param arg Argument.
         */
        private SessionLoadTestJob(String arg) {
            super(arg);
        }

        /** {@inheritDoc} */
        @Override public Serializable execute() throws GridException {
            assert taskSes != null;
            assert argument(0) != null;

            Serializable ser = taskSes.getAttribute(argument(0));

            assert ser != null;

            int val = (Integer)ser + 1;

            if (log.isDebugEnabled())
                log.debug("Executing session load job: " + val);

            // Generate garbage.
            for (int i = 0; i < 10; i++)
                taskSes.setAttribute(argument(0), i);

            // Set final value (+1 to original value).
            taskSes.setAttribute(argument(0), val);

            if (log.isDebugEnabled())
                log.debug("Set session attribute [name=" + argument(0) + ", value=" + val + ']');

            return val;
        }
    }
}
