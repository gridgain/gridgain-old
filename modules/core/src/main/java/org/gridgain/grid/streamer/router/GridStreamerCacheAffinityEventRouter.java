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

package org.gridgain.grid.streamer.router;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.streamer.*;
import org.jetbrains.annotations.*;

/**
 * Router used to colocate streamer events with data stored in a partitioned cache.
 * <h1 class="header">Affinity Key</h1>
 * Affinity key for collocation of event together on the same node is specified
 * via {@link CacheAffinityEvent#affinityKey()} method. If event does not implement
 * {@link CacheAffinityEvent} interface, then event will be routed always to local node.
 */
public class GridStreamerCacheAffinityEventRouter extends GridStreamerEventRouterAdapter {
    /**
     * All events that implement this interface will be routed based on key affinity.
     */
    @SuppressWarnings("PublicInnerClass")
    public interface CacheAffinityEvent {
        /**
         * @return Affinity route key for the event.
         */
        public Object affinityKey();

        /**
         * @return Cache name, if {@code null}, the default cache is used.
         */
        @Nullable public String cacheName();
    }

    /** Grid instance. */
    @GridInstanceResource
    private Grid grid;

    /** {@inheritDoc} */
    @Override public <T> GridNode route(GridStreamerContext ctx, String stageName, T evt) {
        if (evt instanceof CacheAffinityEvent) {
            CacheAffinityEvent e = (CacheAffinityEvent)evt;

            GridCache<Object, Object> c = ((GridEx)grid).cachex(e.cacheName());

            assert c != null;

            return c.affinity().mapKeyToNode(e.affinityKey());
        }

        return grid.localNode();
    }
}
