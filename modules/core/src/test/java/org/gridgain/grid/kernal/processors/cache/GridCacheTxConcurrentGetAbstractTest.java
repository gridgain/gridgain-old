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
import org.gridgain.grid.kernal.processors.cache.distributed.dht.*;
import org.gridgain.grid.kernal.processors.cache.distributed.near.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.testframework.junits.common.*;

import java.util.*;
import java.util.concurrent.*;

import static org.gridgain.grid.cache.GridCacheTxConcurrency.*;
import static org.gridgain.grid.cache.GridCacheTxIsolation.*;

/**
 * Checks multithreaded put/get cache operations on one node.
 */
public abstract class GridCacheTxConcurrentGetAbstractTest extends GridCommonAbstractTest {
    /** Debug flag. */
    private static final boolean DEBUG = false;

    /** */
    protected static GridTcpDiscoveryIpFinder ipFinder = new GridTcpDiscoveryVmIpFinder(true);

    /** */
    private static final int THREAD_NUM = 20;

    /**
     * Default constructor.
     *
     */
    protected GridCacheTxConcurrentGetAbstractTest() {
        super(true /** Start grid. */);
    }

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        GridTcpDiscoverySpi spi = new GridTcpDiscoverySpi();

        spi.setIpFinder(ipFinder);

        cfg.setDiscoverySpi(spi);

        return cfg;
    }

    /**
     * @param g Grid.
     * @return Near cache.
     */
    GridNearCacheAdapter<String, Integer> near(Grid g) {
        return (GridNearCacheAdapter<String, Integer>)((GridKernal)g).<String, Integer>internalCache();
    }

    /**
     * @param g Grid.
     * @return DHT cache.
     */
    GridDhtCacheAdapter<String, Integer> dht(Grid g) {
        return near(g).dht();
    }

    /**
     * JUnit.
     *
     * @throws Exception If failed.
     */
    public void testPutGet() throws Exception {
        // Random key.
        final String key = UUID.randomUUID().toString();

        final Grid grid = grid();

        grid.cache(null).put(key, "val");

        GridCacheEntryEx<String,Integer> dhtEntry = dht(grid).peekEx(key);

        if (DEBUG)
            info("DHT entry [hash=" + System.identityHashCode(dhtEntry) + ", entry=" + dhtEntry + ']');

        String val = txGet(grid, key);

        assertNotNull(val);

        info("Starting threads: " + THREAD_NUM);

        multithreaded(new Callable<String>() {
            @Override public String call() throws Exception {
                return txGet(grid, key);
            }
        }, THREAD_NUM, "getter-thread");
    }

    /**
     * @param grid Grid.
     * @param key Key.
     * @return Value.
     * @throws Exception If failed.
     */
    private String txGet(Grid grid, String key) throws Exception {
        try (GridCacheTx tx = grid.cache(null).txStart(PESSIMISTIC, REPEATABLE_READ)) {
            GridCacheEntryEx<String, Integer> dhtEntry = dht(grid).peekEx(key);

            if (DEBUG)
                info("DHT entry [hash=" + System.identityHashCode(dhtEntry) + ", xid=" + tx.xid() +
                    ", entry=" + dhtEntry + ']');

            String val = grid.<String, String>cache(null).get(key);

            assertNotNull(val);
            assertEquals("val", val);

            tx.commit();

            return val;
        }
    }
}
