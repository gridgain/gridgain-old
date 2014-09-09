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
import org.gridgain.grid.kernal.processors.hadoop.*;
import org.gridgain.grid.kernal.processors.hadoop.shuffle.collections.*;
import org.gridgain.grid.kernal.processors.hadoop.shuffle.streams.*;
import org.gridgain.grid.kernal.processors.hadoop.taskexecutor.external.*;
import org.gridgain.grid.kernal.processors.hadoop.taskexecutor.external.communication.*;

/**
 * Test suite for Hadoop Map Reduce engine.
 */
public class GridHadoopTestSuite extends TestSuite {
    /**
     * @return Test suite.
     * @throws Exception Thrown in case of the failure.
     */
    public static TestSuite suite() throws Exception {
        TestSuite suite = new TestSuite("Gridgain Hadoop MR Test Suite");

        suite.addTest(new TestSuite(GridHadoopFileSystemsTest.class));

        suite.addTest(new TestSuite(GridHadoopValidationSelfTest.class));

        suite.addTest(new TestSuite(GridHadoopDefaultMapReducePlannerSelfTest.class));
        suite.addTest(new TestSuite(GridHadoopJobTrackerSelfTest.class));
        suite.addTest(new TestSuite(GridHadoopHashMapSelfTest.class));
        suite.addTest(new TestSuite(GridHadoopDataStreamSelfTest.class));
        suite.addTest(new TestSuite(GridHadoopConcurrentHashMultimapSelftest.class));
        suite.addTestSuite(GridHadoopSkipListSelfTest.class);
        suite.addTest(new TestSuite(GridHadoopTaskExecutionSelfTest.class));

        suite.addTest(new TestSuite(GridHadoopV2JobSelfTest.class));

        suite.addTest(new TestSuite(GridHadoopSerializationWrapperSelfTest.class));
        suite.addTest(new TestSuite(GridHadoopSplitWrapperSelfTest.class));

        suite.addTest(new TestSuite(GridHadoopTasksV1Test.class));
        suite.addTest(new TestSuite(GridHadoopTasksV2Test.class));

        suite.addTest(new TestSuite(GridHadoopMapReduceTest.class));
        suite.addTest(new TestSuite(GridHadoopMapReduceEmbeddedSelfTest.class));

        //TODO: GG-8936 Fix and uncomment ExternalExecution tests
        //suite.addTest(new TestSuite(GridHadoopExternalTaskExecutionSelfTest.class));
        suite.addTest(new TestSuite(GridHadoopExternalCommunicationSelfTest.class));

        suite.addTest(new TestSuite(GridHadoopSortingTest.class));
        suite.addTest(new TestSuite(GridHadoopSortingExternalTest.class));

        suite.addTest(new TestSuite(GridHadoopGroupingTest.class));

        return suite;
    }
}
