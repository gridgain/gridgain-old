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

package org.gridgain.grid.kernal.websession;

import junit.framework.*;

/**
 * Test suite for web sessions caching functionality.
 */
@SuppressWarnings("PublicInnerClass")
public class GridWebSessionSelfTestSuite extends TestSuite {
    /**
     * @return Test suite.
     * @throws Exception Thrown in case of the failure.
     */
    public static TestSuite suite() throws Exception {
        TestSuite suite = new TestSuite("GridGain Web Sessions Test Suite");

        suite.addTestSuite(GridWebSessionSelfTest.class);
        suite.addTestSuite(WebSessionTransactionalSelfTest.class);
        suite.addTestSuite(WebSessionReplicatedSelfTest.class);

        return suite;
    }

    /**
     * Tests web sessions with TRANSACTIONAL cache.
     */
    public static class WebSessionTransactionalSelfTest extends GridWebSessionSelfTest {
        /** {@inheritDoc} */
        @Override protected String getCacheName() {
            return "partitioned_tx";
        }

        /** {@inheritDoc} */
        @Override public void testRestarts() throws Exception {
            // TODO GG-8166, enable when fixed.
        }
    }

    /**
     * Tests web sessions with REPLICATED cache.
     */
    public static class WebSessionReplicatedSelfTest extends GridWebSessionSelfTest {
        /** {@inheritDoc} */
        @Override protected String getCacheName() {
            return "replicated";
        }
    }
}
