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

package org.gridgain.grid.dr;

import org.jetbrains.annotations.*;

/**
 * Data center replication entry.
 */
public interface GridDrEntry<K, V> {
    /**
     * Gets entry's key.
     *
     * @return Entry's key.
     */
    public K key();

    /**
     * Gets entry's value.
     *
     * @return Entry's value.
     */
    @Nullable public V value();

    /**
     * Gets entry's TTL.
     *
     * @return Entry's TTL.
     */
    public long ttl();

    /**
     * Gets entry's expire time.
     *
     * @return Entry's expire time.
     */
    public long expireTime();

    /**
     * Gets ID of initiator data center.
     *
     * @return ID of initiator data center.
     */
    public byte dataCenterId();

    /**
     * Gets entry's topology version in initiator data center.
     *
     * @return Entry's topology version in initiator data center.
     */
    public int topologyVersion();

    /**
     * Gets entry's order in initiator data center.
     *
     * @return Entry's order in initiator data center
     */
    public long order();

    /**
     * Gets entry's global time in initiator data center.
     *
     * @return Entry's global time in initiator data center
     */
    public long globalTime();
}
