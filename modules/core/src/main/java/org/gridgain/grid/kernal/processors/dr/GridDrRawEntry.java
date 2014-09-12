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

package org.gridgain.grid.kernal.processors.dr;

import org.gridgain.grid.*;
import org.gridgain.grid.dr.*;
import org.gridgain.grid.kernal.processors.cache.*;
import org.gridgain.grid.marshaller.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.util.*;

/**
 * Data center entry implementation containing plain and raw keys and values.
 */
public class GridDrRawEntry<K, V> implements GridDrEntry<K, V>, Map.Entry<K, V>, Externalizable {
    /** */
    private static final long serialVersionUID = 0L;

    /** Key. */
    private K key;

    /** Key bytes. */
    private byte[] keyBytes;

    /** Value. */
    private V val;

    /** Value bytes. */
    private byte[] valBytes;

    /** TTL. */
    private long ttl;

    /** Expire time. */
    private long expireTime;

    /** Version. */
    private GridCacheVersion ver;

    /**
     * {@code Externalizable) support.
     */
    public GridDrRawEntry() {
        // No-op.
    }

    /**
     * Constructor.
     *
     * @param key Key.
     * @param keyBytes Key bytes.
     * @param val Value.
     * @param valBytes Value bytes.
     * @param expireTime Expire time.
     * @param ttl TTL.
     * @param ver Version.
     */
    public GridDrRawEntry(K key, @Nullable byte[] keyBytes, @Nullable V val, @Nullable byte[] valBytes,
        long ttl, long expireTime, GridCacheVersion ver) {
        this.key = key;
        this.keyBytes = keyBytes;
        this.val = val;
        this.valBytes = valBytes;
        this.ttl = ttl;
        this.expireTime = expireTime;
        this.ver = ver;
    }

    /** {@inheritDoc} */
    @Override public K key() {
        assert key != null : "Entry is being improperly processed.";

        return key;
    }

    /**
     * @return Key bytes.
     */
    public byte[] keyBytes() {
        return keyBytes;
    }

    /** {@inheritDoc} */
    @Override public V value() {
        return val;
    }

    /**
     * @return Value bytes.
     */
    public byte[] valueBytes() {
        return valBytes;
    }

    /** {@inheritDoc} */
    @Override public long ttl() {
        return ttl;
    }

    /** {@inheritDoc} */
    @Override public long expireTime() {
        return expireTime;
    }

    /** {@inheritDoc} */
    @Override public byte dataCenterId() {
        return ver.dataCenterId();
    }

    /** {@inheritDoc} */
    @Override public int topologyVersion() {
        return ver.topologyVersion();
    }

    /** {@inheritDoc} */
    @Override public long order() {
        return ver.order();
    }

    /** {@inheritDoc} */
    @Override public long globalTime() {
        return ver.globalTime();
    }

    /**
     * @return Version.
     */
    public GridCacheVersion version() {
        return ver;
    }

    /**
     * Note that this method can be called only after entry is marshalled.
     *
     * @return Approximate size of this entry serialized.
     */
    public int size() {
        assert keyBytes != null : "Entry is being improperly processed.";

        return 4 + 4 + keyBytes.length + (valBytes != null ? valBytes.length : 0);
    }

    /**
     * Perform internal unmarshal of this entry. It must be performed after entry is deserialized and before
     * its restored key/value are needed.
     *
     * @param marsh Marshaller.
     * @throws GridException If failed.
     */
    public void unmarshal(GridMarshaller marsh) throws GridException {
        unmarshalKey(marsh);

        if (valBytes != null && val == null)
            val = marsh.unmarshal(valBytes, null);
    }

    /**
     * Perform internal key unmarshal of this entry. It must be performed after entry is deserialized and before
     * its restored key/value are needed.
     *
     * @param marsh Marshaller.
     * @throws GridException If failed.
     */
    public void unmarshalKey(GridMarshaller marsh) throws GridException {
        if (key == null)
            key = marsh.unmarshal(keyBytes, null);
    }

    /**
     * Perform internal marshal of this entry before it will be serialized.
     *
     * @param marsh Marshaller.
     * @throws GridException If failed.
     */
    public void marshal(GridMarshaller marsh) throws GridException {
        if (keyBytes == null)
            keyBytes = marsh.marshal(key);

        if (valBytes == null && val != null)
            valBytes = marsh.marshal(val);
    }

    /** {@inheritDoc} */
    @Override public void writeExternal(ObjectOutput out) throws IOException {
        assert keyBytes != null;

        U.writeByteArray(out, keyBytes);
        U.writeByteArray(out, valBytes);

        out.writeLong(ttl);

        if (ttl != 0)
            out.writeLong(expireTime);

        out.writeObject(ver);
    }

    /** {@inheritDoc} */
    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        keyBytes = U.readByteArray(in);
        valBytes = U.readByteArray(in);

        ttl = in.readLong();

        if (ttl != 0)
            expireTime = in.readLong();

        ver = (GridCacheVersion)in.readObject();

        assert keyBytes != null;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridDrRawEntry.class, this, "keyBytesLen", keyBytes != null ? keyBytes.length : "n/a",
            "valBytesLen", valBytes != null ? valBytes.length : "n/a");
    }

    /** {@inheritDoc} */
    @Override public K getKey() {
        return key();
    }

    /** {@inheritDoc} */
    @Override public V getValue() {
        return value();
    }

    /** {@inheritDoc} */
    @Override public V setValue(V val) {
        throw new UnsupportedOperationException();
    }
}
