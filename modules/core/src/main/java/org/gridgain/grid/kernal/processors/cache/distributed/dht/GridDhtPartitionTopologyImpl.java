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

package org.gridgain.grid.kernal.processors.cache.distributed.dht;

import org.gridgain.grid.*;
import org.gridgain.grid.kernal.processors.cache.*;
import org.gridgain.grid.kernal.processors.cache.distributed.dht.preloader.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.tostring.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.jdk8.backport.*;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

import static org.gridgain.grid.kernal.processors.cache.distributed.dht.GridDhtPartitionState.*;

/**
 * Partition topology.
 */
@GridToStringExclude
class GridDhtPartitionTopologyImpl<K, V> implements GridDhtPartitionTopology<K, V> {
    /** If true, then check consistency. */
    private static final boolean CONSISTENCY_CHECK = false;

    /** Flag to control amount of output for full map. */
    private static final boolean FULL_MAP_DEBUG = false;

    /** Context. */
    private final GridCacheContext<K, V> cctx;

    /** Logger. */
    private final GridLogger log;

    /** */
    private final ConcurrentMap<Integer, GridDhtLocalPartition<K, V>> locParts =
        new ConcurrentHashMap8<>();

    /** Node to partition map. */
    private GridDhtPartitionFullMap node2part;

    /** Partition to node map. */
    private Map<Integer, Set<UUID>> part2node = new HashMap<>();

    /** */
    private GridDhtPartitionExchangeId lastExchangeId;

    /** */
    private long topVer = -1;

    /** A future that will be completed when topology with version topVer will be ready to use. */
    private GridDhtTopologyFuture topReadyFut;

    /** */
    private final GridAtomicLong updateSeq = new GridAtomicLong(1);

    /** Lock. */
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * @param cctx Context.
     */
    GridDhtPartitionTopologyImpl(GridCacheContext<K, V> cctx) {
        this.cctx = cctx;

        log = cctx.logger(getClass());
    }

    /**
     * @return Full map string representation.
     */
    @SuppressWarnings( {"ConstantConditions"})
    private String fullMapString() {
        return node2part == null ? "null" : FULL_MAP_DEBUG ? node2part.toFullString() : node2part.toString();
    }

    /**
     * @param map Map to get string for.
     * @return Full map string representation.
     */
    @SuppressWarnings( {"ConstantConditions"})
    private String mapString(GridDhtPartitionMap map) {
        return map == null ? "null" : FULL_MAP_DEBUG ? map.toFullString() : map.toString();
    }

    /**
     * Waits for renting partitions.
     *
     * @return {@code True} if mapping was changed.
     * @throws GridException If failed.
     */
    private boolean waitForRent() throws GridException {
        boolean changed = false;

        // Synchronously wait for all renting partitions to complete.
        for (Iterator<GridDhtLocalPartition<K, V>> it = locParts.values().iterator(); it.hasNext();) {
            GridDhtLocalPartition<K, V> p = it.next();

            GridDhtPartitionState state = p.state();

            if (state == RENTING || state == EVICTED) {
                if (log.isDebugEnabled())
                    log.debug("Waiting for renting partition: " + p);

                // Wait for partition to empty out.
                p.rent(true).get();

                if (log.isDebugEnabled())
                    log.debug("Finished waiting for renting partition: " + p);

                // Remove evicted partition.
                it.remove();

                changed = true;
            }
        }

        return changed;
    }

    /** {@inheritDoc} */
    @SuppressWarnings( {"LockAcquiredButNotSafelyReleased"})
    @Override public void readLock() {
        lock.readLock().lock();
    }

    /** {@inheritDoc} */
    @Override public void readUnlock() {
        lock.readLock().unlock();
    }

    /** {@inheritDoc} */
    @Override public void updateTopologyVersion(GridDhtPartitionExchangeId exchId,
        GridDhtPartitionsExchangeFuture<K, V> exchFut) {
        lock.writeLock().lock();

        try {
            assert exchId.topologyVersion() > topVer : "Invalid topology version [topVer=" + topVer +
                ", exchId=" + exchId + ']';

            topVer = exchId.topologyVersion();

            topReadyFut = exchFut;
        }
        finally {
            lock.writeLock().unlock();
        }
    }

    /** {@inheritDoc} */
    @Override public long topologyVersion() {
        lock.readLock().lock();

        try {
            assert topVer > 0;

            return topVer;
        }
        finally {
            lock.readLock().unlock();
        }
    }

