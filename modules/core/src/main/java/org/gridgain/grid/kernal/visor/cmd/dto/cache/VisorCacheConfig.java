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

package org.gridgain.grid.kernal.visor.cmd.dto.cache;

import org.gridgain.grid.cache.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.util.*;

import static org.gridgain.grid.kernal.visor.cmd.VisorTaskUtils.*;

/**
 * Data transfer object for cache configuration properties.
 */
public class VisorCacheConfig implements Serializable {
    /** */
    private static final long serialVersionUID = 0L;

    /** Cache name. */
    private String name;

    /** Cache mode. */
    private GridCacheMode mode;

    /** Distribution mode. */
    private GridCacheDistributionMode distributionMode;

    /** Cache atomicity mode */
    private GridCacheAtomicityMode atomicityMode;

    /** Cache atomic sequence reserve size */
    private int atomicSequenceReserveSize;

    /** Cache atomicity write ordering mode. */
    private GridCacheAtomicWriteOrderMode atomicWriteOrderMode;

    /** Eager ttl flag */
    private boolean eagerTtl;

    /** Refresh ahead ratio. */
    private double refreshAheadRatio;

    /** Write synchronization mode. */
    private GridCacheWriteSynchronizationMode writeSynchronizationMode;

    /** Sequence reserve size. */
    private int seqReserveSize;

    /** Swap enabled flag. */
    private boolean swapEnabled;

    /** Flag indicating whether GridGain should attempt to index value and/or key instances stored in cache. */
    private boolean queryIndexEnabled;

    /** Flag indicating whether to persist once on commit, or after every operation. */
    private boolean batchUpdateOnCommit;

    /** Invalidate. */
    private boolean invalidate;

    /** Start size. */
    private int startSize;

    /** Cloner. */
    private String cloner;

    /** Name of class implementing GridCacheTmLookup. */
    private String tmLookupClsName;

    /** Flag to enable/disable transaction serializable isolation level. */
    private boolean txSerializableEnabled;

    /** Off-heap max memory. */
    private long offHeapMaxMemory;

    /** Max query iterator count */
    private int maxQueryIteratorCnt;

    /** Max concurrent async operations */
    private int maxConcurrentAsyncOps;

    /** Pessimistic tx logger size */
    private int pessimisticTxLogSize;

    /** Pessimistic tx logger linger. */
    private int pessimisticTxLogLinger;

    /** Memory mode. */
    private GridCacheMemoryMode memoryMode;

    /** Name of SPI to use for indexing. */
    private String indexingSpiName;

    /** Cache interceptor. */
    private String interceptor;

    /** Cache affinity config. */
    private VisorAffinityConfig affinity;

    /** Preload config. */
    private VisorPreloadConfig preload;

    /** Eviction config. */
    private VisorEvictionConfig evict;

    /** Near cache config. */
    private VisorNearCacheConfig near;

    /** Default config */
    private VisorDefaultConfig dflt;

    /** Dgc config */
    private VisorDgcConfig dgc;

    /** Store config */
    private VisorStoreConfig store;

    /** Write behind config */
    private VisorWriteBehindConfig writeBehind;

    /** Data center replication send configuration. * */
    private VisorDrSenderConfig drSendConfig;

    /** Data center replication receive configuration. */
    private VisorDrReceiverConfig drReceiveConfig;

