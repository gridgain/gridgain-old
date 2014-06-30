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

package org.gridgain.grid.kernal.processors.cache.distributed.near;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.kernal.processors.cache.*;
import org.gridgain.grid.kernal.processors.cache.distributed.dht.*;
import org.gridgain.grid.kernal.processors.cache.distributed.dht.atomic.*;
import org.gridgain.grid.kernal.processors.cache.dr.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.product.*;
import org.gridgain.grid.security.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.future.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.util.*;

import static org.gridgain.grid.GridSystemProperties.*;
import static org.gridgain.grid.cache.GridCacheFlag.*;
import static org.gridgain.grid.kernal.processors.cache.GridCacheOperation.*;
import static org.gridgain.grid.kernal.processors.dr.GridDrType.*;

/**
 * Near cache for atomic cache.
 */
public class GridNearAtomicCache<K, V> extends GridNearCacheAdapter<K, V> {
    /** */
    private static final long serialVersionUID = 0L;

    /** */
    public static final GridProductVersion SINCE_VER = GridProductVersion.fromString("6.0.3");

    /** */
    private GridDhtCacheAdapter<K, V> dht;

    /** Remove queue. */
    private GridCircularBuffer<T2<K, GridCacheVersion>> rmvQueue;

    /**
     * Empty constructor required for {@link Externalizable}.
     */
    public GridNearAtomicCache() {
        // No-op.
    }

    /**
     * @param ctx Context.
     */
    public GridNearAtomicCache(GridCacheContext<K, V> ctx) {
        super(ctx);

        int size = Integer.getInteger(GG_ATOMIC_CACHE_DELETE_HISTORY_SIZE, 1_000_000);

        rmvQueue = new GridCircularBuffer<>(U.ceilPow2(size / 10));
    }

    /** {@inheritDoc} */
    @Override public void start() throws GridException {
        ctx.io().addHandler(GridNearGetResponse.class, new CI2<UUID, GridNearGetResponse<K, V>>() {
            @Override public void apply(UUID nodeId, GridNearGetResponse<K, V> res) {
                processGetResponse(nodeId, res);
            }
        });
    }

    /**
     * @param dht DHT cache.
     */
    public void dht(GridDhtAtomicCache<K, V> dht) {
        this.dht = dht;
    }

    /** {@inheritDoc} */
    @Override public GridDhtCacheAdapter<K, V> dht() {
        return dht;
    }

    /**
     * @param req Update request.
     * @param res Update response.
     */
    public void processNearAtomicUpdateResponse(
        GridNearAtomicUpdateRequest<K, V> req,
        GridNearAtomicUpdateResponse<K, V> res
    ) {
        /*
         * Choose value to be stored in near cache: first check key is not in failed and not in skipped list,
         * then check if value was generated on primary node, if not then use value sent in request.
         */

        Collection<K> failed = res.failedKeys();
        List<Integer> nearValsIdxs = res.nearValuesIndexes();
        List<Integer> skipped = res.skippedIndexes();

        GridCacheVersion ver = req.updateVersion();

        if (ver == null)
            ver = res.nearVersion();

        assert ver != null;

        int nearValIdx = 0;

        for (int i = 0; i < req.keys().size(); i++) {
            if (F.contains(skipped, i))
                continue;

            K key = req.keys().get(i);

            if (F.contains(failed, key))
                continue;

            if (ctx.affinity().nodes(key, req.topologyVersion()).contains(ctx.localNode())) { // Reader became backup.
                GridCacheEntryEx<K, V> entry = peekEx(key);

                if (entry != null && entry.markObsolete(ver))
                    removeEntry(entry);

                continue;
            }

            V val = null;
            byte[] valBytes = null;

            if (F.contains(nearValsIdxs, i)) {
                val = res.nearValue(nearValIdx);
                valBytes = res.nearValueBytes(nearValIdx);

                nearValIdx++;
            }
            else {
                assert req.operation() != TRANSFORM;

                if (req.operation() != DELETE) {
                    val = req.value(i);
                    valBytes = req.valueBytes(i);
                }
            }

            try {
                processNearAtomicUpdateResponse(ver, key, val, valBytes, res.nearTtl(), req.nodeId(), req.subjectId());
            }
            catch (GridException e) {
                res.addFailedKey(key, new GridException("Failed to update key in near cache: " + key, e));
            }
        }
    }

