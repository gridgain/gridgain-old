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

package org.gridgain.grid.cache.affinity.rendezvous;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.cache.affinity.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.marshaller.*;
import org.gridgain.grid.marshaller.optimized.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.security.*;
import java.util.*;

/**
 * Affinity function for partitioned cache based on Highest Random Weight algorithm.
 * This function supports the following configuration:
 * <ul>
 * <li>
 *      {@code partitions} - Number of partitions to spread across nodes.
 * </li>
 * <li>
 *      {@code excludeNeighbors} - If set to {@code true}, will exclude same-host-neighbors
 *      from being backups of each other. Note that {@code backupFilter} is ignored if
 *      {@code excludeNeighbors} is set to {@code true}.
 * </li>
 * <li>
 *      {@code backupFilter} - Optional filter for back up nodes. If provided, then only
 *      nodes that pass this filter will be selected as backup nodes. If not provided, then
 *      primary and backup nodes will be selected out of all nodes available for this cache.
 * </li>
 * </ul>
 * <p>
 * Cache affinity can be configured for individual caches via {@link GridCacheConfiguration#getAffinity()} method.
 */
public class GridCacheRendezvousAffinityFunction implements GridCacheAffinityFunction, Externalizable {
    /** */
    private static final long serialVersionUID = 0L;

    /** Default number of partitions. */
    public static final int DFLT_PARTITION_COUNT = 10000;

    /** Comparator. */
    private static final Comparator<GridBiTuple<Long, GridNode>> COMPARATOR =
        new HashComparator();

    /** Thread local message digest. */
    private ThreadLocal<MessageDigest> digest = new ThreadLocal<MessageDigest>() {
        @Override protected MessageDigest initialValue() {
            try {
                return MessageDigest.getInstance("MD5");
            }
            catch (NoSuchAlgorithmException e) {
                assert false : "Should have failed in constructor";

                throw new GridRuntimeException("Failed to obtain message digest (digest was available in constructor)",
                    e);
            }
        }
    };

    /** Number of partitions. */
    private int parts;

    /** Exclude neighbors flag. */
    private boolean exclNeighbors;

    /** Optional backup filter. First node is primary, second node is a node being tested. */
    private GridBiPredicate<GridNode, GridNode> backupFilter;

    /** Hash ID resolver. */
    private GridCacheAffinityNodeHashResolver hashIdRslvr = new GridCacheAffinityNodeAddressHashResolver();

    /** Marshaller. */
    private GridMarshaller marshaller = new GridOptimizedMarshaller(false);

    /**
     * Empty constructor with all defaults.
     */
    public GridCacheRendezvousAffinityFunction() {
        this(false);
    }

    /**
     * Initializes affinity with flag to exclude same-host-neighbors from being backups of each other
     * and specified number of backups.
     * <p>
     * Note that {@code excludeNeighbors} parameter is ignored if {@code #getBackupFilter()} is set.
     *
     * @param exclNeighbors {@code True} if nodes residing on the same host may not act as backups
     *      of each other.
     */
    public GridCacheRendezvousAffinityFunction(boolean exclNeighbors) {
        this(exclNeighbors, DFLT_PARTITION_COUNT);
    }

    /**
     * Initializes affinity with flag to exclude same-host-neighbors from being backups of each other,
     * and specified number of backups and partitions.
     * <p>
     * Note that {@code excludeNeighbors} parameter is ignored if {@code #getBackupFilter()} is set.
     *
     * @param exclNeighbors {@code True} if nodes residing on the same host may not act as backups
     *      of each other.
     * @param parts Total number of partitions.
     */
    public GridCacheRendezvousAffinityFunction(boolean exclNeighbors, int parts) {
        this(exclNeighbors, parts, null);
    }

    /**
     * Initializes optional counts for replicas and backups.
     * <p>
     * Note that {@code excludeNeighbors} parameter is ignored if {@code backupFilter} is set.
     *
     * @param parts Total number of partitions.
     * @param backupFilter Optional back up filter for nodes. If provided, backups will be selected
     *      from all nodes that pass this filter. First argument for this filter is primary node, and second
     *      argument is node being tested.
     * <p>
     * Note that {@code excludeNeighbors} parameter is ignored if {@code backupFilter} is set.
     */
    public GridCacheRendezvousAffinityFunction(int parts,
        @Nullable GridBiPredicate<GridNode, GridNode> backupFilter) {
        this(false, parts, backupFilter);
    }

    /**
     * Private constructor.
     *
     * @param exclNeighbors Exclude neighbors flag.
     * @param parts Partitions count.
     * @param backupFilter Backup filter.
     */
    private GridCacheRendezvousAffinityFunction(boolean exclNeighbors, int parts,
        GridBiPredicate<GridNode, GridNode> backupFilter) {
        A.ensure(parts != 0, "parts != 0");

        this.exclNeighbors = exclNeighbors;
        this.parts = parts;
        this.backupFilter = backupFilter;

        try {
            MessageDigest.getInstance("MD5");
        }
        catch (NoSuchAlgorithmException e) {
            throw new GridRuntimeException("Failed to obtain MD5 message digest instance.", e);
        }
    }

