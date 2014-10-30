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

package org.gridgain.grid.spi.loadbalancing.weightedrandom;

import org.gridgain.grid.*;
import org.gridgain.grid.compute.*;
import org.gridgain.grid.events.*;
import org.gridgain.grid.kernal.managers.eventstorage.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.loadbalancing.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.jdk8.backport.*;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.concurrent.*;

import static org.gridgain.grid.events.GridEventType.*;

/**
 * Load balancing SPI that picks a random node for job execution. Note that you can
 * optionally assign weights to nodes, so nodes with larger weights will end up getting
 * proportionally more jobs routed to them (see {@link #setNodeWeight(int)}
 * configuration property). By default all nodes get equal weight defined by
 * {@link #DFLT_NODE_WEIGHT} (value is {@code 10}).
 * <h1 class="header">Coding Example</h1>
 * If you are using {@link GridComputeTaskSplitAdapter} then load balancing logic
 * is transparent to your code and is handled automatically by the adapter.
 * Here is an example of how your task could look:
 * <pre name="code" class="java">
 * public class MyFooBarTask extends GridComputeTaskSplitAdapter&lt;Object, Object&gt; {
 *    &#64;Override
 *    protected Collection&lt;? extends GridComputeJob&gt; split(int gridSize, Object arg) throws GridException {
 *        List&lt;MyFooBarJob&gt; jobs = new ArrayList&lt;MyFooBarJob&gt;(gridSize);
 *
 *        for (int i = 0; i &lt; gridSize; i++) {
 *            jobs.add(new MyFooBarJob(arg));
 *        }
 *
 *        // Node assignment via load balancer
 *        // happens automatically.
 *        return jobs;
 *    }
 *    ...
 * }
 * </pre>
 * If you need more fine-grained control over how some jobs within task get mapped to a node
 * and use affinity load balancing for some other jobs within task, then you should use
 * {@link GridComputeTaskAdapter}. Here is an example of how your task will look. Note that in this
 * case we manually inject load balancer and use it to pick the best node. Doing it in
 * such way would allow user to map some jobs manually and for others use load balancer.
 * <pre name="code" class="java">
 * public class MyFooBarTask extends GridComputeTaskAdapter&lt;String, String&gt; {
 *    // Inject load balancer.
 *    &#64;GridLoadBalancerResource
 *    GridComputeLoadBalancer balancer;
 *
 *    // Map jobs to grid nodes.
 *    public Map&lt;? extends GridComputeJob, GridNode&gt; map(List&lt;GridNode&gt; subgrid, String arg) throws GridException {
 *        Map&lt;MyFooBarJob, GridNode&gt; jobs = new HashMap&lt;MyFooBarJob, GridNode&gt;(subgrid.size());
 *
 *        // In more complex cases, you can actually do
 *        // more complicated assignments of jobs to nodes.
 *        for (int i = 0; i &lt; subgrid.size(); i++) {
 *            // Pick the next best balanced node for the job.
 *            jobs.put(new MyFooBarJob(arg), balancer.getBalancedNode())
 *        }
 *
 *        return jobs;
 *    }
 *
 *    // Aggregate results into one compound result.
 *    public String reduce(List&lt;GridComputeJobResult&gt; results) throws GridException {
 *        // For the purpose of this example we simply
 *        // concatenate string representation of every
 *        // job result
 *        StringBuilder buf = new StringBuilder();
 *
 *        for (GridComputeJobResult res : results) {
 *            // Append string representation of result
 *            // returned by every job.
 *            buf.append(res.getData().string());
 *        }
 *
 *        return buf.string();
 *    }
 * }
 * </pre>
 * <p>
 * <h1 class="header">Configuration</h1>
 * In order to use this load balancer, you should configure your grid instance
 * to use {@code GridRandomLoadBalancingSpi} either from Spring XML file or
 * directly. The following configuration parameters are supported:
 * <h2 class="header">Mandatory</h2>
 * This SPI has no mandatory configuration parameters.
 * <h2 class="header">Optional</h2>
 * The following configuration parameters are optional:
 * <ul>
 * <li>
 *      Flag that indicates whether to use weight policy or simple random policy
 *      (see {@link #setUseWeights(boolean)})
 * </li>
 * <li>
 *      Weight of this node (see {@link #setNodeWeight(int)}). This parameter is ignored
 *      if {@link #setUseWeights(boolean)} is set to {@code false}.
 * </li>
 * </ul>
 * Below is Java configuration example:
 * <pre name="code" class="java">
 * GridWeightedRandomLoadBalancingSpi = new GridWeightedLoadBalancingSpi();
 *
 * // Configure SPI to used weighted
 * // random load balancing.
 * spi.setUseWeights(true);
 *
 * // Set weight for the local node.
 * spi.setWeight( *);
 *
 * GridConfiguration cfg = new GridConfiguration();
 *
 * // Override default load balancing SPI.
 * cfg.setLoadBalancingSpi(spi);
 *
 * // Starts grid.
 * G.start(cfg);
 * </pre>
 * Here is how you can configure {@code GridRandomLoadBalancingSpi} using Spring XML configuration:
 * <pre name="code" class="xml">
 * &lt;property name="loadBalancingSpi"&gt;
 *     &lt;bean class="org.gridgain.grid.spi.loadBalancing.weightedrandom.GridWeightedRandomLoadBalancingSpi"&gt;
 *         &lt;property name="useWeights" value="true"/&gt;
 *         &lt;property name="nodeWeight" value="10"/&gt;
 *     &lt;/bean&gt;
 * &lt;/property&gt;
 * </pre>
 * <p>
 * <img src="http://www.gridgain.com/images/spring-small.png">
 * <br>
 * For information about Spring framework visit <a href="http://www.springframework.org/">www.springframework.org</a>
 */
