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

package org.gridgain.grid.cache.eviction.random;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.cache.eviction.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;

import static org.gridgain.grid.cache.GridCachePeekMode.*;

/**
 * Cache eviction policy which will select random cache entry for eviction if cache
 * size exceeds the {@link #getMaxSize()} parameter. This implementation is
 * extremely light weight, lock-free, and does not create any data structures to maintain
 * any order for eviction.
 * <p>
 * Random eviction will provide the best performance over any key set in which every
 * key has the same probability of being accessed.
 */
public class GridCacheRandomEvictionPolicy<K, V> implements GridCacheEvictionPolicy<K, V>,
    GridCacheRandomEvictionPolicyMBean {
    /** Maximum size. */
    private volatile int max = GridCacheConfiguration.DFLT_CACHE_SIZE;

    /**
     * Constructs random eviction policy with all defaults.
     */
    public GridCacheRandomEvictionPolicy() {
        // No-op.
    }

    /**
     * Constructs random eviction policy with maximum size.
     *
     * @param max Maximum allowed size of cache before entry will start getting evicted.
     */
    public GridCacheRandomEvictionPolicy(int max) {
        A.ensure(max > 0, "max > 0");

        this.max = max;
    }

    /**
     * Gets maximum allowed size of cache before entry will start getting evicted.
     *
     * @return Maximum allowed size of cache before entry will start getting evicted.
     */
    @Override public int getMaxSize() {
        return max;
    }

    /**
     * Sets maximum allowed size of cache before entry will start getting evicted.
     *
     * @param max Maximum allowed size of cache before entry will start getting evicted.
     */
    @Override public void setMaxSize(int max) {
        A.ensure(max > 0, "max > 0");

        this.max = max;
    }

    /** {@inheritDoc} */
    @Override public void onEntryAccessed(boolean rmv, GridCacheEntry<K, V> entry) {
        if (!entry.isCached())
            return;

        GridCache<K, V> cache = entry.projection().cache();

        int size = cache.size();

        for (int i = max; i < size; i++) {
            GridCacheEntry<K, V> e = cache.randomEntry();

            if (e != null)
                e.evict();
        }
    }

    /**
     * Checks entry for empty value.
     *
     * @param entry Entry to check.
     * @return {@code True} if entry is empty.
     */
    private boolean empty(GridCacheEntry<K, V> entry) {
        try {
            return entry.peek(F.asList(GLOBAL)) == null;
        }
        catch (GridException e) {
            U.error(null, e.getMessage(), e);

            assert false : "Should never happen: " + e;

            return false;
        }
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridCacheRandomEvictionPolicy.class, this);
    }
}
