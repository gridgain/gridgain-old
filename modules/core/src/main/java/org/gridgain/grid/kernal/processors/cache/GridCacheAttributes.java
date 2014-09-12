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

package org.gridgain.grid.kernal.processors.cache;

import org.gridgain.grid.cache.*;
import org.gridgain.grid.cache.affinity.*;
import org.gridgain.grid.cache.affinity.consistenthash.*;
import org.gridgain.grid.kernal.processors.cache.dr.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.jetbrains.annotations.*;

import java.io.*;

import static org.gridgain.grid.cache.GridCacheConfiguration.*;
import static org.gridgain.grid.cache.GridCacheMode.*;
import static org.gridgain.grid.cache.GridCacheDistributionMode.*;

/**
 * Cache attributes.
 * <p>
 * This class contains information on a single cache configured on some node.
 */
public class GridCacheAttributes implements Externalizable {
    /** */
    private static final long serialVersionUID = 0L;

    /** Cache name. */
    private String name;

    /** Cache mode. */
    private GridCacheMode cacheMode;

    /** Cache atomicity mode. */
    private GridCacheAtomicityMode atomicityMode;

    /** Default time to live for cache entries. */
    private long ttl;

    /** Flag indicating whether eviction is synchronized. */
    private boolean evictSync;

    /** Flag indicating whether eviction is synchronized with near nodes. */
    private boolean evictNearSync;

    /** Maximum eviction overflow ratio. */
    private float evictMaxOverflowRatio;

    /** Transaction isolation. */
    private GridCacheTxIsolation dfltIsolation;

    /** Transaction concurrency. */
    private GridCacheTxConcurrency dfltConcurrency;

    /** Default transaction serializable flag. */
    private boolean txSerEnabled;

    /** Flag to enable transactional batch update. */
    private boolean txBatchUpdate;

    /** Default transaction timeout. */
    private long dfltTxTimeout;

    /** Default query timeout. */
    private long dfltQryTimeout;

    /** Default lock timeout. */
    private long dfltLockTimeout;

    /** Flag indicating if cached values should be additionally stored in serialized form. */
    private boolean storeValBytes;

    /** Cache preload mode. */
    private GridCachePreloadMode preloadMode;

    /** Partitioned cache mode. */
    private GridCacheDistributionMode partDistro;

    /** Preload batch size. */
    private int preloadBatchSize;

    /** Distributed garbage collection frequency. */
    private long dgcFreq;

    /** Timeout after which DGC will consider remote locks as suspects. */
    private long dgcSuspectLockTimeout;

    /** Flag indicating whether DGC should remove locks. */
    private boolean dgcRmvLocks;

    /** Synchronization mode. */
    private GridCacheWriteSynchronizationMode writeSyncMode;

    /** Flag indicating whether GridGain should use swap storage by default. */
    protected boolean swapEnabled;

    /** Flag indicating whether  query indexing is enabled. */
    private boolean qryIdxEnabled;

    /** Flag indicating whether GridGain should activate read-through/write-through behaviour by default. */
    private boolean storeEnabled;

    /** Flag indicating whether GridGain should use write-behind behaviour for the cache store. */
    private boolean writeBehindEnabled;

    /** Maximum size of write-behind cache. */
    private int writeBehindFlushSize;

    /** Write-behind flush frequency in milliseconds. */
    private long writeBehindFlushFreq;

    /** Flush thread count for write-behind cache store. */
    private int writeBehindFlushThreadCnt;

    /** Maximum batch size for write-behind cache store. */
    private int writeBehindBatchSize;

    /** Default batch size for all cache's sequences. */
    private int seqReserveSize;

    /** Name of SPI to use for indexing. */
    private String indexingSpiName;

    /** Cache affinity class name. */
    private String affClsName;

    /** Affinity mapper class name. */
    private String affMapperClsName;

    /** */
    private boolean affInclNeighbors;

    /** */
    private int affKeyBackups = -1;

    /** */
    private int affReplicas = -1;

    /** */
    private String affReplicaCntAttrName;

    /** */
    private String affHashIdRslvrClsName;

    /** */
    private int affPartsCnt;

    /** Cloner class name. */
    private String clonerClsName;

    /** Eviction filter class name. */
    private String evictFilterClsName;

    /** Eviction policy class name. */
    private String evictPlcClsName;

