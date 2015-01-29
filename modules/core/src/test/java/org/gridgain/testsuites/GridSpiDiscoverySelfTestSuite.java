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

package org.gridgain.testsuites;

import junit.framework.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.jdbc.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.multicast.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.sharedfs.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.grid.spi.discovery.tcp.metricsstore.jdbc.*;
import org.gridgain.grid.spi.discovery.tcp.metricsstore.sharedfs.*;
import org.gridgain.grid.spi.discovery.tcp.metricsstore.vm.*;

/**
 * Test suite for all discovery spi implementations.
 */
public class GridSpiDiscoverySelfTestSuite extends TestSuite {
    /**
     * @return Discovery SPI tests suite.
     * @throws Exception If failed.
     */
    public static TestSuite suite() throws Exception {
        TestSuite suite = new TestSuite("Gridgain Discovery SPI Test Suite");

        // Tcp.
        suite.addTest(new TestSuite(GridTcpDiscoveryVmIpFinderSelfTest.class));
        suite.addTest(new TestSuite(GridTcpDiscoverySharedFsIpFinderSelfTest.class));
        suite.addTest(new TestSuite(GridTcpDiscoveryJdbcIpFinderSelfTest.class));
        suite.addTest(new TestSuite(GridTcpDiscoveryMulticastIpFinderSelfTest.class));

        suite.addTest(new TestSuite(GridTcpDiscoveryVmMetricsStoreSelfTest.class));
        suite.addTest(new TestSuite(GridTcpDiscoverySharedFsMetricsStoreSelfTest.class));
        suite.addTest(new TestSuite(GridTcpDiscoveryJdbcMetricsStoreSelfTest.class));
        suite.addTest(new TestSuite(GridTcpDiscoveryJdbcMetricsStoreInitSchemaSelfTest.class));

        suite.addTest(new TestSuite(GridTcpDiscoverySelfTest.class));
        suite.addTest(new TestSuite(GridTcpDiscoverySpiSelfTest.class));
        suite.addTest(new TestSuite(GridTcpDiscoverySpiStartStopSelfTest.class));
        suite.addTest(new TestSuite(GridTcpDiscoverySpiConfigSelfTest.class));
        suite.addTest(new TestSuite(GridTcpDiscoveryMarshallerCheckSelfTest.class));
        suite.addTest(new TestSuite(GridTcpDiscoverySnapshotHistoryTest.class));

        suite.addTest(new TestSuite(GridTcpSpiForwardingSelfTest.class));

        return suite;
    }
}
