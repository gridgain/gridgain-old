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

package org.gridgain.examples.datagrid.store;

import org.gridgain.examples.datagrid.store.dummy.*;
import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.multicast.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;

import java.util.*;

import static org.gridgain.grid.cache.GridCacheAtomicityMode.*;

/**
 * Starts up an empty node with example cache configuration.
 */
public class CacheNodeWithStoreStartup {
    /**
     * Start up an empty node with specified cache configuration.
     *
     * @param args Command line arguments, none required.
     * @throws GridException If example execution failed.
     */
    public static void main(String[] args) throws GridException {
        GridGain.start(configure());
    }

    /**
     * Configure grid.
     *
     * @return Grid configuration.
     * @throws GridException If failed.
     */
    public static GridConfiguration configure() throws GridException {
        GridConfiguration cfg = new GridConfiguration();

        cfg.setLocalHost("127.0.0.1");

        // Discovery SPI.
        GridTcpDiscoverySpi discoSpi = new GridTcpDiscoverySpi();

        GridTcpDiscoveryVmIpFinder ipFinder = new GridTcpDiscoveryMulticastIpFinder();

        ipFinder.setAddresses(Arrays.asList("127.0.0.1:47500..47509"));

        discoSpi.setIpFinder(ipFinder);

        GridCacheConfiguration cacheCfg = new GridCacheConfiguration();

        // Set atomicity as transaction, since we are showing transactions in example.
        cacheCfg.setAtomicityMode(TRANSACTIONAL);

        // Uncomment other cache stores to try them.
        cacheCfg.setStore(new CacheDummyPersonStore());
        // cacheCfg.setStore(new CacheJdbcPersonStore());
        // cacheCfg.setStore(new CacheHibernatePersonStore());

        cfg.setDiscoverySpi(discoSpi);
        cfg.setCacheConfiguration(cacheCfg);

        return cfg;
    }
}
