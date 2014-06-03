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
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.kernal.processors.cache.*;
import org.gridgain.grid.kernal.processors.cache.distributed.*;
import org.gridgain.grid.util.direct.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.grid.util.lang.*;
import org.gridgain.grid.util.tostring.*;

import java.io.*;
import java.nio.*;
import java.util.*;

/**
 * Near cache prepare response.
 */
public class GridNearTxPrepareResponse<K, V> extends GridDistributedTxPrepareResponse<K, V> {
    /** */
    private static final long serialVersionUID = 0L;

    /** Collection of versions that are pending and less than lock version. */
    @GridToStringInclude
    @GridDirectCollection(GridCacheVersion.class)
    private Collection<GridCacheVersion> pending;

    /** Future ID.  */
    private GridUuid futId;

    /** Mini future ID. */
    private GridUuid miniId;

    /** DHT version. */
    private GridCacheVersion dhtVer;

    /** */
    @GridToStringInclude
    @GridDirectCollection(int.class)
    private Collection<Integer> invalidParts;

    /** Map of owned values to set on near node. */
    @GridToStringInclude
    @GridDirectTransient
    private Map<K, GridTuple3<GridCacheVersion, V, byte[]>> ownedVals;

    /** Marshalled owned bytes. */
    @GridToStringExclude
    @GridDirectCollection(byte[].class)
    private Collection<byte[]> ownedValsBytes;

    /**
     * Empty constructor required by {@link Externalizable}.
     */
    public GridNearTxPrepareResponse() {
        // No-op.
    }

    /**
     * @param xid Xid version.
     * @param futId Future ID.
     * @param miniId Mini future ID.
     * @param dhtVer DHT version.
     * @param invalidParts Invalid partitions.
     * @param err Error.
     */
    public GridNearTxPrepareResponse(GridCacheVersion xid, GridUuid futId, GridUuid miniId, GridCacheVersion dhtVer,
        Collection<Integer> invalidParts, Throwable err) {
        super(xid, err);

        assert futId != null;
        assert miniId != null;
        assert dhtVer != null;

        this.futId = futId;
        this.miniId = miniId;
        this.dhtVer = dhtVer;
        this.invalidParts = invalidParts;
    }

    /**
     * Gets pending versions that are less than {@link #version()}.
     *
     * @return Pending versions.
     */
    public Collection<GridCacheVersion> pending() {
        return pending == null ? Collections.<GridCacheVersion>emptyList() : pending;
    }

    /**
     * Sets pending versions that are less than {@link #version()}.
     *
     * @param pending Pending versions.
     */
    public void pending(Collection<GridCacheVersion> pending) {
        this.pending = pending;
    }

    /**
     * @return Mini future ID.
     */
    public GridUuid miniId() {
        return miniId;
    }

    /**
     * @return Future ID.
     */
    public GridUuid futureId() {
        return futId;
    }

    /**
     * @return DHT version.
     */
    public GridCacheVersion dhtVersion() {
        return dhtVer;
    }

    /**
     * Adds owned value.
     *
     * @param key Key.
     * @param ver DHT version.
     * @param val Value.
     * @param valBytes Value bytes.
     */
    public void addOwnedValue(K key, GridCacheVersion ver, V val, byte[] valBytes) {
        if (ownedVals == null)
            ownedVals = new HashMap<>();

        ownedVals.put(key, F.t(ver, val, valBytes));
    }

    /**
     * @return Owned values map.
     */
    public Map<K, GridTuple3<GridCacheVersion, V, byte[]>> ownedValues() {
        return ownedVals == null ? Collections.<K, GridTuple3<GridCacheVersion,V,byte[]>>emptyMap() :
            Collections.unmodifiableMap(ownedVals);
    }

    /**
     * @param key Key.
     * @return {@code True} if response has owned value for given key.
     */
    public boolean hasOwnedValue(K key) {
        return ownedVals != null && ownedVals.containsKey(key);
    }

    /**
     * @return Invalid partitions.
     */
    public Collection<Integer> invalidPartitions() {
        return invalidParts;
    }

    /** {@inheritDoc} */
    @Override public void prepareMarshal(GridCacheContext<K, V> ctx) throws GridException {
        super.prepareMarshal(ctx);

        if (ownedVals != null && ownedValsBytes == null) {
            ownedValsBytes = new ArrayList<>(ownedVals.size());

            for (Map.Entry<K, GridTuple3<GridCacheVersion, V, byte[]>> entry : ownedVals.entrySet()) {
                GridTuple3<GridCacheVersion, V, byte[]> tup = entry.getValue();

                boolean rawBytes = false;

                byte[] valBytes = tup.get3();

                if (valBytes == null) {
                    if (tup.get2() != null && tup.get2() instanceof byte[]) {
                        rawBytes = true;

                        valBytes = (byte[])tup.get2();
                    }
                    else
                        valBytes = ctx.marshaller().marshal(tup.get2());
                }

                ownedValsBytes.add(ctx.marshaller().marshal(F.t(entry.getKey(), tup.get1(), valBytes, rawBytes)));
            }
        }
    }