    /**
     * @param ccfg Cache configuration.
     * @return Data transfer object for cache configuration properties.
     */
    public static VisorCacheConfig from(GridCacheConfiguration ccfg) {
        VisorCacheConfig cfg = new VisorCacheConfig();

        cfg.name(ccfg.getName());
        cfg.mode(ccfg.getCacheMode());
        cfg.distributionMode(ccfg.getDistributionMode());
        cfg.atomicityMode(ccfg.getAtomicityMode());
        cfg.atomicSequenceReserveSize(ccfg.getAtomicSequenceReserveSize());
        cfg.atomicWriteOrderMode(ccfg.getAtomicWriteOrderMode());
        cfg.atomicSequenceReserveSize(ccfg.getAtomicSequenceReserveSize());
        cfg.eagerTtl(ccfg.isEagerTtl());
        cfg.refreshAheadRatio(ccfg.getRefreshAheadRatio());
        cfg.writeSynchronizationMode(ccfg.getWriteSynchronizationMode());
        cfg.swapEnabled(ccfg.isSwapEnabled());
        cfg.queryIndexEnabled(ccfg.isQueryIndexEnabled());
        cfg.batchUpdateOnCommit(ccfg.isBatchUpdateOnCommit());
        cfg.invalidate(ccfg.isInvalidate());
        cfg.startSize(ccfg.getStartSize());
        cfg.cloner(compactClass(ccfg.getCloner()));
        cfg.transactionManagerLookupClassName(ccfg.getTransactionManagerLookupClassName());
        cfg.txSerializableEnabled(ccfg.isTxSerializableEnabled());
        cfg.offsetHeapMaxMemory(ccfg.getOffHeapMaxMemory());
        cfg.maxQueryIteratorCount(ccfg.getMaximumQueryIteratorCount());
        cfg.maxConcurrentAsyncOperations(ccfg.getMaxConcurrentAsyncOperations());
        cfg.pessimisticTxLoggerSize(ccfg.getPessimisticTxLogSize());
        cfg.pessimisticTxLoggerLinger(ccfg.getPessimisticTxLogLinger());
        cfg.memoryMode(ccfg.getMemoryMode());
        cfg.indexingSpiName(ccfg.getIndexingSpiName());
        cfg.interceptor(compactClass(ccfg.getInterceptor()));
        cfg.affinityConfig(VisorAffinityConfig.from(ccfg));
        cfg.preloadConfig(VisorPreloadConfig.from(ccfg));
        cfg.evictConfig(VisorEvictionConfig.from(ccfg));
        cfg.nearConfig(VisorNearCacheConfig.from(ccfg));
        cfg.defaultConfig(VisorDefaultConfig.from(ccfg));
        cfg.dgcConfig(VisorDgcConfig.from(ccfg));
        cfg.storeConfig(VisorStoreConfig.from(ccfg));
        cfg.VisorWriteBehindConfig(VisorWriteBehindConfig.from(ccfg));

        if (ccfg.getDrReceiverConfiguration() != null)
            cfg.drReceiveConfig(VisorDrReceiverConfig.from(ccfg.getDrReceiverConfiguration()));

        if (ccfg.getDrSenderConfiguration() != null)
            cfg.drSendConfig(VisorDrSenderConfig.from(ccfg.getDrSenderConfiguration()));

        return cfg;
    }

    /**
     * @param caches Cache configurations.
     * @return Data transfer object for cache configurations properties.
     */
    public static Iterable<VisorCacheConfig> list(GridCacheConfiguration[] caches) {
        if (caches == null)
            return Collections.emptyList();

        final Collection<VisorCacheConfig> cfgs = new ArrayList<>(caches.length);

        for (GridCacheConfiguration cache : caches)
            cfgs.add(from(cache));

        return cfgs;
    }

    /**
     * @return Cache name.
     */
    @Nullable public String name() {
        return name;
    }

    /**
     * @param name New cache name.
     */
    public void name(@Nullable String name) {
        this.name = name;
    }

    /**
     * @return Cache mode.
     */
    public GridCacheMode mode() {
        return mode;
    }

    /**
     * @param mode New cache mode.
     */
    public void mode(GridCacheMode mode) {
        this.mode = mode;
    }

    /**
     * @return Distribution mode.
     */
    public GridCacheDistributionMode distributionMode() {
        return distributionMode;
    }

    /**
     * @param distributionMode New distribution mode.
     */
    public void distributionMode(GridCacheDistributionMode distributionMode) {
        this.distributionMode = distributionMode;
    }

    /**
     * @return Cache atomicity mode
     */
    public GridCacheAtomicityMode atomicityMode() {
        return atomicityMode;
    }

    /**
     * @param atomicityMode New cache atomicity mode
     */
    public void atomicityMode(GridCacheAtomicityMode atomicityMode) {
        this.atomicityMode = atomicityMode;
    }

    /**
     * @return Cache atomic sequence reserve size
     */
    public int atomicSequenceReserveSize() {
        return atomicSequenceReserveSize;
    }

    /**
     * @param atomicSeqReserveSize New cache atomic sequence reserve size
     */
    public void atomicSequenceReserveSize(int atomicSeqReserveSize) {
        atomicSequenceReserveSize = atomicSeqReserveSize;
    }

    /**
     * @return Cache atomicity write ordering mode.
     */
    public GridCacheAtomicWriteOrderMode atomicWriteOrderMode() {
        return atomicWriteOrderMode;
    }

    /**
     * @param atomicWriteOrderMode New cache atomicity write ordering mode.
     */
    public void atomicWriteOrderMode(GridCacheAtomicWriteOrderMode atomicWriteOrderMode) {
        this.atomicWriteOrderMode = atomicWriteOrderMode;
    }

    /**
     * @return Eager ttl flag
     */
    public boolean eagerTtl() {
        return eagerTtl;
    }

    /**
     * @param eagerTtl New eager ttl flag
     */
    public void eagerTtl(boolean eagerTtl) {
        this.eagerTtl = eagerTtl;
    }

    /**
     * @return Refresh ahead ratio.
     */
    public double refreshAheadRatio() {
        return refreshAheadRatio;
    }

