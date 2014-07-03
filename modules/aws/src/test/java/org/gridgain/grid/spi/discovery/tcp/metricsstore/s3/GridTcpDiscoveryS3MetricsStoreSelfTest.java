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

package org.gridgain.grid.spi.discovery.tcp.metricsstore.s3;

import com.amazonaws.auth.*;
import org.gridgain.grid.spi.discovery.tcp.metricsstore.*;
import org.gridgain.testframework.config.*;

import java.net.*;

/**
 * GridTcpDiscoveryS3MetricsStore test.
 */
public class GridTcpDiscoveryS3MetricsStoreSelfTest extends
    GridTcpDiscoveryMetricsStoreAbstractSelfTest<GridTcpDiscoveryS3MetricsStore> {
    /**
     * Constructor.
     *
     * @throws Exception If failed.
     */
    public GridTcpDiscoveryS3MetricsStoreSelfTest() throws Exception {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override public void testStoreMultiThreaded() throws Exception {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override protected GridTcpDiscoveryS3MetricsStore metricsStore() throws Exception {
        GridTcpDiscoveryS3MetricsStore store = new GridTcpDiscoveryS3MetricsStore();

        store.setAwsCredentials(new BasicAWSCredentials(GridTestProperties.getProperty("amazon.access.key"),
            GridTestProperties.getProperty("amazon.secret.key")));

        // Bucket name should be unique for the host to parallel test run on one bucket.
        store.setBucketName("metrics-store-test-bucket-" + InetAddress.getLocalHost().getAddress()[3]);

        return store;
    }
}
