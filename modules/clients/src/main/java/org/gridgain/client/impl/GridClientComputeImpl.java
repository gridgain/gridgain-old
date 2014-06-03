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
package org.gridgain.client.impl;

import org.gridgain.client.*;
import org.gridgain.client.balancer.*;
import org.gridgain.client.impl.connection.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.jetbrains.annotations.*;

import java.util.*;

import static org.gridgain.client.util.GridClientUtils.applyFilter;
import static org.gridgain.grid.kernal.GridNodeAttributes.ATTR_DAEMON;

/**
 * Compute projection implementation.
 */
class GridClientComputeImpl extends GridClientAbstractProjection<GridClientComputeImpl> implements GridClientCompute {
    /** */
    private static final GridClientPredicate<GridClientNode> DAEMON = new GridClientPredicate<GridClientNode>() {
        @Override public boolean apply(GridClientNode e) {
            return "true".equals(e.<String>attribute(ATTR_DAEMON));
        }
    };

    /** */
    private static final GridClientPredicate<GridClientNode> NOT_DAEMON = new GridClientPredicate<GridClientNode>() {
        @Override public boolean apply(GridClientNode e) {
            return !"true".equals(e.<String>attribute(ATTR_DAEMON));
        }
    };

    /** Projection factory. */
    @SuppressWarnings("TypeMayBeWeakened")
    private final GridClientComputeFactory prjFactory = new GridClientComputeFactory();

    /**
     * Creates a new compute projection.
     *
     * @param client Started client.
     * @param nodes Nodes to be included in this projection. If {@code null},
     *      then nodes from the current topology snapshot will be used.
     * @param nodeFilter Node filter to be used for this projection. If {@code null},
     *      then no filter would be applied to the node list.
     * @param balancer Balancer to be used in this projection. If {@code null},
     *      then no balancer will be used.
     */
    GridClientComputeImpl(GridClientImpl client, Collection<GridClientNode> nodes,
        GridClientPredicate<? super GridClientNode> nodeFilter, GridClientLoadBalancer balancer) {
        super(client, nodes, nodeFilter, balancer);
    }

    /** {@inheritDoc} */
    @Override public GridClientCompute projection(GridClientNode node) throws GridClientException {
        A.notNull(node, "node");

        return createProjection(Collections.singletonList(node), null, null, prjFactory);
    }

    /** {@inheritDoc} */
    @Override public GridClientCompute projection(GridClientPredicate<? super GridClientNode> filter)
        throws GridClientException {
        return createProjection(null, filter, null, prjFactory);
    }

    /** {@inheritDoc} */
    @Override public GridClientCompute projection(Collection<GridClientNode> nodes) throws GridClientException {
        return createProjection(nodes, null, null, prjFactory);
    }

    /** {@inheritDoc} */
    @Override public GridClientCompute projection(GridClientPredicate<? super GridClientNode> filter,
        GridClientLoadBalancer balancer) throws GridClientException {
        return createProjection(null, filter, balancer, prjFactory);
    }

    /** {@inheritDoc} */
    @Override public GridClientCompute projection(Collection<GridClientNode> nodes, GridClientLoadBalancer balancer)
        throws GridClientException {
        return createProjection(nodes, null, balancer, prjFactory);
    }

    /** {@inheritDoc} */
    @Override public GridClientLoadBalancer balancer() {
        return balancer;
    }

    /** {@inheritDoc} */
    @Override public <R> R execute(String taskName, Object taskArg) throws GridClientException {
        return this.<R>executeAsync(taskName, taskArg).get();
    }

    /** {@inheritDoc} */
    @Override public <R> GridClientFuture<R> executeAsync(final String taskName, final Object taskArg) {
        A.notNull(taskName, "taskName");

        return withReconnectHandling(new ClientProjectionClosure<R>() {
            @Override public GridClientFuture<R> apply(GridClientConnection conn, UUID destNodeId)
                throws GridClientConnectionResetException, GridClientClosedException {
                return conn.execute(taskName, taskArg, destNodeId);
            }
        });
    }

    /** {@inheritDoc} */
    @Override public <R> R affinityExecute(String taskName, String cacheName, Object affKey, Object taskArg)
        throws GridClientException {
        return this.<R>affinityExecuteAsync(taskName, cacheName, affKey, taskArg).get();
    }

    /** {@inheritDoc} */
    @Override public <R> GridClientFuture<R> affinityExecuteAsync(final String taskName, String cacheName,
        Object affKey, final Object taskArg) {
        A.notNull(taskName, "taskName");

        return withReconnectHandling(new ClientProjectionClosure<R>() {
            @Override public GridClientFuture<R> apply(GridClientConnection conn, UUID destNodeId)
                throws GridClientConnectionResetException, GridClientClosedException {
                return conn.execute(taskName, taskArg, destNodeId);
            }
        }, cacheName, affKey);
    }

    /** {@inheritDoc} */
    @Override public GridClientNode node(UUID id) throws GridClientException {
        A.notNull(id, "id");

        return client.topology().node(id);
    }

