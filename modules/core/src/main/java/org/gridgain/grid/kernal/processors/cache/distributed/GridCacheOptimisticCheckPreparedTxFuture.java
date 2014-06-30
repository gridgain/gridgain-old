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

package org.gridgain.grid.kernal.processors.cache.distributed;

import org.gridgain.grid.*;
import org.gridgain.grid.kernal.processors.cache.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.future.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.*;

/**
 * Future verifying that all remote transactions related to some
 * optimistic transaction were prepared.
 */
public class GridCacheOptimisticCheckPreparedTxFuture<K, V> extends GridCompoundIdentityFuture<Boolean>
    implements GridCacheFuture<Boolean> {
    /** */
    private static final long serialVersionUID = 0L;

    /** Logger reference. */
    private static final AtomicReference<GridLogger> logRef = new AtomicReference<>();

    /** Trackable flag. */
    private boolean trackable = true;

    /** Context. */
    private final GridCacheContext<K, V> cctx;

    /** Future ID. */
    private final GridUuid futId = GridUuid.randomUuid();

    /** Transaction. */
    private final GridCacheTxEx<K, V> tx;

    /** All involved nodes. */
    private final Map<UUID, GridNode> nodes;

    /** ID of failed node started transaction. */
    private final UUID failedNodeId;

    /** Logger. */
    private final GridLogger log;

    /** Transaction nodes mapping. */
    private final Map<UUID, Collection<UUID>> txNodes;

    /**
     * @param cctx Context.
     * @param tx Transaction.
     * @param failedNodeId ID of failed node started transaction.
     * @param txNodes Transaction mapping.
     */
    @SuppressWarnings("ConstantConditions")
    public GridCacheOptimisticCheckPreparedTxFuture(GridCacheContext<K, V> cctx, GridCacheTxEx<K, V> tx,
        UUID failedNodeId, Map<UUID, Collection<UUID>> txNodes) {
        super(cctx.kernalContext(), CU.boolReducer());

        this.cctx = cctx;
        this.tx = tx;
        this.txNodes = txNodes;
        this.failedNodeId = failedNodeId;

        log = U.logger(ctx, logRef, GridCacheOptimisticCheckPreparedTxFuture.class);

        nodes = new GridLeanMap<>();

        UUID locNodeId = cctx.localNodeId();

        for (Map.Entry<UUID, Collection<UUID>> e : tx.transactionNodes().entrySet()) {
            if (!locNodeId.equals(e.getKey()) && !failedNodeId.equals(e.getKey()) && !nodes.containsKey(e.getKey())) {
                GridNode node = cctx.discovery().node(e.getKey());

                if (node != null)
                    nodes.put(node.id(), node);
                else if (log.isDebugEnabled())
                    log.debug("Transaction node left (will ignore) " + e.getKey());
            }

            for (UUID nodeId : e.getValue()) {
                if (!locNodeId.equals(nodeId) && !failedNodeId.equals(nodeId) && !nodes.containsKey(nodeId)) {
                    GridNode node = cctx.discovery().node(nodeId);

                    if (node != null)
                        nodes.put(node.id(), node);
                    else if (log.isDebugEnabled())
                        log.debug("Transaction node left (will ignore) " + e.getKey());
                }
            }
        }
    }

    /**
     * Initializes future.
     */
    @SuppressWarnings("ConstantConditions")
    public void prepare() {
        // First check transactions on local node.
        int locTxNum = nodeTransactions(cctx.localNodeId());

        if (locTxNum > 1 && !cctx.tm().txsPreparedOrCommitted(tx.nearXidVersion(), locTxNum)) {
            onDone(false);

            markInitialized();

            return;
        }

        for (Map.Entry<UUID, Collection<UUID>> entry : txNodes.entrySet()) {
            UUID nodeId = entry.getKey();

            // Skip left nodes and local node.
            if (!nodes.containsKey(nodeId) && nodeId.equals(cctx.localNodeId()))
                continue;

            /*
             * If primary node failed then send message to all backups, otherwise
             * send message only to primary node.
             */

            if (nodeId.equals(failedNodeId)) {
                for (UUID id : entry.getValue()) {
                    // Skip backup node if it is local node or if it is also was mapped as primary.
                    if (txNodes.containsKey(id) || id.equals(cctx.localNodeId()))
                        continue;

                    MiniFuture fut = new MiniFuture(id);

                    add(fut);

                    GridCacheOptimisticCheckPreparedTxRequest<K, V>
                        req = new GridCacheOptimisticCheckPreparedTxRequest<>(tx,
                        nodeTransactions(id), futureId(), fut.futureId());

                    try {
                        cctx.io().send(id, req);
                    }
                    catch (GridTopologyException ignored) {
                        fut.onNodeLeft();
                    }
                    catch (GridException e) {
                        fut.onError(e);

                        break;
                    }
                }
            }
            else {
                MiniFuture fut = new MiniFuture(nodeId);

                add(fut);

                GridCacheOptimisticCheckPreparedTxRequest<K, V> req = new GridCacheOptimisticCheckPreparedTxRequest<>(tx,
                        nodeTransactions(nodeId), futureId(), fut.futureId());

                try {
                    cctx.io().send(nodeId, req);
                }
                catch (GridTopologyException ignored) {
                    fut.onNodeLeft();
                }
                catch (GridException e) {
                    fut.onError(e);

                    break;
                }
            }
        }

        markInitialized();
    }

    /**
     * @param nodeId Node ID.
     * @return Number of transactions on node.
     */
    private int nodeTransactions(UUID nodeId) {
        int cnt = txNodes.containsKey(nodeId) ? 1 : 0; // +1 if node is primary.

        for (Collection<UUID> backups : txNodes.values()) {
            for (UUID backup : backups) {
                if (backup.equals(nodeId)) {
                    cnt++; // +1 if node is backup.

                    break;
                }
            }
        }

        return cnt;
    }

    /**
     * @param nodeId Node ID.
     * @param res Response.
     */
    public void onResult(UUID nodeId, GridCacheOptimisticCheckPreparedTxResponse<K, V> res) {
        if (!isDone()) {
            for (GridFuture<Boolean> fut : pending()) {
                if (isMini(fut)) {
                    MiniFuture f = (MiniFuture)fut;

                    if (f.futureId().equals(res.miniId())) {
                        assert f.nodeId().equals(nodeId);

                        f.onResult(res);

                        break;
                    }
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override public GridUuid futureId() {
        return futId;
    }

    /** {@inheritDoc} */
    @Override public GridCacheVersion version() {
        return tx.xidVersion();
    }

    /** {@inheritDoc} */
    @Override public Collection<? extends GridNode> nodes() {
        return nodes.values();
    }

    /** {@inheritDoc} */
    @Override public boolean onNodeLeft(UUID nodeId) {
        for (GridFuture<?> fut : futures())
            if (isMini(fut)) {
                MiniFuture f = (MiniFuture)fut;

                if (f.nodeId().equals(nodeId)) {
                    f.onNodeLeft();

                    return true;
                }
            }

        return false;
    }

    /** {@inheritDoc} */
    @Override public boolean trackable() {
        return trackable;
    }

    /** {@inheritDoc} */
    @Override public void markNotTrackable() {
        trackable = false;
    }

    /** {@inheritDoc} */
    @Override public boolean onDone(@Nullable Boolean res, @Nullable Throwable err) {
        if (super.onDone(res, err)) {
            cctx.mvcc().removeFuture(this);

            if (err == null) {
                assert res != null;

                cctx.tm().finishOptimisticTxOnRecovery(tx, res);
            }
            else {
                if (log.isDebugEnabled())
                    log.debug("Failed to check prepared transactions, " +
                        "invalidating transaction [err=" + err + ", tx=" + tx + ']');

                cctx.tm().salvageTx(tx);
            }
        }

        return false;
    }

    /**
     * @param f Future.
     * @return {@code True} if mini-future.
     */
    private boolean isMini(GridFuture<?> f) {
        return f.getClass().equals(MiniFuture.class);
    }


    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridCacheOptimisticCheckPreparedTxFuture.class, this, "super", super.toString());
    }

    /**
     *
     */
    private class MiniFuture extends GridFutureAdapter<Boolean> {
        /** */
        private static final long serialVersionUID = 0L;

        /** Mini future ID. */
        private final GridUuid futId = GridUuid.randomUuid();

        /** Node ID. */
        private UUID nodeId;

        /**
         * Empty constructor required by {@link Externalizable}
         */
        public MiniFuture() {
            // No-op.
        }

        /**
         * @param nodeId Node ID.
         */
        private MiniFuture(UUID nodeId) {
            super(cctx.kernalContext());

            this.nodeId = nodeId;
        }

        /**
         * @return Node ID.
         */
        private UUID nodeId() {
            return nodeId;
        }

        /**
         * @return Future ID.
         */
        private GridUuid futureId() {
            return futId;
        }

        /**
         * @param e Error.
         */
        private void onError(Throwable e) {
            if (log.isDebugEnabled())
                log.debug("Failed to get future result [fut=" + this + ", err=" + e + ']');

            onDone(e);
        }

        /**
         */
        private void onNodeLeft() {
            if (log.isDebugEnabled())
                log.debug("Transaction node left grid (will ignore) [fut=" + this + ']');

            onDone(true);
        }

        /**
         * @param res Result callback.
         */
        private void onResult(GridCacheOptimisticCheckPreparedTxResponse<K, V> res) {
            onDone(res.success());
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(MiniFuture.class, this, "done", isDone(), "err", error());
        }
    }
}
