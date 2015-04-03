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

package org.gridgain.loadtests.direct.stealing;

import org.gridgain.grid.*;
import org.gridgain.grid.compute.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.resources.*;

import java.io.*;
import java.util.*;

/**
 * Stealing load test.
 */
public class GridStealingLoadTestJob extends GridComputeJobAdapter {
    /** */
    @GridLoggerResource
    private GridLogger log;

    /** */
    @GridLocalNodeIdResource
    private UUID nodeId;

    /** */
    @GridJobContextResource
    private GridComputeJobContext ctx;

    /** {@inheritDoc} */
    @Override public Serializable execute() throws GridException {
        if (log.isDebugEnabled())
            log.debug("Executing job on node [nodeId=" + nodeId + ", jobId=" + ctx.getJobId() + ']');

        try {
            Thread.sleep(500);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Here we gonna return node id which executed this job.
        // Hopefully it would be stealing node.
        return nodeId;
    }
}
