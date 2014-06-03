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

package org.gridgain.benchmarks.storevalbytes;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.cache.affinity.consistenthash.*;
import org.gridgain.grid.kernal.processors.cache.*;
import org.gridgain.grid.spi.communication.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.grid.util.typedef.*;

import java.util.*;

import static org.gridgain.grid.cache.GridCacheMode.*;
import static org.gridgain.grid.cache.GridCacheDistributionMode.*;

/**
 *
 */
public class GridCacheStoreValueBytesNode {
    /**
     * @return Discovery SPI.
     * @throws Exception If failed.
     */
    static GridTcpDiscoverySpi discovery() throws Exception {
        GridTcpDiscoverySpi disc = new GridTcpDiscoverySpi();

        disc.setLocalAddress("localhost");

        GridTcpDiscoveryVmIpFinder ipFinder = new GridTcpDiscoveryVmIpFinder();

        Collection<String> addrs = new ArrayList<>();

        for (int i = 0; i < 10; i++)
            addrs.add("localhost:" + (GridTcpDiscoverySpi.DFLT_PORT + i));

        ipFinder.setAddresses(addrs);

        disc.setIpFinder(ipFinder);

        return disc;
    }

    /**
     * @param size Size.
     * @return Value.
     */
    static String createValue(int size) {
        StringBuilder str = new StringBuilder();

        str.append(new char[size]);

        return str.toString();
    }

    /**
     * @param args Arguments.
     * @param nearOnly Near only flag.
     * @return Configuration.
     * @throws Exception If failed.
     */
    static GridConfiguration parseConfiguration(String[] args, boolean nearOnly) throws Exception {
        boolean p2pEnabled = false;

        boolean storeValBytes = false;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            switch (arg) {
                case "-p2p":
                    p2pEnabled = Boolean.parseBoolean(args[++i]);

                    break;

                case "-storeValBytes":
                    storeValBytes = Boolean.parseBoolean(args[++i]);

                    break;
            }
        }

        X.println("Peer class loading enabled: " + p2pEnabled);
        X.println("Store value bytes: " + storeValBytes);

        GridConfiguration cfg = new GridConfiguration();

        GridTcpCommunicationSpi commSpi = new GridTcpCommunicationSpi();
        commSpi.setSharedMemoryPort(-1);

        cfg.setCommunicationSpi(commSpi);

        cfg.setDiscoverySpi(discovery());

        cfg.setPeerClassLoadingEnabled(p2pEnabled);

        GridCacheConfiguration cacheCfg = new GridCacheConfiguration();

        cacheCfg.setCacheMode(PARTITIONED);

        cacheCfg.setStoreValueBytes(storeValBytes);

        cacheCfg.setBackups(1);

        if (nearOnly) {
            cacheCfg.setNearEvictionPolicy(new GridCacheAlwaysEvictionPolicy());

            cacheCfg.setDistributionMode(NEAR_ONLY);
        }

        cfg.setCacheConfiguration(cacheCfg);

        return cfg;
    }

    /**
     * @param args Arguments.
     * @throws Exception If failed.
     */
    public static void main(String[] args) throws Exception {
        GridGain.start(parseConfiguration(args, false));
    }
}
