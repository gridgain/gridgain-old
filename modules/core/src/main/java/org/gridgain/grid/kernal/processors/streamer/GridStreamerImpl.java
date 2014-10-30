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

package org.gridgain.grid.kernal.processors.streamer;

import org.gridgain.grid.*;
import org.gridgain.grid.events.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.kernal.managers.communication.*;
import org.gridgain.grid.kernal.managers.deployment.*;
import org.gridgain.grid.kernal.managers.eventstorage.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.streamer.*;
import org.gridgain.grid.streamer.router.*;
import org.gridgain.grid.thread.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.direct.*;
import org.gridgain.grid.util.future.*;
import org.gridgain.grid.util.lang.*;
import org.gridgain.grid.util.tostring.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.grid.util.worker.*;
import org.jdk8.backport.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import static org.gridgain.grid.events.GridEventType.*;
import static org.gridgain.grid.kernal.GridTopic.*;

/**
 *
 */
public class GridStreamerImpl implements GridStreamerEx, Externalizable {
    /** */
    private static final long serialVersionUID = 0L;

    /** Number of message send retries. */
    private static final int SEND_RETRY_COUNT = 3;

    /** Retry delay. */
    private static final int SEND_RETRY_DELAY = 1000;

    /** How many cancelled future IDs to keep in history. */
    private static final int CANCELLED_FUTS_HISTORY_SIZE = 4096;

    /** Log. */
    private GridLogger log;

    /** Context. */
    private GridKernalContext ctx;

    /** */
    private GridStreamerContext streamerCtx;

    /** Read-write lock. */
    private GridSpinReadWriteLock lock;

    /** Stopping flag. */
    private boolean stopping;

    /** Streamer configuration. */
    private GridStreamerConfiguration c;

    /** Name. */
    private String name;

    /** Stages. */
    @GridToStringInclude
    private Map<String, GridStreamerStageWrapper> stages;

    /** Windows. */
    @GridToStringInclude
    private Map<String, GridStreamerWindow> winMap;

    /** Default streamer window. */
    private GridStreamerWindow dfltWin;

    /** */
    private String firstStage;

    /** Router. */
    private GridStreamerEventRouter router;

    /** At least once. */
    private boolean atLeastOnce;

    /** Streamer metrics. */
    private volatile GridStreamerMetricsHolder streamerMetrics;

    /** Topic. */
    private Object topic;

    /** Stage execution futures. */
    private ConcurrentMap<GridUuid, GridStreamerStageExecutionFuture> stageFuts;

    /** Batch execution futures. */
    private ConcurrentMap<GridUuid, BatchExecutionFuture> batchFuts;

    /** Streamer executor service. */
    private ExecutorService execSvc;

    /** Will be set to true if executor service is created on start. */
    private boolean dfltExecSvc;

    /** Failure listeners. */
    private Collection<GridStreamerFailureListener> failureLsnrs = new ConcurrentLinkedQueue<>();

    /** Cancelled  */
    private Collection<GridUuid> cancelledFutIds =
        new GridBoundedConcurrentLinkedHashSet<>(CANCELLED_FUTS_HISTORY_SIZE);

    /** Load control semaphore. */
    private Semaphore sem;

    /** Deploy class. */
    private Class<?> depCls;

    /** Executor service capacity. */
    private int execSvcCap;

    /** Window lock. */
    private final GridSpinReadWriteLock winLock = new GridSpinReadWriteLock();

    /**
     * Empty constructor required by {@link Externalizable}.
     */
    public GridStreamerImpl() {
        // No-op.
    }

    /**
     * @param ctx Kernal context.
     * @param c Configuration.
     */
    public GridStreamerImpl(GridKernalContext ctx, GridStreamerConfiguration c) {
        this.ctx = ctx;

        log = ctx.log(GridStreamerImpl.class);

        atLeastOnce = c.isAtLeastOnce();
        name = c.getName();
        router = c.getRouter();
        this.c = c;

        if (atLeastOnce) {
            if (c.getMaximumConcurrentSessions() > 0)
                sem = new Semaphore(c.getMaximumConcurrentSessions());
        }

        topic = name == null ? TOPIC_STREAM : TOPIC_STREAM.topic(name);
        lock = new GridSpinReadWriteLock();
        stageFuts = new ConcurrentHashMap8<>();
        batchFuts = new ConcurrentHashMap8<>();

        streamerCtx = new GridStreamerContextImpl(ctx, c, this);
    }