    /**
     * @param refreshAheadRatio New refresh ahead ratio.
     */
    public void refreshAheadRatio(double refreshAheadRatio) {
        this.refreshAheadRatio = refreshAheadRatio;
    }

    /**
     * @return Write synchronization mode.
     */
    public GridCacheWriteSynchronizationMode writeSynchronizationMode() {
        return writeSynchronizationMode;
    }

    /**
     * @param writeSynchronizationMode New write synchronization mode.
     */
    public void writeSynchronizationMode(GridCacheWriteSynchronizationMode writeSynchronizationMode) {
        this.writeSynchronizationMode = writeSynchronizationMode;
    }

    /**
     * @return Sequence reserve size.
     */
    public int sequenceReserveSize() {
        return seqReserveSize;
    }

    /**
     * @param seqReserveSize New sequence reserve size.
     */
    public void sequenceReserveSize(int seqReserveSize) {
        this.seqReserveSize = seqReserveSize;
    }

    /**
     * @return Swap enabled flag.
     */
    public boolean swapEnabled() {
        return swapEnabled;
    }

    /**
     * @param swapEnabled New swap enabled flag.
     */
    public void swapEnabled(boolean swapEnabled) {
        this.swapEnabled = swapEnabled;
    }

    /**
     * @return Flag indicating whether GridGain should attempt to index value and/or key instances stored in cache.
     */
    public boolean queryIndexEnabled() {
        return queryIndexEnabled;
    }

    /**
     * @param qryIdxEnabled New flag indicating whether GridGain should attempt to index value and/or key instances
     * stored in cache.
     */
    public void queryIndexEnabled(boolean qryIdxEnabled) {
        queryIndexEnabled = qryIdxEnabled;
    }

    /**
     * @return Flag indicating whether to persist once on commit, or after every operation.
     */
    public boolean batchUpdateOnCommit() {
        return batchUpdateOnCommit;
    }

    /**
     * @param batchUpdateOnCommit New batch update on commit.
     */
    public void batchUpdateOnCommit(boolean batchUpdateOnCommit) {
        this.batchUpdateOnCommit = batchUpdateOnCommit;
    }

    /**
     * @return Invalidate.
     */
    public boolean invalidate() {
        return invalidate;
    }

    /**
     * @param invalidate New invalidate.
     */
    public void invalidate(boolean invalidate) {
        this.invalidate = invalidate;
    }

    /**
     * @return Start size.
     */
    public int startSize() {
        return startSize;
    }

    /**
     * @param startSize New start size.
     */
    public void startSize(int startSize) {
        this.startSize = startSize;
    }

    /**
     * @return Cloner.
     */
    @Nullable public String cloner() {
        return cloner;
    }

    /**
     * @param cloner New cloner.
     */
    public void cloner(@Nullable String cloner) {
        this.cloner = cloner;
    }

    /**
     * @return Name of class implementing GridCacheTmLookup.
     */
    @Nullable public String transactionManagerLookupClassName() {
        return tmLookupClsName;
    }

    /**
     * @param tmLookupClsName New name of class implementing GridCacheTmLookup.
     */
    public void transactionManagerLookupClassName(@Nullable String tmLookupClsName) {
        this.tmLookupClsName = tmLookupClsName;
    }

    /**
     * @return Flag to enable/disable transaction serializable isolation level.
     */
    public boolean txSerializableEnabled() {
        return txSerializableEnabled;
    }

    /**
     * @param txSerEnabled New flag to enable/disable transaction serializable isolation level.
     */
    public void txSerializableEnabled(boolean txSerEnabled) {
        txSerializableEnabled = txSerEnabled;
    }

    /**
     * @return Off-heap max memory.
     */
    public long offsetHeapMaxMemory() {
        return offHeapMaxMemory;
    }

    /**
     * @param offHeapMaxMemory New off-heap max memory.
     */
    public void offsetHeapMaxMemory(long offHeapMaxMemory) {
        this.offHeapMaxMemory = offHeapMaxMemory;
    }

    /**
     * @return Max query iterator count
     */
    public int maxQueryIteratorCount() {
        return maxQueryIteratorCnt;
    }

    /**
     * @param maxQryIterCnt New max query iterator count
     */
    public void maxQueryIteratorCount(int maxQryIterCnt) {
        maxQueryIteratorCnt = maxQryIterCnt;
    }

    /**
     * @return Max concurrent async operations
     */
    public int maxConcurrentAsyncOperations() {
        return maxConcurrentAsyncOps;
    }

    /**
     * @param maxConcurrentAsyncOps New max concurrent async operations
     */
    public void maxConcurrentAsyncOperations(int maxConcurrentAsyncOps) {
        this.maxConcurrentAsyncOps = maxConcurrentAsyncOps;
    }

