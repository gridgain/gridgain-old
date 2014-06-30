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

package org.gridgain.grid.kernal.processors.cache;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.affinity.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.util.future.*;

import java.util.*;

/**
 * Adapter for preloading which always assumes that preloading finished.
 */
public class GridCachePreloaderAdapter<K, V> implements GridCachePreloader<K, V> {
    /** Cache context. */
    protected final GridCacheContext<K, V> cctx;

    /** Logger.*/
    protected final GridLogger log;

    /** Affinity. */
    protected final GridCacheAffinityFunction aff;

    /** Start future (always completed by default). */
    private final GridFuture finFut;

    /** Preload predicate. */
    protected GridPredicate<GridCacheEntryInfo<K, V>> preloadPred;

    /**
     * @param cctx Cache context.
     */
    public GridCachePreloaderAdapter(GridCacheContext<K, V> cctx) {
        assert cctx != null;

        this.cctx = cctx;

        log = cctx.logger(getClass());
        aff = cctx.config().getAffinity();

        finFut = new GridFinishedFuture(cctx.kernalContext());
    }

    /** {@inheritDoc} */
    @Override public void start() throws GridException {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override public void stop() {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override public void onKernalStart() throws GridException {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override public void onKernalStop() {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override public void forcePreload() {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override public void preloadPredicate(GridPredicate<GridCacheEntryInfo<K, V>> preloadPred) {
        this.preloadPred = preloadPred;
    }

    /** {@inheritDoc} */
    @Override public GridPredicate<GridCacheEntryInfo<K, V>> preloadPredicate() {
        return preloadPred;
    }

    /** {@inheritDoc} */
    @Override public GridFuture<?> startFuture() {
        return finFut;
    }

    /** {@inheritDoc} */
    @Override public GridFuture<?> syncFuture() {
        return finFut;
    }

    /** {@inheritDoc} */
    @Override public void unwindUndeploys() {
        cctx.deploy().unwind();
    }

    /** {@inheritDoc} */
    @Override public GridFuture<Object> request(Collection<? extends K> keys, long topVer) {
        return new GridFinishedFuture<>(cctx.kernalContext());
    }
}
