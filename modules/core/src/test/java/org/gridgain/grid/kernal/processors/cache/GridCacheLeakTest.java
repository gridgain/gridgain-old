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
import org.gridgain.grid.cache.affinity.rendezvous.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.testframework.junits.common.*;

import java.util.*;

import static org.gridgain.grid.cache.GridCacheAtomicityMode.*;
import static org.gridgain.grid.cache.GridCacheDistributionMode.*;
import static org.gridgain.grid.cache.GridCacheMode.*;
import static org.gridgain.grid.cache.GridCacheWriteSynchronizationMode.*;

/**
 * Leak test.
 */
public class GridCacheLeakTest extends GridCommonAbstractTest {
    /** IP finder. */
    private static final GridTcpDiscoveryIpFinder IP_FINDER = new GridTcpDiscoveryVmIpFinder(true);

    /** Cache name. */
    private static final String CACHE_NAME = "ggfs-data";

    /** Atomicity mode. */
    private GridCacheAtomicityMode atomicityMode;

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        GridTcpDiscoverySpi discoSpi = new GridTcpDiscoverySpi();

        discoSpi.setIpFinder(IP_FINDER);

        cfg.setDiscoverySpi(discoSpi);

        cfg.setCacheConfiguration(cacheConfiguration());

        return cfg;
    }

    /**
     * Gets cache configuration.
     *
     * @return Data cache configuration.
     */
    protected GridCacheConfiguration cacheConfiguration() {
        GridCacheConfiguration cfg = defaultCacheConfiguration();

        cfg.setName(CACHE_NAME);

        cfg.setAffinity(new GridCacheRendezvousAffinityFunction(false, 128));

        cfg.setCacheMode(PARTITIONED);
        cfg.setBackups(1);
        cfg.setDistributionMode(PARTITIONED_ONLY);
        cfg.setWriteSynchronizationMode(FULL_SYNC);
        cfg.setQueryIndexEnabled(false);
        cfg.setAtomicityMode(atomicityMode);

        return cfg;
    }

    /**
     * @throws Exception If failed.
     */
    public void testLeakTransactional() throws Exception {
        checkLeak(TRANSACTIONAL);
    }

    /**
     * @throws Exception If failed.
     */
    public void testLeakAtomic() throws Exception {
        checkLeak(ATOMIC);
    }

    /**
     * @throws Exception If failed.
     */
    private void checkLeak(GridCacheAtomicityMode mode) throws Exception {
        atomicityMode = mode;

        startGrids(3);

        try {
            int i = 0;

            GridCache<Object, Object> cache = grid(0).cache(CACHE_NAME);

            while (!Thread.currentThread().isInterrupted()) {
                UUID key = UUID.randomUUID();

                cache.put(key, 0);

                cache.remove(key);
                cache.remove(key);

                i++;

                if (i % 1000 == 0)
                    info("Put: " + i);

                if (i % 5000 == 0) {
                    for (int g = 0; g < 3; g++) {
                        GridCacheConcurrentMap<Object, Object> map = ((GridKernal)grid(g)).internalCache(CACHE_NAME).map();

                        info("Map size for cache [g=" + g + ", size=" + map.size() +
                            ", pubSize=" + map.publicSize() + ']');

                        assertTrue("Wrong map size: " + map.size(), map.size() <= 8192);
                    }
                }

                if (i == 500_000)
                    break;
            }
        }
        finally {
            stopAllGrids();
        }
    }

    /** {@inheritDoc} */
    @Override protected long getTestTimeout() {
        return Long.MAX_VALUE;
    }
}
