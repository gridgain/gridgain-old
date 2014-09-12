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

package org.gridgain.grid.cache;

import org.gridgain.grid.*;
import org.gridgain.grid.lang.*;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.Map.*;

/**
 * This interface provides a rich API for working with individual cache entries. It
 * includes the following main functionality:
 * <ul>
 * <li>
 *  Various {@code 'get(..)'} methods to synchronously or asynchronously get values from cache.
 *  All {@code 'get(..)'} methods are transactional and will participate in an ongoing transaction
 *  if there is one.
 * </li>
 * <li>
 *  Various {@code 'set(..)'}, {@code 'setIfAbsent(..)'}, and {@code 'replace(..)'} methods to
 *  synchronously or asynchronously put single or multiple entries into cache.
 *  All these methods are transactional and will participate in an ongoing transaction
 *  if there is one.
 * </li>
 * <li>
 *  Various {@code 'remove(..)'} methods to synchronously or asynchronously remove single or multiple keys
 *  from cache. All {@code 'remove(..)'} methods are transactional and will participate in an ongoing transaction
 *  if there is one.
 * </li>
 * <li>
 *  Various {@code 'invalidate(..)'} methods to set cached values to {@code null}.
 * <li>
 * <li>
 *  Various {@code 'isLocked(..)'} methods to check on distributed locks on a single or multiple keys
 *  in cache. All locking methods are not transactional and will not enlist keys into ongoing transaction,
 *  if any.
 * </li>
 * <li>
 *  Various {@code 'peek(..)'} methods to peek at values in global or transactional memory, swap
 *  storage, or persistent storage.
 * </li>
 * <li>
 *  Various {@code 'reload(..)'} methods to reload latest values from persistent storage.
 * </li>
 * <li>
 *  Method {@link #evict()} to evict elements from cache, and optionally store
 *  them in underlying swap storage for later access. All {@code 'evict(..)'} methods are not
 *  transactional and will not enlist evicted keys into ongoing transaction, if any.
 * </li>
 * <li>
 *  Methods for {@link #timeToLive(long)} to change or lookup entry's time to live.
 * </ul>
 * <h1 class="header">Extended Put And Remove Methods</h1>
 * All methods that end with {@code 'x'} provide the same functionality as their sibling
 * methods that don't end with {@code 'x'}, however instead of returning a previous value they
 * return a {@code boolean} flag indicating whether operation succeeded or not. Returning
 * a previous value may involve a network trip or a persistent store lookup and should be
 * avoided whenever not needed.
 * <h1 class="header">Predicate Filters</h1>
 * All filters passed into methods on this API are checked <b>atomically</b>. In other words the value
 * of cache entry is guaranteed not to change throughout the cache operation.
 * <h1 class="header">Transactions</h1>
 * Cache API supports distributed transactions. All {@code 'get(..)'}, {@code 'put(..)'}, {@code 'replace(..)'},
 * and {@code 'remove(..)'} operations are transactional and will participate in an ongoing transaction.
 * Other methods like {@code 'peek(..)'} may be transaction-aware, i.e. check in-transaction entries first, but
 * will not affect the current state of transaction. See {@link GridCacheTx} documentation for more information
 * about transactions.
 * @param <K> Key type.
 * @param <V> Value type.
 */
public interface GridCacheEntry<K, V> extends Map.Entry<K, V>, GridMetadataAware {
    /**
     * Cache projection to which this entry belongs. Note that entry and its
     * parent projections have same flags and filters.
     *
     * @return Cache projection for the cache to which this entry belongs.
     */
    public GridCacheProjection<K, V> projection();

    /**
     * This method has the same semantic as {@link GridCacheProjection#peek(Object)} method.
     *
     * @return See {@link GridCacheProjection#peek(Object)}.
     */
    @Nullable public V peek();

    /**
     * This method has the same semantic as
     * {@link GridCacheProjection#peek(Object, Collection)} method.
     *
     * @param modes See {@link GridCacheProjection#peek(Object, Collection)}.
     * @return See {@link GridCacheProjection#peek(Object, Collection)}.
     * @throws GridException See {@link GridCacheProjection#peek(Object, Collection)}.
     */
    @Nullable public V peek(@Nullable Collection<GridCachePeekMode> modes) throws GridException;

    /**
     * This method has the same semantic as
     * {@link GridCacheProjection#reload(Object)} method.
     *
     * @return See {@link GridCacheProjection#reload(Object)}.
     * @throws GridException See {@link GridCacheProjection#reload(Object)}.
     */
    @Nullable public V reload() throws GridException;

