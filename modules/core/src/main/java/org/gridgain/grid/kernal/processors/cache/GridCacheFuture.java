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

import java.util.*;

/**
 * This interface should be implemented by all distributed futures.
 */
public interface GridCacheFuture<R> extends GridFuture<R> {
    /**
     * @return Unique identifier for this future.
     */
    public GridUuid futureId();

    /**
     * @return Future version.
     */
    public GridCacheVersion version();

    /**
     * @return Involved nodes.
     */
    public Collection<? extends GridNode> nodes();

    /**
     * Callback for when node left.
     *
     * @param nodeId Left node ID.
     * @return {@code True} if future cared about this node.
     */
    public boolean onNodeLeft(UUID nodeId);

    /**
     * @return {@code True} if future should be tracked.
     */
    public boolean trackable();

    /**
     * Marks this future as non-trackable.
     */
    public void markNotTrackable();
}
