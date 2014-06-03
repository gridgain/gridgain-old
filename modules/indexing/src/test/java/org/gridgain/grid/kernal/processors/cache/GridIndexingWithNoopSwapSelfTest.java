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
import org.gridgain.grid.cache.affinity.consistenthash.*;
import org.gridgain.grid.cache.eviction.fifo.*;
import org.gridgain.grid.cache.query.*;
import org.gridgain.grid.kernal.processors.cache.GridCacheAbstractQuerySelfTest.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.grid.spi.indexing.h2.*;
import org.gridgain.grid.spi.swapspace.noop.*;
import org.gridgain.testframework.junits.common.*;

import java.util.*;

import static org.gridgain.grid.cache.GridCacheAtomicityMode.*;
import static org.gridgain.grid.cache.GridCacheMode.*;
import static org.gridgain.grid.cache.GridCachePreloadMode.*;

/**
 * GG-4368
 */
public class GridIndexingWithNoopSwapSelfTest extends GridCommonAbstractTest {
    /** */
    private static final GridTcpDiscoveryIpFinder ipFinder = new GridTcpDiscoveryVmIpFinder(true);

    /** */
    protected Grid grid;

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration c = super.getConfiguration(gridName);

        GridTcpDiscoverySpi disco = new GridTcpDiscoverySpi();

        disco.setIpFinder(ipFinder);

        c.setDiscoverySpi(disco);

        GridH2IndexingSpi indexing = new GridH2IndexingSpi();

        indexing.setDefaultIndexPrimitiveKey(true);

        c.setIndexingSpi(indexing);

        c.setSwapSpaceSpi(new GridNoopSwapSpaceSpi());

        GridCacheConfiguration cc = defaultCacheConfiguration();

        cc.setCacheMode(PARTITIONED);
        cc.setWriteSynchronizationMode(GridCacheWriteSynchronizationMode.FULL_SYNC);
        cc.setPreloadMode(SYNC);
        cc.setSwapEnabled(true);
        cc.setDistributionMode(GridCacheDistributionMode.NEAR_PARTITIONED);
        cc.setEvictNearSynchronized(false);
        cc.setEvictionPolicy(new GridCacheFifoEvictionPolicy(1000));
        cc.setBackups(1);
        cc.setAtomicityMode(TRANSACTIONAL);

        c.setCacheConfiguration(cc);

        return c;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        grid = startGrid();
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        stopAllGrids();

        grid = null;
    }

    /** @throws Exception If failed. */
    public void testQuery() throws Exception {
        GridCache<Integer, ObjectValue> cache = grid.cache(null);

        int cnt = 10;

        for (int i = 0; i < cnt; i++)
            cache.putx(i, new ObjectValue("test" + i, i));

        for (int i = 0; i < cnt; i++) {
            assertNotNull(cache.peek(i));

            cache.evict(i); // Swap.
        }

        GridCacheQuery<Map.Entry<Integer, ObjectValue>> qry =
            cache.queries().createSqlQuery(ObjectValue.class, "intVal >= ? order by intVal");

        qry.enableDedup(true);

        assertEquals(0, qry.execute(0).get().size());
    }
}
