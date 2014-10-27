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

import junit.framework.*;

/**
 * Cache interceptor suite.
 */
public class GridCacheInterceptorSelfTestSuite extends TestSuite {
    /**
     * @return Cache API test suite.
     * @throws Exception If failed.
     */
    public static TestSuite suite() throws Exception {
        TestSuite suite = new TestSuite("Gridgain CacheInterceptor Test Suite");

        suite.addTestSuite(GridCacheInterceptorLocalSelfTest.class);
        suite.addTestSuite(GridCacheInterceptorLocalWithStoreSelfTest.class);

        suite.addTestSuite(GridCacheInterceptorLocalAtomicSelfTest.class);
        suite.addTestSuite(GridCacheInterceptorLocalAtomicWithStoreSelfTest.class);

        suite.addTestSuite(GridCacheInterceptorAtomicSelfTest.class);
        suite.addTestSuite(GridCacheInterceptorAtomicNearEnabledSelfTest.class);
        suite.addTestSuite(GridCacheInterceptorAtomicWithStoreSelfTest.class);
        suite.addTestSuite(GridCacheInterceptorAtomicPrimaryWriteOrderSelfTest.class);

        suite.addTestSuite(GridCacheInterceptorAtomicReplicatedSelfTest.class);
        suite.addTestSuite(GridCacheInterceptorAtomicWithStoreReplicatedSelfTest.class);
        suite.addTestSuite(GridCacheInterceptorAtomicReplicatedPrimaryWriteOrderSelfTest.class);

        suite.addTestSuite(GridCacheInterceptorSelfTest.class);
        suite.addTestSuite(GridCacheInterceptorNearEnabledSelfTest.class);
        suite.addTestSuite(GridCacheInterceptorWithStoreSelfTest.class);
        suite.addTestSuite(GridCacheInterceptorReplicatedSelfTest.class);
        suite.addTestSuite(GridCacheInterceptorReplicatedWithStoreSelfTest.class);

        return suite;
    }
}
