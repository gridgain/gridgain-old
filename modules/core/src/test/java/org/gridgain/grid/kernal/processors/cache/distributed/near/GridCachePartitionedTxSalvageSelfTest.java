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
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.kernal.processors.cache.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.testframework.junits.common.*;

import java.util.*;

import static org.gridgain.grid.GridSystemProperties.*;
import static org.gridgain.grid.cache.GridCacheTxConcurrency.*;
import static org.gridgain.grid.cache.GridCacheTxIsolation.*;

/**
 * Test tx salvage.
 */
public class GridCachePartitionedTxSalvageSelfTest extends GridCommonAbstractTest {
    /** Grid count. */
    private static final int GRID_CNT = 5;

    /** Key count. */
    private static final int KEY_CNT = 10;

    /** Salvage timeout system property value. */
    private static final Integer SALVAGE_TIMEOUT = 5000;

    /** Difference between salvage timeout and actual wait time when performing "before salvage" tests. */
    private static final int DELTA_BEFORE = 1000;

    /** How much time to wait after salvage timeout when performing "after salvage" tests. */
    private static final int DELTA_AFTER = 1000;

    /** Salvage timeout system property value before alteration. */
    private static String salvageTimeoutOld;

    /** Standard VM IP finder. */
    private static final GridTcpDiscoveryIpFinder ipFinder = new GridTcpDiscoveryVmIpFinder(true);

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration c = super.getConfiguration(gridName);

        // Discovery.
        GridTcpDiscoverySpi disco = new GridTcpDiscoverySpi();

        disco.setIpFinder(ipFinder);

        c.setDiscoverySpi(disco);

        GridCacheConfiguration cc = defaultCacheConfiguration();

        cc.setCacheMode(GridCacheMode.PARTITIONED);
        cc.setAffinity(new GridCacheConsistentHashAffinityFunction(false, 18));
        cc.setBackups(1);
        cc.setPreloadMode(GridCachePreloadMode.SYNC);
        cc.setDgcFrequency(0);

        c.setCacheConfiguration(cc);

