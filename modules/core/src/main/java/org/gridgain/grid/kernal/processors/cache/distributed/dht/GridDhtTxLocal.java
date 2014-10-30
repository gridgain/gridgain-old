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

package org.gridgain.grid.kernal.processors.cache.distributed.dht;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.kernal.processors.cache.*;
import org.gridgain.grid.kernal.processors.cache.distributed.*;
import org.gridgain.grid.kernal.processors.cache.distributed.near.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.grid.util.tostring.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import static org.gridgain.grid.kernal.processors.cache.GridCacheUtils.*;
import static org.gridgain.grid.cache.GridCacheTxState.*;

/**
 * Replicated user transaction.
 */
public class GridDhtTxLocal<K, V> extends GridDhtTxLocalAdapter<K, V> implements GridCacheMappedVersion {
    /** */
    private static final long serialVersionUID = 0L;

    /** */
    private UUID nearNodeId;

    /** Near future ID. */
    private GridUuid nearFutId;

    /** Near future ID. */
    private GridUuid nearMiniId;

    /** Near future ID. */
    private GridUuid nearFinFutId;

    /** Near future ID. */
    private GridUuid nearFinMiniId;

    /** Near XID. */
    private GridCacheVersion nearXidVer;

    /** Transaction nodes mapping (primary node -> related backup nodes). */
    private Map<UUID, Collection<UUID>> txNodes;

    /** Future. */
    @GridToStringExclude
    private final AtomicReference<GridDhtTxPrepareFuture<K, V>> prepFut =
        new AtomicReference<>();

    /**
     * Empty constructor required for {@link Externalizable}.
     */
    public GridDhtTxLocal() {
        // No-op.
    }

    /**
     * @param nearNodeId Near node ID that initiated transaction.
     * @param nearXidVer Near transaction ID.
     * @param nearFutId Near future ID.
     * @param nearMiniId Near mini future ID.
     * @param nearThreadId Near thread ID.
     * @param implicit Implicit flag.
     * @param implicitSingle Implicit-with-single-key flag.
     * @param cctx Cache context.
     * @param concurrency Concurrency.
     * @param isolation Isolation.
     * @param timeout Timeout.
     * @param invalidate Invalidation policy.
     * @param syncCommit Synchronous commit flag.
     * @param syncRollback Synchronous rollback flag.
     * @param explicitLock Explicit lock flag.
     * @param txSize Expected transaction size.
     * @param grpLockKey Group lock key if this is a group-lock transaction.
     * @param partLock {@code True} if this is a group-lock transaction and whole partition should be locked.
     * @param txNodes Transaction nodes mapping.
     */
    GridDhtTxLocal(
        UUID nearNodeId,
        GridCacheVersion nearXidVer,
        GridUuid nearFutId,
        GridUuid nearMiniId,
        long nearThreadId,
        boolean implicit,
        boolean implicitSingle,
        GridCacheContext<K, V> cctx,
        GridCacheTxConcurrency concurrency,
        GridCacheTxIsolation isolation,
        long timeout,
        boolean invalidate,
        boolean syncCommit,
        boolean syncRollback,
        boolean explicitLock,
        int txSize,
        @Nullable Object grpLockKey,
        boolean partLock,
        Map<UUID, Collection<UUID>> txNodes,
        UUID subjId,
        int taskNameHash
    ) {
        super(cctx.versions().onReceivedAndNext(nearNodeId, nearXidVer), implicit, implicitSingle, cctx,
            concurrency, isolation, timeout, invalidate, syncCommit, syncRollback, explicitLock, txSize, grpLockKey,
            partLock, subjId, taskNameHash);

        assert cctx != null;
        assert nearNodeId != null;
        assert nearFutId != null;
        assert nearMiniId != null;
        assert nearXidVer != null;

        this.nearNodeId = nearNodeId;
        this.nearXidVer = nearXidVer;
        this.nearFutId = nearFutId;
        this.nearMiniId = nearMiniId;
        this.txNodes = txNodes;

        threadId = nearThreadId;

        assert !F.eq(xidVer, nearXidVer);
    }