    /** {@inheritDoc} */
    @Override public GridDhtTopologyFuture topologyVersionFuture() {
        lock.readLock().lock();

        try {
            assert topReadyFut != null;

            return topReadyFut;
        }
        finally {
            lock.readLock().unlock();
        }
    }

    /** {@inheritDoc} */
    @Override public void beforeExchange(GridDhtPartitionExchangeId exchId) throws GridException {
        waitForRent();

        GridNode loc = cctx.localNode();

        int num = cctx.affinity().partitions();

        lock.writeLock().lock();

        try {
            assert topVer == exchId.topologyVersion() : "Invalid topology version [topVer=" +
                topVer + ", exchId=" + exchId + ']';

            if (!exchId.isJoined())
                removeNode(exchId.nodeId());

            // In case if node joins, get topology at the time of joining node.
            GridNode oldest = CU.oldest(cctx, topVer);

            if (log.isDebugEnabled())
                log.debug("Partition map beforeExchange [exchId=" + exchId + ", fullMap=" + fullMapString() + ']');

            long updateSeq = this.updateSeq.incrementAndGet();

            // If this is the oldest node.
            if (oldest.id().equals(loc.id())) {
                if (node2part == null) {
                    node2part = new GridDhtPartitionFullMap(loc.id(), loc.order(), updateSeq);

                    if (log.isDebugEnabled())
                        log.debug("Created brand new full topology map on oldest node [exchId=" +
                            exchId + ", fullMap=" + fullMapString() + ']');
                }
                else if (!node2part.valid()) {
                    node2part = new GridDhtPartitionFullMap(loc.id(), loc.order(), updateSeq, node2part, false);

                    if (log.isDebugEnabled())
                        log.debug("Created new full topology map on oldest node [exchId=" + exchId + ", fullMap=" +
                            node2part + ']');
                }
                else if (!node2part.nodeId().equals(loc.id())) {
                    node2part = new GridDhtPartitionFullMap(loc.id(), loc.order(), updateSeq, node2part, false);

                    if (log.isDebugEnabled())
                        log.debug("Copied old map into new map on oldest node (previous oldest node left) [exchId=" +
                            exchId + ", fullMap=" + fullMapString() + ']');
                }
            }

            if (cctx.preloadEnabled()) {
                for (int p = 0; p < num; p++) {
                    // If this is the first node in grid.
                    if (oldest.id().equals(loc.id()) && oldest.id().equals(exchId.nodeId())) {
                        assert exchId.isJoined();

                        try {
                            GridDhtLocalPartition<K, V> locPart = localPartition(p, topVer, true, false);

                            assert locPart != null;

                            boolean owned = locPart.own();

                            assert owned : "Failed to own partition for oldest node [cacheName" + cctx.name() +
                                ", part=" + locPart + ']';

                            if (log.isDebugEnabled())
                                log.debug("Owned partition for oldest node: " + locPart);

                            updateLocal(p, loc.id(), locPart.state(), updateSeq);
                        }
                        catch (GridDhtInvalidPartitionException e) {
                            if (log.isDebugEnabled())
                                log.debug("Ignoring invalid partition on oldest node (no need to create a partition " +
                                    "if it no longer belongs to local node: " + e.partition());
                        }
                    }
                    // If this is not the first node in grid.
                    else {
                        if (node2part != null && node2part.valid()) {
                            if (cctx.affinity().localNode(p, topVer)) {
                                try {
                                    // This will make sure that all non-existing partitions
                                    // will be created in MOVING state.
                                    GridDhtLocalPartition<K, V> locPart = localPartition(p, topVer, true, false);

                                    updateLocal(p, loc.id(), locPart.state(), updateSeq);
                                }
                                catch (GridDhtInvalidPartitionException e) {
                                    if (log.isDebugEnabled())
                                        log.debug("Ignoring invalid partition (no need to create a partition if it " +
                                            "no longer belongs to local node: " + e.partition());
                                }
                            }
                        }
                        // If this node's map is empty, we pre-create local partitions,
                        // so local map will be sent correctly during exchange.
                        else if (cctx.affinity().localNode(p, topVer)) {
                            try {
                                localPartition(p, topVer, true, false);
                            }
                            catch (GridDhtInvalidPartitionException e) {
                                if (log.isDebugEnabled())
                                    log.debug("Ignoring invalid partition (no need to pre-create a partition if it " +
                                        "no longer belongs to local node: " + e.partition());
                            }
                        }
                    }
                }
            }
            else {
                // If preloader is disabled, then we simply clear out
                // the partitions this node is not responsible for.
                for (int p = 0; p < num; p++) {
                    GridDhtLocalPartition<K, V> locPart = localPartition(p, topVer, false, false);

                    boolean belongs = cctx.affinity().localNode(p, topVer);

                    if (locPart != null) {
                        if (!belongs) {
                            GridDhtPartitionState state = locPart.state();

                            if (state.active()) {
                                locPart.rent(false);

                                updateLocal(p, loc.id(), locPart.state(), updateSeq);

                                if (log.isDebugEnabled())
                                    log.debug("Evicting partition with preloading disabled " +
                                        "(it does not belong to affinity): " + locPart);
                            }
                        }
                    }
                    else if (belongs) {
                        try {
                            // Pre-create partitions.
                            localPartition(p, topVer, true, false);
                        }
                        catch (GridDhtInvalidPartitionException e) {
                            if (log.isDebugEnabled())
                                log.debug("Ignoring invalid partition with disabled preloader (no need to " +
                                    "pre-create a partition if it no longer belongs to local node: " + e.partition());
                        }
                    }
                }
            }

            if (node2part != null && node2part.valid())
                checkEvictions(updateSeq);

            consistencyCheck();

            if (log.isDebugEnabled())
                log.debug("Partition map after beforeExchange [exchId=" + exchId + ", fullMap=" +
                    fullMapString() + ']');
        }
        finally {
            lock.writeLock().unlock();
        }

        // Wait for evictions.
        waitForRent();
    }

