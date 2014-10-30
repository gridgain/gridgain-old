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

package org.gridgain.grid.kernal.processors.cache.local;

import org.gridgain.grid.kernal.processors.cache.*;
import org.jetbrains.annotations.*;

import static org.gridgain.grid.events.GridEventType.*;

/**
 * Cache entry for local caches.
 */
@SuppressWarnings({"NonPrivateFieldAccessedInSynchronizedContext", "TooBroadScope"})
public class GridLocalCacheEntry<K, V> extends GridCacheMapEntry<K, V> {
    /** */
    private static final long serialVersionUID = 0L;

    /**
     * @param ctx  Cache registry.
     * @param key  Cache key.
     * @param hash Key hash value.
     * @param val Entry value.
     * @param next Next entry in the linked list.
     * @param ttl  Time to live.
     * @param hdrId Header id.
     */
    public GridLocalCacheEntry(GridCacheContext<K, V> ctx, K key, int hash, V val,
        GridCacheMapEntry<K, V> next, long ttl, int hdrId) {
        super(ctx, key, hash, val, next, ttl, hdrId);
    }

    /** {@inheritDoc} */
    @Override public boolean isLocal() {
        return true;
    }

    /**
     * Add local candidate.
     *
     * @param threadId Owning thread ID.
     * @param ver Lock version.
     * @param timeout Timeout to acquire lock.
     * @param reenter Reentry flag.
     * @param tx Transaction flag.
     * @param implicitSingle Implicit transaction flag.
     * @return New candidate.
     * @throws GridCacheEntryRemovedException If entry has been removed.
     */
    @Nullable public GridCacheMvccCandidate<K> addLocal(
        long threadId,
        GridCacheVersion ver,
        long timeout,
        boolean reenter,
        boolean tx,
        boolean implicitSingle) throws GridCacheEntryRemovedException {
        GridCacheMvccCandidate<K> prev;
        GridCacheMvccCandidate<K> cand;
        GridCacheMvccCandidate<K> owner;

        V val;
        boolean hasVal;

        synchronized (this) {
            checkObsolete();

            GridCacheMvcc<K> mvcc = mvccExtras();

            if (mvcc == null) {
                mvcc = new GridCacheMvcc<>(cctx);

                mvccExtras(mvcc);
            }

            prev = mvcc.localOwner();

            cand = mvcc.addLocal(
                this,
                threadId,
                ver,
                timeout,
                reenter,
                tx,
                implicitSingle
            );

            owner = mvcc.localOwner();

            val = this.val;

            hasVal = hasValueUnlocked();

            if (mvcc.isEmpty())
                mvccExtras(null);
        }

        if (cand != null) {
            if (!cand.reentry())
                cctx.mvcc().addNext(cand);

            // Event notification.
            if (cctx.events().isRecordable(EVT_CACHE_OBJECT_LOCKED))
                cctx.events().addEvent(partition(), key, cand.nodeId(), cand, EVT_CACHE_OBJECT_LOCKED, val, hasVal,
                    val, hasVal, null, null, null);
        }

        checkOwnerChanged(prev, owner);

        return cand;
    }

    /**
     *
     * @param cand Candidate.
     * @return Current owner.
     */
    @Nullable public GridCacheMvccCandidate<K> readyLocal(GridCacheMvccCandidate<K> cand) {
        GridCacheMvccCandidate<K> prev = null;
        GridCacheMvccCandidate<K> owner = null;

        synchronized (this) {
            GridCacheMvcc<K> mvcc = mvccExtras();

            if (mvcc != null) {
                prev = mvcc.localOwner();

                owner = mvcc.readyLocal(cand);

                if (mvcc.isEmpty())
                    mvccExtras(null);
            }
        }

        checkOwnerChanged(prev, owner);

        return owner;
    }

    /**
     *
     * @param ver Candidate version.
     * @return Current owner.
     */
    @Nullable public GridCacheMvccCandidate<K> readyLocal(GridCacheVersion ver) {
        GridCacheMvccCandidate<K> prev = null;
        GridCacheMvccCandidate<K> owner = null;

        synchronized (this) {
            GridCacheMvcc<K> mvcc = mvccExtras();

            if (mvcc != null) {
                prev = mvcc.localOwner();

                owner = mvcc.readyLocal(ver);

                if (mvcc.isEmpty())
                    mvccExtras(null);
            }
        }

        checkOwnerChanged(prev, owner);

        return owner;
    }

    /** {@inheritDoc} */
    @Override public boolean tmLock(GridCacheTxEx<K, V> tx, long timeout) throws GridCacheEntryRemovedException {
        GridCacheMvccCandidate<K> cand = addLocal(
            tx.threadId(),
            tx.xidVersion(),
            timeout,
            /*reenter*/false,
            /*tx*/true,
            tx.implicitSingle()
        );

        if (cand != null) {
            readyLocal(cand);

            return true;
        }

        return false;
    }

