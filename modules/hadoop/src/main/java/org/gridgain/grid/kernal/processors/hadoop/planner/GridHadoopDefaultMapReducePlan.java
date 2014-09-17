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

package org.gridgain.grid.kernal.processors.hadoop.planner;

import org.gridgain.grid.hadoop.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * Map-reduce plan.
 */
public class GridHadoopDefaultMapReducePlan implements GridHadoopMapReducePlan {
    /** */
    private static final long serialVersionUID = 0L;

    /** Mappers map. */
    private Map<UUID, Collection<GridHadoopInputSplit>> mappers;

    /** Reducers map. */
    private Map<UUID, int[]> reducers;

    /** Mappers count. */
    private int mappersCnt;

    /** Reducers count. */
    private int reducersCnt;

    /**
     * @param mappers Mappers map.
     * @param reducers Reducers map.
     */
    public GridHadoopDefaultMapReducePlan(Map<UUID, Collection<GridHadoopInputSplit>> mappers,
        Map<UUID, int[]> reducers) {
        this.mappers = mappers;
        this.reducers = reducers;

        if (mappers != null) {
            for (Collection<GridHadoopInputSplit> splits : mappers.values())
                mappersCnt += splits.size();
        }

        if (reducers != null) {
            for (int[] rdcrs : reducers.values())
                reducersCnt += rdcrs.length;
        }
    }

    /** {@inheritDoc} */
    @Override public int mappers() {
        return mappersCnt;
    }

    /** {@inheritDoc} */
    @Override public int reducers() {
        return reducersCnt;
    }

    /** {@inheritDoc} */
    @Override public UUID nodeForReducer(int reducer) {
        assert reducer >= 0 && reducer < reducersCnt : reducer;

        for (Map.Entry<UUID, int[]> entry : reducers.entrySet()) {
            for (int r : entry.getValue()) {
                if (r == reducer)
                    return entry.getKey();
            }
        }

        throw new IllegalStateException("Not found reducer index: " + reducer);
    }

    /** {@inheritDoc} */
    @Override @Nullable public Collection<GridHadoopInputSplit> mappers(UUID nodeId) {
        return mappers.get(nodeId);
    }

    /** {@inheritDoc} */
    @Override @Nullable public int[] reducers(UUID nodeId) {
        return reducers.get(nodeId);
    }

    /** {@inheritDoc} */
    @Override public Collection<UUID> mapperNodeIds() {
        return mappers.keySet();
    }

    /** {@inheritDoc} */
    @Override public Collection<UUID> reducerNodeIds() {
        return reducers.keySet();
    }
}
