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

package org.gridgain.grid.kernal.processors.cache.distributed.near;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.cache.affinity.*;
import org.gridgain.grid.cache.affinity.rendezvous.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.testframework.junits.common.*;

import java.util.*;

import static org.gridgain.grid.cache.GridCacheDistributionMode.*;

/**
 * Tests rendezvous affinity function with CLIENT_ONLY node (GG-8768).
 */
public class GridCacheRendezvousAffinityClientSelfTest extends GridCommonAbstractTest {
    /** Client node. */
    private boolean client;

    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        GridCacheConfiguration ccfg = defaultCacheConfiguration();

        ccfg.setCacheMode(GridCacheMode.PARTITIONED);
        ccfg.setBackups(1);
        ccfg.setAffinity(new GridCacheRendezvousAffinityFunction());

        if (client)
            ccfg.setDistributionMode(CLIENT_ONLY);

        cfg.setCacheConfiguration(ccfg);

        return cfg;
    }

    /**
     * @throws Exception If failed.
     */
    public void testClientNode() throws Exception {
        try {
            client = true;

            startGrid(0);

            client = false;

            startGrid(1);
            startGrid(2);
            startGrid(3);

            Map<Integer, Collection<UUID>> mapping = new HashMap<>();

            for (int i = 0; i < 4; i++) {
                GridCache<Object, Object> cache = grid(i).cache(null);

                GridCacheAffinity<Object> aff = cache.affinity();

                int parts = aff.partitions();

                for (int p = 0; p < parts; p++) {
                    Collection<GridNode> nodes = aff.mapPartitionToPrimaryAndBackups(p);

                    assertEquals(2, nodes.size());

                    Collection<UUID> cur = mapping.get(p);

                    if (cur == null)
                        mapping.put(p, F.nodeIds(nodes));
                    else {
                        Iterator<UUID> nodesIt = F.nodeIds(nodes).iterator();

                        for (UUID curNode : cur) {
                            UUID node = nodesIt.next();

                            assertEquals(curNode, node);
                        }
                    }
                }
            }
        }
        finally {
            stopAllGrids();
        }
    }
}