        return c;
    }

    @Override protected void beforeTestsStarted() throws Exception {
        // Set salvage timeout system property.
        salvageTimeoutOld = System.setProperty(GG_TX_SALVAGE_TIMEOUT, SALVAGE_TIMEOUT.toString());
    }

    @Override protected void afterTestsStopped() throws Exception {
        // Restore salvage timeout system property to its initial state.
        if (salvageTimeoutOld != null)
            System.setProperty(GG_TX_SALVAGE_TIMEOUT, salvageTimeoutOld);
        else
            System.clearProperty(GG_TX_SALVAGE_TIMEOUT);
    }

    @Override protected void beforeTest() throws Exception {
        // Start the grid.
        startGridsMultiThreaded(GRID_CNT);
    }

    @Override protected void afterTest() throws Exception {
        // Shutwodn the gird.
        stopAllGrids();
    }

    /**
     * @throws Exception If failed.
     */
    public void testOptimisticTxSalvageBeforeTimeout() throws Exception {
        checkSalvageBeforeTimeout(OPTIMISTIC, true);
    }

    /**
     * @throws Exception If failed.
     */
    public void testPessimisticcTxSalvageBeforeTimeout() throws Exception {
        checkSalvageBeforeTimeout(PESSIMISTIC, false);
    }

    /**
     * @throws Exception If failed.
     */
    public void testOptimisticTxSalvageAfterTimeout() throws Exception {
        checkSalvageAfterTimeout(OPTIMISTIC, true);
    }

    /**
     * @throws Exception If failed.
     */
    public void testPessimisticTxSalvageAfterTimeout() throws Exception {
        checkSalvageAfterTimeout(PESSIMISTIC, false);
    }

    /**
     * Check whether caches has no transactions after salvage timeout.
     *
     * @param mode Transaction mode (PESSIMISTIC, OPTIMISTIC).
     * @param prepare Whether to preapre transaction state
     *                (i.e. call {@link GridCacheTxEx#prepare()}).
     * @throws Exception If failed.
     */
    private void checkSalvageAfterTimeout(GridCacheTxConcurrency mode, boolean prepare) throws Exception {
        startTxAndPutKeys(mode, prepare);

        stopNodeAndSleep(SALVAGE_TIMEOUT + DELTA_AFTER);

        for (int i = 1; i < GRID_CNT; i++) {
            checkTxsEmpty(near(i).context());
            checkTxsEmpty(dht(i).context());
        }
    }

    /**
     * Check whether caches still has all transactions before salvage timeout.
     *
     * @param mode Transaction mode (PESSIMISTIC, OPTIMISTIC).
     * @param prepare Whether to preapre transaction state
     *                (i.e. call {@link GridCacheTxEx#prepare()}).
     * @throws Exception If failed.
     */
    private void checkSalvageBeforeTimeout(GridCacheTxConcurrency mode, boolean prepare) throws Exception {
        startTxAndPutKeys(mode, prepare);

        List<Integer> nearSizes = new ArrayList<>(GRID_CNT - 1);
        List<Integer> dhtSizes = new ArrayList<>(GRID_CNT - 1);

        for (int i = 1; i < GRID_CNT; i++) {
            nearSizes.add(near(i).context().tm().txs().size());
            dhtSizes.add(dht(i).context().tm().txs().size());
        }

        stopNodeAndSleep(SALVAGE_TIMEOUT - DELTA_BEFORE);

        for (int i = 1; i < GRID_CNT; i++) {
            checkTxsNotEmpty(near(i).context(), nearSizes.get(i - 1));
            checkTxsNotEmpty(dht(i).context(), dhtSizes.get(i - 1));
        }
    }

    /**
     * Start new transaction on the grid(0) and put some keys to it.
     *
     * @param mode Transaction mode (PESSIMISTIC, OPTIMISTIC).
     * @param prepare Whether to preapre transaction state
     *                (i.e. call {@link GridCacheTxEx#prepare()}).
     * @throws Exception If failed.
     */
    private void startTxAndPutKeys(final GridCacheTxConcurrency mode, final boolean prepare) throws Exception {
        Grid grid = grid(0);

        final Collection<Integer> keys = nearKeys(grid);

        GridFuture<?> fut = multithreadedAsync(new Runnable() {
            @Override public void run() {
                GridCache<Object, Object> c = cache(0);

                try {
                    GridCacheTx tx = c.txStart(mode, REPEATABLE_READ);

                    for (Integer key : keys)
                        c.put(key, "val" + key);

                    // Unproxy.
                    if (prepare)
                        U.<GridCacheTxEx>field(tx, "tx").prepare();
                }
                catch (GridException e) {
                    info("Failed to put keys to cache: " + e.getMessage());
                }
            }
        }, 1);

        fut.get();
    }

    /**
     * Stop the very first grid node (the one with 0 index) and sleep for the given amount of time.
     *
     * @param timeout Sleep timeout in milliseconds.
     * @throws Exception If failed.
     */
    private void stopNodeAndSleep(long timeout) throws Exception {
        stopGrid(0);

        info("Stopped grid.");

        U.sleep(timeout);
    }

    /**
     * Gets keys that are not primary nor backup for node.
     *
     * @param grid Grid.
     * @return Collection of keys.
     */
    private Collection<Integer> nearKeys(Grid grid) {
        final Collection<Integer> keys = new ArrayList<>(KEY_CNT);

        GridKernal kernal = (GridKernal)grid;

        GridCacheAffinityManager<Object, Object> affMgr = kernal.internalCache().context().affinity();

        for (int i = 0; i < KEY_CNT * GRID_CNT * 1.5; i++) {
            if (!affMgr.localNode((Object)i, kernal.context().discovery().topologyVersion())) {
                keys.add(i);

                if (keys.size() == KEY_CNT)
                    break;
            }
        }

        return keys;
    }

    /**
     * Checks that transaction manager for cache context does not have any pending transactions.
     *
     * @param ctx Cache context.
     */
    private void checkTxsEmpty(GridCacheContext ctx) {
        Collection txs = ctx.tm().txs();

        assert txs.isEmpty() : "Not all transactions were salvaged: " + txs;
    }

    /**
     * Checks that transaction manager for cache context has expected number of pending transactions.
     *
     * @param ctx Cache context.
     * @param exp Expected amount of transactions.
     */
    private void checkTxsNotEmpty(GridCacheContext ctx, int exp) {
        int size = ctx.tm().txs().size();

        assert size == exp : "Some transactions were salvaged unexpectedly: " + exp +
            " expected, but only " + size + " found.";
    }
}
