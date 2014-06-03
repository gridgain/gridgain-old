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

package org.gridgain.loadtests.datastructures;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.cache.affinity.consistenthash.*;
import org.gridgain.grid.cache.datastructures.*;
import org.gridgain.grid.cache.eviction.lru.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.testframework.junits.common.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static org.gridgain.grid.cache.GridCacheWriteSynchronizationMode.*;

/**
 * Load test for atomic long.
 */
public class GridCachePartitionedAtomicLongLoadTest extends GridCommonAbstractTest {
    /** Test duration. */
    private static final long DURATION = 8 * 60 * 60 * 1000;

    /** */
    private static GridTcpDiscoveryIpFinder ipFinder = new GridTcpDiscoveryVmIpFinder(true);

    /** */
    private static final AtomicInteger idx = new AtomicInteger();

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration c = super.getConfiguration(gridName);

        GridCacheConfiguration cc = defaultCacheConfiguration();

        cc.setCacheMode(GridCacheMode.PARTITIONED);
        cc.setStartSize(200);
        cc.setPreloadMode(GridCachePreloadMode.SYNC);
        cc.setWriteSynchronizationMode(FULL_SYNC);
        cc.setEvictionPolicy(new GridCacheLruEvictionPolicy<>(1000));
        cc.setBackups(1);
        cc.setAffinity(new GridCacheConsistentHashAffinityFunction(true));
        cc.setAtomicSequenceReserveSize(10);
        cc.setEvictSynchronized(true);
        cc.setEvictNearSynchronized(true);
        cc.setDefaultTxConcurrency(GridCacheTxConcurrency.PESSIMISTIC);
        cc.setDefaultTxIsolation(GridCacheTxIsolation.REPEATABLE_READ);

        c.setCacheConfiguration(cc);

        GridTcpDiscoverySpi disco = new GridTcpDiscoverySpi();

        disco.setIpFinder(ipFinder);

        c.setDiscoverySpi(disco);

        return c;
    }

    /**
     * @throws Exception If failed.
     */
    public void testLoad() throws Exception {
        startGrid();

        try {
            multithreaded(new AtomicCallable(), 50);
        }
        finally {
            stopGrid();
        }
    }

    /**
     *
     */
    private class AtomicCallable implements Callable<Boolean> {
        /** {@inheritDoc} */
        @Override public Boolean call() throws Exception {
            Grid grid = grid();

            GridCache cache = grid.cache(null);

            assert cache != null;

            GridCacheAtomicSequence seq = cache.dataStructures().atomicSequence("SEQUENCE", 0, true);

            long start = System.currentTimeMillis();

            while (System.currentTimeMillis() - start < DURATION && !Thread.currentThread().isInterrupted()) {
                GridCacheTx tx = cache.txStart();

                long seqVal = seq.incrementAndGet();

                int curIdx = idx.incrementAndGet();

                if (curIdx % 1000 == 0)
                    info("Sequence value [seq=" + seqVal + ", idx=" + curIdx + ']');

                tx.commit();
            }

            return true;
        }
    }
}