    /**
     * @return Pessimistic tx logger size
     */
    public int pessimisticTxLoggerSize() {
        return pessimisticTxLogSize;
    }

    /**
     * @param pessimisticTxLogSize New pessimistic tx logger size
     */
    public void pessimisticTxLoggerSize(int pessimisticTxLogSize) {
        this.pessimisticTxLogSize = pessimisticTxLogSize;
    }

    /**
     * @return Pessimistic tx logger linger.
     */
    public int pessimisticTxLoggerLinger() {
        return pessimisticTxLogLinger;
    }

    /**
     * @param pessimisticTxLogLinger New pessimistic tx logger linger.
     */
    public void pessimisticTxLoggerLinger(int pessimisticTxLogLinger) {
        this.pessimisticTxLogLinger = pessimisticTxLogLinger;
    }

    /**
     * @return Memory mode.
     */
    public GridCacheMemoryMode memoryMode() {
        return memoryMode;
    }

    /**
     * @param memoryMode New memory mode.
     */
    public void memoryMode(GridCacheMemoryMode memoryMode) {
        this.memoryMode = memoryMode;
    }

    /**
     * @return Name of SPI to use for indexing.
     */
    public String indexingSpiName() {
        return indexingSpiName;
    }

    /**
     * @param indexingSpiName New name of SPI to use for indexing.
     */
    public void indexingSpiName(String indexingSpiName) {
        this.indexingSpiName = indexingSpiName;
    }

    /**
     * @return Cache interceptor.
     */
    @Nullable public String interceptor() {
        return interceptor;
    }

    /**
     * @param interceptor New cache interceptor.
     */
    public void interceptor(@Nullable String interceptor) {
        this.interceptor = interceptor;
    }

    /**
     * @return Cache affinity config.
     */
    public VisorAffinityConfig affinityConfig() {
        return affinity;
    }

    /**
     * @param affinity New cache affinity config.
     */
    public void affinityConfig(VisorAffinityConfig affinity) {
        this.affinity = affinity;
    }

    /**
     * @return Preload config.
     */
    public VisorPreloadConfig preloadConfig() {
        return preload;
    }

    /**
     * @param preload New preload config.
     */
    public void preloadConfig(VisorPreloadConfig preload) {
        this.preload = preload;
    }

    /**
     * @return Eviction config.
     */
    public VisorEvictionConfig evictConfig() {
        return evict;
    }

    /**
     * @param evict New eviction config.
     */
    public void evictConfig(VisorEvictionConfig evict) {
        this.evict = evict;
    }

    /**
     * @return Near cache config.
     */
    public VisorNearCacheConfig nearConfig() {
        return near;
    }

    /**
     * @param near New near cache config.
     */
    public void nearConfig(VisorNearCacheConfig near) {
        this.near = near;
    }

    /**
     * @return Dgc config
     */
    public VisorDefaultConfig defaultConfig() {
        return dflt;
    }

    /**
     * @param dflt New default config
     */
    public void defaultConfig(VisorDefaultConfig dflt) {
        this.dflt = dflt;
    }

    /**
     * @return Dgc config
     */
    public VisorDgcConfig dgcConfig() {
        return dgc;
    }

    /**
     * @param dgc New dgc config
     */
    public void dgcConfig(VisorDgcConfig dgc) {
        this.dgc = dgc;
    }

    /**
     * @return Store config
     */
    public VisorStoreConfig storeConfig() {
        return store;
    }

    /**
     * @param store New store config
     */
    public void storeConfig(VisorStoreConfig store) {
        this.store = store;
    }

    /**
     * @return Write behind config
     */
    public VisorWriteBehindConfig writeBehind() {
        return writeBehind;
    }

    /**
     * @param writeBehind New write behind config
     */
    public void VisorWriteBehindConfig(VisorWriteBehindConfig writeBehind) {
        this.writeBehind = writeBehind;
    }

    /**
     * @return Data center replication send configuration. *
     */
    @Nullable public VisorDrSenderConfig drSendConfig() {
        return drSendConfig;
    }

    /**
     * @param drSndCfg New data center replication send configuration. *
     */
    public void drSendConfig(@Nullable VisorDrSenderConfig drSndCfg) {
        drSendConfig = drSndCfg;
    }

    /**
     * @return Data center replication receive configuration.
     */
    @Nullable public VisorDrReceiverConfig drReceiveConfig() {
        return drReceiveConfig;
    }

    /**
     * @param drRcvCfg New data center replication receive configuration.
     */
    public void drReceiveConfig(@Nullable VisorDrReceiverConfig drRcvCfg) {
        drReceiveConfig = drRcvCfg;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(VisorCacheConfig.class, this);
    }
}
