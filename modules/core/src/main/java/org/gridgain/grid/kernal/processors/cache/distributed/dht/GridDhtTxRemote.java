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
import org.gridgain.grid.lang.*;
import org.gridgain.grid.util.tostring.*;
import org.jdk8.backport.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.util.*;

import static org.gridgain.grid.kernal.processors.cache.GridCacheUtils.*;

/**
 * Transaction created by system implicitly on remote nodes.
 */
public class GridDhtTxRemote<K, V> extends GridDistributedTxRemoteAdapter<K, V> {
    /** */
    private static final long serialVersionUID = 0L;

    /** Near node ID. */
    private UUID nearNodeId;

    /** Remote future ID. */
    private GridUuid rmtFutId;

    /** Near transaction ID. */
    private GridCacheVersion nearXidVer;

    /** Transaction nodes mapping (primary node -> related backup nodes). */
    private Map<UUID, Collection<UUID>> txNodes;

    /**
     * Empty constructor required for {@link Externalizable}.
     */
    public GridDhtTxRemote() {
        // No-op.
    }

    /**
     * This constructor is meant for optimistic transactions.
     *
     * @param nearNodeId Near node ID.
     * @param rmtFutId Remote future ID.
     * @param nodeId Node ID.
     * @param rmtThreadId Remote thread ID.
     * @param topVer Topology version.
     * @param xidVer XID version.
     * @param commitVer Commit version.
     * @param concurrency Concurrency level (should be pessimistic).
     * @param isolation Transaction isolation.
     * @param invalidate Invalidate flag.
     * @param timeout Timeout.
     * @param ctx Cache context.
     * @param txSize Expected transaction size.
     * @param grpLockKey Group lock key if this is a group-lock transaction.
     * @param nearXidVer Near transaction ID.
     * @param txNodes Transaction nodes mapping.
     */
    public GridDhtTxRemote(
        UUID nearNodeId,
        GridUuid rmtFutId,
        UUID nodeId,
        long rmtThreadId,
        long topVer,
        GridCacheVersion xidVer,
        GridCacheVersion commitVer,
        GridCacheTxConcurrency concurrency,
        GridCacheTxIsolation isolation,
        boolean invalidate,
        long timeout,
        GridCacheContext<K, V> ctx,
        int txSize,
        @Nullable Object grpLockKey,
        GridCacheVersion nearXidVer,
        Map<UUID, Collection<UUID>> txNodes,
        @Nullable UUID subjId,
        int taskNameHash
    ) {
        super(ctx, nodeId, rmtThreadId, xidVer, commitVer, concurrency, isolation, invalidate, timeout, txSize,
            grpLockKey, subjId, taskNameHash);

        assert nearNodeId != null;
        assert rmtFutId != null;

        this.nearNodeId = nearNodeId;
        this.rmtFutId = rmtFutId;
        this.nearXidVer = nearXidVer;
        this.txNodes = txNodes;

        readMap = Collections.emptyMap();

        writeMap = new ConcurrentLinkedHashMap<>(txSize, 1.0f);

        topologyVersion(topVer);
    }

    /**
     * This constructor is meant for pessimistic transactions.
     *
     * @param nearNodeId Near node ID.
     * @param rmtFutId Remote future ID.
     * @param nodeId Node ID.
     * @param nearXidVer Near transaction ID.
     * @param rmtThreadId Remote thread ID.
     * @param topVer Topology version.
     * @param xidVer XID version.
     * @param commitVer Commit version.
     * @param concurrency Concurrency level (should be pessimistic).
     * @param isolation Transaction isolation.
     * @param invalidate Invalidate flag.
     * @param timeout Timeout.
     * @param ctx Cache context.
     * @param txSize Expected transaction size.
     * @param grpLockKey Group lock key if transaction is group-lock.
     */
    public GridDhtTxRemote(
        UUID nearNodeId,
        GridUuid rmtFutId,
        UUID nodeId,
        GridCacheVersion nearXidVer,
        long rmtThreadId,
        long topVer,
        GridCacheVersion xidVer,
        GridCacheVersion commitVer,
        GridCacheTxConcurrency concurrency,
        GridCacheTxIsolation isolation,
        boolean invalidate,
        long timeout,
        GridCacheContext<K, V> ctx,
        int txSize,
        @Nullable Object grpLockKey,
        @Nullable UUID subjId,
        int taskNameHash
    ) {
        super(ctx, nodeId, rmtThreadId, xidVer, commitVer, concurrency, isolation, invalidate, timeout, txSize,
            grpLockKey, subjId, taskNameHash);

        assert nearNodeId != null;
        assert rmtFutId != null;

        this.nearXidVer = nearXidVer;
        this.nearNodeId = nearNodeId;
        this.rmtFutId = rmtFutId;

        readMap = Collections.emptyMap();
        writeMap = new ConcurrentLinkedHashMap<>(txSize, 1.0f);

        topologyVersion(topVer);
    }

