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
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.kernal.processors.cache.*;
import org.gridgain.grid.kernal.processors.cache.distributed.dht.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.testframework.junits.common.*;

import java.util.*;

import static org.gridgain.grid.cache.GridCacheAtomicityMode.*;
import static org.gridgain.grid.cache.GridCacheMode.*;
import static org.gridgain.grid.cache.GridCacheDistributionMode.*;
import static org.gridgain.grid.cache.GridCachePreloadMode.*;
import static org.gridgain.grid.cache.GridCacheTxConcurrency.*;
import static org.gridgain.grid.cache.GridCacheTxIsolation.*;
import static org.gridgain.grid.cache.GridCacheWriteSynchronizationMode.*;

/**
 * Tests near transactions.
 */
public class GridCacheNearTxMultiNodeSelfTest extends GridCommonAbstractTest {
    /** */
    protected static GridTcpDiscoveryIpFinder ipFinder = new GridTcpDiscoveryVmIpFinder(true);

    /** */
    private static final int GRID_CNT = 3;

    /** Number of backups for partitioned tests. */
    protected int backups = 1;

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        // Default cache configuration.
        GridCacheConfiguration cacheCfg = defaultCacheConfiguration();

        cacheCfg.setCacheMode(PARTITIONED);
        cacheCfg.setAtomicityMode(TRANSACTIONAL);
        cacheCfg.setDistributionMode(NEAR_PARTITIONED);
        cacheCfg.setWriteSynchronizationMode(FULL_SYNC);
        cacheCfg.setBackups(backups);
        cacheCfg.setPreloadMode(SYNC);

        cfg.setCacheConfiguration(cacheCfg);

        GridTcpDiscoverySpi spi = new GridTcpDiscoverySpi();

        spi.setIpFinder(ipFinder);

        cfg.setDiscoverySpi(spi);

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        backups = 1;
    }

    /**
     * @throws Exception If failed.
     */
    @SuppressWarnings( {"unchecked"})
    public void testTxCleanup() throws Exception {
        backups = 1;

        Grid grid = startGrids(GRID_CNT);

        try {
            Integer mainKey = 0;

            GridNode priNode = grid.mapKeyToNode(null, mainKey);
            GridNode backupNode = F.first(F.view(grid.cache(null).affinity().mapKeyToPrimaryAndBackups(mainKey),
                F.notIn(F.asList(priNode))));
            GridNode otherNode = F.first(grid.forPredicate(F.notIn(F.asList(priNode, backupNode))).nodes());

            assert priNode != backupNode;
            assert backupNode != otherNode;
            assert priNode != otherNode;

            Grid priGrid = G.grid(priNode.id());
            Grid backupGrid = G.grid(backupNode.id());
            Grid otherGrid = G.grid(otherNode.id());

            List<Grid> grids = F.asList(otherGrid, priGrid, backupGrid);

            int cntr = 0;

            // Update main key from all nodes.
            for (Grid g : grids)
                g.cache(null).put(mainKey, ++cntr);

            info("Updated mainKey from all nodes.");

            int keyCnt = 200;

            Collection<Integer> keys = new LinkedList<>();

            // Populate cache from all nodes.
            for (int i = 1; i <= keyCnt; i++) {
                keys.add(i);

                Grid g = F.rand(grids);

                g.cache(null).put(new GridCacheAffinityKey<>(i, mainKey), Integer.toString(cntr++));
            }

            GridCacheProjection cache = priGrid.cache(null).flagsOn(GridCacheFlag.CLONE);

            GridCacheTx tx = cache.txStart(PESSIMISTIC, REPEATABLE_READ);

            try {
                cache.get(mainKey);

                cache.removeAll(keys);

                cache.put(mainKey, ++cntr);

                tx.commit();
            }
            catch (Error | Exception e) {
                error("Transaction failed: " + tx, e);

                throw e;
            } finally {
                tx.close();
            }

            G.stop(priGrid.name(), true);
            G.stop(backupGrid.name(), true);

            Grid newGrid = startGrid(GRID_CNT);

            grids = F.asList(otherGrid, newGrid);

            for (Grid g : grids) {
                GridNearCacheAdapter near = ((GridKernal)g).internalCache().context().near();
                GridDhtCacheAdapter dht = near.dht();

                checkTm(g, near.context().tm());
                checkTm(g, dht.context().tm());
            }
        }
        finally {
            stopAllGrids();
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testTxReadersUpdate() throws Exception {
        startGridsMultiThreaded(GRID_CNT);

        try {
            testReadersUpdate(OPTIMISTIC, REPEATABLE_READ);

            testReadersUpdate(PESSIMISTIC, REPEATABLE_READ);
        }
        finally {
            stopAllGrids();
        }
    }

    /**
     * @param concurrency Transaction concurrency.
     * @param isolation Transaction isolation.
     * @throws Exception If failed.
     */
    private void testReadersUpdate(GridCacheTxConcurrency concurrency, GridCacheTxIsolation isolation) throws Exception {
        GridCache<Integer, Integer> cache = grid(0).cache(null);

        try (GridCacheTx tx = cache.txStart(concurrency, isolation)) {
            for (int i = 0; i < 100; i++)
                cache.put(i, 1);

            tx.commit();
        }

        // Create readers.
        for (int g = 0; g < GRID_CNT; g++) {
            GridCache<Integer, Integer> c = grid(g).cache(null);

            for (int i = 0; i < 100; i++)
                assertEquals((Integer)1, c.get(i));
        }

        try (GridCacheTx tx = cache.txStart(concurrency, isolation)) {
            for (int i = 0; i < 100; i++)
                cache.put(i, 2);

            tx.commit();
        }

        for (int g = 0; g < GRID_CNT; g++) {
            GridCache<Integer, Integer> c = grid(g).cache(null);

            for (int i = 0; i < 100; i++)
                assertEquals((Integer)2, c.get(i));
        }
    }

    /**
     * @param g Grid.
     * @param tm Transaction manager.
     */
    @SuppressWarnings( {"unchecked"})
    private void checkTm(Grid g, GridCacheTxManager tm) {
        Collection<GridCacheTxEx> txs = tm.txs();

        info(">>> Number of transactions in the set [size=" + txs.size() + ", nodeId=" + g.localNode().id() + ']');

        for (GridCacheTxEx tx : txs)
            assert tx.done() : "Transaction is not finished: " + tx;
    }
}
