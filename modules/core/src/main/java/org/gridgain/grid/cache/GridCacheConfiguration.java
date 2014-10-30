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
import org.gridgain.grid.cache.affinity.consistenthash.*;
import org.gridgain.grid.cache.cloner.*;
import org.gridgain.grid.cache.datastructures.*;
import org.gridgain.grid.cache.eviction.*;
import org.gridgain.grid.cache.query.*;
import org.gridgain.grid.cache.store.*;
import org.gridgain.grid.dr.cache.receiver.*;
import org.gridgain.grid.dr.cache.sender.*;
import org.gridgain.grid.portables.*;
import org.gridgain.grid.spi.indexing.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * This class defines grid cache configuration. This configuration is passed to
 * grid via {@link GridConfiguration#getCacheConfiguration()} method. It defines all configuration
 * parameters required to start a cache within grid instance. You can have multiple caches
 * configured with different names within one grid.
 * <p>
 * Cache configuration is set on {@link
 * GridConfiguration#setCacheConfiguration(GridCacheConfiguration...)} method. This adapter is a simple bean and
 * can be configured from Spring XML files (or other DI frameworks). <p> Note that absolutely all configuration
 * properties are optional, so users should only change what they need.
 */
@SuppressWarnings("RedundantFieldInitialization")
public class GridCacheConfiguration {
    /** DGC tracing logger name. */
    public static final String DGC_TRACE_LOGGER_NAME =
        "org.gridgain.grid.kernal.processors.cache.GridCacheDgcManager.trace";

    /** Default atomic sequence reservation size. */
    public static final int DFLT_ATOMIC_SEQUENCE_RESERVE_SIZE = 1000;

    /** Default size of preload thread pool. */
    public static final int DFLT_PRELOAD_THREAD_POOL_SIZE = 2;

    /** Default preload timeout (ms).*/
    public static final long DFLT_PRELOAD_TIMEOUT = 10000;

    /** Time in milliseconds to wait between preload messages to avoid overloading CPU. */
    public static final long DFLT_PRELOAD_THROTTLE = 0;

    /**
     * Default time to live. The value is <tt>0</tt> which means that
     * cached objects never expire based on time.
     */
    public static final long DFLT_TIME_TO_LIVE = 0;

    /** Default number of backups. */
    public static final int DFLT_BACKUPS = 0;

    /** Default caching mode. */
    public static final GridCacheMode DFLT_CACHE_MODE = GridCacheMode.REPLICATED;

    /** Default atomicity mode. */
    public static final GridCacheAtomicityMode DFLT_CACHE_ATOMICITY_MODE = GridCacheAtomicityMode.ATOMIC;

    /** Default value for cache distribution mode. */
    public static final GridCacheDistributionMode DFLT_DISTRIBUTION_MODE = GridCacheDistributionMode.PARTITIONED_ONLY;

    /** Default transaction timeout. */
    public static final long DFLT_TRANSACTION_TIMEOUT = 0;

    /** Default query timeout. */
    public static final long DFLT_QUERY_TIMEOUT = 0;

    /** Default lock timeout. */
    public static final long DFLT_LOCK_TIMEOUT = 0;

    /** Default concurrency mode. */
    public static final GridCacheTxConcurrency DFLT_TX_CONCURRENCY = GridCacheTxConcurrency.PESSIMISTIC;

    /** Default transaction isolation level. */
    public static final GridCacheTxIsolation DFLT_TX_ISOLATION = GridCacheTxIsolation.REPEATABLE_READ;

    /** Initial default cache size. */
    public static final int DFLT_START_SIZE = 1500000;

    /** Default cache size to use with eviction policy. */
    public static final int DFLT_CACHE_SIZE = 100000;

    /** Initial default near cache size. */
    public static final int DFLT_NEAR_START_SIZE = DFLT_START_SIZE / 4;

    /** Default value for 'txSerializableEnabled' flag. */
    public static final boolean DFLT_TX_SERIALIZABLE_ENABLED = false;

    /** Default value for 'txBatchUpdate' flag. */
    public static final boolean DFLT_TX_BATCH_UPDATE = true;

    /** Default value for 'invalidate' flag that indicates if this is invalidation-based cache. */
    public static final boolean DFLT_INVALIDATE = false;

    /** Default value for 'storeValueBytes' flag indicating if value bytes should be stored. */
    public static final boolean DFLT_STORE_VALUE_BYTES = true;

    /** Default size of pessimistic transactions log. */
    public static final int DFLT_PESSIMISTIC_TX_LOG_LINGER = 10_000;

    /** Default preload mode for distributed cache. */
    public static final GridCachePreloadMode DFLT_PRELOAD_MODE = GridCachePreloadMode.ASYNC;

    /** Default preload batch size in bytes. */
    public static final int DFLT_PRELOAD_BATCH_SIZE = 512 * 1024; // 512K

    /** Default distributed garbage collection frequency. */
    public static final long DFLT_DGC_FREQUENCY = 10000;

    /** Default timeout for lock not to be considered as suspicious. */
    public static final long DFLT_DGC_SUSPECT_LOCK_TIMEOUT = 10000;

    /** Default value for whether DGC should remove long running locks, or only report them. */
    public static final boolean DFLT_DGC_REMOVE_LOCKS = true;

    /** Default maximum eviction queue ratio. */
    public static final float DFLT_MAX_EVICTION_OVERFLOW_RATIO = 10;

    /** Default eviction synchronized flag. */
    public static final boolean DFLT_EVICT_SYNCHRONIZED = false;

    /** Default near nodes eviction synchronized flag. */
    public static final boolean DFLT_EVICT_NEAR_SYNCHRONIZED = true;

    /** Default eviction key buffer size for batching synchronized evicts. */
    public static final int DFLT_EVICT_KEY_BUFFER_SIZE = 1024;

    /** Default synchronous eviction timeout in milliseconds. */
    public static final long DFLT_EVICT_SYNCHRONIZED_TIMEOUT = 10000;

    /** Default synchronous eviction concurrency level. */
    public static final int DFLT_EVICT_SYNCHRONIZED_CONCURRENCY_LEVEL = 4;

    /** Default value for eager ttl flag. */
    public static final boolean DFLT_EAGER_TTL = true;

    /** Default off-heap storage size is {@code -1} which means that off-heap storage is disabled. */
    public static final long DFLT_OFFHEAP_MEMORY = -1;

    /** Default value for 'swapEnabled' flag. */
    public static final boolean DFLT_SWAP_ENABLED = false;

    /** Default value for 'maxConcurrentAsyncOps'. */
    public static final int DFLT_MAX_CONCURRENT_ASYNC_OPS = 500;

    /** Default value for 'queryIndexEnabled' flag. */
    public static final boolean DFLT_QUERY_INDEX_ENABLED = false;

    /** Default value for 'writeBehindEnabled' flag. */
    public static final boolean DFLT_WRITE_BEHIND_ENABLED = false;

    /** Default flush size for write-behind cache store. */
    public static final int DFLT_WRITE_BEHIND_FLUSH_SIZE = 10240; // 10K

    /** Default critical size used when flush size is not specified. */
    public static final int DFLT_WRITE_BEHIND_CRITICAL_SIZE = 16384; // 16K

    /** Default flush frequency for write-behind cache store in milliseconds. */
    public static final long DFLT_WRITE_BEHIND_FLUSH_FREQUENCY = 5000;

    /** Default count of flush threads for write-behind cache store. */
    public static final int DFLT_WRITE_FROM_BEHIND_FLUSH_THREAD_CNT = 1;

    /** Default batch size for write-behind cache store. */
    public static final int DFLT_WRITE_BEHIND_BATCH_SIZE = 512;

    /** Default maximum number of query iterators that can be stored. */
    public static final int DFLT_MAX_QUERY_ITERATOR_CNT = 1024;

    /** Default continuous query buffers queue size. */
    @SuppressWarnings("UnusedDeclaration")
    @Deprecated
    public static final int DFLT_CONT_QUERY_QUEUE_SIZE = 1024 * 1024;

    /** Default memory mode. */
    public static final GridCacheMemoryMode DFLT_MEMORY_MODE = GridCacheMemoryMode.ONHEAP_TIERED;

    /** Default continuous query max buffer size. */
    @SuppressWarnings("UnusedDeclaration")
    @Deprecated
    public static final int DFLT_CONT_QUERY_MAX_BUF_SIZE = 1024;

    /** Cache name. */
    private String name;

    /** Default batch size for all cache's sequences. */
    private int seqReserveSize = DFLT_ATOMIC_SEQUENCE_RESERVE_SIZE;

    /** Preload thread pool size. */
    private int preloadPoolSize = DFLT_PRELOAD_THREAD_POOL_SIZE;

    /** Preload timeout. */
    private long preloadTimeout = DFLT_PRELOAD_TIMEOUT;

    /** Default time to live for cache entries. */
    private long ttl = DFLT_TIME_TO_LIVE;

    /** Cache expiration policy. */
    private GridCacheEvictionPolicy evictPlc;

    /** Near cache eviction policy. */
    private GridCacheEvictionPolicy nearEvictPlc;

    /** Flag indicating whether eviction is synchronized. */
    private boolean evictSync = DFLT_EVICT_SYNCHRONIZED;

    /** Flag indicating whether eviction is synchronized with near nodes. */
    private boolean evictNearSync = DFLT_EVICT_NEAR_SYNCHRONIZED;

    /** Eviction key buffer size. */
    private int evictKeyBufSize = DFLT_EVICT_KEY_BUFFER_SIZE;

    /** Synchronous eviction timeout. */
    private int evictSyncConcurrencyLvl = DFLT_EVICT_SYNCHRONIZED_CONCURRENCY_LEVEL;

    /** Synchronous eviction timeout. */
    private long evictSyncTimeout = DFLT_EVICT_SYNCHRONIZED_TIMEOUT;

    /** Eviction filter. */
    private GridCacheEvictionFilter<?, ?> evictFilter;

    /** Maximum eviction overflow ratio. */
    private float evictMaxOverflowRatio = DFLT_MAX_EVICTION_OVERFLOW_RATIO;

    /** Eager ttl flag. */
    private boolean eagerTtl = DFLT_EAGER_TTL;

    /** Transaction isolation. */
    private GridCacheTxIsolation dfltIsolation = DFLT_TX_ISOLATION;

    /** Cache concurrency. */
    private GridCacheTxConcurrency dfltConcurrency = DFLT_TX_CONCURRENCY;

    /** Default transaction serializable flag. */
    private boolean txSerEnabled = DFLT_TX_SERIALIZABLE_ENABLED;

    /** Default transaction timeout. */
    private long dfltTxTimeout = DFLT_TRANSACTION_TIMEOUT;

    /** Default lock timeout. */
    private long dfltLockTimeout = DFLT_LOCK_TIMEOUT;

    /** Default query timeout. */
    private long dfltQryTimeout = DFLT_QUERY_TIMEOUT;

    /** Default cache start size. */
    private int startSize = DFLT_START_SIZE;

    /** Default near cache start size. */
    private int nearStartSize = DFLT_NEAR_START_SIZE;

    /** Cache distribution mode. */
    private GridCacheDistributionMode distro = DFLT_DISTRIBUTION_MODE;

    /** Write synchronization mode. */
    private GridCacheWriteSynchronizationMode writeSync;

    /** */
    private GridCacheStore<?, ?> store;

    /** Node group resolver. */
    private GridCacheAffinityFunction aff;

    /** Cache mode. */
    private GridCacheMode cacheMode = DFLT_CACHE_MODE;

    /** Cache atomicity mode. */
    private GridCacheAtomicityMode atomicityMode;

    /** Write ordering mode. */
    private GridCacheAtomicWriteOrderMode atomicWriteOrderMode;

    /** Number of backups for cache. */
    private int backups = DFLT_BACKUPS;

    /** Flag to enable transactional batch update. */
    private boolean txBatchUpdate = DFLT_TX_BATCH_UPDATE;

    /** Flag indicating whether this is invalidation-based cache. */
    private boolean invalidate = DFLT_INVALIDATE;

    /** Flag indicating if cached values should be additionally stored in serialized form. */
    private boolean storeValBytes = DFLT_STORE_VALUE_BYTES;

    /** Refresh-ahead ratio. */
    private double refreshAheadRatio;

    /** Pessimistic tx log size. */
    private int pessimisticTxLogSize;

    /** Pessimistic tx log linger. */
    private int pessimisticTxLogLinger = DFLT_PESSIMISTIC_TX_LOG_LINGER;

    /** Name of class implementing GridCacheTmLookup. */
    private String tmLookupClsName;

    /** Distributed cache preload mode. */
    private GridCachePreloadMode preloadMode = DFLT_PRELOAD_MODE;

    /** Cache preload order. */
    private int preloadOrder;

    /** Preload batch size. */
    private int preloadBatchSize = DFLT_PRELOAD_BATCH_SIZE;

    /** Distributed garbage collection frequency. */
    private long dgcFreq = DFLT_DGC_FREQUENCY;

    /** Timeout after which DGC will consider remote locks as suspects. */
    private long dgcSuspectLockTimeout = DFLT_DGC_SUSPECT_LOCK_TIMEOUT;

    /** Flag indicating whether DGC should remove locks. */
    private boolean dgcRmvLocks = DFLT_DGC_REMOVE_LOCKS;

    /** Off-heap memory size. */
    private long offHeapMaxMem = DFLT_OFFHEAP_MEMORY;

    /** */
    private boolean swapEnabled = DFLT_SWAP_ENABLED;

    /** Maximum number of concurrent asynchronous operations. */
    private int maxConcurrentAsyncOps = DFLT_MAX_CONCURRENT_ASYNC_OPS;

    /** */
    private boolean qryIdxEnabled = DFLT_QUERY_INDEX_ENABLED;

    /** Write-behind feature. */
    private boolean writeBehindEnabled = DFLT_WRITE_BEHIND_ENABLED;

    /** Maximum size of write-behind cache. */
    private int writeBehindFlushSize = DFLT_WRITE_BEHIND_FLUSH_SIZE;

    /** Write-behind flush frequency in milliseconds. */
    private long writeBehindFlushFreq = DFLT_WRITE_BEHIND_FLUSH_FREQUENCY;

    /** Flush thread count for write-behind cache store. */
    private int writeBehindFlushThreadCnt = DFLT_WRITE_FROM_BEHIND_FLUSH_THREAD_CNT;

    /** Maximum batch size for write-behind cache store. */
    private int writeBehindBatchSize = DFLT_WRITE_BEHIND_BATCH_SIZE;

    /** Maximum number of query iterators that can be stored. */
    private int maxQryIterCnt = DFLT_MAX_QUERY_ITERATOR_CNT;

    /** Memory mode. */
    private GridCacheMemoryMode memMode = DFLT_MEMORY_MODE;

    /** */
    private GridCacheCloner cloner;

    /** */
    private GridCacheAffinityKeyMapper affMapper;

    /** */
    private String indexingSpiName;

    /** */
    private long preloadDelay;

    /** */
    private long preloadThrottle = DFLT_PRELOAD_THROTTLE;

    /** */
    private GridDrReceiverCacheConfiguration drRcvCacheCfg;

    /** */
    private GridDrSenderCacheConfiguration drSndCacheCfg;

    /** */
    private GridCacheInterceptor<?, ?> interceptor;

    /** */
    private boolean portableEnabled;

    /** Query configuration. */
    private GridCacheQueryConfiguration qryCfg;

    /** Empty constructor (all values are initialized to their defaults). */
    public GridCacheConfiguration() {
        /* No-op. */
    }

    /**
     * Copy constructor.
     *
     * @param cc Configuration to copy.
     */
    public GridCacheConfiguration(GridCacheConfiguration cc) {
        /*
         * NOTE: MAKE SURE TO PRESERVE ALPHABETIC ORDER!
         * ==============================================
         */
        aff = cc.getAffinity();
        affMapper = cc.getAffinityMapper();
        atomicityMode = cc.getAtomicityMode();
        atomicWriteOrderMode = cc.getAtomicWriteOrderMode();
        backups = cc.getBackups();
        cacheMode = cc.getCacheMode();
        cloner = cc.getCloner();
        dfltConcurrency = cc.getDefaultTxConcurrency();
        dfltIsolation = cc.getDefaultTxIsolation();
        dfltLockTimeout = cc.getDefaultLockTimeout();
        dfltQryTimeout = cc.getDefaultQueryTimeout();
        dfltTxTimeout = cc.getDefaultTxTimeout();
        dgcFreq = cc.getDgcFrequency();
        dgcRmvLocks = cc.isDgcRemoveLocks();
        dgcSuspectLockTimeout = cc.getDgcSuspectLockTimeout();
        distro = cc.getDistributionMode();
        drSndCacheCfg = cc.getDrSenderConfiguration() != null ?
            new GridDrSenderCacheConfiguration(cc.getDrSenderConfiguration()) : null;
        drRcvCacheCfg = cc.getDrReceiverConfiguration() != null ?
            new GridDrReceiverCacheConfiguration(cc.getDrReceiverConfiguration()) : null;
        eagerTtl = cc.isEagerTtl();
        evictFilter = cc.getEvictionFilter();
        evictKeyBufSize = cc.getEvictSynchronizedKeyBufferSize();
        evictMaxOverflowRatio = cc.getEvictMaxOverflowRatio();
        evictNearSync = cc.isEvictNearSynchronized();
        evictPlc = cc.getEvictionPolicy();
        evictSync = cc.isEvictSynchronized();
        evictSyncConcurrencyLvl = cc.getEvictSynchronizedConcurrencyLevel();
        evictSyncTimeout = cc.getEvictSynchronizedTimeout();
        indexingSpiName = cc.getIndexingSpiName();
        interceptor = cc.getInterceptor();
        invalidate = cc.isInvalidate();
        offHeapMaxMem = cc.getOffHeapMaxMemory();
        maxConcurrentAsyncOps = cc.getMaxConcurrentAsyncOperations();
        maxQryIterCnt = cc.getMaximumQueryIteratorCount();
        memMode = cc.getMemoryMode();
        name = cc.getName();
        nearStartSize = cc.getNearStartSize();
        nearEvictPlc = cc.getNearEvictionPolicy();
        pessimisticTxLogLinger = cc.getPessimisticTxLogLinger();
        pessimisticTxLogSize = cc.getPessimisticTxLogSize();
        portableEnabled = cc.isPortableEnabled();
        preloadMode = cc.getPreloadMode();
        preloadBatchSize = cc.getPreloadBatchSize();
        preloadDelay = cc.getPreloadPartitionedDelay();
        preloadOrder = cc.getPreloadOrder();
        preloadPoolSize = cc.getPreloadThreadPoolSize();
        preloadTimeout = cc.getPreloadTimeout();
        preloadThrottle = cc.getPreloadThrottle();
        qryCfg = cc.getQueryConfiguration();
        refreshAheadRatio = cc.getRefreshAheadRatio();
        qryIdxEnabled = cc.isQueryIndexEnabled();
        seqReserveSize = cc.getAtomicSequenceReserveSize();
        startSize = cc.getStartSize();
        store = cc.getStore();
        storeValBytes = cc.isStoreValueBytes();
        swapEnabled = cc.isSwapEnabled();
        tmLookupClsName = cc.getTransactionManagerLookupClassName();
        ttl = cc.getDefaultTimeToLive();
        txBatchUpdate = cc.isBatchUpdateOnCommit();
        txSerEnabled = cc.isTxSerializableEnabled();
        writeBehindBatchSize = cc.getWriteBehindBatchSize();
        writeBehindEnabled = cc.isWriteBehindEnabled();
        writeBehindFlushFreq = cc.getWriteBehindFlushFrequency();
        writeBehindFlushSize = cc.getWriteBehindFlushSize();
        writeBehindFlushThreadCnt = cc.getWriteBehindFlushThreadCount();
        writeSync = cc.getWriteSynchronizationMode();
    }

    /**
     * Cache name. If not provided or {@code null}, then this will be considered a default
     * cache which can be accessed via {@link Grid#cache(String) Grid.cache(null)} method. Otherwise, if name
     * is provided, the cache will be accessed via {@link Grid#cache(String)} method.
     *
     * @return Cache name.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets cache name.
     *
     * @param name Cache name. May be <tt>null</tt>, but may not be empty string.
     */
    public void setName(String name) {
        A.ensure(name == null || !name.isEmpty(), "Name cannot be null or empty.");

        this.name = name;
    }

    /**
     * Gets time to live for all objects in cache. This value can be overridden for individual objects.
     * If not set, then value is {@code 0} which means that objects never expire.
     *
     * @return Time to live for all objects in cache.
     */
    public long getDefaultTimeToLive() {
        return ttl;
    }

    /**
     * Sets time to live for all objects in cache. This value can be override for individual objects.
     *
     * @param ttl Time to live for all objects in cache.
     */
    public void setDefaultTimeToLive(long ttl) {
        this.ttl = ttl;
    }

    /**
     * Gets cache eviction policy. By default, returns {@code null}
     * which means that evictions are disabled for cache.
     *
     * @return Cache eviction policy or {@code null} if evictions should be disabled.
     */
    @SuppressWarnings({"unchecked"})
    @Nullable public <K, V> GridCacheEvictionPolicy<K, V> getEvictionPolicy() {
        return evictPlc;
    }

    /**
     * Sets cache eviction policy.
     *
     * @param evictPlc Cache expiration policy.
     */
    public void setEvictionPolicy(@Nullable GridCacheEvictionPolicy evictPlc) {
        this.evictPlc = evictPlc;
    }

    /**
     * Gets cache distribution mode. This parameter is taken into account only if
     * {@link #getCacheMode()} is set to {@link GridCacheMode#PARTITIONED} or {@link GridCacheMode#REPLICATED} mode.
     * <p>
     * If not set, default value is {@link #DFLT_DISTRIBUTION_MODE}.
     *
     * @return Cache distribution mode.
     */
    public GridCacheDistributionMode getDistributionMode() {
        return distro;
    }

    /**
     * Sets cache distribution mode.
     *
     * @param distro Distribution mode.
     */
    public void setDistributionMode(GridCacheDistributionMode distro) {
        this.distro = distro;
    }

    /**
     * Gets write synchronization mode. This mode controls whether the main
     * caller should wait for update on other nodes to complete or not.
     *
     * @return Write synchronization mode.
     */
    public GridCacheWriteSynchronizationMode getWriteSynchronizationMode() {
        return writeSync;
    }

    /**
     * Sets write synchronization mode.
     *
     * @param writeSync Write synchronization mode.
     */
    public void setWriteSynchronizationMode(GridCacheWriteSynchronizationMode writeSync) {
        this.writeSync = writeSync;
    }

    /**
     * Gets eviction policy for {@code near} cache which is different from the one used for
     * {@code partitioned} cache. By default, returns {@code null}
     * which means that evictions are disabled for near cache.
     *
     * @return Cache eviction policy or {@code null} if evictions should be disabled.
     */
    @SuppressWarnings({"unchecked"})
    @Nullable public <K, V> GridCacheEvictionPolicy<K, V> getNearEvictionPolicy() {
        return nearEvictPlc;
    }

    /**
     * Sets eviction policy for near cache. This property is only used for {@link GridCacheMode#PARTITIONED} caching
     * mode.
     *
     * @param nearEvictPlc Eviction policy for near cache.
     */
    public void setNearEvictionPolicy(@Nullable GridCacheEvictionPolicy nearEvictPlc) {
        this.nearEvictPlc = nearEvictPlc;
    }

    /**
     * Gets flag indicating whether eviction is synchronized between primary and
     * backup nodes on partitioned cache. If this parameter is {@code true} and
     * swap is disabled then {@link GridCacheProjection#evict(Object)}
     * and all its variations will involve all nodes where an entry is kept -
     * this is a group of nodes responsible for partition to which
     * corresponding key belongs. If this property is set to {@code false} then
     * eviction is done independently on cache nodes.
     * <p>
     * Default value is defined by {@link #DFLT_EVICT_SYNCHRONIZED}.
     * <p>
     * Note that it's not recommended to set this value to {@code true} if cache
     * store is configured since it will allow to significantly improve cache
     * performance.
     *
     * @return {@code true} If eviction is synchronized with backup nodes (or the
     *      rest of the nodes in case of replicated cache), {@code false} if not.
     */
    public boolean isEvictSynchronized() {
        return evictSync;
    }

    /**
     * Sets flag indicating whether eviction is synchronized with backup nodes (or the rest of the nodes for replicated
     * cache).
     *
     * @param evictSync {@code true} if synchronized, {@code false} if not.
     */
    public void setEvictSynchronized(boolean evictSync) {
        this.evictSync = evictSync;
    }

    /**
     * Sets flag indicating whether eviction is synchronized with near nodes.
     *
     * @param evictNearSync {@code true} if synchronized, {@code false} if not.
     */
    public void setEvictNearSynchronized(boolean evictNearSync) {
        this.evictNearSync = evictNearSync;
    }

    /**
     * Gets flag indicating whether eviction on primary node is synchronized with
     * near nodes where entry is kept. Default value is {@code true} and
     * is defined by {@link #DFLT_EVICT_NEAR_SYNCHRONIZED}.
     * <p>
     * Note that in most cases this property should be set to {@code true} to keep
     * cache consistency. But there may be the cases when user may use some
     * special near eviction policy to have desired control over near cache
     * entry set.
     *
     * @return {@code true} If eviction is synchronized with near nodes in
     *      partitioned cache, {@code false} if not.
     */
    public boolean isEvictNearSynchronized() {
        return evictNearSync;
    }

    /**
     * Gets size of the key buffer for synchronized evictions.
     * <p>
     * Default value is defined by {@link #DFLT_EVICT_KEY_BUFFER_SIZE}.
     *
     * @return Eviction key buffer size.
     */
    public int getEvictSynchronizedKeyBufferSize() {
        return evictKeyBufSize;
    }

    /**
     * Sets eviction key buffer size.
     *
     * @param evictKeyBufSize Eviction key buffer size.
     */
    public void setEvictSynchronizedKeyBufferSize(int evictKeyBufSize) {
        this.evictKeyBufSize = evictKeyBufSize;
    }

    /**
     * Gets concurrency level for synchronized evictions. This flag only makes sense
     * with {@link #isEvictNearSynchronized()} or {@link #isEvictSynchronized()} set
     * to {@code true}. When synchronized evictions are enabled, it is possible that
     * local eviction policy will try to evict entries faster than evictions can be
     * synchronized with backup or near nodes. This value specifies how many concurrent
     * synchronous eviction sessions should be allowed before the system is forced to
     * wait and let synchronous evictions catch up with the eviction policy.
     * <p>
     * Note that if synchronous evictions start lagging, it is possible that you have either
     * too big or too small eviction key buffer size or small eviction timeout. In that case
     * you will need to adjust {@link #getEvictSynchronizedKeyBufferSize} or
     * {@link #getEvictSynchronizedTimeout()} values as well.
     * <p>
     * Default value is defined by {@link #DFLT_EVICT_SYNCHRONIZED_CONCURRENCY_LEVEL}.
     *
     * @return Synchronous eviction concurrency level.
     */
    public int getEvictSynchronizedConcurrencyLevel() {
        return evictSyncConcurrencyLvl;
    }

    /**
     * Sets concurrency level for synchronized evictions.
     *
     * @param evictSyncConcurrencyLvl Concurrency level for synchronized evictions.
     */
    public void setEvictSynchronizedConcurrencyLevel(int evictSyncConcurrencyLvl) {
        this.evictSyncConcurrencyLvl = evictSyncConcurrencyLvl;
    }

    /**
     * Gets timeout for synchronized evictions.
     * <p>
     * Node that initiates eviction waits for responses
     * from remote nodes within this timeout.
     * <p>
     * Default value is defined by {@link #DFLT_EVICT_SYNCHRONIZED_TIMEOUT}.
     *
     * @return Synchronous eviction timeout.
     */
    public long getEvictSynchronizedTimeout() {
        return evictSyncTimeout;
    }

    /**
     * Sets timeout for synchronized evictions.
     *
     * @param evictSyncTimeout Timeout for synchronized evictions.
     */
    public void setEvictSynchronizedTimeout(long evictSyncTimeout) {
        this.evictSyncTimeout = evictSyncTimeout;
    }

    /**
     * This value denotes the maximum size of eviction queue in percents of cache
     * size in case of distributed cache (replicated and partitioned) and using
     * synchronized eviction (that is if {@link #isEvictSynchronized()} returns
     * {@code true}).
     * <p>
     * That queue is used internally as a buffer to decrease network costs for
     * synchronized eviction. Once queue size reaches specified value all required
     * requests for all entries in the queue are sent to remote nodes and the queue
     * is cleared.
     * <p>
     * Default value is defined by {@link #DFLT_MAX_EVICTION_OVERFLOW_RATIO} and
     * equals to {@code 10%}.
     *
     * @return Maximum size of eviction queue in percents of cache size.
     */
    public float getEvictMaxOverflowRatio() {
        return evictMaxOverflowRatio;
    }

    /**
     * Sets maximum eviction overflow ratio.
     *
     * @param evictMaxOverflowRatio Maximum eviction overflow ratio.
     */
    public void setEvictMaxOverflowRatio(float evictMaxOverflowRatio) {
        this.evictMaxOverflowRatio = evictMaxOverflowRatio;
    }

    /**
     * Gets eviction filter to specify which entries should not be evicted
     * (except explicit evict by calling {@link GridCacheEntry#evict()}).
     * If {@link GridCacheEvictionFilter#evictAllowed(GridCacheEntry)} method returns
     * {@code false} then eviction policy will not be notified and entry will
     * never be evicted.
     * <p>
     * If not provided, any entry may be evicted depending on
     * {@link #getEvictionPolicy() eviction policy} configuration.
     *
     * @return Eviction filter or {@code null}.
     */
    @SuppressWarnings("unchecked")
    public <K, V> GridCacheEvictionFilter<K, V> getEvictionFilter() {
        return (GridCacheEvictionFilter<K, V>)evictFilter;
    }

    /**
     * Sets eviction filter.
     *
     * @param evictFilter Eviction filter.
     */
    public <K, V> void setEvictionFilter(GridCacheEvictionFilter<K, V> evictFilter) {
        this.evictFilter = evictFilter;
    }

    /**
     * Gets flag indicating whether expired cache entries will be eagerly removed from cache. When
     * set to {@code false}, expired entries will be removed on next entry access.
     * <p>
     * When not set, default value is {@link #DFLT_EAGER_TTL}.
     * <p>
     * <b>Note</b> that this flag only matters for entries expiring based on
     * {@link GridCacheEntry#timeToLive()} value and should not be confused with entry
     * evictions based on configured {@link GridCacheEvictionPolicy}.
     *
     * @return Flag indicating whether GridGain will eagerly remove expired entries.
     */
    public boolean isEagerTtl() {
        return eagerTtl;
    }

    /**
     * Sets eager ttl flag.
     *
     * @param eagerTtl {@code True} if GridGain should eagerly remove expired cache entries.
     * @see #isEagerTtl()
     */
    public void setEagerTtl(boolean eagerTtl) {
        this.eagerTtl = eagerTtl;
    }

    /**
     * Default cache transaction concurrency to use when one is not explicitly
     * specified. Default value is defined by {@link #DFLT_TX_CONCURRENCY}.
     *
     * @return Default cache transaction concurrency.
     * @see GridCacheTx
     */
    public GridCacheTxConcurrency getDefaultTxConcurrency() {
        return dfltConcurrency;
    }

    /**
     * Sets default transaction concurrency.
     *
     * @param dfltConcurrency Default cache transaction concurrency.
     */
    public void setDefaultTxConcurrency(GridCacheTxConcurrency dfltConcurrency) {
        this.dfltConcurrency = dfltConcurrency;
    }

    /**
     * Gets flag to enable/disable {@link GridCacheTxIsolation#SERIALIZABLE} isolation
     * level for cache transactions. Serializable level does carry certain overhead and
     * if not used, should be disabled. Default value is {@code false}.
     *
     * @return {@code True} if serializable transactions are enabled, {@code false} otherwise.
     */
    public boolean isTxSerializableEnabled() {
        return txSerEnabled;
    }

    /**
     * Enables/disables serializable cache transactions. See {@link #isTxSerializableEnabled()} for more information.
     *
     * @param txSerEnabled Flag to enable/disable serializable cache transactions.
     */
    public void setTxSerializableEnabled(boolean txSerEnabled) {
        this.txSerEnabled = txSerEnabled;
    }

    /**
     * Default cache transaction isolation to use when one is not explicitly
     * specified. Default value is defined by {@link #DFLT_TX_ISOLATION}.
     *
     * @return Default cache transaction isolation.
     * @see GridCacheTx
     */
    public GridCacheTxIsolation getDefaultTxIsolation() {
        return dfltIsolation;
    }

    /**
     * Sets default transaction isolation.
     *
     * @param dfltIsolation Default cache transaction isolation.
     */
    public void setDefaultTxIsolation(GridCacheTxIsolation dfltIsolation) {
        this.dfltIsolation = dfltIsolation;
    }

    /**
     * Gets initial cache size which will be used to pre-create internal
     * hash table after start. Default value is defined by {@link #DFLT_START_SIZE}.
     *
     * @return Initial cache size.
     */
    public int getStartSize() {
        return startSize;
    }

    /**
     * Initial size for internal hash map.
     *
     * @param startSize Cache start size.
     */
    public void setStartSize(int startSize) {
        this.startSize = startSize;
    }

    /**
     * Gets initial cache size for near cache which will be used to pre-create internal
     * hash table after start. Default value is defined by {@link #DFLT_NEAR_START_SIZE}.
     *
     * @return Initial near cache size.
     */
    public int getNearStartSize() {
        return nearStartSize;
    }

    /**
     * Start size for near cache. This property is only used for {@link GridCacheMode#PARTITIONED} caching mode.
     *
     * @param nearStartSize Start size for near cache.
     */
    public void setNearStartSize(int nearStartSize) {
        this.nearStartSize = nearStartSize;
    }

    /**
     * Gets underlying persistent storage for read-through and write-through operations.
     * If not provided, cache will not exhibit read-through or write-through behavior.
     *
     * @return Underlying persistent storage for read-through and write-through operations.
     */
    @SuppressWarnings({"unchecked"})
    public <K, V> GridCacheStore<K, V> getStore() {
        return (GridCacheStore<K, V>)store;
    }

    /**
     * Sets persistent storage for cache data.
     *
     * @param store Persistent cache store.
     */
    public <K, V> void setStore(GridCacheStore<K, V> store) {
        this.store = store;
    }

    /**
     * Gets key topology resolver to provide mapping from keys to nodes.
     *
     * @return Key topology resolver to provide mapping from keys to nodes.
     */
    public GridCacheAffinityFunction getAffinity() {
        return aff;
    }

    /**
     * Sets affinity for cache keys.
     *
     * @param aff Cache key affinity.
     */
    public void setAffinity(GridCacheAffinityFunction aff) {
        this.aff = aff;
    }

    /**
     * Gets caching mode to use. You can configure cache either to be local-only,
     * fully replicated, partitioned, or near. If not provided, {@link GridCacheMode#REPLICATED}
     * mode will be used by default (defined by {@link #DFLT_CACHE_MODE} constant).
     *
     * @return {@code True} if cache is local.
     */
    public GridCacheMode getCacheMode() {
        return cacheMode;
    }

    /**
     * Sets caching mode.
     *
     * @param cacheMode Caching mode.
     */
    public void setCacheMode(GridCacheMode cacheMode) {
        this.cacheMode = cacheMode;
    }

    /**
     * Gets cache atomicity mode.
     * <p>
     * Default value is defined by {@link #DFLT_CACHE_ATOMICITY_MODE}.
     *
     * @return Cache atomicity mode.
     */
    public GridCacheAtomicityMode getAtomicityMode() {
        return atomicityMode;
    }

    /**
     * Sets cache atomicity mode.
     *
     * @param atomicityMode Cache atomicity mode.
     */
    public void setAtomicityMode(GridCacheAtomicityMode atomicityMode) {
        this.atomicityMode = atomicityMode;
    }

    /**
     * Gets cache write ordering mode. This property can be enabled only for {@link GridCacheAtomicityMode#ATOMIC}
     * cache (for other atomicity modes it will be ignored).
     *
     * @return Cache write ordering mode.
     */
    public GridCacheAtomicWriteOrderMode getAtomicWriteOrderMode() {
        return atomicWriteOrderMode;
    }

    /**
     * Sets cache write ordering mode. This property can be enabled only for {@link GridCacheAtomicityMode#ATOMIC}
     * cache (for other atomicity modes it will be ignored).
     *
     * @param atomicWriteOrderMode Cache write ordering mode.
     */
    public void setAtomicWriteOrderMode(GridCacheAtomicWriteOrderMode atomicWriteOrderMode) {
        this.atomicWriteOrderMode = atomicWriteOrderMode;
    }

    /**
     * Gets number of nodes used to back up single partition for {@link GridCacheMode#PARTITIONED} cache.
     * <p>
     * If not set, default value is {@link #DFLT_BACKUPS}.
     *
     * @return Number of backup nodes for one partition.
     */
    public int getBackups() {
        return backups;
    }

    /**
     * Sets number of nodes used to back up single partition for {@link GridCacheMode#PARTITIONED} cache.
     * <p>
     * If not set, default value is {@link #DFLT_BACKUPS}.
     *
     * @param backups Number of backup nodes for one partition.
     */
    public void setBackups(int backups) {
        this.backups = backups;
    }

    /**
     * If {@code true}, then all transactional values will be written to persistent
     * storage at {@link GridCacheTx#commit()} phase. If {@code false}, then values
     * will be persisted after every operation. Default value is {@code true}.
     *
     * @return Flag indicating whether to persist once on commit, or after every
     *      operation.
     */
    public boolean isBatchUpdateOnCommit() {
        return txBatchUpdate;
    }

    /**
     * Sets flag indicating if persistent store should be updated after every cache operation or once at commit time.
     * Default is {@code true}.
     *
     * @param txBatchUpdate {@code True} if updates should be batched at the end of transaction, {@code false} if
     * updates should be propagated to persistent store individually as they occur (without waiting to the end of
     * transaction).
     */
    public void setBatchUpdateOnCommit(boolean txBatchUpdate) {
        this.txBatchUpdate = txBatchUpdate;
    }

    /**
     * Gets default transaction timeout. Default value is defined by {@link #DFLT_TRANSACTION_TIMEOUT}
     * which is {@code 0} and means that transactions will never time out.
     *
     * @return Default transaction timeout.
     */
    public long getDefaultTxTimeout() {
        return dfltTxTimeout;
    }

    /**
     * Sets default transaction timeout in milliseconds. By default this value is defined by {@link
     * #DFLT_TRANSACTION_TIMEOUT}.
     *
     * @param dfltTxTimeout Default transaction timeout.
     */
    public void setDefaultTxTimeout(long dfltTxTimeout) {
        this.dfltTxTimeout = dfltTxTimeout;
    }

    /**
     * Gets default lock acquisition timeout. Default value is defined by {@link #DFLT_LOCK_TIMEOUT}
     * which is {@code 0} and means that lock acquisition will never timeout.
     *
     * @return Default lock timeout.
     */
    public long getDefaultLockTimeout() {
        return dfltLockTimeout;
    }

    /**
     * Sets default lock timeout in milliseconds. By default this value is defined by {@link #DFLT_LOCK_TIMEOUT}.
     *
     * @param dfltLockTimeout Default lock timeout.
     */
    public void setDefaultLockTimeout(long dfltLockTimeout) {
        this.dfltLockTimeout = dfltLockTimeout;
    }

    /**
     * Gets default query timeout. Default value is defined by {@link #DFLT_QUERY_TIMEOUT}. {@code 0} (zero)
     * means that the query will never timeout and will wait for completion.
     *
     * @return Default query timeout, {@code 0} for never.
     */
    public long getDefaultQueryTimeout() {
        return dfltQryTimeout;
    }

    /**
     * Sets default query timeout, {@code 0} for never. For more information see {@link #getDefaultQueryTimeout()}.
     *
     * @param dfltQryTimeout Default query timeout.
     */
    public void setDefaultQueryTimeout(long dfltQryTimeout) {
        this.dfltQryTimeout = dfltQryTimeout;
    }

    /**
     * Invalidation flag. If {@code true}, values will be invalidated (nullified) upon commit in near cache.
     *
     * @return Invalidation flag.
     */
    public boolean isInvalidate() {
        return invalidate;
    }

    /**
     * Sets invalidation flag for near cache entries in this transaction. Default is {@code false}.
     *
     * @param invalidate Flag to set this cache into invalidation-based mode. Default value is {@code false}.
     */
    public void setInvalidate(boolean invalidate) {
        this.invalidate = invalidate;
    }

    /**
     * Flag indicating if cached values should be additionally stored in serialized form. It's set to true by default.
     *
     * @param storeValBytes {@code true} if cached values should be additionally stored in serialized form, {@code
     * false} otherwise.
     */
    public void setStoreValueBytes(boolean storeValBytes) {
        this.storeValBytes = storeValBytes;
    }

    /**
     * Flag indicating if cached values should be additionally stored in serialized form.
     * It's set to {@code true} by default.
     *
     * @return {@code true} if cached values should be additionally stored in
     *      serialized form, {@code false} otherwise.
     */
    public boolean isStoreValueBytes() {
        return storeValBytes;
    }

    /**
     * Gets refresh-ahead ratio. If non-zero, then entry will be preloaded in the background
     * whenever it's accessed and the refresh ratio of it's total time-to-live has passed.
     * This feature ensures that entries are always automatically re-cached whenever they are
     * nearing expiration.
     * <p>
     * For example, if refresh ratio is set to {@code 0.75} and entry's time-to-live is
     * {@code 1} minute, then if this entry is accessed any time after {@code 45} seconds
     * (which is 0.75 of a minute), the cached value will be immediately returned, but
     * entry will be automatically reloaded from persistent store in the background.
     *
     * @return Refresh-ahead ratio.
     */
    public double getRefreshAheadRatio() {
        return refreshAheadRatio;
    }

    /**
     * Sets refresh-ahead ratio for cache entries. Values other than zero specify how soon entries will be auto-reloaded
     * from persistent store prior to expiration.
     *
     * @param refreshAheadRatio Refresh-ahead ratio.
     */
    public void setRefreshAheadRatio(double refreshAheadRatio) {
        this.refreshAheadRatio = refreshAheadRatio;
    }

    /**
     * Gets size of pessimistic transactions log stored on node in order to recover transaction commit if originating
     * node has left grid before it has sent all messages to transaction nodes.
     * <p>
     * If not set, default value is {@code 0} which means unlimited log size.
     *
     * @return Pessimistic transaction log size.
     */
    public int getPessimisticTxLogSize() {
        return pessimisticTxLogSize;
    }

    /**
     * Sets pessimistic transactions log size.
     *
     * @param pessimisticTxLogSize Pessimistic transactions log size.
     * @see #getPessimisticTxLogSize()
     */
    public void setPessimisticTxLogSize(int pessimisticTxLogSize) {
        this.pessimisticTxLogSize = pessimisticTxLogSize;
    }

    /**
     * Gets delay, in milliseconds, after which pessimistic recovery entries will be cleaned up for failed node.
     * <p>
     * If not set, default value is {@link #DFLT_PESSIMISTIC_TX_LOG_LINGER}.
     *
     * @return Pessimistic log cleanup delay in milliseconds.
     */
    public int getPessimisticTxLogLinger() {
        return pessimisticTxLogLinger;
    }

    /**
     * Sets cleanup delay for pessimistic transaction recovery log for failed node, in milliseconds.
     *
     * @param pessimisticTxLogLinger Pessimistic log cleanup delay.
     * @see #getPessimisticTxLogLinger()
     */
    public void setPessimisticTxLogLinger(int pessimisticTxLogLinger) {
        this.pessimisticTxLogLinger = pessimisticTxLogLinger;
    }

    /**
     * Gets class name of transaction manager finder for integration for JEE app servers.
     *
     * @return Transaction manager finder.
     */
    public String getTransactionManagerLookupClassName() {
        return tmLookupClsName;
    }

    /**
     * Sets look up mechanism for available {@code TransactionManager} implementation, if any.
     *
     * @param tmLookupClsName Name of class implementing GridCacheTmLookup interface that is used to
     *      receive JTA transaction manager.
     */
    public void setTransactionManagerLookupClassName(String tmLookupClsName) {
        this.tmLookupClsName = tmLookupClsName;
    }

    /**
     * Sets cache preload mode.
     *
     * @param preloadMode Preload mode.
     */
    public void setPreloadMode(GridCachePreloadMode preloadMode) {
        this.preloadMode = preloadMode;
    }

    /**
     * Gets preload mode for distributed cache.
     * <p>
     * Default is defined by {@link #DFLT_PRELOAD_MODE}.
     *
     * @return Preload mode.
     */
    public GridCachePreloadMode getPreloadMode() {
        return preloadMode;
    }

    /**
     * Gets cache preload order. Preload order can be set to non-zero value for caches with
     * {@link GridCachePreloadMode#SYNC SYNC} or {@link GridCachePreloadMode#ASYNC ASYNC} preload modes only.
     * <p/>
     * If cache preload order is positive, preloading for this cache will be started only when preloading for
     * all caches with smaller preload order (except caches with preload order {@code 0}) will be completed.
     * <p/>
     * Note that cache with order {@code 0} does not participate in ordering. This means that cache with
     * preload order {@code 1} will never wait for any other caches. All caches with order {@code 0} will
     * be preloaded right away concurrently with each other and ordered preload processes.
     * <p/>
     * If not set, cache order is 0, i.e. preloading is not ordered.
     *
     * @return Cache preload order.
     */
    public int getPreloadOrder() {
        return preloadOrder;
    }

    /**
     * Sets cache preload order.
     *
     * @param preloadOrder Cache preload order.
     * @see #getPreloadOrder()
     */
    public void setPreloadOrder(int preloadOrder) {
        this.preloadOrder = preloadOrder;
    }

    /**
     * Gets size (in number bytes) to be loaded within a single preload message.
     * Preloading algorithm will split total data set on every node into multiple
     * batches prior to sending data. Default value is defined by
     * {@link #DFLT_PRELOAD_BATCH_SIZE}.
     *
     * @return Size in bytes of a single preload message.
     */
    public int getPreloadBatchSize() {
        return preloadBatchSize;
    }

    /**
     * Sets preload batch size.
     *
     * @param preloadBatchSize Preload batch size.
     */
    public void setPreloadBatchSize(int preloadBatchSize) {
        this.preloadBatchSize = preloadBatchSize;
    }

    /**
     * Gets frequency at which distributed garbage collector will
     * check other nodes if there are any zombie locks left over.
     * <p>
     * If not provided, default value is {@link GridCacheConfiguration#DFLT_DGC_FREQUENCY}.
     *
     * @return Frequency of distributed GC in milliseconds ({@code 0} to disable GC).
     */
    public long getDgcFrequency() {
        return dgcFreq;
    }

    /**
     * Sets frequency in milliseconds for internal distributed garbage collector. Pass {@code 0} to disable distributed
     * garbage collection. <p> If not provided, default value is {@link GridCacheConfiguration#DFLT_DGC_FREQUENCY}.
     *
     * @param dgcFreq Frequency of distributed GC in milliseconds ({@code 0} to disable GC).
     */
    public void setDgcFrequency(long dgcFreq) {
        this.dgcFreq = dgcFreq;
    }

    /**
     * Gets timeout after which locks are considered to be suspicious.
     * <p>
     * If not provided, default value is {@link GridCacheConfiguration#DFLT_DGC_SUSPECT_LOCK_TIMEOUT}.
     *
     * @return Distributed GC suspect lock timeout.
     */
    public long getDgcSuspectLockTimeout() {
        return dgcSuspectLockTimeout;
    }

    /**
     * Sets suspect lock timeout in milliseconds for internal distributed garbage collector. If lock's lifetime is
     * greater than the timeout, then lock is considered to be suspicious. <p> If not provided, default value is {@link
     * GridCacheConfiguration#DFLT_DGC_SUSPECT_LOCK_TIMEOUT}.
     *
     * @param dgcSuspectLockTimeout Timeout in milliseconds.
     */
    public void setDgcSuspectLockTimeout(long dgcSuspectLockTimeout) {
        this.dgcSuspectLockTimeout = dgcSuspectLockTimeout;
    }

    /**
     * Gets system-wide flag indicating whether DGC manager should remove locks in question or only
     * report them. Note, that this behavior could be overridden by specifically calling
     * {@link GridCache#dgc(long, boolean, boolean)} method.
     * <p>
     * If {@code false} DGC manager will not release the locks that are not owned by any other node.
     * This may be useful for debugging purposes. You may also enable DGC tracing by enabling DEBUG
     * on {@link #DGC_TRACE_LOGGER_NAME} category.
     * <p>
     * If not provided, default value is {@link GridCacheConfiguration#DFLT_DGC_REMOVE_LOCKS}.
     *
     * @return {@code True} if DGC should remove locks.
     * @see #DGC_TRACE_LOGGER_NAME
     */
    public boolean isDgcRemoveLocks() {
        return dgcRmvLocks;
    }

    /**
     * Sets DGC remove locks flag.
     *
     * @param dgcRmvLocks {@code True} to remove locks.
     * @see #isDgcRemoveLocks()
     */
    public void setDgcRemoveLocks(boolean dgcRmvLocks) {
        this.dgcRmvLocks = dgcRmvLocks;
    }

    /**
     * Flag indicating whether GridGain should use swap storage by default. By default
     * swap is disabled which is defined via {@link #DFLT_SWAP_ENABLED} constant.
     * <p>
     * Note that this flag may be overridden for cache projection created with flag
     * {@link GridCacheFlag#SKIP_SWAP}.
     *
     * @return {@code True} if swap storage is enabled.
     */
    public boolean isSwapEnabled() {
        return swapEnabled;
    }

    /**
     * Flag indicating whether swap storage is enabled or not.
     *
     * @param swapEnabled {@code True} if swap storage is enabled.
     */
    public void setSwapEnabled(boolean swapEnabled) {
        this.swapEnabled = swapEnabled;
    }

    /**
     * Gets maximum number of allowed concurrent asynchronous operations. If 0 returned then number
     * of concurrent asynchronous operations is unlimited.
     * <p>
     * If not set, default value is {@link #DFLT_MAX_CONCURRENT_ASYNC_OPS}.
     * <p>
     * If user threads do not wait for asynchronous operations to complete, it is possible to overload
     * a system. This property enables back-pressure control by limiting number of scheduled asynchronous
     * cache operations.
     *
     * @return Maximum number of concurrent asynchronous operations or {@code 0} if unlimited.
     */
    public int getMaxConcurrentAsyncOperations() {
        return maxConcurrentAsyncOps;
    }

    /**
     * Sets maximum number of concurrent asynchronous operations.
     *
     * @param maxConcurrentAsyncOps Maximum number of concurrent asynchronous operations.
     * @see #getMaxConcurrentAsyncOperations()
     */
    public void setMaxConcurrentAsyncOperations(int maxConcurrentAsyncOps) {
        this.maxConcurrentAsyncOps = maxConcurrentAsyncOps;
    }

    /**
     * Flag indicating whether GridGain should attempt to index value and/or key instances
     * stored in cache. If this property is {@code false}, then all indexing annotations
     * inside of any class will be ignored. By default query indexing is disabled and
     * defined via {@link #DFLT_QUERY_INDEX_ENABLED} constant.
     *
     * @return {@code True} if query indexing is enabled.
     * @see #getMemoryMode()
     */
    public boolean isQueryIndexEnabled() {
        return qryIdxEnabled;
    }

    /**
     * Flag indicating whether query indexing is enabled or not.
     *
     * @param qryIdxEnabled {@code True} if query indexing is enabled.
     */
    public void setQueryIndexEnabled(boolean qryIdxEnabled) {
        this.qryIdxEnabled = qryIdxEnabled;
    }

    /**
     * Flag indicating whether GridGain should use write-behind behaviour for the cache store.
     * By default write-behind is disabled which is defined via {@link #DFLT_WRITE_BEHIND_ENABLED}
     * constant.
     *
     * @return {@code True} if write-behind is enabled.
     */
    public boolean isWriteBehindEnabled() {
        return writeBehindEnabled;
    }

    /**
     * Sets flag indicating whether write-behind is enabled.
     *
     * @param writeBehindEnabled {@code true} if write-behind is enabled.
     */
    public void setWriteBehindEnabled(boolean writeBehindEnabled) {
        this.writeBehindEnabled = writeBehindEnabled;
    }

    /**
     * Maximum size of the write-behind cache. If cache size exceeds this value,
     * all cached items are flushed to the cache store and write cache is cleared.
     * <p/>
     * If not provided, default value is {@link #DFLT_WRITE_BEHIND_FLUSH_SIZE}.
     * If this value is {@code 0}, then flush is performed according to the flush frequency interval.
     * <p/>
     * Note that you cannot set both, {@code flush} size and {@code flush frequency}, to {@code 0}.
     *
     * @return Maximum object count in write-behind cache.
     */
    public int getWriteBehindFlushSize() {
        return writeBehindFlushSize;
    }

    /**
     * Sets write-behind flush size.
     *
     * @param writeBehindFlushSize Write-behind cache flush size.
     * @see #getWriteBehindFlushSize()
     */
    public void setWriteBehindFlushSize(int writeBehindFlushSize) {
        this.writeBehindFlushSize = writeBehindFlushSize;
    }

    /**
     * Frequency with which write-behind cache is flushed to the cache store in milliseconds.
     * This value defines the maximum time interval between object insertion/deletion from the cache
     * ant the moment when corresponding operation is applied to the cache store.
     * <p>
     * If not provided, default value is {@link #DFLT_WRITE_BEHIND_FLUSH_FREQUENCY}.
     * If this value is {@code 0}, then flush is performed according to the flush size.
     * <p>
     * Note that you cannot set both, {@code flush} size and {@code flush frequency}, to {@code 0}.
     *
     * @return Write-behind flush frequency in milliseconds.
     */
    public long getWriteBehindFlushFrequency() {
        return writeBehindFlushFreq;
    }

    /**
     * Sets write-behind flush frequency.
     *
     * @param writeBehindFlushFreq Write-behind flush frequency in milliseconds.
     * @see #getWriteBehindFlushFrequency()
     */
    public void setWriteBehindFlushFrequency(long writeBehindFlushFreq) {
        this.writeBehindFlushFreq = writeBehindFlushFreq;
    }

    /**
     * Number of threads that will perform cache flushing. Cache flushing is performed
     * when cache size exceeds value defined by
     * {@link #getWriteBehindFlushSize}, or flush interval defined by
     * {@link #getWriteBehindFlushFrequency} is elapsed.
     * <p/>
     * If not provided, default value is {@link #DFLT_WRITE_FROM_BEHIND_FLUSH_THREAD_CNT}.
     *
     * @return Count of flush threads.
     */
    public int getWriteBehindFlushThreadCount() {
        return writeBehindFlushThreadCnt;
    }

    /**
     * Sets flush thread count for write-behind cache.
     *
     * @param writeBehindFlushThreadCnt Count of flush threads.
     * @see #getWriteBehindFlushThreadCount()
     */
    public void setWriteBehindFlushThreadCount(int writeBehindFlushThreadCnt) {
        this.writeBehindFlushThreadCnt = writeBehindFlushThreadCnt;
    }

    /**
     * Maximum batch size for write-behind cache store operations. Store operations (get or remove)
     * are combined in a batch of this size to be passed to
     * {@link GridCacheStore#putAll(GridCacheTx, Map)} or
     * {@link GridCacheStore#removeAll(GridCacheTx, Collection)} methods.
     * <p/>
     * If not provided, default value is {@link #DFLT_WRITE_BEHIND_BATCH_SIZE}.
     *
     * @return Maximum batch size for store operations.
     */
    public int getWriteBehindBatchSize() {
        return writeBehindBatchSize;
    }

    /**
     * Sets maximum batch size for write-behind cache.
     *
     * @param writeBehindBatchSize Maximum batch size.
     * @see #getWriteBehindBatchSize()
     */
    public void setWriteBehindBatchSize(int writeBehindBatchSize) {
        this.writeBehindBatchSize = writeBehindBatchSize;
    }

    /**
     * Cloner to be used for cloning values that are returned to user only if {@link GridCacheFlag#CLONE}
     * is set on {@link GridCacheProjection}. Cloning values is useful when it is needed to get value from
     * cache, change it and put it back (if the value was not cloned, then user would be updating the
     * cached reference which would violate cache integrity).
     * <p>
     * <b>NOTE:</b> by default, cache uses {@link GridCacheBasicCloner} implementation which will clone only objects
     * implementing {@link Cloneable} interface. You can also configure cache to use
     * {@link GridCacheDeepCloner} which will perform deep-cloning of all objects returned from cache,
     * regardless of the {@link Cloneable} interface. If none of the above cloners fit your
     * logic, you can also provide your own implementation of {@link GridCacheCloner} interface.
     *
     * @return Cloner to be used if {@link GridCacheFlag#CLONE} flag is set on cache projection.
     */
    @SuppressWarnings({"unchecked"})
    public GridCacheCloner getCloner() {
        return cloner;
    }

    /**
     * Sets cloner to be used if {@link GridCacheFlag#CLONE} flag is set on projection.
     *
     * @param cloner Cloner to use.
     * @see #getCloner()
     */
    public void setCloner(GridCacheCloner cloner) {
        this.cloner = cloner;
    }

    /**
     * Gets default number of sequence values reserved for {@link GridCacheAtomicSequence} instances. After
     * a certain number has been reserved, consequent increments of sequence will happen locally,
     * without communication with other nodes, until the next reservation has to be made.
     * <p>
     * Default value is {@link #DFLT_ATOMIC_SEQUENCE_RESERVE_SIZE}.
     *
     * @return Atomic sequence reservation size.
     */
    public int getAtomicSequenceReserveSize() {
        return seqReserveSize;
    }

    /**
     * Sets default number of sequence values reserved for {@link GridCacheAtomicSequence} instances. After a certain
     * number has been reserved, consequent increments of sequence will happen locally, without communication with other
     * nodes, until the next reservation has to be made.
     *
     * @param seqReserveSize Atomic sequence reservation size.
     * @see #getAtomicSequenceReserveSize()
     */
    public void setAtomicSequenceReserveSize(int seqReserveSize) {
        this.seqReserveSize = seqReserveSize;
    }

    /**
     * Gets size of preloading thread pool. Note that size serves as a hint and implementation
     * may create more threads for preloading than specified here (but never less threads).
     * <p>
     * Default value is {@link #DFLT_PRELOAD_THREAD_POOL_SIZE}.
     *
     * @return Size of preloading thread pool.
     */
    public int getPreloadThreadPoolSize() {
        return preloadPoolSize;
    }

    /**
     * Sets size of preloading thread pool. Note that size serves as a hint and implementation may create more threads
     * for preloading than specified here (but never less threads).
     *
     * @param preloadPoolSize Size of preloading thread pool.
     */
    public void setPreloadThreadPoolSize(int preloadPoolSize) {
        this.preloadPoolSize = preloadPoolSize;
    }

    /**
     * Gets preload timeout (ms).
     * <p>
     * Default value is {@link #DFLT_PRELOAD_TIMEOUT}.
     *
     * @return Preload timeout (ms).
     */
    public long getPreloadTimeout() {
        return preloadTimeout;
    }

    /**
     * Sets preload timeout (ms).
     *
     * @param preloadTimeout Preload timeout (ms).
     */
    public void setPreloadTimeout(long preloadTimeout) {
        this.preloadTimeout = preloadTimeout;
    }

    /**
     * Gets delay in milliseconds upon a node joining or leaving topology (or crash) after which preloading
     * should be started automatically. Preloading should be delayed if you plan to restart nodes
     * after they leave topology, or if you plan to start multiple nodes at once or one after another
     * and don't want to repartition and preload until all nodes are started.
     * <p>
     * Delayed preloading is applied to {@link GridCacheMode#PARTITIONED} caches only.
     * For better efficiency user should usually make sure that new nodes get placed on
     * the same place of consistent hash ring as the left nodes, and that nodes are
     * restarted before this delay expires. To place nodes on the same place in consistent hash ring,
     * use {@link GridCacheConsistentHashAffinityFunction#setHashIdResolver(GridCacheAffinityNodeHashResolver)}
     * to make sure that a node maps to the same hash ID event if restarted. As an example,
     * node IP address and port combination may be used in this case.
     * <p>
     * Default value is {@code 0} which means that repartitioning and preloading will start
     * immediately upon node leaving topology. If {@code -1} is returned, then preloading
     * will only be started manually by calling {@link GridCache#forceRepartition()} method or
     * from management console.
     *
     * @return Preloading delay, {@code 0} to start preloading immediately, {@code -1} to
     *      start preloading manually, or positive value to specify delay in milliseconds
     *      after which preloading should start automatically.
     */
    public long getPreloadPartitionedDelay() {
        return preloadDelay;
    }

    /**
     * Sets preload delay (see {@link #getPreloadPartitionedDelay()} for more information).
     *
     * @param preloadDelay Preload delay to set.
     */
    public void setPreloadPartitionedDelay(long preloadDelay) {
        this.preloadDelay = preloadDelay;
    }

    /**
     * Time in milliseconds to wait between preload messages to avoid overloading of CPU or network.
     * When preloading large data sets, the CPU or network can get over-consumed with preloading messages,
     * which consecutively may slow down the application performance. This parameter helps tune
     * the amount of time to wait between preload messages to make sure that preloading process
     * does not have any negative performance impact. Note that application will continue to work
     * properly while preloading is still in progress.
     * <p>
     * Value of {@code 0} means that throttling is disabled. By default throttling is disabled -
     * the default is defined by {@link #DFLT_PRELOAD_THROTTLE} constant.
     *
     * @return Time in milliseconds to wait between preload messages to avoid overloading of CPU,
     *      {@code 0} to disable throttling.
     */
    public long getPreloadThrottle() {
        return preloadThrottle;
    }

    /**
     * Time in milliseconds to wait between preload messages to avoid overloading of CPU or network. When preloading
     * large data sets, the CPU or network can get over-consumed with preloading messages, which consecutively may slow
     * down the application performance. This parameter helps tune the amount of time to wait between preload messages
     * to make sure that preloading process does not have any negative performance impact. Note that application will
     * continue to work properly while preloading is still in progress. <p> Value of {@code 0} means that throttling is
     * disabled. By default throttling is disabled - the default is defined by {@link #DFLT_PRELOAD_THROTTLE} constant.
     *
     * @param preloadThrottle Time in milliseconds to wait between preload messages to avoid overloading of CPU, {@code
     * 0} to disable throttling.
     */
    public void setPreloadThrottle(long preloadThrottle) {
        this.preloadThrottle = preloadThrottle;
    }

    /**
     * Affinity key mapper used to provide custom affinity key for any given key.
     * Affinity mapper is particularly useful when several objects need to be collocated
     * on the same node (they will also be backed up on the same nodes as well).
     * <p>
     * If not provided, then default implementation will be used. The default behavior
     * is described in {@link GridCacheAffinityKeyMapper} documentation.
     *
     * @return Mapper to use for affinity key mapping.
     */
    public GridCacheAffinityKeyMapper getAffinityMapper() {
        return affMapper;
    }

    /**
     * Sets custom affinity mapper. If not provided, then default implementation will be used. The default behavior is
     * described in {@link GridCacheAffinityKeyMapper} documentation.
     *
     * @param affMapper Affinity mapper.
     */
    public void setAffinityMapper(GridCacheAffinityKeyMapper affMapper) {
        this.affMapper = affMapper;
    }

    /**
     * Gets name of the SPI to use for indexing. If not specified, the default
     * indexing SPI will be used.
     * <p>
     * This property becomes useful in rare cases when more than one indexing
     * SPI is configured. In majority of the cases default value should be used.
     *
     * @return Name of SPI to use for indexing.
     * @see GridIndexingSpi
     */
    public String getIndexingSpiName() {
        return indexingSpiName;
    }

    /**
     * Sets name of the SPI to use for indexing. If not specified, the default
     * indexing SPI will be used.
     * <p>
     * This property becomes useful in rare cases when more than one indexing
     * SPI is configured. In majority of the cases default value should be used.
     *
     * @param indexingSpiName Name.
     * @see GridIndexingSpi
     */
    public void setIndexingSpiName(String indexingSpiName) {
        this.indexingSpiName = indexingSpiName;
    }

    /**
     * Gets maximum amount of memory available to off-heap storage. Possible values are
     * <ul>
     * <li>{@code -1} - Means that off-heap storage is disabled.</li>
     * <li>
     *     {@code 0} - GridGain will not limit off-heap storage (it's up to user to properly
     *     add and remove entries from cache to ensure that off-heap storage does not grow
     *     indefinitely.
     * </li>
     * <li>Any positive value specifies the limit of off-heap storage in bytes.</li>
     * </ul>
     * Default value is {@code -1}, specified by {@link #DFLT_OFFHEAP_MEMORY} constant
     * which means that off-heap storage is disabled by default.
     * <p>
     * Use off-heap storage to load gigabytes of data in memory without slowing down
     * Garbage Collection. Essentially in this case you should allocate very small amount
     * of memory to JVM and GridGain will cache most of the data in off-heap space
     * without affecting JVM performance at all.
     * <p>
     * Note that GridGain will throw an exception if max memory is set to {@code -1} and
     * {@code offHeapValuesOnly} flag is set to {@code true}.
     *
     * @return Maximum memory in bytes available to off-heap memory space.
     */
    public long getOffHeapMaxMemory() {
        return offHeapMaxMem;
    }

    /**
     * Sets maximum amount of memory available to off-heap storage. Possible values are <ul> <li>{@code -1} - Means that
     * off-heap storage is disabled.</li> <li> {@code 0} - GridGain will not limit off-heap storage (it's up to user to
     * properly add and remove entries from cache to ensure that off-heap storage does not grow infinitely. </li>
     * <li>Any positive value specifies the limit of off-heap storage in bytes.</li> </ul> Default value is {@code -1},
     * specified by {@link #DFLT_OFFHEAP_MEMORY} constant which means that off-heap storage is disabled by default. <p>
     * Use off-heap storage to load gigabytes of data in memory without slowing down Garbage Collection. Essentially in
     * this case you should allocate very small amount of memory to JVM and GridGain will cache most of the data in
     * off-heap space without affecting JVM performance at all.
     *
     * @param offHeapMaxMem Maximum memory in bytes available to off-heap memory space.
     */
    public void setOffHeapMaxMemory(long offHeapMaxMem) {
        this.offHeapMaxMem = offHeapMaxMem;
    }

    /**
     * Gets maximum number of query iterators that can be stored. Iterators are stored to
     * support query pagination when each page of data is sent to user's node only on demand.
     * Increase this property if you are running and processing lots of queries in parallel.
     * <p>
     * Default value is {@link #DFLT_MAX_QUERY_ITERATOR_CNT}.
     *
     * @return Maximum number of query iterators that can be stored.
     */
    public int getMaximumQueryIteratorCount() {
        return maxQryIterCnt;
    }

    /**
     * Sets maximum number of query iterators that can be stored.
     *
     * @param maxQryIterCnt Maximum number of query iterators that can be stored.
     */
    public void setMaximumQueryIteratorCount(int maxQryIterCnt) {
        this.maxQryIterCnt = maxQryIterCnt;
    }

    /**
     * Gets maximum number of entries that can be accumulated before back-pressure
     * is enabled to postpone cache updates until query listeners are notified.
     * If your system is configured properly, then this number should never be reached.
     * <p>
     * Default value is {@link #DFLT_CONT_QUERY_QUEUE_SIZE}.
     *
     * @return Continuous query queue size.
     * @deprecated Ignored in current version.
     */
    @Deprecated
    public int getContinuousQueryQueueSize() {
        return 0;
    }

    /**
     * Sets continuous query queue size.
     *
     * @param contQryQueueSize Continuous query queue size.
     * @deprecated Ignored in current version.
     */
    @Deprecated
    public void setContinuousQueryQueueSize(int contQryQueueSize) {
        // No-op.
    }

    /**
     * Gets memory mode for cache. Memory mode helps control whether value is stored in on-heap memory,
     * off-heap memory, or swap space. Refer to {@link GridCacheMemoryMode} for more info.
     * <p>
     * Default value is {@link #DFLT_MEMORY_MODE}.
     *
     * @return Memory mode.
     */
    public GridCacheMemoryMode getMemoryMode() {
        return memMode;
    }

    /**
     * Sets memory mode for cache.
     *
     * @param memMode Memory mode.
     */
    public void setMemoryMode(GridCacheMemoryMode memMode) {
        this.memMode = memMode;
    }

    /**
     * Gets the maximum buffer size for continuous queries. When the current
     * number of entries in buffer exceeds the maximum buffer size, the buffer
     * is flushed to the notification queue. Greater buffer size may improve throughput,
     * but also may increase latency.
     * <p>
     * Default value is either {@link #DFLT_CONT_QUERY_MAX_BUF_SIZE} or
     * {@code GG_CONT_QUERY_MAX_BUF_SIZE} system property value (if specified).
     *
     * @return Maximum buffer size for continuous queries.
     * @deprecated Ignored in current version.
     */
    @Deprecated
    public int getContinuousQueryMaximumBufferSize() {
        return 0;
    }

    /**
     * Sets maximum buffer size for continuous queries.
     *
     * @param contQryMaxBufSize Maximum buffer size for continuous queries.
     * @deprecated Ignored in current version.
     */
    @Deprecated
    public void setContinuousQueryMaximumBufferSize(int contQryMaxBufSize) {
        // No-op.
    }

    /**
     * Gets data center replication send configuration.
     *
     * @return Data center replication send configuration.
     */
    @Nullable public GridDrSenderCacheConfiguration getDrSenderConfiguration() {
        return drSndCacheCfg;
    }

    /**
     * Sets data center replication send configuration. See {@link #getDrSenderConfiguration} for more information.
     *
     * @param drSndCacheCfg Data center replication send configuration.
     */
    public void setDrSenderConfiguration(GridDrSenderCacheConfiguration drSndCacheCfg) {
        this.drSndCacheCfg = drSndCacheCfg;
    }

    /**
     * Gets data center replication receive configuration.
     *
     * @return Data center replication receive configuration.
     */
    @Nullable public GridDrReceiverCacheConfiguration getDrReceiverConfiguration() {
        return drRcvCacheCfg;
    }

    /**
     * Sets data center replication receive configuration. See {@link #getDrReceiverConfiguration} for more information.
     *
     * @param drRcvCacheCfg Data center replication receive configuration.
     */
    public void setDrReceiverConfiguration(GridDrReceiverCacheConfiguration drRcvCacheCfg) {
        this.drRcvCacheCfg = drRcvCacheCfg;
    }

    /**
     * Gets cache interceptor.
     *
     * @return Cache interceptor.
     */
    @SuppressWarnings({"unchecked"})
    @Nullable public <K, V> GridCacheInterceptor<K, V> getInterceptor() {
        return (GridCacheInterceptor<K, V>)interceptor;
    }

    /**
     * Sets cache interceptor.
     *
     * @param interceptor Cache interceptor.
     */
    public <K, V> void setInterceptor(GridCacheInterceptor<K, V> interceptor) {
        this.interceptor = interceptor;
    }

    /**
     * Flag indicating whether GridGain should store portable keys and values
     * as instances of {@link GridPortableObject}.
     *
     * @return Portable enabled flag.
     */
    public boolean isPortableEnabled() {
        return portableEnabled;
    }

    /**
     * Gets portable enabled flag value.
     *
     * @param portableEnabled Portable enabled flag value.
     */
    public void setPortableEnabled(boolean portableEnabled) {
        this.portableEnabled = portableEnabled;
    }

    /**
     * Gets query configuration. Query configuration defines which fields should be indexed for objects
     * without annotations or portable objects.
     *
     * @return Cache query configuration.
     */
    public GridCacheQueryConfiguration getQueryConfiguration() {
        return qryCfg;
    }

    /**
     * Sets query configuration.
     *
     * @param qryCfg Query configuration.
     * @see GridCacheQueryConfiguration
     */
    public void setQueryConfiguration(GridCacheQueryConfiguration qryCfg) {
        this.qryCfg = qryCfg;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridCacheConfiguration.class, this);
    }
}
