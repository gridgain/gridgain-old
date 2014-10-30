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

package org.gridgain.grid.spi.discovery.tcp.metricsstore;

import org.gridgain.grid.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.jdk8.backport.*;

import java.util.*;

/**
 * Convenient metrics store adapter.
 */
public abstract class GridTcpDiscoveryMetricsStoreAdapter implements GridTcpDiscoveryMetricsStore {
    /** Default metrics expire time in milliseconds (value is <tt>10000</tt>). */
    public static final int DFLT_METRICS_EXPIRE_TIME = 10 * 1000;

    /** SPI context. */
    private volatile GridSpiContext spiCtx;

    /** Local metrics cache. */
    private final Map<UUID, GridNodeMetrics> metricsMap = new ConcurrentHashMap8<>();

    /** Local metrics cache timestamps. */
    private final Map<UUID, Long> tsMap = new ConcurrentHashMap8<>();

    /** Metrics expire time. */
    @SuppressWarnings({"FieldAccessedSynchronizedAndUnsynchronized"})
    private int metricsExpireTime = DFLT_METRICS_EXPIRE_TIME;

    /** {@inheritDoc} */
    @Override public void onSpiContextInitialized(GridSpiContext spiCtx) throws GridSpiException {
        this.spiCtx = spiCtx;
    }

    /** {@inheritDoc} */
    @Override public void onSpiContextDestroyed() {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override public final Map<UUID, GridNodeMetrics> metrics(Collection<UUID> nodeIds)
        throws GridSpiException {
        assert !F.isEmpty(nodeIds);

        long now = U.currentTimeMillis();

        Collection<UUID> expired = new LinkedList<>();

        for (UUID id : nodeIds) {
            GridNodeMetrics nodeMetrics = metricsMap.get(id);

            Long ts = tsMap.get(id);

            if (nodeMetrics == null || ts == null || ts < now - metricsExpireTime)
                expired.add(id);
        }

        if (!expired.isEmpty()) {
            Map<UUID, GridNodeMetrics> refreshed = metrics0(expired);

            for (UUID id : refreshed.keySet())
                tsMap.put(id, now);

            metricsMap.putAll(refreshed);
        }

        return F.view(metricsMap, F.contains(nodeIds));
    }

    /** {@inheritDoc} */
    @Override public final void removeMetrics(Collection<UUID> nodeIds) throws GridSpiException {
        assert !F.isEmpty(nodeIds);

        for (UUID id : nodeIds) {
            metricsMap.remove(id);

            tsMap.remove(id);
        }

        removeMetrics0(nodeIds);
    }

    /** {@inheritDoc} */
    @Override public int getMetricsExpireTime() {
        return metricsExpireTime;
    }

    /**
     * Sets metrics expire time in milliseconds.
     * <p>
     * Cached metrics are purged and requested from the store again if older than expire time.
     * <p>
     * If not provided, default value is {@link #DFLT_METRICS_EXPIRE_TIME}
     *
     * @param metricsExpireTime Metrics expire time.
     */
    @GridSpiConfiguration(optional = true)
    public void setMetricsExpireTime(int metricsExpireTime) {
        this.metricsExpireTime = metricsExpireTime;
    }

    /**
     * Requests metrics from store in case local cached metrics are outdated.
     *
     * @param nodeIds Nodes to request metrics for.
     * @return Metrics map.
     * @throws GridSpiException If any error occurs.
     */
    protected abstract Map<UUID, GridNodeMetrics> metrics0(Collection<UUID> nodeIds) throws GridSpiException;

    /**
     * Removes metrics from store.
     *
     * @param nodeIds Nodes to remove metrics of.
     * @throws GridSpiException If any error occurs.
     */
    protected abstract void removeMetrics0(Collection<UUID> nodeIds) throws GridSpiException;

    /**
     * @return SPI context.
     */
    protected GridSpiContext spiContext() {
        return spiCtx;
    }
}
