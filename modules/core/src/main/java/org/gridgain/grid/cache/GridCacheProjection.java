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
import org.gridgain.grid.cache.affinity.*;
import org.gridgain.grid.cache.query.*;
import org.gridgain.grid.cache.store.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.portables.*;
import org.jetbrains.annotations.*;

import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.concurrent.*;

/**
 * This interface provides a rich API for working with distributed caches. It includes the following
 * main functionality:
 * <ul>
 * <li>
 *  Various {@code 'get(..)'} methods to synchronously or asynchronously get values from cache.
 *  All {@code 'get(..)'} methods are transactional and will participate in an ongoing transaction
 *  if there is one.
 * </li>
 * <li>
 *  Various {@code 'put(..)'}, {@code 'putIfAbsent(..)'}, and {@code 'replace(..)'} methods to
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
 *  Various {@code 'contains(..)'} method to check if cache contains certain keys or values locally.
 * </li>
 * <li>
 *  Various {@code 'forEach(..)'}, {@code 'forAny(..)'}, and {@code 'reduce(..)'} methods to visit
 *  every local cache entry within this projection.
 * </li>
 * <li>
 *  Various {@code flagsOn(..)'}, {@code 'flagsOff(..)'}, and {@code 'projection(..)'} methods to
 *  set specific flags and filters on a cache projection.
 * </li>
 * <li>
 *  Methods like {@code 'keySet(..)'}, {@code 'values(..)'}, and {@code 'entrySet(..)'} to provide
 *  views on cache keys, values, and entries.
 * </li>
 * <li>
 *  Various {@code 'peek(..)'} methods to peek at values in global or transactional memory, swap
 *  storage, or persistent storage.
 * </li>
 * <li>
 *  Various {@code 'reload(..)'} methods to reload latest values from persistent storage.
 * </li>
 * <li>
 *  Various {@code 'promote(..)'} methods to load specified keys from swap storage into
 *  global cache memory.
 * </li>
 * <li>
 *  Various {@code 'lock(..)'}, {@code 'unlock(..)'}, and {@code 'isLocked(..)'} methods to acquire, release,
 *  and check on distributed locks on a single or multiple keys in cache. All locking methods
 *  are not transactional and will not enlist keys into ongoing transaction, if any.
 * </li>
 * <li>
 *  Various {@code 'clear(..)'} methods to clear elements from cache, and optionally from
 *  swap storage. All {@code 'clear(..)'} methods are not transactional and will not enlist cleared
 *  keys into ongoing transaction, if any.
 * </li>
 * <li>
 *  Various {@code 'evict(..)'} methods to evict elements from cache, and optionally store
 *  them in underlying swap storage for later access. All {@code 'evict(..)'} methods are not
 *  transactional and will not enlist evicted keys into ongoing transaction, if any.
 * </li>
 * <li>
 *  Various {@code 'txStart(..)'} methods to perform various cache
 *  operations within a transaction (see {@link GridCacheTx} for more information).
 * </li>
 * <li>
 *  {@link #queries()} method to get an instance of {@link GridCacheQueries} service for working
 *  with distributed cache queries.
 * </li>
 * <li>
 *  Various {@code 'gridProjection(..)'} methods which provide {@link GridProjection} only
 *  for nodes on which given keys reside. All {@code 'gridProjection(..)'} methods are not
 *  transactional and will not enlist keys into ongoing transaction.
 * </li>
 * <li>Method {@link GridCache#toMap()} to convert this interface into standard Java {@link ConcurrentMap} interface.
 * </ul>
 * <h1 class="header">Extended Put And Remove Methods</h1>
 * All methods that end with {@code 'x'} provide the same functionality as their sibling
 * methods that don't end with {@code 'x'}, however instead of returning a previous value they
 * return a {@code boolean} flag indicating whether operation succeeded or not. Returning
 * a previous value may involve a network trip or a persistent store lookup and should be
 * avoided whenever not needed.
 * <h1 class="header">Predicate Filters</h1>
 * All filters passed into methods on this API are checked <b>atomically</b>. In other words the
 * value returned by the methods is guaranteed to be consistent with the filters passed in. Note
 * that filters are optional, and if not passed in, then methods will still work as is without
 * filter validation.
 * <h1 class="header">Transactions</h1>
 * Cache API supports distributed transactions. All {@code 'get(..)'}, {@code 'put(..)'}, {@code 'replace(..)'},
 * and {@code 'remove(..)'} operations are transactional and will participate in an ongoing transaction,
 * if any. Other methods like {@code 'peek(..)'} or various {@code 'contains(..)'} methods may
 * be transaction-aware, i.e. check in-transaction entries first, but will not affect the current
 * state of transaction. See {@link GridCacheTx} documentation for more information
 * about transactions.
 * <h1 class="header">Group Locking</h1>
 * <i>Group Locking</i> is a feature where instead of acquiring individual locks, GridGain will lock
 * multiple keys with one lock to save on locking overhead. There are 2 types of <i>Group Locking</i>:
 * <i>affinity-based</i>, and <i>partitioned-based</i>.
 * <p>
 * With {@code affinity-based-group-locking} the keys are grouped by <i>affinity-key</i>. This means that
 * only keys with identical affinity-key (see {@link GridCacheAffinityKeyMapped}) can participate in the
 * transaction, and only one lock on the <i>affinity-key</i> will be acquired for the whole transaction.
 * {@code Affinity-group-locked} transactions are started via
 * {@link #txStartAffinity(Object, GridCacheTxConcurrency, GridCacheTxIsolation, long, int)} method.
 * <p>
 * With {@code partition-based-group-locking} the keys are grouped by partition ID. This means that
 * only keys belonging to identical partition (see {@link GridCacheAffinity#partition(Object)}) can participate in the
 * transaction, and only one lock on the whole partition will be acquired for the whole transaction.
 * {@code Partition-group-locked} transactions are started via
 * {@link #txStartPartition(int, GridCacheTxConcurrency, GridCacheTxIsolation, long, int)} method.
 * <p>
 * <i>Group locking</i> should always be used for transactions whenever possible. If your requirements fit either
 * <i>affinity-based</i> or <i>partition-based</i> scenarios outlined above then <i>group-locking</i>
 * can significantly improve performance of your application, often by an order of magnitude.
 * <h1 class="header">Null Keys or Values</h1>
 * Neither {@code null} keys or values are allowed to be stored in cache. If a {@code null} value
 * happens to be in cache (e.g. after invalidation or remove), then cache will treat this case
 * as there is no value at all.
 * <h1 class="header">Peer Class Loading</h1>
 * If peer-class-loading is enabled, all classes passed into cache API will be automatically deployed
 * to any participating grid nodes. However, in case of redeployment, caches will be cleared and
 * all entries will be removed. This behavior is useful during development, but should not be
 * used in production.
 * <h1 class="header">Portable Objects</h1>
 * If an object is defined as portable GridGain cache will automatically store it in portable (i.e. binary)
 * format. User can choose to work either with the portable format or with the deserialized form (assuming
 * that class definitions are present in the classpath). By default, cache works with deserialized form
 * (example shows the case when {@link Integer} is used as a key for a portable object):
 * <pre>
 * GridCacheProjection<Integer, Value> prj = GridGain.grid().cache(null);
 *
 * // Value will be serialized and stored in cache in portable format.
 * prj.put(1, new Value());
 *
 * // Value will be deserialized since it's stored in portable format.
 * Value val = prj.get(1);
 * </pre>
 * You won't be able to work with deserialized form if class definition for the {@code Value} is not on
 * classpath. Even if you have the class definition, you should always avoid full deserialization if it's not
 * needed for performance reasons. To work with portable format directly you should create special projection
 * using {@link #keepPortable()} method:
 * <pre>
 * GridCacheProjection<Integer, GridPortableObject> prj = GridGain.grid().cache(null).keepPortable();
 *
 * // Value is not deserialized and returned in portable format.
 * GridPortableObject po = prj.get(1);
 * </pre>
 * See {@link #keepPortable()} method JavaDoc for more details.
 */
public interface GridCacheProjection<K, V> extends Iterable<GridCacheEntry<K, V>> {
    /**
     * Gets name of this cache ({@code null} for default cache).
     *
     * @return Cache name.
     */
    public String name();

    /**
     * Gets grid projection for this cache. This projection includes all nodes which have this cache configured.
     *
     * @return Projection instance.
     */
    public GridProjection gridProjection();

    /**
     * Gets base cache for this projection.
     *
     * @param <K1> Cache key type.
     * @param <V1> Cache value type.
     * @return Base cache for this projection.
     */
    @SuppressWarnings({"ClassReferencesSubclass"})
    public <K1, V1> GridCache<K1, V1> cache();

    /**
     * Gets cache flags enabled on this projection.
     *
     * @return Flags for this projection (empty set if no flags have been set).
     */
    public Set<GridCacheFlag> flags();

    /**
     * Returns queries facade responsible for creating various SQL, TEXT, or SCAN queries.
     *
     * @return Queries facade responsible for creating various SQL, TEXT, or SCAN queries.
     */
    public GridCacheQueries<K, V> queries();

    /**
     * Gets cache projection only for given key and value type. Only {@code non-null} key-value
     * pairs that have matching key and value pairs will be used in this projection.
     *
     * @param keyType Key type.
     * @param valType Value type.
     * @param <K1> Key type.
     * @param <V1> Value type.
     * @return Cache projection for given key and value types.
     */
    public <K1, V1> GridCacheProjection<K1, V1> projection(Class<? super K1> keyType, Class<? super V1> valType);

    /**
     * Gets cache projection based on given key-value predicate. Whenever makes sense,
     * this predicate will be used to pre-filter cache operations. If
     * operation passed pre-filtering, this filter will be passed through
     * to cache operations as well.
     * <p>
     * For example, for {@link #putAll(Map, GridPredicate[])} method only
     * elements that pass the filter will be given to {@code GridCache.putAll(m, filter)}
     * where it will be checked once again prior to put.
     *
     * @param p Key-value predicate for this projection. If {@code null}, then the
     *      same projection is returned.
     * @return Projection for given key-value predicate.
     */
    public GridCacheProjection<K, V> projection(@Nullable GridBiPredicate<K, V> p);

    /**
     * Gets cache projection based on given entry filter. This filter will be simply passed through
     * to all cache operations on this projection. Unlike {@link #projection(GridBiPredicate)}
     * method, this filter will <b>not</b> be used for pre-filtering.
     *
     * @param filter Filter to be passed through to all cache operations. If {@code null}, then the
     *      same projection is returned.  If cache operation receives its own filter, then filters
     *      will be {@code 'anded'}.
     * @return Projection based on given filter.
     */
    public GridCacheProjection<K, V> projection(@Nullable GridPredicate<GridCacheEntry<K, V>> filter);

    /**
     * Gets cache projection base on this one, but with the specified flags turned on.
     * <h1 class="header">Cache Flags</h1>
     * The resulting projection will inherit all the flags from this projection.
     *
     * @param flags Flags to turn on (if empty, then no-op).
     * @return New projection based on this one, but with the specified flags turned on.
     */
    public GridCacheProjection<K, V> flagsOn(@Nullable GridCacheFlag... flags);

    /**
     * Gets cache projection base on this but with the specified flags turned off.
     * <h1 class="header">Cache Flags</h1>
     * The resulting projection will inherit all the flags from this projection except for
     * the ones that were turned off.
     *
     * @param flags Flags to turn off (if empty, then all flags will be turned off).
     * @return New projection based on this one, but with the specified flags turned off.
     */
    public GridCacheProjection<K, V> flagsOff(@Nullable GridCacheFlag... flags);

