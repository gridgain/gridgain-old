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

package org.gridgain.testframework;

import org.gridgain.grid.*;
import org.gridgain.grid.events.*;
import org.gridgain.grid.kernal.managers.communication.*;
import org.gridgain.grid.kernal.managers.eventstorage.*;
import org.gridgain.grid.security.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.discovery.*;
import org.gridgain.grid.spi.swapspace.*;
import org.gridgain.grid.util.typedef.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.*;

import static org.gridgain.grid.events.GridEventType.*;

/**
 * Test SPI context.
 */
public class GridSpiTestContext implements GridSpiContext {
    /** */
    private final Collection<GridNode> rmtNodes = new ConcurrentLinkedQueue<>();

    /** */
    private GridNode locNode;

    /** */
    private final Map<GridLocalEventListener, Set<Integer>> evtLsnrs = new HashMap<>();

    /** */
    @SuppressWarnings("deprecation")
    private final Collection<GridMessageListener> msgLsnrs = new ArrayList<>();

    /** */
    private final Map<GridNode, Serializable> sentMsgs = new HashMap<>();

    /** */
    private final ConcurrentMap<String, Map> cache = new ConcurrentHashMap<>();

    /** {@inheritDoc} */
    @Override public Collection<GridNode> remoteNodes() {
        return rmtNodes;
    }

    /** {@inheritDoc} */
    @Override public GridNode localNode() {
        return locNode;
    }

    /** {@inheritDoc} */
    @Override public Collection<GridNode> remoteDaemonNodes() {
        Collection<GridNode> daemons = new ArrayList<>();

        for (GridNode node : rmtNodes) {
            if (node.isDaemon())
                daemons.add(node);
        }

        return daemons;
    }

    /** {@inheritDoc} */
    @Override public Collection<GridNode> nodes() {
        Collection<GridNode> all = new ArrayList<>(rmtNodes);

        if (locNode != null)
            all.add(locNode);

        return all;
    }

    /**
     * @param locNode Local node.
     */
    public void setLocalNode(@Nullable GridNode locNode) {
        this.locNode = locNode;
    }

    /** {@inheritDoc} */
    @Nullable @Override
    public GridNode node(UUID nodeId) {
        if (locNode != null && locNode.id().equals(nodeId))
            return locNode;

        for (GridNode node : rmtNodes) {
            if (node.id().equals(nodeId))
                return node;
        }

        return null;
    }

    /** */
    public void createLocalNode() {
        setLocalNode(new GridTestNode(UUID.randomUUID(), createMetrics(1, 1)));
    }

    /**
     * @param cnt Number of nodes.
     */
    public void createRemoteNodes(int cnt) {
        for (int i = 0; i < cnt; i++)
            addNode(new GridTestNode(UUID.randomUUID(), createMetrics(1, 1)));
    }

    /** */
    public void reset() {
        setLocalNode(null);

        rmtNodes.clear();
    }

    /**
     * @param waitingJobs Waiting jobs count.
     * @param activeJobs Active jobs count.
     * @return Metrics adapter.
     */
    private GridDiscoveryMetricsAdapter createMetrics(int waitingJobs, int activeJobs) {
        GridDiscoveryMetricsAdapter metrics = new GridDiscoveryMetricsAdapter();

        metrics.setCurrentWaitingJobs(waitingJobs);
        metrics.setCurrentActiveJobs(activeJobs);

        return metrics;
    }

    /**
     * @param nodes Nodes to reset.
     * @param rmv Whether nodes that were not passed in should be removed or not.
     */
    public void resetNodes(Collection<GridNode> nodes, boolean rmv) {
        for (GridNode node : nodes) {
            assert !node.equals(locNode);

            if (!rmtNodes.contains(node))
                addNode(node);
        }

        if (rmv) {
            for (Iterator<GridNode> iter = rmtNodes.iterator(); iter.hasNext();) {
                GridNode node = iter.next();

                if (!nodes.contains(node)) {
                    iter.remove();

                    notifyListener(new GridDiscoveryEvent(locNode, "Node left", EVT_NODE_LEFT, node));
                }
            }
        }
    }

    /**
     * @param node Node to check.
     * @return {@code True} if the node is local.
     */
    public boolean isLocalNode(GridNode node) {
        return locNode.equals(node);
    }

    /**
     * @param node Node to add.
     */
    public void addNode(GridNode node) {
        rmtNodes.add(node);

        notifyListener(new GridDiscoveryEvent(locNode, "Node joined", EVT_NODE_JOINED, node));
    }

    /**
     * @param node Node to remove.
     */
    public void removeNode(GridNode node) {
        if (rmtNodes.remove(node))
            notifyListener(new GridDiscoveryEvent(locNode, "Node left", EVT_NODE_LEFT, node));
    }