    /** Near eviction policy class name. */
    private String nearEvictPlcClsName;

    /** Cache store class name. */
    private String storeClsName;

    /** Transaction Manager lookup class name. */
    private String tmLookupClsName;

    /** DR receive attributes. */
    private GridCacheDrReceiveAttributes drRcvAttrs;

    /** DR send attributes. */
    private GridCacheDrSendAttributes drSndAttrs;

    /**
     * @param cfg Cache configuration.
     */
    public GridCacheAttributes(GridCacheConfiguration cfg) {
        atomicityMode = cfg.getAtomicityMode();
        cacheMode = cfg.getCacheMode();
        dfltConcurrency = cfg.getDefaultTxConcurrency();
        dfltIsolation = cfg.getDefaultTxIsolation();
        dfltLockTimeout = cfg.getDefaultLockTimeout();
        dfltQryTimeout = cfg.getDefaultQueryTimeout();
        dfltTxTimeout = cfg.getDefaultTxTimeout();
        dgcFreq = cfg.getDgcFrequency();
        dgcRmvLocks = cfg.isDgcRemoveLocks();
        dgcSuspectLockTimeout  = cfg.getDgcSuspectLockTimeout();

        drRcvAttrs = cfg.getDrReceiverConfiguration() != null ?
            new GridCacheDrReceiveAttributes(cfg.getDrReceiverConfiguration()) : null;

        drSndAttrs = cfg.getDrSenderConfiguration() != null ?
            new GridCacheDrSendAttributes(cfg.getDrSenderConfiguration()) : null;

        evictMaxOverflowRatio = cfg.getEvictMaxOverflowRatio();
        evictNearSync = cfg.isEvictNearSynchronized();
        evictSync = cfg.isEvictSynchronized();
        indexingSpiName = cfg.getIndexingSpiName();
        name = cfg.getName();
        partDistro = GridCacheUtils.distributionMode(cfg);
        preloadBatchSize = cfg.getPreloadBatchSize();
        preloadMode = cfg.getPreloadMode();
        qryIdxEnabled = cfg.isQueryIndexEnabled();
        seqReserveSize = cfg.getAtomicSequenceReserveSize();
        storeEnabled = cfg.getStore() != null;
        storeValBytes = cfg.isStoreValueBytes();
        swapEnabled = cfg.isSwapEnabled();
        ttl = cfg.getDefaultTimeToLive();
        txBatchUpdate  = cfg.isBatchUpdateOnCommit();
        txSerEnabled = cfg.isTxSerializableEnabled();
        writeBehindBatchSize = cfg.getWriteBehindBatchSize();
        writeBehindEnabled = cfg.isWriteBehindEnabled();
        writeBehindFlushFreq  = cfg.getWriteBehindFlushFrequency();
        writeBehindFlushSize = cfg.getWriteBehindFlushSize();
        writeBehindFlushThreadCnt = cfg.getWriteBehindFlushThreadCount();
        writeSyncMode = cfg.getWriteSynchronizationMode();

        affMapperClsName = className(cfg.getAffinityMapper());

        affKeyBackups = cfg.getBackups();

        GridCacheAffinityFunction aff = cfg.getAffinity();

        if (aff != null) {
            if (aff instanceof GridCacheConsistentHashAffinityFunction) {
                GridCacheConsistentHashAffinityFunction aff0 = (GridCacheConsistentHashAffinityFunction) aff;

                affInclNeighbors = aff0.isExcludeNeighbors();
                affReplicas = aff0.getDefaultReplicas();
                affReplicaCntAttrName = aff0.getReplicaCountAttributeName();
                affHashIdRslvrClsName = className(aff0.getHashIdResolver());
            }

            affPartsCnt = aff.partitions();
            affClsName = className(aff);
        }

        clonerClsName = className(cfg.getCloner());
        evictFilterClsName = className(cfg.getEvictionFilter());
        evictPlcClsName = className(cfg.getEvictionPolicy());
        nearEvictPlcClsName = className(cfg.getNearEvictionPolicy());
        storeClsName = className(cfg.getStore());
        tmLookupClsName = cfg.getTransactionManagerLookupClassName();
    }

    /**
     * Public no-arg constructor for {@link Externalizable}.
     */
    public GridCacheAttributes() {
        // No-op.
    }

