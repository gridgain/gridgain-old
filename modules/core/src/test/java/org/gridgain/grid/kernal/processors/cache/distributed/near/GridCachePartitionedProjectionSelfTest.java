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
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.kernal.processors.cache.*;
import org.gridgain.grid.util.typedef.*;

import java.util.*;

import static org.gridgain.grid.cache.GridCacheFlag.*;
import static org.gridgain.grid.cache.GridCacheMode.*;

/**
 * Projection tests for partitioned cache.
 */
public class GridCachePartitionedProjectionSelfTest extends GridCacheAbstractProjectionSelfTest {
    /** {@inheritDoc} */
    @Override protected GridCacheConfiguration cacheConfiguration(String gridName) throws Exception {
        GridCacheConfiguration cc = super.cacheConfiguration(gridName);

        cc.setEvictSynchronized(false);
        cc.setEvictNearSynchronized(false);

        return cc;
    }

    /** {@inheritDoc} */
    @Override protected GridCacheMode cacheMode() {
        return PARTITIONED;
    }

    /**
     * @throws Exception In case of error.
     */
    public void testInvalidateFlag() throws Exception {
        long topVer = ((GridKernal)grid(0)).context().discovery().topologyVersion();

        try {
            // Top ver + 2.
            startGridsMultiThreaded(1, 2);

            awaitPartitionMapExchange();

            String key = "1";
            Integer val = Integer.valueOf(key);

            Grid primary = G.grid(cache(0).affinity().mapKeyToNode(key).id());

            Collection<GridNode> affNodes = cache(0).affinity().mapKeyToPrimaryAndBackups(key);

            Grid near = G.grid(F.first(F.view(grid(0).nodes(), F.notIn(affNodes))).id());

            assert primary != null;
            assert near != null;

            // Populate near cache.
            near.cache(null).flagsOn(INVALIDATE).put(key, val);

            // The entry is either in near cache, or otherwise it is in
            // DHT primary or backup caches. Since we have 3 nodes, all
            // peek operations should return non-null value.
            for (int i = 0; i < 3; i++) {
                if (grid(i) != near)
                    assertNotNull(grid(i).<String, Integer>cache(null).peek(key));
                else {
                    if (nearEnabled())
                        assertNotNull(grid(i).<String, Integer>cache(null).peek(key));
                    else
                        assertNull(grid(i).<String, Integer>cache(null).peek(key));
                }
            }

            // Invalidate near.
            primary.cache(null).flagsOn(INVALIDATE).put(key, val);

            for (int i = 0; i < 3; i++) {
                if (grid(i) != near)
                    assertNotNull(grid(i).<String, Integer>cache(null).peek(key));
                else
                    assertNull(grid(i).<String, Integer>cache(null).peek(key));
            }
        }
        finally {
            // Top ver + 2.
            for (int i = 1; i < 3; i++)
                stopGrid(i);

            for (Grid g : G.allGrids()) {
                GridKernal grid = (GridKernal)g;

                // Wait until all nodes get topology version event.
                grid.context().discovery().topologyFuture(topVer + 4).get();
            }
        }
    }

    /** {@inheritDoc} */
    @Override public void testSkipSwapFlag() throws Exception {
        cache().put("key", 1);

        cache().evict("key");

        assert cache().peek("key") == null;

        Integer one = 1;

        assertEquals(one, cache().get("key"));

        cache().evict("key");

        assertEquals(one, cache().reload("key"));

        cache().remove("key");

        assertFalse(cache().containsKey("key"));
        assertNull(cache().get("key"));

        GridCacheProjection<String, Integer> prj = cache().flagsOn(SKIP_SWAP, SKIP_STORE);

        prj.put("key", 1);

        assertEquals(one, prj.get("key"));
        assertEquals(one, prj.peek("key"));

        // Entry will be evicted since evictions are not synchronized.
        assert prj.evict("key");

        assertNull(prj.peek("key"));
        assertNull(prj.get("key"));

        assertNull(prj.remove("key"));

        assert prj.peek("key") == null;
        assert prj.get("key") == null;
    }
}
