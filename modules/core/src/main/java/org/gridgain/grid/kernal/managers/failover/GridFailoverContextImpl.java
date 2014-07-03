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

package org.gridgain.grid.kernal.managers.failover;

import org.gridgain.grid.compute.*;
import org.gridgain.grid.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.kernal.managers.loadbalancer.*;
import org.gridgain.grid.spi.failover.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.grid.util.tostring.*;
import java.util.*;

/**
 * GridFailoverContext implementation.
 */
public class GridFailoverContextImpl implements GridFailoverContext {
    /** Grid task session. */
    private final GridTaskSessionImpl taskSes;

    /** Failed job result. */
    private final GridComputeJobResult jobRes;

    /** Load balancing manager. */
    @GridToStringExclude
    private final GridLoadBalancerManager loadMgr;

    /**
     * Initializes failover context.
     *
     * @param taskSes Grid task session.
     * @param jobRes Failed job result.
     * @param loadMgr Load manager.
     */
    public GridFailoverContextImpl(GridTaskSessionImpl taskSes, GridComputeJobResult jobRes,
        GridLoadBalancerManager loadMgr) {
        assert taskSes != null;
        assert jobRes != null;
        assert loadMgr != null;

        this.taskSes = taskSes;
        this.jobRes = jobRes;
        this.loadMgr = loadMgr;
    }

    /** {@inheritDoc} */
    @Override public GridComputeTaskSession getTaskSession() {
        return taskSes;
    }

    /** {@inheritDoc} */
    @Override public GridComputeJobResult getJobResult() {
        return jobRes;
    }

    /** {@inheritDoc} */
    @Override public GridNode getBalancedNode(List<GridNode> top) throws GridException {
        return loadMgr.getBalancedNode(taskSes, top, jobRes.getJob());
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridFailoverContextImpl.class, this);
    }
}