    /**
     * This method has the same semantic as
     * {@link GridCacheProjection#reloadAsync(Object)} method.
     *
     * @return See {@link GridCacheProjection#reloadAsync(Object)}.
     */
    public GridFuture<V> reloadAsync();

    /**
     * This method has the same semantic as
     * {@link GridCacheProjection#isLocked(Object)} method.
     *
     * @return See {@link GridCacheProjection#isLocked(Object)}.
     */
    public boolean isLocked();

    /**
     * This method has the same semantic as
     * {@link GridCacheProjection#isLockedByThread(Object)} method.
     *
     * @return See {@link GridCacheProjection#isLockedByThread(Object)}.
     */
    public boolean isLockedByThread();

    /**
     * Gets current version of this cache entry.
     *
     * @return Version of this cache entry.
     */
    public Object version();

    /**
     * Gets expiration time for this entry.
     *
     * @return Absolute time when this value expires.
     */
    public long expirationTime();

    /**
     * Gets time to live, i.e. maximum life time, of this entry in milliseconds.
     *
     * @return Time to live value for this entry.
     */
    public long timeToLive();

    /**
     * Sets time to live, i.e. maximum life time, of this entry in milliseconds.
     * Note that this method is transactional - if entry is enlisted into a transaction,
     * then time-to-live will not be set until transaction commit.
     * <p>
     * When called outside the transaction, this method will have no effect until the
     * next update operation.
     *
     * @param ttl Time to live value for this entry.
     */
    public void timeToLive(long ttl);

    /**
     * Gets the flag indicating current node's primary ownership for this entry.
     * <p>
     * Note, that this value is dynamic and may change with grid topology changes.
     *
     * @return {@code True} if current grid node is the primary owner for this entry.
     */
    public boolean primary();

    /**
     * Gets the flag indicating if current node is backup for this entry.
     * <p>
     * Note, that this value is dynamic and may change with grid topology changes.
     *
     * @return {@code True} if current grid node is the backup for this entry.
     */
    public boolean backup();

    /**
     * Gets affinity partition id for this entry.
     *
     * @return Partition id.
     */
    public int partition();

    /**
     * This method has the same semantic as {@link #get()} method, however it
     * wraps {@link GridException} into {@link GridRuntimeException} if failed in order to
     * comply with {@link Entry} interface.
     *
     * @return See {@link #get()}
     */
    @Nullable @Override public V getValue();

    /**
     * This method has the same semantic as
     * {@link GridCacheProjection#get(Object)} method.
     *
     * @return See {@link GridCacheProjection#get(Object)}.
     * @throws GridException See {@link GridCacheProjection#get(Object)}.
     */
    @Nullable public V get() throws GridException;

    /**
     * This method has the same semantic as
     * {@link GridCacheProjection#getAsync(Object)} method.
     *
     * @return See {@link GridCacheProjection#getAsync(Object)}.
     */
    public GridFuture<V> getAsync();

    /**
     * This method has the same semantic as {@link #set(Object, GridPredicate[])} method, however it
     * wraps {@link GridException} into {@link GridRuntimeException} if failed in order to
     * comply with {@link Entry} interface.
     *
     * @return See {@link #set(Object, GridPredicate[])}
     */
    @Nullable @Override public V setValue(V val);

    /**
     * This method has the same semantic as
     * {@link GridCacheProjection#put(Object, Object, GridPredicate[])} method.
     *
     * @param val See {@link GridCacheProjection#put(Object, Object, GridPredicate[])}
     * @param filter See {@link GridCacheProjection#put(Object, Object, GridPredicate[])}.
     * @return See {@link GridCacheProjection#put(Object, Object, GridPredicate[])}.
     * @throws GridException See {@link GridCacheProjection#put(Object, Object, GridPredicate[])}.
     */
    @Nullable public V set(V val, @Nullable GridPredicate<GridCacheEntry<K, V>>... filter) throws GridException;

    /**
     * This method has the same semantic as
     * {@link GridCacheProjection#putAsync(Object, Object, GridPredicate[])} method.
     *
     * @param val See {@link GridCacheProjection#putAsync(Object, Object, GridPredicate[])}
     * @param filter See {@link GridCacheProjection#putAsync(Object, Object, GridPredicate[])}.
     * @return See {@link GridCacheProjection#putAsync(Object, Object, GridPredicate[])}.
     */
    public GridFuture<V> setAsync(V val, @Nullable GridPredicate<GridCacheEntry<K, V>>... filter);