    /** {@inheritDoc} */
    @Override public boolean afterExchange(GridDhtPartitionExchangeId exchId) throws GridException {
        boolean changed = waitForRent();

        GridNode loc = cctx.localNode();

        int num = cctx.affinity().partitions();

        long topVer = exchId.topologyVersion();

        lock.writeLock().lock();

        try {
            assert topVer == exchId.topologyVersion() : "Invalid topology version [topVer=" +
                topVer + ", exchId=" + exchId + ']';

            if (log.isDebugEnabled())
                log.debug("Partition map before afterExchange [exchId=" + exchId + ", fullMap=" +
                    fullMapString() + ']');

            long updateSeq = this.updateSeq.incrementAndGet();

            for (int p = 0; p < num; p++) {
                GridDhtLocalPartition<K, V> locPart = localPartition(p, topVer, false, false);

                if (cctx.affinity().localNode(p, topVer)) {
                    // This partition will be created during next topology event,
                    // which obviously has not happened at this point.
                    if (locPart == null) {
                        if (log.isDebugEnabled())
                            log.debug("Skipping local partition afterExchange (will not create): " + p);

                        continue;
                    }

                    GridDhtPartitionState state = locPart.state();

                    if (state == MOVING) {
                        if (cctx.preloadEnabled()) {
                            Collection<GridNode> owners = owners(p);

                            // If there are no other owners, then become an owner.
                            if (F.isEmpty(owners)) {
                                boolean owned = locPart.own();

                                assert owned : "Failed to own partition [cacheName" + cctx.name() + ", locPart=" +
                                    locPart + ']';

                                updateLocal(p, loc.id(), locPart.state(), updateSeq);

                                changed = true;

                                if (log.isDebugEnabled())
                                    log.debug("Owned partition: " + locPart);
                            }
                            else if (log.isDebugEnabled())
                                log.debug("Will not own partition (there are owners to preload from) [locPart=" +
                                    locPart + ", owners = " + owners + ']');
                        }
                        else
                            updateLocal(p, loc.id(), locPart.state(), updateSeq);
                    }
                }
                else {
                    if (locPart != null) {
                        GridDhtPartitionState state = locPart.state();

                        if (state == MOVING) {
                            locPart.rent(false);

                            updateLocal(p, loc.id(), locPart.state(), updateSeq);

                            changed = true;

                            if (log.isDebugEnabled())
                                log.debug("Evicting moving partition (it does not belong to affinity): " + locPart);
                        }
                    }
                }
            }

            consistencyCheck();
        }
        finally {
            lock.writeLock().unlock();
        }

        return changed;
    }

    /** {@inheritDoc} */
    @Nullable @Override public GridDhtLocalPartition<K, V> localPartition(int p, long topVer, boolean create)
        throws GridDhtInvalidPartitionException {
        return localPartition(p, topVer, create, true);
    }

