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

package org.gridgain.grid;

import org.gridgain.grid.compute.*;

import java.io.*;

/**
 * Test job result.
 */
public class GridTestJobResult implements GridComputeJobResult {
    /** */
    private final Serializable data;

    /** */
    private final GridException e;

    /** */
    private final GridComputeJob job;

    /** */
    private final GridNode node;

    /** */
    private final GridComputeJobContext jobCtx;

    /** */
    public GridTestJobResult() {
        e = null;
        job = null;
        data = null;
        node = null;
        jobCtx = new GridTestJobContext();
    }

    /**
     * @param data Data.
     * @param e Exception.
     * @param job Gird job.
     * @param node Grid node.
     * @param jobCtx Job context.
     */
    public GridTestJobResult(Serializable data, GridException e, GridComputeJob job, GridNode node, GridComputeJobContext jobCtx) {
        this.data = data;
        this.e = e;
        this.job = job;
        this.node = node;
        this.jobCtx = jobCtx;
    }

    /**
     * @param node Grid node.
     */
    public GridTestJobResult(GridNode node) {
        this.node = node;

        e = null;
        job = null;
        data = null;
        jobCtx = new GridTestJobContext();
    }

    /** {@inheritDoc} */ @Override public Serializable getData() { return data; }

    /** {@inheritDoc} */ @Override public GridException getException() { return e; }

    /** {@inheritDoc} */ @Override public boolean isCancelled() { return false; }

    /** {@inheritDoc} */ @Override public GridComputeJob getJob() { return job; }

    /** {@inheritDoc} */ @Override public GridNode getNode() { return node; }

    /** {@inheritDoc} */ @Override public GridComputeJobContext getJobContext() { return jobCtx; }
}
