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

package org.gridgain.grid.spi.loadbalancing.adaptive;

import org.gridgain.grid.*;

/**
 * Pluggable implementation of node load probing. Implementations
 * of this can be configured to be used with {@link GridAdaptiveLoadBalancingSpi}
 * by setting {@link GridAdaptiveLoadBalancingSpi#setLoadProbe(GridAdaptiveLoadProbe)}
 * configuration parameter.
 * <p>
 * Note that if {@link #getLoad(GridNode, int)} returns a value of {@code 0},
 * then implementation will assume that load value is simply not available and
 * will try to calculate an average of load values for other nodes. If such
 * average cannot be obtained (all node load values are {@code 0}), then a value
 * of {@code 1} will be used.
 * <p>
 * By default, {@link GridAdaptiveCpuLoadProbe} probing implementation is used.
 * <p>
 * <h1 class="header">Example</h1>
 * Here is an example of how probing can be implemented to use
 * number of active and waiting jobs as probing mechanism:
 * <pre name="code" class="java">
 * public class FooBarLoadProbe implements GridAdaptiveLoadProbe {
 *     // Flag indicating whether to use average value or current.
 *     private int useAvg = true;
 *
 *     public FooBarLoadProbe(boolean useAvg) {
 *         this.useAvg = useAvg;
 *     }
 *
 *     // Calculate load based on number of active and waiting jobs.
 *     public double getLoad(GridNode node, int jobsSentSinceLastUpdate) {
 *         GridNodeMetrics metrics = node.getMetrics();
 *
 *         if (useAvg) {
 *             double load = metrics.getAverageActiveJobs() + metrics.getAverageWaitingJobs();
 *
 *             if (load > 0) {
 *                 return load;
 *             }
 *         }
 *
 *         return metrics.getCurrentActiveJobs() + metrics.getCurrentWaitingJobs();
 *     }
 * }
 * </pre>
 * Below is an example of how a probe shown above would be configured with {@link GridAdaptiveLoadBalancingSpi}
 * SPI:
 * <pre name="code" class="xml">
 * &lt;property name="loadBalancingSpi"&gt;
 *     &lt;bean class="org.gridgain.grid.spi.loadBalancing.adaptive.GridAdaptiveLoadBalancingSpi"&gt;
 *         &lt;property name="loadProbe"&gt;
 *             &lt;bean class="foo.bar.FooBarLoadProbe"&gt;
 *                 &lt;constructor-arg value="true"/&gt;
 *             &lt;/bean&gt;
 *         &lt;/property&gt;
 *     &lt;/bean&gt;
 * &lt;/property&gt;
 * </pre>
 */
public interface GridAdaptiveLoadProbe {
    /**
     * Calculates load value for a given node. Specific implementations would
     * usually take into account some of the values provided by
     * {@link GridNode#metrics()} method. For example, load can be calculated
     * based on job execution time or number of active jobs, or CPU/Heap utilization.
     * <p>
     * Note that if this method returns a value of {@code 0},
     * then implementation will assume that load value is simply not available and
     * will try to calculate an average of load values for other nodes. If such
     * average cannot be obtained (all node load values are {@code 0}), then a value
     * of {@code 1} will be used.
     *
     * @param node Grid node to calculate load for.
     * @param jobsSentSinceLastUpdate Number of jobs sent to this node since
     *      last metrics update. This parameter may be useful when
     *      implementation takes into account the current job count on a node.
     * @return Non-negative load value for the node (zero and above).
     */
    public double getLoad(GridNode node, int jobsSentSinceLastUpdate);
}