    /**
     * @throws GridException If failed.
     */
    @SuppressWarnings("unchecked")
    public void start() throws GridException {
        if (log.isDebugEnabled())
            log.debug("Starting streamer: " + name);

        if (F.isEmpty(c.getStages()))
            throw new GridException("Streamer should have at least one stage configured " +
                "(fix configuration and restart): " + name);

        if (F.isEmpty(c.getWindows()))
            throw new GridException("Streamer should have at least one window configured " +
                "(fix configuration and restart): " + name);

        prepareResources();

        U.startLifecycleAware(lifecycleAwares());

        stages = new LinkedHashMap<>(c.getStages().size());

        int stageIdx = 0;

        GridStreamerStageWrapper prev = null;

        for (GridStreamerStage s : c.getStages()) {
            String sName = s.name();

            if (F.isEmpty(sName))
                throw new GridException("Streamer stage should have non-empty name [streamerName=" + name +
                    ", stage=" + s + ']');

            if (stages.containsKey(sName))
                throw new GridException("Streamer stages have duplicate names (all names should be unique) " +
                    "[streamerName=" + name + ", stage=" + s + ", stageName=" + sName + ']');

            if (firstStage == null)
                firstStage = sName;

            GridStreamerStageWrapper wrapper = new GridStreamerStageWrapper(s, stageIdx);

            stages.put(sName, wrapper);

            if (prev != null)
                prev.nextStageName(s.name());

            prev = wrapper;

            stageIdx++;
        }

        winMap = new LinkedHashMap<>();

        for (GridStreamerWindow w : c.getWindows()) {
            String wName = w.name();

            if (F.isEmpty(wName))
                throw new GridException("Streamer window should have non-empty name [streamerName=" + name +
                    ", window=" + w + ']');

            if (winMap.containsKey(wName))
                throw new GridException("Streamer windows have duplicate names (all names should be unique). " +
                    "If you use two or more windows of the same type you need to assign their names explicitly " +
                    "[streamer=" + name + ", windowName=" + wName + ']');

            winMap.put(wName, w);

            if (dfltWin == null)
                dfltWin = w;
        }

        execSvc = c.getExecutorService();

        if (execSvc == null) {
            execSvc = new GridThreadPoolExecutor(
                ctx.gridName(),
                Runtime.getRuntime().availableProcessors(),
                Runtime.getRuntime().availableProcessors(),
                0,
                new LinkedBlockingQueue<Runnable>());

            execSvcCap = Runtime.getRuntime().availableProcessors();
            dfltExecSvc = true;
        }
        else {
            if (execSvc instanceof ThreadPoolExecutor) {
                ThreadPoolExecutor exec = (ThreadPoolExecutor)execSvc;

                execSvcCap = exec.getMaximumPoolSize();
            }
            else
                execSvcCap = -1;
        }

        resetMetrics();

        if (router == null)
            router = new GridStreamerLocalEventRouter();

        ctx.io().addMessageListener(topic, new GridMessageListener() {
            @Override public void onMessage(UUID nodeId, Object msg) {
                if (log.isDebugEnabled())
                    log.debug("Received message [nodeId=" + nodeId + ", msg=" + msg + ']');

                processStreamerMessage(nodeId, msg);
            }
        });

        ctx.event().addLocalEventListener(new GridLocalEventListener() {
            @Override public void onEvent(GridEvent evt) {
                GridDiscoveryEvent discoEvt = (GridDiscoveryEvent)evt;

                for (GridStreamerStageExecutionFuture fut : stageFuts.values())
                    fut.onNodeLeft(discoEvt.eventNode().id());
            }
        }, EVT_NODE_LEFT, EVT_NODE_FAILED);
    }

    /**
     * Injects resources into streamer components.
     *
     * @throws GridException If failed.
     */
    private void prepareResources() throws GridException {
        for (GridStreamerStage s : c.getStages())
            ctx.resource().injectGeneric(s);

        if (router == null)
            router = new GridStreamerLocalEventRouter();

        ctx.resource().injectGeneric(router);

        for (GridStreamerWindow w : c.getWindows())
            ctx.resource().injectGeneric(w);
    }

    /**
     * On kernal stop callback.
     *
     * @param cancel Cancel.
     */
    public void onKernalStop(boolean cancel) {
        // No further requests will be processed neither locally nor remotely.
        lock.writeLock();

        try {
            stopping = true;
        }
        finally {
            lock.writeUnlock();
        }

        if (cancel) {
            for (BatchExecutionFuture execFut : batchFuts.values()) {
                try {
                    execFut.cancel();
                }
                catch (GridException e) {
                    U.warn(log, "Failed to cancel batch execution future on node stop (will ignore) " +
                        "[execFut=" + execFut + ", err=" + e + ']');
                }
            }
        }
        else {
            // We need to wait for all locally scheduled stage futures to complete.
            for (GridStreamerStageExecutionFuture fut : stageFuts.values()) {
                try {
                    if (fut.rootExecution()) {
                        if (log.isDebugEnabled())
                            log.debug("Waiting root execution future on kernal stop: " + fut);

                        fut.get();
                    }
                }
                catch (GridException ignore) {
                    // For failed futures callback will be executed, no need to care about this exception here.
                }
            }

            for (BatchExecutionFuture execFut : batchFuts.values()) {
                try {
                    execFut.get();
                }
                catch (GridException e) {
                    if (!e.hasCause(GridInterruptedException.class))
                        U.warn(log, "Failed to wait for batch execution future completion (will ignore) " +
                            "[execFut=" + execFut + ", e=" + e + ']');
                }
            }
        }

        for (GridStreamerStageWrapper stage : stages.values()) {
            try {
                ctx.resource().cleanupGeneric(stage.unwrap());
            }
            catch (GridException e) {
                U.error(log, "Failed to cleanup stage [stage=" + stage + ", streamer=" + this + ']', e);
            }
        }
    }

