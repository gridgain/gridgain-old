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
import org.gridgain.grid.logger.*;
import org.gridgain.grid.resources.*;
import org.gridgain.testframework.junits.common.*;

import java.io.*;
import java.util.*;

/**
 * Test continuous mapper with siblings.
 */
@GridCommonTest(group = "Kernal Self")
public class GridContinuousJobSiblingsSelfTest extends GridCommonAbstractTest {
    /** */
    private static final int JOB_COUNT = 10;

    /**
     * @throws Exception If test failed.
     */
    public void testContinuousJobSiblings() throws Exception {
        try {
            Grid grid = startGrid(0);
            startGrid(1);

            grid.compute().execute(TestTask.class, null).get();
        }
        finally {
            stopAllGrids();
        }
    }

    /**
     * @throws Exception If test failed.
     */
    public void testContinuousJobSiblingsLocalNode() throws Exception {
        try {
            Grid grid = startGrid(0);

            grid.forLocal().compute().execute(TestTask.class, null).get();
        }
        finally {
            stopAllGrids();
        }
    }

    /** */
    private static class TestTask extends GridComputeTaskSplitAdapter<Object, Object> {
        /** */
        @GridTaskContinuousMapperResource
        private GridComputeTaskContinuousMapper mapper;

        /** */
        @GridTaskSessionResource
        private GridComputeTaskSession ses;

        /** */
        private volatile int jobCnt;

        /** {@inheritDoc} */
        @Override protected Collection<? extends GridComputeJob> split(int gridSize, Object arg) throws GridException {
            return Collections.singleton(new TestJob(++jobCnt));
        }

        /** {@inheritDoc} */
        @Override public GridComputeJobResultPolicy result(GridComputeJobResult res, List<GridComputeJobResult> received)
            throws GridException {
            if (res.getException() != null)
                throw new GridException("Job resulted in error: " + res, res.getException());

            assert ses.getJobSiblings().size() == jobCnt;

            if (jobCnt < JOB_COUNT) {
                mapper.send(new TestJob(++jobCnt));

                assert ses.getJobSiblings().size() == jobCnt;
            }

            return GridComputeJobResultPolicy.WAIT;
        }

        /** {@inheritDoc} */
        @Override public Object reduce(List<GridComputeJobResult> results) throws GridException {
            assertEquals(JOB_COUNT, results.size());

            return null;
        }
    }

    /** */
    private static class TestJob extends GridComputeJobAdapter {
        /** */
        @GridTaskSessionResource
        private GridComputeTaskSession ses;

        /** */
        @GridLoggerResource
        private GridLogger log;

        /**
         * @param sibCnt Siblings count to check.
         */
        TestJob(int sibCnt) {
            super(sibCnt);
        }

        /** {@inheritDoc} */
        @Override public Serializable execute() throws GridException {
            assert ses != null;
            assert argument(0) != null;

            Integer sibCnt = argument(0);

            log.info("Executing job.");

            assert sibCnt != null;

            Collection<GridComputeJobSibling> sibs = ses.getJobSiblings();

            assert sibs != null;
            assert sibs.size() == sibCnt : "Unexpected siblings collection [expectedSize=" + sibCnt +
                ", siblingsCnt=" + sibs.size() + ", siblings=" + sibs + ']';

            return null;
        }
    }
}
