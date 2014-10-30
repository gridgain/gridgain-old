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

package org.gridgain.grid.kernal.managers.communication;

import org.gridgain.grid.*;
import org.gridgain.grid.events.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.kernal.managers.*;
import org.gridgain.grid.kernal.managers.deployment.*;
import org.gridgain.grid.kernal.managers.eventstorage.*;
import org.gridgain.grid.kernal.processors.timeout.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.marshaller.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.communication.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.direct.*;
import org.gridgain.grid.util.tostring.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.grid.util.worker.*;
import org.jdk8.backport.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.util.*;
import java.util.Map.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

import static org.gridgain.grid.events.GridEventType.*;
import static org.gridgain.grid.kernal.GridTopic.*;
import static org.gridgain.grid.kernal.managers.communication.GridIoPolicy.*;
import static org.jdk8.backport.ConcurrentLinkedHashMap.QueuePolicy.*;
import static org.gridgain.grid.util.nio.GridNioBackPressureControl.*;

/**
 * Grid communication manager.
 */
public class GridIoManager extends GridManagerAdapter<GridCommunicationSpi<Serializable>> {
    /** Max closed topics to store. */
    public static final int MAX_CLOSED_TOPICS = 10240;

    /** Ordered messages comparator. */
    private static final Comparator<GridBiTuple<GridIoMessage, Long>> MSG_CMP =
        new Comparator<GridBiTuple<GridIoMessage, Long>>() {
            @Override public int compare(GridBiTuple<GridIoMessage, Long> t1, GridBiTuple<GridIoMessage, Long> t2) {
                return t1.get1().messageId() < t2.get1().messageId() ? -1 :
                    t1.get1().messageId() == t2.get1().messageId() ? 0 : 1;
            }
        };

    /** Listeners by topic. */
    private final ConcurrentMap<Object, GridMessageListener> lsnrMap = new ConcurrentHashMap8<>();

    /** Disconnect listeners. */
    private final Collection<GridDisconnectListener> disconnectLsnrs = new ConcurrentLinkedQueue<>();

    /** Public pool. */
    private ExecutorService pubPool;

    /** Internal P2P pool. */
    private ExecutorService p2pPool;

    /** Internal system pool. */
    private ExecutorService sysPool;

    /** Internal management pool. */
    private ExecutorService mgmtPool;

    /** Affinity assignment executor service. */
    private ExecutorService affPool;

    /** Internal DR pool. */
    private ExecutorService drPool;

    /** Discovery listener. */
    private GridLocalEventListener discoLsnr;

    /** */
    private final ConcurrentMap<Object, ConcurrentMap<UUID, GridCommunicationMessageSet>> msgSetMap =
        new ConcurrentHashMap8<>();

    /** Messages ID generator (per topic). */
    private final ConcurrentMap<Object, ConcurrentMap<UUID, AtomicLong>> msgIdMap =
        new ConcurrentHashMap8<>();

    /** Local node ID. */
    private final UUID locNodeId;

    /** Discovery delay. */
    private final long discoDelay;

    /** Cache for messages that were received prior to discovery. */
    private final ConcurrentMap<UUID, ConcurrentLinkedDeque8<DelayedMessage>> waitMap =
        new ConcurrentHashMap8<>();

    /** Communication message listener. */
    private GridCommunicationListener<Serializable> commLsnr;

    /** Grid marshaller. */
    private final GridMarshaller marsh;

    /** Busy lock. */
    private final GridSpinReadWriteLock busyLock = new GridSpinReadWriteLock();

    /** Lock to sync maps access. */
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /** Message cache. */
    private ThreadLocal<GridBiTuple<Object, byte[]>> cacheMsg =
        new GridThreadLocal<GridBiTuple<Object, byte[]>>() {
            @Nullable @Override protected GridBiTuple<Object, byte[]> initialValue() {
                return null;
            }
        };

    /** Fully started flag. When set to true, can send and receive messages. */
    private volatile boolean started;

    /** Closed topics. */
    private final GridBoundedConcurrentLinkedHashSet<Object> closedTopics =
        new GridBoundedConcurrentLinkedHashSet<>(MAX_CLOSED_TOPICS, MAX_CLOSED_TOPICS, 0.75f, 256,
            PER_SEGMENT_Q_OPTIMIZED_RMV);

    /** Workers count. */
    private final LongAdder workersCnt = new LongAdder();

    /**
     * @param ctx Grid kernal context.
     */
    @SuppressWarnings("deprecation")
    public GridIoManager(GridKernalContext ctx) {
        super(ctx, ctx.config().getCommunicationSpi());

        locNodeId = ctx.localNodeId();

        discoDelay = ctx.config().getDiscoveryStartupDelay();

        marsh = ctx.config().getMarshaller();
    }

    /**
     * Resets metrics for this manager.
     */
    public void resetMetrics() {
        getSpi().resetMetrics();
    }

    /** {@inheritDoc} */
    @SuppressWarnings("deprecation")
    @Override public void start() throws GridException {
        assertParameter(discoDelay > 0, "discoveryStartupDelay > 0");

        startSpi();

        pubPool = ctx.config().getExecutorService();
        p2pPool = ctx.config().getPeerClassLoadingExecutorService();
        sysPool = ctx.config().getSystemExecutorService();
        mgmtPool = ctx.config().getManagementExecutorService();
        drPool = ctx.drPool();
        affPool = Executors.newFixedThreadPool(1);

        getSpi().setListener(commLsnr = new GridCommunicationListener<Serializable>() {
            @Override public void onMessage(UUID nodeId, Serializable msg, GridRunnable msgC) {
                try {
                    onMessage0(nodeId, (GridIoMessage)msg, msgC);
                }
                catch (ClassCastException ignored) {
                    U.error(log, "Communication manager received message of unknown type (will ignore): " +
                        msg.getClass().getName() + ". Most likely GridCommunicationSpi is being used directly, " +
                        "which is illegal - make sure to send messages only via GridProjection API.");
                }
            }

            @Override public void onDisconnected(UUID nodeId) {
                for (GridDisconnectListener lsnr : disconnectLsnrs)
                    lsnr.onNodeDisconnected(nodeId);
            }
        });

        if (log.isDebugEnabled())
            log.debug(startInfo());
    }

