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

package org.gridgain.grid.kernal.processors.hadoop.proto;

import org.gridgain.grid.*;
import org.gridgain.grid.compute.*;
import org.gridgain.grid.hadoop.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.util.typedef.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * Hadoop protocol task adapter.
 */
public abstract class GridHadoopProtocolTaskAdapter<R> implements GridComputeTask<GridHadoopProtocolTaskArguments, R> {
    /** {@inheritDoc} */
    @Nullable @Override public Map<? extends GridComputeJob, GridNode> map(List<GridNode> subgrid,
        @Nullable GridHadoopProtocolTaskArguments arg) throws GridException {
        return Collections.singletonMap(new Job(arg), subgrid.get(0));
    }

    /** {@inheritDoc} */
    @Override public GridComputeJobResultPolicy result(GridComputeJobResult res, List<GridComputeJobResult> rcvd)
        throws GridException {
        return GridComputeJobResultPolicy.REDUCE;
    }

    /** {@inheritDoc} */
    @Nullable @Override public R reduce(List<GridComputeJobResult> results) throws GridException {
        if (!F.isEmpty(results)) {
            GridComputeJobResult res = results.get(0);

            return res.getData();
        }
        else
            return null;
    }

    /**
     * Job wrapper.
     */
    private class Job implements GridComputeJob {
        /** */
        private static final long serialVersionUID = 0L;

        @GridInstanceResource
        private Grid grid;

        @SuppressWarnings("UnusedDeclaration")
        @GridJobContextResource
        private GridComputeJobContext jobCtx;

        /** Argument. */
        private final GridHadoopProtocolTaskArguments args;

        /**
         * Constructor.
         *
         * @param args Job argument.
         */
        private Job(GridHadoopProtocolTaskArguments args) {
            this.args = args;
        }

        /** {@inheritDoc} */
        @Override public void cancel() {
            // No-op.
        }

        /** {@inheritDoc} */
        @Nullable @Override public Object execute() throws GridException {
            return run(jobCtx, grid.hadoop(), args);
        }
    }

    /**
     * Run the task.
     *
     * @param jobCtx Job context.
     * @param hadoop Hadoop facade.
     * @param args Arguments.
     * @return Job result.
     * @throws GridException If failed.
     */
    public abstract R run(GridComputeJobContext jobCtx, GridHadoop hadoop, GridHadoopProtocolTaskArguments args)
        throws GridException;
}
