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

import org.gridgain.grid.*;
import org.gridgain.grid.events.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * Affinity function context. This context is passed to {@link GridCacheAffinityFunction} for
 * partition reassignment on every topology change event.
 */
public interface GridCacheAffinityFunctionContext {
    /**
     * Gets affinity assignment for given partition on previous topology version. First node in returned list is
     * a primary node, other nodes are backups.
     *
     * @param part Partition to get previous assignment for.
     * @return List of nodes assigned to given partition on previous topology version or {@code null}
     *      if this information is not available.
     */
    @Nullable public List<GridNode> previousAssignment(int part);

    /**
     * Gets number of backups for new assignment.
     *
     * @return Number of backups for new assignment.
     */
    public int backups();

    /**
     * Gets current topology snapshot. Snapshot will contain only nodes on which particular cache is configured.
     * List of passed nodes is guaranteed to be sorted in a same order on all nodes on which partition assignment
     * is performed.
     *
     * @return Cache topology snapshot.
     */
    public List<GridNode> currentTopologySnapshot();

    /**
     * Gets current topology version number.
     *
     * @return Current topology version number.
     */
    public long currentTopologyVersion();

    /**
     * Gets discovery event caused topology change.
     *
     * @return Discovery event caused latest topology change or {@code null} if this information is
     *      not available.
     */
    @Nullable public GridDiscoveryEvent discoveryEvent();
}
