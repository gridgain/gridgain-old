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

package org.gridgain.grid.spi.failover;

import org.gridgain.grid.*;
import org.gridgain.grid.compute.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.failover.always.*;
import org.gridgain.grid.spi.failover.jobstealing.*;
import org.gridgain.grid.spi.failover.never.*;

import java.util.*;

/**
 * Failover SPI provides developer with ability to supply custom logic for handling
 * failed execution of a grid job. Job execution can fail for a number of reasons:
 * <ul>
 *      <li>Job execution threw an exception (runtime, assertion or error)</li>
 *      <li>Node on which job was execution left topology (crashed or stopped)</li>
 *      <li>Collision SPI on remote node cancelled a job before it got a chance to execute (job rejection).</li>
 * </ul>
 * In all cases failover SPI takes failed job (as failover context) and list of all
 * grid nodes and provides another node on which the job execution will be retried.
 * It is up to failover SPI to make sure that job is not mapped to the node it
 * failed on. The failed node can be retrieved from
 * {@link GridComputeJobResult#getNode() GridFailoverContext.getJobResult().node()}
 * method.
 * <p>
 * GridGain comes with the following built-in failover SPI implementations:
 * <ul>
 *      <li>{@link GridNeverFailoverSpi}</li>
 *      <li>{@link GridAlwaysFailoverSpi}</li>
 *      <li>{@link GridJobStealingFailoverSpi}</li>
 * </ul>
 * <b>NOTE:</b> this SPI (i.e. methods in this interface) should never be used directly. SPIs provide
 * internal view on the subsystem and is used internally by GridGain kernal. In rare use cases when
 * access to a specific implementation of this SPI is required - an instance of this SPI can be obtained
 * via {@link Grid#configuration()} method to check its configuration properties or call other non-SPI
 * methods. Note again that calling methods from this interface on the obtained instance can lead
 * to undefined behavior and explicitly not supported.
 */
public interface GridFailoverSpi extends GridSpi {
    /**
     * This method is called when method {@link GridComputeTask#result(GridComputeJobResult, List)} returns
     * value {@link GridComputeJobResultPolicy#FAILOVER} policy indicating that the result of
     * job execution must be failed over. Implementation of this method should examine failover
     * context and choose one of the grid nodes from supplied {@code topology} to retry job execution
     * on it. For best performance it is advised that {@link GridFailoverContext#getBalancedNode(List)}
     * method is used to select node for execution of failed job.
     *
     * @param ctx Failover context.
     * @param top Collection of all grid nodes within task topology (may include failed node).
     * @return New node to route this job to or {@code null} if new node cannot be picked.
     *      If job failover fails (returns {@code null}) the whole task will be failed.
     */
    public GridNode failover(GridFailoverContext ctx, List<GridNode> top);
}