    /**
     * Creates projection that will operate with portable objects.
     * <p>
     * Projection returned by this method will force cache not to deserialize portable objects,
     * so keys and values will be returned from cache API methods without changes. Therefore,
     * signature of the projection can contain only following types:
     * <ul>
     *     <li>{@link GridPortableObject} for portable classes</li>
     *     <li>All primitives (byte, int, ...) and there boxed versions (Byte, Integer, ...)</li>
     *     <li>Arrays of primitives (byte[], int[], ...)</li>
     *     <li>{@link String} and array of {@link String}s</li>
     *     <li>{@link UUID} and array of {@link UUID}s</li>
     *     <li>{@link Date} and array of {@link Date}s</li>
     *     <li>{@link Timestamp} and array of {@link Timestamp}s</li>
     *     <li>Enums and array of enums</li>
     *     <li>
     *         Maps, collections and array of objects (but objects inside
     *         them will still be converted if they are portable)
     *     </li>
     * </ul>
     * <p>
     * For example, if you use {@link Integer} as a key and {@code Value} class as a value
     * (which will be stored in portable format), you should acquire following projection
     * to avoid deserialization:
     * <pre>
     * GridCacheProjection<Integer, GridPortableObject> prj = cache.keepPortable();
     *
     * // Value is not deserialized and returned in portable format.
     * GridPortableObject po = prj.get(1);
     * </pre>
     * <p>
     * Note that this method makes sense only if cache is working in portable mode
     * ({@link GridCacheConfiguration#isPortableEnabled()} returns {@code true}. If not,
     * this method is no-op and will return current projection.
     *
     * @return Projection for portable objects.
     */
    public <K1, V1> GridCacheProjection<K1, V1> keepPortable();

    /**
     * Returns {@code true} if this map contains no key-value mappings.
     *
     * @return {@code true} if this map contains no key-value mappings.
     */
    public boolean isEmpty();

    /**
     * Converts this API into standard Java {@link ConcurrentMap} interface.
     *
     * @return {@link ConcurrentMap} representation of given cache projection.
     */
    public ConcurrentMap<K, V> toMap();

    /**
     * Returns {@code true} if this cache contains a mapping for the specified
     * key.
     *
     * @param key key whose presence in this map is to be tested.
     * @return {@code true} if this map contains a mapping for the specified key.
     * @throws NullPointerException if the key is {@code null}.
     */
    public boolean containsKey(K key);

    /**
     * Returns {@code true} if this cache contains given value.
     *
     * @param val Value to check.
     * @return {@code True} if given value is present in cache.
     * @throws NullPointerException if the value is {@code null}.
     */
    public boolean containsValue(V val);

    /**
     * Executes visitor closure on each cache element.
     * <h2 class="header">Transactions</h2>
     * This method is not transactional and will not enlist keys into transaction simply
     * because they were visited. However, if you perform transactional operations on the
     * visited entries, those operations will enlist the entry into transaction.
     *
     * @param vis Closure which will be invoked for each cache entry.
     */
    public void forEach(GridInClosure<GridCacheEntry<K, V>> vis);

    /**
     * Tests whether the predicate holds for all entries. If cache is empty,
     * then {@code true} is returned.
     * <h2 class="header">Transactions</h2>
     * This method is not transactional and will not enlist keys into transaction simply
     * because they were visited. However, if you perform transactional operations on the
     * visited entries, those operations will enlist the entry into transaction.
     *
     * @param vis Predicate to test for each cache entry.
     * @return {@code True} if the given predicate holds for all visited entries, {@code false} otherwise.
     */
    public boolean forAll(GridPredicate<GridCacheEntry<K, V>> vis);

    /**
     * Reloads a single key from persistent storage. This method
     * delegates to {@link GridCacheStore#load(GridCacheTx, Object)}
     * method.
     * <h2 class="header">Transactions</h2>
     * This method does not participate in transactions, however it does not violate
     * cache integrity and can be used safely with or without transactions.
     *
     * @param key Key to reload.
     * @return Reloaded value or current value if entry was updated while reloading.
     * @throws GridException If reloading failed.
     */
    @Nullable public V reload(K key) throws GridException;

    /**
     * Asynchronously reloads a single key from persistent storage. This method
     * delegates to {@link GridCacheStore#load(GridCacheTx, Object)}
     * method.
     * <h2 class="header">Transactions</h2>
     * This method does not participate in transactions, however it does not violate
     * cache integrity and can be used safely with or without transactions.
     *
     * @param key Key to reload.
     * @return Future to be completed whenever the entry is reloaded.
     */
    public GridFuture<V> reloadAsync(K key);

    /**
     * Reloads all currently cached keys form persistent storage.
     * <h2 class="header">Transactions</h2>
     * This method does not participate in transactions, however it does not violate
     * cache integrity and can be used safely with or without transactions.
     *
     * @throws GridException If reloading failed.
     */
    public void reloadAll() throws GridException;

    /**
     * Asynchronously reloads all specified entries from underlying
     * persistent storage.
     * <h2 class="header">Transactions</h2>
     * This method does not participate in transactions, however it does not violate
     * cache integrity and can be used safely with or without transactions.
     *
     * @return Future which will complete whenever {@code reload} completes.
     */
    public GridFuture<?> reloadAllAsync();

    /**
     * Reloads specified entries from underlying persistent storage.
     * <h2 class="header">Transactions</h2>
     * This method does not participate in transactions, however it does not violate
     * cache integrity and can be used safely with or without transactions.
     *
     * @param keys Keys to reload.
     * @throws GridException if reloading failed.
     */
    public void reloadAll(@Nullable Collection<? extends K> keys) throws GridException;

    /**
     * Asynchronously reloads all specified entries from underlying
     * persistent storage.
     * <h2 class="header">Transactions</h2>
     * This method does not participate in transactions, however it does not violate
     * cache integrity and can be used safely with or without transactions.
     *
     * @param keys Keys to reload.
     * @return Future which will complete whenever {@code reload} completes.
     */
    public GridFuture<?> reloadAllAsync(@Nullable Collection<? extends K> keys);

    /**
     * Peeks at in-memory cached value using default {@link GridCachePeekMode#SMART}
     * peek mode.
     * <p>
     * This method will not load value from any persistent store or from a remote node.
     * <h2 class="header">Transactions</h2>
     * This method does not participate in any transactions, however, it will
     * peek at transactional value according to the {@link GridCachePeekMode#SMART} mode
     * semantics. If you need to look at global cached value even from within transaction,
     * you can use {@link GridCache#peek(Object, Collection)} method.
     *
     * @param key Entry key.
     * @return Peeked value.
     * @throws NullPointerException If key is {@code null}.
     */
    @Nullable public V peek(K key);

    /**
     * Peeks at cached value using optional set of peek modes. This method will sequentially
     * iterate over given peek modes in the order passed in, and try to peek at value using
     * each peek mode. Once a {@code non-null} value is found, it will be immediately returned.
     * <p>
     * Note that if modes are not provided this method works exactly the same way as
     * {@link #peek(Object)}, implicitly using {@link GridCachePeekMode#SMART} mode.
     * <h2 class="header">Transactions</h2>
     * This method does not participate in any transactions, however, it may
     * peek at transactional value depending on the peek modes used.
     *
     * @param key Entry key.
     * @param modes Optional set of peek modes.
     * @return Peeked value.
     * @throws GridException If peek operation failed.
     * @throws NullPointerException If key is {@code null}.
     */
    @Nullable public V peek(K key, @Nullable Collection<GridCachePeekMode> modes) throws GridException;

    /**
     * Retrieves value mapped to the specified key from cache. Value will only be returned if
     * its entry passed the optional filter provided. Filter check is atomic, and therefore the
     * returned value is guaranteed to be consistent with the filter. The return value of {@code null}
     * means entry did not pass the provided filter or cache has no mapping for the
     * key.
     * <p>
     * If the value is not present in cache, then it will be looked up from swap storage. If
     * it's not present in swap, or if swap is disable, and if read-through is allowed, value
     * will be loaded from {@link GridCacheStore} persistent storage via
     * {@link GridCacheStore#load(GridCacheTx, Object)} method.
     * <h2 class="header">Transactions</h2>
     * This method is transactional and will enlist the entry into ongoing transaction
     * if there is one.
     * <h2 class="header">Cache Flags</h2>
     * This method is not available if {@link GridCacheFlag#LOCAL} flag is set on projection.
     *
     * @param key Key to retrieve the value for.
     * @return Value for the given key.
     * @throws GridException If get operation failed.
     * @throws GridCacheFlagException If failed projection flags validation.
     * @throws NullPointerException if the key is {@code null}.
     */
    @Nullable public V get(K key) throws GridException;

    /**
     * Asynchronously retrieves value mapped to the specified key from cache. Value will only be returned if
     * its entry passed the optional filter provided. Filter check is atomic, and therefore the
     * returned value is guaranteed to be consistent with the filter. The return value of {@code null}
     * means entry did not pass the provided filter or cache has no mapping for the
     * key.
     * <p>
     * If the value is not present in cache, then it will be looked up from swap storage. If
     * it's not present in swap, or if swap is disabled, and if read-through is allowed, value
     * will be loaded from {@link GridCacheStore} persistent storage via
     * {@link GridCacheStore#load(GridCacheTx, Object)} method.
     * <h2 class="header">Transactions</h2>
     * This method is transactional and will enlist the entry into ongoing transaction
     * if there is one.
     * <h2 class="header">Cache Flags</h2>
     * This method is not available if {@link GridCacheFlag#LOCAL} flag is set on projection.
     *
     * @param key Key for the value to get.
     * @return Future for the get operation.
     * @throws NullPointerException if the key is {@code null}.
     * @throws GridCacheFlagException If projection flags validation failed.
     */
    public GridFuture<V> getAsync(K key);

    /**
     * Retrieves values mapped to the specified keys from cache. Value will only be returned if
     * its entry passed the optional filter provided. Filter check is atomic, and therefore the
     * returned value is guaranteed to be consistent with the filter. If requested key-value pair
     * is not present in the returned map, then it means that its entry did not pass the provided
     * filter or cache has no mapping for the key.
     * <p>
     * If some value is not present in cache, then it will be looked up from swap storage. If
     * it's not present in swap, or if swap is disabled, and if read-through is allowed, value
     * will be loaded from {@link GridCacheStore} persistent storage via
     * {@link GridCacheStore#loadAll(GridCacheTx, Collection, GridBiInClosure)} method.
     * <h2 class="header">Transactions</h2>
     * This method is transactional and will enlist the entry into ongoing transaction
     * if there is one.
     * <h2 class="header">Cache Flags</h2>
     * This method is not available if {@link GridCacheFlag#LOCAL} flag is set on projection.
     *
     * @param keys Keys to get.
     * @return Map of key-value pairs.
     * @throws GridException If get operation failed.
     * @throws GridCacheFlagException If failed projection flags validation.
     */
    public Map<K, V> getAll(@Nullable Collection<? extends K> keys) throws GridException;

    /**
     * Asynchronously retrieves values mapped to the specified keys from cache. Value will only be returned if
     * its entry passed the optional filter provided. Filter check is atomic, and therefore the
     * returned value is guaranteed to be consistent with the filter. If requested key-value pair
     * is not present in the returned map, then it means that its entry did not pass the provided
     * filter or cache has no mapping for the key.
     * <p>
     * If some value is not present in cache, then it will be looked up from swap storage. If
     * it's not present in swap, or if swap is disabled, and if read-through is allowed, value
     * will be loaded from {@link GridCacheStore} persistent storage via
     * {@link GridCacheStore#loadAll(GridCacheTx, Collection, GridBiInClosure)} method.
     * <h2 class="header">Transactions</h2>
     * This method is transactional and will enlist the entry into ongoing transaction
     * if there is one.
     * <h2 class="header">Cache Flags</h2>
     * This method is not available if {@link GridCacheFlag#LOCAL} flag is set on projection.
     *
     * @param keys Key for the value to get.
     * @return Future for the get operation.
     * @throws GridCacheFlagException If projection flags validation failed.
     */
    public GridFuture<Map<K, V>> getAllAsync(@Nullable Collection<? extends K> keys);