    /**
     * @param cancel Whether currently running tasks should be cancelled.
     */
    public void stop(boolean cancel) {
        ctx.io().removeMessageListener(topic);

        if (dfltExecSvc)
            // There is no point to wait for tasks execution since it was already handled by flags.
            execSvc.shutdownNow();
        else {
            // Cannot call shutdownNow here since there may be user tasks in thread pool which cannot be discarded.
            if (c.isExecutorServiceShutdown())
                execSvc.shutdown();
        }

        U.stopLifecycleAware(log, lifecycleAwares());
    }

    /**
     * @return Streamer components which can implement {@link GridLifecycleAware} interface.
     */
    private Iterable<Object> lifecycleAwares() {
        Collection<Object> objs = new ArrayList<>();

        objs.addAll(configuration().getStages());
        objs.addAll(configuration().getWindows());
        objs.add(router);

        return objs;
    }

    /** {@inheritDoc} */
    @Nullable @Override public String name() {
        return name;
    }

    /** {@inheritDoc} */
    @Override public GridStreamerConfiguration configuration() {
        return c;
    }

    /** {@inheritDoc} */
    @Override public void addEvent(Object evt, Object... evts) throws GridException {
        A.notNull(evt, "evt");

        if (!F.isEmpty(evts))
            addEvents(F.concat(false, evt, Arrays.asList(evts)));
        else
            addEvents(Collections.singleton(evt));
    }

    /** {@inheritDoc} */
    @Override public void addEventToStage(String stageName, Object evt, Object... evts) throws GridException {
        A.notNull(stageName, "stageName");
        A.notNull(evt, "evt");

        if (!F.isEmpty(evts))
            addEventsToStage(stageName, F.concat(false, evt, Arrays.asList(evts)));
        else
            addEventsToStage(stageName, Collections.singleton(evt));
    }

    /** {@inheritDoc} */
    @Override public void addEvents(Collection<?> evts) throws GridException {
        A.ensure(!F.isEmpty(evts), "evts cannot be null or empty");

        addEventsToStage(firstStage, evts);
    }

    /** {@inheritDoc} */
    @Override public void addEventsToStage(String stageName, Collection<?> evts) throws GridException {
        A.notNull(stageName, "stageName");
        A.ensure(!F.isEmpty(evts), "evts cannot be empty or null");

        ctx.gateway().readLock();

        try {
            addEvents0(null, 0, U.currentTimeMillis(), null, Collections.singleton(ctx.localNodeId()), stageName, evts);
        }
        finally {
            ctx.gateway().readUnlock();
        }
    }

    /** {@inheritDoc} */
    @Override public GridStreamerContext context() {
        return streamerCtx;
    }

    /** {@inheritDoc} */
    @Override public void addStreamerFailureListener(GridStreamerFailureListener lsnr) {
        failureLsnrs.add(lsnr);
    }

    /** {@inheritDoc} */
    @Override public void removeStreamerFailureListener(GridStreamerFailureListener lsnr) {
        failureLsnrs.remove(lsnr);
    }

    /** {@inheritDoc} */
    @Override public GridStreamerMetrics metrics() {
        GridStreamerMetrics ret = new GridStreamerMetricsAdapter(streamerMetrics);

        streamerMetrics.sampleCurrentStages();

        return ret;
    }

    /** {@inheritDoc} */
    @Override public void reset() {
        winLock.writeLock();

        try {
            for (GridStreamerWindow win : winMap.values())
                win.reset();

            streamerCtx.localSpace().clear();
        }
        finally {
            winLock.writeUnlock();
        }
    }

    /** {@inheritDoc} */
    @Override public void resetMetrics() {
        GridStreamerStageMetricsHolder[] stageHolders = new GridStreamerStageMetricsHolder[c.getStages().size()];

        int idx = 0;

        for (GridStreamerStage stage : c.getStages()) {
            stageHolders[idx] = new GridStreamerStageMetricsHolder(stage.name());

            idx++;
        }

        GridStreamerWindowMetricsHolder[] windowHolders = new GridStreamerWindowMetricsHolder[c.getWindows().size()];

        idx = 0;

        for (GridStreamerWindow w : c.getWindows()) {
            windowHolders[idx] = new GridStreamerWindowMetricsHolder(w);

            idx++;
        }

        streamerMetrics = new GridStreamerMetricsHolder(stageHolders, windowHolders, execSvcCap);
    }

    /** {@inheritDoc} */
    @Override public void deployClass(Class<?> depCls) {
        this.depCls = depCls;
    }

    /** {@inheritDoc} */
    @Override public <E> GridStreamerWindow<E> window() {
        return (GridStreamerWindow<E>)dfltWin;
    }

    /** {@inheritDoc} */
    @Override public <E> GridStreamerWindow<E> window(String windowName) {
        return (GridStreamerWindow<E>)winMap.get(windowName);
    }