    /**
     * @param p Partition number.
     * @param topVer Topology version.
     * @param create Create flag.
     * @param updateSeq Update sequence.
     * @return Local partition.
     */
    private GridDhtLocalPartition<K, V> localPartition(int p, long topVer, boolean create, boolean updateSeq) {
        while (true) {
            boolean belongs = cctx.affinity().localNode(p, topVer);

            GridDhtLocalPartition<K, V> loc = locParts.get(p);

            if (loc != null && loc.state() == EVICTED) {
                locParts.remove(p, loc);

                if (!create)
                    return null;

                if (!belongs)
                    throw new GridDhtInvalidPartitionException(p, "Adding entry to evicted partition [part=" + p +
                        ", topVer=" + topVer + ", this.topVer=" + this.topVer + ']');

                continue;
            }

            if (loc == null && create) {
                if (!belongs)
                    throw new GridDhtInvalidPartitionException(p, "Creating partition which does not belong [part=" +
                        p + ", topVer=" + topVer + ", this.topVer=" + this.topVer + ']');

                lock.writeLock().lock();

                try {
                    GridDhtLocalPartition<K, V> old = locParts.putIfAbsent(p,
                        loc = new GridDhtLocalPartition<>(cctx, p));

                    if (old != null)
                        loc = old;
                    else {
                        if (updateSeq)
                            this.updateSeq.incrementAndGet();

                        if (log.isDebugEnabled())
                            log.debug("Created local partition: " + loc);
                    }
                }
                finally {
                    lock.writeLock().unlock();
                }
            }

            return loc;
        }
    }

    /** {@inheritDoc} */
    @Override public GridDhtLocalPartition<K, V> localPartition(K key, boolean create) {
        return localPartition(cctx.affinity().partition(key), -1, create);
    }

    /** {@inheritDoc} */
    @Override public List<GridDhtLocalPartition<K, V>> localPartitions() {
        return new LinkedList<>(locParts.values());
    }

    /** {@inheritDoc} */
    @Override public Collection<GridDhtLocalPartition<K, V>> currentLocalPartitions() {
        return locParts.values();
    }

    /** {@inheritDoc} */
    @Override public GridDhtLocalPartition<K, V> onAdded(long topVer, GridDhtCacheEntry<K, V> e) {
        /*
         * Make sure not to acquire any locks here as this method
         * may be called from sensitive synchronization blocks.
         * ===================================================
         */

        int p = cctx.affinity().partition(e.key());

        GridDhtLocalPartition<K, V> loc = localPartition(p, topVer, true);

        assert loc != null;

        loc.onAdded(e);

        return loc;
    }

    /** {@inheritDoc} */
    @Override public void onRemoved(GridDhtCacheEntry<K, V> e) {
        /*
         * Make sure not to acquire any locks here as this method
         * may be called from sensitive synchronization blocks.
         * ===================================================
         */

        GridDhtLocalPartition<K, V> loc = localPartition(cctx.affinity().partition(e.key()), topologyVersion(), false);

        if (loc != null)
            loc.onRemoved(e);
    }

    /** {@inheritDoc} */
    @Override public GridDhtPartitionMap localPartitionMap() {
        lock.readLock().lock();

        try {
            return new GridDhtPartitionMap(cctx.nodeId(), updateSeq.get(),
                F.viewReadOnly(locParts, CU.<K, V>part2state()), true);
        }
        finally {
            lock.readLock().unlock();
        }
    }

    /** {@inheritDoc} */
    @Override public Collection<GridNode> nodes(int p, long topVer) {
        Collection<GridNode> affNodes = cctx.affinity().nodes(p, topVer);

        lock.readLock().lock();

        try {
            assert node2part != null && node2part.valid() : "Invalid node-to-partitions map [topVer=" + topVer +
                ", node2part=" + node2part + ']';

            Collection<GridNode> nodes = null;

            Collection<UUID> nodeIds = part2node.get(p);

            if (!F.isEmpty(nodeIds)) {
                Collection<UUID> affIds = new HashSet<>(F.viewReadOnly(affNodes, F.node2id()));

                for (UUID nodeId : nodeIds) {
                    if (!affIds.contains(nodeId) && hasState(p, nodeId, OWNING, MOVING, RENTING)) {
                        GridNode n = cctx.discovery().node(nodeId);

                        if (n != null && (topVer < 0 || n.order() <= topVer)) {
                            if (nodes == null) {
                                nodes = new ArrayList<>(affNodes.size() + 2);

                                nodes.addAll(affNodes);
                            }

                            nodes.add(n);
                        }
                    }
                }
            }

            return nodes != null ? nodes : affNodes;
        }
        finally {
            lock.readLock().unlock();
        }
    }

