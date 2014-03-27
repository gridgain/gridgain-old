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

package org.gridgain.grid.cache.eviction;

import org.gridgain.grid.cache.*;
import org.gridgain.grid.lang.*;

/**
 * Eviction filter to specify which entries should not be evicted. Not applicable when
 * calling explicit evict via {@link GridCacheEntry#evict()}.
 * If {@link #evictAllowed(GridCacheEntry)} method returns {@code false} then eviction
 * policy will not be notified and entry will never be evicted.
 * <p>
 * Eviction filter can be configured via {@link GridCacheConfiguration#getEvictionFilter()}
 * configuration property. Default value is {@code null} which means that all
 * cache entries will be tracked by eviction policy.
 */
public interface GridCacheEvictionFilter<K, V> {
    /**
     * Checks if entry may be evicted from cache.
     *
     * @param entry Cache entry.
     * @return {@code True} if it is allowed to evict this entry.
     */
    public boolean evictAllowed(GridCacheEntry<K, V> entry);
}
