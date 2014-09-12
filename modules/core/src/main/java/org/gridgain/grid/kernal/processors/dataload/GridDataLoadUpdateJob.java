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
import org.gridgain.grid.kernal.processors.cache.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.util.lang.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * Job to put entries to cache on affinity node.
 */
class GridDataLoadUpdateJob<K, V> implements GridPlainCallable<Object> {
    /** */
    private final GridKernalContext ctx;

    /** */
    private final GridLogger log;

    /** Cache name. */
    private final String cacheName;

    /** Entries to put. */
    private final Collection<Map.Entry<K, V>> col;

    /** {@code True} to ignore deployment ownership. */
    private final boolean ignoreDepOwnership;

    /** */
    private final GridDataLoadCacheUpdater<K, V> updater;

    /**
     * @param ctx Context.
     * @param log Log.
     * @param cacheName Cache name.
     * @param col Entries to put.
     * @param ignoreDepOwnership {@code True} to ignore deployment ownership.
     * @param updater Updater.
     */
    GridDataLoadUpdateJob(
        GridKernalContext ctx, GridLogger log, @Nullable String cacheName,
        Collection<Map.Entry<K, V>> col,
        boolean ignoreDepOwnership,
        GridDataLoadCacheUpdater<K, V> updater) {
        this.ctx = ctx;
        this.log = log;

        assert col != null && !col.isEmpty();
        assert updater != null;

        this.cacheName = cacheName;
        this.col = col;
        this.ignoreDepOwnership = ignoreDepOwnership;
        this.updater = updater;
    }

    /** {@inheritDoc} */
    @Override public Object call() throws Exception {
        if (log.isDebugEnabled())
            log.debug("Running put job [nodeId=" + ctx.localNodeId() + ", size=" + col.size() + ']');

        GridCacheAdapter<Object, Object> cache = ctx.cache().internalCache(cacheName);

        GridFuture<?> f = cache.context().preloader().startFuture();

        if (!f.isDone())
            f.get();

        if (ignoreDepOwnership)
            cache.context().deploy().ignoreOwnership(true);

        try {
            updater.update(cache.<K, V>cache(), col);

            return null;
        }
        finally {
            if (ignoreDepOwnership)
                cache.context().deploy().ignoreOwnership(false);

            if (log.isDebugEnabled())
                log.debug("Update job finished on node: " + ctx.localNodeId());
        }
    }
}