    /**
     * @param nodeId Node ID.
     */
    public void removeNode(UUID nodeId) {
        for (Iterator<GridNode> iter = rmtNodes.iterator(); iter.hasNext();) {
            GridNode node = iter.next();

            if (node.id().equals(nodeId)) {
                iter.remove();

                notifyListener(new GridDiscoveryEvent(locNode, "Node left", EVT_NODE_LEFT, node));
            }
        }
    }

    /**
     * @param node Node to fail.
     */
    public void failNode(GridNode node) {
        if (rmtNodes.remove(node))
            notifyListener(new GridDiscoveryEvent(locNode, "Node failed", EVT_NODE_FAILED, node));
    }

    /**
     * @param node Node for metrics update.
     */
    public void updateMetrics(GridNode node) {
        if (locNode.equals(node) || rmtNodes.contains(node))
            notifyListener(new GridDiscoveryEvent(locNode, "Metrics updated.", EVT_NODE_METRICS_UPDATED, node));
    }

    /** */
    public void updateAllMetrics() {
        notifyListener(new GridDiscoveryEvent(locNode, "Metrics updated", EVT_NODE_METRICS_UPDATED, locNode));

        for (GridNode node : rmtNodes) {
            notifyListener(new GridDiscoveryEvent(locNode, "Metrics updated", EVT_NODE_METRICS_UPDATED, node));
        }
    }

    /**
     * @param evt Event node.
     */
    private void notifyListener(GridEvent evt) {
        assert evt.type() > 0;

        for (Map.Entry<GridLocalEventListener, Set<Integer>> entry : evtLsnrs.entrySet()) {
            if (F.isEmpty(entry.getValue()) || entry.getValue().contains(evt.type()))
                entry.getKey().onEvent(evt);
        }
    }

    /** {@inheritDoc} */
    @Override public boolean pingNode(UUID nodeId) {
        return node(nodeId) != null;
    }

    /** {@inheritDoc} */
    @Override public void send(GridNode node, Serializable msg, String topic)
        throws GridSpiException {
        sentMsgs.put(node, msg);
    }

    /**
     * @param node Node message was sent to.
     * @return Sent message.
     */
    public Serializable getSentMessage(GridNode node) {
        return sentMsgs.get(node);
    }

    /**
     * @param node Node message was sent to.
     * @return Sent message.
     */
    public Serializable removeSentMessage(GridNode node) {
        return sentMsgs.remove(node);
    }

