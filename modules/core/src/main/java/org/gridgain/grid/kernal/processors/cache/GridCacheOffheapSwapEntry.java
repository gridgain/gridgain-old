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
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.jetbrains.annotations.*;
import sun.misc.*;

import static org.gridgain.grid.kernal.processors.cache.GridCacheSwapEntryImpl.*;

/**
 * GridCacheSwapEntry over offheap pointer.
 * <p>
 * Offheap pointer points to marshalled {@link GridCacheSwapEntryImpl} instance, marshalled data:
 * <ul>
 *     <li>TTL</li>
 *     <li>Expire time</li>
 *     <li>GridCacheVersion or GridCacheVersionEx</li>
 *     <li>Value is byte array flag</li>
 *     <li>Value byte array (marshalled with portable or grid marshaller)</li>
 *     <li>Value classloader UUID</li>
 *     <li>Key classloader UUID</li>
 * </ul>
 */
public class GridCacheOffheapSwapEntry<V> implements GridCacheSwapEntry<V> {
    /** */
    private static final Unsafe UNSAFE = GridUnsafe.unsafe();

    /** */
    private final long ptr;

    /** */
    private final long valPtr;

    /** */
    private final GridCacheVersion ver;

    /** */
    private V val;

    /** */
    private final boolean valIsByteArr;

    /**
     * @param ptr Value pointer.
     * @param size Value size.
     */
    public GridCacheOffheapSwapEntry(long ptr, int size) {
        assert ptr > 0 : ptr;
        assert size > 40 : size;

        this.ptr = ptr;

        long readPtr = ptr + VERSION_OFFSET;

        boolean verEx = UNSAFE.getByte(readPtr++) != 0;

        ver = U.readVersion(readPtr, verEx);

        readPtr += verEx ? VERSION_EX_SIZE : VERSION_SIZE;

        valIsByteArr = UNSAFE.getByte(readPtr) != 0;

        valPtr = readPtr;

        assert (ptr + size) > (UNSAFE.getInt(valPtr + 1) + valPtr + 5);
    }

    /**
     * @param ptr Marshaled swap entry address.
     * @param size Marshalled data size.
     * @return Value data address.
     */
    public static long valueAddress(long ptr, int size) {
        assert ptr > 0 : ptr;
        assert size > 40 : size;

        ptr += VERSION_OFFSET; // Skip ttl, expire time.

        boolean verEx = UNSAFE.getByte(ptr++) != 0;

        ptr += verEx ? VERSION_EX_SIZE : VERSION_SIZE;

        assert (ptr + size) > (UNSAFE.getInt(ptr + 1) + ptr + 5);

        return ptr;
    }

    /**
     * @param ptr Marshaled swap entry address.
     * @return TTL.
     */
    public static long timeToLive(long ptr) {
        return UNSAFE.getLong(ptr);
    }

    /**
     * @param ptr Marshaled swap entry address.
     * @return Expire time.
     */
    public static long expireTime(long ptr) {
        return UNSAFE.getLong(ptr + EXPIRE_TIME_OFFSET);
    }

    /**
     * @param ptr Marshaled swap entry address.
     * @return Version.
     */
    public static GridCacheVersion version(long ptr) {
        long addr = ptr + VERSION_OFFSET;

        boolean verEx = UNSAFE.getByte(addr) != 0;

        addr++;

        return U.readVersion(addr, verEx);
    }

    /** {@inheritDoc} */
    @Override public byte[] valueBytes() {
        return null;
    }

    /** {@inheritDoc} */
    @Override public void valueBytes(byte[] valBytes) {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override public V value() {
        return val;
    }

    /** {@inheritDoc} */
    @Override public void value(V val) {
        this.val = val;
    }

    /** {@inheritDoc} */
    @Override public boolean valueIsByteArray() {
        return valIsByteArr;
    }

    /** {@inheritDoc} */
    @Override public GridCacheVersion version() {
        return ver;
    }

    /** {@inheritDoc} */
    @Override public long ttl() {
        return UNSAFE.getLong(ptr);
    }

    /** {@inheritDoc} */
    @Override public long expireTime() {
        return UNSAFE.getLong(ptr + EXPIRE_TIME_OFFSET);
    }

    /** {@inheritDoc} */
    @Nullable @Override public GridUuid keyClassLoaderId() {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override public long offheapPointer() {
        return valPtr;
    }

    /** {@inheritDoc} */
    @Nullable @Override public GridUuid valueClassLoaderId() {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridCacheOffheapSwapEntry.class, this);
    }
}