    /**
     * @param ver Version.
     * @param key Key.
     * @param val Value.
     * @param valBytes Value bytes.
     * @param ttl Time to live.
     * @param nodeId Node ID.
     * @throws GridException If failed.
     */
    private void processNearAtomicUpdateResponse(
        GridCacheVersion ver,
        K key,
        @Nullable V val,
        @Nullable byte[] valBytes,
        Long ttl,
        UUID nodeId,
        UUID subjId
    ) throws GridException {
        try {
            while (true) {
                GridCacheEntryEx<K, V> entry = null;

                long topVer = ctx.affinity().affinityTopologyVersion();

                try {
                    entry = entryEx(key, topVer);

                    GridCacheOperation op = (val != null || valBytes != null) ? UPDATE : DELETE;

                    GridCacheUpdateAtomicResult<K, V> updRes = entry.innerUpdate(
                        ver,
                        nodeId,
                        nodeId,
                        op,
                        val,
                        valBytes,
                        /*write-through*/false,
                        /*retval*/false,
                        ttl,
                        /*event*/true,
                        /*metrics*/true,
                        /*primary*/false,
                        /*check version*/true,
                        CU.<K, V>empty(),
                        DR_NONE,
                        -1,
                        -1,
                        null,
                        false,
                        false,
                        subjId);

                    if (updRes.removeVersion() != null)
                        ctx.onDeferredDelete(entry, updRes.removeVersion());

                    break; // While.
                }
                catch (GridCacheEntryRemovedException ignored) {
                    if (log.isDebugEnabled())
                        log.debug("Got removed entry while updating near cache value (will retry): " + key);

                    entry = null;
                }
                finally {
                    if (entry != null)
                        ctx.evicts().touch(entry, topVer);
                }
            }
        }
        catch (GridDhtInvalidPartitionException ignored) {
            // Ignore.
        }
    }

    /**
     * @param nodeId Sender node ID.
     * @param req Dht atomic update request.
     * @param res Dht atomic update response.
     */
    public void processDhtAtomicUpdateRequest(
        UUID nodeId,
        GridDhtAtomicUpdateRequest<K, V> req,
        GridDhtAtomicUpdateResponse<K, V> res
    ) {
        GridCacheVersion ver = req.writeVersion();

        assert ver != null;

        Collection<K> backupKeys = req.keys();

        boolean intercept = req.forceTransformBackups() && ctx.config().getInterceptor() != null;

        for (int i = 0; i < req.nearSize(); i++) {
            K key = req.nearKey(i);

            try {
                while (true) {
                    try {
                        GridCacheEntryEx<K, V> entry = peekEx(key);

                        if (entry == null) {
                            res.addNearEvicted(key, req.nearKeyBytes(i));

                            break;
                        }

                        if (F.contains(backupKeys, key)) { // Reader became backup.
                            if (entry.markObsolete(ver))
                                removeEntry(entry);

                            break;
                        }

                        V val = req.nearValue(i);
                        byte[] valBytes = req.nearValueBytes(i);
                        GridClosure<V, V> transform = req.nearTransformClosure(i);

                        GridCacheOperation op = transform != null ? TRANSFORM :
                            (val != null || valBytes != null) ?
                                UPDATE :
                                DELETE;

                        GridCacheUpdateAtomicResult<K, V> updRes = entry.innerUpdate(
                            ver,
                            nodeId,
                            nodeId,
                            op,
                            op == TRANSFORM ? transform : val,
                            valBytes,
                            /*write-through*/false,
                            /*retval*/false,
                            req.ttl(),
                            /*event*/true,
                            /*metrics*/true,
                            /*primary*/false,
                            /*check version*/!req.forceTransformBackups(),
                            CU.<K, V>empty(),
                            DR_NONE,
                            -1,
                            -1,
                            null,
                            false,
                            intercept,
                            req.subjectId());

                        if (updRes.removeVersion() != null)
                            ctx.onDeferredDelete(entry, updRes.removeVersion());

                        break;
                    }
                    catch (GridCacheEntryRemovedException ignored) {
                        if (log.isDebugEnabled())
                            log.debug("Got removed entry while updating near value (will retry): " + key);
                    }
                }
            }
            catch (GridException e) {
                res.addFailedKey(key, new GridException("Failed to update near cache key: " + key, e));
            }
        }
    }