    /**
     * Gets total number of key partitions. To ensure that all partitions are
     * equally distributed across all nodes, please make sure that this
     * number is significantly larger than a number of nodes. Also, partition
     * size should be relatively small. Try to avoid having partitions with more
     * than quarter million keys.
     * <p>
     * Note that for fully replicated caches this method should always
     * return {@code 1}.
     *
     * @return Total partition count.
     */
    public int getPartitions() {
        return parts;
    }

    /**
     * Sets total number of partitions.
     *
     * @param parts Total number of partitions.
     */
    public void setPartitions(int parts) {
        this.parts = parts;
    }

    /**
     * Gets hash ID resolver for nodes. This resolver is used to provide
     * alternate hash ID, other than node ID.
     * <p>
     * Node IDs constantly change when nodes get restarted, which causes them to
     * be placed on different locations in the hash ring, and hence causing
     * repartitioning. Providing an alternate hash ID, which survives node restarts,
     * puts node on the same location on the hash ring, hence minimizing required
     * repartitioning.
     *
     * @return Hash ID resolver.
     */
    public GridCacheAffinityNodeHashResolver getHashIdResolver() {
        return hashIdRslvr;
    }

    /**
     * Sets hash ID resolver for nodes. This resolver is used to provide
     * alternate hash ID, other than node ID.
     * <p>
     * Node IDs constantly change when nodes get restarted, which causes them to
     * be placed on different locations in the hash ring, and hence causing
     * repartitioning. Providing an alternate hash ID, which survives node restarts,
     * puts node on the same location on the hash ring, hence minimizing required
     * repartitioning.
     *
     * @param hashIdRslvr Hash ID resolver.
     */
    public void setHashIdResolver(GridCacheAffinityNodeHashResolver hashIdRslvr) {
        this.hashIdRslvr = hashIdRslvr;
    }

    /**
     * Gets optional backup filter. If not {@code null}, backups will be selected
     * from all nodes that pass this filter. First node passed to this filter is primary node,
     * and second node is a node being tested.
     * <p>
     * Note that {@code excludeNeighbors} parameter is ignored if {@code backupFilter} is set.
     *
     * @return Optional backup filter.
     */
    @Nullable public GridBiPredicate<GridNode, GridNode> getBackupFilter() {
        return backupFilter;
    }

    /**
     * Sets optional backup filter. If provided, then backups will be selected from all
     * nodes that pass this filter. First node being passed to this filter is primary node,
     * and second node is a node being tested.
     * <p>
     * Note that {@code excludeNeighbors} parameter is ignored if {@code backupFilter} is set.
     *
     * @param backupFilter Optional backup filter.
     */
    public void setBackupFilter(@Nullable GridBiPredicate<GridNode, GridNode> backupFilter) {
        this.backupFilter = backupFilter;
    }

    /**
     * Checks flag to exclude same-host-neighbors from being backups of each other (default is {@code false}).
     * <p>
     * Note that {@code excludeNeighbors} parameter is ignored if {@code #getBackupFilter()} is set.
     *
     * @return {@code True} if nodes residing on the same host may not act as backups of each other.
     */
    public boolean isExcludeNeighbors() {
        return exclNeighbors;
    }

    /**
     * Sets flag to exclude same-host-neighbors from being backups of each other (default is {@code false}).
     * <p>
     * Note that {@code excludeNeighbors} parameter is ignored if {@code #getBackupFilter()} is set.
     *
     * @param exclNeighbors {@code True} if nodes residing on the same host may not act as backups of each other.
     */
    public void setExcludeNeighbors(boolean exclNeighbors) {
        this.exclNeighbors = exclNeighbors;
    }

    /**
     * Returns collection of nodes (primary first) for specified partition.
     */
    public List<GridNode> assignPartition(int part, List<GridNode> nodes, int backups,
        @Nullable Map<UUID, Collection<GridNode>> neighborhoodCache) {
        if (nodes.size() <= 1)
            return nodes;

        List<GridBiTuple<Long, GridNode>> lst = new ArrayList<>();

        MessageDigest d = digest.get();

        for (GridNode node : nodes) {
            Object nodeHash = hashIdRslvr.resolve(node);

            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();

                byte[] nodeHashBytes = marshaller.marshal(nodeHash);

                out.write(nodeHashBytes, 0, nodeHashBytes.length); // Avoid IOException.
                out.write(U.intToBytes(part), 0, 4); // Avoid IOException.

                d.reset();

                byte[] bytes = d.digest(out.toByteArray());

                long hash =
                      (bytes[0] & 0xFFL)
                    | ((bytes[1] & 0xFFL) << 8)
                    | ((bytes[2] & 0xFFL) << 16)
                    | ((bytes[3] & 0xFFL) << 24)
                    | ((bytes[4] & 0xFFL) << 32)
                    | ((bytes[5] & 0xFFL) << 40)
                    | ((bytes[6] & 0xFFL) << 48)
                    | ((bytes[7] & 0xFFL) << 56);

                lst.add(F.t(hash, node));
            }
            catch (GridException e) {
                throw new GridRuntimeException(e);
            }
        }