    /**
     * @return {@code atLeastOnce} configuration property.
     */
    boolean atLeastOnce() {
        return atLeastOnce;
    }

    /**
     * @return Stage future map size.
     */
    int stageFutureMapSize() {
        return stageFuts.size();
    }

    /**
     * @return Batch future map size.
     */
    int batchFutureMapSize() {
        return batchFuts.size();
    }

    /**
     * @param execId Execution ID, {@code null} if root execution.
     * @param failoverAttempt Attempt count.
     * @param execStartTs Execution start timestamp, ignored if root execution.
     * @param parentFutId Parent future ID.
     * @param execNodeIds Execution node IDs.
     * @param stageName Stage name.
     * @param evts Events.
     * @return Future.
     * @throws GridInterruptedException If failed.
     */
    private GridStreamerStageExecutionFuture addEvents0(
        @Nullable GridUuid execId,
        int failoverAttempt,
        long execStartTs,
        @Nullable GridUuid parentFutId,
        @Nullable Collection<UUID> execNodeIds,
        String stageName,
        Collection<?> evts
    ) throws GridInterruptedException {
        assert !F.isEmpty(evts);
        assert !F.isEmpty(stageName);

        GridStreamerStageExecutionFuture fut = new GridStreamerStageExecutionFuture(
            this,
            execId,
            failoverAttempt,
            execStartTs,
            parentFutId,
            execNodeIds,
            stageName,
            evts);

        if (atLeastOnce && fut.rootExecution()) {
            GridStreamerMetricsHolder metrics0 = streamerMetrics;

            metrics0.onSessionStarted();

            fut.metrics(metrics0);
        }

        // Acquire semaphore on first future submit.
        if (atLeastOnce && fut.rootExecution() && fut.failoverAttemptCount() == 0) {
            try {
                if (sem != null)
                    sem.acquire();
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();

                throw new GridInterruptedException(e);
            }
        }

        fut.map();

        if (!atLeastOnce && fut.isFailed())
            notifyFailure(fut.stageName(), fut.events(), fut.error());

        // Mind the gap.
        for (UUID nodeId : fut.executionNodeIds()) {
            if (!ctx.discovery().alive(nodeId))
                fut.onNodeLeft(nodeId);
        }

        return fut;
    }

    /** {@inheritDoc} */
    @Override public GridKernalContext kernalContext() {
        return ctx;
    }

    /** {@inheritDoc} */
    @Override public void onFutureMapped(GridStreamerStageExecutionFuture fut) {
        if (atLeastOnce) {
            GridStreamerStageExecutionFuture old = stageFuts.putIfAbsent(fut.id(), fut);

            assert old == null : "Streamer execution future should be mapped only once: " + old;
        }
    }

    /** {@inheritDoc} */
    @Override public void onFutureCompleted(GridStreamerStageExecutionFuture fut) {
        if (atLeastOnce) {
            if (fut.rootExecution() && !fut.isFailed() && sem != null)
                sem.release();

            GridStreamerStageExecutionFuture old = stageFuts.remove(fut.id());

            assert fut == old : "Invalid future in map [fut=" + fut + ", old=" + old + ']';

            if (fut.isFailed() || fut.isCancelled())
                cancelChildStages(fut);

            if (fut.rootExecution() && fut.isFailed())
                failover(fut);
        }
    }

    /** {@inheritDoc} */
    @Override public GridStreamerEventRouter eventRouter() {
        return router;
    }

    /** {@inheritDoc} */
    @Override public void scheduleExecutions(GridStreamerStageExecutionFuture fut,
        Map<UUID, GridStreamerExecutionBatch> execs) throws GridException {
        for (Map.Entry<UUID, GridStreamerExecutionBatch> entry : execs.entrySet()) {
            UUID nodeId = entry.getKey();
            GridStreamerExecutionBatch batch = entry.getValue();

            if (ctx.localNodeId().equals(nodeId))
                scheduleLocal(batch);
            else {
                if (log.isDebugEnabled())
                    log.debug("Sending batch execution request to remote node [nodeId=" + nodeId +
                        ", futId=" + batch.futureId() + ", stageName=" + batch.stageName() + ']');

                sendWithRetries(nodeId, createExecutionRequest(batch));

                if (!ctx.discovery().alive(nodeId))
                    fut.onNodeLeft(nodeId);
            }
        }
    }

    /** {@inheritDoc} */
    @Override public void onUndeploy(UUID leftNodeId, ClassLoader undeployedLdr) {
        if (log.isDebugEnabled())
            log.debug("Processing undeployment event [leftNodeId=" + leftNodeId +
                ", undeployedLdr=" + undeployedLdr + ']');

        unwindUndeploys(undeployedLdr, true);
    }

    /** {@inheritDoc} */
    @Override public void onQueryCompleted(long time, int nodes) {
        streamerMetrics.onQueryCompleted(time, nodes);
    }