    /**
     * This method has the same semantic as
     * {@link GridCacheProjection#putIfAbsent(Object, Object)} method.
     *
     * @param val See {@link GridCacheProjection#putIfAbsent(Object, Object)}
     * @return See {@link GridCacheProjection#putIfAbsent(Object, Object)}.
     * @throws GridException See {@link GridCacheProjection#putIfAbsent(Object, Object)}.
     */
    @Nullable public V setIfAbsent(V val) throws GridException;

    /**
     * This method has the same semantic as
     * {@link GridCacheProjection#putIfAbsentAsync(Object, Object)} method.
     *
     * @param val See {@link GridCacheProjection#putIfAbsentAsync(Object, Object)}
     * @return See {@link GridCacheProjection#putIfAbsentAsync(Object, Object)}.
     */
    public GridFuture<V> setIfAbsentAsync(V val);

    /**
     * This method has the same semantic as
     * {@link GridCacheProjection#putx(Object, Object, GridPredicate[])} method.
     *
     * @param val See {@link GridCacheProjection#putx(Object, Object, GridPredicate[])}
     * @param filter See {@link GridCacheProjection#putx(Object, Object, GridPredicate[])}.
     * @return See {@link GridCacheProjection#putx(Object, Object, GridPredicate[])}.
     * @throws GridException See {@link GridCacheProjection#putx(Object, Object, GridPredicate[])}.
     */
    public boolean setx(V val, @Nullable GridPredicate<GridCacheEntry<K, V>>... filter)
        throws GridException;

    /**
     * This method has the same semantic as
     * {@link GridCacheProjection#putxAsync(Object, Object, GridPredicate[])} method.
     *
     * @param val See {@link GridCacheProjection#putxAsync(Object, Object, GridPredicate[])}
     * @param filter See {@link GridCacheProjection#putxAsync(Object, Object, GridPredicate[])}.
     * @return See {@link GridCacheProjection#putxAsync(Object, Object, GridPredicate[])}.
     */
    public GridFuture<Boolean> setxAsync(V val,
        @Nullable GridPredicate<GridCacheEntry<K, V>>... filter);

    /**
     * This method has the same semantic as
     * {@link GridCacheProjection#putxIfAbsent(Object, Object)} method.
     *
     * @param val See {@link GridCacheProjection#putxIfAbsent(Object, Object)}
     * @return See {@link GridCacheProjection#putxIfAbsent(Object, Object)}.
     * @throws GridException See {@link GridCacheProjection#putxIfAbsent(Object, Object)}.
     */
    public boolean setxIfAbsent(@Nullable V val) throws GridException;

    /**
     * This method has the same semantic as
     * {@link GridCacheProjection#putxIfAbsentAsync(Object, Object)} method.
     *
     * @param val See {@link GridCacheProjection#putxIfAbsentAsync(Object, Object)}
     * @return See {@link GridCacheProjection#putxIfAbsentAsync(Object, Object)}.
     */
    public GridFuture<Boolean> setxIfAbsentAsync(V val);

    /**
     * This method has the same semantic as
     * {@link GridCacheProjection#transform(Object, GridClosure)} method.
     *
     * @param transformer Closure to be applied to the previous value in cache. If this closure returns
     *      {@code null}, the associated value will be removed from cache.
     * @throws GridException If cache update failed.
     * @see GridCacheProjection#transform(Object, GridClosure)
     */
    public void transform(GridClosure<V, V> transformer) throws GridException;

    /**
     * This method has the same semantic as
     * {@link GridCacheProjection#transformAsync(Object, GridClosure)} method.
     *
     * @param transformer Closure to be applied to the previous value in cache. If this closure returns
     *      {@code null}, the associated value will be removed from cache.
     * @return Transform operation future.
     */
    public GridFuture<?> transformAsync(GridClosure<V, V> transformer);

    /**
     * This method has the same semantic as
     * {@link GridCacheProjection#replace(Object, Object)} method.
     *
     * @param val See {@link GridCacheProjection#replace(Object, Object)}
     * @return See {@link GridCacheProjection#replace(Object, Object)}.
     * @throws GridException See {@link GridCacheProjection#replace(Object, Object)}.
     */
    @Nullable public V replace(V val) throws GridException;

    /**
     * This method has the same semantic as
     * {@link GridCacheProjection#replaceAsync(Object, Object)} method.
     *
     * @param val See {@link GridCacheProjection#replaceAsync(Object, Object)}
     * @return See {@link GridCacheProjection#replaceAsync(Object, Object)}.
     */
    public GridFuture<V> replaceAsync(V val);

