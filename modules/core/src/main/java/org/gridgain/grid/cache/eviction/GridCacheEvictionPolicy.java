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
import org.gridgain.grid.cache.eviction.fifo.*;
import org.gridgain.grid.cache.eviction.lru.*;
import org.gridgain.grid.cache.eviction.random.*;

/**
 * Pluggable cache eviction policy. Usually, implementations will internally order
 * cache entries based on {@link #onEntryAccessed(boolean, GridCacheEntry)} notifications and
 * whenever an element needs to be evicted, {@link GridCacheEntry#evict()}
 * method should be called. If you need to access the underlying cache directly
 * from this policy, you can get it via {@link GridCacheEntry#projection()} method.
 * <p>
 * GridGain comes with following eviction policies out-of-the-box:
 * <ul>
 * <li>{@link GridCacheLruEvictionPolicy}</li>
 * <li>{@link GridCacheRandomEvictionPolicy}</li>
 * <li>{@link GridCacheFifoEvictionPolicy}</li>
 * </ul>
 * <p>
 * The eviction policy thread-safety is ensured by GridGain. Implementations of this interface should
 * not worry about concurrency and should be implemented as they were only accessed from one thread.
 * <p>
 * Note that implementations of all eviction policies provided by GridGain are very
 * light weight in a way that they are all lock-free (or very close to it), and do not
 * create any internal tables, arrays, or other expensive structures.
 * The eviction order is preserved by attaching light-weight meta-data to existing
 * cache entries.
 */
public interface GridCacheEvictionPolicy<K, V> {
    /**
     * Callback for whenever entry is accessed.
     *
     * @param rmv {@code True} if entry has been removed, {@code false} otherwise.
     * @param entry Accessed entry.
     */
    public void onEntryAccessed(boolean rmv, GridCacheEntry<K, V> entry);
}
