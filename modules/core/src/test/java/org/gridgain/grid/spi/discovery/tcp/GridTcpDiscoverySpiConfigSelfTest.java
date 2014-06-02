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

package org.gridgain.grid.spi.discovery.tcp;

import org.gridgain.testframework.junits.spi.*;

/**
 *
 */
@GridSpiTest(spi = GridTcpDiscoverySpi.class, group = "Discovery SPI")
public class GridTcpDiscoverySpiConfigSelfTest extends GridSpiAbstractConfigTest<GridTcpDiscoverySpi> {
    /**
     * @throws Exception If failed.
     */
    public void testNegativeConfig() throws Exception {
        checkNegativeSpiProperty(new GridTcpDiscoverySpi(), "ipFinder", null);
        checkNegativeSpiProperty(new GridTcpDiscoverySpi(), "storesCleanFrequency", 0);
        checkNegativeSpiProperty(new GridTcpDiscoverySpi(), "localPort", 1023);
        checkNegativeSpiProperty(new GridTcpDiscoverySpi(), "localPortRange", 0);
        checkNegativeSpiProperty(new GridTcpDiscoverySpi(), "networkTimeout", 0);
        checkNegativeSpiProperty(new GridTcpDiscoverySpi(), "socketTimeout", 0);
        checkNegativeSpiProperty(new GridTcpDiscoverySpi(), "ackTimeout", 0);
        checkNegativeSpiProperty(new GridTcpDiscoverySpi(), "maxAckTimeout", 0);
        checkNegativeSpiProperty(new GridTcpDiscoverySpi(), "reconnectCount", 0);
        checkNegativeSpiProperty(new GridTcpDiscoverySpi(), "heartbeatFrequency", 0);
        checkNegativeSpiProperty(new GridTcpDiscoverySpi(), "threadPriority", -1);
        checkNegativeSpiProperty(new GridTcpDiscoverySpi(), "maxMissedHeartbeats", 0);
        checkNegativeSpiProperty(new GridTcpDiscoverySpi(), "statisticsPrintFrequency", 0);
    }
}
