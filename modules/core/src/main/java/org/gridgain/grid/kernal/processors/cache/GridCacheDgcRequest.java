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

import org.gridgain.grid.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.util.direct.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.grid.util.tostring.*;

import java.nio.*;
import java.util.*;

/**
 * DGC request.
 */
public class GridCacheDgcRequest<K, V> extends GridCacheMessage<K, V> implements GridCacheDeployable {
    /** */
    private static final long serialVersionUID = 0L;

    /** */
    @GridToStringInclude
    @GridDirectTransient
    private Map<K, Collection<GridCacheDgcLockCandidate>> map = new HashMap<>();

    /** */
    @GridToStringExclude
    private byte[] mapBytes;

    /** */
    private boolean rmvLocks;

    /** {@inheritDoc} */
    @Override public void prepareMarshal(GridCacheContext<K, V> ctx) throws GridException {
        super.prepareMarshal(ctx);

        if (map != null) {
            if (ctx.deploymentEnabled()) {
                for (K key : map.keySet())
                    prepareObject(key, ctx);
            }

            mapBytes = CU.marshal(ctx, map);
        }
    }

    /** {@inheritDoc} */
    @Override public void finishUnmarshal(GridCacheContext<K, V> ctx, ClassLoader ldr) throws GridException {
        super.finishUnmarshal(ctx, ldr);

        if (mapBytes != null)
            map = ctx.marshaller().unmarshal(mapBytes, ldr);
    }

    /**
     * Add information about key and version to request.
     * <p>
     * Other version has to be provided if suspect lock is DHT local.
     *
     * @param key Key.
     * @param cand Candidate.
     */
    @SuppressWarnings({"MismatchedQueryAndUpdateOfCollection"})
    void addCandidate(K key, GridCacheDgcLockCandidate cand) {
        Collection<GridCacheDgcLockCandidate> col = F.addIfAbsent(map, key, new ArrayList<GridCacheDgcLockCandidate>());

        assert col != null;

        col.add(cand);
    }

    /**
     * @return Candidates map.
     */
    Map<K, Collection<GridCacheDgcLockCandidate>> candidatesMap() {
        return Collections.unmodifiableMap(map);
    }

    /**
     * @return Remove locks flag for this DGC iteration.
     */
    public boolean removeLocks() {
        return rmvLocks;
    }

    /**
     * @param rmvLocks Remove locks flag for this DGC iteration.
     */
    public void removeLocks(boolean rmvLocks) {
        this.rmvLocks = rmvLocks;
    }

    /** {@inheritDoc} */
    @SuppressWarnings({"CloneDoesntCallSuperClone", "CloneCallsConstructors"})
    @Override public GridTcpCommunicationMessageAdapter clone() {
        GridCacheDgcRequest _clone = new GridCacheDgcRequest();

        clone0(_clone);

        return _clone;
    }

    /** {@inheritDoc} */
    @Override protected void clone0(GridTcpCommunicationMessageAdapter _msg) {
        super.clone0(_msg);

        GridCacheDgcRequest _clone = (GridCacheDgcRequest)_msg;

        _clone.map = map;
        _clone.mapBytes = mapBytes;
        _clone.rmvLocks = rmvLocks;
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
                if (!commState.putByteArray(mapBytes))
                    return false;

                commState.idx++;

            case 3:
                if (!commState.putBoolean(rmvLocks))
                    return false;

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
                byte[] mapBytes0 = commState.getByteArray();

                if (mapBytes0 == BYTE_ARR_NOT_READ)
                    return false;

                mapBytes = mapBytes0;

                commState.idx++;

            case 3:
                if (buf.remaining() < 1)
                    return false;

                rmvLocks = commState.getBoolean();

                commState.idx++;

        }

        return true;
    }

    /** {@inheritDoc} */
    @Override public byte directType() {
        return 14;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridCacheDgcRequest.class, this);
    }
}
