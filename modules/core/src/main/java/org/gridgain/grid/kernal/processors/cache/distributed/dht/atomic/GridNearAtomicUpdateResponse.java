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

package org.gridgain.grid.kernal.processors.cache.distributed.dht.atomic;

import org.gridgain.grid.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.kernal.processors.cache.*;
import org.gridgain.grid.util.direct.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.grid.util.tostring.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * DHT atomic cache near update response.
 */
public class GridNearAtomicUpdateResponse<K, V> extends GridCacheMessage<K, V> implements GridCacheDeployable {
    /** */
    private static final long serialVersionUID = 0L;

    /** Cache message index. */
    public static final int CACHE_MSG_IDX = nextIndexId();

    /** Node ID this reply should be sent to. */
    @GridDirectTransient
    private UUID nodeId;

    /** Future version. */
    private GridCacheVersion futVer;

    /** Update error. */
    @GridDirectTransient
    private volatile GridException err;

    /** Serialized error. */
    private byte[] errBytes;

    /** Return value. */
    @GridDirectTransient
    private GridCacheReturn<Object> retVal;

    /** Serialized return value. */
    private byte[] retValBytes;

    /** Failed keys. */
    @GridToStringInclude
    @GridDirectTransient
    private volatile Collection<K> failedKeys;

    /** Serialized failed keys. */
    private byte[] failedKeysBytes;

    /** Keys that should be remapped. */
    @GridToStringInclude
    @GridDirectTransient
    private Collection<K> remapKeys;

    /** Serialized keys that should be remapped. */
    private byte[] remapKeysBytes;

    /** Indexes of keys for which values were generated on primary node (used if originating node has near cache). */
    @GridDirectCollection(int.class)
    @GridDirectVersion(1)
    private List<Integer> nearValsIdxs;

    /** Indexes of keys for which update was skipped (used if originating node has near cache). */
    @GridDirectCollection(int.class)
    @GridDirectVersion(1)
    private List<Integer> nearSkipIdxs;

    /** Values generated on primary node which should be put to originating node's near cache. */
    @GridToStringInclude
    @GridDirectTransient
    private List<V> nearVals;

    /** Serialized values generated on primary node which should be put to originating node's near cache. */
    @GridToStringInclude
    @GridDirectCollection(GridCacheValueBytes.class)
    @GridDirectVersion(1)
    private List<GridCacheValueBytes> nearValBytes;

    /** Version generated on primary node to be used for originating node's near cache update. */
    @GridDirectVersion(1)
    private GridCacheVersion nearVer;

    /** Ttl to be used for originating node's near cache update. */
    @GridDirectVersion(1)
    private long nearTtl;

    /**
     * Empty constructor required by {@link Externalizable}.
     */
    public GridNearAtomicUpdateResponse() {
        // No-op.
    }

    /**
     * @param nodeId Node ID this reply should be sent to.
     * @param futVer Future version.
     */
    public GridNearAtomicUpdateResponse(UUID nodeId, GridCacheVersion futVer) {
        this.nodeId = nodeId;
        this.futVer = futVer;
    }

    /** {@inheritDoc} */
    @Override public int lookupIndex() {
        return CACHE_MSG_IDX;
    }

    /**
     * @return Node ID this response should be sent to.
     */
    public UUID nodeId() {
        return nodeId;
    }

    /**
     * @param nodeId Node ID.
     */
    public void nodeId(UUID nodeId) {
        this.nodeId = nodeId;
    }

    /**
     * @return Future version.
     */
    public GridCacheVersion futureVersion() {
        return futVer;
    }

    /**
     * @return Update error, if any.
     */
    public Throwable error() {
        return err;
    }

    /**
     * @return Collection of failed keys.
     */
    public Collection<K> failedKeys() {
        return failedKeys;
    }

    /**
     * @return Return value.
     */
    public GridCacheReturn<Object> returnValue() {
        return retVal;
    }

    /**
     * @param retVal Return value.
     */
    public void returnValue(GridCacheReturn<Object> retVal) {
        this.retVal = retVal;
    }

    /**
     * @param remapKeys Remap keys.
     */
    public void remapKeys(Collection<K> remapKeys) {
        this.remapKeys = remapKeys;
    }

    /**
     * @return Remap keys.
     */
    public Collection<K> remapKeys() {
        return remapKeys;
    }

