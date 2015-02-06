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

import java.util.*;

/**
 * Update future for atomic cache.
 */
public interface GridCacheAtomicFuture<K, R> extends GridCacheFuture<R> {
    /**
     * @return {@code True} if partition exchange should wait for this future to complete.
     */
    public boolean waitForPartitionExchange();

    /**
     * @return Future topology version.
     */
    public long topologyVersion();

    /**
     * @return Future keys.
     */
    public Collection<? extends K> keys();

    /**
     * Checks if timeout occurred.
     *
     * @param timeout Timeout to check.
     */
    public void checkTimeout(long timeout);
}