    /** {@inheritDoc} */
    @Override public Map<UUID, Collection<UUID>> transactionNodes() {
        return txNodes;
    }

    /** {@inheritDoc} */
    @Override public UUID eventNodeId() {
        return nearNodeId;
    }

    /** {@inheritDoc} */
    @Override public Collection<UUID> masterNodeIds() {
        assert nearNodeId != null;

        return Collections.singleton(nearNodeId);
    }

    /** {@inheritDoc} */
    @Override public UUID otherNodeId() {
        assert nearNodeId != null;

        return nearNodeId;
    }

    /** {@inheritDoc} */
    @Override public UUID originatingNodeId() {
        return nearNodeId;
    }

    /** {@inheritDoc} */
    @Override protected UUID nearNodeId() {
        return nearNodeId;
    }

    /** {@inheritDoc} */
    @Override public GridCacheVersion nearXidVersion() {
        return nearXidVer;
    }

    /** {@inheritDoc} */
    @Override public GridCacheVersion mappedVersion() {
        return nearXidVer;
    }

    /** {@inheritDoc} */
    @Override protected GridUuid nearFutureId() {
        return nearFutId;
    }

    /** {@inheritDoc} */
    @Override protected GridUuid nearMiniId() {
        return nearMiniId;
    }

    /** {@inheritDoc} */
    @Override public boolean dht() {
        return true;
    }

    /** {@inheritDoc} */
    @Override protected boolean isBatchUpdate() {
        // Cache store updates may happen from DHT local transactions if write behind is enabled.
        return (cctx.writeToStoreFromDht() || onePhaseCommit()) && super.isBatchUpdate();
    }

    /** {@inheritDoc} */
    @Override protected boolean isSingleUpdate() {
        // Cache store updates may happen from DHT local transactions if write behind is enabled.
        return (cctx.writeToStoreFromDht() || onePhaseCommit()) && super.isSingleUpdate();
    }

    /** {@inheritDoc} */
    @Override protected boolean updateNearCache(K key, long topVer) {
        return cctx.isDht() && isNearEnabled(cctx) && !cctx.localNodeId().equals(nearNodeId());
    }

    /**
     * @return Near future ID.
     */
    public GridUuid nearFinishFutureId() {
        return nearFinFutId;
    }

    /**
     * @param nearFinFutId Near future ID.
     */
    public void nearFinishFutureId(GridUuid nearFinFutId) {
        this.nearFinFutId = nearFinFutId;
    }

    /**
     * @return Near future mini ID.
     */
    public GridUuid nearFinishMiniId() {
        return nearFinMiniId;
    }

    /** {@inheritDoc} */
    @Override public GridFuture<GridCacheTxEx<K, V>> future() {
        return prepFut.get();
    }

    /**
     * @param nearFinMiniId Near future mini ID.
     */
    public void nearFinishMiniId(GridUuid nearFinMiniId) {
        this.nearFinMiniId = nearFinMiniId;
    }

    /** {@inheritDoc} */
    @Override @Nullable protected GridFuture<Boolean> addReader(long msgId, GridDhtCacheEntry<K, V> cached,
        GridCacheTxEntry<K, V> entry, long topVer) {
        // Don't add local node as reader.
        if (!cctx.nodeId().equals(nearNodeId)) {
            while (true) {
                try {
                    return cached.addReader(nearNodeId, msgId, topVer);
                }
                catch (GridCacheEntryRemovedException ignore) {
                    if (log.isDebugEnabled())
                        log.debug("Got removed entry when adding to DHT local transaction: " + cached);

                    cached = cctx.dht().entryExx(entry.key(), topVer);
                }
            }
        }

        return null;
    }

    /** {@inheritDoc} */
    @Override protected void updateExplicitVersion(GridCacheTxEntry<K, V> txEntry, GridCacheEntryEx<K, V> entry)
        throws GridCacheEntryRemovedException {
        // DHT local transactions don't have explicit locks.
        // No-op.
    }