    /** {@inheritDoc} */
    @SuppressWarnings({"deprecation", "SynchronizationOnLocalVariableOrMethodParameter"})
    @Override public void onKernalStart0() throws GridException {
        discoLsnr = new GridLocalEventListener() {
            @SuppressWarnings({"TooBroadScope", "fallthrough"})
            @Override public void onEvent(GridEvent evt) {
                assert evt instanceof GridDiscoveryEvent : "Invalid event: " + evt;

                GridDiscoveryEvent discoEvt = (GridDiscoveryEvent)evt;

                UUID nodeId = discoEvt.eventNode().id();

                switch (evt.type()) {
                    case EVT_NODE_JOINED:
                        ConcurrentLinkedDeque8<DelayedMessage> delayedMsgs = null;

                        lock.writeLock().lock();

                        try {
                            if (started)
                                delayedMsgs = waitMap.remove(nodeId);
                        }
                        finally {
                            lock.writeLock().unlock();
                        }

                        if (log.isDebugEnabled())
                            log.debug("Processing messages from discovery startup delay list " +
                                "(sender node joined topology): " + delayedMsgs);

                        // After write lock released.
                        if (delayedMsgs != null)
                            for (DelayedMessage msg : delayedMsgs)
                                commLsnr.onMessage(msg.nodeId(), msg.message(), msg.callback());

                        break;

                    case EVT_NODE_LEFT:
                    case EVT_NODE_FAILED:
                        for (Map.Entry<Object, ConcurrentMap<UUID, GridCommunicationMessageSet>> e :
                            msgSetMap.entrySet()) {
                            ConcurrentMap<UUID, GridCommunicationMessageSet> map = e.getValue();

                            GridCommunicationMessageSet set;

                            boolean empty;

                            synchronized (map) {
                                set = map.remove(nodeId);

                                empty = map.isEmpty();
                            }

                            if (set != null) {
                                if (log.isDebugEnabled())
                                    log.debug("Removed message set due to node leaving grid: " + set);

                                // Unregister timeout listener.
                                ctx.timeout().removeTimeoutObject(set);

                                // Node may still send stale messages for this topic
                                // even after discovery notification is done.
                                closedTopics.add(set.topic());
                            }

                            if (empty)
                                msgSetMap.remove(e.getKey(), map);
                        }

                        // Clean up delayed and ordered messages (need exclusive lock).
                        lock.writeLock().lock();

                        try {
                            ConcurrentLinkedDeque8<DelayedMessage> waitList = waitMap.remove(nodeId);

                            if (log.isDebugEnabled())
                                log.debug("Removed messages from discovery startup delay list " +
                                    "(sender node left topology): " + waitList);
                        }
                        finally {
                            lock.writeLock().unlock();
                        }

                        break;

                    default:
                        assert false : "Unexpected event: " + evt;
                }
            }
        };

        ctx.event().addLocalEventListener(discoLsnr, EVT_NODE_JOINED, EVT_NODE_LEFT, EVT_NODE_FAILED);

        // Make sure that there are no stale messages due to window between communication
        // manager start and kernal start.
        // 1. Process wait list.
        Collection<Collection<DelayedMessage>> delayedMsgs = new ArrayList<>();

        lock.writeLock().lock();

        try {
            started = true;

            for (Entry<UUID, ConcurrentLinkedDeque8<DelayedMessage>> e : waitMap.entrySet()) {
                if (ctx.discovery().node(e.getKey()) != null) {
                    ConcurrentLinkedDeque8<DelayedMessage> waitList = waitMap.remove(e.getKey());

                    if (log.isDebugEnabled())
                        log.debug("Processing messages from discovery startup delay list: " + waitList);

                    if (waitList != null)
                        delayedMsgs.add(waitList);
                }
            }
        }
        finally {
            lock.writeLock().unlock();
        }

        // After write lock released.
        if (!delayedMsgs.isEmpty()) {
            for (Collection<DelayedMessage> col : delayedMsgs)
                for (DelayedMessage msg : col)
                    commLsnr.onMessage(msg.nodeId(), msg.message(), msg.callback());
        }

        // 2. Process messages sets.
        for (Map.Entry<Object, ConcurrentMap<UUID, GridCommunicationMessageSet>> e : msgSetMap.entrySet()) {
            ConcurrentMap<UUID, GridCommunicationMessageSet> map = e.getValue();

            for (GridCommunicationMessageSet set : map.values()) {
                if (ctx.discovery().node(set.nodeId()) == null) {
                    // All map modifications should be synced for consistency.
                    boolean rmv;

                    synchronized (map) {
                        rmv = map.remove(set.nodeId(), set);
                    }

                    if (rmv) {
                        if (log.isDebugEnabled())
                            log.debug("Removed message set due to node leaving grid: " + set);

                        // Unregister timeout listener.
                        ctx.timeout().removeTimeoutObject(set);
                    }

                }
            }

            boolean rmv;

            synchronized (map) {
                rmv = map.isEmpty();
            }

            if (rmv) {
                msgSetMap.remove(e.getKey(), map);

                // Node may still send stale messages for this topic
                // even after discovery notification is done.
                closedTopics.add(e.getKey());
            }
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("BusyWait")
    @Override public void onKernalStop0(boolean cancel) {
        // No more communication messages.
        getSpi().setListener(null);

        busyLock.writeLock();

        U.shutdownNow(getClass(), affPool, log);

        boolean interrupted = false;

        while (workersCnt.sum() != 0) {
            try {
                Thread.sleep(200);
            }
            catch (InterruptedException ignored) {
                interrupted = true;
            }
        }

        if (interrupted)
            Thread.currentThread().interrupt();

        GridEventStorageManager evtMgr = ctx.event();

        if (evtMgr != null && discoLsnr != null)
            evtMgr.removeLocalEventListener(discoLsnr);
    }

    /** {@inheritDoc} */
    @Override public void stop(boolean cancel) throws GridException {
        stopSpi();

        // Clear cache.
        cacheMsg.set(null);

        if (log.isDebugEnabled())
            log.debug(stopInfo());
    }

    /**
     * @param nodeId Node ID.
     * @param msg Message bytes.
     * @param msgC Closure to call when message processing finished.
     */
    @SuppressWarnings("fallthrough")
    private void onMessage0(UUID nodeId, GridIoMessage msg, GridRunnable msgC) {
        assert nodeId != null;
        assert msg != null;

        if (!busyLock.tryReadLock()) {
            if (log.isDebugEnabled())
                log.debug("Received communication message while stopping grid.");

            return;
        }

        try {
            // Check discovery.
            GridNode node = ctx.discovery().node(nodeId);

            if (node == null) {
                if (log.isDebugEnabled())
                    log.debug("Ignoring message from dead node [senderId=" + nodeId + ", msg=" + msg + ']');

                return; // We can't receive messages from non-discovered ones.
            }

            if (msg.topic() == null) {
                int topicOrd = msg.topicOrdinal();

                msg.topic(topicOrd >= 0 ? GridTopic.fromOrdinal(topicOrd) : marsh.unmarshal(msg.topicBytes(), null));
            }

            if (!started) {
                lock.readLock().lock();

                try {
                    if (!started) { // Sets to true in write lock, so double checking.
                        // Received message before valid context is set to manager.
                        if (log.isDebugEnabled())
                            log.debug("Adding message to waiting list [senderId=" + nodeId +
                                ", msg=" + msg + ']');

                        ConcurrentLinkedDeque8<DelayedMessage> list =
                            F.addIfAbsent(waitMap, nodeId, F.<DelayedMessage>newDeque());

                        assert list != null;

                        list.add(new DelayedMessage(nodeId, msg, msgC));

                        return;
                    }
                }
                finally {
                    lock.readLock().unlock();
                }
            }

            // If message is P2P, then process in P2P service.
            // This is done to avoid extra waiting and potential deadlocks
            // as thread pool may not have any available threads to give.
            GridIoPolicy plc = msg.policy();

            switch (plc) {
                case P2P_POOL: {
                    processP2PMessage(node, msg, msgC);

                    break;
                }

                case PUBLIC_POOL:
                case SYSTEM_POOL:
                case MANAGEMENT_POOL:
                case AFFINITY_POOL:
                case DR_POOL: {
                    if (msg.isOrdered())
                        processOrderedMessage(node, msg, plc, msgC);
                    else
                        processRegularMessage(node, msg, plc, msgC);

                    break;
                }
            }
        }
        catch (GridException e) {
            U.error(log, "Failed to process message (will ignore): " + msg, e);
        }
        finally {
            busyLock.readUnlock();
        }
    }

    /**
     * Gets execution pool for policy.
     *
     * @param plc Policy.
     * @return Execution pool.
     */
    private Executor pool(GridIoPolicy plc) {
        switch (plc) {
            case P2P_POOL:
                return p2pPool;
            case SYSTEM_POOL:
                return sysPool;
            case PUBLIC_POOL:
                return pubPool;
            case MANAGEMENT_POOL:
                return mgmtPool;
            case AFFINITY_POOL:
                return affPool;
            case DR_POOL:
                assert drPool != null : "DR pool is not configured.";

                return drPool;

            default: {
                assert false : "Invalid communication policy: " + plc;

                // Never reached.
                return null;
            }
        }
    }

    /**
     * @param msg Message bytes.
     * @return Policy.
     */
    private GridIoPolicy policy(byte[] msg) {
        GridIoPolicy plc = GridIoPolicy.fromOrdinal(msg[0]);

        if (plc == null)
            throw new IllegalStateException("Failed to parse message policy: " + Arrays.toString(msg));

        return plc;
    }

    /**
     * @param msg Message bytes.
     * @return {@code True} if ordered.
     */
    private boolean ordered(byte[] msg) {
        return msg[1] == 1;
    }

    /**
     * @param node Node.
     * @param msg Message.
     * @param msgC Closure to call when message processing finished.
     */
    @SuppressWarnings("deprecation")
    private void processP2PMessage(final GridNode node, final GridIoMessage msg, final GridRunnable msgC) {
        workersCnt.increment();

        Runnable c = new GridWorker(ctx.gridName(), "msg-worker", log) {
            @Override protected void body() {
                try {
                    threadProcessingMessage(true);

                    GridMessageListener lsnr = lsnrMap.get(msg.topic());

                    if (lsnr == null)
                        return;

                    Object obj = msg.message();

                    assert obj != null;

                    lsnr.onMessage(node.id(), obj);
                }
                finally {
                    threadProcessingMessage(false);

                    workersCnt.decrement();

                    msgC.run();
                }
            }
        };

        try {
            p2pPool.execute(c);
        }
        catch (RejectedExecutionException e) {
            U.error(log, "Failed to process P2P message due to execution rejection. Increase the upper bound " +
                "on 'ExecutorService' provided by 'GridConfiguration.getPeerClassLoadingExecutorService()'. " +
                "Will attempt to process message in the listener thread instead.", e);

            c.run();
        }
    }

    /**
     * @param node Node.
     * @param msg Message.
     * @param plc Execution policy.
     * @param msgC Closure to call when message processing finished.
     */
    private void processRegularMessage(final GridNode node, final GridIoMessage msg, GridIoPolicy plc,
        final GridRunnable msgC) {
        workersCnt.increment();

        Runnable c = new GridWorker(ctx.gridName(), "msg-worker", log) {
            @Override protected void body() {
                try {
                    threadProcessingMessage(true);

                    processRegularMessage0(msg, node.id());
                }
                finally {
                    threadProcessingMessage(false);

                    workersCnt.decrement();

                    msgC.run();
                }
            }
        };

        try {
            pool(plc).execute(c);
        }
        catch (RejectedExecutionException e) {
            U.error(log, "Failed to process regular message due to execution rejection. Increase the upper bound " +
                "on 'ExecutorService' provided by 'GridConfiguration.getExecutorService()'. " +
                "Will attempt to process message in the listener thread instead.", e);

            c.run();
        }
    }

    /**
     * @param msg Message.
     * @param nodeId Node ID.
     */
    @SuppressWarnings("deprecation")
    private void processRegularMessage0(GridIoMessage msg, UUID nodeId) {
        GridMessageListener lsnr = lsnrMap.get(msg.topic());

        if (lsnr == null)
            return;

        Object obj = msg.message();

        assert obj != null;

        lsnr.onMessage(nodeId, obj);
    }

    /**
     * @param node Node.
     * @param msg Ordered message.
     * @param plc Execution policy.
     * @param msgC Closure to call when message processing finished.
     */
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    private void processOrderedMessage(final GridNode node, final GridIoMessage msg, final GridIoPolicy plc,
        final GridRunnable msgC) {
        assert msg != null;

        workersCnt.increment();

        Runnable c = new GridWorker(ctx.gridName(), "msg-worker", log) {
            @Override protected void body() {
                try {
                    threadProcessingMessage(true);

                    processOrderedMessage0(msg, plc, node.id());
                }
                finally {
                    threadProcessingMessage(false);

                    workersCnt.decrement();

                    msgC.run();
                }
            }
        };

        try {
            pool(plc).execute(c);
        }
        catch (RejectedExecutionException e) {
            U.error(log, "Failed to process ordered message due to execution rejection. " +
                "Increase the upper bound on executor service provided by corresponding " +
                "configuration property. Will attempt to process message in the listener " +
                "thread instead [msgPlc=" + plc + ']', e);

            c.run();
        }
    }

    /**
     * @param msg Message.
     * @param plc Policy.
     * @param nodeId Node ID.
     */
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    private void processOrderedMessage0(GridIoMessage msg, GridIoPolicy plc, UUID nodeId) {
        long timeout = msg.timeout();
        boolean skipOnTimeout = msg.skipOnTimeout();

        boolean isNew = false;

        ConcurrentMap<UUID, GridCommunicationMessageSet> map;

        GridCommunicationMessageSet set = null;

        while (true) {
            map = msgSetMap.get(msg.topic());

            if (map == null) {
                set = new GridCommunicationMessageSet(plc, msg.topic(), nodeId, timeout, skipOnTimeout, msg);

                map = new ConcurrentHashMap0<>();

                map.put(nodeId, set);

                ConcurrentMap<UUID, GridCommunicationMessageSet> old = msgSetMap.putIfAbsent(
                    msg.topic(), map);

                if (old != null)
                    map = old;
                else {
                    isNew = true;

                    // Put succeeded.
                    break;
                }
            }

            boolean rmv = false;

            synchronized (map) {
                if (map.isEmpty())
                    rmv = true;
                else {
                    set = map.get(nodeId);

                    if (set == null) {
                        GridCommunicationMessageSet old = map.putIfAbsent(nodeId,
                            set = new GridCommunicationMessageSet(plc, msg.topic(),
                                nodeId, timeout, skipOnTimeout, msg));

                        assert old == null;

                        isNew = true;

                        // Put succeeded.
                        break;
                    }
                }
            }

            if (rmv)
                msgSetMap.remove(msg.topic(), map);
            else {
                assert set != null;
                assert !isNew;

                set.add(msg);

                break;
            }
        }

        if (ctx.discovery().node(nodeId) == null) {
            if (log.isDebugEnabled())
                log.debug("Message is ignored as sender has left the grid: " + msg);

            assert map != null;

            boolean rmv;

            synchronized (map) {
                map.remove(nodeId);

                rmv = map.isEmpty();
            }

            if (rmv)
                msgSetMap.remove(msg.topic(), map);

            return;
        }

        if (isNew && set.endTime() != Long.MAX_VALUE)
            ctx.timeout().addTimeoutObject(set);

        GridMessageListener lsnr = lsnrMap.get(msg.topic());

        if (lsnr != null)
            unwindMessageSet(set, lsnr, false);
        else if (closedTopics.contains(msg.topic())) {
            if (log.isDebugEnabled())
                log.debug("Message is ignored as it came for the closed topic: " + msg);

            assert map != null;

            msgSetMap.remove(msg.topic(), map);
        }
        else if (log.isDebugEnabled()) {
            // Note that we simply keep messages if listener is not
            // registered yet, until one will be registered.
            log.debug("Received message for unknown listener (messages will be kept until a " +
                "listener is registered): " + msg);
        }
    }

    /**
     * @param msgSet Message set to unwind.
     * @param lsnr Listener to notify.
     * @param force Whether to force unwind and drop missing
     *      ordered messages that are not received yet.
     */
    @SuppressWarnings({"SynchronizationOnLocalVariableOrMethodParameter", "deprecation"})
    private void unwindMessageSet(GridCommunicationMessageSet msgSet, GridMessageListener lsnr, boolean force) {
        // Loop until message set is empty or
        // another thread owns the reservation.
        while (true) {
            if (msgSet.reserve()) {
                try {
                    Collection<GridIoMessage> orderedMsgs = msgSet.unwind(force);

                    if (!orderedMsgs.isEmpty()) {
                        for (GridIoMessage msg : orderedMsgs) {
                            Object obj = msg.message();

                            assert obj != null;

                            lsnr.onMessage(msgSet.nodeId(), obj);
                        }
                    }
                    else if (log.isDebugEnabled())
                        log.debug("No messages were unwound: " + msgSet);
                }
                finally {
                    msgSet.release();
                }

                // Check outside of reservation block.
                if (!msgSet.changed()) {
                    if (log.isDebugEnabled())
                        log.debug("Message set has not been changed: " + msgSet);

                    break;
                }
            }
            else {
                if (log.isDebugEnabled())
                    log.debug("Another thread owns reservation: " + msgSet);

                return;
            }
        }
    }

    /**
     * @param node Destination node.
     * @param topic Topic to send the message to.
     * @param topicOrd GridTopic enumeration ordinal.
     * @param msg Message to send.
     * @param plc Type of processing.
     * @param msgId Message ID.
     * @param timeout Timeout.
     * @param skipOnTimeout Whether message can be skipped on timeout.
     * @throws GridException Thrown in case of any errors.
     */
    private void send(GridNode node, Object topic, int topicOrd, GridTcpCommunicationMessageAdapter msg,
        GridIoPolicy plc, long msgId, long timeout, boolean skipOnTimeout) throws GridException {
        assert node != null;
        assert topic != null;
        assert msg != null;
        assert plc != null;

        GridIoMessage ioMsg = new GridIoMessage(plc, topic, topicOrd, msg, msgId, timeout, skipOnTimeout);

        if (locNodeId.equals(node.id())) {
            assert plc != P2P_POOL;

            GridCommunicationListener commLsnr = this.commLsnr;

            if (commLsnr == null)
                throw new GridException("Trying to send message when grid is not fully started.");

            if (msgId > 0)
                processOrderedMessage0(ioMsg, plc, locNodeId);
            else
                processRegularMessage0(ioMsg, locNodeId);
        }
        else {
            if (topicOrd < 0)
                ioMsg.topicBytes(marsh.marshal(topic));

            try {
                getSpi().sendMessage(node, ioMsg);
            }
            catch (GridSpiException e) {
                throw new GridException("Failed to send message (node may have left the grid or " +
                    "TCP connection cannot be established due to firewall issues) " +
                    "[node=" + node + ", topic=" + topic +
                    ", msg=" + msg + ", policy=" + plc + ']', e);
            }
        }
    }

    /**
     * @param nodeId Id of destination node.
     * @param topic Topic to send the message to.
     * @param msg Message to send.
     * @param plc Type of processing.
     * @throws GridException Thrown in case of any errors.
     */
    public void send(UUID nodeId, Object topic, GridTcpCommunicationMessageAdapter msg, GridIoPolicy plc)
        throws GridException {
        GridNode node = ctx.discovery().node(nodeId);

        if (node == null)
            throw new GridException("Failed to send message to node (has node left grid?): " + nodeId);

        send(node, topic, msg, plc);
    }

    /**
     * @param nodeId Id of destination node.
     * @param topic Topic to send the message to.
     * @param msg Message to send.
     * @param plc Type of processing.
     * @throws GridException Thrown in case of any errors.
     */
    @SuppressWarnings("TypeMayBeWeakened")
    public void send(UUID nodeId, GridTopic topic, GridTcpCommunicationMessageAdapter msg, GridIoPolicy plc)
        throws GridException {
        GridNode node = ctx.discovery().node(nodeId);

        if (node == null)
            throw new GridException("Failed to send message to node (has node left grid?): " + nodeId);

        send(node, topic, topic.ordinal(), msg, plc, -1, 0, false);
    }

    /**
     * @param node Destination node.
     * @param topic Topic to send the message to.
     * @param msg Message to send.
     * @param plc Type of processing.
     * @throws GridException Thrown in case of any errors.
     */
    public void send(GridNode node, Object topic, GridTcpCommunicationMessageAdapter msg, GridIoPolicy plc)
        throws GridException {
        send(node, topic, -1, msg, plc, -1, 0, false);
    }

    /**
     * @param node Destination node.
     * @param topic Topic to send the message to.
     * @param msg Message to send.
     * @param plc Type of processing.
     * @throws GridException Thrown in case of any errors.
     */
    public void send(GridNode node, GridTopic topic, GridTcpCommunicationMessageAdapter msg, GridIoPolicy plc)
        throws GridException {
        send(node, topic, topic.ordinal(), msg, plc, -1, 0, false);
    }

    /**
     * @param topic Message topic.
     * @param nodeId Node ID.
     * @return Next ordered message ID.
     */
    public long nextMessageId(Object topic, UUID nodeId) {
        ConcurrentMap<UUID, AtomicLong> map = msgIdMap.get(topic);

        if (map == null) {
            ConcurrentMap<UUID, AtomicLong> lastMap = msgIdMap.putIfAbsent(topic,
                map = new ConcurrentHashMap8<>());

            if (lastMap != null)
                map = lastMap;
        }

        AtomicLong msgId = map.get(nodeId);

        if (msgId == null) {
            AtomicLong lastMsgId = map.putIfAbsent(nodeId, msgId = new AtomicLong(0));

            if (lastMsgId != null)
                msgId = lastMsgId;
        }

        long id = msgId.incrementAndGet();

        if (log.isDebugEnabled())
            log.debug("Got next message ID [topic=" + topic + ", nodeId=" + nodeId + ", id=" + id + ']');

        return id;
    }

    /**
     * @param topic Message topic.
     */
    public void removeMessageId(Object topic) {
        if (log.isDebugEnabled())
            log.debug("Remove message ID for topic: " + topic);

        msgIdMap.remove(topic);
    }

    /**
     * @param node Destination node.
     * @param topic Topic to send the message to.
     * @param msgId Ordered message ID.
     * @param msg Message to send.
     * @param plc Type of processing.
     * @param timeout Timeout to keep a message on receiving queue.
     * @param skipOnTimeout Whether message can be skipped on timeout.
     * @throws GridException Thrown in case of any errors.
     */
    public void sendOrderedMessage(GridNode node, Object topic, long msgId, GridTcpCommunicationMessageAdapter msg,
        GridIoPolicy plc, long timeout, boolean skipOnTimeout) throws GridException {
        assert timeout > 0 || skipOnTimeout;

        send(node, topic, (byte)-1, msg, plc, msgId, timeout, skipOnTimeout);
    }

    /**
     * @param nodeId Destination node.
     * @param topic Topic to send the message to.
     * @param msgId Ordered message ID.
     * @param msg Message to send.
     * @param plc Type of processing.
     * @param timeout Timeout to keep a message on receiving queue.
     * @param skipOnTimeout Whether message can be skipped on timeout.
     * @throws GridException Thrown in case of any errors.
     */
    public void sendOrderedMessage(UUID nodeId, Object topic, long msgId, GridTcpCommunicationMessageAdapter msg,
        GridIoPolicy plc, long timeout, boolean skipOnTimeout) throws GridException {
        assert timeout > 0 || skipOnTimeout;

        GridNode node = ctx.discovery().node(nodeId);

        if (node == null)
            throw new GridException("Failed to send message to node (has node left grid?): " + nodeId);

        send(node, topic, (byte)-1, msg, plc, msgId, timeout, skipOnTimeout);
    }

    /**
     * @param nodes Destination nodes.
     * @param topic Topic to send the message to.
     * @param msgId Ordered message ID.
     * @param msg Message to send.
     * @param plc Type of processing.
     * @param timeout Timeout to keep a message on receiving queue.
     * @param skipOnTimeout Whether message can be skipped on timeout.
     * @throws GridException Thrown in case of any errors.
     */
    public void sendOrderedMessage(Collection<? extends GridNode> nodes, Object topic, long msgId,
        GridTcpCommunicationMessageAdapter msg, GridIoPolicy plc, long timeout, boolean skipOnTimeout)
        throws GridException {
        assert timeout > 0 || skipOnTimeout;

        send(nodes, topic, -1, msg, plc, msgId, timeout, skipOnTimeout);
    }

    /**
     * @param nodes Destination nodes.
     * @param topic Topic to send the message to.
     * @param msg Message to send.
     * @param plc Type of processing.
     * @throws GridException Thrown in case of any errors.
     */
    public void send(Collection<? extends GridNode> nodes, Object topic, GridTcpCommunicationMessageAdapter msg,
        GridIoPolicy plc) throws GridException {
        send(nodes, topic, -1, msg, plc, -1, 0, false);
    }

    /**
     * @param nodes Destination nodes.
     * @param topic Topic to send the message to.
     * @param msg Message to send.
     * @param plc Type of processing.
     * @throws GridException Thrown in case of any errors.
     */
    public void send(Collection<? extends GridNode> nodes, GridTopic topic, GridTcpCommunicationMessageAdapter msg,
        GridIoPolicy plc) throws GridException {
        send(nodes, topic, topic.ordinal(), msg, plc, -1, 0, false);
    }

    /**
     * Sends a peer deployable user message.
     *
     * @param nodes Destination nodes.
     * @param msg Message to send.
     * @throws GridException Thrown in case of any errors.
     */
    public void sendUserMessage(Collection<? extends GridNode> nodes, Object msg) throws GridException {
        sendUserMessage(nodes, msg, null, false, 0);
    }

    /**
     * Sends a peer deployable user message.
     *
     * @param nodes Destination nodes.
     * @param msg Message to send.
     * @param topic Message topic to use.
     * @param ordered Is message ordered?
     * @param timeout Message timeout in milliseconds for ordered messages.
     * @throws GridException Thrown in case of any errors.
     */
    @SuppressWarnings("ConstantConditions")
    public void sendUserMessage(Collection<? extends GridNode> nodes, Object msg,
        @Nullable Object topic, boolean ordered, long timeout) throws GridException {
        boolean loc = nodes.size() == 1 && F.first(nodes).id().equals(locNodeId);

        byte[] serMsg = null;
        byte[] serTopic = null;

        if (!loc) {
            serMsg = marsh.marshal(msg);

            if (topic != null)
                serTopic = marsh.marshal(topic);
        }

        GridDeployment dep = null;

        String depClsName = null;

        if (ctx.config().isPeerClassLoadingEnabled()) {
            Class<?> cls0 = U.detectClass(msg);

            if (U.isJdk(cls0) && topic != null)
                cls0 = U.detectClass(topic);

            dep = ctx.deploy().deploy(cls0, U.detectClassLoader(cls0));

            if (dep == null)
                throw new GridDeploymentException("Failed to deploy user message: " + msg);

            depClsName = cls0.getName();
        }

        GridTcpCommunicationMessageAdapter ioMsg = new GridIoUserMessage(
            msg,
            serMsg,
            depClsName,
            topic,
            serTopic,
            dep != null ? dep.classLoaderId() : null,
            dep != null ? dep.deployMode() : null,
            dep != null ? dep.userVersion() : null,
            dep != null ? dep.participants() : null);

        if (ordered) {
            long msgId = nextMessageId(TOPIC_COMM_USER, locNodeId);

            sendOrderedMessage(nodes, TOPIC_COMM_USER, msgId, ioMsg, PUBLIC_POOL, timeout, true);
        }
        else if (loc)
            send(F.first(nodes), TOPIC_COMM_USER, ioMsg, PUBLIC_POOL);
        else {
            GridNode locNode = F.find(nodes, null, F.localNode(locNodeId));

            Collection<? extends GridNode> rmtNodes = F.view(nodes, F.remoteNodes(locNodeId));

            if (locNode != null)
                send(locNode, TOPIC_COMM_USER, ioMsg, PUBLIC_POOL);

            if (!rmtNodes.isEmpty())
                send(rmtNodes, TOPIC_COMM_USER, ioMsg, PUBLIC_POOL);
        }
    }

    /**
     * @param topic Topic to subscribe to.
     * @param p Message predicate.
     */
    public void addUserMessageListener(@Nullable final Object topic, @Nullable final GridBiPredicate<UUID, ?> p) {
        if (p != null) {
            try {
                addMessageListener(TOPIC_COMM_USER,
                    new GridUserMessageListener(topic, (GridBiPredicate<UUID, Object>)p));
            }
            catch (GridException e) {
                throw new GridRuntimeException(e);
            }
        }
    }

    /**
     * @param topic Topic to unsubscribe from.
     * @param p Message predicate.
     */
    public void removeUserMessageListener(@Nullable Object topic, GridBiPredicate<UUID, ?> p) {
        try {
            removeMessageListener(TOPIC_COMM_USER,
                new GridUserMessageListener(topic, (GridBiPredicate<UUID, Object>)p));
        }
        catch (GridException e) {
            throw new GridRuntimeException(e);
        }
    }

    /**
     * @param nodes Destination nodes.
     * @param topic Topic to send the message to.
     * @param topicOrd Topic ordinal value.
     * @param msg Message to send.
     * @param plc Type of processing.
     * @param msgId Message ID (for ordered messages) or -1 (for unordered messages).
     * @param timeout Message timeout.
     * @param skipOnTimeout Whether message can be skipped in timeout.
     * @throws GridException Thrown in case of any errors.
     */
    private void send(Collection<? extends GridNode> nodes, Object topic, int topicOrd,
        GridTcpCommunicationMessageAdapter msg, GridIoPolicy plc, long msgId, long timeout, boolean skipOnTimeout)
        throws GridException {
        assert nodes != null;
        assert topic != null;
        assert msg != null;
        assert plc != null;

        if (msgId < 0)
            assert F.find(nodes, null, F.localNode(locNodeId)) == null :
                "Internal GridGain code should never call the method with local node in a node list.";

        try {
            // Small optimization, as communication SPIs may have lighter implementation for sending
            // messages to one node vs. many.
            if (!nodes.isEmpty()) {
                boolean first = true;

                for (GridNode node : nodes) {
                    GridTcpCommunicationMessageAdapter msg0 = first ? msg : msg.clone();

                    first = false;

                    send(node, topic, topicOrd, msg0, plc, msgId, timeout, skipOnTimeout);
                }
            }
            else if (log.isDebugEnabled())
                log.debug("Failed to send message to empty nodes collection [topic=" + topic + ", msg=" +
                    msg + ", policy=" + plc + ']');
        }
        catch (GridSpiException e) {
            throw new GridException("Failed to send message (nodes may have left the grid or " +
                "TCP connection cannot be established due to firewall issues) " +
                "[nodes=" + nodes + ", topic=" + topic +
                ", msg=" + msg + ", policy=" + plc + ']', e);
        }
    }

    /**
     * @param topic Listener's topic.
     * @param lsnr Listener to add.
     */
    @SuppressWarnings({"TypeMayBeWeakened", "deprecation"})
    public void addMessageListener(GridTopic topic, GridMessageListener lsnr) {
        addMessageListener((Object)topic, lsnr);
    }

    /**
     * @param lsnr Listener to add.
     */
    public void addDisconnectListener(GridDisconnectListener lsnr) {
        disconnectLsnrs.add(lsnr);
    }

    /**
     * @param topic Listener's topic.
     * @param lsnr Listener to add.
     */
    @SuppressWarnings({"deprecation", "SynchronizationOnLocalVariableOrMethodParameter"})
    public void addMessageListener(Object topic, final GridMessageListener lsnr) {
        assert lsnr != null;
        assert topic != null;

        // Make sure that new topic is not in the list of closed topics.
        closedTopics.remove(topic);

        GridMessageListener lsnrs;

        for (;;) {
            lsnrs = lsnrMap.putIfAbsent(topic, lsnr);

            if (lsnrs == null) {
                lsnrs = lsnr;

                break;
            }

            assert lsnrs != null;

            if (!(lsnrs instanceof ArrayListener)) { // We are putting the second listener, creating array.
                GridMessageListener arrLsnr = new ArrayListener(lsnrs, lsnr);

                if (lsnrMap.replace(topic, lsnrs, arrLsnr)) {
                    lsnrs = arrLsnr;

                    break;
                }
            }
            else {
                if (((ArrayListener)lsnrs).add(lsnr))
                    break;

                // Add operation failed because array is already empty and is about to be removed, helping and retrying.
                lsnrMap.remove(topic, lsnrs);
            }
        }

        Map<UUID, GridCommunicationMessageSet> map = msgSetMap.get(topic);

        Collection<GridCommunicationMessageSet> msgSets = map != null ? map.values() : null;

        if (msgSets != null) {
            final GridMessageListener lsnrs0 = lsnrs;

            boolean success = true;

            try {
                for (final GridCommunicationMessageSet msgSet : msgSets) {
                    success = false;

                    workersCnt.increment();

                    pool(msgSet.policy()).execute(new GridWorker(ctx.gridName(), "msg-worker", log) {
                        @Override protected void body() {
                            try {
                                unwindMessageSet(msgSet, lsnrs0, false);
                            }
                            finally {
                                workersCnt.decrement();
                            }
                        }
                    });

                    success = true;
                }
            }
            catch (RejectedExecutionException e) {
                U.error(log, "Failed to process delayed message due to execution rejection. Increase the upper bound " +
                    "on executor service provided in 'GridConfiguration.getExecutorService()'). Will attempt to " +
                    "process message in the listener thread instead.", e);

                for (GridCommunicationMessageSet msgSet : msgSets)
                    unwindMessageSet(msgSet, lsnr, false);
            }
            finally {
                // Decrement for last runnable submission of which failed.
                if (!success)
                    workersCnt.decrement();
            }
        }
    }

    /**
     * @param topic Message topic.
     * @return Whether or not listener was indeed removed.
     */
    public boolean removeMessageListener(GridTopic topic) {
        return removeMessageListener((Object)topic);
    }

    /**
     * @param topic Message topic.
     * @return Whether or not listener was indeed removed.
     */
    public boolean removeMessageListener(Object topic) {
        return removeMessageListener(topic, null);
    }

    /**
     * @param topic Listener's topic.
     * @param lsnr Listener to remove.
     * @return Whether or not the lsnr was removed.
     */
    @SuppressWarnings("deprecation")
    public boolean removeMessageListener(GridTopic topic, @Nullable GridMessageListener lsnr) {
        return removeMessageListener((Object)topic, lsnr);
    }

    /**
     * @param topic Listener's topic.
     * @param lsnr Listener to remove.
     * @return Whether or not the lsnr was removed.
     */
    @SuppressWarnings({"deprecation", "SynchronizationOnLocalVariableOrMethodParameter"})
    public boolean removeMessageListener(Object topic, @Nullable final GridMessageListener lsnr) {
        assert topic != null;

        boolean rmv = true;

        Collection<GridCommunicationMessageSet> msgSets = null;

        // If listener is null, then remove all listeners.
        if (lsnr == null) {
            closedTopics.add(topic);

            rmv = lsnrMap.remove(topic) != null;

            Map<UUID, GridCommunicationMessageSet> map = msgSetMap.remove(topic);

            if (map != null)
                msgSets = map.values();
        }
        else {
            for (;;) {
                GridMessageListener lsnrs = lsnrMap.get(topic);

                // If removing listener before subscription happened.
                if (lsnrs == null) {
                    closedTopics.add(topic);

                    Map<UUID, GridCommunicationMessageSet> map = msgSetMap.remove(topic);

                    if (map != null)
                        msgSets = map.values();

                    rmv = false;

                    break;
                }
                else {
                    boolean empty = false;

                    if (!(lsnrs instanceof ArrayListener)) {
                        if (lsnrs.equals(lsnr)) {
                            if (!lsnrMap.remove(topic, lsnrs))
                                continue; // Retry because it can be packed to array listener.

                            empty = true;
                        }
                        else
                            rmv = false;
                    }
                    else {
                        ArrayListener arrLsnr = (ArrayListener)lsnrs;

                        if (arrLsnr.remove(lsnr))
                            empty = arrLsnr.isEmpty();
                        else
                            // Listener was not found.
                            rmv = false;

                        if (empty)
                            lsnrMap.remove(topic, lsnrs);
                    }

                    // If removing last subscribed listener.
                    if (empty) {
                        closedTopics.add(topic);

                        Map<UUID, GridCommunicationMessageSet> map = msgSetMap.remove(topic);

                        if (map != null)
                            msgSets = map.values();
                    }

                    break;
                }
            }
        }

        if (msgSets != null)
            for (GridCommunicationMessageSet msgSet : msgSets)
                ctx.timeout().removeTimeoutObject(msgSet);

        if (rmv && log.isDebugEnabled())
            log.debug("Removed message listener [topic=" + topic + ", lsnr=" + lsnr + ']');

        return rmv;
    }

    /**
     * Gets sent messages count.
     *
     * @return Sent messages count.
     */
    public int getSentMessagesCount() {
        return getSpi().getSentMessagesCount();
    }

    /**
     * Gets sent bytes count.
     *
     * @return Sent bytes count.
     */
    public long getSentBytesCount() {
        return getSpi().getSentBytesCount();
    }

    /**
     * Gets received messages count.
     *
     * @return Received messages count.
     */
    public int getReceivedMessagesCount() {
        return getSpi().getReceivedMessagesCount();
    }

    /**
     * Gets received bytes count.
     *
     * @return Received bytes count.
     */
    public long getReceivedBytesCount() {
        return getSpi().getReceivedBytesCount();
    }

    /**
     * Gets outbound messages queue size.
     *
     * @return Outbound messages queue size.
     */
    public int getOutboundMessagesQueueSize() {
        return getSpi().getOutboundMessagesQueueSize();
    }

    /** {@inheritDoc} */
    @Override public void printMemoryStats() {
        X.println(">>>");
        X.println(">>> IO manager memory stats [grid=" + ctx.gridName() + ']');
        X.println(">>>  lsnrMapSize: " + lsnrMap.size());
        X.println(">>>  msgSetMapSize: " + msgSetMap.size());
        X.println(">>>  msgIdMapSize: " + msgIdMap.size());
        X.println(">>>  closedTopicsSize: " + closedTopics.sizex());
        X.println(">>>  discoWaitMapSize: " + waitMap.size());
    }

    /**
     * Linked chain of listeners.
     */
    private static class ArrayListener implements GridMessageListener {
        /** */
        private volatile GridMessageListener[] arr;

        /**
         * @param arr Array of listeners.
         */
        ArrayListener(GridMessageListener... arr) {
            this.arr = arr;
        }

        /**
         * Passes message to the whole chain.
         *
         * @param nodeId Node ID.
         * @param msg Message.
         */
        @Override public void onMessage(UUID nodeId, Object msg) {
            GridMessageListener[] arr0 = arr;

            if (arr0 == null)
                return;

            for (GridMessageListener l : arr0)
                l.onMessage(nodeId, msg);
        }

        /**
         * @return {@code true} If this instance is empty.
         */
        boolean isEmpty() {
            return arr == null;
        }

        /**
         * @param l Listener.
         * @return {@code true} If listener was removed.
         */
        synchronized boolean remove(GridMessageListener l) {
            GridMessageListener[] arr0 = arr;

            if (arr0 == null)
                return false;

            if (arr0.length == 1) {
                if (!arr0[0].equals(l))
                    return false;

                arr = null;

                return true;
            }

            for (int i = 0; i < arr0.length; i++) {
                if (arr0[i].equals(l)) {
                    int newLen = arr0.length - 1;

                    if (i == newLen) // Remove last.
                        arr = Arrays.copyOf(arr0, newLen);
                    else {
                        GridMessageListener[] arr1 = new GridMessageListener[newLen];

                        if (i != 0) // Not remove first.
                            System.arraycopy(arr0, 0, arr1, 0, i);

                        System.arraycopy(arr0, i + 1, arr1, i, newLen - i);

                        arr = arr1;
                    }

                    return true;
                }
            }

            return false;
        }

        /**
         * @param l Listener.
         * @return {@code true} if listener was added. Add can fail if this instance is empty and is about to be removed
         *         from map.
         */
        synchronized boolean add(GridMessageListener l) {
            GridMessageListener[] arr0 = arr;

            if (arr0 == null)
                return false;

            int oldLen = arr0.length;

            arr0 = Arrays.copyOf(arr0, oldLen + 1);

            arr0[oldLen] = l;

            arr = arr0;

            return true;
        }
    }

    /**
     * This class represents a message listener wrapper that knows about peer deployment.
     */
    private class GridUserMessageListener implements GridMessageListener {
        /** Predicate listeners. */
        private final GridBiPredicate<UUID, Object> predLsnr;

        /** User message topic. */
        private final Object topic;

        /**
         * @param topic User topic.
         * @param predLsnr Predicate listener.
         * @throws GridException If failed to inject resources to predicates.
         */
        GridUserMessageListener(@Nullable Object topic, @Nullable GridBiPredicate<UUID, Object> predLsnr)
            throws GridException {
            this.topic = topic;
            this.predLsnr = predLsnr;

            if (predLsnr != null)
                ctx.resource().injectGeneric(predLsnr);
        }

        /** {@inheritDoc} */
        @SuppressWarnings({"SynchronizationOnLocalVariableOrMethodParameter", "ConstantConditions",
            "OverlyStrongTypeCast"})
        @Override public void onMessage(UUID nodeId, Object msg) {
            if (!(msg instanceof GridIoUserMessage)) {
                U.error(log, "Received unknown message (potentially fatal problem): " + msg);

                return;
            }

            GridIoUserMessage ioMsg = (GridIoUserMessage)msg;

            GridNode node = ctx.discovery().node(nodeId);

            if (node == null) {
                U.warn(log, "Failed to resolve sender node (did the node left grid?): " + nodeId);

                return;
            }

            Object msgBody = ioMsg.body();

            assert msgBody != null || ioMsg.bodyBytes() != null;

            try {
                byte[] msgTopicBytes = ioMsg.topicBytes();

                Object msgTopic = ioMsg.topic();

                GridDeployment dep = ioMsg.deployment();

                if (dep == null && ctx.config().isPeerClassLoadingEnabled() &&
                    ioMsg.deploymentClassName() != null) {
                    dep = ctx.deploy().getGlobalDeployment(
                        ioMsg.deploymentMode(),
                        ioMsg.deploymentClassName(),
                        ioMsg.deploymentClassName(),
                        ioMsg.userVersion(),
                        nodeId,
                        ioMsg.classLoaderId(),
                        ioMsg.loaderParticipants(),
                        null);

                    if (dep == null)
                        throw new GridDeploymentException(
                            "Failed to obtain deployment information for user message. " +
                            "If you are using custom message or topic class, try implementing " +
                            "GridPeerDeployAware interface. [msg=" + ioMsg + ']');

                    ioMsg.deployment(dep); // Cache deployment.
                }

                // Unmarshall message topic if needed.
                if (msgTopic == null && msgTopicBytes != null) {
                    msgTopic = marsh.unmarshal(msgTopicBytes, dep != null ? dep.classLoader() : null);

                    ioMsg.topic(msgTopic); // Save topic to avoid future unmarshallings.
                }

                if (!F.eq(topic, msgTopic))
                    return;

                if (msgBody == null) {
                    msgBody = marsh.unmarshal(ioMsg.bodyBytes(), dep != null ? dep.classLoader() : null);

                    ioMsg.body(msgBody); // Save body to avoid future unmarshallings.
                }

                // Resource injection.
                if (dep != null)
                    ctx.resource().inject(dep, dep.deployedClass(ioMsg.deploymentClassName()), msgBody);
            }
            catch (GridException e) {
                U.error(log, "Failed to unmarshal user message [node=" + nodeId + ", message=" +
                    msg + ']', e);
            }

            if (msgBody != null) {
                if (predLsnr != null) {
                    if (!predLsnr.apply(nodeId, msgBody))
                        removeMessageListener(TOPIC_COMM_USER, this);
                }
            }
        }

        /** {@inheritDoc} */
        @Override public boolean equals(Object o) {
            if (this == o)
                return true;

            if (o == null || getClass() != o.getClass())
                return false;

            GridUserMessageListener l = (GridUserMessageListener)o;

            return F.eq(predLsnr, l.predLsnr) && F.eq(topic, l.topic);
        }

        /** {@inheritDoc} */
        @Override public int hashCode() {
            int res = predLsnr != null ? predLsnr.hashCode() : 0;

            res = 31 * res + (topic != null ? topic.hashCode() : 0);

            return res;
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(GridUserMessageListener.class, this);
        }
    }

    /**
     * Ordered communication message set.
     */
    private class GridCommunicationMessageSet implements GridTimeoutObject {
        /** */
        private final UUID nodeId;

        /** */
        private long endTime;

        /** */
        private final GridUuid timeoutId;

        /** */
        private final Object topic;

        /** */
        private final GridIoPolicy plc;

        /** */
        @GridToStringInclude
        private final List<GridBiTuple<GridIoMessage, Long>> msgs = new ArrayList<>();

        /** */
        private long nextMsgId = 1;

        /** */
        private final AtomicBoolean reserved = new AtomicBoolean();

        /** */
        private final long timeout;

        /** */
        private final boolean skipOnTimeout;

        /** */
        private long lastTs;

        /** */
        private volatile boolean changed;

        /**
         * @param plc Communication policy.
         * @param topic Communication topic.
         * @param nodeId Node ID.
         * @param timeout Timeout.
         * @param skipOnTimeout Whether message can be skipped on timeout.
         * @param msg Message to add immediately.
         */
        GridCommunicationMessageSet(GridIoPolicy plc, Object topic, UUID nodeId, long timeout, boolean skipOnTimeout,
            GridIoMessage msg) {
            assert nodeId != null;
            assert topic != null;
            assert plc != null;
            assert msg != null;

            this.plc = plc;
            this.nodeId = nodeId;
            this.topic = topic;
            this.timeout = timeout == 0 ? ctx.config().getNetworkTimeout() : timeout;
            this.skipOnTimeout = skipOnTimeout;

            endTime = endTime(timeout);

            timeoutId = GridUuid.randomUuid();

            lastTs = U.currentTimeMillis();

            msgs.add(F.t(msg, lastTs));
        }

        /** {@inheritDoc} */
        @Override public GridUuid timeoutId() {
            return timeoutId;
        }

        /** {@inheritDoc} */
        @Override public long endTime() {
            return endTime;
        }

        /** {@inheritDoc} */
        @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
        @Override public void onTimeout() {
            GridMessageListener lsnr = lsnrMap.get(topic);

            if (lsnr != null) {
                long delta = 0;

                if (skipOnTimeout) {
                    while (true) {
                        delta = 0;

                        boolean unwind = false;

                        synchronized (this) {
                            if (!msgs.isEmpty()) {
                                delta = U.currentTimeMillis() - lastTs;

                                if (delta >= timeout)
                                    unwind = true;
                            }
                        }

                        if (unwind)
                            unwindMessageSet(this, lsnr, true);
                        else
                            break;
                    }
                }

                // Someone is still listening to messages, so delay set removal.
                endTime = endTime(timeout - delta);

                ctx.timeout().addTimeoutObject(this);

                return;
            }

            if (log.isDebugEnabled())
                log.debug("Removing message set due to timeout: " + this);

            ConcurrentMap<UUID, GridCommunicationMessageSet> map = msgSetMap.get(topic);

            if (map != null) {
                boolean rmv;

                synchronized (map) {
                    rmv = map.remove(nodeId, this) && map.isEmpty();
                }

                if (rmv)
                    msgSetMap.remove(topic, map);
            }
        }

        /**
         * @return ID of node that sent the messages in the set.
         */
        UUID nodeId() {
            return nodeId;
        }

        /**
         * @return Communication policy.
         */
        GridIoPolicy policy() {
            return plc;
        }

        /**
         * @return Message topic.
         */
        Object topic() {
            return topic;
        }

        /**
         * @return {@code True} if successful.
         */
        boolean reserve() {
            return reserved.compareAndSet(false, true);
        }

        /**
         * Releases reservation.
         */
        void release() {
            assert reserved.get() : "Message set was not reserved: " + this;

            reserved.set(false);
        }

        /**
         * @param force Whether to force unwind and drop missing
         *      ordered messages that are not received yet.
         * @return Session request.
         */
        synchronized Collection<GridIoMessage> unwind(boolean force) {
            assert reserved.get();

            changed = false;

            if (msgs.isEmpty())
                return Collections.emptyList();

            if (msgs.size() == 1) {
                GridBiTuple<GridIoMessage, Long> t = msgs.get(0);

                GridIoMessage msg = t.get1();

                if (force || msg.messageId() == nextMsgId) {
                    if (msg.messageId() != nextMsgId) {
                        for (long skipped = nextMsgId; skipped < msg.messageId(); skipped++) {
                            U.warn(log, "Skipped ordered message due to timeout, consider increasing " +
                                "networkTimeout configuration property [topic=" + topic + ", msgId=" +
                                skipped + ", timeout=" + timeout + ']');
                        }
                    }

                    nextMsgId = msg.messageId() + 1;

                    lastTs = t.get2();

                    msgs.clear();

                    return Collections.singleton(msg);
                }

                return Collections.emptyList();
            }

            // Sort before unwinding.
            Collections.sort(msgs, MSG_CMP);

            Collection<GridIoMessage> orderedMsgs = new LinkedList<>();

            for (Iterator<GridBiTuple<GridIoMessage, Long>> iter = msgs.iterator(); iter.hasNext();) {
                GridBiTuple<GridIoMessage, Long> t = iter.next();

                GridIoMessage msg = t.get1();

                if (force || msg.messageId() == nextMsgId) {
                    if (msg.messageId() != nextMsgId) {
                        for (long skipped = nextMsgId; skipped < msg.messageId(); skipped++) {
                            U.warn(log, "Skipped ordered message due to timeout, consider increasing " +
                                "networkTimeout configuration property [topic=" + topic + ", msgId=" +
                                skipped + ", timeout=" + timeout + ']');
                        }
                    }

                    force = false;

                    orderedMsgs.add(msg);

                    nextMsgId = msg.messageId() + 1;

                    lastTs = t.get2();

                    iter.remove();
                }
                else
                    break;
            }

            return orderedMsgs;
        }

        /**
         * @param msg Message to add.
         */
        synchronized void add(GridIoMessage msg) {
            if (msg.messageId() >= nextMsgId) {
                msgs.add(F.t(msg, U.currentTimeMillis()));

                changed = true;
            }
            else {
                U.warn(log, "Received previously skipped ordered message (will be dropped) [topic=" + topic +
                    ", msgId=" + msg.messageId() + ", timeout=" + timeout + ']');
            }
        }

        /**
         * @return {@code True} if set has messages to unwind.
         */
        boolean changed() {
            return changed;
        }

        /**
         * Calculates end time with overflow check.
         *
         * @param timeout Timeout in milliseconds.
         * @return End time in milliseconds.
         */
        private long endTime(long timeout) {
            long endTime = U.currentTimeMillis() + timeout;

            // Account for overflow.
            if (endTime < 0)
                endTime = Long.MAX_VALUE;

            return endTime;
        }

        /** {@inheritDoc} */
        @Override public synchronized String toString() {
            return S.toString(GridCommunicationMessageSet.class, this);
        }
    }

    /**
     *
     */
    private static class ConcurrentHashMap0<K, V> extends ConcurrentHashMap8<K, V> {
        /** */
        private static final long serialVersionUID = 0L;

        /** */
        private int hash;

        /**
         * @param o Object to be compared for equality with this map.
         * @return {@code True} only for {@code this}.
         */
        @Override public boolean equals(Object o) {
            return o == this;
        }

        /**
         * @return Identity hash code.
         */
        @Override public int hashCode() {
            if (hash == 0) {
                int hash0 = System.identityHashCode(this);

                hash = hash0 != 0 ? hash0 : -1;
            }

            return hash;
        }
    }

    /**
     *
     */
    private static class ConcurrentHashSet0<E> extends GridConcurrentHashSet<E> {
        /** */
        private static final long serialVersionUID = 0L;

        /** */
        private int hash;

        /**
         *
         */
        private ConcurrentHashSet0() {
            super(1, 1, 1);
        }

        /**
         * @param o Object to be compared for equality with this map.
         * @return {@code True} only for {@code this}.
         */
        @Override public boolean equals(Object o) {
            return o == this;
        }

        /**
         * @return Identity hash code.
         */
        @Override public int hashCode() {
            if (hash == 0) {
                int hash0 = System.identityHashCode(this);

                hash = hash0 != 0 ? hash0 : -1;
            }

            return hash;
        }
    }

    /**
     *
     */
    private static class DelayedMessage {
        /** */
        private final UUID nodeId;

        /** */
        private final GridIoMessage msg;

        /** */
        private final GridRunnable msgC;

        /** */
        private final long rcvTime = U.currentTimeMillis();

        /**
         * @param nodeId Node ID.
         * @param msg Message.
         * @param msgC Callback.
         */
        private DelayedMessage(UUID nodeId, GridIoMessage msg, GridRunnable msgC) {
            this.nodeId = nodeId;
            this.msg = msg;
            this.msgC = msgC;
        }

        /**
         * @return Receive time.
         */
        public long receiveTime() {
            return rcvTime;
        }

        /**
         * @return Message char.
         */
        public GridRunnable callback() {
            return msgC;
        }

        /**
         * @return Message.
         */
        public GridIoMessage message() {
            return msg;
        }

        /**
         * @return Node id.
         */
        public UUID nodeId() {
            return nodeId;
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(DelayedMessage.class, this, super.toString());
        }
    }
}