    /**
     * Schedules batch execution locally.
     *
     * @param batch Batch to execute.
     * @throws GridException If schedule was attempted on stopping grid.
     */
    private void scheduleLocal(final GridStreamerExecutionBatch batch) throws GridException {
        final GridUuid futId = batch.futureId();

        lock.readLock();

        try {
            if (stopping)
                throw new GridException("Failed to schedule local batch execution (grid is stopping): " + batch);

            if (log.isDebugEnabled())
                log.debug("Scheduling local batch execution [futId=" + futId + ", stageName=" + batch.stageName() + ']');

            GridStreamerStageWrapper wrapper = stages.get(batch.stageName());

            if (wrapper == null) {
                completeParentStage(ctx.localNodeId(), batch.futureId(),
                    new GridException("Failed to process streamer batch (stage was not found): " +
                        batch.stageName() + ']'));

                return;
            }

            // Capture metrics holders for batch execution.
            GridStreamerMetricsHolder streamerMetrics0 = streamerMetrics;

            BatchWorker worker = new BatchWorker(batch, wrapper, streamerMetrics0);

            BatchExecutionFuture batchFut = worker.completionFuture();

            BatchExecutionFuture old = batchFuts.putIfAbsent(futId, batchFut);

            assert old == null : "Duplicate batch execution future [old=" + old + ", batchFut=" + batchFut + ']';

            if (cancelled(futId)) {
                // Batch was cancelled before execution started, remove future and return.
                batchFuts.remove(futId, batchFut);

                return;
            }

            streamerMetrics0.onStageScheduled();

            execSvc.submit(worker);

            batchFut.listenAsync(new CI1<GridFuture<Object>>() {
                @Override public void apply(GridFuture<Object> t) {
                    BatchExecutionFuture fut = (BatchExecutionFuture)t;

                    if (log.isDebugEnabled())
                        log.debug("Completed batch execution future: " + fut);

                    batchFuts.remove(futId, fut);

                    if (!fut.isCancelled() && atLeastOnce)
                        completeParentStage(ctx.localNodeId(), batch.futureId(), fut.error());
                }
            });
        }
        finally {
            lock.readUnlock();
        }
    }

    /**
     * Completes parent execution future.
     *
     * @param completeNodeId Node ID completed batch.
     * @param futId Future ID to complete.
     * @param err Error, if any.
     */
    private void completeParentStage(UUID completeNodeId, GridUuid futId, @Nullable Throwable err) {
        lock.readLock();

        try {
            if (stopping && !atLeastOnce) {
                if (log.isDebugEnabled())
                    log.debug("Failed to notify parent stage completion (node is stopping) [futId=" + futId +
                        ", err=" + err + ']');

                return;
            }

            UUID dstNodeId = futId.globalId();

            if (ctx.localNodeId().equals(dstNodeId)) {
                GridStreamerStageExecutionFuture stageFut = stageFuts.get(futId);

                if (log.isDebugEnabled())
                    log.debug("Notifying local execution future [completeNodeId=" + completeNodeId +
                        ", stageFut=" + stageFut + ", err=" + err + ']');

                if (stageFut != null)
                    stageFut.onExecutionCompleted(completeNodeId, err);
            }
            else {
                try {
                    if (log.isDebugEnabled())
                        log.debug("Sending completion response to remote node [nodeId=" + dstNodeId +
                            ", futId=" + futId + ", err=" + err + ']');

                    byte[] errBytes = err != null ? ctx.config().getMarshaller().marshal(err) : null;

                    sendWithRetries(dstNodeId, new GridStreamerResponse(futId, errBytes));
                }
                catch (GridException e) {
                    if (!e.hasCause(GridTopologyException.class))
                        log.error("Failed to complete parent stage [futId=" + futId + ", err=" + e + ']');
                }
            }
        }
        finally {
            lock.readUnlock();
        }
    }

    /**
     * Cancels child batches of execution future.
     *
     * @param fut Future to cancel child batches.
     */
    private void cancelChildStages(GridStreamerStageExecutionFuture fut) {
        for (UUID nodeId : fut.childExecutions().keySet())
            cancelChildStage(nodeId, fut.id());
    }

    /**
     * @param nodeId Node ID to cancel future on.
     * @param cancelledFutId Future ID to cancel.
     */
    private void cancelChildStage(UUID nodeId, GridUuid cancelledFutId) {
        assert atLeastOnce;

        if (nodeId.equals(ctx.localNodeId())) {
            cancelledFutIds.add(cancelledFutId);

            Iterator<Map.Entry<GridUuid, BatchExecutionFuture>> it = batchFuts.entrySet().iterator();

            while (it.hasNext()) {
                Map.Entry<GridUuid, BatchExecutionFuture> entry = it.next();

                if (entry.getKey().equals(cancelledFutId)) {
                    BatchExecutionFuture batchFut = entry.getValue();

                    try {
                        batchFut.cancel();
                    }
                    catch (GridException e) {
                        log.warning("Failed to cancel batch execution future [cancelledFutId=" + cancelledFutId +
                            ", batchFut=" + batchFut + ']', e);
                    }

                    it.remove();
                }
            }
        }
        else {
            try {
                sendWithRetries(nodeId, new GridStreamerCancelRequest(cancelledFutId));
            }
            catch (GridException e) {
                if (!e.hasCause(GridTopologyException.class))
                    log.error("Failed to send streamer cancel request to remote node [nodeId=" + nodeId +
                        ", cancelledFutId=" + cancelledFutId + ']', e);
            }
        }
    }

