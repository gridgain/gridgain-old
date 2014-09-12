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

package org.gridgain.grid.kernal.processors.dataload;

import org.gridgain.grid.*;
import org.gridgain.grid.dataload.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.kernal.managers.communication.*;
import org.gridgain.grid.kernal.managers.deployment.*;
import org.gridgain.grid.kernal.processors.*;
import org.gridgain.grid.marshaller.*;
import org.gridgain.grid.thread.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.worker.*;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.concurrent.*;

import static org.gridgain.grid.kernal.GridTopic.*;
import static org.gridgain.grid.kernal.managers.communication.GridIoPolicy.*;

/**
 *
 */
public class GridDataLoaderProcessor<K, V> extends GridProcessorAdapter {
    /** Loaders map (access is not supposed to be highly concurrent). */
    private Collection<GridDataLoaderImpl> ldrs = new GridConcurrentHashSet<>();

    /** Busy lock. */
    private final GridSpinBusyLock busyLock = new GridSpinBusyLock();

    /** Flushing thread. */
    private Thread flusher;

    /** */
    private final DelayQueue<GridDataLoaderImpl<K, V>> flushQ = new DelayQueue<>();

    /** Marshaller. */
    private final GridMarshaller marsh;

    /**
     * @param ctx Kernal context.
     */
    public GridDataLoaderProcessor(GridKernalContext ctx) {
        super(ctx);

        ctx.io().addMessageListener(TOPIC_DATALOAD, new GridMessageListener() {
            @Override public void onMessage(UUID nodeId, Object msg) {
                assert msg instanceof GridDataLoadRequest;

                processDataLoadRequest(nodeId, (GridDataLoadRequest<K, V>)msg);
            }
        });

        marsh = ctx.config().getMarshaller();
    }

    /** {@inheritDoc} */
    @Override public void start() throws GridException {
        if (ctx.config().isDaemon())
            return;

        flusher = new GridThread(new GridWorker(ctx.gridName(), "grid-data-loader-flusher", log) {
            @Override protected void body() throws InterruptedException, GridInterruptedException {
                while (!isCancelled()) {
                    GridDataLoaderImpl<K, V> ldr = flushQ.take();

                    if (!busyLock.enterBusy())
                        return;

                    try {
                        if (ldr.isClosed())
                            continue;

                        ldr.tryFlush();

                        flushQ.offer(ldr);
                    }
                    finally {
                        busyLock.leaveBusy();
                    }
                }
            }
        });

        flusher.start();

        if (log.isDebugEnabled())
            log.debug("Started data loader processor.");
    }

    /** {@inheritDoc} */
    @Override public void onKernalStop(boolean cancel) {
        if (ctx.config().isDaemon())
            return;

        ctx.io().removeMessageListener(TOPIC_DATALOAD);

        busyLock.block();

        U.interrupt(flusher);
        U.join(flusher, log);

        for (GridDataLoader<?, ?> ldr : ldrs) {
            if (log.isDebugEnabled())
                log.debug("Closing active data loader on grid stop [ldr=" + ldr + ", cancel=" + cancel + ']');

            try {
                ldr.close(cancel);
            }
            catch (GridInterruptedException e) {
                U.warn(log, "Interrupted while waiting for completion of the data loader: " + ldr, e);
            }
            catch (GridException e) {
                U.error(log, "Failed to close data loader: " + ldr, e);
            }
        }

        if (log.isDebugEnabled())
            log.debug("Stopped data loader processor.");
    }

    /**
     * @param cacheName Cache name ({@code null} for default cache).
     * @return Data loader.
     */
    public GridDataLoader<K, V> dataLoader(@Nullable String cacheName) {
        if (!busyLock.enterBusy())
            throw new IllegalStateException("Failed to create data loader (grid is stopping).");

        try {
            final GridDataLoaderImpl<K, V> ldr = new GridDataLoaderImpl<>(ctx, cacheName, flushQ);

            ldrs.add(ldr);

            ldr.future().listenAsync(new CI1<GridFuture<?>>() {
                @Override public void apply(GridFuture<?> f) {
                    boolean b = ldrs.remove(ldr);

                    assert b : "Loader has not been added to set: " + ldr;

                    if (log.isDebugEnabled())
                        log.debug("Loader has been completed: " + ldr);
                }
            });

            return ldr;
        }
        finally {
            busyLock.leaveBusy();
        }
    }