    /** {@inheritDoc} */
    @Override public void finishUnmarshal(GridCacheContext<K, V> ctx, ClassLoader ldr) throws GridException {
        super.finishUnmarshal(ctx, ldr);

        if (ownedValsBytes != null && ownedVals == null) {
            ownedVals = new HashMap<>();

            for (byte[] bytes : ownedValsBytes) {
                GridTuple4<K, GridCacheVersion, byte[], Boolean> tup = ctx.marshaller().unmarshal(bytes, ldr);

                V val = tup.get4() ? (V)tup.get3() : ctx.marshaller().<V>unmarshal(tup.get3(), ldr);

                ownedVals.put(tup.get1(), F.t(tup.get2(), val, tup.get4() ? null : tup.get3()));
            }
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings({"CloneDoesntCallSuperClone", "CloneCallsConstructors"})
    @Override public GridTcpCommunicationMessageAdapter clone() {
        GridNearTxPrepareResponse _clone = new GridNearTxPrepareResponse();

        clone0(_clone);

        return _clone;
    }

    /** {@inheritDoc} */
    @Override protected void clone0(GridTcpCommunicationMessageAdapter _msg) {
        super.clone0(_msg);

        GridNearTxPrepareResponse _clone = (GridNearTxPrepareResponse)_msg;

        _clone.pending = pending;
        _clone.futId = futId;
        _clone.miniId = miniId;
        _clone.dhtVer = dhtVer;
        _clone.invalidParts = invalidParts;
        _clone.ownedVals = ownedVals;
        _clone.ownedValsBytes = ownedValsBytes;
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
            case 9:
                if (!commState.putCacheVersion(dhtVer))
                    return false;

                commState.idx++;

            case 10:
                if (!commState.putGridUuid(futId))
                    return false;

                commState.idx++;

            case 11:
                if (invalidParts != null) {
                    if (commState.it == null) {
                        if (!commState.putInt(invalidParts.size()))
                            return false;

                        commState.it = invalidParts.iterator();
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

            case 12:
                if (!commState.putGridUuid(miniId))
                    return false;

                commState.idx++;

            case 13:
                if (ownedValsBytes != null) {
                    if (commState.it == null) {
                        if (!commState.putInt(ownedValsBytes.size()))
                            return false;

                        commState.it = ownedValsBytes.iterator();
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

            case 14:
                if (pending != null) {
                    if (commState.it == null) {
                        if (!commState.putInt(pending.size()))
                            return false;

                        commState.it = pending.iterator();
                    }

                    while (commState.it.hasNext() || commState.cur != NULL) {
                        if (commState.cur == NULL)
                            commState.cur = commState.it.next();

                        if (!commState.putCacheVersion((GridCacheVersion)commState.cur))
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
            case 9:
                GridCacheVersion dhtVer0 = commState.getCacheVersion();

                if (dhtVer0 == CACHE_VER_NOT_READ)
                    return false;

                dhtVer = dhtVer0;

                commState.idx++;

            case 10:
                GridUuid futId0 = commState.getGridUuid();

                if (futId0 == GRID_UUID_NOT_READ)
                    return false;

                futId = futId0;

                commState.idx++;

            case 11:
                if (commState.readSize == -1) {
                    if (buf.remaining() < 4)
                        return false;

                    commState.readSize = commState.getInt();
                }

                if (commState.readSize >= 0) {
                    if (invalidParts == null)
                        invalidParts = new ArrayList<>(commState.readSize);

                    for (int i = commState.readItems; i < commState.readSize; i++) {
                        if (buf.remaining() < 4)
                            return false;

                        int _val = commState.getInt();

                        invalidParts.add((Integer)_val);

                        commState.readItems++;
                    }
                }

                commState.readSize = -1;
                commState.readItems = 0;

                commState.idx++;

            case 12:
                GridUuid miniId0 = commState.getGridUuid();

                if (miniId0 == GRID_UUID_NOT_READ)
                    return false;

                miniId = miniId0;

                commState.idx++;

            case 13:
                if (commState.readSize == -1) {
                    if (buf.remaining() < 4)
                        return false;

                    commState.readSize = commState.getInt();
                }

                if (commState.readSize >= 0) {
                    if (ownedValsBytes == null)
                        ownedValsBytes = new ArrayList<>(commState.readSize);

                    for (int i = commState.readItems; i < commState.readSize; i++) {
                        byte[] _val = commState.getByteArray();

                        if (_val == BYTE_ARR_NOT_READ)
                            return false;

                        ownedValsBytes.add((byte[])_val);

                        commState.readItems++;
                    }
                }

                commState.readSize = -1;
                commState.readItems = 0;

                commState.idx++;

            case 14:
                if (commState.readSize == -1) {
                    if (buf.remaining() < 4)
                        return false;

                    commState.readSize = commState.getInt();
                }

                if (commState.readSize >= 0) {
                    if (pending == null)
                        pending = new ArrayList<>(commState.readSize);

                    for (int i = commState.readItems; i < commState.readSize; i++) {
                        GridCacheVersion _val = commState.getCacheVersion();

                        if (_val == CACHE_VER_NOT_READ)
                            return false;

                        pending.add((GridCacheVersion)_val);

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
        return 55;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridNearTxPrepareResponse.class, this, "super", super.toString());
    }
}