    /**
     * This method has the same semantic as
     * {@link GridCacheProjection#replacex(Object, Object)} method.
     *
     * @param val See {@link GridCacheProjection#replacex(Object, Object)}
     * @return See {@link GridCacheProjection#replacex(Object, Object)}.
     * @throws GridException See {@link GridCacheProjection#replacex(Object, Object)}.
     */
    public boolean replacex(V val) throws GridException;

    /**
     * This method has the same semantic as
     * {@link GridCacheProjection#replacexAsync(Object, Object)} method.
     *
     * @param val See {@link GridCacheProjection#replacexAsync(Object, Object)}
     * @return See {@link GridCacheProjection#replacexAsync(Object, Object)}.
     */
    public GridFuture<Boolean> replacexAsync(V val);

    /**
     * This method has the same semantic as
     * {@link GridCacheProjection#replace(Object, Object, Object)} method.
     *
     * @param oldVal See {@link GridCacheProjection#replace(Object, Object, Object)}
     * @param newVal See {@link GridCacheProjection#replace(Object, Object, Object)}
     * @return See {@link GridCacheProjection#replace(Object, Object)}.
     * @throws GridException See {@link GridCacheProjection#replace(Object, Object)}.
     */
    public boolean replace(V oldVal, V newVal) throws GridException;

    /**
     * This method has the same semantic as
     * {@link GridCacheProjection#replaceAsync(Object, Object, Object)} method.
     *
     * @param oldVal See {@link GridCacheProjection#replaceAsync(Object, Object, Object)}
     * @param newVal See {@link GridCacheProjection#replaceAsync(Object, Object, Object)}
     * @return See {@link GridCacheProjection#replaceAsync(Object, Object)}.
     */
    public GridFuture<Boolean> replaceAsync(V oldVal, V newVal);

    /**
     * This method has the same semantic as
     * {@link GridCacheProjection#remove(Object, GridPredicate[])} method.
     *
     * @param filter See {@link GridCacheProjection#remove(Object, GridPredicate[])}.
     * @return See {@link GridCacheProjection#remove(Object, GridPredicate[])}.
     * @throws GridException See {@link GridCacheProjection#remove(Object, GridPredicate[])}.
     */
    @Nullable public V remove(@Nullable GridPredicate<GridCacheEntry<K, V>>... filter) throws GridException;

    /**
     * This method has the same semantic as
     * {@link GridCacheProjection#removeAsync(Object, GridPredicate[])} method.
     *
     * @param filter See {@link GridCacheProjection#removeAsync(Object, GridPredicate[])}.
     * @return See {@link GridCacheProjection#removeAsync(Object, GridPredicate[])}.
     */
    public GridFuture<V> removeAsync(@Nullable GridPredicate<GridCacheEntry<K, V>>... filter);

    /**
     * This method has the same semantic as
     * {@link GridCacheProjection#removex(Object, GridPredicate[])} method.
     *
     * @param filter See {@link GridCacheProjection#removex(Object, GridPredicate[])}.
     * @return See {@link GridCacheProjection#removex(Object, GridPredicate[])}.
     * @throws GridException See {@link GridCacheProjection#removex(Object, GridPredicate[])}.
     */
    public boolean removex(@Nullable GridPredicate<GridCacheEntry<K, V>>... filter) throws GridException;

    /**
     * This method has the same semantic as
     * {@link GridCacheProjection#removexAsync(Object, GridPredicate[])} method.
     *
     * @param filter See {@link GridCacheProjection#removexAsync(Object, GridPredicate[])}.
     * @return See {@link GridCacheProjection#removexAsync(Object, GridPredicate[])}.
     */
    public GridFuture<Boolean> removexAsync(@Nullable GridPredicate<GridCacheEntry<K, V>>... filter);

    /**
     * This method has the same semantic as
     * {@link GridCacheProjection#remove(Object, Object)} method.
     *
     * @param val See {@link GridCacheProjection#remove(Object, Object)}.
     * @return See {@link GridCacheProjection#remove(Object, Object)}.
     * @throws GridException See {@link GridCacheProjection#remove(Object, Object)}.
     */
    public boolean remove(V val) throws GridException;

    /**
     * This method has the same semantic as
     * {@link GridCacheProjection#removeAsync(Object, Object)} method.
     *
     * @param val See {@link GridCacheProjection#removeAsync(Object, Object)}.
     * @return See {@link GridCacheProjection#removeAsync(Object, Object)}.
     */
    public GridFuture<Boolean> removeAsync(V val);

    /**
     * This method has the same semantic as
     * {@link GridCacheProjection#evict(Object)} method.
     *
     * @return See {@link GridCacheProjection#evict(Object)}.
     */
    public boolean evict();

