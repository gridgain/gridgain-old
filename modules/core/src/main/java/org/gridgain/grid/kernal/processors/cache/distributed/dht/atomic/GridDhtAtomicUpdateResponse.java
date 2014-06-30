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

/**
 * DHT atomic cache backup update response.
 */
public class GridDhtAtomicUpdateResponse<K, V> extends GridCacheMessage<K, V> implements GridCacheDeployable {
    /** */
    private static final long serialVersionUID = 0L;

    /** Message index. */
    public static final int CACHE_MSG_IDX = nextIndexId();

    /** Future version. */
    private GridCacheVersion futVer;

    /** Failed keys. */
    @GridToStringInclude
    @GridDirectTransient
    private Collection<K> failedKeys;

    /** Serialized failed keys. */
    private byte[] failedKeysBytes;

    /** Update error. */
    @GridDirectTransient
    private GridException err;

    /** Serialized update error. */
    private byte[] errBytes;

    /** Evicted readers. */
    @GridToStringInclude
    @GridDirectTransient
    private Collection<K> nearEvicted;

    /** Evicted reader key bytes. */
    @GridDirectCollection(byte[].class)
    @GridDirectVersion(1)
    private Collection<byte[]> nearEvictedBytes;

    /**
     * Empty constructor required by {@link Externalizable}.
     */
    public GridDhtAtomicUpdateResponse() {
        // No-op.
    }

    /**
     * @param futVer Future version.
     */
    public GridDhtAtomicUpdateResponse(GridCacheVersion futVer) {
        this.futVer = futVer;
    }

    /** {@inheritDoc} */
    @Override public int lookupIndex() {
        return CACHE_MSG_IDX;
    }

    /**
     * @return Future version.
     */
    public GridCacheVersion futureVersion() {
        return futVer;
    }

    /**
     * @return Gets update error.
     */
    public GridException error() {
        return err;
    }

    /**
     * @return Failed keys.
     */
    public Collection<K> failedKeys() {
        return failedKeys;
    }

    /**
     * Adds key to collection of failed keys.
     *
     * @param key Key to add.
     * @param e Error cause.
     */
    public void addFailedKey(K key, Throwable e) {
        if (failedKeys == null)
            failedKeys = new ArrayList<>();

        failedKeys.add(key);

        if (err == null)
            err = new GridException("Failed to update keys on primary node.");

        err.addSuppressed(e);
    }

    /**
     * @return Evicted readers.
     */
    public Collection<K> nearEvicted() {
        return nearEvicted;
    }

    /**
     * Adds near evicted key..
     *
     * @param key Evicted key.
     * @param bytes Bytes of evicted key.
     */
    public void addNearEvicted(K key, @Nullable byte[] bytes) {
        if (nearEvicted == null)
            nearEvicted = new ArrayList<>();

        nearEvicted.add(key);

        if (bytes != null) {
            if (nearEvictedBytes == null)
                nearEvictedBytes = new ArrayList<>();

            nearEvictedBytes.add(bytes);
        }
    }

    /** {@inheritDoc} */
    @Override public void prepareMarshal(GridCacheContext<K, V> ctx) throws GridException {
        super.prepareMarshal(ctx);

        failedKeysBytes = ctx.marshaller().marshal(failedKeys);
        errBytes = ctx.marshaller().marshal(err);

        if (nearEvictedBytes == null)
            nearEvictedBytes = marshalCollection(nearEvicted, ctx);
    }

    /** {@inheritDoc} */
    @Override public void finishUnmarshal(GridCacheContext<K, V> ctx, ClassLoader ldr) throws GridException {
        super.finishUnmarshal(ctx, ldr);

        failedKeys = ctx.marshaller().unmarshal(failedKeysBytes, ldr);
        err = ctx.marshaller().unmarshal(errBytes, ldr);

        if (nearEvicted == null && nearEvictedBytes != null)
            nearEvicted = unmarshalCollection(nearEvictedBytes, ctx, ldr);
    }

    /** {@inheritDoc} */
    @SuppressWarnings({"CloneDoesntCallSuperClone", "CloneCallsConstructors"})
    @Override public GridTcpCommunicationMessageAdapter clone() {
        GridDhtAtomicUpdateResponse _clone = new GridDhtAtomicUpdateResponse();

        clone0(_clone);

        return _clone;
    }

    /** {@inheritDoc} */
    @Override protected void clone0(GridTcpCommunicationMessageAdapter _msg) {
        super.clone0(_msg);

        GridDhtAtomicUpdateResponse _clone = (GridDhtAtomicUpdateResponse)_msg;

        _clone.futVer = futVer;
        _clone.failedKeys = failedKeys;
        _clone.failedKeysBytes = failedKeysBytes;
        _clone.err = err;
        _clone.errBytes = errBytes;
        _clone.nearEvicted = nearEvicted;
        _clone.nearEvictedBytes = nearEvictedBytes;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("all")
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
                if (nearEvictedBytes != null) {
                    if (commState.it == null) {
                        if (!commState.putInt(nearEvictedBytes.size()))
                            return false;

                        commState.it = nearEvictedBytes.iterator();
                    }

                    while (commState.it.hasNext() || commState.cur != NULL) {
                        if (commState.cur == NULL)
                            commState.cur = commState.it.next();

                        if (!commState.putByteArray((byte[])commState.cur))
                            return false;

                        commState.cur = NULL;
                    }

                    commState.it = null;
                } else {
                    if (!commState.putInt(-1))
                        return false;
                }

                commState.idx++;

        }

        return true;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("all")
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
                if (commState.readSize == -1) {
                    if (buf.remaining() < 4)
                        return false;

                    commState.readSize = commState.getInt();
                }

                if (commState.readSize >= 0) {
                    if (nearEvictedBytes == null)
                        nearEvictedBytes = new ArrayList<>(commState.readSize);

                    for (int i = commState.readItems; i < commState.readSize; i++) {
                        byte[] _val = commState.getByteArray();

                        if (_val == BYTE_ARR_NOT_READ)
                            return false;

                        nearEvictedBytes.add((byte[])_val);

                        commState.readItems++;
                    }
                }

                commState.readSize = -1;
                commState.readItems = 0;

                commState.idx++;

        }

        return true;
    }

    /** {@inheritDoc} */
    @Override public byte directType() {
        return 38;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridDhtAtomicUpdateResponse.class, this);
    }
}