    /**
     * Adds value to be put in near cache on originating node.
     *
     * @param keyIdx Key index.
     * @param val Value.
     * @param valBytes Value bytes.
     */
    public void addNearValue(int keyIdx, @Nullable V val, @Nullable byte[] valBytes) {
        if (nearValsIdxs == null) {
            nearValsIdxs = new ArrayList<>();
            nearValBytes = new ArrayList<>();
            nearVals = new ArrayList<>();
        }

        nearValsIdxs.add(keyIdx);
        nearVals.add(val);
        nearValBytes.add(valBytes != null ? GridCacheValueBytes.marshaled(valBytes) : null);
    }

    /**
     * @param ttl Time to live to be used for originating node's near cache update.
     */
    public void nearTtl(long ttl) {
        nearTtl = ttl;
    }

    /**
     * @return Time to live to be used for originating node's near cache update.
     */
    public long nearTtl() {
        return nearTtl;
    }

    /**
     * @param nearVer Version generated on primary node to be used for originating node's near cache update.
     */
    public void nearVersion(GridCacheVersion nearVer) {
        this.nearVer = nearVer;
    }

    /**
     * @return Version generated on primary node to be used for originating node's near cache update.
     */
    public GridCacheVersion nearVersion() {
        return nearVer;
    }

    /**
     * @param keyIdx Index of key for which update was skipped
     */
    public void addSkippedIndex(int keyIdx) {
        if (nearSkipIdxs == null)
            nearSkipIdxs = new ArrayList<>();

        nearSkipIdxs.add(keyIdx);
    }

    /**
     * @return Indexes of keys for which update was skipped
     */
    @Nullable public List<Integer> skippedIndexes() {
        return nearSkipIdxs;
    }

    /**
     * @return Indexes of keys for which values were generated on primary node.
     */
   @Nullable public List<Integer> nearValuesIndexes() {
        return nearValsIdxs;
   }

    /**
     * @param idx Index.
     * @return Value generated on primary node which should be put to originating node's near cache.
     */
    @Nullable public V nearValue(int idx) {
        return nearVals.get(idx);
    }

    /**
     * @param idx Index.
     * @return Serialized value generated on primary node which should be put to originating node's near cache.
     */
    @Nullable public byte[] nearValueBytes(int idx) {
        if (nearValBytes != null) {
            GridCacheValueBytes valBytes0 = nearValBytes.get(idx);

            if (valBytes0 != null && !valBytes0.isPlain())
                return valBytes0.get();
        }

        return null;
    }

    /**
     * Adds key to collection of failed keys.
     *
     * @param key Key to add.
     * @param e Error cause.
     */
    public synchronized void addFailedKey(K key, Throwable e) {
        if (failedKeys == null)
            failedKeys = new ConcurrentLinkedQueue<>();

        failedKeys.add(key);

        if (err == null)
            err = new GridException("Failed to update keys on primary node.");

        err.addSuppressed(e);
    }

    /**
     * Adds keys to collection of failed keys.
     *
     * @param keys Key to add.
     * @param e Error cause.
     */
    public synchronized void addFailedKeys(Collection<K> keys, Throwable e) {
        if (failedKeys == null)
            failedKeys = new ArrayList<>(keys.size());

        failedKeys.addAll(keys);

        if (err == null)
            err = new GridException("Failed to update keys on primary node.");

        err.addSuppressed(e);
    }

    /** {@inheritDoc} */
    @Override public void prepareMarshal(GridCacheContext<K, V> ctx) throws GridException {
        super.prepareMarshal(ctx);

        if (err != null)
            errBytes = ctx.marshaller().marshal(err);

        if (retVal != null)
            retValBytes = ctx.marshaller().marshal(retVal);

        if (failedKeys != null)
            failedKeysBytes = ctx.marshaller().marshal(failedKeys);

        if (remapKeys != null)
            remapKeysBytes = ctx.marshaller().marshal(remapKeys);

        nearValBytes = marshalValuesCollection(nearVals, ctx);
    }

