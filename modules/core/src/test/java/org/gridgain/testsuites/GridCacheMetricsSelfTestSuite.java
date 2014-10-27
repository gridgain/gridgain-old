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
import org.gridgain.grid.kernal.processors.cache.distributed.near.*;
import org.gridgain.grid.kernal.processors.cache.distributed.replicated.*;
import org.gridgain.grid.kernal.processors.cache.local.*;

/**
 * Test suite for cache metrics.
 */
public class GridCacheMetricsSelfTestSuite extends TestSuite {
    /**
     * @return Cache metrics test suite.
     * @throws Exception If failed.
     */
    public static TestSuite suite() throws Exception {
        TestSuite suite = new TestSuite("Gridgain Cache Metrics Test Suite");

        suite.addTest(new TestSuite(GridCacheLocalMetricsSelfTest.class));
        suite.addTest(new TestSuite(GridCacheNearMetricsSelfTest.class));
        suite.addTest(new TestSuite(GridCacheReplicatedMetricsSelfTest.class));
        suite.addTest(new TestSuite(GridCachePartitionedMetricsSelfTest.class));

        // Atomic cache.
        suite.addTest(new TestSuite(GridCacheAtomicLocalMetricsSelfTest.class));
        suite.addTest(new TestSuite(GridCacheAtomicLocalMetricsNoStoreSelfTest.class));
        suite.addTest(new TestSuite(GridCacheAtomicReplicatedMetricsSelfTest.class));
        suite.addTest(new TestSuite(GridCacheAtomicPartitionedMetricsSelfTest.class));

        return suite;
    }
}