    /**
     * @param nodeId Sender ID.
     * @param req Request.
     */
    private void processDataLoadRequest(UUID nodeId, GridDataLoadRequest<K, V> req) {
        if (!busyLock.enterBusy()) {
            if (log.isDebugEnabled())
                log.debug("Ignoring data load request (node is stopping): " + req);

            return;
        }

        try {
            if (log.isDebugEnabled())
                log.debug("Processing data load request: " + req);

            Object topic;

            try {
                topic = marsh.unmarshal(req.responseTopicBytes(), null);
            }
            catch (GridException e) {
                U.error(log, "Failed to unmarshal topic from request: " + req, e);

                return;
            }

            ClassLoader clsLdr;

            if (req.forceLocalDeployment())
                clsLdr = U.gridClassLoader();
            else {
                GridDeployment dep = ctx.deploy().getGlobalDeployment(
                    req.deploymentMode(),
                    req.sampleClassName(),
                    req.sampleClassName(),
                    req.userVersion(),
                    nodeId,
                    req.classLoaderId(),
                    req.participants(),
                    null);

                if (dep == null) {
                    sendResponse(nodeId,
                        topic,
                        req.requestId(),
                        new GridException("Failed to get deployment for request [sndId=" + nodeId +
                            ", req=" + req + ']'),
                        false);

                    return;
                }

                clsLdr = dep.classLoader();
            }

            Collection<Map.Entry<K, V>> col;
            GridDataLoadCacheUpdater<K, V> updater;

            try {
                col = marsh.unmarshal(req.collectionBytes(), clsLdr);
                updater = marsh.unmarshal(req.updaterBytes(), clsLdr);
            }
            catch (GridException e) {
                U.error(log, "Failed to unmarshal message [nodeId=" + nodeId + ", req=" + req + ']', e);

                sendResponse(nodeId, topic, req.requestId(), e, false);

                return;
            }

            GridDataLoadUpdateJob<K, V> job = new GridDataLoadUpdateJob<>(ctx, log, req.cacheName(), col,
                req.ignoreDeploymentOwnership(), updater);

            Exception err = null;

            try {
                job.call();
            }
            catch (Exception e) {
                U.error(log, "Failed to finish update job.", e);

                err = e;
            }

            sendResponse(nodeId, topic, req.requestId(), err, req.forceLocalDeployment());
        }
        finally {
            busyLock.leaveBusy();
        }
    }

    /**
     * @param nodeId Node ID.
     * @param resTopic Response topic.
     * @param reqId Request ID.
     * @param err Error.
     * @param forceLocDep Force local deployment.
     */
    private void sendResponse(UUID nodeId, Object resTopic, long reqId, @Nullable Throwable err,
        boolean forceLocDep) {
        byte[] errBytes;

        try {
            errBytes = err != null ? marsh.marshal(err) : null;
        }
        catch (GridException e) {
            U.error(log, "Failed to marshal message.", e);

            return;
        }

        GridDataLoadResponse res = new GridDataLoadResponse(reqId, errBytes, forceLocDep);

        try {
            ctx.io().send(nodeId, resTopic, res, PUBLIC_POOL);
        }
        catch (GridException e) {
            if (ctx.discovery().alive(nodeId))
                U.error(log, "Failed to respond to node [nodeId=" + nodeId + ", res=" + res + ']', e);
            else if (log.isDebugEnabled())
                log.debug("Node has left the grid: " + nodeId);
        }
    }

    /** {@inheritDoc} */
    @Override public void printMemoryStats() {
        X.println(">>>");
        X.println(">>> Data loader processor memory stats [grid=" + ctx.gridName() + ']');
        X.println(">>>   ldrsSize: " + ldrs.size());
    }
}
