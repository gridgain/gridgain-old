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
import org.gridgain.grid.kernal.managers.*;
import org.gridgain.grid.spi.failover.*;
import java.util.*;

/**
 * Grid failover spi manager.
 */
public class GridFailoverManager extends GridManagerAdapter<GridFailoverSpi> {
    /**
     * @param ctx Kernal context.
     */
    public GridFailoverManager(GridKernalContext ctx) {
        super(ctx, ctx.config().getFailoverSpi());
    }

    /** {@inheritDoc} */
    @Override public void start() throws GridException {
        startSpi();

        if (log.isDebugEnabled())
            log.debug(startInfo());
    }

    /** {@inheritDoc} */
    @Override public void stop(boolean cancel) throws GridException {
        stopSpi();

        if (log.isDebugEnabled())
            log.debug(stopInfo());
    }

    /**
     * @param taskSes Task session.
     * @param jobRes Job result.
     * @param top Collection of all top nodes that does not include the failed node.
     * @return New node to route this job to.
     */
    public GridNode failover(GridTaskSessionImpl taskSes, GridComputeJobResult jobRes, List<GridNode> top) {
        return getSpi(taskSes.getFailoverSpi()).failover(new GridFailoverContextImpl(taskSes, jobRes,
            ctx.loadBalancing()), top);
    }
}
