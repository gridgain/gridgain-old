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

package org.gridgain.grid.kernal.visor.cmd.tasks;

import org.gridgain.grid.*;
import org.gridgain.grid.compute.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * Nop task with random timeout.
 */
public class VisorNopTask implements GridComputeTask<Integer, Void> {
    /** */
    private static final long serialVersionUID = 0L;

    /** {@inheritDoc} */
    @Nullable @Override public Map<? extends GridComputeJob, GridNode> map(List<GridNode> subgrid,
        @Nullable Integer arg) throws GridException {

        Map<GridComputeJob, GridNode> map = new GridLeanMap<>(subgrid.size());

        for (GridNode node : subgrid)
            map.put(new VisorNopJob(arg), node);

        return map;
    }

    /** {@inheritDoc} */
    @Override public GridComputeJobResultPolicy result(GridComputeJobResult res,
        List<GridComputeJobResult> rcvd) throws GridException {
        return GridComputeJobResultPolicy.WAIT;
    }

    /** {@inheritDoc} */
    @Nullable @Override public Void reduce(List<GridComputeJobResult> results) throws GridException {
        return null;
    }

    /**
     * Nop job with random timeout.
     */
    private static class VisorNopJob extends GridComputeJobAdapter {
        /** */
        private static final long serialVersionUID = 0L;

        private VisorNopJob(@Nullable Object arg) {
            super(arg);
        }

        /** {@inheritDoc} */
        @SuppressWarnings("ConstantConditions")
        @Nullable @Override public Object execute() throws GridException {
            try {
                Integer maxTimeout = argument(0);

                Thread.sleep(new Random().nextInt(maxTimeout));
            }
            catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }

            return null;
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(VisorNopJob.class, this);
        }
    }
}
