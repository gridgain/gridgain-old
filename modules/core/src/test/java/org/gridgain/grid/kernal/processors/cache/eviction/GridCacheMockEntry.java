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

package org.gridgain.grid.kernal.processors.cache.eviction;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.grid.util.future.*;
import org.gridgain.grid.util.lang.*;
import org.gridgain.grid.util.tostring.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * Mock cache entry.
 */
public class GridCacheMockEntry<K, V> extends GridMetadataAwareAdapter implements GridCacheEntry<K, V> {
    /** */
    @GridToStringInclude
    private K key;

    /** */
    @GridToStringInclude
    private boolean evicted;

    /** */
    @GridToStringInclude
    private boolean canEvict = true;

    /**
     * Constructor.
     *
     * @param key Key.
     */
    public GridCacheMockEntry(K key) {
        this.key = key;
    }

    /**
     * Constructor.
     *
     * @param key Key.
     * @param canEvict Evict or not.
     */
    public GridCacheMockEntry(K key, boolean canEvict) {
        this.key = key;
        this.canEvict = canEvict;
    }

    /** {@inheritDoc} */
    @Override public K getKey() throws IllegalStateException {
        return key;
    }

    /** {@inheritDoc} */
    @Override public V getValue() throws IllegalStateException {
        return null;
    }

    /** {@inheritDoc} */
    @Override public V setValue(V val) {
        return null;
    }

    /** {@inheritDoc} */
    @Override public boolean evict() {
        evicted = true;

        onEvicted();

        return canEvict;
    }

    /**
     * Eviction callback.
     */
    public void onEvicted() {
        for (String key : allMeta().keySet())
            removeMeta(key);
    }

    /**
     *
     * @return Evicted or not.
     */
    public boolean isEvicted() {
        return evicted;
    }

    /** {@inheritDoc} */
    @Override public V peek() {
        return null;
    }

    /** {@inheritDoc} */
    @Override public V peek(@Nullable Collection<GridCachePeekMode> modes) {
        return null;
    }

    /** {@inheritDoc} */
    @Override public boolean isLocked() {
        return false;
    }

    /** {@inheritDoc} */
    @Override public boolean isLockedByThread() {
        return false;
    }

    /** {@inheritDoc} */
    @Nullable @Override public Object version() {
        return null;
    }

    /** {@inheritDoc} */
    @Override public long expirationTime() {
        return 0;
    }

    /** {@inheritDoc} */
    @Override public long timeToLive() {
        return 0;
    }

    /** {@inheritDoc} */
    @Override public boolean primary() {
        // No-op.
        return false;
    }

    /** {@inheritDoc} */
    @Override public boolean backup() {
        // No-op.
        return false;
    }

    /** {@inheritDoc} */
    @Override public int partition() {
        return 0;
    }

    /** {@inheritDoc} */
    @Nullable @Override public V set(V val,
        @Nullable GridPredicate<GridCacheEntry<K, V>>... filter) throws GridException {
        // No-op.
        return null;
    }

    /** {@inheritDoc} */
    @Nullable @Override public GridFuture<V> setAsync(V val,
        @Nullable GridPredicate<GridCacheEntry<K, V>>... filter) {
        // No-op.
        return null;
    }

    /** {@inheritDoc} */
    @Nullable @Override public V setIfAbsent(V val) throws GridException {
        // No-op.
        return null;
    }

    /** {@inheritDoc} */
    @Nullable @Override public GridFuture<V> setIfAbsentAsync(V val) {
        // No-op.
        return null;
    }

    /** {@inheritDoc} */
    @Override public boolean setx(V val,
        @Nullable GridPredicate<GridCacheEntry<K, V>>... filter) throws GridException {
        // No-op.
        return false;
    }

    /** {@inheritDoc} */
    @Nullable @Override public GridFuture<Boolean> setxAsync(V val,
        @Nullable GridPredicate<GridCacheEntry<K, V>>... filter) {
        // No-op.
        return null;
    }

    /** {@inheritDoc} */
    @Override public boolean setxIfAbsent(@Nullable V val) throws GridException {
        // No-op.
        return false;
    }