    /**
     * Stores given key-value pair in cache. If filters are provided, then entries will
     * be stored in cache only if they pass the filter. Note that filter check is atomic,
     * so value stored in cache is guaranteed to be consistent with the filters. If cache
     * previously contained value for the given key, then this value is returned.
     * In case of {@link GridCacheMode#PARTITIONED} or {@link GridCacheMode#REPLICATED} caches,
     * the value will be loaded from the primary node, which in its turn may load the value
     * from the swap storage, and consecutively, if it's not in swap,
     * from the underlying persistent storage. If value has to be loaded from persistent
     * storage,  {@link GridCacheStore#load(GridCacheTx, Object)} method will be used.
     * <p>
     * If the returned value is not needed, method {@link #putx(Object, Object, GridPredicate[])} should
     * always be used instead of this one to avoid the overhead associated with returning of the previous value.
     * <p>
     * If write-through is enabled, the stored value will be persisted to {@link GridCacheStore}
     * via {@link GridCacheStore#put(GridCacheTx, Object, Object)} method.
     * <h2 class="header">Transactions</h2>
     * This method is transactional and will enlist the entry into ongoing transaction
     * if there is one.
     * <h2 class="header">Cache Flags</h2>
     * This method is not available if any of the following flags are set on projection:
     * {@link GridCacheFlag#LOCAL}, {@link GridCacheFlag#READ}.
     *
     * @param key Key to store in cache.
     * @param val Value to be associated with the given key.
     * @param filter Optional filter to check prior to putting value in cache. Note
     *      that filter check is atomic with put operation.
     * @return Previous value associated with specified key, or {@code null}
     *  if entry did not pass the filter, or if there was no mapping for the key in swap
     *  or in persistent storage.
     * @throws NullPointerException If either key or value are {@code null}.
     * @throws GridException If put operation failed.
     * @throws GridCacheFlagException If projection flags validation failed.
     */
    @Nullable public V put(K key, V val, @Nullable GridPredicate<GridCacheEntry<K, V>>... filter)
        throws GridException;

    /**
     * Asynchronously stores given key-value pair in cache. If filters are provided, then entries will
     * be stored in cache only if they pass the filter. Note that filter check is atomic,
     * so value stored in cache is guaranteed to be consistent with the filters. If cache
     * previously contained value for the given key, then this value is returned. Otherwise,
     * in case of {@link GridCacheMode#REPLICATED} caches, the value will be loaded from swap
     * and, if it's not there, and read-through is allowed, from the underlying
     * {@link GridCacheStore} storage. In case of {@link GridCacheMode#PARTITIONED} caches,
     * the value will be loaded from the primary node, which in its turn may load the value
     * from the swap storage, and consecutively, if it's not in swap and read-through is allowed,
     * from the underlying persistent storage. If value has to be loaded from persistent
     * storage,  {@link GridCacheStore#load(GridCacheTx, Object)} method will be used.
     * <p>
     * If the returned value is not needed, method {@link #putx(Object, Object, GridPredicate[])} should
     * always be used instead of this one to avoid the overhead associated with returning of the previous value.
     * <p>
     * If write-through is enabled, the stored value will be persisted to {@link GridCacheStore}
     * via {@link GridCacheStore#put(GridCacheTx, Object, Object)} method.
     * <h2 class="header">Transactions</h2>
     * This method is transactional and will enlist the entry into ongoing transaction
     * if there is one.
     * <h2 class="header">Cache Flags</h2>
     * This method is not available if any of the following flags are set on projection:
     * {@link GridCacheFlag#LOCAL}, {@link GridCacheFlag#READ}.
     *
     * @param key Key to store in cache.
     * @param val Value to be associated with the given key.
     * @param filter Optional filter to check prior to putting value in cache. Note
     *      that filter check is atomic with put operation.
     * @return Future for the put operation.
     * @throws NullPointerException If either key or value are {@code null}.
     * @throws GridCacheFlagException If projection flags validation failed.
     */
    public GridFuture<V> putAsync(K key, V val, @Nullable GridPredicate<GridCacheEntry<K, V>>... filter);

    /**
     * Stores given key-value pair in cache. If filters are provided, then entries will
     * be stored in cache only if they pass the filter. Note that filter check is atomic,
     * so value stored in cache is guaranteed to be consistent with the filters.
     * <p>
     * This method will return {@code true} if value is stored in cache and {@code false} otherwise.
     * Unlike {@link #put(Object, Object, GridPredicate[])} method, it does not return previous
     * value and, therefore, does not have any overhead associated with returning a value. It
     * should be used whenever return value is not required.
     * <p>
     * If write-through is enabled, the stored value will be persisted to {@link GridCacheStore}
     * via {@link GridCacheStore#put(GridCacheTx, Object, Object)} method.
     * <h2 class="header">Transactions</h2>
     * This method is transactional and will enlist the entry into ongoing transaction
     * if there is one.
     * <h2 class="header">Cache Flags</h2>
     * This method is not available if any of the following flags are set on projection:
     * {@link GridCacheFlag#LOCAL}, {@link GridCacheFlag#READ}.
     *
     * @param key Key to store in cache.
     * @param val Value to be associated with the given key.
     * @param filter Optional filter to check prior to putting value in cache. Note
     *      that filter check is atomic with put operation.
     * @return {@code True} if optional filter passed and value was stored in cache,
     *      {@code false} otherwise. Note that this method will return {@code true} if filter is not
     *      specified.
     * @throws NullPointerException If either key or value are {@code null}.
     * @throws GridException If put operation failed.
     * @throws GridCacheFlagException If projection flags validation failed.
     */
    public boolean putx(K key, V val, @Nullable GridPredicate<GridCacheEntry<K, V>>... filter)
        throws GridException;

    /**
     * Stores given key-value pair in cache. If filters are provided, then entries will
     * be stored in cache only if they pass the filter. Note that filter check is atomic,
     * so value stored in cache is guaranteed to be consistent with the filters.
     * <p>
     * This method will return {@code true} if value is stored in cache and {@code false} otherwise.
     * Unlike {@link #put(Object, Object, GridPredicate[])} method, it does not return previous
     * value and, therefore, does not have any overhead associated with returning of a value. It
     * should always be used whenever return value is not required.
     * <p>
     * If write-through is enabled, the stored value will be persisted to {@link GridCacheStore}
     * via {@link GridCacheStore#put(GridCacheTx, Object, Object)} method.
     * <h2 class="header">Transactions</h2>
     * This method is transactional and will enlist the entry into ongoing transaction
     * if there is one.
     * <h2 class="header">Cache Flags</h2>
     * This method is not available if any of the following flags are set on projection:
     * {@link GridCacheFlag#LOCAL}, {@link GridCacheFlag#READ}.
     *
     * @param key Key to store in cache.
     * @param val Value to be associated with the given key.
     * @param filter Optional filter to check prior to putting value in cache. Note
     *      that filter check is atomic with put operation.
     * @return Future for the put operation. Future will return {@code true} if optional filter
     *      passed and value was stored in cache, {@code false} otherwise. Note that future will
     *      return {@code true} if filter is not specified.
     * @throws NullPointerException If either key or value are {@code null}.
     * @throws GridCacheFlagException If projection flags validation failed.
     */
    public GridFuture<Boolean> putxAsync(K key, V val, @Nullable GridPredicate<GridCacheEntry<K, V>>... filter);

    /**
     * Stores result of applying {@code valTransform} closure to the previous value associated with
     * given key in cache. Result of closure application is guaranteed to be atomic, however, closure
     * itself can be applied more than once.
     * <p>
     * Note that transform closure must not throw any exceptions. If exception is thrown from {@code apply}
     * method, the transaction will be invalidated and entries participating in transaction will be nullified.
     * <p>
     * Unlike {@link #putx(Object, Object, GridPredicate[])} or {@link #put(Object, Object, GridPredicate[])}
     * methods, this method will not transfer the whole updated value over the network, but instead will
     * transfer the transforming closure that will be applied on each remote node involved in transaction.
     * It may add significant performance gain when dealing with large values as the value is much larger
     * than the closure itself. If write-through is enabled, the stored value will be persisted to
     * {@link GridCacheStore} via {@link GridCacheStore#put(GridCacheTx, Object, Object)} method.
     * <h2 class="header">Transactions</h2>
     * This method is transactional and will enlist the entry into ongoing transaction
     * if there is one.
     * <h2 class="header">Cache Flags</h2>
     * This method is not available if any of the following flags are set on projection:
     * {@link GridCacheFlag#LOCAL}, {@link GridCacheFlag#READ}.
     *
     * @param key Key to store in cache.
     * @param transformer Closure to be applied to the previous value in cache. If this closure returns
     *      {@code null}, the associated value will be removed from cache.
     * @throws NullPointerException If either key or transform closure is {@code null}.
     * @throws GridException On any error occurred while storing value in cache.
     */
    public void transform(K key, GridClosure<V, V> transformer) throws GridException;

    /**
     * Applies {@code transformer} closure to the previous value associated with given key in cache,
     * closure should return {@link GridBiTuple} instance where first value is new value stored in cache
     * and second value is returned as result of this method.
     * <h2 class="header">Transactions</h2>
     * This method is transactional and will enlist the entry into ongoing transaction
     * if there is one.
     * <h2 class="header">Cache Flags</h2>
     * This method is not available if any of the following flags are set on projection:
     * {@link GridCacheFlag#LOCAL}, {@link GridCacheFlag#READ}.
     *
     * @param key Key to store in cache.
     * @param transformer Closure to be applied to the previous value in cache.
     * @return Value computed by the closure.
     * @throws GridException On any error occurred while storing value in cache.
     */
    public <R> R transformAndCompute(K key, GridClosure<V, GridBiTuple<V, R>> transformer) throws GridException;

    /**
     * Stores result of applying {@code transformer} closure to the previous value associated with
     * given key in cache. Result of closure application is guaranteed to be atomic, however, closure
     * itself can be applied more than once.
     * <p>
     * Note that transform closure must not throw any exceptions. If exception is thrown from {@code apply}
     * method, the transaction will be invalidated and entries participating in transaction will be nullified.
     * <p>
     * Unlike {@link #putx(Object, Object, GridPredicate[])} method, this method will not transfer
     * the whole updated value over the network, but instead will transfer the transforming closure
     * that will be applied on each remote node involved in transaction. It may add significant performance
     * gain when dealing with large values as the value is much larger than the closure itself.
     * If write-through is enabled, the stored value will be persisted to {@link GridCacheStore}
     * via {@link GridCacheStore#put(GridCacheTx, Object, Object)} method.
     * <h2 class="header">Transactions</h2>
     * This method is transactional and will enlist the entry into ongoing transaction
     * if there is one.
     * <h2 class="header">Cache Flags</h2>
     * This method is not available if any of the following flags are set on projection:
     * {@link GridCacheFlag#LOCAL}, {@link GridCacheFlag#READ}.
     *
     * @param key Key to store in cache.
     * @param transformer Closure to be applied to the previous value in cache. If this closure returns
     *      {@code null}, the associated value will be removed from cache.
     * @return Future for the transform operation.
     * @throws NullPointerException If either key or transform closure is {@code null}.
     */
    public GridFuture<?> transformAsync(K key, GridClosure<V, V> transformer);

