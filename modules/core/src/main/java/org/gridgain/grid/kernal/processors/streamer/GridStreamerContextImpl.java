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
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.kernal.processors.streamer.task.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.streamer.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.jdk8.backport.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Streamer context implementation.
 */
public class GridStreamerContextImpl implements GridStreamerContext {
    /** Kernal context. */
    private GridKernalContext ctx;

    /** Local space. */
    private final ConcurrentMap<Object, Object> locSpace = new ConcurrentHashMap8<>();

    /** Streamer projection. */
    private AtomicReference<GridProjection> streamPrj = new AtomicReference<>();

    /** Streamer. */
    private GridStreamerEx streamer;

    /** Next stage name. */
    private String nextStageName;

    /**
     * @param ctx Kernal context.
     * @param cfg Streamer configuration.
     * @param streamer Streamer impl.
     */
    public GridStreamerContextImpl(GridKernalContext ctx, GridStreamerConfiguration cfg, GridStreamerEx streamer) {
        assert ctx != null;
        assert cfg != null;
        assert streamer != null;

        this.ctx = ctx;
        this.streamer = streamer;
    }

    /** {@inheritDoc} */
    @Override public GridProjection projection() {
        ctx.gateway().readLock();

        try {
            return projection0();
        }
        finally {
            ctx.gateway().readUnlock();
        }
    }

    /** {@inheritDoc} */
    @Override public <K, V> ConcurrentMap<K, V> localSpace() {
        return (ConcurrentMap<K, V>)locSpace;
    }

    /** {@inheritDoc} */
    @Override public <E> GridStreamerWindow<E> window() {
        return streamer.window();
    }

    /** {@inheritDoc} */
    @Override public <E> GridStreamerWindow<E> window(String winName) {
        GridStreamerWindow<E> window = streamer.window(winName);

        if (window == null)
            throw new IllegalArgumentException("Streamer window is not configured: " + winName);

        return window;
    }

    /** {@inheritDoc} */
    @Override public String nextStageName() {
        return nextStageName;
    }

    /**
     * Sets next stage name for main context.
     *
     * @param nextStageName Next stage name.
     */
    public void nextStageName(String nextStageName) {
        this.nextStageName = nextStageName;
    }

    /** {@inheritDoc} */
    @Override public <R> Collection<R> query(GridClosure<GridStreamerContext, R> clo) throws GridException {
        return query(clo, Collections.<GridNode>emptyList());
    }

    /** {@inheritDoc} */
    @Override public <R> Collection<R> query(GridClosure<GridStreamerContext, R> clo, Collection<GridNode> nodes)
        throws GridException {
        ctx.gateway().readLock();

        try {
            GridProjection prj = projection0();

            if (!F.isEmpty(nodes))
                prj = prj.forNodes(nodes);

            long startTime = U.currentTimeMillis();

            Collection<R> res =  prj.compute().execute(new GridStreamerQueryTask<>(clo, streamer.name()), null).get();

            streamer.onQueryCompleted(U.currentTimeMillis() - startTime, prj.nodes().size());

            return res;
        }
        finally {
            ctx.gateway().readUnlock();
        }
    }

    /** {@inheritDoc} */
    @Override public void broadcast(GridInClosure<GridStreamerContext> clo) throws GridException {
        broadcast(clo, Collections.<GridNode>emptyList());
    }

    /** {@inheritDoc} */
    @Override public void broadcast(GridInClosure<GridStreamerContext> clo, Collection<GridNode> nodes)
        throws GridException {
        ctx.gateway().readLock();

        try {
            GridProjection prj = projection0();

            if (!F.isEmpty(nodes))
                prj = prj.forNodes(nodes);

            prj.compute().execute(new GridStreamerBroadcastTask(clo, streamer.name()), null).get();
        }
        finally {
            ctx.gateway().readUnlock();
        }
    }

    /** {@inheritDoc} */
    @Override public <R1, R2> R2 reduce(GridClosure<GridStreamerContext, R1> clo, GridReducer<R1, R2> rdc)
        throws GridException {
        return reduce(clo, rdc, Collections.<GridNode>emptyList());
    }

    /** {@inheritDoc} */
    @Override public <R1, R2> R2 reduce(GridClosure<GridStreamerContext, R1> clo, GridReducer<R1, R2> rdc,
        Collection<GridNode> nodes) throws GridException {
        ctx.gateway().readLock();

        try {
            GridProjection prj = projection0();

            if (!F.isEmpty(nodes))
                prj = prj.forNodes(nodes);

            return prj.compute().execute(new GridStreamerReduceTask<>(clo, rdc, streamer.name()), null).get();
        }
        finally {
            ctx.gateway().readUnlock();
        }
    }

    /**
     * @return Streamer projection without grabbing read lock.
     */
    private GridProjection projection0() {
        GridProjection prj = streamPrj.get();

        if (prj == null) {
            prj = ctx.grid().forStreamer(streamer.name());

            streamPrj.compareAndSet(null, prj);

            prj = streamPrj.get();
        }

        return prj;
    }
}
