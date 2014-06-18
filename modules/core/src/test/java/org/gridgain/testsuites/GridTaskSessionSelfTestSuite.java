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
import org.gridgain.grid.session.*;

/**
 * Task session test suite.
 */
public class GridTaskSessionSelfTestSuite extends TestSuite {
    /**
     * @return TaskSession test suite.
     * @throws Exception If failed.
     */
    public static TestSuite suite() throws Exception {
        TestSuite suite = new TestSuite("Gridgain TaskSession Test Suite");

        suite.addTest(new TestSuite(GridSessionCancelSiblingsFromFutureSelfTest.class));
        suite.addTest(new TestSuite(GridSessionCancelSiblingsFromJobSelfTest.class));
        suite.addTest(new TestSuite(GridSessionCancelSiblingsFromTaskSelfTest.class));
        suite.addTest(new TestSuite(GridSessionSetFutureAttributeSelfTest.class));
        suite.addTest(new TestSuite(GridSessionSetFutureAttributeWaitListenerSelfTest.class));
        suite.addTest(new TestSuite(GridSessionSetJobAttributeWaitListenerSelfTest.class));
        suite.addTest(new TestSuite(GridSessionSetJobAttributeSelfTest.class));
        suite.addTest(new TestSuite(GridSessionSetJobAttribute2SelfTest.class));
        suite.addTest(new TestSuite(GridSessionJobWaitTaskAttributeSelfTest.class));
        suite.addTest(new TestSuite(GridSessionSetTaskAttributeSelfTest.class));
        suite.addTest(new TestSuite(GridSessionFutureWaitTaskAttributeSelfTest.class));
        suite.addTest(new TestSuite(GridSessionFutureWaitJobAttributeSelfTest.class));
        suite.addTest(new TestSuite(GridSessionTaskWaitJobAttributeSelfTest.class));
        suite.addTest(new TestSuite(GridSessionSetJobAttributeOrderSelfTest.class));
        suite.addTest(new TestSuite(GridSessionWaitAttributeSelfTest.class));
        suite.addTest(new TestSuite(GridSessionJobFailoverSelfTest.class));
        suite.addTest(new TestSuite(GridSessionLoadSelfTest.class));
        suite.addTest(new TestSuite(GridSessionCollisionSpiSelfTest.class));
        suite.addTest(new TestSuite(GridSessionCheckpointSelfTest.class));

        return suite;
    }
}