    /**
     * Stores given key-value pair in cache only if cache had no previous mapping for it. If cache
     * previously contained value for the given key, then this value is returned.
     * In case of {@link GridCacheMode#PARTITIONED} or {@link GridCacheMode#REPLICATED} caches,
     * the value will be loaded from the primary node, which in its turn may load the value
     * from the swap storage, and consecutively, if it's not in swap,
     * from the underlying persistent storage. If value has to be loaded from persistent
     * storage, {@link GridCacheStore#load(GridCacheTx, Object)} method will be used.
     * <p>
     * If the returned value is not needed, method {@link #putxIfAbsent(Object, Object)} should
     * always be used instead of this one to avoid the overhead associated with returning of the
     * previous value.
     * <p>
     * If write-through is enabled, the stored value will be persisted to {@link GridCacheStore}
     * via {@link GridCacheStore#put(GridCacheTx, Object, Object)} method.
     * <h2 class="header">Transactions</h2>
     * This method is transactional and will enlist the entry into ongoing transaction
     * if there is one.
     * <h2 class="header">Cache Flags</h2>
     * This method is not available if any of the following flags are set on projection:
     * {@link GridCacheFlag#LOCAL}, {@link GridCacheFlag#READ}.
     *
     * @param key Key to store in cache.
     * @param val Value to be associated with the given key.
     * @return Previously contained value regardless of whether put happened or not.
     * @throws NullPointerException If either key or value are {@code null}.
     * @throws GridException If put operation failed.
     * @throws GridCacheFlagException If projection flags validation failed.
     */
    @Nullable public V putIfAbsent(K key, V val) throws GridException;

    /**
     * Asynchronously stores given key-value pair in cache only if cache had no previous mapping for it. If cache
     * previously contained value for the given key, then this value is returned. In case of
     * {@link GridCacheMode#PARTITIONED} or {@link GridCacheMode#REPLICATED} caches,
     * the value will be loaded from the primary node, which in its turn may load the value
     * from the swap storage, and consecutively, if it's not in swap,
     * from the underlying persistent storage. If value has to be loaded from persistent
     * storage, {@link GridCacheStore#load(GridCacheTx, Object)} method will be used.
     * <p>
     * If the returned value is not needed, method {@link #putxIfAbsentAsync(Object, Object)} should
     * always be used instead of this one to avoid the overhead associated with returning of the
     * previous value.
     * <p>
     * If write-through is enabled, the stored value will be persisted to {@link GridCacheStore}
     * via {@link GridCacheStore#put(GridCacheTx, Object, Object)} method.
     * <h2 class="header">Transactions</h2>
     * This method is transactional and will enlist the entry into ongoing transaction
     * if there is one.
     * <h2 class="header">Cache Flags</h2>
     * This method is not available if any of the following flags are set on projection:
     * {@link GridCacheFlag#LOCAL}, {@link GridCacheFlag#READ}.
     *
     * @param key Key to store in cache.
     * @param val Value to be associated with the given key.
     * @return Future of put operation which will provide previously contained value
     *   regardless of whether put happened or not.
     * @throws NullPointerException If either key or value are {@code null}.
     * @throws GridCacheFlagException If projection flags validation failed.
     */
    public GridFuture<V> putIfAbsentAsync(K key, V val);

    /**
     * Stores given key-value pair in cache only if cache had no previous mapping for it.
     * <p>
     * This method will return {@code true} if value is stored in cache and {@code false} otherwise.
     * Unlike {@link #putIfAbsent(Object, Object)} method, it does not return previous
     * value and, therefore, does not have any overhead associated with returning of a value. It
     * should always be used whenever return value is not required.
     * <p>
     * If write-through is enabled, the stored value will be persisted to {@link GridCacheStore}
     * via {@link GridCacheStore#put(GridCacheTx, Object, Object)} method.
     * <h2 class="header">Transactions</h2>
     * This method is transactional and will enlist the entry into ongoing transaction
     * if there is one.
     * <h2 class="header">Cache Flags</h2>
     * This method is not available if any of the following flags are set on projection:
     * {@link GridCacheFlag#LOCAL}, {@link GridCacheFlag#READ}.
     *
     * @param key Key to store in cache.
     * @param val Value to be associated with the given key.
     * @return {@code true} if value is stored in cache and {@code false} otherwise.
     * @throws NullPointerException If either key or value are {@code null}.
     * @throws GridException If put operation failed.
     * @throws GridCacheFlagException If projection flags validation failed.
     */
    public boolean putxIfAbsent(K key, V val) throws GridException;

    /**
     * Asynchronously stores given key-value pair in cache only if cache had no previous mapping for it.
     * <p>
     * This method will return {@code true} if value is stored in cache and {@code false} otherwise.
     * Unlike {@link #putIfAbsent(Object, Object)} method, it does not return previous
     * value and, therefore, does not have any overhead associated with returning of a value. It
     * should always be used whenever return value is not required.
     * <p>
     * If write-through is enabled, the stored value will be persisted to {@link GridCacheStore}
     * via {@link GridCacheStore#put(GridCacheTx, Object, Object)} method.
     * <h2 class="header">Transactions</h2>
     * This method is transactional and will enlist the entry into ongoing transaction
     * if there is one.
     * <h2 class="header">Cache Flags</h2>
     * This method is not available if any of the following flags are set on projection:
     * {@link GridCacheFlag#LOCAL}, {@link GridCacheFlag#READ}.
     *
     * @param key Key to store in cache.
     * @param val Value to be associated with the given key.
     * @return Future for this put operation.
     * @throws NullPointerException If either key or value are {@code null}.
     * @throws GridCacheFlagException If projection flags validation failed.
     */
    public GridFuture<Boolean> putxIfAbsentAsync(K key, V val);

    /**
     * Stores given key-value pair in cache only if there is a previous mapping for it.
     * In case of {@link GridCacheMode#PARTITIONED} or {@link GridCacheMode#REPLICATED} caches,
     * the value will be loaded from the primary node, which in its turn may load the value
     * from the swap storage, and consecutively, if it's not in swap,
     * from the underlying persistent storage. If value has to be loaded from persistent
     * storage, {@link GridCacheStore#load(GridCacheTx, Object)} method will be used.
     * <p>
     * If the returned value is not needed, method {@link #replacex(Object, Object)} should
     * always be used instead of this one to avoid the overhead associated with returning of the
     * previous value.
     * <p>
     * If write-through is enabled, the stored value will be persisted to {@link GridCacheStore}
     * via {@link GridCacheStore#put(GridCacheTx, Object, Object)} method.
     * <h2 class="header">Transactions</h2>
     * This method is transactional and will enlist the entry into ongoing transaction
     * if there is one.
     * <h2 class="header">Cache Flags</h2>
     * This method is not available if any of the following flags are set on projection:
     * {@link GridCacheFlag#LOCAL}, {@link GridCacheFlag#READ}.
     *
     * @param key Key to store in cache.
     * @param val Value to be associated with the given key.
     * @return Previously contained value regardless of whether replace happened or not.
     * @throws NullPointerException If either key or value are {@code null}.
     * @throws GridException If replace operation failed.
     * @throws GridCacheFlagException If projection flags validation failed.
     */
    @Nullable public V replace(K key, V val) throws GridException;

    /**
     * Asynchronously stores given key-value pair in cache only if there is a previous mapping for it. If cache
     * previously contained value for the given key, then this value is returned.In case of
     * {@link GridCacheMode#PARTITIONED} caches, the value will be loaded from the primary node,
     * which in its turn may load the value from the swap storage, and consecutively, if it's not in swap,
     * from the underlying persistent storage. If value has to be loaded from persistent
     * storage, {@link GridCacheStore#load(GridCacheTx, Object)} method will be used.
     * <p>
     * If the returned value is not needed, method {@link #replacex(Object, Object)} should
     * always be used instead of this one to avoid the overhead associated with returning of the
     * previous value.
     * <p>
     * If write-through is enabled, the stored value will be persisted to {@link GridCacheStore}
     * via {@link GridCacheStore#put(GridCacheTx, Object, Object)} method.
     * <h2 class="header">Transactions</h2>
     * This method is transactional and will enlist the entry into ongoing transaction
     * if there is one.
     * <h2 class="header">Cache Flags</h2>
     * This method is not available if any of the following flags are set on projection:
     * {@link GridCacheFlag#LOCAL}, {@link GridCacheFlag#READ}.
     *
     * @param key Key to store in cache.
     * @param val Value to be associated with the given key.
     * @return Future for replace operation.
     * @throws NullPointerException If either key or value are {@code null}.
     * @throws GridCacheFlagException If projection flags validation failed.
     */
    public GridFuture<V> replaceAsync(K key, V val);

    /**
     * Stores given key-value pair in cache only if only if there is a previous mapping for it.
     * <p>
     * This method will return {@code true} if value is stored in cache and {@code false} otherwise.
     * Unlike {@link #replace(Object, Object)} method, it does not return previous
     * value and, therefore, does not have any overhead associated with returning of a value. It
     * should always be used whenever return value is not required.
     * <p>
     * If write-through is enabled, the stored value will be persisted to {@link GridCacheStore}
     * via {@link GridCacheStore#put(GridCacheTx, Object, Object)} method.
     * <h2 class="header">Transactions</h2>
     * This method is transactional and will enlist the entry into ongoing transaction
     * if there is one.
     * <h2 class="header">Cache Flags</h2>
     * This method is not available if any of the following flags are set on projection:
     * {@link GridCacheFlag#LOCAL}, {@link GridCacheFlag#READ}.
     *
     * @param key Key to store in cache.
     * @param val Value to be associated with the given key.
     * @return {@code True} if replace happened, {@code false} otherwise.
     * @throws NullPointerException If either key or value are {@code null}.
     * @throws GridException If replace operation failed.
     * @throws GridCacheFlagException If projection flags validation failed.
     */
    public boolean replacex(K key, V val) throws GridException;

    /**
     * Asynchronously stores given key-value pair in cache only if only if there is a previous mapping for it.
     * <p>
     * This method will return {@code true} if value is stored in cache and {@code false} otherwise.
     * Unlike {@link #replaceAsync(Object, Object)} method, it does not return previous
     * value and, therefore, does not have any overhead associated with returning of a value. It
     * should always be used whenever return value is not required.
     * <p>
     * If write-through is enabled, the stored value will be persisted to {@link GridCacheStore}
     * via {@link GridCacheStore#put(GridCacheTx, Object, Object)} method.
     * <h2 class="header">Transactions</h2>
     * This method is transactional and will enlist the entry into ongoing transaction
     * if there is one.
     * <h2 class="header">Cache Flags</h2>
     * This method is not available if any of the following flags are set on projection:
     * {@link GridCacheFlag#LOCAL}, {@link GridCacheFlag#READ}.
     *
     * @param key Key to store in cache.
     * @param val Value to be associated with the given key.
     * @return Future for the replace operation.
     * @throws NullPointerException If either key or value are {@code null}.
     * @throws GridCacheFlagException If projection flags validation failed.
     */
    public GridFuture<Boolean> replacexAsync(K key, V val);

    /**
     * Stores given key-value pair in cache only if only if the previous value is equal to the
     * {@code 'oldVal'} passed in.
     * <p>
     * This method will return {@code true} if value is stored in cache and {@code false} otherwise.
     * <p>
     * If write-through is enabled, the stored value will be persisted to {@link GridCacheStore}
     * via {@link GridCacheStore#put(GridCacheTx, Object, Object)} method.
     * <h2 class="header">Transactions</h2>
     * This method is transactional and will enlist the entry into ongoing transaction
     * if there is one.
     * <h2 class="header">Cache Flags</h2>
     * This method is not available if any of the following flags are set on projection:
     * {@link GridCacheFlag#LOCAL}, {@link GridCacheFlag#READ}.
     *
     * @param key Key to store in cache.
     * @param oldVal Old value to match.
     * @param newVal Value to be associated with the given key.
     * @return {@code True} if replace happened, {@code false} otherwise.
     * @throws NullPointerException If either key or value are {@code null}.
     * @throws GridException If replace operation failed.
     * @throws GridCacheFlagException If projection flags validation failed.
     */
    public boolean replace(K key, V oldVal, V newVal) throws GridException;