    /**
     * Attempts to failover pipeline execution.
     *
     * @param fut Future.
     */
    private void failover(GridStreamerStageExecutionFuture fut) {
        assert fut.rootExecution();
        assert fut.error() != null;

        if (fut.failoverAttemptCount() >= c.getMaximumFailoverAttempts() || stopping) {
            // Release semaphore when no failover will be attempted anymore.
            if (sem != null)
                sem.release();

            notifyFailure(fut.stageName(), fut.events(), fut.error());
        }
        else {
            try {
                addEvents0(null, fut.failoverAttemptCount() + 1, 0, null, Collections.singleton(ctx.localNodeId()),
                    fut.stageName(), fut.events());
            }
            catch (GridInterruptedException e) {
                e.printStackTrace();

                assert false : "Failover submit should never attempt to acquire semaphore: " + fut + ']';
            }
        }
    }

    /**
     * Notifies failure listeners.
     *
     * @param stageName Stage name.
     * @param evts Events.
     * @param err Error cause.
     */
    private void notifyFailure(String stageName, Collection<Object> evts, Throwable err) {
        for (GridStreamerFailureListener lsnr : failureLsnrs)
            lsnr.onFailure(stageName, evts, err);
    }

    /**
     * Checks if cancel request was received for this future ID.
     *
     * @param futId Future ID.
     * @return {@code True} if future was cancelled, {@code false} otherwise.
     */
    public boolean cancelled(GridUuid futId) {
        return cancelledFutIds.contains(futId);
    }

    /**
     * Processes streamer message.
     *
     * @param sndNodeId Sender node ID.
     * @param msg Message.
     */
    private void processStreamerMessage(UUID sndNodeId, Object msg) {
        if (msg instanceof GridStreamerExecutionRequest) {
            GridStreamerExecutionRequest req = (GridStreamerExecutionRequest)msg;

            GridStreamerExecutionBatch batch;

            try {
                batch = executionBatch(sndNodeId, req);
            }
            catch (GridException e) {
                U.error(log, "Failed to unmarshal execution batch (was class undeployed?) " +
                    "[sndNodeId=" + sndNodeId + ", msg=" + msg + ']', e);

                return;
            }

            try {
                scheduleLocal(batch);
            }
            catch (GridException e) {
                // Notify parent in case of error.
                completeParentStage(ctx.localNodeId(), batch.futureId(), e);
            }
        }
        else if (msg instanceof GridStreamerCancelRequest)
            // This will be a local call.
            cancelChildStage(ctx.localNodeId(), ((GridStreamerCancelRequest)msg).cancelledFutureId());
        else if (msg instanceof GridStreamerResponse) {
            GridStreamerResponse res = (GridStreamerResponse)msg;

            assert res.futureId().globalId().equals(ctx.localNodeId()) :
                "Wrong message received [res=" + res + ", sndNodeId=" + sndNodeId +
                ", locNodeId=" + ctx.localNodeId() + ']';

            Throwable err = null;

            if (res.errorBytes() != null) {
                try {
                    err = ctx.config().getMarshaller().unmarshal(res.errorBytes(), null);
                }
                catch (GridException e) {
                    U.error(log, "Failed to unmarshal response.", e);
                }
            }

            // This will complete stage locally.
            completeParentStage(sndNodeId, res.futureId(), err);
        }
    }

    /**
     * Clear entries of removed classloader classes.
     *
     * @param undeployedClsLdr Classloader.
     * @param info Whether to print info to log.
     */
    private void unwindUndeploys(ClassLoader undeployedClsLdr, boolean info) {
        assert undeployedClsLdr != null;

        int undeployWindowCnt = 0;

        Iterator<Object> it = streamerCtx.window().iterator();

        while (it.hasNext()) {
            Object evt = it.next();

            if (undeployedClsLdr.equals(evt.getClass().getClassLoader())) {
                it.remove();

                undeployWindowCnt++;
            }
        }

        int undeploySpaceCnt = 0;

        Iterator<Map.Entry<Object, Object>> entryIt = streamerCtx.localSpace().entrySet().iterator();

        while (entryIt.hasNext()) {
            Map.Entry<Object, Object> entry = entryIt.next();

            if (undeployedClsLdr.equals(entry.getKey().getClass().getClassLoader()) ||
                undeployedClsLdr.equals(entry.getValue().getClass().getClassLoader())) {
                entryIt.remove();

                undeploySpaceCnt++;
            }
        }

        if (info) {
            if (log.isInfoEnabled() && (undeployWindowCnt > 0 || undeploySpaceCnt > 0))
                log.info("Undeployed all streamer events (if any) for obsolete class loader " +
                    "[undeployedClsLdr=" + undeployedClsLdr + ", undeployWindowCnt=" + undeployWindowCnt +
                    ", undeploySpaceCnt=" + undeploySpaceCnt + ']');
        }
    }