    /** {@inheritDoc} */
    @Nullable @Override public GridFuture<Boolean> setxIfAbsentAsync(V val) {
        // No-op.
        return null;
    }

    /** {@inheritDoc} */
    @Override public void transform(GridClosure<V, V> transformer) throws GridException {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override public GridFuture<?> transformAsync(GridClosure<V, V> transformer) {
        // No-op.
        return null;
    }

    /** {@inheritDoc} */
    @Nullable @Override public V replace(V val) throws GridException {
        // No-op.
        return null;
    }

    /** {@inheritDoc} */
    @Nullable @Override public GridFuture<V> replaceAsync(V val) {
        // No-op.
        return null;
    }

    /** {@inheritDoc} */
    @Override public boolean replacex(V val) throws GridException {
        // No-op.
        return false;
    }

    /** {@inheritDoc} */
    @Nullable @Override public GridFuture<Boolean> replacexAsync(V val) {
        // No-op.
        return null;
    }

    /** {@inheritDoc} */
    @Override public boolean replace(V oldVal, V newVal) throws GridException {
        // No-op.
        return false;
    }

    /** {@inheritDoc} */
    @Nullable @Override public GridFuture<Boolean> replaceAsync(V oldVal, V newVal) {
        // No-op.
        return null;
    }

    /** {@inheritDoc} */
    @Nullable @Override public V remove(
        @Nullable GridPredicate<GridCacheEntry<K, V>>... filter) throws GridException {
        // No-op.
        return null;
    }

    /** {@inheritDoc} */
    @Nullable @Override public GridFuture<V> removeAsync(
        @Nullable GridPredicate<GridCacheEntry<K, V>>... filter) {
        // No-op.
        return null;
    }

    /** {@inheritDoc} */
    @Override public boolean removex(@Nullable GridPredicate<GridCacheEntry<K, V>>... filter) throws GridException {
        // No-op.
        return false;
    }

    /** {@inheritDoc} */
    @Nullable @Override public GridFuture<Boolean> removexAsync(
        @Nullable GridPredicate<GridCacheEntry<K, V>>... filter) {
        // No-op.
        return null;
    }

    /** {@inheritDoc} */
    @Override public boolean remove(V val) throws GridException {
        // No-op.
        return false;
    }

    /** {@inheritDoc} */
    @Nullable @Override public GridFuture<Boolean> removeAsync(V val) {
        // No-op.
        return null;
    }

    /** {@inheritDoc} */
    @Override public void timeToLive(long ttl) {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override public boolean lock(long timeout,
        @Nullable GridPredicate<GridCacheEntry<K, V>>... filter) throws GridException {
        return false;
    }

    /** {@inheritDoc} */
    @Override public GridFuture<Boolean> lockAsync(long timeout,
        @Nullable GridPredicate<GridCacheEntry<K, V>>... filter) {
        return new GridFinishedFuture<>(null, false);
    }

    /** {@inheritDoc} */
    @Override public void unlock(GridPredicate<GridCacheEntry<K, V>>... filter) throws GridException {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override public boolean isCached() {
        return !evicted;
    }

    /** {@inheritDoc} */
    @Override public int memorySize() {
        return 1024;
    }

    /** {@inheritDoc} */
    @Override public GridCacheProjection<K, V> projection() {
        return null;
    }

    /** {@inheritDoc} */
    @Nullable @Override public V reload() throws GridException {
        return null;
    }

    /** {@inheritDoc} */
    @Override public GridFuture<V> reloadAsync() {
        return new GridFinishedFuture<>();
    }

    /** {@inheritDoc} */
    @Nullable @Override public V get() throws GridException {
        return null;
    }

    /** {@inheritDoc} */
    @Override public GridFuture<V> getAsync() {
        return new GridFinishedFuture<>();
    }

    /** {@inheritDoc} */
    @Override public boolean clear() {
        return false;
    }

    /** {@inheritDoc} */
    @Override public boolean compact() throws GridException {
        return false;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridCacheMockEntry.class, this);
    }
}