    /**
     * Asynchronously stores given key-value pair in cache only if only if the previous value is equal to the
     * {@code 'oldVal'} passed in.
     * <p>
     * This method will return {@code true} if value is stored in cache and {@code false} otherwise.
     * <p>
     * If write-through is enabled, the stored value will be persisted to {@link GridCacheStore}
     * via {@link GridCacheStore#put(GridCacheTx, Object, Object)} method.
     * <h2 class="header">Transactions</h2>
     * This method is transactional and will enlist the entry into ongoing transaction
     * if there is one.
     * <h2 class="header">Cache Flags</h2>
     * This method is not available if any of the following flags are set on projection:
     * {@link GridCacheFlag#LOCAL}, {@link GridCacheFlag#READ}.
     *
     * @param key Key to store in cache.
     * @param oldVal Old value to match.
     * @param newVal Value to be associated with the given key.
     * @return Future for the replace operation.
     * @throws NullPointerException If either key or value are {@code null}.
     * @throws GridCacheFlagException If projection flags validation failed.
     */
    public GridFuture<Boolean> replaceAsync(K key, V oldVal, V newVal);

    /**
     * Stores given key-value pairs in cache. If filters are provided, then entries will
     * be stored in cache only if they pass the filter. Note that filter check is atomic,
     * so value stored in cache is guaranteed to be consistent with the filters.
     * <p>
     * If write-through is enabled, the stored values will be persisted to {@link GridCacheStore}
     * via {@link GridCacheStore#putAll(GridCacheTx, Map)} method.
     * <h2 class="header">Transactions</h2>
     * This method is transactional and will enlist the entry into ongoing transaction
     * if there is one.
     * <h2 class="header">Cache Flags</h2>
     * This method is not available if any of the following flags are set on projection:
     * {@link GridCacheFlag#LOCAL}, {@link GridCacheFlag#READ}.
     *
     * @param m Key-value pairs to store in cache.
     * @param filter Optional entry filter. If provided, then entry will
     *      be stored only if the filter returned {@code true}.
     * @throws GridException If put operation failed.
     * @throws GridCacheFlagException If projection flags validation failed.
     */
    public void putAll(@Nullable Map<? extends K, ? extends V> m,
        @Nullable GridPredicate<GridCacheEntry<K, V>>... filter) throws GridException;

    /**
     * Stores result of applying transform closures from the given map to previous values associated
     * with corresponding keys in cache. Execution of closure is guaranteed to be atomic,
     * however, closure itself can be applied more than once.
     * <p>
     * Note that transform closure must not throw any exceptions. If exception is thrown from {@code apply}
     * method, the transaction will be invalidated and entries participating in transaction will be nullified.
     * <p>
     * Unlike {@link #putAll(Map, GridPredicate[])} method, this method will not transfer
     * the whole updated value over the network, but instead will transfer the transforming closures
     * that will be applied on each remote node involved in transaction. It may add significant
     * performance gain when dealing with large values as the value is much larger than the closure itself.
     * If write-through is enabled, the stored value will be persisted to {@link GridCacheStore}
     * via {@link GridCacheStore#put(GridCacheTx, Object, Object)} method.
     * <h2 class="header">Transactions</h2>
     * This method is transactional and will enlist the entry into ongoing transaction
     * if there is one.
     * <h2 class="header">Cache Flags</h2>
     * This method is not available if any of the following flags are set on projection:
     * {@link GridCacheFlag#LOCAL}, {@link GridCacheFlag#READ}.
     *
     * @param m Map containing keys and closures to be applied to values.
     * @throws GridException On any error occurred while storing value in cache.
     */
    public void transformAll(@Nullable Map<? extends K, ? extends GridClosure<V, V>> m) throws GridException;

    /**
     * Stores result of applying the specified transform closure to previous values associated
     * with the specified keys in cache. Execution of closure is guaranteed to be atomic,
     * however, closure itself can be applied more than once.
     * <p>
     * Note that transform closure must not throw any exceptions. If exception is thrown from {@code apply}
     * method, the transaction will be invalidated and entries participating in transaction will be nullified.
     * <p>
     * Unlike {@link #putAll(Map, GridPredicate[])} method, this method will not transfer
     * the whole updated value over the network, but instead will transfer the transforming closure
     * that will be applied on each remote node involved in transaction. It may add significant
     * performance gain when dealing with large values as the value is much larger than the closure itself.
     * If write-through is enabled, the stored value will be persisted to {@link GridCacheStore}
     * via {@link GridCacheStore#put(GridCacheTx, Object, Object)} method.
     * <h2 class="header">Transactions</h2>
     * This method is transactional and will enlist the entry into ongoing transaction
     * if there is one.
     * <h2 class="header">Cache Flags</h2>
     * This method is not available if any of the following flags are set on projection:
     * {@link GridCacheFlag#LOCAL}, {@link GridCacheFlag#READ}.
     *
     * @param keys Keys for entries, to which the transformation closure will be applied.
     *             If the collection is {@code null} or empty, this method is no-op.
     * @param transformer Transformation closure to be applied to each value.
     * @throws GridException On any error occurred while storing value in cache.
     */
    public void transformAll(@Nullable Set<? extends K> keys, GridClosure<V, V> transformer) throws GridException;

    /**
     * Asynchronously stores given key-value pairs in cache. If filters are provided, then entries will
     * be stored in cache only if they pass the filter. Note that filter check is atomic,
     * so value stored in cache is guaranteed to be consistent with the filters.
     * <p>
     * If write-through is enabled, the stored values will be persisted to {@link GridCacheStore}
     * via {@link GridCacheStore#putAll(GridCacheTx, Map)} method.
     * <h2 class="header">Transactions</h2>
     * This method is transactional and will enlist the entry into ongoing transaction
     * if there is one.
     * <h2 class="header">Cache Flags</h2>
     * This method is not available if any of the following flags are set on projection:
     * {@link GridCacheFlag#LOCAL}, {@link GridCacheFlag#READ}.
     *
     * @param m Key-value pairs to store in cache.
     * @param filter Optional entry filter. If provided, then entry will
     *      be stored only if the filter returned {@code true}.
     * @return Future for putAll operation.
     * @throws GridCacheFlagException If projection flags validation failed.
     */
    public GridFuture<?> putAllAsync(@Nullable Map<? extends K, ? extends V> m,
        @Nullable GridPredicate<GridCacheEntry<K, V>>... filter);

    /**
     * Stores result of applying transform closures from the given map to previous values associated
     * with corresponding keys in cache. Result of closure application is guaranteed to be atomic,
     * however, closure itself can be applied more than once.
     * <p>
     * Note that transform closure must not throw any exceptions. If exception is thrown from {@code apply}
     * method, the transaction will be invalidated and entries participating in transaction will be nullified.
     * <p>
     * Unlike {@link #putAll(Map, GridPredicate[])} method, this method will not transfer
     * the whole updated value over the network, but instead will transfer the transforming closures
     * that will be applied on each remote node involved in transaction. It may add significant performance
     * gain when dealing with large values as the value is much larger than the closure itself.
     * If write-through is enabled, the stored value will be persisted to {@link GridCacheStore}
     * via {@link GridCacheStore#put(GridCacheTx, Object, Object)} method.
     * <h2 class="header">Transactions</h2>
     * This method is transactional and will enlist the entry into ongoing transaction
     * if there is one.
     * <h2 class="header">Cache Flags</h2>
     * This method is not available if any of the following flags are set on projection:
     * {@link GridCacheFlag#LOCAL}, {@link GridCacheFlag#READ}.
     *
     * @param m Map containing keys and closures to be applied to values.
     * @return Future for operation.
     */
    public GridFuture<?> transformAllAsync(@Nullable Map<? extends K, ? extends GridClosure<V, V>> m);

    /**
     * Stores result of applying the specified transform closure to previous values associated
     * with the specified keys in cache. Result of closure application is guaranteed to be atomic,
     * however, closure itself can be applied more than once.
     * <p>
     * Note that transform closure must not throw any exceptions. If exception is thrown from {@code apply}
     * method, the transaction will be invalidated and entries participating in transaction will be nullified.
     * <p>
     * Unlike {@link #putAll(Map, GridPredicate[])} method, this method will not transfer
     * the whole updated value over the network, but instead will transfer the transforming closure
     * that will be applied on each remote node involved in transaction. It may add significant
     * performance gain when dealing with large values as the value is much larger than the closure itself.
     * If write-through is enabled, the stored value will be persisted to {@link GridCacheStore}
     * via {@link GridCacheStore#put(GridCacheTx, Object, Object)} method.
     * <h2 class="header">Transactions</h2>
     * This method is transactional and will enlist the entry into ongoing transaction
     * if there is one.
     * <h2 class="header">Cache Flags</h2>
     * This method is not available if any of the following flags are set on projection:
     * {@link GridCacheFlag#LOCAL}, {@link GridCacheFlag#READ}.
     *
     * @param keys Keys for entries, to which the transformation closure will be applied.
     *             If the collection is {@code null} or empty, this method is no-op.
     * @param transformer Transformation closure to be applied to each value.
     * @return Future for operation.
     * @throws GridException On any error occurred while storing value in cache.
     */
    public GridFuture<?> transformAllAsync(@Nullable Set<? extends K> keys, GridClosure<V, V> transformer)
        throws GridException;

    /**
     * Set of keys cached on this node. You can remove elements from this set, but you cannot add elements
     * to this set. All removal operation will be reflected on the cache itself.
     * <p>
     * Iterator over this set will not fail if set was concurrently updated
     * by another thread. This means that iterator may or may not return latest
     * keys depending on whether they were added before or after current
     * iterator position.
     * <p>
     * NOTE: this operation is not distributed and returns only the keys cached on this node.
     *
     * @return Key set for this cache projection.
     */
    public Set<K> keySet();

    /**
     * Set of keys for which this node is primary.
     * This set is dynamic and may change with grid topology changes.
     * Note that this set will contain mappings for all keys, even if their values are
     * {@code null} because they were invalidated. You can remove elements from
     * this set, but you cannot add elements to this set. All removal operation will be
     * reflected on the cache itself.
     * <p>
     * Iterator over this set will not fail if set was concurrently updated
     * by another thread. This means that iterator may or may not return latest
     * keys depending on whether they were added before or after current
     * iterator position.
     * <p>
     * NOTE: this operation is not distributed and returns only the keys cached on this node.
     *
     * @return Primary key set for the current node.
     */
    public Set<K> primaryKeySet();

    /**
     * Collection of values cached on this node. You can remove
     * elements from this collection, but you cannot add elements to this collection.
     * All removal operation will be reflected on the cache itself.
     * <p>
     * Iterator over this collection will not fail if collection was
     * concurrently updated by another thread. This means that iterator may or
     * may not return latest values depending on whether they were added before
     * or after current iterator position.
     * <p>
     * NOTE: this operation is not distributed and returns only the values cached on this node.
     *
     * @return Collection of cached values.
     */
    public Collection<V> values();

    /**
     * Collection of cached values for which this node is primary.
     * This collection is dynamic and may change with grid topology changes.
     * Note that this collection will not contain values that are {@code null}
     * because they were invalided. You can remove elements from this collection,
     * but you cannot add elements to this collection. All removal operation will be
     * reflected on the cache itself.
     * <p>
     * Iterator over this collection will not fail if collection was
     * concurrently updated by another thread. This means that iterator may or
     * may not return latest values depending on whether they were added before
     * or after current iterator position.
     * <p>
     * NOTE: this operation is not distributed and returns only the values cached on this node.
     *
     * @return Collection of primary cached values for the current node.
     */
    public Collection<V> primaryValues();

    /**
     * Gets set of all entries cached on this node. You can remove
     * elements from this set, but you cannot add elements to this set.
     * All removal operation will be reflected on the cache itself.
     * <p>
     * NOTE: this operation is not distributed and returns only the entries cached on this node.
     *
     * @return Entries that pass through key filter.
     */
    public Set<GridCacheEntry<K, V>> entrySet();

