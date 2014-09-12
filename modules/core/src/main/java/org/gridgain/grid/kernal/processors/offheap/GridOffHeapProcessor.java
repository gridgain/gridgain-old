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

package org.gridgain.grid.kernal.processors.offheap;

import org.gridgain.grid.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.kernal.processors.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.marshaller.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.lang.*;
import org.gridgain.grid.util.offheap.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.jdk8.backport.*;
import org.jetbrains.annotations.*;

/**
 * Manages offheap memory caches.
 */
public class GridOffHeapProcessor extends GridProcessorAdapter {
    /** */
    private final ConcurrentHashMap8<String, GridOffHeapPartitionedMap> offheap =
        new ConcurrentHashMap8<>();

    /** */
    private final GridMarshaller marsh;

    /**
     * @param ctx Kernal context.
     */
    public GridOffHeapProcessor(GridKernalContext ctx) {
        super(ctx);

        marsh = ctx.config().getMarshaller();
    }

    /**
     * Creates offheap map for given space name. Previous one will be destructed if it exists.
     *
     * @param spaceName Space name.
     * @param parts Partitions number.
     * @param init Initial size.
     * @param max Maximum size.
     * @param lsnr Eviction listener.
     */
    public void create(@Nullable String spaceName, int parts, long init, long max,
        @Nullable GridOffHeapEvictListener lsnr) {
        spaceName = maskNull(spaceName);

        GridOffHeapPartitionedMap m = GridOffHeapMapFactory.unsafePartitionedMap(parts, 1024, 0.75f, init, max,
            (short)512, lsnr);

        GridOffHeapPartitionedMap old = offheap.put(spaceName, m);

        if (old != null)
            old.destruct();
    }

    /** {@inheritDoc} */
    @Override public void stop(boolean cancel) throws GridException {
        super.stop(cancel);

        for (GridOffHeapPartitionedMap m : offheap.values())
            m.destruct();
    }

    /**
     * Gets offheap swap space for given space name.
     *
     * @param spaceName Space name.
     * @return Offheap swap space.
     */
    @SuppressWarnings("unchecked")
    @Nullable private GridOffHeapPartitionedMap offheap(@Nullable String spaceName) {
        return offheap.get(maskNull(spaceName));
    }

    /**
     * Ensures that we have {@code keyBytes}.
     *
     * @param key Key.
     * @param keyBytes Optional key bytes.
     * @return Key bytes
     * @throws GridException If failed.
     */
    private byte[] keyBytes(Object key, @Nullable byte[] keyBytes) throws GridException {
        assert key != null;

        return keyBytes != null ? keyBytes : marsh.marshal(key);
    }

    /**
     * Masks {@code null} space name.
     *
     * @param spaceName Space name.
     * @return Masked space name.
     */
    private String maskNull(@Nullable String spaceName) {
        if (spaceName == null)
            return "gg-dflt-offheap-swap";

        return spaceName;
    }

    /**
     * Checks if offheap space contains value for the given key.
     *
     * @param spaceName Space name.
     * @param part Partition.
     * @param key Key.
     * @param keyBytes Key bytes.
     * @return {@code true} If offheap space contains value for the given key.
     * @throws GridException If failed.
     */
    public boolean contains(@Nullable String spaceName, int part, Object key, byte[] keyBytes) throws GridException {
        GridOffHeapPartitionedMap m = offheap(spaceName);

        return m != null && m.contains(part, U.hash(key), keyBytes(key, keyBytes));
    }

    /**
     * Gets value bytes from offheap space for the given key.
     *
     * @param spaceName Space name.
     * @param part Partition.
     * @param key Key.
     * @param keyBytes Key bytes.
     * @return Value bytes.
     * @throws GridException If failed.
     */
    @Nullable public byte[] get(@Nullable String spaceName, int part, Object key, byte[] keyBytes) throws GridException {
        GridOffHeapPartitionedMap m = offheap(spaceName);

        return m == null ? null : m.get(part, U.hash(key), keyBytes(key, keyBytes));
    }

