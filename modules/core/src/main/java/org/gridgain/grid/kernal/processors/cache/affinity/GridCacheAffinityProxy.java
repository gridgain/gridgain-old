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

package org.gridgain.grid.kernal.processors.cache.affinity;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.affinity.*;
import org.gridgain.grid.kernal.processors.cache.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * Affinity interface implementation.
 */
public class GridCacheAffinityProxy<K, V> implements GridCacheAffinity<K> {
    /** Cache gateway. */
    private GridCacheGateway<K, V> gate;

    /** Affinity delegate. */
    private GridCacheAffinity<K> delegate;

    /**
     * @param cctx Context.
     * @param delegate Delegate object.
     */
    public GridCacheAffinityProxy(GridCacheContext<K, V> cctx, GridCacheAffinity<K> delegate) {
        gate = cctx.gate();
        this.delegate = delegate;
    }

    /** {@inheritDoc} */
    @Override public int partitions() {
        GridCacheProjectionImpl<K, V> old = gate.enter(null);

        try {
            return delegate.partitions();
        }
        finally {
            gate.leave(old);
        }
    }

    /** {@inheritDoc} */
    @Override public int partition(K key) {
        GridCacheProjectionImpl<K, V> old = gate.enter(null);

        try {
            return delegate.partition(key);
        }
        finally {
            gate.leave(old);
        }
    }

    /** {@inheritDoc} */
    @Override public boolean isPrimary(GridNode n, K key) {
        GridCacheProjectionImpl<K, V> old = gate.enter(null);

        try {
            return delegate.isPrimary(n, key);
        }
        finally {
            gate.leave(old);
        }
    }

    /** {@inheritDoc} */
    @Override public boolean isBackup(GridNode n, K key) {
        GridCacheProjectionImpl<K, V> old = gate.enter(null);

        try {
            return delegate.isBackup(n, key);
        }
        finally {
            gate.leave(old);
        }
    }

    /** {@inheritDoc} */
    @Override public boolean isPrimaryOrBackup(GridNode n, K key) {
        GridCacheProjectionImpl<K, V> old = gate.enter(null);

        try {
            return delegate.isPrimaryOrBackup(n, key);
        }
        finally {
            gate.leave(old);
        }
    }

    /** {@inheritDoc} */
    @Override public int[] primaryPartitions(GridNode n) {
        GridCacheProjectionImpl<K, V> old = gate.enter(null);

        try {
            return delegate.primaryPartitions(n);
        }
        finally {
            gate.leave(old);
        }
    }

    /** {@inheritDoc} */
    @Override public int[] backupPartitions(GridNode n) {
        GridCacheProjectionImpl<K, V> old = gate.enter(null);

        try {
            return delegate.backupPartitions(n);
        }
        finally {
            gate.leave(old);
        }
    }

    /** {@inheritDoc} */
    @Override public int[] allPartitions(GridNode n) {
        GridCacheProjectionImpl<K, V> old = gate.enter(null);

        try {
            return delegate.allPartitions(n);
        }
        finally {
            gate.leave(old);
        }
    }

    /** {@inheritDoc} */
    @Override public GridNode mapPartitionToNode(int part) {
        GridCacheProjectionImpl<K, V> old = gate.enter(null);

        try {
            return delegate.mapPartitionToNode(part);
        }
        finally {
            gate.leave(old);
        }
    }

    /** {@inheritDoc} */
    @Override public Map<Integer, GridNode> mapPartitionsToNodes(Collection<Integer> parts) {
        GridCacheProjectionImpl<K, V> old = gate.enter(null);

        try {
            return delegate.mapPartitionsToNodes(parts);
        }
        finally {
            gate.leave(old);
        }
    }

    /** {@inheritDoc} */
    @Override public Object affinityKey(K key) {
        GridCacheProjectionImpl<K, V> old = gate.enter(null);

        try {
            return delegate.affinityKey(key);
        }
        finally {
            gate.leave(old);
        }
    }


    /** {@inheritDoc} */
    @Override @Nullable public GridNode mapKeyToNode(K key) {
        GridCacheProjectionImpl<K, V> old = gate.enter(null);

        try {
            return delegate.mapKeyToNode(key);
        }
        finally {
            gate.leave(old);
        }
    }

    /** {@inheritDoc} */
    @Override public Map<GridNode, Collection<K>> mapKeysToNodes(@Nullable Collection<? extends K> keys) {
        GridCacheProjectionImpl<K, V> old = gate.enter(null);

        try {
            return delegate.mapKeysToNodes(keys);
        }
        finally {
            gate.leave(old);
        }
    }

    /** {@inheritDoc} */
    @Override public Collection<GridNode> mapKeyToPrimaryAndBackups(K key) {
        GridCacheProjectionImpl<K, V> old = gate.enter(null);

        try {
            return delegate.mapKeyToPrimaryAndBackups(key);
        }
        finally {
            gate.leave(old);
        }
    }

    /** {@inheritDoc} */
    @Override public Collection<GridNode> mapPartitionToPrimaryAndBackups(int part) {
        GridCacheProjectionImpl<K, V> old = gate.enter(null);

        try {
            return delegate.mapPartitionToPrimaryAndBackups(part);
        }
        finally {
            gate.leave(old);
        }
    }
}