    /**
     * Gets set containing cache entries that belong to provided partition or {@code null}
     * if partition is not found locally.
     * <p>
     * NOTE: this operation is not distributed and returns only the entries cached on this node.
     *
     * @param part Partition.
     * @return Set containing partition's entries or {@code null} if partition is
     *      not found locally.
     */
    @Nullable public Set<GridCacheEntry<K, V>> entrySet(int part);

    /**
     * Gets set of cache entries for which this node is primary.
     * This set is dynamic and may change with grid topology changes. You can remove
     * elements from this set, but you cannot add elements to this set.
     * All removal operation will be reflected on the cache itself.
     * <p>
     * NOTE: this operation is not distributed and returns only the entries cached on this node.
     *
     * @return Primary cache entries that pass through key filter.
     */
    public Set<GridCacheEntry<K, V>> primaryEntrySet();

    /**
     * Starts transaction with default isolation, concurrency, timeout, and invalidation policy.
     * All defaults are set in {@link GridCacheConfiguration} at startup.
     *
     * @return New transaction
     * @throws IllegalStateException If transaction is already started by this thread.
     * @throws UnsupportedOperationException If cache is {@link GridCacheAtomicityMode#ATOMIC}.
     */
    public GridCacheTx txStart() throws IllegalStateException;

    /**
     * Starts new transaction with the specified concurrency and isolation.
     *
     * @param concurrency Concurrency.
     * @param isolation Isolation.
     * @return New transaction.
     * @throws IllegalStateException If transaction is already started by this thread.
     * @throws UnsupportedOperationException If cache is {@link GridCacheAtomicityMode#ATOMIC}.
     */
    public GridCacheTx txStart(GridCacheTxConcurrency concurrency, GridCacheTxIsolation isolation);

    /**
     * Starts transaction with specified isolation, concurrency, timeout, invalidation flag,
     * and number of participating entries.
     *
     * @param concurrency Concurrency.
     * @param isolation Isolation.
     * @param timeout Timeout.
     * @param txSize Number of entries participating in transaction (may be approximate).
     * @return New transaction.
     * @throws IllegalStateException If transaction is already started by this thread.
     * @throws UnsupportedOperationException If cache is {@link GridCacheAtomicityMode#ATOMIC}.
     */
    public GridCacheTx txStart(GridCacheTxConcurrency concurrency, GridCacheTxIsolation isolation, long timeout,
        int txSize);

    /**
     * Starts {@code affinity-group-locked} transaction based on affinity key. In this mode only affinity key
     * is locked and all other entries in transaction are written without locking. However,
     * all keys in such transaction must have the same affinity key. Node on which transaction
     * is started must be primary for the given affinity key (an exception is thrown otherwise).
     * <p>
     * Since only affinity key is locked, and no individual keys, it is user's responsibility to make sure
     * there are no other concurrent explicit updates directly on individual keys participating in the
     * transaction. All updates to the keys involved should always go through {@code affinity-group-locked}
     * transaction, otherwise cache may be left in inconsistent state.
     * <p>
     * If cache sanity check is enabled ({@link GridConfiguration#isCacheSanityCheckEnabled()}),
     * the following checks are performed:
     * <ul>
     *     <li>
     *         An exception will be thrown if affinity key differs from one specified on transaction start.
     *     </li>
     *     <li>
     *         An exception is thrown if entry participating in transaction is externally locked at commit.
     *     </li>
     * </ul>
     *
     * @param affinityKey Affinity key for all entries updated by transaction. This node
     *      must be primary for this key.
     * @param timeout Timeout ({@code 0} for default).
     * @param txSize Number of entries participating in transaction (may be approximate), {@code 0} for default.
     * @param concurrency Transaction concurrency control.
     * @param isolation Cache transaction isolation level.
     * @return Started transaction.
     * @throws IllegalStateException If transaction is already started by this thread.
     * @throws GridException If local node is not primary for any of provided keys.
     * @throws UnsupportedOperationException If cache is {@link GridCacheAtomicityMode#ATOMIC}.
     */
    public GridCacheTx txStartAffinity(Object affinityKey, GridCacheTxConcurrency concurrency,
        GridCacheTxIsolation isolation, long timeout, int txSize) throws IllegalStateException, GridException;

    /**
     * Starts {@code partition-group-locked} transaction based on partition ID. In this mode the whole partition
     * is locked and all other entries in transaction are written without locking. However,
     * all keys in such transaction must belong to the same partition. Node on which transaction
     * is started must be primary for the given partition (an exception is thrown otherwise).
     * <p>
     * Since only partition is locked, and no individual keys, it is user's responsibility to make sure
     * there are no other concurrent explicit updates directly on individual keys participating in the
     * transaction. All updates to the keys involved should always go through {@code partition-group-locked}
     * transaction, otherwise, cache may be left in inconsistent state.
     * <p>
     * If cache sanity check is enabled ({@link GridConfiguration#isCacheSanityCheckEnabled()}),
     * the following checks are performed:
     * <ul>
     *     <li>
     *         An exception will be thrown if key partition differs from one specified on transaction start.
     *     </li>
     *     <li>
     *         An exception is thrown if entry participating in transaction is externally locked at commit.
     *     </li>
     * </ul>
     *
     * @param partId Partition id for which transaction is started. This node
     *      must be primary for this partition.
     * @param timeout Timeout ({@code 0} for default).
     * @param txSize Number of entries participating in transaction (may be approximate), {@code 0} for default.
     * @param concurrency Transaction concurrency control.
     * @param isolation Cache transaction isolation level.
     * @return Started transaction.
     * @throws IllegalStateException If transaction is already started by this thread.
     * @throws GridException If local node is not primary for any of provided keys.
     * @throws UnsupportedOperationException If cache is {@link GridCacheAtomicityMode#ATOMIC}.
     */
    public GridCacheTx txStartPartition(int partId, GridCacheTxConcurrency concurrency,
        GridCacheTxIsolation isolation, long timeout, int txSize) throws IllegalStateException, GridException;

    /**
     * Gets transaction started by this thread or {@code null} if this thread does
     * not have a transaction.
     *
     * @return Transaction started by this thread or {@code null} if this thread
     *      does not have a transaction.
     */
    @Nullable public GridCacheTx tx();

    /**
     * Gets entry from cache with the specified key. The returned entry can
     * be used even after entry key has been removed from cache. In that
     * case, every operation on returned entry will result in creation of a
     * new entry.
     * <p>
     * Note that this method can return {@code null} if projection is configured as
     * pre-filtered and entry key and value don't pass key-value filter of the projection.
     *
     * @param key Entry key.
     * @return Cache entry or {@code null} if projection pre-filtering was not passed.
     */
    @Nullable public GridCacheEntry<K, V> entry(K key);

    /**
     * Evicts entry associated with given key from cache. Note, that entry will be evicted
     * only if it's not used (not participating in any locks or transactions).
     * <p>
     * If {@link GridCacheConfiguration#isSwapEnabled()} is set to {@code true} and
     * {@link GridCacheFlag#SKIP_SWAP} is not enabled, the evicted entry will
     * be swapped to offheap, and then to disk.
     * <h2 class="header">Cache Flags</h2>
     * This method is not available if any of the following flags are set on projection:
     * {@link GridCacheFlag#READ}.
     *
     * @param key Key to evict from cache.
     * @return {@code True} if entry could be evicted, {@code false} otherwise.
     */
    public boolean evict(K key);

    /**
     * Attempts to evict all cache entries. Note, that entry will be
     * evicted only if it's not used (not participating in any locks or
     * transactions).
     * <p>
     * If {@link GridCacheConfiguration#isSwapEnabled()} is set to {@code true} and
     * {@link GridCacheFlag#SKIP_SWAP} is not enabled, the evicted entry will
     * be swapped to offheap, and then to disk.
     * <h2 class="header">Cache Flags</h2>
     * This method is not available if any of the following flags are set on projection:
     * {@link GridCacheFlag#READ}.
     */
    public void evictAll();

    /**
     * Attempts to evict all entries associated with keys. Note,
     * that entry will be evicted only if it's not used (not
     * participating in any locks or transactions).
     * <p>
     * If {@link GridCacheConfiguration#isSwapEnabled()} is set to {@code true} and
     * {@link GridCacheFlag#SKIP_SWAP} is not enabled, the evicted entry will
     * be swapped to offheap, and then to disk.
     * <h2 class="header">Cache Flags</h2>
     * This method is not available if any of the following flags are set on projection:
     * {@link GridCacheFlag#READ}.
     *
     * @param keys Keys to evict.
     */
    public void evictAll(@Nullable Collection<? extends K> keys);

    /**
     * Clears all entries from this cache only if the entry is not
     * currently locked or participating in a transaction.
     * <p>
     * If {@link GridCacheConfiguration#isSwapEnabled()} is set to {@code true} and
     * {@link GridCacheFlag#SKIP_SWAP} is not enabled, the evicted entries will
     * also be cleared from swap.
     * <p>
     * Note that this operation is local as it merely clears
     * entries from local cache. It does not remove entries from
     * remote caches or from underlying persistent storage.
     * <h2 class="header">Cache Flags</h2>
     * This method is not available if any of the following flags are set on projection:
     * {@link GridCacheFlag#READ}.
     */
    public void clearAll();

    /**
     * Clears an entry from this cache and swap storage only if the entry
     * is not currently locked, and is not participating in a transaction.
     * <p>
     * If {@link GridCacheConfiguration#isSwapEnabled()} is set to {@code true} and
     * {@link GridCacheFlag#SKIP_SWAP} is not enabled, the evicted entries will
     * also be cleared from swap.
     * <p>
     * Note that this operation is local as it merely clears
     * an entry from local cache. It does not remove entries from
     * remote caches or from underlying persistent storage.
     * <h2 class="header">Cache Flags</h2>
     * This method is not available if any of the following flags are set on projection:
     * {@link GridCacheFlag#READ}.
     *
     * @param key Key to clear.
     * @return {@code True} if entry was successfully cleared from cache, {@code false}
     *      if entry was in use at the time of this method invocation and could not be
     *      cleared.
     */
    public boolean clear(K key);

    /**
     * Clears cache on all nodes that store it's data. That is, caches are cleared on remote
     * nodes and local node, as opposed to {@link GridCacheProjection#clearAll()} method which only
     * clears local node's cache.
     * <p>
     * GridGain will make the best attempt to clear caches on all nodes. If some caches
     * could not be cleared, then exception will be thrown.
     * <p>
     * This method is identical to calling {@link #globalClearAll(long) globalClearAll(0)}.
     *
     * @throws GridException In case of cache could not be cleared on any of the nodes.
     * @deprecated Deprecated in favor of {@link #globalClearAll(long)} method.
     */
    @Deprecated
    public void globalClearAll() throws GridException;

    /**
     * Clears cache on all nodes that store it's data. That is, caches are cleared on remote
     * nodes and local node, as opposed to {@link GridCacheProjection#clearAll()} method which only
     * clears local node's cache.
     * <p>
     * GridGain will make the best attempt to clear caches on all nodes. If some caches
     * could not be cleared, then exception will be thrown.
     *
     * @param timeout Timeout for clear all task in milliseconds (0 for never).
     *      Set it to larger value for large caches.
     * @throws GridException In case of cache could not be cleared on any of the nodes.
     */
    public void globalClearAll(long timeout) throws GridException;

    /**
     * Clears serialized value bytes from entry (if any) leaving only object representation.
     *
     * @param key Key to compact.
     * @throws GridException If failed to compact.
     * @return {@code true} if entry was deleted from cache (i.e. was expired).
     */
    public boolean compact(K key) throws GridException;

    /**
     * Clears serialized value bytes from cache entries (if any) leaving only object representation.
     * @throws GridException If failed to compact.
     */
    public void compactAll() throws GridException;