    /**
     * Creates execution request from batch. Will include deployment information if P2P class loading
     * is enabled.
     *
     * @param batch Execution batch.
     * @return Execution request.
     * @throws GridException If failed.
     */
    private GridTcpCommunicationMessageAdapter createExecutionRequest(GridStreamerExecutionBatch batch)
        throws GridException {
        boolean depEnabled = ctx.deploy().enabled();

        byte[] batchBytes = ctx.config().getMarshaller().marshal(batch);

        if (!depEnabled)
            return new GridStreamerExecutionRequest(true, batchBytes, null, null, null, null, null);
        else {
            GridPeerDeployAware pda = new StreamerPda(batch.events());

            GridDeployment dep = ctx.deploy().deploy(pda.deployClass(), pda.classLoader());

            if (dep == null)
                throw new GridException("Failed to get deployment for batch request [batch=" + batch +
                    ", pda=" + pda + ']');

            return new GridStreamerExecutionRequest(
                false,
                batchBytes,
                dep.deployMode(),
                dep.sampleClassName(),
                dep.userVersion(),
                dep.participants(),
                dep.classLoaderId()
            );
        }
    }

    /**
     * Gets execution batch from execution request. Will explicitly unmarshal batch if P2P class loading
     * is enabled.
     *
     * @param nodeId Node ID.
     * @param req Execution request.
     * @return Execution batch.
     * @throws GridException If unmarshalling failed.
     */
    private GridStreamerExecutionBatch executionBatch(UUID nodeId, GridStreamerExecutionRequest req)
        throws GridException {
        GridDeployment dep = null;

        if (!req.forceLocalDeployment()) {
            dep = ctx.deploy().getGlobalDeployment(
                req.deploymentMode(),
                req.sampleClassName(),
                req.sampleClassName(),
                req.userVersion(),
                nodeId,
                req.classLoaderId(),
                req.loaderParticipants(),
                null);

            if (dep == null)
                throw new GridException("Failed to obtain global deployment based on deployment metadata " +
                    "[nodeId=" + nodeId + ", req=" + req + ']');
        }

        GridStreamerExecutionBatch batch = ctx.config().getMarshaller().unmarshal(req.batchBytes(),
            dep != null ? dep.classLoader() : null);

        // Set deployment to check for undeployment after stage execution.
        batch.deployment(dep);

        return batch;
    }

    /**
     * Tries to send message to remote node.
     *
     * @param dstNodeId Destination node ID.
     * @param msg Message to send.
     * @throws GridException If failed.
     */
    private void sendWithRetries(UUID dstNodeId, GridTcpCommunicationMessageAdapter msg) throws GridException {
        for (int i = 0; i < SEND_RETRY_COUNT; i++) {
            try {
                ctx.io().send(dstNodeId, topic, msg, GridIoPolicy.SYSTEM_POOL);

                return;
            }
            catch (GridException e) {
                if (log.isDebugEnabled())
                    log.debug("Failed to send message to remote node (will retry) [dstNodeId=" + dstNodeId +
                        ", msg=" + msg + ", err=" + e + ']');

                if (!ctx.discovery().alive(dstNodeId))
                    throw new GridTopologyException("Failed to send message (destination node left grid): " +
                        dstNodeId);

                if (i == SEND_RETRY_COUNT - 1)
                    throw e;

                U.sleep(SEND_RETRY_DELAY);
            }
        }
    }

