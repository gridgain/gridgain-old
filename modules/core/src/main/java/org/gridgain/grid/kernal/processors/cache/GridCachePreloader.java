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
import org.gridgain.grid.lang.*;

import java.util.*;

/**
 * Cache preloader that is responsible for loading cache entries either from remote
 * nodes (for distributed cache) or anywhere else at cache startup.
 */
public interface GridCachePreloader<K, V> {
    /**
     * Starts preloading.
     *
     * @throws GridException If start failed.
     */
    public void start() throws GridException;

    /**
     * Stops preloading.
     */
    public void stop();

    /**
     * Kernal start callback.
     *
     * @throws GridException If failed.
     */
    public void onKernalStart() throws GridException;

    /**
     * Kernal stop callback.
     */
    public void onKernalStop();

    /**
     * @param p Preload predicate.
     */
    public void preloadPredicate(GridPredicate<GridCacheEntryInfo<K, V>> p);

    /**
     * @return Preload predicate. If not {@code null}, will evaluate each preloaded entry during
     *      send and receive, and if predicate evaluates to {@code false}, entry will be skipped.
     */
    public GridPredicate<GridCacheEntryInfo<K, V>> preloadPredicate();

    /**
     * @return Future which will complete when preloader is safe to use.
     */
    public GridFuture<?> startFuture();

    /**
     * @return Future which will complete when preloading is finished.
     */
    public GridFuture<?> syncFuture();

    /**
     * Requests that preloader sends the request for the key.
     *
     * @param keys Keys to request.
     * @param topVer Topology version, {@code -1} if not required.
     * @return Future to complete when all keys are preloaded.
     */
    public GridFuture<Object> request(Collection<? extends K> keys, long topVer);

    /**
     * Force preload process.
     */
    public void forcePreload();

    /**
     * Unwinds undeploys.
     */
    public void unwindUndeploys();
}