    /**
     * @param p Partition.
     * @param topVer Topology version ({@code -1} for all nodes).
     * @param state Partition state.
     * @param states Additional partition states.
     * @return List of nodes for the partition.
     */
    private List<GridNode> nodes(int p, long topVer, GridDhtPartitionState state, GridDhtPartitionState... states) {
        Collection<UUID> allIds = topVer > 0 ? F.nodeIds(CU.allNodes(cctx, topVer)) : null;

        lock.readLock().lock();

        try {
            assert node2part != null && node2part.valid() : "Invalid node-to-partitions map [topVer=" + topVer +
                ", allIds=" + allIds + ", node2part=" + node2part + ']';

            Collection<UUID> nodeIds = part2node.get(p);

            // Node IDs can be null if both, primary and backup, nodes disappear.
            int size = nodeIds == null ? 0 : nodeIds.size();

            if (size == 0)
                return Collections.emptyList();

            List<GridNode> nodes = new ArrayList<>(size);

            for (UUID id : nodeIds) {
                if (topVer > 0 && !allIds.contains(id))
                    continue;

                if (hasState(p, id, state, states)) {
                    GridNode n = cctx.discovery().node(id);

                    if (n != null && (topVer < 0 || n.order() <= topVer))
                        nodes.add(n);
                }
            }

            return nodes;
        }
        finally {
            lock.readLock().unlock();
        }
    }

    /** {@inheritDoc} */
    @Override public List<GridNode> owners(int p, long topVer) {
        if (!cctx.preloadEnabled())
            return ownersAndMoving(p, topVer);

        return nodes(p, topVer, OWNING);
    }

    /** {@inheritDoc} */
    @Override public List<GridNode> owners(int p) {
        return owners(p, -1);
    }

    /** {@inheritDoc} */
    @Override public List<GridNode> moving(int p) {
        if (!cctx.preloadEnabled())
            return ownersAndMoving(p, -1);

        return nodes(p, -1, MOVING);
    }

    /**
     * @param p Partition.
     * @param topVer Topology version.
     * @return List of nodes in state OWNING or MOVING.
     */
    private List<GridNode> ownersAndMoving(int p, long topVer) {
        return nodes(p, topVer, OWNING, MOVING);
    }

    /** {@inheritDoc} */
    @Override public long updateSequence() {
        return updateSeq.get();
    }

