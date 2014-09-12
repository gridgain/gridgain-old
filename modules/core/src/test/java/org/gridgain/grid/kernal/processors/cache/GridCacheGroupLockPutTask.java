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
import org.gridgain.grid.cache.*;
import org.gridgain.grid.compute.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.util.typedef.*;
import org.jetbrains.annotations.*;

import java.util.*;

import static org.gridgain.grid.cache.GridCacheTxConcurrency.*;
import static org.gridgain.grid.cache.GridCacheTxIsolation.*;

/**
 * Puts all the passed data into partitioned cache in small chunks.
 */
class GridCacheGroupLockPutTask extends GridComputeTaskAdapter<Collection<Integer>, Void> {
    /** Preferred node. */
    private final UUID preferredNode;

    /** Cache name. */
    private final String cacheName;

    /** Optimistic transaction flag. */
    private final boolean optimistic;

    /**
     *
     * @param preferredNode A node that we'd prefer to take from grid.
     * @param cacheName A name of the cache to work with.
     * @param optimistic Optimistic transaction flag.
     */
    GridCacheGroupLockPutTask(UUID preferredNode, String cacheName, boolean optimistic) {
        this.preferredNode = preferredNode;
        this.cacheName = cacheName;
        this.optimistic = optimistic;
    }

    /**
     * This method is called to map or split grid task into multiple grid jobs. This is the first method that gets called
     * when task execution starts.
     *
     * @param data     Task execution argument. Can be {@code null}. This is the same argument as the one passed into {@code
     *                Grid#execute(...)} methods.
     * @param subgrid Nodes available for this task execution. Note that order of nodes is guaranteed to be randomized by
     *                container. This ensures that every time you simply iterate through grid nodes, the order of nodes
     *                will be random which over time should result into all nodes being used equally.
     * @return Map of grid jobs assigned to subgrid node. Unless {@link GridComputeTaskContinuousMapper} is injected into task, if
     *         {@code null} or empty map is returned, exception will be thrown.
     * @throws GridException If mapping could not complete successfully. This exception will be thrown out of {@link
     *                       GridComputeTaskFuture#get()} method.
     */
    @Override public Map<? extends GridComputeJob, GridNode> map(List<GridNode> subgrid,
        @Nullable final Collection<Integer> data) throws GridException {
        assert !subgrid.isEmpty();

        // Give preference to wanted node. Otherwise, take the first one.
        GridNode targetNode = F.find(subgrid, subgrid.get(0), new GridPredicate<GridNode>() {
            @Override public boolean apply(GridNode e) {
                return preferredNode.equals(e.id());
            }
        });

        return Collections.singletonMap(
            new GridComputeJobAdapter() {
                @GridLoggerResource
                private GridLogger log;

                @GridInstanceResource
                private Grid grid;

                @Override public Object execute() throws GridException {
                    log.info("Going to put data: " + data.size());

                    GridCache<Object, Object> cache = grid.cache(cacheName);

                    assert cache != null;

                    Map<Integer, T2<Integer, Collection<Integer>>> putMap = groupData(data);

                    for (Map.Entry<Integer, T2<Integer, Collection<Integer>>> entry : putMap.entrySet()) {
                        T2<Integer, Collection<Integer>> pair = entry.getValue();

                        Object affKey = pair.get1();

                        // Group lock partition.
                        try (GridCacheTx tx = cache.txStartPartition(cache.affinity().partition(affKey),
                            optimistic ? OPTIMISTIC : PESSIMISTIC, REPEATABLE_READ, 0, pair.get2().size())) {
                            for (Integer val : pair.get2())
                                cache.put(val, val);

                            tx.commit();
                        }
                    }

                    log.info("Finished put data: " + data.size());

                    return data;
                }

                /**
                 * Groups values by partitions.
                 *
                 * @param data Data to put.
                 * @return Grouped map.
                 */
                private Map<Integer, T2<Integer, Collection<Integer>>> groupData(Iterable<Integer> data) {
                    GridCache<Object, Object> cache = grid.cache(cacheName);

                    Map<Integer, T2<Integer, Collection<Integer>>> res = new HashMap<>();

                    for (Integer val : data) {
                        int part = cache.affinity().partition(val);

                        T2<Integer, Collection<Integer>> tup = res.get(part);

                        if (tup == null) {
                            tup = new T2<Integer, Collection<Integer>>(val, new LinkedList<Integer>());

                            res.put(part, tup);
                        }

                        tup.get2().add(val);
                    }

                    return res;
                }
            },
            targetNode);
    }

    /** {@inheritDoc} */
    @Nullable @Override public Void reduce(List<GridComputeJobResult> results) throws GridException {
        return null;
    }
}