    /** {@inheritDoc} */
    @Override public boolean dht() {
        return true;
    }

    /** {@inheritDoc} */
    @Override public UUID eventNodeId() {
        return nearNodeId();
    }

    /** {@inheritDoc} */
    @Override public Collection<UUID> masterNodeIds() {
        return Arrays.asList(nearNodeId, nodeId);
    }

    /** {@inheritDoc} */
    @Override public UUID otherNodeId() {
        return nearNodeId;
    }

    /** {@inheritDoc} */
    @Override public boolean enforceSerializable() {
        return false; // Serializable will be enforced on primary mode.
    }

    /** {@inheritDoc} */
    @Override public GridCacheVersion nearXidVersion() {
        return nearXidVer;
    }

    /** {@inheritDoc} */
    @Override public Map<UUID, Collection<UUID>> transactionNodes() {
        return txNodes;
    }

    /**
     * @return Near node ID.
     */
    UUID nearNodeId() {
        return nearNodeId;
    }

    /**
     * @return Remote future ID.
     */
    GridUuid remoteFutureId() {
        return rmtFutId;
    }

    /** {@inheritDoc} */
    @Override protected boolean updateNearCache(K key, long topVer) {
        if (!cctx.isDht() || !isNearEnabled(cctx) || cctx.localNodeId().equals(nearNodeId))
            return false;

        if (cctx.config().getBackups() == 0)
            return true;

        // Check if we are on the backup node.
        return !cctx.affinity().backups(key, topVer).contains(cctx.localNode());
    }

    /** {@inheritDoc} */
    @Override public void addInvalidPartition(int part) {
        super.addInvalidPartition(part);

        for (Iterator<GridCacheTxEntry<K, V>> it = writeMap.values().iterator(); it.hasNext();) {
            GridCacheTxEntry<K, V> e = it.next();

            GridCacheEntryEx<K, V> cached = e.cached();

            if (cached != null) {
                if (cached.partition() == part)
                    it.remove();
            }
            else if (cctx.affinity().partition(e.key()) == part)
                it.remove();
        }
    }

    /**
     * @param entry Write entry.
     * @param ldr Class loader.
     * @throws GridException If failed.
     */
    void addWrite(GridCacheTxEntry<K, V> entry, ClassLoader ldr) throws GridException {
        entry.unmarshal(cctx, ldr);

        try {
            GridDhtCacheEntry<K, V> cached = cctx.dht().entryExx(entry.key(), topologyVersion());

            checkInternal(entry.key());

            // Initialize cache entry.
            entry.cached(cached, entry.keyBytes());

            writeMap.put(entry.key(), entry);

            addExplicit(entry);
        }
        catch (GridDhtInvalidPartitionException e) {
            addInvalidPartition(e.partition());
        }
    }

    /**
     * @param op Write operation.
     * @param key Key to add to write set.
     * @param keyBytes Key bytes.
     * @param val Value.
     * @param valBytes Value bytes.
     * @param drVer Data center replication version.
     * @param clos Transform closures.
     */
    void addWrite(GridCacheOperation op, K key, byte[] keyBytes, @Nullable V val, @Nullable byte[] valBytes,
        @Nullable Collection<GridClosure<V, V>> clos, @Nullable GridCacheVersion drVer) {
        checkInternal(key);

        if (isSystemInvalidate())
            return;

        GridDhtCacheEntry<K, V> cached = cctx.dht().entryExx(key, topologyVersion());

        GridCacheTxEntry<K, V> txEntry = new GridCacheTxEntry<>(cctx, this, op, val, 0L, -1L, cached, drVer);

        txEntry.keyBytes(keyBytes);
        txEntry.valueBytes(valBytes);
        txEntry.transformClosures(clos);

        writeMap.put(key, txEntry);
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return GridToStringBuilder.toString(GridDhtTxRemote.class, this, "super", super.toString());
    }
}