    /**
     * @param node Destination node.
     * @param msg Message.
     */
    @SuppressWarnings("deprecation")
    public void triggerMessage(GridNode node, Object msg) {
        for (GridMessageListener lsnr : msgLsnrs) {
            lsnr.onMessage(node.id(), msg);
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("deprecation")
    @Override public void addMessageListener(GridMessageListener lsnr, String topic) {
        msgLsnrs.add(lsnr);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("deprecation")
    @Override public boolean removeMessageListener(GridMessageListener lsnr, String topic) {
        return msgLsnrs.remove(lsnr);
    }

    /**
     * @param type Event type.
     * @param taskName Task name.
     * @param taskSesId Session ID.
     * @param msg Event message.
     */
    public void triggerTaskEvent(int type, String taskName, GridUuid taskSesId, String msg) {
        assert type > 0;

        triggerEvent(new GridTaskEvent(locNode, msg, type, taskSesId, taskName, null, false, null));
    }

    /**
     * @param evt Event to trigger.
     */
    public void triggerEvent(GridEvent evt) {
        notifyListener(evt);
    }

    /** {@inheritDoc} */
    @Override public void addLocalEventListener(GridLocalEventListener lsnr, int... types) {
        Set<Integer> typeSet = F.addIfAbsent(evtLsnrs, lsnr, F.<Integer>newSet());

        assert typeSet != null;

        if (types != null) {
            for (int type : types) {
                typeSet.add(type);
            }
        }
    }

    /** {@inheritDoc} */
    @Override public boolean removeLocalEventListener(GridLocalEventListener lsnr) {
        boolean res = evtLsnrs.containsKey(lsnr);

        evtLsnrs.remove(lsnr);

        return res;
    }

    /** {@inheritDoc} */
    @Override public boolean isEventRecordable(int... types) {
        return true;
    }

    /** {@inheritDoc} */
    @Override public void recordEvent(GridEvent evt) {
        notifyListener(evt);
    }

    /** {@inheritDoc} */
    @Override public void registerPort(int port, GridPortProtocol proto) {
        /* No-op. */
    }

    /** {@inheritDoc} */
    @Override public void deregisterPort(int port, GridPortProtocol proto) {
        /* No-op. */
    }

    /** {@inheritDoc} */
    @Override public void deregisterPorts() {
        /* No-op. */
    }

    /** {@inheritDoc} */
    @Override public <K, V> V get(String cacheName, K key) throws GridException {
        assert cacheName != null;
        assert key != null;

        V res = null;

        Map<K, CachedObject<V>> cache = getOrCreateCache(cacheName);

        CachedObject<V> obj = cache.get(key);

        if (obj != null) {
            if (obj.expire == 0 || obj.expire > System.currentTimeMillis())
                res = obj.obj;
            else
                cache.remove(key);
        }

        return res;
    }

    /** {@inheritDoc} */
    @Override public <K, V> V put(String cacheName, K key, V val, long ttl) throws GridException {
        assert cacheName != null;
        assert key != null;
        assert ttl >= 0;

        long expire = ttl > 0 ? System.currentTimeMillis() + ttl : 0;

        CachedObject<V> obj = new CachedObject<>(expire, val);

        Map<K, CachedObject<V>> cache = getOrCreateCache(cacheName);

        CachedObject<V> prev = cache.put(key, obj);

        return prev != null ? prev.obj : null;
    }

    /** {@inheritDoc} */
    @SuppressWarnings({"unchecked"})
    @Override public <K, V> V putIfAbsent(String cacheName, K key, V val, long ttl) throws GridException {
        V v = get(cacheName, key);

        if (v != null)
            return put(cacheName, key, val, ttl);

        return v;
    }

    /** {@inheritDoc} */
    @Override public <K, V> V remove(String cacheName, K key) throws GridException {
        assert cacheName != null;
        assert key != null;

        Map<K, CachedObject<V>> cache = getOrCreateCache(cacheName);

        CachedObject<V> prev = cache.remove(key);

        return prev != null ? prev.obj : null;
    }

    /** {@inheritDoc} */
    @Override public <K> boolean containsKey(String cacheName, K key) {
        assert cacheName != null;
        assert key != null;

        boolean res = false;

        try {
            res =  get(cacheName, key) != null;
        }
        catch (GridException ignored) {

        }

        return res;
    }

    /** {@inheritDoc} */
    @Override public void writeToSwap(String spaceName, Object key, @Nullable Object val,
        @Nullable ClassLoader ldr) throws GridException {
        /* No-op. */
    }

    /** {@inheritDoc} */
    @Override public <T> T readFromSwap(String spaceName, GridSwapKey key, @Nullable ClassLoader ldr)
        throws GridException {
        return null;
    }

    /** {@inheritDoc} */
    @Override public <T> T readFromOffheap(String spaceName, int part, Object key, byte[] keyBytes,
        @Nullable ClassLoader ldr) throws GridException {
        return null;
    }

    /** {@inheritDoc} */
    @Override public boolean removeFromOffheap(@Nullable String spaceName, int part, Object key,
        @Nullable byte[] keyBytes) throws GridException {
        return false;
    }

    /** {@inheritDoc} */
    @Override public void writeToOffheap(@Nullable String spaceName, int part, Object key, @Nullable byte[] keyBytes,
        Object val, @Nullable byte[] valBytes, @Nullable ClassLoader ldr) throws GridException {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override public int partition(String cacheName, Object key) {
        return -1;
    }

    /** {@inheritDoc} */
    @Override public void removeFromSwap(String spaceName, Object key,
        @Nullable ClassLoader ldr) throws GridException {
        // No-op.
    }

    /** {@inheritDoc} */
    @Nullable @Override public GridNodeValidationResult validateNode(GridNode node) {
        return null;
    }

    /** {@inheritDoc} */
    @Override public boolean writeDelta(UUID nodeId, Class<?> msgCls, ByteBuffer buf) {
        return false;
    }

    /** {@inheritDoc} */
    @Override public boolean readDelta(UUID nodeId, Class<?> msgCls, ByteBuffer buf) {
        return false;
    }

    /** {@inheritDoc} */
    @Override public Collection<GridSecuritySubject> authenticatedSubjects() throws GridException {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override public GridSecuritySubject authenticatedSubject(UUID subjId) throws GridException {
        return null;
    }

    /** {@inheritDoc} */
    @Nullable @Override public <T> T readValueFromOffheapAndSwap(@Nullable String spaceName, Object key,
        @Nullable ClassLoader ldr) throws GridException {
        return null;
    }

    /**
     * @param cacheName Cache name.
     * @return Map representing cache.
     */
    @SuppressWarnings("unchecked")
    private <K, V> Map<K, V> getOrCreateCache(String cacheName) {
        synchronized (cache) {
            Map<K, V> map = cache.get(cacheName);

            if (map == null)
                cache.put(cacheName, map = new ConcurrentHashMap<>());

            return map;
        }
    }

    /**
     * Cached object.
     */
    private static class CachedObject<V> {
        /** */
        private long expire;

        /** */
        private V obj;

        /**
         * @param expire Expire time.
         * @param obj Object.
         */
        private CachedObject(long expire, V obj) {
            this.expire = expire;
            this.obj = obj;
        }
    }
}