        Collections.sort(lst, COMPARATOR);

        int primaryAndBackups;

        List<GridNode> res;

        if (backups == Integer.MAX_VALUE) {
            primaryAndBackups = Integer.MAX_VALUE;

            res = new ArrayList<>();
        }
        else {
            primaryAndBackups = backups + 1;

            res = new ArrayList<>(primaryAndBackups);
        }

        GridNode primary = lst.get(0).get2();

        res.add(primary);

        // Select backups.
        if (backups > 0) {
            for (int i = 1; i < lst.size(); i++) {
                GridBiTuple<Long, GridNode> next = lst.get(i);

                GridNode node = next.get2();

                if (exclNeighbors) {
                    Collection<GridNode> allNeighbors = allNeighbors(neighborhoodCache, res);

                    if (!allNeighbors.contains(node))
                        res.add(node);
                }
                else {
                    if (!res.contains(node) && (backupFilter == null || backupFilter.apply(primary, node)))
                        res.add(next.get2());
                }

                if (res.size() == primaryAndBackups)
                    break;
            }
        }

        if (res.size() < primaryAndBackups && nodes.size() >= primaryAndBackups && exclNeighbors) {
            // Need to iterate one more time in case if there are no nodes which pass exclude backups criteria.
            for (int i = 1; i < lst.size(); i++) {
                GridBiTuple<Long, GridNode> next = lst.get(i);

                GridNode node = next.get2();

                if (!res.contains(node))
                    res.add(next.get2());

                if (res.size() == primaryAndBackups)
                    break;
            }
        }

        assert res.size() <= primaryAndBackups;

        return res;
    }

    /** {@inheritDoc} */
    @Override public void reset() {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override public int partitions() {
        return parts;
    }

    /** {@inheritDoc} */
    @Override public int partition(Object key) {
        return U.safeAbs(key.hashCode() % parts);
    }

    /** {@inheritDoc} */
    @Override public List<List<GridNode>> assignPartitions(GridCacheAffinityFunctionContext affCtx) {
        List<List<GridNode>> assignments = new ArrayList<>(parts);

        Map<UUID, Collection<GridNode>> neighborhoodCache = exclNeighbors ?
            neighbors(affCtx.currentTopologySnapshot()) : null;

        for (int i = 0; i < parts; i++) {
            List<GridNode> partAssignment = assignPartition(i, affCtx.currentTopologySnapshot(), affCtx.backups(),
                neighborhoodCache);

            assignments.add(partAssignment);
        }

        return assignments;
    }

    /** {@inheritDoc} */
    @Override public void removeNode(UUID nodeId) {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(parts);
        out.writeBoolean(exclNeighbors);
        out.writeObject(hashIdRslvr);
        out.writeObject(backupFilter);
    }

    /** {@inheritDoc} */
    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        parts = in.readInt();
        exclNeighbors = in.readBoolean();
        hashIdRslvr = (GridCacheAffinityNodeHashResolver)in.readObject();
        backupFilter = (GridBiPredicate<GridNode, GridNode>)in.readObject();
    }

    /**
     * Builds neighborhood map for all nodes in snapshot.
     *
     * @param topSnapshot Topology snapshot.
     * @return Neighbors map.
     */
    private Map<UUID, Collection<GridNode>> neighbors(Collection<GridNode> topSnapshot) {
        Map<String, Collection<GridNode>> macMap = new HashMap<>(topSnapshot.size(), 1.0f);

        // Group by mac addresses.
        for (GridNode node : topSnapshot) {
            String macs = node.attribute(GridNodeAttributes.ATTR_MACS);

            Collection<GridNode> nodes = macMap.get(macs);

            if (nodes == null) {
                nodes = new HashSet<>();

                macMap.put(macs, nodes);
            }

            nodes.add(node);
        }

        Map<UUID, Collection<GridNode>> neighbors = new HashMap<>(topSnapshot.size(), 1.0f);

        for (Collection<GridNode> group : macMap.values()) {
            for (GridNode node : group)
                neighbors.put(node.id(), group);
        }

        return neighbors;
    }

    /**
     * @param neighborhoodCache Neighborhood cache.
     * @param nodes Nodes.
     * @return All neighbors for given nodes.
     */
    private Collection<GridNode> allNeighbors(Map<UUID, Collection<GridNode>> neighborhoodCache,
        Iterable<GridNode> nodes) {
        Collection<GridNode> res = new HashSet<>();

        for (GridNode node : nodes) {
            if (!res.contains(node))
                res.addAll(neighborhoodCache.get(node.id()));
        }

        return res;
    }

    /**
     *
     */
    private static class HashComparator implements Comparator<GridBiTuple<Long, GridNode>>, Serializable {
        /** */
        private static final long serialVersionUID = 0L;

        /** {@inheritDoc} */
        @Override public int compare(GridBiTuple<Long, GridNode> o1, GridBiTuple<Long, GridNode> o2) {
            return o1.get1() < o2.get1() ? -1 : o1.get1() > o2.get1() ? 1 :
                o1.get2().id().compareTo(o2.get2().id());
        }
    }
}
