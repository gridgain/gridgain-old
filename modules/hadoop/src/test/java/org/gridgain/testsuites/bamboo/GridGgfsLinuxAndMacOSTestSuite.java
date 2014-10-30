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
import org.gridgain.grid.kernal.processors.hadoop.*;

import static org.gridgain.testsuites.bamboo.GridHadoopTestSuite.*;

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
        downloadHadoop();

        GridHadoopClassLoader ldr = new GridHadoopClassLoader(null);
        
        TestSuite suite = new TestSuite("Gridgain GGFS Test Suite For Linux And Mac OS");

        suite.addTest(new TestSuite(ldr.loadClass(GridGgfsServerManagerIpcEndpointRegistrationOnLinuxAndMacSelfTest.class.getName())));

        suite.addTest(new TestSuite(ldr.loadClass(GridGgfsHadoopFileSystemShmemExternalPrimarySelfTest.class.getName())));
        suite.addTest(new TestSuite(ldr.loadClass(GridGgfsHadoopFileSystemShmemExternalSecondarySelfTest.class.getName())));
        suite.addTest(new TestSuite(ldr.loadClass(GridGgfsHadoopFileSystemShmemExternalDualSyncSelfTest.class.getName())));
        suite.addTest(new TestSuite(ldr.loadClass(GridGgfsHadoopFileSystemShmemExternalDualAsyncSelfTest.class.getName())));

        suite.addTest(new TestSuite(ldr.loadClass(GridGgfsHadoopFileSystemShmemEmbeddedPrimarySelfTest.class.getName())));
        suite.addTest(new TestSuite(ldr.loadClass(GridGgfsHadoopFileSystemShmemEmbeddedSecondarySelfTest.class.getName())));
        suite.addTest(new TestSuite(ldr.loadClass(GridGgfsHadoopFileSystemShmemEmbeddedDualSyncSelfTest.class.getName())));
        suite.addTest(new TestSuite(ldr.loadClass(GridGgfsHadoopFileSystemShmemEmbeddedDualAsyncSelfTest.class.getName())));

        suite.addTest(new TestSuite(ldr.loadClass(GridGgfsHadoopFileSystemIpcCacheSelfTest.class.getName())));

        suite.addTest(new TestSuite(ldr.loadClass(GridGgfsHadoop20FileSystemShmemPrimarySelfTest.class.getName())));

        suite.addTest(GridGgfsEventsTestSuite.suite());

        return suite;
    }
}