    /** {@inheritDoc} */
    @Override public void finishUnmarshal(GridCacheContext<K, V> ctx, ClassLoader ldr) throws GridException {
        super.finishUnmarshal(ctx, ldr);

        if (errBytes != null)
            err = ctx.marshaller().unmarshal(errBytes, ldr);

        if (retValBytes != null)
            retVal = ctx.marshaller().unmarshal(retValBytes, ldr);

        if (failedKeysBytes != null)
            failedKeys = ctx.marshaller().unmarshal(failedKeysBytes, ldr);

        if (remapKeysBytes != null)
            remapKeys = ctx.marshaller().unmarshal(remapKeysBytes, ldr);

        nearVals = unmarshalValueBytesCollection(nearValBytes, ctx, ldr);
    }

    /** {@inheritDoc} */
    @SuppressWarnings({"CloneDoesntCallSuperClone", "CloneCallsConstructors"})
    @Override public GridTcpCommunicationMessageAdapter clone() {
        GridNearAtomicUpdateResponse _clone = new GridNearAtomicUpdateResponse();

        clone0(_clone);

        return _clone;
    }

    /** {@inheritDoc} */
    @Override protected void clone0(GridTcpCommunicationMessageAdapter _msg) {
        super.clone0(_msg);

        GridNearAtomicUpdateResponse _clone = (GridNearAtomicUpdateResponse)_msg;

        _clone.nodeId = nodeId;
        _clone.futVer = futVer;
        _clone.err = err;
        _clone.errBytes = errBytes;
        _clone.retVal = retVal;
        _clone.retValBytes = retValBytes;
        _clone.failedKeys = failedKeys;
        _clone.failedKeysBytes = failedKeysBytes;
        _clone.remapKeys = remapKeys;
        _clone.remapKeysBytes = remapKeysBytes;
        _clone.nearValsIdxs = nearValsIdxs;
        _clone.nearSkipIdxs = nearSkipIdxs;
        _clone.nearVals = nearVals;
        _clone.nearValBytes = nearValBytes;
        _clone.nearVer = nearVer;
        _clone.nearTtl = nearTtl;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("fallthrough")
    @Override public boolean writeTo(ByteBuffer buf) {
        commState.setBuffer(buf);

        if (!super.writeTo(buf))
            return false;

        if (!commState.typeWritten) {
            if (!commState.putByte(directType()))
                return false;

            commState.typeWritten = true;
        }

        switch (commState.idx) {
            case 2:
                if (!commState.putByteArray(errBytes))
                    return false;

                commState.idx++;

            case 3:
                if (!commState.putByteArray(failedKeysBytes))
                    return false;

                commState.idx++;

            case 4:
                if (!commState.putCacheVersion(futVer))
                    return false;

                commState.idx++;

            case 5:
                if (!commState.putByteArray(remapKeysBytes))
                    return false;

                commState.idx++;

            case 6:
                if (!commState.putByteArray(retValBytes))
                    return false;

                commState.idx++;

            case 7:
                if (nearSkipIdxs != null) {
                    if (commState.it == null) {
                        if (!commState.putInt(nearSkipIdxs.size()))
                            return false;

                        commState.it = nearSkipIdxs.iterator();
                    }

                    while (commState.it.hasNext() || commState.cur != NULL) {
                        if (commState.cur == NULL)
                            commState.cur = commState.it.next();

                        if (!commState.putInt((int)commState.cur))
                            return false;

                        commState.cur = NULL;
                    }

                    commState.it = null;
                } else {
                    if (!commState.putInt(-1))
                        return false;
                }

                commState.idx++;

            case 8:
                if (!commState.putLong(nearTtl))
                    return false;

                commState.idx++;

            case 9:
                if (nearValBytes != null) {
                    if (commState.it == null) {
                        if (!commState.putInt(nearValBytes.size()))
                            return false;

                        commState.it = nearValBytes.iterator();
                    }

                    while (commState.it.hasNext() || commState.cur != NULL) {
                        if (commState.cur == NULL)
                            commState.cur = commState.it.next();

                        if (!commState.putValueBytes((GridCacheValueBytes)commState.cur))
                            return false;

                        commState.cur = NULL;
                    }

                    commState.it = null;
                } else {
                    if (!commState.putInt(-1))
                        return false;
                }

                commState.idx++;

            case 10:
                if (nearValsIdxs != null) {
                    if (commState.it == null) {
                        if (!commState.putInt(nearValsIdxs.size()))
                            return false;

                        commState.it = nearValsIdxs.iterator();
                    }

                    while (commState.it.hasNext() || commState.cur != NULL) {
                        if (commState.cur == NULL)
                            commState.cur = commState.it.next();

                        if (!commState.putInt((int)commState.cur))
                            return false;

                        commState.cur = NULL;
                    }

                    commState.it = null;
                } else {
                    if (!commState.putInt(-1))
                        return false;
                }

                commState.idx++;

            case 11:
                if (!commState.putCacheVersion(nearVer))
                    return false;

                commState.idx++;

        }

        return true;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("fallthrough")
    @Override public boolean readFrom(ByteBuffer buf) {
        commState.setBuffer(buf);

        if (!super.readFrom(buf))
            return false;

        switch (commState.idx) {
            case 2:
                byte[] errBytes0 = commState.getByteArray();

                if (errBytes0 == BYTE_ARR_NOT_READ)
                    return false;

                errBytes = errBytes0;

                commState.idx++;

            case 3:
                byte[] failedKeysBytes0 = commState.getByteArray();

                if (failedKeysBytes0 == BYTE_ARR_NOT_READ)
                    return false;

                failedKeysBytes = failedKeysBytes0;

                commState.idx++;

            case 4:
                GridCacheVersion futVer0 = commState.getCacheVersion();

                if (futVer0 == CACHE_VER_NOT_READ)
                    return false;

                futVer = futVer0;

                commState.idx++;

            case 5:
                byte[] remapKeysBytes0 = commState.getByteArray();

                if (remapKeysBytes0 == BYTE_ARR_NOT_READ)
                    return false;

                remapKeysBytes = remapKeysBytes0;

                commState.idx++;

            case 6:
                byte[] retValBytes0 = commState.getByteArray();

                if (retValBytes0 == BYTE_ARR_NOT_READ)
                    return false;

                retValBytes = retValBytes0;

                commState.idx++;

            case 7:
                if (commState.readSize == -1) {
                    if (buf.remaining() < 4)
                        return false;

                    commState.readSize = commState.getInt();
                }

                if (commState.readSize >= 0) {
                    if (nearSkipIdxs == null)
                        nearSkipIdxs = new ArrayList<>(commState.readSize);

                    for (int i = commState.readItems; i < commState.readSize; i++) {
                        if (buf.remaining() < 4)
                            return false;

                        int _val = commState.getInt();

                        nearSkipIdxs.add((Integer)_val);

                        commState.readItems++;
                    }
                }

                commState.readSize = -1;
                commState.readItems = 0;

                commState.idx++;

            case 8:
                if (buf.remaining() < 8)
                    return false;

                nearTtl = commState.getLong();

                commState.idx++;

            case 9:
                if (commState.readSize == -1) {
                    if (buf.remaining() < 4)
                        return false;

                    commState.readSize = commState.getInt();
                }

                if (commState.readSize >= 0) {
                    if (nearValBytes == null)
                        nearValBytes = new ArrayList<>(commState.readSize);

                    for (int i = commState.readItems; i < commState.readSize; i++) {
                        GridCacheValueBytes _val = commState.getValueBytes();

                        if (_val == VAL_BYTES_NOT_READ)
                            return false;

                        nearValBytes.add((GridCacheValueBytes)_val);

                        commState.readItems++;
                    }
                }

                commState.readSize = -1;
                commState.readItems = 0;

                commState.idx++;

            case 10:
                if (commState.readSize == -1) {
                    if (buf.remaining() < 4)
                        return false;

                    commState.readSize = commState.getInt();
                }

                if (commState.readSize >= 0) {
                    if (nearValsIdxs == null)
                        nearValsIdxs = new ArrayList<>(commState.readSize);

                    for (int i = commState.readItems; i < commState.readSize; i++) {
                        if (buf.remaining() < 4)
                            return false;

                        int _val = commState.getInt();

                        nearValsIdxs.add((Integer)_val);

                        commState.readItems++;
                    }
                }

                commState.readSize = -1;
                commState.readItems = 0;

                commState.idx++;

            case 11:
                GridCacheVersion nearVer0 = commState.getCacheVersion();

                if (nearVer0 == CACHE_VER_NOT_READ)
                    return false;

                nearVer = nearVer0;

                commState.idx++;

        }

        return true;
    }

    /** {@inheritDoc} */
    @Override public byte directType() {
        return 40;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridNearAtomicUpdateResponse.class, this, "parent");
    }
}
