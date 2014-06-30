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
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.kernal.processors.cache.distributed.dht.*;
import org.gridgain.grid.spi.checkpoint.noop.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.testframework.junits.common.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static org.gridgain.grid.cache.GridCacheMode.*;
import static org.gridgain.grid.cache.GridCacheDistributionMode.*;
import static org.gridgain.grid.cache.GridCachePreloadMode.*;
import static org.gridgain.grid.cache.GridCacheTxConcurrency.*;
import static org.gridgain.grid.cache.GridCacheTxIsolation.*;
import static org.gridgain.grid.cache.GridCacheWriteSynchronizationMode.*;

/**
 * Tests multi-update locks.
 */
public class GridCacheMultiUpdateLockSelfTest extends GridCommonAbstractTest {
    /** Shared IP finder. */
    private static final GridTcpDiscoveryIpFinder IP_FINDER = new GridTcpDiscoveryVmIpFinder(true);

    /** Near enabled flag. */
    private boolean nearEnabled;

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String name) throws Exception {
        GridConfiguration cfg = super.getConfiguration(name);

        GridTcpDiscoverySpi discoSpi = new GridTcpDiscoverySpi();

        discoSpi.setIpFinder(IP_FINDER);

        cfg.setDiscoverySpi(discoSpi);

        cfg.setCacheConfiguration(cacheConfiguration());

        cfg.setCheckpointSpi(new GridNoopCheckpointSpi());

        return cfg;
    }

    /**
     * @return Cache configuration.
     */
    protected GridCacheConfiguration cacheConfiguration() {
        GridCacheConfiguration cfg = defaultCacheConfiguration();

        cfg.setCacheMode(PARTITIONED);
        cfg.setBackups(1);
        cfg.setDistributionMode(nearEnabled ? NEAR_PARTITIONED : PARTITIONED_ONLY);

        cfg.setWriteSynchronizationMode(FULL_SYNC);
        cfg.setPreloadMode(SYNC);

        return cfg;
    }

    /**
     * @throws Exception If failed.
     */
    public void testMultiUpdateLocksNear() throws Exception {
        checkMultiUpdateLocks(true);
    }

    /**
     * @throws Exception If failed.
     */
    public void testMultiUpdateLocksColocated() throws Exception {
        checkMultiUpdateLocks(false);
    }

    /**
     * @param nearEnabled Near enabled flag.
     * @throws Exception If failed.
     */
    private void checkMultiUpdateLocks(boolean nearEnabled) throws Exception {
        this.nearEnabled = nearEnabled;

        startGrids(3);

        try {
            GridKernal g = (GridKernal)grid(0);

            GridCacheContext<Object, Object> cctx = g.internalCache().context();

            GridDhtCacheAdapter cache = nearEnabled ? cctx.near().dht() : cctx.colocated();

            long topVer = cache.beginMultiUpdate();

            GridFuture<?> startFut;

            try {
                assertEquals(3, topVer);

                final AtomicBoolean started = new AtomicBoolean();

                startFut = multithreadedAsync(new Callable<Object>() {
                    @Override public Object call() throws Exception {
                        info(">>>> Starting grid.");

                        Grid g4 = startGrid(4);

                        started.set(true);

                        GridCache<Object, Object> c = g4.cache(null);

                        info(">>>> Checking tx in new grid.");

                        try (GridCacheTx tx = c.txStart(PESSIMISTIC, REPEATABLE_READ)) {
                            assertEquals(2, c.get("a"));
                            assertEquals(4, c.get("b"));
                            assertEquals(6, c.get("c"));
                        }

                        return null;
                    }
                }, 1);

                U.sleep(200);

                info(">>>> Checking grid has not started yet.");

                assertFalse(started.get());

                // Check we can proceed with transactions.
                GridCache<Object, Object> cache0 = g.cache(null);

                info(">>>> Checking tx commit.");

                GridCacheTx tx = cache0.txStart(PESSIMISTIC, REPEATABLE_READ);

                try {
                    cache0.put("a", 1);
                    cache0.put("b", 2);
                    cache0.put("c", 3);

                    tx.commit();
                }
                finally {
                    tx.close();
                }

                info(">>>> Checking grid still is not started");

                assertFalse(started.get());

                tx = cache0.txStart(PESSIMISTIC, REPEATABLE_READ);

                try {
                    cache0.put("a", 2);
                    cache0.put("b", 4);
                    cache0.put("c", 6);

                    tx.commit();
                }
                finally {
                    tx.close();
                }
            }
            finally {
                info(">>>> Releasing multi update.");

                cache.endMultiUpdate();
            }

            info("Waiting for thread termination.");

            startFut.get();
        }
        finally {
            stopAllGrids();
        }
    }
}
