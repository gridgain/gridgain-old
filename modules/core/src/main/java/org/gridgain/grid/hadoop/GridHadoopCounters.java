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

package org.gridgain.grid.hadoop;

import java.util.*;

/**
 * Counters store.
 */
public interface GridHadoopCounters {
    /**
     * Returns counter for the specified group and counter name. Creates new if it does not exist.
     *
     * @param grp Counter group name.
     * @param name Counter name.
     * @param cls Class for new instance creation if it's needed.
     * @return The counter that was found or added or {@code null} if create is false.
     */
    <T extends GridHadoopCounter> T counter(String grp, String name, Class<T> cls);

    /**
     * Returns all existing counters.
     *
     * @return Collection of counters.
     */
    Collection<GridHadoopCounter> all();

    /**
     * Merges all counters from another store with existing counters.
     *
     * @param other Counters to merge with.
     */
    void merge(GridHadoopCounters other);
}
