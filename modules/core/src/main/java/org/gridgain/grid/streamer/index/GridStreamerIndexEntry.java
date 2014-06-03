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

package org.gridgain.grid.streamer.index;


import org.jetbrains.annotations.*;

import java.util.*;

/**
 * Streamer index entry. Individual index entry contains index key, value, and all events
 * associated with given key.
 *
 */
public interface GridStreamerIndexEntry<E, K, V> {
    /**
     * Gets events associated with given index key and value.
     * <p>
     * Events are tracked only if {@link GridStreamerIndexProvider#getPolicy()}
     * is set to {@link GridStreamerIndexPolicy#EVENT_TRACKING_ON} or
     * {@link GridStreamerIndexPolicy#EVENT_TRACKING_ON_DEDUP}.
     *
     * @return Events associated with given index key and value or {@code null} if event tracking is off.
     */
    @Nullable public Collection<E> events();

    /**
     * Gets index entry key.
     *
     * @return Index entry key.
     */
    public K key();

    /**
     * Gets index entry value.
     * <p>
     * For sorted indexes, the sorting happens based on this value.
     *
     * @return Index entry value.
     */
    public V value();
}
