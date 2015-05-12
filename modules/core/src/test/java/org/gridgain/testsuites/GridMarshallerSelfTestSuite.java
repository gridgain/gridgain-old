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
import org.gridgain.grid.marshaller.jdk.*;
import org.gridgain.grid.marshaller.optimized.*;
import org.gridgain.grid.util.io.*;

/**
 * Test suite for all marshallers.
 */
public class GridMarshallerSelfTestSuite extends TestSuite {
    /**
     * @return Kernal test suite.
     * @throws Exception If failed.
     */
    public static TestSuite suite() throws Exception {
        TestSuite suite = new TestSuite("Gridgain Marshaller Test Suite");

        suite.addTest(new TestSuite(GridJdkMarshallerSelfTest.class));
        suite.addTest(new TestSuite(GridOptimizedMarshallerEnumSelfTest.class));
        suite.addTest(new TestSuite(GridOptimizedMarshallerSelfTest.class));
        suite.addTest(new TestSuite(GridOptimizedMarshallerTest.class));
        suite.addTest(new TestSuite(GridOptimizedObjectStreamSelfTest.class));
        suite.addTest(new TestSuite(GridUnsafeDataOutputArraySizingSelfTest.class));
        suite.addTest(new TestSuite(OptimizedMarshallerSerialPersistentFieldsSelfTest.class));

        return suite;
    }
}