    /**
     * Removes given key mapping from cache. If cache previously contained value for the given key,
     * then this value is returned. In case of {@link GridCacheMode#PARTITIONED} or {@link GridCacheMode#REPLICATED}
     * caches, the value will be loaded from the primary node, which in its turn may load the value
     * from the disk-based swap storage, and consecutively, if it's not in swap,
     * from the underlying persistent storage. If value has to be loaded from persistent
     * storage, {@link GridCacheStore#load(GridCacheTx, Object)} method will be used.
     * <p>
     * If the returned value is not needed, method {@link #removex(Object, GridPredicate[])} should
     * always be used instead of this one to avoid the overhead associated with returning of the
     * previous value.
     * <p>
     * If write-through is enabled, the value will be removed from {@link GridCacheStore}
     * via {@link GridCacheStore#remove(GridCacheTx, Object)} method.
     * <h2 class="header">Transactions</h2>
     * This method is transactional and will enlist the entry into ongoing transaction
     * if there is one.
     * <h2 class="header">Cache Flags</h2>
     * This method is not available if any of the following flags are set on projection:
     * {@link GridCacheFlag#LOCAL}, {@link GridCacheFlag#READ}.
     *
     * @param key Key whose mapping is to be removed from cache.
     * @param filter Optional filter to check prior to removing value form cache. Note
     *      that filter is checked atomically together with remove operation.
     * @return Previous value associated with specified key, or {@code null}
     *      if there was no value for this key.
     * @throws NullPointerException If key is {@code null}.
     * @throws GridException If remove operation failed.
     * @throws GridCacheFlagException If projection flags validation failed.
     */
    @Nullable public V remove(K key, @Nullable GridPredicate<GridCacheEntry<K, V>>... filter)
        throws GridException;

    /**
     * Asynchronously removes given key mapping from cache. If cache previously contained value for the given key,
     * then this value is returned. In case of {@link GridCacheMode#PARTITIONED} or {@link GridCacheMode#REPLICATED}
     * caches, the value will be loaded from the primary node, which in its turn may load the value
     * from the swap storage, and consecutively, if it's not in swap,
     * from the underlying persistent storage. If value has to be loaded from persistent
     * storage, {@link GridCacheStore#load(GridCacheTx, Object)} method will be used.
     * <p>
     * If the returned value is not needed, method {@link #removex(Object, GridPredicate[])} should
     * always be used instead of this one to avoid the overhead associated with returning of the
     * previous value.
     * <p>
     * If write-through is enabled, the value will be removed from {@link GridCacheStore}
     * via {@link GridCacheStore#remove(GridCacheTx, Object)} method.
     * <h2 class="header">Transactions</h2>
     * This method is transactional and will enlist the entry into ongoing transaction
     * if there is one.
     * <h2 class="header">Cache Flags</h2>
     * This method is not available if any of the following flags are set on projection:
     * {@link GridCacheFlag#LOCAL}, {@link GridCacheFlag#READ}.
     *
     * @param key Key whose mapping is to be removed from cache.
     * @param filter Optional filter to check prior to removing value form cache. Note
     *      that filter is checked atomically together with remove operation.
     * @return Future for the remove operation.
     * @throws NullPointerException if the key is {@code null}.
     * @throws GridCacheFlagException If projection flags validation failed.
     */
    public GridFuture<V> removeAsync(K key, GridPredicate<GridCacheEntry<K, V>>... filter);

    /**
     * Removes given key mapping from cache.
     * <p>
     * This method will return {@code true} if remove did occur, which means that all optionally
     * provided filters have passed and there was something to remove, {@code false} otherwise.
     * <p>
     * If write-through is enabled, the value will be removed from {@link GridCacheStore}
     * via {@link GridCacheStore#remove(GridCacheTx, Object)} method.
     * <h2 class="header">Transactions</h2>
     * This method is transactional and will enlist the entry into ongoing transaction
     * if there is one.
     * <h2 class="header">Cache Flags</h2>
     * This method is not available if any of the following flags are set on projection:
     * {@link GridCacheFlag#LOCAL}, {@link GridCacheFlag#READ}.
     *
     * @param key Key whose mapping is to be removed from cache.
     * @param filter Optional filter to check prior to removing value form cache. Note
     *      that filter is checked atomically together with remove operation.
     * @return {@code True} if filter passed validation and entry was removed, {@code false} otherwise.
     *      Note that if filter is not specified, this method will return {@code true}.
     * @throws NullPointerException if the key is {@code null}.
     * @throws GridException If remove failed.
     * @throws GridCacheFlagException If projection flags validation failed.
     */
    public boolean removex(K key, @Nullable GridPredicate<GridCacheEntry<K, V>>... filter)
        throws GridException;

    /**
     * Asynchronously removes given key mapping from cache.
     * <p>
     * This method will return {@code true} if remove did occur, which means that all optionally
     * provided filters have passed and there was something to remove, {@code false} otherwise.
     * <p>
     * If write-through is enabled, the value will be removed from {@link GridCacheStore}
     * via {@link GridCacheStore#remove(GridCacheTx, Object)} method.
     * <h2 class="header">Transactions</h2>
     * This method is transactional and will enlist the entry into ongoing transaction
     * if there is one.
     * <h2 class="header">Cache Flags</h2>
     * This method is not available if any of the following flags are set on projection:
     * {@link GridCacheFlag#LOCAL}, {@link GridCacheFlag#READ}.
     *
     * @param key Key whose mapping is to be removed from cache.
     * @param filter Optional filter to check prior to removing value form cache. Note
     *      that filter is checked atomically together with remove operation.
     * @return Future for the remove operation. The future will return {@code true}
     *      if optional filters passed validation and remove did occur, {@code false} otherwise.
     *      Note that if filter is not specified, this method will return {@code true}.
     * @throws NullPointerException if the key is {@code null}.
     * @throws GridCacheFlagException If projection flags validation failed.
     */
    public GridFuture<Boolean> removexAsync(K key,
        @Nullable GridPredicate<GridCacheEntry<K, V>>... filter);

    /**
     * Removes given key mapping from cache if one exists and value is equal to the passed in value.
     * <p>
     * If write-through is enabled, the value will be removed from {@link GridCacheStore}
     * via {@link GridCacheStore#remove(GridCacheTx, Object)} method.
     * <h2 class="header">Transactions</h2>
     * This method is transactional and will enlist the entry into ongoing transaction
     * if there is one.
     * <h2 class="header">Cache Flags</h2>
     * This method is not available if any of the following flags are set on projection:
     * {@link GridCacheFlag#LOCAL}, {@link GridCacheFlag#READ}.
     *
     * @param key Key whose mapping is to be removed from cache.
     * @param val Value to match against currently cached value.
     * @return {@code True} if entry was removed and passed in value matched the cached one,
     *      {@code false} otherwise.
     * @throws NullPointerException if the key or value is {@code null}.
     * @throws GridException If remove failed.
     * @throws GridCacheFlagException If projection flags validation failed.
     */
    public boolean remove(K key, V val) throws GridException;

    /**
     * Asynchronously removes given key mapping from cache if one exists and value is equal to the passed in value.
     * <p>
     * This method will return {@code true} if remove did occur, which means that all optionally
     * provided filters have passed and there was something to remove, {@code false} otherwise.
     * <p>
     * If write-through is enabled, the value will be removed from {@link GridCacheStore}
     * via {@link GridCacheStore#remove(GridCacheTx, Object)} method.
     * <h2 class="header">Transactions</h2>
     * This method is transactional and will enlist the entry into ongoing transaction
     * if there is one.
     * <h2 class="header">Cache Flags</h2>
     * This method is not available if any of the following flags are set on projection:
     * {@link GridCacheFlag#LOCAL}, {@link GridCacheFlag#READ}.
     *
     * @param key Key whose mapping is to be removed from cache.
     * @param val Value to match against currently cached value.
     * @return Future for the remove operation. The future will return {@code true}
     *      if currently cached value will match the passed in one.
     * @throws NullPointerException if the key or value is {@code null}.
     * @throws GridCacheFlagException If projection flags validation failed.
     */
    public GridFuture<Boolean> removeAsync(K key, V val);

    /**
     * Removes given key mappings from cache for entries for which the optionally passed in filters do
     * pass.
     * <p>
     * If write-through is enabled, the values will be removed from {@link GridCacheStore}
     * via {@link GridCacheStore#removeAll(GridCacheTx, Collection)} method.
     * <h2 class="header">Transactions</h2>
     * This method is transactional and will enlist the entry into ongoing transaction
     * if there is one.
     * <h2 class="header">Cache Flags</h2>
     * This method is not available if any of the following flags are set on projection:
     * {@link GridCacheFlag#LOCAL}, {@link GridCacheFlag#READ}.
     *
     * @param keys Keys whose mappings are to be removed from cache.
     * @param filter Optional filter to check prior to removing value form cache. Note
     *      that filter is checked atomically together with remove operation.
     * @throws GridException If remove failed.
     * @throws GridCacheFlagException If flags validation failed.
     */
    public void removeAll(@Nullable Collection<? extends K> keys,
        @Nullable GridPredicate<GridCacheEntry<K, V>>... filter) throws GridException;

    /**
     * Asynchronously removes given key mappings from cache for entries for which the optionally
     * passed in filters do pass.
     * <p>
     * If write-through is enabled, the values will be removed from {@link GridCacheStore}
     * via {@link GridCacheStore#removeAll(GridCacheTx, Collection)} method.
     * <h2 class="header">Transactions</h2>
     * This method is transactional and will enlist the entry into ongoing transaction
     * if there is one.
     * <h2 class="header">Cache Flags</h2>
     * This method is not available if any of the following flags are set on projection:
     * {@link GridCacheFlag#LOCAL}, {@link GridCacheFlag#READ}.
     *
     * @param keys Keys whose mappings are to be removed from cache.
     * @param filter Optional filter to check prior to removing value form cache. Note
     *      that filter is checked atomically together with remove operation.
     * @return Future for the remove operation. The future will complete whenever
     *      remove operation completes.
     * @throws GridCacheFlagException If flags validation failed.
     */
    public GridFuture<?> removeAllAsync(@Nullable Collection<? extends K> keys,
        @Nullable GridPredicate<GridCacheEntry<K, V>>... filter);

    /**
     * Removes mappings from cache for entries for which the optionally passed in filters do
     * pass. If passed in filters are {@code null}, then all entries in cache will be enrolled
     * into transaction.
     * <p>
     * <b>USE WITH CARE</b> - if your cache has many entries that pass through the filter or if filter
     * is empty, then transaction will quickly become very heavy and slow. Also, locks
     * are acquired in undefined order, so it may cause a deadlock when used with
     * other concurrent transactional updates.
     * <p>
     * If write-through is enabled, the values will be removed from {@link GridCacheStore}
     * via {@link GridCacheStore#removeAll(GridCacheTx, Collection)} method.
     * <h2 class="header">Transactions</h2>
     * This method is transactional and will enlist the entry into ongoing transaction
     * if there is one.
     * <h2 class="header">Cache Flags</h2>
     * This method is not available if any of the following flags are set on projection:
     * {@link GridCacheFlag#LOCAL}, {@link GridCacheFlag#READ}.
     *
     * @param filter Filter used to supply keys for remove operation (if {@code null},
     *      then nothing will be removed).
     * @throws GridException If remove failed.
     * @throws GridCacheFlagException If flags validation failed.
     */
    public void removeAll(@Nullable GridPredicate<GridCacheEntry<K, V>>... filter)
        throws GridException;