    /** {@inheritDoc} */
    @Override public GridFuture<GridCacheTxEx<K, V>> prepareAsync() {
        if (optimistic()) {
            assert isSystemInvalidate();

            return prepareAsync(null, null, Collections.<K, GridCacheVersion>emptyMap(), 0, nearMiniId, null, true,
                null);
        }

        // For pessimistic mode we don't distribute prepare request.
        GridDhtTxPrepareFuture<K, V> fut = prepFut.get();

        if (fut == null) {
            // Future must be created before any exception can be thrown.
            if (!prepFut.compareAndSet(null, fut = new GridDhtTxPrepareFuture<>(cctx, this, nearMiniId,
                Collections.<K, GridCacheVersion>emptyMap(), true, null)))
                return prepFut.get();
        }
        else
            // Prepare was called explicitly.
            return fut;

        if (!state(PREPARING)) {
            if (setRollbackOnly()) {
                if (timedOut())
                    fut.onError(new GridCacheTxTimeoutException("Transaction timed out and was rolled back: " + this));
                else
                    fut.onError(new GridException("Invalid transaction state for prepare [state=" + state() +
                        ", tx=" + this + ']'));
            }
            else
                fut.onError(new GridCacheTxRollbackException("Invalid transaction state for prepare [state=" + state()
                    + ", tx=" + this + ']'));

            return fut;
        }

        try {
            userPrepare();

            if (!state(PREPARED)) {
                setRollbackOnly();

                fut.onError(new GridException("Invalid transaction state for commit [state=" + state() +
                    ", tx=" + this + ']'));

                return fut;
            }

            fut.complete();

            return fut;
        }
        catch (GridException e) {
            fut.onError(e);

            return fut;
        }
    }

    /**
     * Prepares next batch of entries in dht transaction.
     *
     * @param reads Read entries.
     * @param writes Write entries.
     * @param verMap Version map.
     * @param msgId Message ID.
     * @param nearMiniId Near mini future ID.
     * @param txNodes Transaction nodes mapping.
     * @param last {@code True} if this is last prepare request.
     * @param lastBackups IDs of backup nodes receiving last prepare request.
     * @return Future that will be completed when locks are acquired.
     */
    public GridFuture<GridCacheTxEx<K, V>> prepareAsync(@Nullable Iterable<GridCacheTxEntry<K, V>> reads,
        @Nullable Iterable<GridCacheTxEntry<K, V>> writes, Map<K, GridCacheVersion> verMap, long msgId,
        GridUuid nearMiniId, Map<UUID, Collection<UUID>> txNodes, boolean last, Collection<UUID> lastBackups) {
        assert optimistic();

        // In optimistic mode prepare still can be called explicitly from salvageTx.
        GridDhtTxPrepareFuture<K, V> fut = prepFut.get();

        if (fut == null) {
            init();

            // Future must be created before any exception can be thrown.
            if (!prepFut.compareAndSet(null, fut = new GridDhtTxPrepareFuture<>(cctx, this, nearMiniId, verMap, last,
                lastBackups))) {
                GridDhtTxPrepareFuture<K, V> f = prepFut.get();

                assert f.nearMiniId().equals(nearMiniId) : "Wrong near mini id on existing future " +
                    "[futMiniId=" + f.nearMiniId() + ", miniId=" + nearMiniId + ", fut=" + f + ']';

                return f;
            }
        }
        else {
            assert fut.nearMiniId().equals(nearMiniId) : "Wrong near mini id on existing future " +
                "[futMiniId=" + fut.nearMiniId() + ", miniId=" + nearMiniId + ", fut=" + fut + ']';

            // Prepare was called explicitly.
            return fut;
        }

        if (state() != PREPARING) {
            if (!state(PREPARING)) {
                if (state() == PREPARED && isSystemInvalidate())
                    fut.complete();
                if (setRollbackOnly()) {
                    if (timedOut())
                        fut.onError(new GridCacheTxTimeoutException("Transaction timed out and was rolled back: " +
                            this));
                    else
                        fut.onError(new GridException("Invalid transaction state for prepare [state=" + state() +
                            ", tx=" + this + ']'));
                }
                else
                    fut.onError(new GridCacheTxRollbackException("Invalid transaction state for prepare [state=" +
                        state() + ", tx=" + this + ']'));

                return fut;
            }
        }

        try {
            if (reads != null)
                for (GridCacheTxEntry<K, V> e : reads)
                    addEntry(msgId, e);

            if (writes != null)
                for (GridCacheTxEntry<K, V> e : writes)
                    addEntry(msgId, e);

            userPrepare();

            // Make sure to add future before calling prepare on it.
            cctx.mvcc().addFuture(fut);

            if (isSystemInvalidate())
                fut.complete();
            else
                fut.prepare(reads, writes, txNodes);
        }
        catch (GridCacheTxTimeoutException | GridCacheTxOptimisticException e) {
            fut.onError(e);
        }
        catch (GridException e) {
            setRollbackOnly();

            fut.onError(new GridCacheTxRollbackException("Failed to prepare transaction: " + this, e));

            try {
                rollback();
            }
            catch (GridCacheTxOptimisticException e1) {
                if (log.isDebugEnabled())
                    log.debug("Failed optimistically to prepare transaction [tx=" + this + ", e=" + e1 + ']');

                fut.onError(e);
            }
            catch (GridException e1) {
                U.error(log, "Failed to rollback transaction: " + this, e1);
            }
        }

        return fut;
    }