@GridSpiMultipleInstancesSupport(true)
@GridSpiConsistencyChecked(optional = true)
public class GridWeightedRandomLoadBalancingSpi extends GridSpiAdapter implements GridLoadBalancingSpi,
    GridWeightedRandomLoadBalancingSpiMBean {
    /** Random number generator. */
    private static final Random RAND = new Random();

    /**
     * Name of node attribute used to indicate load weight of a node
     * (value is {@code "gridgain.node.weight.attr.name"}).
     *
     * @see GridNode#attributes()
     */
    public static final String NODE_WEIGHT_ATTR_NAME = "gridgain.node.weight.attr.name";

    /** Default weight assigned to every node if explicit one is not provided (value is {@code 10}). */
    public static final int DFLT_NODE_WEIGHT = 10;

    /** Grid logger. */
    @GridLoggerResource private GridLogger log;

    /** */
    private boolean isUseWeights;

    /** Local event listener to listen to task completion events. */
    private GridLocalEventListener evtLsnr;

    /** Weight of this node. */
    private int nodeWeight = DFLT_NODE_WEIGHT;

    /** Task topologies. First pair value indicates whether or not jobs have been mapped. */
    private ConcurrentMap<GridUuid, GridBiTuple<Boolean, WeightedTopology>> taskTops =
        new ConcurrentHashMap8<>();

    /**
     * Sets a flag to indicate whether node weights should be checked when
     * doing random load balancing. Default value is {@code false} which
     * means that node weights are disregarded for load balancing logic.
     *
     * @param isUseWeights If {@code true} then random load is distributed according
     *      to node weights.
     */
    @GridSpiConfiguration(optional = true)
    public void setUseWeights(boolean isUseWeights) {
        this.isUseWeights = isUseWeights;
    }

    /** {@inheritDoc} */
    @Override public boolean isUseWeights() {
        return isUseWeights;
    }

    /**
     * Sets weight of this node. Nodes with more processing capacity
     * should be assigned proportionally larger weight. Default value
     * is {@link #DFLT_NODE_WEIGHT} and is equal for all nodes.
     *
     * @param nodeWeight Weight of this node.
     */
    @GridSpiConfiguration(optional = true)
    public void setNodeWeight(int nodeWeight) {
        this.nodeWeight = nodeWeight;
    }

    /** {@inheritDoc} */
    @Override public int getNodeWeight() {
        return nodeWeight;
    }

    /** {@inheritDoc} */
    @Override public Map<String, Object> getNodeAttributes() throws GridSpiException {
        return F.<String, Object>asMap(createSpiAttributeName(NODE_WEIGHT_ATTR_NAME), nodeWeight);
    }

    /** {@inheritDoc} */
    @Override public void spiStart(@Nullable String gridName) throws GridSpiException {
        startStopwatch();

        assertParameter(nodeWeight > 0, "nodeWeight > 0");

        if (log.isDebugEnabled()) {
            log.debug(configInfo("isUseWeights", isUseWeights));
            log.debug(configInfo("nodeWeight", nodeWeight));
        }

        registerMBean(gridName, this, GridWeightedRandomLoadBalancingSpiMBean.class);

        // Ack ok start.
        if (log.isDebugEnabled())
            log.debug(startInfo());
    }

    /** {@inheritDoc} */
    @Override public void spiStop() throws GridSpiException {
        unregisterMBean();

        // Ack ok stop.
        if (log.isDebugEnabled())
            log.debug(stopInfo());
    }

    /** {@inheritDoc} */
    @Override protected void onContextInitialized0(GridSpiContext spiCtx) throws GridSpiException {
        getSpiContext().addLocalEventListener(evtLsnr = new GridLocalEventListener() {
            @Override public void onEvent(GridEvent evt) {
                assert evt instanceof GridTaskEvent || evt instanceof GridJobEvent;

                if (evt.type() == EVT_TASK_FINISHED ||
                    evt.type() == EVT_TASK_FAILED) {
                    GridUuid sesId = ((GridTaskEvent)evt).taskSessionId();

                    taskTops.remove(sesId);

                    if (log.isDebugEnabled())
                        log.debug("Removed task topology from topology cache for session: " + sesId);
                }
                // We should keep topology and use cache in GridComputeTask#map() method to
                // avoid O(n*n/2) complexity, after that we can drop caches.
                // Here we set mapped property and later cache will be ignored
                else if (evt.type() == EVT_JOB_MAPPED) {
                    GridUuid sesId = ((GridJobEvent)evt).taskSessionId();

                    GridBiTuple<Boolean, WeightedTopology> weightedTop = taskTops.get(sesId);

                    if (weightedTop != null)
                        weightedTop.set1(true);

                    if (log.isDebugEnabled())
                        log.debug("Job has been mapped. Ignore cache for session: " + sesId);
                }
            }
        },
            EVT_TASK_FAILED,
            EVT_TASK_FINISHED,
            EVT_JOB_MAPPED
        );
    }

    /** {@inheritDoc} */
    @Override protected void onContextDestroyed0() {
        if (evtLsnr != null) {
            GridSpiContext ctx = getSpiContext();

            if (ctx != null)
                ctx.removeLocalEventListener(evtLsnr);
        }
    }

    /** {@inheritDoc} */
    @Override public GridNode getBalancedNode(GridComputeTaskSession ses, List<GridNode> top, GridComputeJob job) {
        A.notNull(ses, "ses");
        A.notNull(top, "top");
        A.notNull(job, "job");

        // Optimization for non-weighted randomization.
        if (!isUseWeights)
            return top.get(RAND.nextInt(top.size()));

        GridBiTuple<Boolean, WeightedTopology> weightedTop = taskTops.get(ses.getId());

        // Create new cached topology if there is no one. Do not
        // use cached topology after task has been mapped.
        if (weightedTop == null) {
            // Called from GridComputeTask#map(). Put new topology and false as not mapped yet.
            taskTops.put(ses.getId(), weightedTop = F.t(false, new WeightedTopology(top)));
        }
        // We have topology - check if task has been mapped.
        else if (weightedTop.get1()) {
            // Do not use cache after GridComputeTask#map().
            return new WeightedTopology(top).pickWeightedNode();
        }

        return weightedTop.get2().pickWeightedNode();
    }

    /**
     * @param node Node to get weight for.
     * @return Node weight
     */
    private int getWeight(GridNode node) {
        Integer weight = (Integer)node.attribute(createSpiAttributeName(NODE_WEIGHT_ATTR_NAME));

        if (weight != null && weight == 0)
            throw new IllegalStateException("Node weight cannot be zero: " + node);

        return weight == null ? DFLT_NODE_WEIGHT : weight;
    }

    /**
     * Holder for weighted topology.
     */
    private class WeightedTopology {
        /** Total topology weight. */
        private final int totalWeight;

        /** Topology sorted by weight. */
        private final SortedMap<Integer, GridNode> circle = new TreeMap<>();

        /**
         * @param top Topology.
         */
        WeightedTopology(Collection<GridNode> top) {
            assert !F.isEmpty(top);

            int totalWeight = 0;

            for (GridNode node : top) {
                totalWeight += getWeight(node);

                // Complexity of this put is O(logN).
                circle.put(totalWeight, node);
            }

            this.totalWeight = totalWeight;
        }

        /**
         * Gets weighted node in random fashion.
         *
         * @return Weighted node.
         */
        GridNode pickWeightedNode() {
            int weight = RAND.nextInt(totalWeight) + 1;

            SortedMap<Integer, GridNode> pick = circle.tailMap(weight);

            assert !pick.isEmpty();

            return pick.get(pick.firstKey());
        }
    }

    /** {@inheritDoc} */
    @Override protected List<String> getConsistentAttributeNames() {
        return Collections.singletonList(createSpiAttributeName(NODE_WEIGHT_ATTR_NAME));
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridWeightedRandomLoadBalancingSpi.class, this);
    }
}