    /** {@inheritDoc} */
    @Override protected GridFuture<Map<K, V>> getAllAsync(
        @Nullable Collection<? extends K> keys,
        boolean forcePrimary,
        boolean skipTx,
        @Nullable GridCacheEntryEx<K, V> entry,
        @Nullable UUID subjId,
        @Nullable GridPredicate<GridCacheEntry<K, V>>... filter
    ) {
        ctx.denyOnFlag(LOCAL);
        ctx.checkSecurity(GridSecurityPermission.CACHE_READ);

        if (F.isEmpty(keys))
            return new GridFinishedFuture<>(ctx.kernalContext(), Collections.<K, V>emptyMap());

        subjId = ctx.subjectIdPerCall(subjId);

        return loadAsync(null, keys, false, forcePrimary, filter, subjId);
    }

    /** {@inheritDoc} */
    @Override public V put(
        K key,
        V val,
        @Nullable GridCacheEntryEx<K, V> cached,
        long ttl,
        @Nullable GridPredicate<GridCacheEntry<K, V>>[] filter
    ) throws GridException {
        return dht.put(key, val, cached, ttl, filter);
    }

    /** {@inheritDoc} */
    @Override public boolean putx(K key,
        V val,
        @Nullable GridCacheEntryEx<K, V> cached,
        long ttl,
        @Nullable GridPredicate<GridCacheEntry<K, V>>... filter) throws GridException {
        return dht.putx(key, val, cached, ttl, filter);
    }