    /** {@inheritDoc} */
    @Override public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(ctx);
        U.writeString(out, name);
    }

    /** {@inheritDoc} */
    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        ctx = (GridKernalContext)in.readObject();
        name = U.readString(in);
    }

    /**
     * Reconstructs object on unmarshalling.
     *
     * @return Reconstructed object.
     */
    protected Object readResolve() {
        return ctx.stream().streamer(name);
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridStreamerImpl.class, this);
    }

    /**
     * Data loader peer-deploy aware.
     */
    private class StreamerPda implements GridPeerDeployAware {
        /** */
        private static final long serialVersionUID = 0L;

        /** Deploy class. */
        private Class<?> cls;

        /** Class loader. */
        private ClassLoader ldr;

        /** Collection of objects to detect deploy class and class loader. */
        private Collection<Object> objs;

        /**
         * Constructs data loader peer-deploy aware.
         *
         * @param objs Collection of objects to detect deploy class and class loader.
         */
        private StreamerPda(Collection<Object> objs) {
            this.objs = objs;
        }

        /** {@inheritDoc} */
        @Override public Class<?> deployClass() {
            if (cls == null) {
                Class<?> cls0 = null;

                if (depCls != null)
                    cls0 = depCls;
                else {
                    for (Iterator<Object> it = objs.iterator(); (cls0 == null || U.isJdk(cls0)) && it.hasNext();)
                        cls0 = U.detectClass(it.next());

                    if (cls0 == null || U.isJdk(cls0))
                        cls0 = GridStreamerImpl.class;
                }

                assert cls0 != null : "Failed to detect deploy class [objs=" + objs + ']';

                cls = cls0;
            }

            return cls;
        }

        /** {@inheritDoc} */
        @Override public ClassLoader classLoader() {
            if (ldr == null) {
                ClassLoader ldr0 = deployClass().getClassLoader();

                // Safety.
                if (ldr0 == null)
                    ldr0 = U.gridClassLoader();

                assert ldr0 != null : "Failed to detect classloader [objs=" + objs + ']';

                ldr = ldr0;
            }

            return ldr;
        }
    }

    /**
     * Stage batch worker.
     */
    private class BatchWorker extends GridWorker {
        /** Batch */
        private GridStreamerExecutionBatch batch;

        /** */
        private GridStreamerStageWrapper stageWrapper;

        /** Streamer metrics holder. */
        private GridStreamerMetricsHolder streamerHolder;

        /** Schedule timestamp. */
        private long schedTs;

        /** Stage completion future. */
        private BatchExecutionFuture fut = new BatchExecutionFuture(ctx);

        /**
         * Creates worker.
         *
         * @param batch Batch.
         * @param stageWrapper Wrapper.
         * @param streamerHolder Stream holder.
         */
        private BatchWorker(
            GridStreamerExecutionBatch batch,
            GridStreamerStageWrapper stageWrapper,
            GridStreamerMetricsHolder streamerHolder
        ) {
            super(ctx.gridName(), "streamer-batch-worker-" + batch.stageName(), log);

            assert stageWrapper != null;

            this.batch = batch;
            this.stageWrapper = stageWrapper;
            this.streamerHolder = streamerHolder;

            schedTs = U.currentTimeMillis();

            fut.setWorker(this);
        }

        /**
         * @return Completion future.
         */
        public BatchExecutionFuture completionFuture() {
            return fut;
        }

        /** {@inheritDoc} */
        @Override protected void body() throws InterruptedException, GridInterruptedException {
            try {
                long start = U.currentTimeMillis();

                streamerHolder.onStageExecutionStarted(stageWrapper.index(), start - schedTs);

                long end = 0;

                try {
                    if (log.isDebugEnabled())
                        log.debug("Running streamer stage [stage=" + stageWrapper.name() +
                            ", futId=" + batch.futureId() + ']');

                    GridStreamerContext ctxDelegate = new GridStreamerContextDelegate(context(),
                        stageWrapper.nextStageName());

                    winLock.readLock();

                    Map<String, Collection<?>> res;

                    try {
                        res = stageWrapper.run(ctxDelegate, batch.events());
                    }
                    finally {
                        winLock.readUnlock();
                    }

                    // Close window for undeploy event.
                    GridDeployment dep = batch.deployment();

                    if (dep != null && dep.obsolete())
                        unwindUndeploys(dep.classLoader(), false);

                    if (res != null) {
                        for (Map.Entry<String, Collection<?>> entry : res.entrySet()) {
                            if (entry.getKey() == null)
                                throw new GridException("Failed to pass events to next stage " +
                                    "(stage name cannot be null).");

                            GridStreamerStageExecutionFuture part = addEvents0(
                                batch.executionId(),
                                0,
                                batch.executionStartTimeStamp(),
                                batch.futureId(),
                                batch.executionNodeIds(),
                                entry.getKey(),
                                entry.getValue());

                            if (atLeastOnce)
                                fut.add(part);
                        }
                    }
                    else {
                        if (log.isDebugEnabled())
                            log.debug("Finished pipeline execution [stage=" + stageWrapper.name() +
                                ", futId=" + batch.futureId() + ']');

                        end = U.currentTimeMillis();

                        streamerHolder.onPipelineCompleted(end - batch.executionStartTimeStamp(),
                            batch.executionNodeIds().size());
                    }
                }
                catch (GridException e) {
                    if (!atLeastOnce) {
                        notifyFailure(batch.stageName(), batch.events(), e);

                        streamerHolder.onStageFailure(stageWrapper.index());
                    }

                    fut.onDone(e);
                }
                finally {
                    if (end == 0)
                        end = U.currentTimeMillis();

                    streamerHolder.onStageExecutionFinished(stageWrapper.index(), end - start);
                }
            }
            finally {
                fut.markInitialized();
            }
        }
    }

    /**
     * Batch execution future.
     */
    private static class BatchExecutionFuture extends GridCompoundFuture<Object, Object> {
        /** */
        private static final long serialVersionUID = 0L;

        /** */
        private BatchWorker w;

        /**
         * Empty constructor required for {@link Externalizable}.
         */
        public BatchExecutionFuture() {
            // No-op.
        }

        /**
         * @param ctx Context.
         */
        private BatchExecutionFuture(GridKernalContext ctx) {
            super(ctx);
        }

        /** {@inheritDoc} */
        @Override public boolean cancel() throws GridException {
            assert w != null;

            if (!super.cancel())
                return false;

            w.cancel();

            return true;
        }

        /**
         * @param w Worker.
         */
        public void setWorker(BatchWorker w) {
            assert w != null;

            this.w = w;
        }

        /** {@inheritDoc} */
        @Override public Throwable error() {
            return super.error();
        }
    }
}