    /**
     * This method has the same semantic as
     * {@link GridCacheProjection#clear(Object)} method.
     *
     * @return See {@link GridCacheProjection#clear(Object)}.
     */
    public boolean clear();

    /**
     * Optimizes the size of this entry. If entry is expired at the time
     * of the call then entry is removed locally.
     *
     * @throws GridException If failed to compact.
     * @return {@code true} if entry was cleared from cache (if value was {@code null}).
     */
    public boolean compact() throws GridException;

    /**
     * Synchronously acquires lock on a cached object associated with this entry
     * only if the passed in filter (if any) passes. This method together with
     * filter check will be executed as one atomic operation.
     * <h2 class="header">Transactions</h2>
     * Locks are not transactional and should not be used from within transactions.
     * If you do need explicit locking within transaction, then you should use
     * {@link GridCacheTxConcurrency#PESSIMISTIC} concurrency control for transaction
     * which will acquire explicit locks for relevant cache operations.
     * <h2 class="header">Cache Flags</h2>
     * This method is not available if any of the following flags are set on projection:
     * {@link GridCacheFlag#LOCAL}, {@link GridCacheFlag#READ}.
     *
     * @param timeout Timeout in milliseconds to wait for lock to be acquired
     *      ({@code '0'} for no expiration).
     * @param filter Optional filter to validate prior to acquiring the lock.
     * @return {@code True} if all filters passed and lock was acquired,
     *      {@code false} otherwise.
     * @throws GridException If lock acquisition resulted in error.
     * @throws GridCacheFlagException If flags validation failed.
     */
    public boolean lock(long timeout, @Nullable GridPredicate<GridCacheEntry<K, V>>... filter)
        throws GridException;

    /**
     * Asynchronously acquires lock on a cached object associated with this entry
     * only if the passed in filter (if any) passes. This method together with
     * filter check will be executed as one atomic operation.
     * <h2 class="header">Transactions</h2>
     * Locks are not transactional and should not be used from within transactions. If you do
     * need explicit locking within transaction, then you should use
     * {@link GridCacheTxConcurrency#PESSIMISTIC} concurrency control for transaction
     * which will acquire explicit locks for relevant cache operations.
     * <h2 class="header">Cache Flags</h2>
     * This method is not available if any of the following flags are set on projection:
     * {@link GridCacheFlag#LOCAL}, {@link GridCacheFlag#READ}.
     *
     * @param timeout Timeout in milliseconds to wait for lock to be acquired
     *      ({@code '0'} for no expiration).
     * @param filter Optional filter to validate prior to acquiring the lock.
     * @return Future for the lock operation. The future will return {@code true}
     *      whenever all filters pass and locks are acquired before timeout is expired,
     *      {@code false} otherwise.
     * @throws GridCacheFlagException If flags validation failed.
     */
    public GridFuture<Boolean> lockAsync(long timeout,
        @Nullable GridPredicate<GridCacheEntry<K, V>>... filter);

    /**
     * Unlocks this entry only if current thread owns the lock. If optional filter
     * will not pass, then unlock will not happen. If this entry was never locked by
     * current thread, then this method will do nothing.
     * <h2 class="header">Transactions</h2>
     * Locks are not transactional and should not be used from within transactions. If you do
     * need explicit locking within transaction, then you should use
     * {@link GridCacheTxConcurrency#PESSIMISTIC} concurrency control for transaction
     * which will acquire explicit locks for relevant cache operations.
     * <h2 class="header">Cache Flags</h2>
     * This method is not available if any of the following flags are set on projection:
     * {@link GridCacheFlag#LOCAL}, {@link GridCacheFlag#READ}.
     *
     * @param filter Optional filter that needs to pass prior to unlock taking effect.
     * @throws GridException If unlock execution resulted in error.
     * @throws GridCacheFlagException If flags validation failed.
     */
    public void unlock(GridPredicate<GridCacheEntry<K, V>>... filter) throws GridException;

    /**
     * Checks whether entry is currently present in cache or not. If entry is not in
     * cache (e.g. has been removed) {@code false} is returned. In this case all
     * operations on this entry will cause creation of a new entry in cache.
     *
     * @return {@code True} if entry is in cache, {@code false} otherwise.
     */
    public boolean isCached();

    /**
     * Gets size of serialized key and value in addition to any overhead added by {@code GridGain} itself.
     *
     * @return size in bytes.
     * @throws GridException If failed to evaluate entry size.
     */
    public int memorySize() throws GridException;
}
