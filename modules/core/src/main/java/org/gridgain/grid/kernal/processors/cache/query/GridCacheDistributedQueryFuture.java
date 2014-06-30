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

package org.gridgain.grid.kernal.processors.cache.query;

import org.gridgain.grid.*;
import org.gridgain.grid.kernal.processors.cache.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Distributed query future.
 */
public class GridCacheDistributedQueryFuture<K, V, R> extends GridCacheQueryFutureAdapter<K, V, R> {
    /** */
    private static final long serialVersionUID = 0L;

    /** */
    private long reqId;

    /** */
    private final Collection<UUID> subgrid = new HashSet<>();

    /** */
    private final Collection<UUID> rcvd = new HashSet<>();

    /** */
    private CountDownLatch firstPageLatch = new CountDownLatch(1);

    /**
     * Required by {@link Externalizable}.
     */
    public GridCacheDistributedQueryFuture() {
        // No-op.
    }

    /**
     * @param ctx Cache context.
     * @param reqId Request ID.
     * @param qry Query.
     * @param nodes Nodes.
     */
    @SuppressWarnings("unchecked")
    protected GridCacheDistributedQueryFuture(GridCacheContext<K, V> ctx, long reqId, GridCacheQueryBean qry,
        Iterable<GridNode> nodes) {
        super(ctx, qry, false);

        assert reqId > 0;

        this.reqId = reqId;

        GridCacheQueryManager<K, V> mgr = ctx.queries();

        assert mgr != null;

        synchronized (mux) {
            for (GridNode node : nodes)
                subgrid.add(node.id());
        }
    }

    /** {@inheritDoc} */
    @Override protected void cancelQuery() throws GridException {
        final GridCacheQueryManager<K, V> qryMgr = cctx.queries();

        assert qryMgr != null;

        try {
            Collection<GridNode> allNodes = cctx.discovery().allNodes();
            Collection<GridNode> nodes;

            synchronized (mux) {
                nodes = F.retain(allNodes, true,
                    new P1<GridNode>() {
                        @Override public boolean apply(GridNode node) {
                            return !cctx.localNodeId().equals(node.id()) && subgrid.contains(node.id());
                        }
                    }
                );

                subgrid.clear();
            }

            final GridCacheQueryRequest<K, V> req = new GridCacheQueryRequest<>(reqId, fields());

            // Process cancel query directly (without sending) for local node,
            cctx.closures().callLocalSafe(new Callable<Object>() {
                @Override public Object call() throws Exception {
                    qryMgr.processQueryRequest(cctx.localNodeId(), req);

                    return null;
                }
            });

            if (!nodes.isEmpty()) {
                cctx.io().safeSend(nodes, req,
                    new P1<GridNode>() {
                        @Override public boolean apply(GridNode node) {
                            onNodeLeft(node.id());

                            return !isDone();
                        }
                    });
            }
        }
        catch (GridException e) {
            U.error(log, "Failed to send cancel request (will cancel query in any case).", e);
        }

        qryMgr.onQueryFutureCanceled(reqId);

        clear();
    }

    /** {@inheritDoc} */
    @SuppressWarnings("NonPrivateFieldAccessedInSynchronizedContext")
    @Override protected void onNodeLeft(UUID nodeId) {
        boolean callOnPage;

        synchronized (mux) {
            callOnPage = !loc && subgrid.contains(nodeId);
        }

        if (callOnPage)
            // We consider node departure as a reception of last empty
            // page from this node.
            onPage(nodeId, Collections.emptyList(), null, true);
    }

    /** {@inheritDoc} */
    @Override protected boolean onPage(UUID nodeId, boolean last) {
        assert Thread.holdsLock(mux);

        if (!loc) {
            rcvd.add(nodeId);

            if (rcvd.containsAll(subgrid))
                firstPageLatch.countDown();
        }

        boolean futFinish;

        if (last) {
            futFinish = loc || (subgrid.remove(nodeId) && subgrid.isEmpty());

            if (futFinish)
                firstPageLatch.countDown();
        }
        else
            futFinish = false;

        return futFinish;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("NonPrivateFieldAccessedInSynchronizedContext")
    @Override protected void loadPage() {
        assert !Thread.holdsLock(mux);

        Collection<GridNode> nodes = null;

        synchronized (mux) {
            if (!isDone() && rcvd.containsAll(subgrid)) {
                rcvd.clear();

                nodes = nodes();
            }
        }

        if (nodes != null)
            cctx.queries().loadPage(reqId, qry.query(), nodes, false);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("NonPrivateFieldAccessedInSynchronizedContext")
    @Override protected void loadAllPages() throws GridInterruptedException {
        assert !Thread.holdsLock(mux);

        U.await(firstPageLatch);

        Collection<GridNode> nodes = null;

        synchronized (mux) {
            if (!isDone() && !subgrid.isEmpty())
                nodes = nodes();
        }

        if (nodes != null)
            cctx.queries().loadPage(reqId, qry.query(), nodes, true);
    }

    /**
     * @return Nodes to send requests to.
     */
    private Collection<GridNode> nodes() {
        assert Thread.holdsLock(mux);

        Collection<GridNode> nodes = new ArrayList<>(subgrid.size());

        for (UUID nodeId : subgrid) {
            GridNode node = cctx.discovery().node(nodeId);

            if (node != null)
                nodes.add(node);
        }

        return nodes;
    }

    /** {@inheritDoc} */
    @Override public boolean onDone(Collection<R> res, Throwable err) {
        firstPageLatch.countDown();

        return super.onDone(res, err);
    }

    /** {@inheritDoc} */
    @Override public boolean onCancelled() {
        firstPageLatch.countDown();

        return super.onCancelled();
    }

    /** {@inheritDoc} */
    @Override public void onTimeout() {
        firstPageLatch.countDown();

        super.onTimeout();
    }

    /** {@inheritDoc} */
    @Override void clear() {
        GridCacheDistributedQueryManager<K, V> qryMgr = (GridCacheDistributedQueryManager<K, V>)cctx.queries();

        assert qryMgr != null;

        qryMgr.removeQueryFuture(reqId);
    }
}
