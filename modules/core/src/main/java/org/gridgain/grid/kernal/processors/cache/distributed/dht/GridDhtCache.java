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
import org.gridgain.grid.kernal.processors.cache.distributed.near.*;
import org.gridgain.grid.util.tostring.*;

import java.io.*;

/**
 * DHT cache.
 */
public class GridDhtCache<K, V> extends GridDhtTransactionalCacheAdapter<K, V> {
    /** */
    private static final long serialVersionUID = 0L;

    /** Near cache. */
    @GridToStringExclude
    private GridNearTransactionalCache<K, V> near;

    /**
     * Empty constructor required for {@link Externalizable}.
     */
    public GridDhtCache() {
        // No-op.
    }

    /**
     * @param ctx Context.
     */
    public GridDhtCache(GridCacheContext<K, V> ctx) {
        super(ctx);
    }

    /**
     * @param ctx Cache context.
     * @param map Cache map.
     */
    public GridDhtCache(GridCacheContext<K, V> ctx, GridCacheConcurrentMap<K, V> map) {
        super(ctx, map);
    }

    /** {@inheritDoc} */
    @Override public boolean isDht() {
        return true;
    }

    /** {@inheritDoc} */
    @Override public String name() {
        String name = super.name();

        return name == null ? "defaultDhtCache" : name + "Dht";
    }

    /** {@inheritDoc} */
    @Override public void start() throws GridException {
        resetMetrics();

        super.start();
    }

    /** {@inheritDoc} */
    @Override public void resetMetrics() {
        boolean isDrSndCache = cacheCfg.getDrSenderConfiguration() != null;
        boolean isDrRcvCache = cacheCfg.getDrReceiverConfiguration() != null;

        GridCacheMetricsAdapter m = new GridCacheMetricsAdapter(isDrSndCache, isDrRcvCache);

        m.delegate(ctx.dht().near().metrics0());

        metrics = m;
    }

    /**
     * @return Near cache.
     */
    @Override public GridNearTransactionalCache<K, V> near() {
        return near;
    }

    /**
     * @param near Near cache.
     */
    public void near(GridNearTransactionalCache<K, V> near) {
        this.near = near;
    }
}
