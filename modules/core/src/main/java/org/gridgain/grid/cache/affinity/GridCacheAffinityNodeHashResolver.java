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

import java.io.*;

/**
 * Resolver which is used to provide node hash value for affinity function.
 * <p>
 * Node IDs constantly change when nodes get restarted, which causes affinity mapping to change between restarts,
 * and hence causing redundant repartitioning. Providing an alternate node hash value, which survives node restarts,
 * will help to map keys to the same nodes whenever possible.
 */
public interface GridCacheAffinityNodeHashResolver extends Serializable {
    /**
     * Resolve alternate hash value for the given Grid node.
     *
     * @param node Grid node.
     * @return Resolved hash ID.
     */
    public Object resolve(GridNode node);
}