    /**
     * @return Cache name.
     */
    public String cacheName() {
        return name;
    }

    /**
     * @return Cache mode.
     */
    public GridCacheMode cacheMode() {
        return cacheMode != null ? cacheMode : DFLT_CACHE_MODE;
    }

    /**
     * @return Cache atomicity mode.
     */
    public GridCacheAtomicityMode atomicityMode() {
        return atomicityMode != null ? atomicityMode : DFLT_CACHE_ATOMICITY_MODE;
    }

    /**
     * @return {@code True} if near cache is enabled.
     */
    public boolean nearCacheEnabled() {
        return cacheMode() != LOCAL &&
            (partDistro == NEAR_PARTITIONED || partDistro == NEAR_ONLY);
    }

    /**
     * @return {@code True} if the local node will not contribute any local storage to this
     * cache, {@code false} otherwise.
     */
    @SuppressWarnings("SimplifiableIfStatement")
    public boolean isAffinityNode() {
        if (cacheMode() == LOCAL)
            return true;

        return partDistro == PARTITIONED_ONLY || partDistro == NEAR_PARTITIONED;
    }

    /**
     * @return Preload mode.
     */
    public GridCachePreloadMode cachePreloadMode() {
        return preloadMode;
    }

    /**
     * @return Affinity class name.
     */
    public String cacheAffinityClassName() {
        return affClsName;
    }

    /**
     * @return Affinity mapper class name.
     */
    public String cacheAffinityMapperClassName() {
        return affMapperClsName;
    }

    /**
     * @return Affinity include neighbors.
     */
    public boolean affinityIncludeNeighbors() {
        return affInclNeighbors;
    }

    /**
     * @return Affinity key backups.
     */
    public int affinityKeyBackups() {
        return affKeyBackups;
    }

    /**
     * @return Affinity replicas.
     */
    public int affinityReplicas() {
        return affReplicas;
    }

    /**
     * @return Affinity partitions count.
     */
    public int affinityPartitionsCount() {
        return affPartsCnt;
    }

    /**
     * @return Aff replicas count attr name.
     */
    public String affinityReplicaCountAttrName() {
        return affReplicaCntAttrName;
    }

    /**
     * @return Affinity hash ID resolver class name.
     */
    public String affinityHashIdResolverClassName() {
        return affHashIdRslvrClsName;
    }

    /**
     * @return Cloner class name.
     */
    public String clonerClassName() {
        return clonerClsName;
    }

    /**
     * @return Eviction filter class name.
     */
    public String evictionFilterClassName() {
        return evictFilterClsName;
    }

    /**
     * @return Eviction policy class name.
     */
    public String evictionPolicyClassName() {
        return evictPlcClsName;
    }

    /**
     * @return Near eviction policy class name.
     */
    public String nearEvictionPolicyClassName() {
        return nearEvictPlcClsName;
    }

    /**
     * @return Store class name.
     */
    public String storeClassName() {
        return storeClsName;
    }

    /**
     * @return Transaction manager lookup class name.
     */
    public String transactionManagerLookupClassName() {
        return tmLookupClsName;
    }

    /**
     * @return {@code True} if swap enabled.
     */
    public boolean swapEnabled() {
        return swapEnabled;
    }


    /**
     * @return Default time to live for cache entries.
     */
    public long defaultTimeToLive() {
        return ttl;
    }

    /**
     * @return Flag indicating whether eviction is synchronized.
     */
    public boolean evictSynchronized() {
        return evictSync;
    }

    /**
     * @return Flag indicating whether eviction is synchronized with near nodes.
     */
    public boolean evictNearSynchronized() {
        return evictNearSync;
    }

    /**
     * @return Maximum eviction overflow ratio.
     */
    public float evictMaxOverflowRatio() {
        return evictMaxOverflowRatio;
    }

    /**
     * @return Transaction isolation.
     */
    public GridCacheTxIsolation defaultIsolation() {
        return dfltIsolation;
    }

    /**
     * @return Transaction concurrency.
     */
    public GridCacheTxConcurrency defaultConcurrency() {
        return dfltConcurrency;
    }

    /**
     * @return Partitioned cache mode.
     */
    public GridCacheDistributionMode partitionedTaxonomy() {
        return partDistro;
    }

    /**
     * @return Default transaction serializable flag.
     */
    public boolean txSerializableEnabled() {
        return txSerEnabled;
    }

