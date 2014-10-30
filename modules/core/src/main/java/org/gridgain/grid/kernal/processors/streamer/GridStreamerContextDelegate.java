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
import org.gridgain.grid.lang.*;
import org.gridgain.grid.streamer.*;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.concurrent.*;

/**
 * Context delegate allowing to override next stage name.
 */
public class GridStreamerContextDelegate implements GridStreamerContext {
    /** Context delegate. */
    private GridStreamerContext delegate;

    /** Next stage name. */
    private String nextStageName;

    /**
     * @param delegate Delegate object.
     * @param nextStageName Next stage name.
     */
    public GridStreamerContextDelegate(GridStreamerContext delegate, @Nullable String nextStageName) {
        this.delegate = delegate;
        this.nextStageName = nextStageName;
    }

    /** {@inheritDoc} */
    @Override public GridProjection projection() {
        return delegate.projection();
    }

    /** {@inheritDoc} */
    @Override public <K, V> ConcurrentMap<K, V> localSpace() {
        return delegate.localSpace();
    }

    /** {@inheritDoc} */
    @Override public <E> GridStreamerWindow<E> window() {
        return delegate.window();
    }

    /** {@inheritDoc} */
    @Override public <E> GridStreamerWindow<E> window(String winName) {
        return delegate.window(winName);
    }

    /** {@inheritDoc} */
    @Override public String nextStageName() {
        return nextStageName;
    }

    /** {@inheritDoc} */
    @Override public <R> Collection<R> query(GridClosure<GridStreamerContext, R> clo) throws GridException {
        return delegate.query(clo);
    }

    /** {@inheritDoc} */
    @Override public <R> Collection<R> query(GridClosure<GridStreamerContext, R> clo, Collection<GridNode> nodes)
        throws GridException {
        return delegate.query(clo, nodes);
    }

    /** {@inheritDoc} */
    @Override public void broadcast(GridInClosure<GridStreamerContext> clo) throws GridException {
        delegate.broadcast(clo);
    }

    /** {@inheritDoc} */
    @Override public void broadcast(GridInClosure<GridStreamerContext> clo, Collection<GridNode> nodes)
        throws GridException {
        delegate.broadcast(clo, nodes);
    }

    /** {@inheritDoc} */
    @Override public <R1, R2> R2 reduce(GridClosure<GridStreamerContext, R1> clo, GridReducer<R1, R2> rdc)
        throws GridException {
        return delegate.reduce(clo, rdc);
    }

    /** {@inheritDoc} */
    @Override public <R1, R2> R2 reduce(GridClosure<GridStreamerContext, R1> clo, GridReducer<R1, R2> rdc,
        Collection<GridNode> nodes) throws GridException {
        return delegate.reduce(clo, rdc, nodes);
    }
}