    /**
     * Asynchronously removes mappings from cache for entries for which the optionally passed in filters do
     * pass. If passed in filters are {@code null}, then all entries in cache will be enrolled
     * into transaction.
     * <p>
     * <b>USE WITH CARE</b> - if your cache has many entries that pass through the filter or if filter
     * is empty, then transaction will quickly become very heavy and slow.
     * <p>
     * If write-through is enabled, the values will be removed from {@link GridCacheStore}
     * via {@link GridCacheStore#removeAll(GridCacheTx, Collection)} method.
     * <h2 class="header">Transactions</h2>
     * This method is transactional and will enlist the entry into ongoing transaction
     * if there is one.
     * <h2 class="header">Cache Flags</h2>
     * This method is not available if any of the following flags are set on projection:
     * {@link GridCacheFlag#LOCAL}, {@link GridCacheFlag#READ}.
     *
     * @param filter Filter used to supply keys for remove operation (if {@code null},
     *      then nothing will be removed).
     * @return Future for the remove operation. The future will complete whenever
     *      remove operation completes.
     * @throws GridCacheFlagException If flags validation failed.
     */
    public GridFuture<?> removeAllAsync(@Nullable GridPredicate<GridCacheEntry<K, V>>... filter);

    /**
     * Synchronously acquires lock on a cached object with given
     * key only if the passed in filter (if any) passes. This method
     * together with filter check will be executed as one atomic operation.
     * <h2 class="header">Transactions</h2>
     * Locks are not transactional and should not be used from within transactions. If you do
     * need explicit locking within transaction, then you should use
     * {@link GridCacheTxConcurrency#PESSIMISTIC} concurrency control for transaction
     * which will acquire explicit locks for relevant cache operations.
     * <h2 class="header">Cache Flags</h2>
     * This method is not available if any of the following flags are set on projection:
     * {@link GridCacheFlag#LOCAL}, {@link GridCacheFlag#READ}.
     *
     * @param key Key to lock.
     * @param timeout Timeout in milliseconds to wait for lock to be acquired
     *      ({@code '0'} for no expiration), {@code -1} for immediate failure if
     *      lock cannot be acquired immediately).
     * @param filter Optional filter to validate prior to acquiring the lock.
     * @return {@code True} if all filters passed and lock was acquired,
     *      {@code false} otherwise.
     * @throws GridException If lock acquisition resulted in error.
     * @throws GridCacheFlagException If flags validation failed.
     */
    public boolean lock(K key, long timeout, @Nullable GridPredicate<GridCacheEntry<K, V>>... filter)
        throws GridException;

    /**
     * Asynchronously acquires lock on a cached object with given
     * key only if the passed in filter (if any) passes. This method
     * together with filter check will be executed as one atomic operation.
     * <h2 class="header">Transactions</h2>
     * Locks are not transactional and should not be used from within transactions. If you do
     * need explicit locking within transaction, then you should use
     * {@link GridCacheTxConcurrency#PESSIMISTIC} concurrency control for transaction
     * which will acquire explicit locks for relevant cache operations.
     * <h2 class="header">Cache Flags</h2>
     * This method is not available if any of the following flags are set on projection:
     * {@link GridCacheFlag#LOCAL}, {@link GridCacheFlag#READ}.
     *
     * @param key Key to lock.
     * @param timeout Timeout in milliseconds to wait for lock to be acquired
     *      ({@code '0'} for no expiration, {@code -1} for immediate failure if
     *      lock cannot be acquired immediately).
     * @param filter Optional filter to validate prior to acquiring the lock.
     * @return Future for the lock operation. The future will return {@code true}
     *      whenever all filters pass and locks are acquired before timeout is expired,
     *      {@code false} otherwise.
     * @throws GridCacheFlagException If flags validation failed.
     */
    public GridFuture<Boolean> lockAsync(K key, long timeout,
        @Nullable GridPredicate<GridCacheEntry<K, V>>... filter);

    /**
     * All or nothing synchronous lock for passed in keys. This method
     * together with filter check will be executed as one atomic operation.
     * If at least one filter validation failed, no locks will be acquired.
     * <h2 class="header">Transactions</h2>
     * Locks are not transactional and should not be used from within transactions. If you do
     * need explicit locking within transaction, then you should use
     * {@link GridCacheTxConcurrency#PESSIMISTIC} concurrency control for transaction
     * which will acquire explicit locks for relevant cache operations.
     * <h2 class="header">Cache Flags</h2>
     * This method is not available if any of the following flags are set on projection:
     * {@link GridCacheFlag#LOCAL}, {@link GridCacheFlag#READ}.
     *
     * @param keys Keys to lock.
     * @param timeout Timeout in milliseconds to wait for lock to be acquired
     *      ({@code '0'} for no expiration).
     * @param filter Optional filter that needs to atomically pass in order for the locks
     *      to be acquired.
     * @return {@code True} if all filters passed and locks were acquired before
     *      timeout has expired, {@code false} otherwise.
     * @throws GridException If lock acquisition resulted in error.
     * @throws GridCacheFlagException If flags validation failed.
     */
    public boolean lockAll(@Nullable Collection<? extends K> keys, long timeout,
        @Nullable GridPredicate<GridCacheEntry<K, V>>... filter) throws GridException;

    /**
     * All or nothing synchronous lock for passed in keys. This method
     * together with filter check will be executed as one atomic operation.
     * If at least one filter validation failed, no locks will be acquired.
     * <h2 class="header">Transactions</h2>
     * Locks are not transactional and should not be used from within transactions. If you do
     * need explicit locking within transaction, then you should use
     * {@link GridCacheTxConcurrency#PESSIMISTIC} concurrency control for transaction
     * which will acquire explicit locks for relevant cache operations.
     * <h2 class="header">Cache Flags</h2>
     * This method is not available if any of the following flags are set on projection:
     * {@link GridCacheFlag#LOCAL}, {@link GridCacheFlag#READ}.
     *
     * @param keys Keys to lock.
     * @param timeout Timeout in milliseconds to wait for lock to be acquired
     *      ({@code '0'} for no expiration).
     * @param filter Optional filter that needs to atomically pass in order for the locks
     *      to be acquired.
     * @return Future for the collection of locks. The future will return
     *      {@code true} if all filters passed and locks were acquired before
     *      timeout has expired, {@code false} otherwise.
     * @throws GridCacheFlagException If flags validation failed.
     */
    public GridFuture<Boolean> lockAllAsync(@Nullable Collection<? extends K> keys, long timeout,
        @Nullable GridPredicate<GridCacheEntry<K, V>>... filter);

    /**
     * Unlocks given key only if current thread owns the lock. If optional filter
     * will not pass, then unlock will not happen. If the key being unlocked was
     * never locked by current thread, then this method will do nothing.
     * <h2 class="header">Transactions</h2>
     * Locks are not transactional and should not be used from within transactions. If you do
     * need explicit locking within transaction, then you should use
     * {@link GridCacheTxConcurrency#PESSIMISTIC} concurrency control for transaction
     * which will acquire explicit locks for relevant cache operations.
     * <h2 class="header">Cache Flags</h2>
     * This method is not available if any of the following flags are set on projection:
     * {@link GridCacheFlag#LOCAL}, {@link GridCacheFlag#READ}.
     *
     * @param key Key to unlock.
     * @param filter Optional filter that needs to pass prior to unlock taking effect.
     * @throws GridException If unlock execution resulted in error.
     * @throws GridCacheFlagException If flags validation failed.
     */
    public void unlock(K key, GridPredicate<GridCacheEntry<K, V>>... filter) throws GridException;

    /**
     * Unlocks given keys only if current thread owns the locks. Only the keys
     * that have been locked by calling thread and pass through the filter (if any)
     * will be unlocked. If none of the key locks is owned by current thread, then
     * this method will do nothing.
     * <h2 class="header">Transactions</h2>
     * Locks are not transactional and should not be used from within transactions. If you do
     * need explicit locking within transaction, then you should use
     * {@link GridCacheTxConcurrency#PESSIMISTIC} concurrency control for transaction
     * which will acquire explicit locks for relevant cache operations.
     * <h2 class="header">Cache Flags</h2>
     * This method is not available if any of the following flags are set on projection:
     * {@link GridCacheFlag#LOCAL}, {@link GridCacheFlag#READ}.
     *
     * @param keys Keys to unlock.
     * @param filter Optional filter which needs to pass for individual entries
     *      to be unlocked.
     * @throws GridException If unlock execution resulted in error.
     * @throws GridCacheFlagException If flags validation failed.
     */
    public void unlockAll(@Nullable Collection<? extends K> keys,
        @Nullable GridPredicate<GridCacheEntry<K, V>>... filter) throws GridException;

    /**
     * Checks if any node owns a lock for this key.
     * <p>
     * This is a local in-VM operation and does not involve any network trips
     * or access to persistent storage in any way.
     *
     * @param key Key to check.
     * @return {@code True} if lock is owned by some node.
     */
    public boolean isLocked(K key);

    /**
     * Checks if current thread owns a lock on this key.
     * <p>
     * This is a local in-VM operation and does not involve any network trips
     * or access to persistent storage in any way.
     *
     * @param key Key to check.
     * @return {@code True} if key is locked by current thread.
     */
    public boolean isLockedByThread(K key);

    /**
     * Gets the number of all entries cached on this node. This method will return the count of
     * all cache entries and has O(1) complexity on base {@link GridCache} projection. It is essentially the
     * size of cache key set and is semantically identical to {{@code GridCache.keySet().size()}.
     * <p>
     * NOTE: this operation is not distributed and returns only the number of entries cached on this node.
     *
     * @return Size of cache on this node.
     */
    public int size();

    /**
     * Gets the number of all entries cached across all nodes.
     * <p>
     * NOTE: this operation is distributed and will query all participating nodes for their cache sizes.
     *
     * @return Total cache size across all nodes.
     */
    public int globalSize() throws GridException;

    /**
     * Gets size of near cache key set. This method will return count of all entries in near
     * cache and has O(1) complexity on base cache projection.
     * <p>
     * Note that for {@code LOCAL} non-distributed caches this method will always return {@code 0}
     *
     * @return Size of near cache key set or {@code 0} if cache is not {@link GridCacheMode#PARTITIONED}.
     */
    public int nearSize();

    /**
     * Gets the number of all primary entries cached on this node. For {@link GridCacheMode#LOCAL} non-distributed
     * cache mode, this method is identical to {@link #size()}.
     * <p>
     * For {@link GridCacheMode#PARTITIONED} and {@link GridCacheMode#REPLICATED} modes, this method will
     * return number of primary entries cached on this node (excluding any backups). The complexity of
     * this method is O(P), where P is the total number of partitions.
     * <p>
     * NOTE: this operation is not distributed and returns only the number of primary entries cached on this node.
     *
     * @return Number of primary entries in cache.
     */
    public int primarySize();

    /**
     * Gets the number of all primary entries cached across all nodes (excluding backups).
     * <p>
     * NOTE: this operation is distributed and will query all participating nodes for their primary cache sizes.
     *
     * @return Total primary cache size across all nodes.
     */
    public int globalPrimarySize() throws GridException;

    /**
     * This method promotes cache entry by given key, if any, from offheap or swap storage
     * into memory.
     * <h2 class="header">Transactions</h2>
     * This method is not transactional.
     * <h2 class="header">Cache Flags</h2>
     * This method is not available if any of the following flags are set on projection:
     * {@link GridCacheFlag#SKIP_SWAP}, {@link GridCacheFlag#READ}.
     *
     * @param key Key to promote entry for.
     * @return Unswapped entry value or {@code null} for non-existing key.
     * @throws GridException If promote failed.
     * @throws GridCacheFlagException If flags validation failed.
     */
    @Nullable public V promote(K key) throws GridException;

    /**
     * This method unswaps cache entries by given keys, if any, from swap storage
     * into memory.
     * <h2 class="header">Transactions</h2>
     * This method is not transactional.
     * <h2 class="header">Cache Flags</h2>
     * This method is not available if any of the following flags are set on projection:
     * {@link GridCacheFlag#SKIP_SWAP}, {@link GridCacheFlag#READ}.
     *
     * @param keys Keys to promote entries for.
     * @throws GridException If promote failed.
     * @throws GridCacheFlagException If flags validation failed.
     */
    public void promoteAll(@Nullable Collection<? extends K> keys) throws GridException;
}