    /**
     * Gets value from offheap space for the given key.
     *
     * @param spaceName Space name.
     * @param part Partition.
     * @param key Key.
     * @param keyBytes Key bytes.
     * @param ldr Class loader.
     * @return Value bytes.
     * @throws GridException If failed.
     */
    @Nullable public <T> T getValue(@Nullable String spaceName, int part, Object key, byte[] keyBytes,
        @Nullable ClassLoader ldr) throws GridException {
        byte[] valBytes = get(spaceName, part, key, keyBytes);

        if (valBytes == null)
            return null;

        return marsh.unmarshal(valBytes, ldr == null ? U.gridClassLoader() : ldr);
    }

    /**
     * Removes value from offheap space for the given key.
     *
     * @param spaceName Space name.
     * @param part Partition.
     * @param key Key.
     * @param keyBytes Key bytes.
     * @return Value bytes.
     * @throws GridException If failed.
     */
    @Nullable public byte[] remove(@Nullable String spaceName, int part, Object key, byte[] keyBytes) throws GridException {
        GridOffHeapPartitionedMap m = offheap(spaceName);

        return m == null ? null : m.remove(part, U.hash(key), keyBytes(key, keyBytes));
    }

    /**
     * Puts the given value to offheap space for the given key.
     *
     * @param spaceName Space name.
     * @param part Partition.
     * @param key Key.
     * @param keyBytes Key bytes.
     * @param valBytes Value bytes.
     * @throws GridException If failed.
     */
    public void put(@Nullable String spaceName, int part, Object key, byte[] keyBytes, byte[] valBytes)
        throws GridException {
        GridOffHeapPartitionedMap m = offheap(spaceName);

        if (m == null)
            throw new GridException("Failed to write data to off-heap space, no space registered for name: " +
                spaceName);

        m.put(part, U.hash(key), keyBytes(key, keyBytes), valBytes);
    }

    /**
     * Removes value from offheap space for the given key.
     *
     * @param spaceName Space name.
     * @param part Partition.
     * @param key Key.
     * @param keyBytes Key bytes.
     * @return {@code true} If succeeded.
     * @throws GridException If failed.
     */
    public boolean removex(@Nullable String spaceName, int part, Object key, byte[] keyBytes) throws GridException {
        GridOffHeapPartitionedMap m = offheap(spaceName);

        return m != null && m.removex(part, U.hash(key), keyBytes(key, keyBytes));
    }

    /**
     * Gets iterator over contents of the given space.
     *
     * @param spaceName Space name.
     * @return Iterator.
     */
    public GridCloseableIterator<GridBiTuple<byte[], byte[]>> iterator(@Nullable String spaceName) {
        GridOffHeapPartitionedMap m = offheap(spaceName);

        return m == null ? new GridEmptyCloseableIterator<GridBiTuple<byte[], byte[]>>() : m.iterator();
    }

    /**
     * Gets number of elements in the given space.
     *
     * @param spaceName Space name. Optional.
     * @return Number of elements or {@code -1} if no space with the given name has been found.
     */
    public long entriesCount(@Nullable String spaceName) {
        GridOffHeapPartitionedMap m = offheap(spaceName);

        return m == null ? -1 : m.size();
    }

    /**
     * Gets size of a memory allocated for the entries of the given space.
     *
     * @param spaceName Space name. Optional.
     * @return Allocated memory size or {@code -1} if no space with the given name has been found.
     */
    public long allocatedSize(@Nullable String spaceName) {
        GridOffHeapPartitionedMap m = offheap(spaceName);

        return m == null ? -1 : m.allocatedSize();
    }

    /**
     * Gets iterator over contents of partition.
     *
     * @param spaceName Space name.
     * @param part Partition.
     * @return Iterator.
     */
    public GridCloseableIterator<GridBiTuple<byte[], byte[]>> iterator(@Nullable String spaceName, int part) {
        GridOffHeapPartitionedMap m = offheap(spaceName);

        return m == null ? new GridEmptyCloseableIterator<GridBiTuple<byte[], byte[]>>() : m.iterator(part);
    }
}
