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
import org.gridgain.grid.spi.failover.*;
import org.gridgain.grid.spi.failover.always.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.testframework.junits.common.*;

import java.io.*;
import java.util.*;

/**
 * Always failover SPI test.
 */
@GridCommonTest(group = "Kernal Self")
public class GridAlwaysFailoverSpiFailSelfTest extends GridCommonAbstractTest {
    /** */
    private boolean isFailoverCalled;

    /** */
    public GridAlwaysFailoverSpiFailSelfTest() {
        super(true);
    }

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration() throws Exception {
        GridConfiguration cfg = super.getConfiguration();

        cfg.setFailoverSpi(new GridTestFailoverSpi());

        return cfg;
    }

    /**
     * @throws Exception If failed.
     */
    @SuppressWarnings({"UnusedCatchParameter", "ThrowableInstanceNeverThrown"})
    public void testFailoverTask() throws Exception {
        isFailoverCalled = false;

        Grid grid = G.grid(getTestGridName());

        grid.compute().localDeployTask(GridTestFailoverTask.class, GridTestFailoverTask.class.getClassLoader());

        try {
            grid.compute().execute(GridTestFailoverTask.class.getName(),
                new GridComputeExecutionRejectedException("Task should be failed over")).get();

            assert false;
        }
        catch (GridException e) {
            //No-op
        }

        assert isFailoverCalled;
    }

    /**
     * @throws Exception If failed.
     */
    @SuppressWarnings({"UnusedCatchParameter", "ThrowableInstanceNeverThrown"})
    public void testNoneFailoverTask() throws Exception {
        isFailoverCalled = false;

        Grid grid = G.grid(getTestGridName());

        grid.compute().localDeployTask(GridTestFailoverTask.class, GridTestFailoverTask.class.getClassLoader());

        try {
            grid.compute().execute(GridTestFailoverTask.class.getName(),
                new GridException("Task should NOT be failed over")).get();

            assert false;
        }
        catch (GridException e) {
            //No-op
        }

        assert !isFailoverCalled;
    }

    /** */
    private class GridTestFailoverSpi extends GridAlwaysFailoverSpi {
        /** {@inheritDoc} */
        @Override public GridNode failover(GridFailoverContext ctx, List<GridNode> grid) {
            isFailoverCalled = true;

            return super.failover(ctx, grid);
        }
    }

    /**
     * Task which splits to the jobs that always fail.
     */
    @SuppressWarnings({"PublicInnerClass"})
    public static final class GridTestFailoverTask extends GridComputeTaskSplitAdapter<Object, Object> {
        /** {@inheritDoc} */
        @Override public Collection<? extends GridComputeJob> split(int gridSize, Object arg) {
            assert gridSize == 1;
            assert arg instanceof GridException;

            Collection<GridComputeJob> res = new ArrayList<>(gridSize);

            for (int i = 0; i < gridSize; i++)
                res.add(new GridTestFailoverJob((GridException)arg));

            return res;
        }

        /** {@inheritDoc} */
        @Override public GridComputeJobResultPolicy result(GridComputeJobResult res,
            List<GridComputeJobResult> received) throws GridException {
            if (res.getException() != null)
                return GridComputeJobResultPolicy.FAILOVER;

            return super.result(res, received);
        }

        /** {@inheritDoc} */
        @Override public Serializable reduce(List<GridComputeJobResult> results) {
            return null;
        }
    }

    /**
     * Job that always throws exception.
     */
    private static class GridTestFailoverJob extends GridComputeJobAdapter {
        /**
         * @param ex Exception to be thrown in {@link #execute}.
         */
        GridTestFailoverJob(GridException ex) { super(ex); }

        /** {@inheritDoc} */
        @Override public GridException execute() throws GridException {
            throw this.<GridException>argument(0);
        }
    }
}
