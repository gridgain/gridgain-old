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
import org.gridgain.grid.marshaller.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.spi.checkpoint.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.testframework.junits.common.*;

import java.io.*;
import java.util.*;

/**
 * Grid session checkpoint self test.
 */
@GridCommonTest(group = "Task Session")
public abstract class GridSessionCheckpointAbstractSelfTest extends GridCommonAbstractTest {
    /** */
    protected static GridCheckpointSpi spi;

    /** */
    private static final int SPLIT_COUNT = 5;

    /** */
    protected GridSessionCheckpointAbstractSelfTest() {
        super(/*start grid*/false);
    }

     /**
     * @param sesKey Session key.
     * @param globalKey Global key.
     * @param globalState Global state.
     * @throws Exception If check failed.
     */
    private void checkFinishedState(String sesKey, String globalKey, String globalState) throws Exception {
        byte[] serState = spi.loadCheckpoint(sesKey);

        assert serState == null : "Session scope variable is not null: " + Arrays.toString(serState);

        serState = spi.loadCheckpoint(globalKey);

        GridMarshaller marshaller = getTestResources().getMarshaller();

        assert marshaller != null;

        String state = marshaller.unmarshal(serState, getClass().getClassLoader());

        assert state != null : "Global state is missing: " + globalKey;
        assert state.equals(globalState) : "Invalid state value: " + state;

        spi.removeCheckpoint(globalKey);

        Object cp = spi.loadCheckpoint(globalKey);

        assert cp == null;
    }

    /**
     * @param sesKey Session key.
     * @param sesState Session state.
     * @param globalKey Global key.
     * @param globalState Global state.
     * @param marsh Marshaller.
     * @param cl Class loader.
     * @throws Exception If check failed.
     */
    private static void checkRunningState(String sesKey, String sesState, String globalKey, String globalState,
        GridMarshaller marsh, ClassLoader cl) throws Exception {
        assert marsh != null;
        assert cl != null;

        byte[] serState = spi.loadCheckpoint(sesKey);

        String state = marsh.unmarshal(serState, cl);

        assert state != null : "Session state is missing: " + sesKey;
        assert state.equals(sesState) : "Invalid state value: " + state;

        serState = spi.loadCheckpoint(globalKey);

        state = marsh.unmarshal(serState, cl);

        assert state != null : "Global state is missing: " + globalKey;
        assert state.equals(globalState) : "Invalid state value: " + state;
    }

    /**
     * @param cfg Configuration.
     * @throws Exception If check failed.
     */
    protected void checkCheckpoints(GridConfiguration cfg) throws Exception {
        Grid grid = G.start(cfg);

        try {
            grid.compute().localDeployTask(GridCheckpointTestTask.class, GridCheckpointTestTask.class.getClassLoader());

            GridComputeTaskFuture<?> fut = grid.compute().execute("GridCheckpointTestTask", null);

            fut.getTaskSession().saveCheckpoint("future:session:key", "future:session:testval");
            fut.getTaskSession().saveCheckpoint("future:global:key", "future:global:testval",
                GridComputeTaskSessionScope.GLOBAL_SCOPE, 0);

            int res = (Integer) fut.get();

            assert res == SPLIT_COUNT : "Invalid result: " + res;

            // Check fut states.
            checkFinishedState("future:session:key", "future:global:key", "future:global:testval");

            // Check states saved by jobs.
            for (int i = 0; i < SPLIT_COUNT; i++)
                checkFinishedState("job:session:key:" + i, "job:global:key:" + i, "job:global:testval:" + i);
            // Check states saved by map(..).
            for (int i = 0; i < SPLIT_COUNT; i++)
                checkFinishedState("map:session:key:" + i, "map:global:key:" + i, "map:global:testval:" + i);
            // Check states saved by reduce(..).
            for (int i = 0; i < SPLIT_COUNT; i++)
                checkFinishedState("reduce:session:key:" + i, "reduce:global:key:" + i, "reduce:global:testval:" + i);
        }
        finally {
            G.stop(getTestGridName(), false);
        }
    }

    /** */
    @GridComputeTaskName("GridCheckpointTestTask")
    @GridComputeTaskSessionFullSupport
    private static class GridCheckpointTestTask extends GridComputeTaskSplitAdapter<Object, Object> {
        /** */
        @GridTaskSessionResource private GridComputeTaskSession ses;

        /** */
        @GridMarshallerResource private GridMarshaller marshaller;

        /** {@inheritDoc} */
        @Override protected Collection<GridComputeJobAdapter> split(int gridSize, Object arg) throws GridException {
            for (int i = 0; i < SPLIT_COUNT; i++) {
                ses.saveCheckpoint("map:session:key:" + i, "map:session:testval:" + i);
                ses.saveCheckpoint("map:global:key:" + i, "map:global:testval:" + i,
                    GridComputeTaskSessionScope.GLOBAL_SCOPE, 0);
            }

            Collection<GridComputeJobAdapter> jobs = new ArrayList<>(SPLIT_COUNT);

            for (int i = 0; i < SPLIT_COUNT; i++) {
                jobs.add(new GridComputeJobAdapter(i) {
                    /** */
                    private static final long serialVersionUID = -9118687978815477993L;

                    /** {@inheritDoc} */
                    @Override public Serializable execute() throws GridException {
                        ses.saveCheckpoint("job:session:key:" + argument(0), "job:session:testval:" + argument(0));
                        ses.saveCheckpoint("job:global:key:" + argument(0), "job:global:testval:" + argument(0),
                            GridComputeTaskSessionScope.GLOBAL_SCOPE, 0);

                        return 1;
                    }
                });
            }

            return jobs;
        }

        /** {@inheritDoc} */
        @Override public Object reduce(List<GridComputeJobResult> results) throws GridException {
            int res = 0;

            for (GridComputeJobResult result : results) {
                res += (Integer)result.getData();
            }

            for (int i = 0; i < SPLIT_COUNT; i++) {
                ses.saveCheckpoint("reduce:session:key:" + i, "reduce:session:testval:" + i);
                ses.saveCheckpoint("reduce:global:key:" + i, "reduce:global:testval:" + i,
                    GridComputeTaskSessionScope.GLOBAL_SCOPE, 0);
            }

            // Sleep to let task future store a session attribute.
            try {
                Thread.sleep(200);
            }
            catch (InterruptedException e) {
                throw new GridException("Got interrupted during reducing.", e);
            }

            try {
                // Check task and job states.
                for (int i =  0; i < SPLIT_COUNT; i++) {
                    // Check task map state.
                    checkRunningState("map:session:key:" + i, "map:session:testval:" + i,
                        "map:global:key:" + i, "map:global:testval:" + i, marshaller, getClass().getClassLoader());

                    // Check task reduce state.
                    checkRunningState("reduce:session:key:" + i, "reduce:session:testval:" + i,
                        "reduce:global:key:" + i, "reduce:global:testval:" + i, marshaller, getClass().getClassLoader());

                    // Check task map state.
                    checkRunningState("job:session:key:" + i, "job:session:testval:" + i,
                        "job:global:key:" + i, "job:global:testval:" + i, marshaller, getClass().getClassLoader());
                }
            }
            catch (Exception e) {
                throw new GridException("Running state check failure.", e);
            }

            return res;
        }
    }
}
