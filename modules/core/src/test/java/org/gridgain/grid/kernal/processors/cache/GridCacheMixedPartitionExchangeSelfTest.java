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
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.testframework.*;
import org.gridgain.testframework.junits.common.*;

import java.util.*;
import java.util.concurrent.atomic.*;

import static org.gridgain.grid.cache.GridCacheAtomicityMode.*;
import static org.gridgain.grid.cache.GridCacheDistributionMode.*;
import static org.gridgain.grid.cache.GridCacheMode.*;
import static org.gridgain.grid.cache.GridCacheTxConcurrency.*;
import static org.gridgain.grid.cache.GridCacheTxIsolation.*;

/**
 * Test case checks partition exchange when non-cache node joins topology (partition
 * exchange should be skipped in this case).
 */
public class GridCacheMixedPartitionExchangeSelfTest extends GridCommonAbstractTest {
    /** Flag indicating whether to include cache to the node configuration. */
    private boolean cache;

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        if (cache)
            cfg.setCacheConfiguration(cacheConfiguration());

        return cfg;
    }

    /**
     * @return Cache configuration.
     */
    private GridCacheConfiguration cacheConfiguration() {
        GridCacheConfiguration ccfg = defaultCacheConfiguration();

        ccfg.setCacheMode(PARTITIONED);
        ccfg.setAtomicityMode(TRANSACTIONAL);
        ccfg.setDistributionMode(PARTITIONED_ONLY);
        ccfg.setBackups(1);

        return ccfg;
    }

    /**
     * @throws Exception If failed.
     */
    public void testNodeJoinLeave() throws Exception {
        try {
            cache = true;

            startGrids(4);

            awaitPartitionMapExchange();

            final AtomicBoolean finished = new AtomicBoolean();

            GridFuture<Long> fut = GridTestUtils.runMultiThreadedAsync(new GridCallable<Object>() {
                @Override public Object call() throws Exception {
                    Random rnd = new Random();

                    int keys = 100;

                    while (!finished.get()) {
                        int g = rnd.nextInt(4);

                        int key = rnd.nextInt(keys);

                        GridCache<Integer, Integer> prj = grid(g).cache(null);

                        try (GridCacheTx tx = prj.txStart(PESSIMISTIC, REPEATABLE_READ)) {
                            Integer val = prj.get(key);

                            val = val == null ? 1 : val + 1;

                            prj.put(key, val);

                            tx.commit();
                        }
                    }

                    return null;
                }
            }, 4, "async-runner");

            cache = false;

            for (int r = 0; r < 3; r++) {
                for (int i = 4; i < 8; i++)
                    startGrid(i);

                for (int i = 4; i < 8; i++)
                    stopGrid(i);
            }

            // Check we can start more cache nodes after non-cache ones.
            cache = true;

            startGrid(4);

            U.sleep(500);

            finished.set(true);

            fut.get();

            long topVer = grid(0).topologyVersion();

            assertEquals(29, topVer);

            // Check all grids have all exchange futures completed.
            for (int i = 0; i < 4; i++) {
                GridKernal grid = (GridKernal)grid(i);

                GridCacheContext<Object, Object> cctx = grid.internalCache(null).context();

                GridFuture<Long> verFut = cctx.affinity().affinityReadyFuture(topVer);

                assertEquals((Long)topVer, verFut.get());
                assertEquals((Long)topVer, cctx.topologyVersionFuture().get());
            }
        }
        finally {
            stopAllGrids();
        }
    }
}
