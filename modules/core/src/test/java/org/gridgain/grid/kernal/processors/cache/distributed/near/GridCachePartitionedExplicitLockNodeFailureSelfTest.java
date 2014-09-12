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
import org.gridgain.grid.cache.affinity.consistenthash.*;
import org.gridgain.grid.events.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.testframework.junits.common.*;

import java.util.*;

import static org.gridgain.grid.cache.GridCacheAtomicityMode.*;
import static org.gridgain.grid.cache.GridCacheMode.*;
import static org.gridgain.grid.cache.GridCacheDistributionMode.*;
import static org.gridgain.grid.events.GridEventType.*;

/**
 * Tests for node failure in transactions.
 */
public class GridCachePartitionedExplicitLockNodeFailureSelfTest extends GridCommonAbstractTest {
    /** */
    private static GridTcpDiscoveryIpFinder ipFinder = new GridTcpDiscoveryVmIpFinder(true);

    /** */
    public static final int GRID_CNT = 4;

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration c = super.getConfiguration(gridName);

        GridTcpDiscoverySpi disco = new GridTcpDiscoverySpi();

        disco.setIpFinder(ipFinder);

        c.setDiscoverySpi(disco);

        GridCacheConfiguration cc = defaultCacheConfiguration();

        cc.setCacheMode(PARTITIONED);
        cc.setDgcFrequency(0);
        cc.setTxSerializableEnabled(true);
        cc.setWriteSynchronizationMode(GridCacheWriteSynchronizationMode.FULL_SYNC);
        cc.setBackups(GRID_CNT - 1);
        cc.setAtomicityMode(TRANSACTIONAL);
        cc.setDistributionMode(NEAR_PARTITIONED);

        c.setCacheConfiguration(cc);

        return c;
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        super.afterTest();

        stopAllGrids();
    }

    /** @throws Exception If check failed. */
    @SuppressWarnings("ErrorNotRethrown")
    public void testLockFromNearOrBackup() throws Exception {
        startGrids(GRID_CNT);

        int idx = 0;

        info("Grid will be stopped: " + idx);

        Integer key = 0;

        while (grid(idx).mapKeyToNode(null, key).id().equals(grid(0).localNode().id()))
            key++;

        GridNode node = grid(idx).mapKeyToNode(null, key);

        info("Primary node for key [id=" + node.id() + ", order=" + node.order() + ", key=" + key + ']');

        GridCache<Integer, String> cache = cache(idx);

        cache.put(key, "val");

        assert cache.lock(key, -1);

        for (int checkIdx = 1; checkIdx < GRID_CNT; checkIdx++) {
            info("Check grid index: " + checkIdx);

            GridCache<Integer, String> checkCache = cache(checkIdx);

            assert !checkCache.lock(key, -1);

            GridCacheEntry e = checkCache.entry(key);

            assert e.isLocked() : "Entry is not locked for grid [idx=" + checkIdx + ", entry=" + e + ']';
        }

        Collection<GridFuture<?>> futs = new LinkedList<>();

        for (int i = 1; i < GRID_CNT; i++) {
            futs.add(
                grid(i).events().waitForLocal(new P1<GridEvent>() {
                    @Override public boolean apply(GridEvent e) {
                        info("Received grid event: " + e);

                        return true;
                    }
                }, EVT_NODE_LEFT));
        }

        stopGrid(idx);

        for (GridFuture<?> fut : futs)
            fut.get();

        for (int i = 0; i < 3; i++) {
            try {
                for (int checkIdx = 1; checkIdx < GRID_CNT; checkIdx++) {
                    info("Check grid index: " + checkIdx);

                    GridCache<Integer, String> checkCache = cache(checkIdx);

                    GridCacheEntry e = checkCache.entry(key);

                    info("Checking entry: " + e);

                    assert !e.isLocked() : "Entry is locked for grid [idx=" + checkIdx + ", entry=" + e + ']';
                }
            }
            catch (AssertionError e) {
                if (i == 2)
                    throw e;

                U.warn(log, "Check failed (will retry in 1000 ms): " + e.getMessage());

                U.sleep(1000);

                continue;
            }

            // Check passed on all grids.
            break;
        }
    }
}
