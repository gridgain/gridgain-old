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
import org.gridgain.grid.util.typedef.*;
import org.gridgain.testframework.junits.common.*;

import java.io.*;
import java.util.*;

/**
 *
 */
@GridCommonTest(group = "Kernal Self")
public class GridTaskResultCacheSelfTest extends GridCommonAbstractTest {
    /**
     *
     */
    public GridTaskResultCacheSelfTest() {
        super(true);
    }

    /**
     * @throws Exception If failed.
     */
    public void testNoCacheResults() throws Exception {
        Grid grid = G.grid(getTestGridName());

        grid.compute().execute(GridResultNoCacheTestTask.class, "Grid Result No Cache Test Argument").get();
    }

    /**
     * @throws Exception If failed.
     */
    public void testCacheResults() throws Exception {
        Grid grid = G.grid(getTestGridName());

        grid.compute().execute(GridResultCacheTestTask.class, "Grid Result Cache Test Argument").get();
    }

    /**
     *
     */
    @GridComputeTaskNoResultCache
    private static class GridResultNoCacheTestTask extends GridAbstractCacheTestTask {
        /** {@inheritDoc} */
        @Override public GridComputeJobResultPolicy result(GridComputeJobResult res, List<GridComputeJobResult> rcvd) throws GridException {
            assert res.getData() != null;
            assert rcvd.isEmpty();

            return super.result(res, rcvd);
        }

        /** {@inheritDoc} */
        @Override public Object reduce(List<GridComputeJobResult> results) throws GridException {
            assert results.isEmpty();

            return null;
        }
    }

    /**
     *
     */
    private static class GridResultCacheTestTask extends GridAbstractCacheTestTask {
        /** {@inheritDoc} */
        @Override public GridComputeJobResultPolicy result(GridComputeJobResult res, List<GridComputeJobResult> rcvd)
            throws GridException {
            assert res.getData() != null;
            assert rcvd.contains(res);

            for (GridComputeJobResult jobRes : rcvd)
                assert jobRes.getData() != null;

            return super.result(res, rcvd);
        }

        /** {@inheritDoc} */
        @Override public Object reduce(List<GridComputeJobResult> results) throws GridException {
            for (GridComputeJobResult res : results) {
                if (res.getException() != null)
                    throw res.getException();

                assert res.getData() != null;
            }

            return null;
        }
    }

    /**
     * Test task.
     */
    private abstract static class GridAbstractCacheTestTask extends GridComputeTaskSplitAdapter<String, Object> {
        /** {@inheritDoc} */
        @Override protected Collection<? extends GridComputeJob> split(int gridSize, String arg) throws GridException {
            String[] words = arg.split(" ");

            Collection<GridComputeJobAdapter> jobs = new ArrayList<>(words.length);

            for (String word : words) {
                jobs.add(new GridComputeJobAdapter(word) {
                    @Override public Serializable execute() {
                        return argument(0);
                    }
                });
            }

            return jobs;
        }
    }
}
