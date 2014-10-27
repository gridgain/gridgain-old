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

package org.gridgain.testsuites.bamboo;

import junit.framework.*;
import org.gridgain.grid.spi.checkpoint.s3.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.s3.*;
import org.gridgain.grid.spi.discovery.tcp.metricsstore.s3.*;

/**
 * S3 integration tests.
 */
public class GridS3TestSuite extends TestSuite {
    /**
     * @return Test suite.
     * @throws Exception Thrown in case of the failure.
     */
    public static TestSuite suite() throws Exception {
        TestSuite suite = new TestSuite("S3 Integration Test Suite");

        // Checkpoint SPI.
        suite.addTest(new TestSuite(GridS3CheckpointSpiConfigSelfTest.class));
        suite.addTest(new TestSuite(GridS3CheckpointSpiSelfTest.class));
        suite.addTest(new TestSuite(GridS3CheckpointSpiStartStopSelfTest.class));
        suite.addTest(new TestSuite(GridS3CheckpointManagerSelfTest.class));
        suite.addTest(new TestSuite(GridS3SessionCheckpointSelfTest.class));

        // S3 IP finder.
        suite.addTest(new TestSuite(GridTcpDiscoveryS3IpFinderSelfTest.class));

        // S3 metrics store.
        suite.addTest(new TestSuite(GridTcpDiscoveryS3MetricsStoreSelfTest.class));

        return suite;
    }
}