    /**
     * Gets most recently refreshed topology. If this compute instance is a projection,
     * then only nodes that satisfy projection criteria will be returned.
     *
     * @return Most recently refreshed topology.
     */
    @Override public Collection<GridClientNode> nodes() throws GridClientException {
        return applyFilter(projectionNodes(), NOT_DAEMON);
    }

    /** {@inheritDoc} */
    @Override public Collection<GridClientNode> nodes(Collection<UUID> ids) throws GridClientException  {
        A.notNull(ids, "ids");

        return client.topology().nodes(ids);
    }

    /** {@inheritDoc} */
    @Override public Collection<GridClientNode> nodes(GridClientPredicate<GridClientNode> filter)
        throws GridClientException {
        A.notNull(filter, "filter");

        return applyFilter(projectionNodes(), new GridClientAndPredicate<>(filter, NOT_DAEMON));
    }

    /** {@inheritDoc} */
    @Override public Collection<GridClientNode> daemonNodes() throws GridClientException {
        return applyFilter(projectionNodes(), DAEMON);
    }

    /** {@inheritDoc} */
    @Override public GridClientNode refreshNode(UUID id, boolean includeAttrs, boolean includeMetrics)
        throws GridClientException {
        return refreshNodeAsync(id, includeAttrs, includeMetrics).get();
    }

    /** {@inheritDoc} */
    @Override public GridClientFuture<GridClientNode> refreshNodeAsync(final UUID id, final boolean includeAttrs,
        final boolean includeMetrics) {
        A.notNull(id, "id");

        return withReconnectHandling(new ClientProjectionClosure<GridClientNode>() {
            @Override public GridClientFuture<GridClientNode> apply(GridClientConnection conn, UUID destNodeId)
                throws GridClientConnectionResetException, GridClientClosedException {
                return conn.node(id, includeAttrs, includeMetrics, destNodeId);
            }
        });
    }

    /** {@inheritDoc} */
    @Nullable @Override public GridClientNode refreshNode(String ip, boolean includeAttrs, boolean inclMetrics)
        throws GridClientException {
        return refreshNodeAsync(ip, includeAttrs, inclMetrics).get();
    }

    /** {@inheritDoc} */
    @Override public GridClientFuture<GridClientNode> refreshNodeAsync(final String ip, final boolean inclAttrs,
        final boolean includeMetrics) {
        A.notNull(ip, "ip");

        return withReconnectHandling(new ClientProjectionClosure<GridClientNode>() {
            @Override public GridClientFuture<GridClientNode> apply(GridClientConnection conn, UUID destNodeId)
                throws GridClientConnectionResetException, GridClientClosedException {
                return conn.node(ip, inclAttrs, includeMetrics, destNodeId);
            }
        });
    }

    /** {@inheritDoc} */
    @Override public List<GridClientNode> refreshTopology(boolean includeAttrs, boolean includeMetrics)
        throws GridClientException {
        return refreshTopologyAsync(includeAttrs, includeMetrics).get();
    }

    /** {@inheritDoc} */
    @Override public GridClientFuture<List<GridClientNode>> refreshTopologyAsync(final boolean inclAttrs,
        final boolean includeMetrics) {
        return withReconnectHandling(new ClientProjectionClosure<List<GridClientNode>>() {
            @Override public GridClientFuture<List<GridClientNode>> apply(GridClientConnection conn, UUID destNodeId)
                throws GridClientConnectionResetException,
                GridClientClosedException {
                return conn.topology(inclAttrs, includeMetrics, destNodeId);
            }
        });
    }

    /** {@inheritDoc} */
    @Override public List<String> log(int lineFrom, int lineTo) throws GridClientException {
        return logAsync(lineFrom, lineTo).get();
    }

    /** {@inheritDoc} */
    @Override public GridClientFuture<List<String>> logAsync(final int lineFrom, final int lineTo) {
        return withReconnectHandling(new ClientProjectionClosure<List<String>>() {
            @Override public GridClientFuture<List<String>> apply(GridClientConnection conn, UUID destNodeId)
                throws GridClientConnectionResetException, GridClientClosedException {
                return conn.log(null, lineFrom, lineTo, destNodeId);
            }
        });
    }

    /** {@inheritDoc} */
    @Override public List<String> log(String path, int lineFrom, int lineTo) throws GridClientException {
        return logAsync(path, lineFrom, lineTo).get();
    }

    /** {@inheritDoc} */
    @Override public GridClientFuture<List<String>> logAsync(final String path, final int lineFrom, final int lineTo) {
        return withReconnectHandling(new ClientProjectionClosure<List<String>>() {
            @Override public GridClientFuture<List<String>> apply(GridClientConnection conn, UUID destNodeId)
                throws GridClientConnectionResetException, GridClientClosedException {
                return conn.log(path, lineFrom, lineTo, destNodeId);
            }
        });
    }

    /** {@inheritDoc} */
    private class GridClientComputeFactory implements ProjectionFactory<GridClientComputeImpl> {
        /** {@inheritDoc} */
        @Override public GridClientComputeImpl create(Collection<GridClientNode> nodes,
            GridClientPredicate<? super GridClientNode> filter, GridClientLoadBalancer balancer) {
            return new GridClientComputeImpl(client, nodes, filter, balancer);
        }
    }
}
