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

package org.gridgain.grid.kernal.processors.cache.distributed.dht;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.cache.affinity.*;
import org.gridgain.grid.events.*;
import org.gridgain.grid.kernal.processors.cache.distributed.*;
import org.gridgain.grid.kernal.processors.cache.distributed.dht.preloader.*;
import org.gridgain.grid.util.typedef.*;

import java.util.*;

import static org.gridgain.grid.cache.GridCacheMode.*;
import static org.gridgain.grid.cache.GridCachePreloadMode.*;
import static org.gridgain.grid.events.GridEventType.*;

/**
 *
 */
public class GridCachePartitionedPreloadEventsSelfTest extends GridCachePreloadEventsAbstractSelfTest {
    /** */
    private boolean replicatedAffinity = true;

    /** */
    private GridCachePreloadMode preloadMode = SYNC;

    /** {@inheritDoc} */
    @Override protected GridCacheConfiguration cacheConfiguration() {
        GridCacheConfiguration cacheCfg = super.cacheConfiguration();

        if (replicatedAffinity)
            // replicate entries to all nodes
            cacheCfg.setAffinity(new GridCacheAffinityFunction() {
                /** {@inheritDoc} */
                @Override public void reset() {
                }

                /** {@inheritDoc} */
                @Override public int partitions() {
                    return 1;
                }

                /** {@inheritDoc} */
                @Override public int partition(Object key) {
                    return 0;
                }

                /** {@inheritDoc} */
                @Override public List<List<GridNode>> assignPartitions(GridCacheAffinityFunctionContext affCtx) {
                    List<GridNode> nodes = new ArrayList<>(affCtx.currentTopologySnapshot());

                    return Collections.singletonList(nodes);
                }

                /** {@inheritDoc} */
                @Override public void removeNode(UUID nodeId) {
                }
            });

        cacheCfg.setPreloadMode(preloadMode);

        return cacheCfg;
    }

    /** {@inheritDoc} */
    @Override protected GridCacheMode getCacheMode() {
        return PARTITIONED;
    }

    /**
     * Test events fired from
     * {@link GridDhtForceKeysFuture}
     *
     * @throws Exception if failed.
     */
    public void testForcePreload() throws Exception {
        replicatedAffinity = false;
        preloadMode = NONE;

        Grid g1 = startGrid("g1");

        Collection<Integer> keys = new HashSet<>();

        GridCache<Integer, String> cache = g1.cache(null);

        for (int i = 0; i < 100; i++) {
            keys.add(i);
            cache.put(i, "val");
        }

        Grid g2 = startGrid("g2");

        Map<GridNode, Collection<Object>> keysMap = g1.cache(null).affinity().mapKeysToNodes(keys);
        Collection<Object> g2Keys = keysMap.get(g2.localNode());

        assertNotNull(g2Keys);
        assertFalse("There are no keys assigned to g2", g2Keys.isEmpty());

        for (Object key : g2Keys)
            g2.cache(null).put(key, "changed val");

        Collection<GridEvent> evts = g2.events().localQuery(F.<GridEvent>alwaysTrue(), EVT_CACHE_PRELOAD_OBJECT_LOADED);

        checkPreloadEvents(evts, g2, g2Keys);
    }
}