    /** {@inheritDoc} */
    @Override public boolean putx(K key,
        V val,
        GridPredicate<GridCacheEntry<K, V>>[] filter) throws GridException {
        return dht.putx(key, val, filter);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override public GridFuture<V> putAsync(K key,
        V val,
        @Nullable GridCacheEntryEx<K, V> entry,
        long ttl,
        @Nullable GridPredicate<GridCacheEntry<K, V>>... filter) {
        return dht.putAsync(key, val, entry, ttl, filter);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override public GridFuture<Boolean> putxAsync(K key,
        V val,
        @Nullable GridCacheEntryEx<K, V> entry,
        long ttl,
        @Nullable GridPredicate<GridCacheEntry<K, V>>... filter) {
        return dht.putxAsync(key, val, entry, ttl, filter);
    }

    /** {@inheritDoc} */
    @Override public V putIfAbsent(K key, V val) throws GridException {
        return dht.putIfAbsent(key, val);
    }

    /** {@inheritDoc} */
    @Override public GridFuture<V> putIfAbsentAsync(K key, V val) {
        return dht.putIfAbsentAsync(key, val);
    }

    /** {@inheritDoc} */
    @Override public boolean putxIfAbsent(K key, V val) throws GridException {
        return dht.putxIfAbsent(key, val);
    }

    /** {@inheritDoc} */
    @Override public GridFuture<Boolean> putxIfAbsentAsync(K key, V val) {
        return dht.putxIfAbsentAsync(key, val);
    }

    /** {@inheritDoc} */
    @Override public V replace(K key, V val) throws GridException {
        return dht.replace(key, val);
    }

    /** {@inheritDoc} */
    @Override public GridFuture<V> replaceAsync(K key, V val) {
        return dht.replaceAsync(key, val);
    }

    /** {@inheritDoc} */
    @Override public boolean replacex(K key, V val) throws GridException {
        return dht.replacex(key, val);
    }

    /** {@inheritDoc} */
    @Override public GridFuture<Boolean> replacexAsync(K key, V val) {
        return dht.replacexAsync(key, val);
    }

    /** {@inheritDoc} */
    @Override public boolean replace(K key, V oldVal, V newVal) throws GridException {
        return dht.replace(key, oldVal, newVal);
    }

    /** {@inheritDoc} */
    @Override public GridFuture<Boolean> replaceAsync(K key, V oldVal, V newVal) {
        return dht.replaceAsync(key, oldVal, newVal);
    }

    /** {@inheritDoc} */
    @Override public GridCacheReturn<V> removex(K key, V val) throws GridException {
        return dht.removex(key, val);
    }

    /** {@inheritDoc} */
    @Override public GridCacheReturn<V> replacex(K key, V oldVal, V newVal) throws GridException {
        return dht.replacex(key, oldVal, newVal);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override public GridFuture<GridCacheReturn<V>> removexAsync(K key, V val) {
        return dht.removexAsync(key, val);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override public GridFuture<GridCacheReturn<V>> replacexAsync(K key, V oldVal, V newVal) {
        return dht.replacexAsync(key, oldVal, newVal);
    }

    /** {@inheritDoc} */
    @Override public void putAll(Map<? extends K, ? extends V> m, GridPredicate<GridCacheEntry<K, V>>[] filter)
        throws GridException {
        dht.putAll(m, filter);
    }

    /** {@inheritDoc} */
    @Override public GridFuture<?> putAllAsync(Map<? extends K, ? extends V> m,
        @Nullable GridPredicate<GridCacheEntry<K, V>>[] filter) {
        return dht.putAllAsync(m, filter);
    }

    /** {@inheritDoc} */
    @Override public void putAllDr(Map<? extends K, GridCacheDrInfo<V>> drMap) throws GridException {
        dht.putAllDr(drMap);
    }

    /** {@inheritDoc} */
    @Override public GridFuture<?> putAllDrAsync(Map<? extends K, GridCacheDrInfo<V>> drMap) throws GridException {
        return dht.putAllDrAsync(drMap);
    }

    /** {@inheritDoc} */
    @Override public void transform(K key, GridClosure<V, V> transformer) throws GridException {
        dht.transform(key, transformer);
    }

    /** {@inheritDoc} */
    @Override public <R> R transformAndCompute(K key, GridClosure<V, GridBiTuple<V, R>> transformer)
        throws GridException {
        return dht.transformAndCompute(key, transformer);
    }

    /** {@inheritDoc} */
    @Override public GridFuture<?> transformAsync(K key,
        GridClosure<V, V> transformer,
        @Nullable GridCacheEntryEx<K, V> entry,
        long ttl) {
        return dht.transformAsync(key, transformer, entry, ttl);
    }

    /** {@inheritDoc} */
    @Override public void transformAll(@Nullable Map<? extends K, ? extends GridClosure<V, V>> m) throws GridException {
        dht.transformAll(m);
    }

    /** {@inheritDoc} */
    @Override public GridFuture<?> transformAllAsync(@Nullable Map<? extends K, ? extends GridClosure<V, V>> m) {
        return dht.transformAllAsync(m);
    }

    /** {@inheritDoc} */
    @Override public V remove(K key,
        @Nullable GridCacheEntryEx<K, V> entry,
        @Nullable GridPredicate<GridCacheEntry<K, V>>... filter) throws GridException {
        return dht.remove(key, entry, filter);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override public GridFuture<V> removeAsync(K key,
        @Nullable GridCacheEntryEx<K, V> entry,
        @Nullable GridPredicate<GridCacheEntry<K, V>>... filter) {
        return dht.removeAsync(key, entry, filter);
    }

    /** {@inheritDoc} */
    @Override public void removeAll(Collection<? extends K> keys, GridPredicate<GridCacheEntry<K, V>>... filter)
        throws GridException {
        dht.removeAll(keys, filter);
    }

    /** {@inheritDoc} */
    @Override public GridFuture<?> removeAllAsync(Collection<? extends K> keys,
        GridPredicate<GridCacheEntry<K, V>>[] filter) {
        return dht.removeAllAsync(keys, filter);
    }

    /** {@inheritDoc} */
    @Override public boolean removex(K key,
        @Nullable GridCacheEntryEx<K, V> entry,
        @Nullable GridPredicate<GridCacheEntry<K, V>>... filter) throws GridException {
        return dht.removex(key, entry, filter);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override public GridFuture<Boolean> removexAsync(K key,
        @Nullable GridCacheEntryEx<K, V> entry,
        @Nullable GridPredicate<GridCacheEntry<K, V>>... filter) {
        return dht.removexAsync(key, entry, filter);
    }

    /** {@inheritDoc} */
    @Override public boolean remove(K key, V val) throws GridException {
        return dht.remove(key, val);
    }

    /** {@inheritDoc} */
    @Override public GridFuture<Boolean> removeAsync(K key, V val) {
        return dht.removeAsync(key, val);
    }

    /** {@inheritDoc} */
    @Override public void removeAll(GridPredicate<GridCacheEntry<K, V>>[] filter) throws GridException {
        dht.removeAll(keySet(filter));
    }

    /** {@inheritDoc} */
    @Override public GridFuture<?> removeAllAsync(GridPredicate<GridCacheEntry<K, V>>[] filter) {
        return dht.removeAllAsync(keySet(filter));
    }

    /** {@inheritDoc} */
    @Override public void removeAllDr(Map<? extends K, GridCacheVersion> drMap) throws GridException {
        dht.removeAllDr(drMap);
    }

    /** {@inheritDoc} */
    @Override public GridFuture<?> removeAllDrAsync(Map<? extends K, GridCacheVersion> drMap) throws GridException {
        return dht.removeAllDrAsync(drMap);
    }

    /** {@inheritDoc} */
    @Override protected GridFuture<Boolean> lockAllAsync(Collection<? extends K> keys,
        long timeout,
        @Nullable GridCacheTxLocalEx<K, V> tx,
        boolean isInvalidate,
        boolean isRead,
        boolean retval,
        @Nullable GridCacheTxIsolation isolation,
        GridPredicate<GridCacheEntry<K, V>>[] filter) {
        return dht.lockAllAsync(keys, timeout, filter);
    }

    /** {@inheritDoc} */
    @Override public GridCacheTxLocalAdapter<K, V> newTx(boolean implicit,
        boolean implicitSingle,
        GridCacheTxConcurrency concurrency,
        GridCacheTxIsolation isolation,
        long timeout,
        boolean invalidate,
        boolean syncCommit,
        boolean syncRollback,
        boolean swapOrOffheapEnabled,
        boolean storeEnabled,
        int txSize,
        @Nullable Object grpLockKey,
        boolean partLock) {
        return dht.newTx(implicit, implicitSingle, concurrency, isolation, timeout, invalidate, syncCommit,
            syncRollback, swapOrOffheapEnabled, storeEnabled, txSize, grpLockKey, partLock);
    }

    /** {@inheritDoc} */
    @Override public void unlockAll(@Nullable Collection<? extends K> keys,
        @Nullable GridPredicate<GridCacheEntry<K, V>>... filter) throws GridException {
        dht.unlockAll(keys, filter);
    }

    /** {@inheritDoc} */
    @Override public void onDeferredDelete(GridCacheEntryEx<K, V> entry, GridCacheVersion ver) {
        assert entry.isNear();

        try {
            T2<K, GridCacheVersion> evicted = rmvQueue.add(new T2<>(entry.key(), ver));

            if (evicted != null)
                removeVersionedEntry(evicted.get1(), evicted.get2());
        }
        catch (InterruptedException ignore) {
            if (log.isDebugEnabled())
                log.debug("Failed to enqueue deleted entry [key=" + entry.key() + ", ver=" + ver + ']');

            Thread.currentThread().interrupt();
        }
    }
}
