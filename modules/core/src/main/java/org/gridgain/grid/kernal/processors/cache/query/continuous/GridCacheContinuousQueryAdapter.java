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

package org.gridgain.grid.kernal.processors.cache.query.continuous;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.cache.query.*;
import org.gridgain.grid.kernal.processors.cache.*;
import org.gridgain.grid.kernal.processors.continuous.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.security.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.grid.util.*;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.concurrent.locks.*;

import static org.gridgain.grid.cache.GridCacheMode.*;

/**
 * Continuous query implementation.
 */
public class GridCacheContinuousQueryAdapter<K, V> implements GridCacheContinuousQuery<K, V> {
    /** Guard. */
    private final GridBusyLock guard = new GridBusyLock();

    /** Close lock. */
    private final Lock closeLock = new ReentrantLock();

    /** Cache context. */
    private final GridCacheContext<K, V> ctx;

    /** Topic for ordered messages. */
    private final Object topic;

    /** Projection predicate */
    private final GridPredicate<GridCacheEntry<K, V>> prjPred;

    /** Logger. */
    private final GridLogger log;

    /** Local callback. */
    private volatile GridBiPredicate<UUID, Collection<Map.Entry<K, V>>> cb;

    /** Filter. */
    private volatile GridBiPredicate<K, V> filter;

    /** Buffer size. */
    private volatile int bufSize = DFLT_BUF_SIZE;

    /** Time interval. */
    @SuppressWarnings("RedundantFieldInitialization")
    private volatile long timeInterval = DFLT_TIME_INTERVAL;

    /** Automatic unsubscribe flag. */
    private volatile boolean autoUnsubscribe = DFLT_AUTO_UNSUBSCRIBE;

    /** Continuous routine ID. */
    private UUID routineId;

    /**
     * @param ctx Cache context.
     * @param topic Topic for ordered messages.
     * @param prjPred Projection predicate.
     */
    GridCacheContinuousQueryAdapter(GridCacheContext<K, V> ctx, Object topic,
        @Nullable GridPredicate<GridCacheEntry<K, V>> prjPred) {
        assert ctx != null;
        assert topic != null;

        this.ctx = ctx;
        this.topic = topic;
        this.prjPred = prjPred;

        log = ctx.logger(getClass());
    }

    /** {@inheritDoc} */
    @Override public void callback(GridBiPredicate<UUID, Collection<Map.Entry<K, V>>> cb) {
        A.notNull(cb, "cb");

        if (!guard.enterBusy())
            throw new IllegalStateException("Continuous query can't be changed after it was executed.");

        try {
            this.cb = cb;
        }
        finally {
            guard.leaveBusy();
        }
    }

    /** {@inheritDoc} */
    @Nullable @Override public GridBiPredicate<UUID, Collection<Map.Entry<K, V>>> callback() {
        return cb;
    }

    /** {@inheritDoc} */
    @Override public void filter(@Nullable GridBiPredicate<K, V> filter) {
        if (!guard.enterBusy())
            throw new IllegalStateException("Continuous query can't be changed after it was executed.");

        try {
            this.filter = filter;
        }
        finally {
            guard.leaveBusy();
        }
    }

    /** {@inheritDoc} */
    @Nullable @Override public GridBiPredicate<K, V> filter() {
        return filter;
    }

    /** {@inheritDoc} */
    @Override public void bufferSize(int bufSize) {
        A.ensure(bufSize > 0, "bufSize > 0");

        if (!guard.enterBusy())
            throw new IllegalStateException("Continuous query can't be changed after it was executed.");

        try {
            this.bufSize = bufSize;
        }
        finally {
            guard.leaveBusy();
        }
    }

    /** {@inheritDoc} */
    @Override public int bufferSize() {
        return bufSize;
    }

    /** {@inheritDoc} */
    @Override public void timeInterval(long timeInterval) {
        A.ensure(timeInterval >= 0, "timeInterval >= 0");

        if (!guard.enterBusy())
            throw new IllegalStateException("Continuous query can't be changed after it was executed.");

        try {
            this.timeInterval = timeInterval;
        }
        finally {
            guard.leaveBusy();
        }
    }

    /** {@inheritDoc} */
    @Override public long timeInterval() {
        return timeInterval;
    }

    /** {@inheritDoc} */
    @Override public void autoUnsubscribe(boolean autoUnsubscribe) {
        this.autoUnsubscribe = autoUnsubscribe;
    }

    /** {@inheritDoc} */
    @Override public boolean isAutoUnsubscribe() {
        return autoUnsubscribe;
    }

    /** {@inheritDoc} */
    @Override public void execute() throws GridException {
        execute(null, false);
    }

    /** {@inheritDoc} */
    @Override public void execute(@Nullable GridProjection prj) throws GridException {
        execute(prj, false);
    }

    /**
     * Starts continuous query execution.
     *
     * @param prj Grid projection.
     * @param internal If {@code true} then query notified about internal entries updates.
     * @throws GridException If failed.
     */
    public void execute(@Nullable GridProjection prj, boolean internal) throws GridException {
        if (cb == null)
            throw new IllegalStateException("Mandatory local callback is not set for the query: " + this);

        ctx.checkSecurity(GridSecurityPermission.CACHE_READ);

        if (prj == null)
            prj = ctx.grid();

        prj = prj.forCache(ctx.name());

        if (prj.nodes().isEmpty())
            throw new GridTopologyException("Failed to execute query (projection is empty): " + this);

        GridCacheMode mode = ctx.config().getCacheMode();

        if (mode == LOCAL || mode == REPLICATED) {
            Collection<GridNode> nodes = prj.nodes();

            GridNode node = nodes.contains(ctx.localNode()) ? ctx.localNode() : F.rand(nodes);

            assert node != null;

            if (nodes.size() > 1 && !ctx.cache().isDrSystemCache()) {
                if (node.id().equals(ctx.localNodeId()))
                    U.warn(log, "Continuous query for " + mode + " cache can be run only on local node. " +
                        "Will execute query locally: " + this);
                else
                    U.warn(log, "Continuous query for " + mode + " cache can be run only on single node. " +
                        "Will execute query on remote node [qry=" + this + ", node=" + node + ']');
            }

            prj = prj.forNode(node);
        }

        closeLock.lock();

        try {
            if (routineId != null)
                throw new IllegalStateException("Continuous query can't be executed twice.");

            guard.block();

            GridContinuousHandler hnd = new GridCacheContinuousQueryHandler<>(ctx.name(), topic, cb, filter, prjPred,
                internal);

            routineId = ctx.kernalContext().continuous().startRoutine(hnd, bufSize, timeInterval, autoUnsubscribe,
                prj.predicate()).get();
        }
        finally {
            closeLock.unlock();
        }
    }

    /** {@inheritDoc} */
    @Override public void close() throws GridException {
        closeLock.lock();

        try {
            if (routineId == null)
                throw new IllegalStateException("Can't cancel query that was not executed.");

            ctx.kernalContext().continuous().stopRoutine(routineId).get();
        }
        finally {
            closeLock.unlock();
        }
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridCacheContinuousQueryAdapter.class, this);
    }
}
