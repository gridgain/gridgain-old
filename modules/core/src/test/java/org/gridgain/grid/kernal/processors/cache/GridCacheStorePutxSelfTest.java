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
import org.gridgain.grid.cache.store.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.testframework.junits.common.*;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.concurrent.atomic.*;

import static org.gridgain.grid.cache.GridCacheAtomicityMode.*;
import static org.gridgain.grid.cache.GridCacheMode.*;
import static org.gridgain.grid.cache.GridCacheWriteSynchronizationMode.*;

/**
 * Tests for reproduce problem with GG-6895:
 * putx calls CacheStore.load() when null GridPredicate passed in to avoid IDE warnings
 */
public class GridCacheStorePutxSelfTest extends GridCommonAbstractTest {
    /** */
    private static final GridTcpDiscoveryIpFinder IP_FINDER = new GridTcpDiscoveryVmIpFinder(true);

    /** */
    private static AtomicInteger loads;

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        GridCacheConfiguration cache = new GridCacheConfiguration();

        cache.setCacheMode(PARTITIONED);
        cache.setAtomicityMode(TRANSACTIONAL);
        cache.setWriteSynchronizationMode(FULL_SYNC);
        cache.setStore(new TestStore());

        cfg.setCacheConfiguration(cache);

        GridTcpDiscoverySpi disco = new GridTcpDiscoverySpi();

        disco.setIpFinder(IP_FINDER);

        cfg.setDiscoverySpi(disco);

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        loads = new AtomicInteger();

        startGrid();
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        stopGrid();
    }

    /**
     * @throws Exception If failed.
     */
    public void testPutxShouldNotTriggerLoad() throws Exception {
        assertTrue(cache().putx(1, 1));
        assertTrue(cache().putx(2, 2, (GridPredicate)null));

        assertEquals(0, loads.get());
    }

    /**
     * @throws Exception If failed.
     */
    public void testPutxShouldNotTriggerLoadWithTx() throws Exception {
        GridCache<Integer, Integer> cache = cache();

        try (GridCacheTx tx = cache.txStart()) {
            assertTrue(cache.putx(1, 1));
            assertTrue(cache.putx(2, 2, (GridPredicate)null));

            tx.commit();
        }

        assertEquals(0, loads.get());
    }

    /** */
    private static class TestStore implements GridCacheStore<Integer, Integer> {
        /** {@inheritDoc} */
        @Nullable @Override public Integer load(@Nullable GridCacheTx tx, Integer key) throws GridException {
            loads.incrementAndGet();

            return null;
        }

        /** {@inheritDoc} */
        @Override public void loadCache(GridBiInClosure<Integer, Integer> clo, @Nullable Object... args)
            throws GridException {
            // No-op.
        }

        /** {@inheritDoc} */
        @Override public void loadAll(@Nullable GridCacheTx tx, Collection<? extends Integer> keys,
            GridBiInClosure<Integer, Integer> c) throws GridException {
            // No-op.
        }

        /** {@inheritDoc} */
        @Override public void put(@Nullable GridCacheTx tx, Integer key,
            @Nullable Integer val) throws GridException {
            // No-op.
        }

        /** {@inheritDoc} */
        @Override public void putAll(@Nullable GridCacheTx tx,
            Map<? extends Integer, ? extends Integer> map) throws GridException {
            // No-op.
        }

        /** {@inheritDoc} */
        @Override public void remove(@Nullable GridCacheTx tx, Integer key)
            throws GridException {
            // No-op.
        }

        /** {@inheritDoc} */
        @Override public void removeAll(@Nullable GridCacheTx tx, Collection<? extends Integer> keys)
            throws GridException {
            // No-op.
        }

        /** {@inheritDoc} */
        @Override public void txEnd(GridCacheTx tx, boolean commit) throws GridException {
            // No-op.
        }
    }
}