    /** {@inheritDoc} */
    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    @Override public GridFuture<GridCacheTx> commitAsync() {
        if (log.isDebugEnabled())
            log.debug("Committing dht local tx: " + this);

        // In optimistic mode prepare was called explicitly.
        if (pessimistic())
            prepareAsync();

        final GridDhtTxFinishFuture<K, V> fut = new GridDhtTxFinishFuture<>(cctx, this, /*commit*/true);

        cctx.mvcc().addFuture(fut);

        GridDhtTxPrepareFuture<K, V> prep = prepFut.get();

        if (prep != null) {
            if (prep.isDone()) {
                try {
                    prep.get(); // Check for errors of a parent future.

                    if (finish(true))
                        fut.finish();
                    else
                        fut.onError(new GridException("Failed to commit transaction: " + CU.txString(this)));
                }
                catch (GridCacheTxOptimisticException e) {
                    if (log.isDebugEnabled())
                        log.debug("Failed to optimistically prepare transaction [tx=" + this + ", e=" + e + ']');

                    fut.onError(e);
                }
                catch (GridException e) {
                    U.error(log, "Failed to prepare transaction: " + this, e);

                    fut.onError(e);
                }
            }
            else
                prep.listenAsync(new CI1<GridFuture<GridCacheTxEx<K, V>>>() {
                    @Override public void apply(GridFuture<GridCacheTxEx<K, V>> f) {
                        try {
                            f.get(); // Check for errors of a parent future.

                            if (finish(true))
                                fut.finish();
                            else
                                fut.onError(new GridException("Failed to commit transaction: " +
                                    CU.txString(GridDhtTxLocal.this)));
                        }
                        catch (GridCacheTxOptimisticException e) {
                            if (log.isDebugEnabled())
                                log.debug("Failed optimistically to prepare transaction [tx=" + this + ", e=" + e + ']');

                            fut.onError(e);
                        }
                        catch (GridException e) {
                            U.error(log, "Failed to prepare transaction: " + this, e);

                            fut.onError(e);
                        }
                    }
                });
        }
        else {
            assert optimistic();

            try {
                if (finish(true))
                    fut.finish();
                else
                    fut.onError(new GridException("Failed to commit transaction: " + CU.txString(this)));
            }
            catch (GridCacheTxOptimisticException e) {
                if (log.isDebugEnabled())
                    log.debug("Failed optimistically to prepare transaction [tx=" + this + ", e=" + e + ']');

                fut.onError(e);
            }
            catch (GridException e) {
                U.error(log, "Failed to commit transaction: " + this, e);

                fut.onError(e);
            }
        }

        return fut;
    }