    /**
     * @return Flag to enable transactional batch update.
     */
    public boolean txBatchUpdate() {
        return txBatchUpdate;
    }

    /**
     * @return Default transaction timeout.
     */
    public long defaultTxTimeout() {
        return dfltTxTimeout;
    }

    /**
     * @return Default query timeout.
     */
    public long defaultQueryTimeout() {
        return dfltQryTimeout;
    }

    /**
     * @return Default lock timeout.
     */
    public long defaultLockTimeout() {
        return dfltLockTimeout;
    }

    /**
     * @return Flag indicating if cached values should be additionally stored in serialized form.
     */
    public boolean storeValueBytes() {
        return storeValBytes;
    }

    /**
     * @return Preload batch size.
     */
    public int preloadBatchSize() {
        return preloadBatchSize;
    }

    /**
     * @return Distributed garbage collection frequency.
     */
    public long dgcFrequency() {
        return dgcFreq;
    }

    /**
     * @return Timeout after which DGC will consider remote locks as suspects.
     */
    public long dgcSuspectLockTimeout() {
        return dgcSuspectLockTimeout;
    }

    /**
     * @return Flag indicating whether DGC should remove locks.
     */
    public boolean dgcRemoveLocks() {
        return dgcRmvLocks;
    }

    /**
     * @return Synchronization mode.
     */
    public GridCacheWriteSynchronizationMode writeSynchronization() {
        return writeSyncMode;
    }

    /**
     * @return Flag indicating whether  query indexing is enabled.
     */
    public boolean queryIndexEnabled() {
        return qryIdxEnabled;
    }

    /**
     * @return Flag indicating whether GridGain should activate read-through/write-through behaviour by default.
     */
    public boolean storeEnabled() {
        return storeEnabled;
    }

    /**
     * @return Flag indicating whether GridGain should use write-behind behaviour for the cache store.
     */
    public boolean writeBehindEnabled() {
        return writeBehindEnabled;
    }

    /**
     * @return Maximum size of write-behind cache.
     */
    public int writeBehindFlushSize() {
        return writeBehindFlushSize;
    }

    /**
     * @return Write-behind flush frequency in milliseconds.
     */
    public long writeBehindFlushFrequency() {
        return writeBehindFlushFreq;
    }

    /**
     * @return Flush thread count for write-behind cache store.
     */
    public int writeBehindFlushThreadCount() {
        return writeBehindFlushThreadCnt;
    }

    /**
     * @return Maximum batch size for write-behind cache store.
     */
    public int writeBehindBatchSize() {
        return writeBehindBatchSize;
    }

    /**
     * @return Default batch size for all cache's sequences.
     */
    public int sequenceReserveSize() {
        return seqReserveSize;
    }

    /**
     * @return Name of SPI to use for indexing.
     */
    public String indexingSpiName() {
        return indexingSpiName;
    }

    /**
     * @return DR receive attributes.
     */
    @Nullable public GridCacheDrReceiveAttributes drReceiveAttributes() {
        return drRcvAttrs;
    }

    /**
     * @return DR send attributes.
     */
    @Nullable public GridCacheDrSendAttributes drSendAttributes() {
        return drSndAttrs;
    }

