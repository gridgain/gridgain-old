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

package org.gridgain.grid.dataload;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.product.*;
import org.jetbrains.annotations.*;

import java.util.*;

import static org.gridgain.grid.product.GridProductEdition.*;

/**
 * Data loader is responsible for loading external data into cache. It achieves it by
 * properly buffering updates and properly mapping keys to nodes responsible for the data
 * to make sure that there is the least amount of data movement possible and optimal
 * network and memory utilization.
 * <p>
 * Note that loader will load data concurrently by multiple internal threads, so the
 * data may get to remote nodes in different order from which it was added to
 * the loader.
 * <p>
 * Also note that {@code GridDataLoader} is not the only way to load data into cache.
 * Alternatively you can use {@link GridCache#loadCache(GridBiPredicate, long, Object...)}
 * method to load data from underlying data store. You can also use standard
 * cache {@code put(...)} and {@code putAll(...)} operations as well, but they most
 * likely will not perform as well as this class for loading data. And finally,
 * data can be loaded from underlying data store on demand, whenever it is accessed -
 * for this no explicit data loading step is needed.
 * <p>
 * {@code GridDataLoader} supports the following configuration properties:
 * <ul>
 *  <li>
 *      {@link #perNodeBufferSize(int)} - when entries are added to data loader via
 *      {@link #addData(Object, Object)} method, they are not sent to in-memory data grid right
 *      away and are buffered internally for better performance and network utilization.
 *      This setting controls the size of internal per-node buffer before buffered data
 *      is sent to remote node. Default is defined by {@link #DFLT_PER_NODE_BUFFER_SIZE}
 *      value.
 *  </li>
 *  <li>
 *      {@link #perNodeParallelLoadOperations(int)} - sometimes data may be added
 *      to the data loader via {@link #addData(Object, Object)} method faster than it can
 *      be put in cache. In this case, new buffered load messages are sent to remote nodes
 *      before responses from previous ones are received. This could cause unlimited heap
 *      memory utilization growth on local and remote nodes. To control memory utilization,
 *      this setting limits maximum allowed number of parallel buffered load messages that
 *      are being processed on remote nodes. If this number is exceeded, then
 *      {@link #addData(Object, Object)} method will block to control memory utilization.
 *      Default is defined by {@link #DFLT_MAX_PARALLEL_OPS} value.
 *  </li>
 *  <li>
 *      {@link #autoFlushFrequency(long)} - automatic flush frequency in milliseconds. Essentially,
 *      this is the time after which the loader will make an attempt to submit all data
 *      added so far to remote nodes. Note that there is no guarantee that data will be
 *      delivered after this concrete attempt (e.g., it can fail when topology is
 *      changing), but it won't be lost anyway. Disabled by default (default value is {@code 0}).
 *  </li>
 *  <li>
 *      {@link #isolated(boolean)} - defines if data loader will assume that there are no other concurrent
 *      updates and allow data loader choose most optimal concurrent implementation.
 *  </li>
 *  <li>
 *      {@link #updater(GridDataLoadCacheUpdater)} - defines how cache will be updated with loaded entries.
 *      It allows to provide user-defined custom logic to update the cache in the most effective and flexible way.
 *  </li>
 *  <li>
 *      {@link #deployClass(Class)} - optional deploy class for peer deployment. All classes
 *      loaded by a data loader must be class-loadable from the same class-loader.
 *      GridGain will make the best effort to detect the most suitable class-loader
 *      for data loading. However, in complex cases, where compound or deeply nested
 *      class-loaders are used, it is best to specify a deploy class which can be any
 *      class loaded by the class-loader for given data.
 *  </li>
 * </ul>
 */
@GridOnlyAvailableIn(DATA_GRID)
public interface GridDataLoader<K, V> extends AutoCloseable {
    /** Default max concurrent put operations count. */
    public static final int DFLT_MAX_PARALLEL_OPS = 16;

    /** Default per node buffer size. */
    public static final int DFLT_PER_NODE_BUFFER_SIZE = 1024;

    /**
     * Name of cache to load data to.
     *
     * @return Cache name or {@code null} for default cache.
     */
    @Nullable public String cacheName();

    /**
     * Gets flag value indicating that this data loader assumes that there are no other concurrent updates to the cache.
     * Default is {@code false}.
     *
     * @return Flag value.
     */
    public boolean isolated();