    /**
     * Rechecks if lock should be reassigned.
     *
     * @return Current owner.
     */
    @Nullable public GridCacheMvccCandidate<K> recheck() {
        GridCacheMvccCandidate<K> prev = null;
        GridCacheMvccCandidate<K> owner = null;

        synchronized (this) {
            GridCacheMvcc<K> mvcc = mvccExtras();

            if (mvcc != null) {
                prev = mvcc.localOwner();

                owner = mvcc.recheck();

                if (mvcc.isEmpty())
                    mvccExtras(null);
            }
        }

        checkOwnerChanged(prev, owner);

        return owner;
    }

    /**
     * @param prev Previous owner.
     * @param owner Current owner.
     */
    private void checkOwnerChanged(GridCacheMvccCandidate<K> prev, GridCacheMvccCandidate<K> owner) {
        assert !Thread.holdsLock(this);

        if (owner != prev) {
            cctx.mvcc().callback().onOwnerChanged(this, prev, owner);

            if (owner != null)
                checkThreadChain(owner);
        }
    }

    /**
     * @param owner Starting candidate in the chain.
     */
    private void checkThreadChain(GridCacheMvccCandidate<K> owner) {
        assert !Thread.holdsLock(this);

        assert owner != null;
        assert owner.owner() || owner.used() : "Neither owner or used flags are set on ready local candidate: " +
            owner;

        if (owner.next() != null) {
            for (GridCacheMvccCandidate<K> cand = owner.next(); cand != null; cand = cand.next()) {
                assert cand.local();

                // Allow next lock in the thread to proceed.
                if (!cand.used()) {
                    GridLocalCacheEntry<K, V> e =
                        (GridLocalCacheEntry<K, V>)cctx.cache().peekEx(cand.key());

                    // At this point candidate may have been removed and entry destroyed,
                    // so we check for null.
                    if (e != null)
                        e.recheck();

                    break;
                }
            }
        }
    }

    /**
     * Unlocks lock if it is currently owned.
     *
     * @param tx Transaction to unlock.
     */
    @Override public void txUnlock(GridCacheTxEx<K, V> tx) throws GridCacheEntryRemovedException {
        removeLock(tx.xidVersion());
    }

    /**
     * Releases local lock.
     */
    void releaseLocal() {
        releaseLocal(Thread.currentThread().getId());
    }

    /**
     * Releases local lock.
     *
     * @param threadId Thread ID.
     */
    void releaseLocal(long threadId) {
        GridCacheMvccCandidate<K> prev = null;
        GridCacheMvccCandidate<K> owner = null;

        V val;
        boolean hasVal;

        synchronized (this) {
            GridCacheMvcc<K> mvcc = mvccExtras();

            if (mvcc != null) {
                prev = mvcc.localOwner();

                owner = mvcc.releaseLocal(threadId);

                if (mvcc.isEmpty())
                    mvccExtras(null);
            }

            val = this.val;
            hasVal = hasValueUnlocked();
        }

        if (prev != null && owner != prev) {
            checkThreadChain(prev);

            // Event notification.
            if (cctx.events().isRecordable(EVT_CACHE_OBJECT_UNLOCKED))
                cctx.events().addEvent(partition(), key, prev.nodeId(), prev, EVT_CACHE_OBJECT_UNLOCKED, val, hasVal,
                    val, hasVal, null, null, null);
        }

        checkOwnerChanged(prev, owner);
    }

    /**
     * Removes candidate regardless if it is owner or not.
     *
     * @param cand Candidate to remove.
     * @throws GridCacheEntryRemovedException If the entry was removed by version other
     *      than one passed in.
     */
    void removeLock(GridCacheMvccCandidate<K> cand) throws GridCacheEntryRemovedException {
        removeLock(cand.version());
    }

    /** {@inheritDoc} */
    @Override public boolean removeLock(GridCacheVersion ver) throws GridCacheEntryRemovedException {
        GridCacheMvccCandidate<K> prev = null;
        GridCacheMvccCandidate<K> owner = null;

        GridCacheMvccCandidate<K> doomed;

        V val;
        boolean hasVal;

        synchronized (this) {
            GridCacheVersion obsoleteVer = obsoleteVersionExtras();

            if (obsoleteVer != null && !obsoleteVer.equals(ver))
                checkObsolete();

            GridCacheMvcc<K> mvcc = mvccExtras();

            doomed = mvcc == null ? null : mvcc.candidate(ver);

            if (doomed != null) {
                prev = mvcc.localOwner();

                owner = mvcc.remove(ver);

                if (mvcc.isEmpty())
                    mvccExtras(null);
            }

            val = this.val;
            hasVal = hasValueUnlocked();
        }

        if (doomed != null) {
            checkThreadChain(doomed);

            // Event notification.
            if (cctx.events().isRecordable(EVT_CACHE_OBJECT_UNLOCKED))
                cctx.events().addEvent(partition(), key, doomed.nodeId(), doomed, EVT_CACHE_OBJECT_UNLOCKED,
                    val, hasVal, val, hasVal, null, null, null);
        }

        checkOwnerChanged(prev, owner);

        return doomed != null;
    }
}
