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

package org.gridgain.examples.datagrid.hibernate;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;

import java.util.*;

import static org.gridgain.grid.cache.GridCacheAtomicityMode.*;
import static org.gridgain.grid.cache.GridCacheMode.*;
import static org.gridgain.grid.cache.GridCacheWriteSynchronizationMode.*;

/**
 * Starts up an empty node with example cache configuration.
 */
public class HibernateL2CacheExampleNodeStartup {
    /**
     * Start up an empty node with specified cache configuration.
     *
     * @param args Command line arguments, none required.
     * @throws GridException If example execution failed.
     */
    public static void main(String[] args) throws GridException {
        GridGain.start(configuration());
    }

    /**
     * Create Grid configuration with GGFS and enabled IPC.
     *
     * @return Grid configuration.
     * @throws GridException If configuration creation failed.
     */
    public static GridConfiguration configuration() throws GridException {
        GridConfiguration cfg = new GridConfiguration();

        cfg.setGridName("hibernate-grid");
        cfg.setLocalHost("127.0.0.1");
        cfg.setRestEnabled(false);

        GridTcpDiscoverySpi discoSpi = new GridTcpDiscoverySpi();

        GridTcpDiscoveryVmIpFinder ipFinder = new GridTcpDiscoveryVmIpFinder();

        ipFinder.setAddresses(Collections.singletonList("127.0.0.1:47500..47509"));

        discoSpi.setIpFinder(ipFinder);

        cfg.setDiscoverySpi(discoSpi);

        cfg.setCacheConfiguration(
            cacheConfiguration("org.hibernate.cache.spi.UpdateTimestampsCache", ATOMIC),
            cacheConfiguration("org.hibernate.cache.internal.StandardQueryCache", ATOMIC),
            cacheConfiguration("org.gridgain.examples.datagrid.hibernate.User", TRANSACTIONAL),
            cacheConfiguration("org.gridgain.examples.datagrid.hibernate.User.posts", TRANSACTIONAL),
            cacheConfiguration("org.gridgain.examples.datagrid.hibernate.Post", TRANSACTIONAL)
        );

        return cfg;
    }

    /**
     * Create cache configuration.
     *
     * @param name Cache name.
     * @param atomicityMode Atomicity mode.
     * @return Cache configuration.
     */
    private static GridCacheConfiguration cacheConfiguration(String name, GridCacheAtomicityMode atomicityMode) {
        GridCacheConfiguration ccfg = new GridCacheConfiguration();

        ccfg.setName(name);
        ccfg.setCacheMode(PARTITIONED);
        ccfg.setAtomicityMode(atomicityMode);
        ccfg.setWriteSynchronizationMode(FULL_SYNC);

        return ccfg;
    }
}
