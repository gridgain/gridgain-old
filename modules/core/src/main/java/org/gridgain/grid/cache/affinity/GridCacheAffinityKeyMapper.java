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

package org.gridgain.grid.cache.affinity;

import org.gridgain.grid.cache.*;

import java.io.*;

/**
 * Affinity mapper which maps cache key to an affinity key. Affinity key is a key which will be
 * used to determine a node on which this key will be cached. Every cache key will first be passed
 * through {@link #affinityKey(Object)} method, and the returned value of this method
 * will be given to {@link GridCacheAffinityFunction} implementation to find out key-to-node affinity.
 * <p>
 * The default implementation, which will be used if no explicit affinity mapper is specified
 * in cache configuration, will first look for any field or method annotated with
 * {@link GridCacheAffinityKeyMapped @GridCacheAffinityKeyMapped} annotation. If such field or method
 * is not found, then the cache key itself will be returned from {@link #affinityKey(Object) affinityKey(Object)}
 * method (this means that all objects with the same cache key will always be routed to the same node).
 * If such field or method is found, then the value of this field or method will be returned from
 * {@link #affinityKey(Object) affinityKey(Object)} method. This allows to specify alternate affinity key, other
 * than the cache key itself, whenever needed.
 * <p>
 * A custom (other than default) affinity mapper can be provided
 * via {@link GridCacheConfiguration#getAffinityMapper()} configuration property.
 * <p>
 * For more information on affinity mapping and examples refer to {@link GridCacheAffinityFunction} and
 * {@link GridCacheAffinityKeyMapped @GridCacheAffinityKeyMapped} documentation.
 * @see GridCacheAffinityFunction
 * @see GridCacheAffinityKeyMapped
 */
public interface GridCacheAffinityKeyMapper extends Serializable {
    /**
     * Maps passed in key to an alternate key which will be used for node affinity.
     *
     * @param key Key to map.
     * @return Key to be used for node-to-affinity mapping (may be the same
     *      key as passed in).
     */
    public Object affinityKey(Object key);

    /**
     * Resets cache affinity mapper to its initial state. This method will be called by
     * the system any time the affinity mapper has been sent to remote node where
     * it has to be reinitialized. If your implementation of affinity mapper
     * has no initialization logic, leave this method empty.
     */
    public void reset();
}