    /** {@inheritDoc} */
    @Override public GridDhtPartitionFullMap partitionMap(boolean onlyActive) {
        lock.readLock().lock();

        try {
            assert node2part != null && node2part.valid() : "Invalid node2part [node2part: " + node2part +
                ", locNodeId=" + cctx.localNode().id() + ", locName=" + cctx.gridName() + ']';

            GridDhtPartitionFullMap m = node2part;

            return new GridDhtPartitionFullMap(m.nodeId(), m.nodeOrder(), m.updateSequence(), m, onlyActive);
        }
        finally {
            lock.readLock().unlock();
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings({"MismatchedQueryAndUpdateOfCollection"})
    @Nullable @Override public GridDhtPartitionMap update(@Nullable GridDhtPartitionExchangeId exchId,
        GridDhtPartitionFullMap partMap) {
        if (log.isDebugEnabled())
            log.debug("Updating full partition map [exchId=" + exchId + ", parts=" + fullMapString() + ']');

        lock.writeLock().lock();

        try {
            if (exchId != null && lastExchangeId != null && lastExchangeId.compareTo(exchId) >= 0) {
                if (log.isDebugEnabled())
                    log.debug("Stale exchange id for full partition map update (will ignore) [lastExchId=" +
                        lastExchangeId + ", exchId=" + exchId + ']');

                return null;
            }

            if (node2part != null && node2part.compareTo(partMap) >= 0) {
                if (log.isDebugEnabled())
                    log.debug("Stale partition map for full partition map update (will ignore) [lastExchId=" +
                        lastExchangeId + ", exchId=" + exchId + ", curMap=" + node2part + ", newMap=" + partMap + ']');

                return null;
            }

            long updateSeq = this.updateSeq.incrementAndGet();

            if (exchId != null)
                lastExchangeId = exchId;

            if (node2part != null) {
                for (GridDhtPartitionMap part : node2part.values()) {
                    GridDhtPartitionMap newPart = partMap.get(part.nodeId());

                    // If for some nodes current partition has a newer map,
                    // then we keep the newer value.
                    if (newPart != null && newPart.updateSequence() < part.updateSequence()) {
                        if (log.isDebugEnabled())
                            log.debug("Overriding partition map in full update map [exchId=" + exchId + ", curPart=" +
                                mapString(part) + ", newPart=" + mapString(newPart) + ']');

                        partMap.put(part.nodeId(), part);
                    }
                }

                for (Iterator<UUID> it = partMap.keySet().iterator(); it.hasNext();) {
                    UUID nodeId = it.next();

                    if (!cctx.discovery().alive(nodeId)) {
                        if (log.isDebugEnabled())
                            log.debug("Removing left node from full map update [nodeId=" + nodeId + ", partMap=" +
                                partMap + ']');

                        it.remove();
                    }
                }
            }

            node2part = partMap;

            Map<Integer, Set<UUID>> p2n = new HashMap<>(cctx.affinity().partitions(), 1.0f);

            for (Map.Entry<UUID, GridDhtPartitionMap> e : partMap.entrySet()) {
                for (Integer p : e.getValue().keySet()) {
                    Set<UUID> ids = p2n.get(p);

                    if (ids == null)
                        // Initialize HashSet to size 3 in anticipation that there won't be
                        // more than 3 nodes per partitions.
                        p2n.put(p, ids = new HashSet<>(3));

                    ids.add(e.getKey());
                }
            }

            part2node = p2n;

            boolean changed = checkEvictions(updateSeq);

            consistencyCheck();

            if (log.isDebugEnabled())
                log.debug("Partition map after full update: " + fullMapString());

            return changed ? localPartitionMap() : null;
        }
        finally {
            lock.writeLock().unlock();
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings({"MismatchedQueryAndUpdateOfCollection"})
    @Nullable @Override public GridDhtPartitionMap update(@Nullable GridDhtPartitionExchangeId exchId,
        GridDhtPartitionMap parts) {
        if (log.isDebugEnabled())
            log.debug("Updating single partition map [exchId=" + exchId + ", parts=" + mapString(parts) + ']');

        if (!cctx.discovery().alive(parts.nodeId())) {
            if (log.isDebugEnabled())
                log.debug("Received partition update for non-existing node (will ignore) [exchId=" + exchId +
                    ", parts=" + parts + ']');

            return null;
        }

        lock.writeLock().lock();

        try {
            if (lastExchangeId != null && exchId != null && lastExchangeId.compareTo(exchId) > 0) {
                if (log.isDebugEnabled())
                    log.debug("Stale exchange id for single partition map update (will ignore) [lastExchId=" +
                        lastExchangeId + ", exchId=" + exchId + ']');

                return null;
            }

            if (exchId != null)
                lastExchangeId = exchId;

            if (node2part == null)
                // Create invalid partition map.
                node2part = new GridDhtPartitionFullMap();

            GridDhtPartitionMap cur = node2part.get(parts.nodeId());

            if (cur != null && cur.updateSequence() >= parts.updateSequence()) {
                if (log.isDebugEnabled())
                    log.debug("Stale update sequence for single partition map update (will ignore) [exchId=" + exchId +
                        ", curSeq=" + cur.updateSequence() + ", newSeq=" + parts.updateSequence() + ']');

                return null;
            }

            long updateSeq = this.updateSeq.incrementAndGet();

            node2part = new GridDhtPartitionFullMap(node2part, updateSeq);

            boolean changed = false;

            if (cur == null || !cur.equals(parts))
                changed = true;

            node2part.put(parts.nodeId(), parts);

            part2node = new HashMap<>(part2node);

            // Add new mappings.
            for (Integer p : parts.keySet()) {
                Set<UUID> ids = part2node.get(p);

                if (ids == null)
                    // Initialize HashSet to size 3 in anticipation that there won't be
                    // more than 3 nodes per partition.
                    part2node.put(p, ids = new HashSet<>(3));

                changed |= ids.add(parts.nodeId());
            }

            // Remove obsolete mappings.
            if (cur != null) {
                for (Integer p : F.view(cur.keySet(), F0.notIn(parts.keySet()))) {
                    Set<UUID> ids = part2node.get(p);

                    if (ids != null)
                        changed |= ids.remove(parts.nodeId());
                }
            }

            changed |= checkEvictions(updateSeq);

            consistencyCheck();

            if (log.isDebugEnabled())
                log.debug("Partition map after single update: " + fullMapString());

            return changed ? localPartitionMap() : null;
        }
        finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * @param updateSeq Update sequence.
     * @return Checks if any of the local partitions need to be evicted.
     */
    private boolean checkEvictions(long updateSeq) {
        assert lock.isWriteLockedByCurrentThread();

        boolean changed = false;

        UUID locId = cctx.nodeId();

        for (GridDhtLocalPartition<K, V> part : locParts.values()) {
            GridDhtPartitionState state = part.state();

            if (state.active()) {
                int p = part.id();

                Collection<GridNode> affNodes = cctx.affinity().nodes(p, topVer);

                if (!affNodes.contains(cctx.localNode())) {
                    Collection<UUID> nodeIds = F.nodeIds(nodes(p, topVer, OWNING));

                    // If all affinity nodes are owners, then evict partition from local node.
                    if (nodeIds.containsAll(F.nodeIds(affNodes))) {
                        part.rent(false);

                        updateLocal(part.id(), locId, part.state(), updateSeq);

                        changed = true;

                        if (log.isDebugEnabled())
                            log.debug("Evicted local partition (all affinity nodes are owners): " + part);
                    }
                    else {
                        int ownerCnt = nodeIds.size();
                        int affCnt = affNodes.size();

                        if (ownerCnt > affCnt) {
                            List<GridNode> sorted = new ArrayList<>(cctx.discovery().nodes(nodeIds));

                            // Sort by node orders in ascending order.
                            Collections.sort(sorted, CU.nodeComparator(true));

                            int diff = sorted.size() - affCnt;

                            for (int i = 0; i < diff; i++) {
                                GridNode n = sorted.get(i);

                                if (locId.equals(n.id())) {
                                    part.rent(false);

                                    updateLocal(part.id(), locId, part.state(), updateSeq);

                                    changed = true;

                                    if (log.isDebugEnabled())
                                        log.debug("Evicted local partition (this node is oldest non-affinity node): " +
                                            part);

                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }

        return changed;
    }

    /**
     * Updates value for single partition.
     *
     * @param p Partition.
     * @param nodeId Node ID.
     * @param state State.
     * @param updateSeq Update sequence.
     */
    @SuppressWarnings({"MismatchedQueryAndUpdateOfCollection"})
    private void updateLocal(int p, UUID nodeId, GridDhtPartitionState state, long updateSeq) {
        assert lock.isWriteLockedByCurrentThread();
        assert nodeId.equals(cctx.nodeId());

        // In case if node joins, get topology at the time of joining node.
        GridNode oldest = CU.oldest(cctx, topVer);

        // If this node became the oldest node.
        if (oldest.id().equals(cctx.nodeId())) {
            long seq = node2part.updateSequence();

            if (seq != updateSeq) {
                if (seq > updateSeq) {
                    if (this.updateSeq.get() < seq) {
                        // Update global counter if necessary.
                        boolean b = this.updateSeq.compareAndSet(this.updateSeq.get(), seq + 1);

                        assert b : "Invalid update sequence [updateSeq=" + updateSeq + ", seq=" + seq +
                            ", curUpdateSeq=" + this.updateSeq.get() + ", node2part=" + node2part.toFullString() + ']';

                        updateSeq = seq + 1;
                    }
                    else
                        updateSeq = seq;
                }

                node2part.updateSequence(updateSeq);
            }
        }

        GridDhtPartitionMap map = node2part.get(nodeId);

        if (map == null)
            node2part.put(nodeId, map = new GridDhtPartitionMap(nodeId, updateSeq,
                Collections.<Integer, GridDhtPartitionState>emptyMap(), false));

        map.updateSequence(updateSeq);

        map.put(p, state);

        Set<UUID> ids = part2node.get(p);

        if (ids == null)
            part2node.put(p, ids = new HashSet<>(3));

        ids.add(nodeId);
    }

    /**
     * @param nodeId Node to remove.
     */
    private void removeNode(UUID nodeId) {
        assert nodeId != null;
        assert lock.writeLock().isHeldByCurrentThread();

        GridNode oldest = CU.oldest(cctx, topVer);

        GridNode loc = cctx.localNode();

        if (node2part != null) {
            if (oldest.equals(loc) && !node2part.nodeId().equals(loc.id())) {
                updateSeq.setIfGreater(node2part.updateSequence());

                node2part = new GridDhtPartitionFullMap(loc.id(), loc.order(), updateSeq.incrementAndGet(),
                    node2part, false);
            }
            else
                node2part = new GridDhtPartitionFullMap(node2part, node2part.updateSequence());

            part2node = new HashMap<>(part2node);

            GridDhtPartitionMap parts = node2part.remove(nodeId);

            if (parts != null) {
                for (Integer p : parts.keySet()) {
                    Set<UUID> nodeIds = part2node.get(p);

                    if (nodeIds != null) {
                        nodeIds.remove(nodeId);

                        if (nodeIds.isEmpty())
                            part2node.remove(p);
                    }
                }
            }

            consistencyCheck();
        }
    }

    /** {@inheritDoc} */
    @Override public boolean own(GridDhtLocalPartition<K, V> part) {
        GridNode loc = cctx.localNode();

        lock.writeLock().lock();

        try {
            if (part.own()) {
                updateLocal(part.id(), loc.id(), part.state(), updateSeq.incrementAndGet());

                consistencyCheck();

                return true;
            }

            consistencyCheck();

            return false;
        }
        finally {
            lock.writeLock().unlock();
        }
    }

    /** {@inheritDoc} */
    @Override public void onEvicted(GridDhtLocalPartition<K, V> part, boolean updateSeq) {
        assert updateSeq || lock.isWriteLockedByCurrentThread();

        lock.writeLock().lock();

        try {
            assert part.state() == EVICTED;

            long seq = updateSeq ? this.updateSeq.incrementAndGet() : this.updateSeq.get();

            updateLocal(part.id(), cctx.localNodeId(), part.state(), seq);

            consistencyCheck();
        }
        finally {
            lock.writeLock().unlock();
        }
    }

    /** {@inheritDoc} */
    @Nullable @Override public GridDhtPartitionMap partitions(UUID nodeId) {
        lock.readLock().lock();

        try {
            return node2part.get(nodeId);
        }
        finally {
            lock.readLock().unlock();
        }
    }

    /** {@inheritDoc} */
    @Override public void printMemoryStats(int threshold) {
        X.println(">>>  Cache partition topology stats [grid=" + cctx.gridName() + ", cache=" + cctx.name() + ']');

        for (GridDhtLocalPartition part : locParts.values()) {
            int size = part.size();

            if (size >= threshold)
                X.println(">>>   Local partition [part=" + part.id() + ", size=" + size + ']');
        }
    }

    /**
     * @param p Partition.
     * @param nodeId Node ID.
     * @param match State to match.
     * @param matches Additional states.
     * @return Filter for owners of this partition.
     */
    private boolean hasState(final int p, @Nullable UUID nodeId, final GridDhtPartitionState match,
        final GridDhtPartitionState... matches) {
        if (nodeId == null)
            return false;

        GridDhtPartitionMap parts = node2part.get(nodeId);

        // Set can be null if node has been removed.
        if (parts != null) {
            GridDhtPartitionState state = parts.get(p);

            if (state == match)
                return true;

            if (matches != null && matches.length > 0)
                for (GridDhtPartitionState s : matches)
                    if (state == s)
                        return true;
        }

        return false;
    }

    /**
     * Checks consistency after all operations.
     */
    private void consistencyCheck() {
        if (CONSISTENCY_CHECK) {
            assert lock.writeLock().isHeldByCurrentThread();

            if (node2part == null)
                return;

            for (Map.Entry<UUID, GridDhtPartitionMap> e : node2part.entrySet()) {
                for (Integer p : e.getValue().keySet()) {
                    Set<UUID> nodeIds = part2node.get(p);

                    assert nodeIds != null : "Failed consistency check [part=" + p + ", nodeId=" + e.getKey() + ']';
                    assert nodeIds.contains(e.getKey()) : "Failed consistency check [part=" + p + ", nodeId=" +
                        e.getKey() + ", nodeIds=" + nodeIds + ']';
                }
            }

            for (Map.Entry<Integer, Set<UUID>> e : part2node.entrySet()) {
                for (UUID nodeId : e.getValue()) {
                    GridDhtPartitionMap map = node2part.get(nodeId);

                    assert map != null : "Failed consistency check [part=" + e.getKey() + ", nodeId=" + nodeId + ']';
                    assert map.containsKey(e.getKey()) : "Failed consistency check [part=" + e.getKey() +
                        ", nodeId=" + nodeId + ']';
                }
            }
        }
    }
}