    /** {@inheritDoc} */
    @Override protected void clearPrepareFuture(GridDhtTxPrepareFuture<K, V> fut) {
        assert optimistic();

        prepFut.compareAndSet(fut, null);
    }

    /** {@inheritDoc} */
    @Override public GridFuture<GridCacheTx> rollbackAsync() {
        GridDhtTxPrepareFuture<K, V> prepFut = this.prepFut.get();

        final GridDhtTxFinishFuture<K, V> fut = new GridDhtTxFinishFuture<>(cctx, this, /*rollback*/false);

        cctx.mvcc().addFuture(fut);

        if (prepFut == null) {
            try {
                if (finish(false) || state() == UNKNOWN)
                    fut.finish();
                else
                    fut.onError(new GridException("Failed to commit transaction: " + CU.txString(this)));
            }
            catch (GridCacheTxOptimisticException e) {
                if (log.isDebugEnabled())
                    log.debug("Failed optimistically to prepare transaction [tx=" + this + ", e=" + e + ']');

                fut.onError(e);
            }
            catch (GridException e) {
                U.error(log, "Failed to rollback transaction (will make the best effort to rollback remote nodes): " +
                    this, e);

                fut.onError(e);
            }
        }
        else {
            prepFut.complete();

            prepFut.listenAsync(new CI1<GridFuture<GridCacheTxEx<K, V>>>() {
                @Override public void apply(GridFuture<GridCacheTxEx<K, V>> f) {
                    try {
                        f.get(); // Check for errors of a parent future.
                    }
                    catch (GridException e) {
                        if (log.isDebugEnabled())
                            log.debug("Failed to prepare or rollback transaction [tx=" + this + ", e=" + e + ']');
                    }

                    try {
                        if (finish(false) || state() == UNKNOWN)
                            fut.finish();
                        else
                            fut.onError(new GridException("Failed to commit transaction: " +
                                CU.txString(GridDhtTxLocal.this)));

                    }
                    catch (GridException e) {
                        U.error(log, "Failed to gracefully rollback transaction: " + CU.txString(GridDhtTxLocal.this),
                            e);

                        fut.onError(e);
                    }
                }
            });
        }

        return fut;
    }

    /** {@inheritDoc} */
    @SuppressWarnings({"CatchGenericClass", "ThrowableInstanceNeverThrown"})
    @Override public boolean finish(boolean commit) throws GridException {
        assert nearFinFutId != null || isInvalidate() || !commit || isSystemInvalidate() || !isNearEnabled(cctx)
            || onePhaseCommit() || state() == PREPARED :
            "Invalid state [nearFinFutId=" + nearFinFutId + ", isInvalidate=" + isInvalidate() + ", commit=" + commit +
            ", sysInvalidate=" + isSystemInvalidate() + ", state=" + state() + ']';

        assert nearMiniId != null;

        return super.finish(commit);
    }

    /** {@inheritDoc} */
    @Override protected void sendFinishReply(boolean commit, @Nullable Throwable err) {
        if (nearFinFutId != null) {
            if (nearNodeId.equals(cctx.localNodeId())) {
                if (log.isDebugEnabled())
                    log.debug("Skipping response sending to local node: " + this);

                return;
            }

            GridNearTxFinishResponse<K, V> res = new GridNearTxFinishResponse<>(nearXidVer, threadId, nearFinFutId,
                nearFinMiniId, err);

            try {
                cctx.io().send(nearNodeId, res);
            }
            catch (GridTopologyException ignored) {
                if (log.isDebugEnabled())
                    log.debug("Node left before sending finish response (transaction was committed) [node=" +
                        nearNodeId + ", res=" + res + ']');
            }
            catch (Throwable ex) {
                U.error(log, "Failed to send finish response to node (transaction was " +
                    (commit ? "committed" : "rolledback") + ") [node=" + nearNodeId + ", res=" + res + ']', ex);
            }
        }
        else {
            if (log.isDebugEnabled())
                log.debug("Will not send finish reply because sender node has not sent finish request yet: " + this);
        }
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return GridToStringBuilder.toString(GridDhtTxLocal.class, this, "super", super.toString());
    }
}
