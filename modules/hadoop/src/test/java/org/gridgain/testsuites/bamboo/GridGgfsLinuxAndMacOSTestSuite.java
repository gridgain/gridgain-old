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
import org.gridgain.grid.ggfs.*;
import org.gridgain.grid.kernal.processors.ggfs.*;

/**
 * Test suite for Hadoop file system over GridGain cache.
 * Contains tests which works on Linux and Mac OS platform only.
 */
public class GridGgfsLinuxAndMacOSTestSuite extends TestSuite {
    /**
     * @return Test suite.
     * @throws Exception Thrown in case of the failure.
     */
    public static TestSuite suite() throws Exception {
        TestSuite suite = new TestSuite("Gridgain GGFS Test Suite For Linux And Mac OS");

        suite.addTest(new TestSuite(GridGgfsServerManagerIpcEndpointRegistrationOnLinuxAndMacSelfTest.class));

        suite.addTest(new TestSuite(GridGgfsHadoopFileSystemShmemExternalPrimarySelfTest.class));
        suite.addTest(new TestSuite(GridGgfsHadoopFileSystemShmemExternalSecondarySelfTest.class));
        suite.addTest(new TestSuite(GridGgfsHadoopFileSystemShmemExternalDualSyncSelfTest.class));
        suite.addTest(new TestSuite(GridGgfsHadoopFileSystemShmemExternalDualAsyncSelfTest.class));

        suite.addTest(new TestSuite(GridGgfsHadoopFileSystemShmemEmbeddedPrimarySelfTest.class));
        suite.addTest(new TestSuite(GridGgfsHadoopFileSystemShmemEmbeddedSecondarySelfTest.class));
        suite.addTest(new TestSuite(GridGgfsHadoopFileSystemShmemEmbeddedDualSyncSelfTest.class));
        suite.addTest(new TestSuite(GridGgfsHadoopFileSystemShmemEmbeddedDualAsyncSelfTest.class));

        suite.addTest(new TestSuite(GridGgfsHadoopFileSystemIpcCacheSelfTest.class));

        suite.addTestSuite(GridGgfsHadoop20FileSystemShmemPrimarySelfTest.class);

        suite.addTest(GridGgfsEventsTestSuite.suite());

        return suite;
    }
}
