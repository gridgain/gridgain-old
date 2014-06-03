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
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * Affinity interface implementation.
 */
public class GridCacheAffinityImpl<K, V> implements GridCacheAffinity<K> {
    /** Cache context. */
    private GridCacheContext<K, V> cctx;

    /**
     * @param cctx Context.
     */
    public GridCacheAffinityImpl(GridCacheContext<K, V> cctx) {
        this.cctx = cctx;
    }

    /** {@inheritDoc} */
    @Override public int partitions() {
        return cctx.config().getAffinity().partitions();
    }

    /** {@inheritDoc} */
    @Override public int partition(K key) {
        A.notNull(key, "key");

        return cctx.affinity().partition(key);
    }

    /** {@inheritDoc} */
    @Override public boolean isPrimary(GridNode n, K key) {
        A.notNull(n, "n", key, "key");

        return cctx.affinity().primary(n, key, topologyVersion());
    }

    /** {@inheritDoc} */
    @Override public boolean isBackup(GridNode n, K key) {
        A.notNull(n, "n", key, "key");

        return cctx.affinity().backups(key, topologyVersion()).contains(n);
    }

    /** {@inheritDoc} */
    @Override public boolean isPrimaryOrBackup(GridNode n, K key) {
        A.notNull(n, "n", key, "key");

        return cctx.affinity().nodes(key, topologyVersion()).contains(n);
    }

    /** {@inheritDoc} */
    @Override public int[] primaryPartitions(GridNode n) {
        A.notNull(n, "n");

        Collection<Integer> parts = new HashSet<>();

        long topVer = cctx.discovery().topologyVersion();

        for (int partsCnt = partitions(), part = 0; part < partsCnt; part++) {
            if (n.id().equals(F.first(cctx.affinity().nodes(part, topVer)).id()))
                parts.add(part);
        }

        return U.toIntArray(parts);
    }

    /** {@inheritDoc} */
    @Override public int[] backupPartitions(GridNode n) {
        A.notNull(n, "n");

        Collection<Integer> parts = new HashSet<>();

        long topVer = cctx.discovery().topologyVersion();

        for (int partsCnt = partitions(), part = 0; part < partsCnt; part++) {
            Collection<GridNode> backups = cctx.affinity().backups(part, topVer);

            for (GridNode backup : backups) {
                if (n.id().equals(backup.id())) {
                    parts.add(part);

                    break;
                }
            }
        }

        return U.toIntArray(parts);
    }

    /** {@inheritDoc} */
    @Override public int[] allPartitions(GridNode n) {
        A.notNull(n, "p");

        Collection<Integer> parts = new HashSet<>();

        long topVer = cctx.discovery().topologyVersion();

        for (int partsCnt = partitions(), part = 0; part < partsCnt; part++) {
            for (GridNode affNode : cctx.affinity().nodes(part, topVer)) {
                if (n.id().equals(affNode.id())) {
                    parts.add(part);

                    break;
                }
            }
        }

        return U.toIntArray(parts);
    }

    /** {@inheritDoc} */
    @Override public GridNode mapPartitionToNode(int part) {
        A.ensure(part >= 0 && part < partitions(), "part >= 0 && part < total partitions");

        return F.first(cctx.affinity().nodes(part, topologyVersion()));
    }

    /** {@inheritDoc} */
    @Override public Map<Integer, GridNode> mapPartitionsToNodes(Collection<Integer> parts) {
        A.notNull(parts, "parts");

        Map<Integer, GridNode> map = new HashMap<>();

        if (!F.isEmpty(parts)) {
            for (int p : parts)
                map.put(p, mapPartitionToNode(p));
        }

        return map;
    }

    /** {@inheritDoc} */
    @Override public Object affinityKey(K key) {
        A.notNull(key, "key");

        return cctx.config().getAffinityMapper().affinityKey(key);
    }

    /** {@inheritDoc} */
    @Override @Nullable public GridNode mapKeyToNode(K key) {
        A.notNull(key, "key");

        return F.first(mapKeysToNodes(F.asList(key)).keySet());
    }

    /** {@inheritDoc} */
    @Override public Map<GridNode, Collection<K>> mapKeysToNodes(@Nullable Collection<? extends K> keys) {
        A.notNull(keys, "keys");

        long topVer = topologyVersion();

        int nodesCnt = cctx.discovery().cacheAffinityNodes(cctx.name(), topVer).size();

        // Must return empty map if no alive nodes present or keys is empty.
        Map<GridNode, Collection<K>> res = new HashMap<>(nodesCnt, 1.0f);

        for (K key : keys) {
            GridNode primary = cctx.affinity().primary(key, topVer);

            if (primary != null) {
                Collection<K> mapped = res.get(primary);

                if (mapped == null) {
                    mapped = new ArrayList<>(Math.max(keys.size() / nodesCnt, 16));

                    res.put(primary, mapped);
                }

                mapped.add(key);
            }
        }

        return res;
    }

    /** {@inheritDoc} */
    @Override public Collection<GridNode> mapKeyToPrimaryAndBackups(K key) {
        A.notNull(key, "key");

        return cctx.affinity().nodes(partition(key), topologyVersion());
    }

    /** {@inheritDoc} */
    @Override public Collection<GridNode> mapPartitionToPrimaryAndBackups(int part) {
        A.ensure(part >= 0 && part < partitions(), "part >= 0 && part < total partitions");

        return cctx.affinity().nodes(part, topologyVersion());
    }

    /**
     * Gets current topology version.
     *
     * @return Topology version.
     */
    private long topologyVersion() {
        return cctx.affinity().affinityTopologyVersion();
    }
}