    /** {@inheritDoc} */
    @Override public void writeExternal(ObjectOutput out) throws IOException {
        U.writeEnum0(out, atomicityMode);
        U.writeEnum0(out, cacheMode);
        U.writeEnum0(out, dfltConcurrency);
        U.writeEnum0(out, dfltIsolation);
        out.writeLong(dfltLockTimeout);
        out.writeLong(dfltQryTimeout);
        out.writeLong(dfltTxTimeout);
        out.writeLong(dgcFreq);
        out.writeBoolean(dgcRmvLocks);
        out.writeLong(dgcSuspectLockTimeout);
        out.writeFloat(evictMaxOverflowRatio);
        out.writeBoolean(evictNearSync);
        out.writeBoolean(evictSync);
        U.writeString(out, indexingSpiName);
        U.writeString(out, name);
        U.writeEnum0(out, partDistro);
        out.writeInt(preloadBatchSize);
        U.writeEnum0(out, preloadMode);
        out.writeBoolean(qryIdxEnabled);
        out.writeInt(seqReserveSize);
        out.writeBoolean(storeEnabled);
        out.writeBoolean(storeValBytes);
        out.writeBoolean(swapEnabled);
        out.writeLong(ttl);
        out.writeBoolean(txBatchUpdate);
        out.writeBoolean(txSerEnabled);
        out.writeInt(writeBehindBatchSize);
        out.writeBoolean(writeBehindEnabled);
        out.writeLong(writeBehindFlushFreq);
        out.writeInt(writeBehindFlushSize);
        out.writeInt(writeBehindFlushThreadCnt);
        U.writeEnum0(out, writeSyncMode);

        U.writeString(out, affClsName);
        U.writeString(out, affMapperClsName);
        out.writeBoolean(affInclNeighbors);
        out.writeInt(affKeyBackups);
        out.writeInt(affPartsCnt);
        out.writeInt(affReplicas);
        U.writeString(out, affReplicaCntAttrName);
        U.writeString(out, affHashIdRslvrClsName);

        U.writeString(out, clonerClsName);
        U.writeString(out, evictFilterClsName);
        U.writeString(out, evictPlcClsName);
        U.writeString(out, nearEvictPlcClsName);
        U.writeString(out, storeClsName);
        U.writeString(out, tmLookupClsName);

        out.writeObject(drRcvAttrs);
        out.writeObject(drSndAttrs);
    }

    /** {@inheritDoc} */
    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        atomicityMode = GridCacheAtomicityMode.fromOrdinal(U.readEnumOrdinal0(in));
        cacheMode = GridCacheMode.fromOrdinal(U.readEnumOrdinal0(in));
        dfltConcurrency = GridCacheTxConcurrency.fromOrdinal(U.readEnumOrdinal0(in));
        dfltIsolation = GridCacheTxIsolation.fromOrdinal(U.readEnumOrdinal0(in));
        dfltLockTimeout = in.readLong();
        dfltQryTimeout = in.readLong();
        dfltTxTimeout = in.readLong();
        dgcFreq = in.readLong();
        dgcRmvLocks = in.readBoolean();
        dgcSuspectLockTimeout = in.readLong();
        evictMaxOverflowRatio = in.readFloat();
        evictNearSync = in.readBoolean();
        evictSync  = in.readBoolean();
        indexingSpiName = U.readString(in);
        name = U.readString(in);
        partDistro = GridCacheDistributionMode.fromOrdinal(U.readEnumOrdinal0(in));
        preloadBatchSize = in.readInt();
        preloadMode = GridCachePreloadMode.fromOrdinal(U.readEnumOrdinal0(in));
        qryIdxEnabled = in.readBoolean();
        seqReserveSize = in.readInt();
        storeEnabled = in.readBoolean();
        storeValBytes = in.readBoolean();
        swapEnabled = in.readBoolean();
        ttl = in.readLong();
        txBatchUpdate = in.readBoolean();
        txSerEnabled = in.readBoolean();
        writeBehindBatchSize = in.readInt();
        writeBehindEnabled = in.readBoolean();
        writeBehindFlushFreq = in.readLong();
        writeBehindFlushSize = in.readInt();
        writeBehindFlushThreadCnt = in.readInt();
        writeSyncMode = GridCacheWriteSynchronizationMode.fromOrdinal(U.readEnumOrdinal0(in));

        affClsName = U.readString(in);
        affMapperClsName = U.readString(in);
        affInclNeighbors = in.readBoolean();
        affKeyBackups = in.readInt();
        affPartsCnt = in.readInt();
        affReplicas = in.readInt();
        affReplicaCntAttrName = U.readString(in);
        affHashIdRslvrClsName = U.readString(in);

        clonerClsName = U.readString(in);
        evictFilterClsName = U.readString(in);
        evictPlcClsName = U.readString(in);
        nearEvictPlcClsName = U.readString(in);
        storeClsName = U.readString(in);
        tmLookupClsName = U.readString(in);

        drRcvAttrs = (GridCacheDrReceiveAttributes)in.readObject();
        drSndAttrs = (GridCacheDrSendAttributes)in.readObject();
    }

    /**
     * @param obj Object to get class of.
     * @return Class name or {@code null}.
     */
    @Nullable private static String className(@Nullable Object obj) {
        return obj != null ? obj.getClass().getName() : null;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridCacheAttributes.class, this);
    }
}