    /**
     * Sets flag indicating that this data loader should assume that there are no other concurrent updates to the cache.
     * Should not be used when custom cache updater set using {@link #updater(GridDataLoadCacheUpdater)} method.
     * Default is {@code false}.
     *
     * @param isolated Flag value.
     * @throws GridException If failed.
     */
    public void isolated(boolean isolated) throws GridException;

    /**
     * Gets size of per node key-value pairs buffer.
     *
     * @return Per node buffer size.
     */
    public int perNodeBufferSize();

    /**
     * Sets size of per node key-value pairs buffer.
     * <p>
     * This method should be called prior to {@link #addData(Object, Object)} call.
     * <p>
     * If not provided, default value is {@link #DFLT_PER_NODE_BUFFER_SIZE}.
     *
     * @param bufSize Per node buffer size.
     */
    public void perNodeBufferSize(int bufSize);

    /**
     * Gets maximum number of parallel load operations for a single node.
     *
     * @return Maximum number of parallel load operations for a single node.
     */
    public int perNodeParallelLoadOperations();

    /**
     * Sets maximum number of parallel load operations for a single node.
     * <p>
     * This method should be called prior to {@link #addData(Object, Object)} call.
     * <p>
     * If not provided, default value is {@link #DFLT_MAX_PARALLEL_OPS}.
     *
     * @param parallelOps Maximum number of parallel load operations for a single node.
     */
    public void perNodeParallelLoadOperations(int parallelOps);

    /**
     * Gets automatic flush frequency. Essentially, this is the time after which the
     * loader will make an attempt to submit all data added so far to remote nodes.
     * Note that there is no guarantee that data will be delivered after this concrete
     * attempt (e.g., it can fail when topology is changing), but it won't be lost anyway.
     * <p>
     * If set to {@code 0}, automatic flush is disabled.
     * <p>
     * Automatic flush is disabled by default (default value is {@code 0}).
     *
     * @return Flush frequency or {@code 0} if automatic flush is disabled.
     * @see #flush()
     */
    public long autoFlushFrequency();

    /**
     * Sets automatic flush frequency. Essentially, this is the time after which the
     * loader will make an attempt to submit all data added so far to remote nodes.
     * Note that there is no guarantee that data will be delivered after this concrete
     * attempt (e.g., it can fail when topology is changing), but it won't be lost anyway.
     * <p>
     * If set to {@code 0}, automatic flush is disabled.
     * <p>
     * Automatic flush is disabled by default (default value is {@code 0}).
     *
     * @param autoFlushFreq Flush frequency or {@code 0} to disable automatic flush.
     * @see #flush()
     */
    public void autoFlushFrequency(long autoFlushFreq);

    /**
     * Gets future for this loading process. This future completes whenever method
     * {@link #close(boolean)} completes. By attaching listeners to this future
     * it is possible to get asynchronous notifications for completion of this
     * loading process.
     *
     * @return Future for this loading process.
     */
    public GridFuture<?> future();

    /**
     * Optional deploy class for peer deployment. All classes loaded by a data loader
     * must be class-loadable from the same class-loader. GridGain will make the best
     * effort to detect the most suitable class-loader for data loading. However,
     * in complex cases, where compound or deeply nested class-loaders are used,
     * it is best to specify a deploy class which can be any class loaded by
     * the class-loader for given data.
     *
     * @param depCls Any class loaded by the class-loader for given data.
     */
    public void deployClass(Class<?> depCls);

    /**
     * Sets custom cache updater to this data loader.
     *
     * @param updater Cache updater.
     */
    public void updater(GridDataLoadCacheUpdater<K, V> updater);

    /**
     * Adds key for removal on remote node. Equivalent to {@link #addData(Object, Object) addData(key, null)}.
     *
     * @param key Key.
     * @return Future fo this operation.
     * @throws GridException If failed to map key to node.
     * @throws GridInterruptedException If thread has been interrupted.
     * @throws IllegalStateException If grid has been concurrently stopped or
     *      {@link #close(boolean)} has already been called on loader.
     */
    public GridFuture<?> removeData(K key)  throws GridException, GridInterruptedException, IllegalStateException;

    /**
     * Adds data for loading on remote node. This method can be called from multiple
     * threads in parallel to speed up loading if needed.
     * <p>
     * Note that loader will load data concurrently by multiple internal threads, so the
     * data may get to remote nodes in different order from which it was added to
     * the loader.
     *
     * @param key Key.
     * @param val Value or {@code null} if respective entry must be removed from cache.
     * @return Future fo this operation.
     * @throws GridException If failed to map key to node.
     * @throws GridInterruptedException If thread has been interrupted.
     * @throws IllegalStateException If grid has been concurrently stopped or
     *      {@link #close(boolean)} has already been called on loader.
     */
    public GridFuture<?> addData(K key, @Nullable V val) throws GridException, GridInterruptedException,
        IllegalStateException;

    /**
     * Adds data for loading on remote node. This method can be called from multiple
     * threads in parallel to speed up loading if needed.
     * <p>
     * Note that loader will load data concurrently by multiple internal threads, so the
     * data may get to remote nodes in different order from which it was added to
     * the loader.
     *
     * @param entry Entry.
     * @return Future fo this operation.
     * @throws GridException If failed to map key to node.
     * @throws GridInterruptedException If thread has been interrupted.
     * @throws IllegalStateException If grid has been concurrently stopped or
     *      {@link #close(boolean)} has already been called on loader.
     */
    public GridFuture<?> addData(Map.Entry<K, V> entry) throws GridException, GridInterruptedException,
        IllegalStateException;

    /**
     * Adds data for loading on remote node. This method can be called from multiple
     * threads in parallel to speed up loading if needed.
     * <p>
     * Note that loader will load data concurrently by multiple internal threads, so the
     * data may get to remote nodes in different order from which it was added to
     * the loader.
     *
     * @param entries Collection of entries to be loaded.
     * @throws IllegalStateException If grid has been concurrently stopped or
     *      {@link #close(boolean)} has already been called on loader.
     * @return Future for this load operation.
     */
    public GridFuture<?> addData(Collection<? extends Map.Entry<K, V>> entries) throws IllegalStateException;

    /**
     * Adds data for loading on remote node. This method can be called from multiple
     * threads in parallel to speed up loading if needed.
     * <p>
     * Note that loader will load data concurrently by multiple internal threads, so the
     * data may get to remote nodes in different order from which it was added to
     * the loader.
     *
     * @param entries Map to be loaded.
     * @throws IllegalStateException If grid has been concurrently stopped or
     *      {@link #close(boolean)} has already been called on loader.
     * @return Future for this load operation.
     */
    public GridFuture<?> addData(Map<K, V> entries) throws IllegalStateException;

    /**
     * Loads any remaining data, but doesn't close the loader. Data can be still added after
     * flush is finished. This method blocks and doesn't allow to add any data until all data
     * is loaded.
     * <p>
     * If another thread is already performing flush, this method will block, wait for
     * another thread to complete flush and exit. If you don't want to wait in this case,
     * use {@link #tryFlush()} method.
     *
     * @throws GridException If failed to map key to node.
     * @throws GridInterruptedException If thread has been interrupted.
     * @throws IllegalStateException If grid has been concurrently stopped or
     *      {@link #close(boolean)} has already been called on loader.
     * @see #tryFlush()
     */
    public void flush() throws GridException, GridInterruptedException, IllegalStateException;

    /**
     * Makes an attempt to load remaining data. This method is mostly similar to {@link #flush},
     * with the difference that it won't wait and will exit immediately.
     *
     * @throws GridException If failed to map key to node.
     * @throws GridInterruptedException If thread has been interrupted.
     * @throws IllegalStateException If grid has been concurrently stopped or
     *      {@link #close(boolean)} has already been called on loader.
     * @see #flush()
     */
    public void tryFlush() throws GridException, GridInterruptedException, IllegalStateException;

    /**
     * Loads any remaining data and closes this loader.
     *
     * @param cancel {@code True} to cancel ongoing loading operations.
     * @throws GridException If failed to map key to node.
     * @throws GridInterruptedException If thread has been interrupted.
     */
    public void close(boolean cancel) throws GridException, GridInterruptedException;

    /**
     * Closes data loader. This method is identical to calling {@link #close(boolean) close(false)} method.
     * <p>
     * The method is invoked automatically on objects managed by the
     * {@code try-with-resources} statement.
     *
     * @throws GridException If failed to close data loader.
     * @throws GridInterruptedException If thread has been interrupted.
     */
    @Override public void close() throws GridException, GridInterruptedException;
}
