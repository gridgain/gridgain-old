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
import org.apache.commons.compress.archivers.tar.*;
import org.apache.commons.compress.compressors.gzip.*;
import org.gridgain.client.hadoop.*;
import org.gridgain.grid.*;
import org.gridgain.grid.ggfs.*;
import org.gridgain.grid.kernal.processors.hadoop.*;
import org.gridgain.grid.kernal.processors.hadoop.shuffle.collections.*;
import org.gridgain.grid.kernal.processors.hadoop.shuffle.streams.*;
import org.gridgain.grid.kernal.processors.hadoop.taskexecutor.external.communication.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Test suite for Hadoop Map Reduce engine.
 */
public class GridHadoopTestSuite extends TestSuite {
    /**
     * @return Test suite.
     * @throws Exception Thrown in case of the failure.
     */
    public static TestSuite suite() throws Exception {
        downloadHadoop();
        
        GridHadoopClassLoader ldr = new GridHadoopClassLoader(null);
        
        TestSuite suite = new TestSuite("Gridgain Hadoop MR Test Suite");

        suite.addTest(new TestSuite(ldr.loadClass(GridGgfsHadoopFileSystemLoopbackExternalPrimarySelfTest.class.getName())));
        suite.addTest(new TestSuite(ldr.loadClass(GridGgfsHadoopFileSystemLoopbackExternalSecondarySelfTest.class.getName())));
        suite.addTest(new TestSuite(ldr.loadClass(GridGgfsHadoopFileSystemLoopbackExternalDualSyncSelfTest.class.getName())));
        suite.addTest(new TestSuite(ldr.loadClass(GridGgfsHadoopFileSystemLoopbackExternalDualAsyncSelfTest.class.getName())));
        suite.addTest(new TestSuite(ldr.loadClass(GridGgfsHadoopFileSystemLoopbackEmbeddedPrimarySelfTest.class.getName())));
        suite.addTest(new TestSuite(ldr.loadClass(GridGgfsHadoopFileSystemLoopbackEmbeddedSecondarySelfTest.class.getName())));
        suite.addTest(new TestSuite(ldr.loadClass(GridGgfsHadoopFileSystemLoopbackEmbeddedDualSyncSelfTest.class.getName())));
        suite.addTest(new TestSuite(ldr.loadClass(GridGgfsHadoopFileSystemLoopbackEmbeddedDualAsyncSelfTest.class.getName())));

        suite.addTest(new TestSuite(ldr.loadClass(GridGgfsHadoopFileSystemSecondaryModeSelfTest.class.getName())));

        suite.addTest(new TestSuite(ldr.loadClass(GridGgfsHadoopFileSystemClientSelfTest.class.getName())));

        suite.addTest(new TestSuite(ldr.loadClass(GridGgfsHadoopFileSystemLoggerStateSelfTest.class.getName())));
        suite.addTest(new TestSuite(ldr.loadClass(GridGgfsHadoopFileSystemLoggerSelfTest.class.getName())));

        suite.addTest(new TestSuite(ldr.loadClass(GridGgfsHadoopFileSystemHandshakeSelfTest.class.getName())));

        suite.addTest(new TestSuite(ldr.loadClass(GridGgfsHadoop20FileSystemLoopbackPrimarySelfTest.class.getName())));

        suite.addTest(new TestSuite(ldr.loadClass(GridGgfsHadoopDualSyncSelfTest.class.getName())));
        suite.addTest(new TestSuite(ldr.loadClass(GridGgfsHadoopDualAsyncSelfTest.class.getName())));

        suite.addTest(GridGgfsEventsTestSuite.suiteNoarchOnly());

        suite.addTest(new TestSuite(ldr.loadClass(GridHadoopFileSystemsTest.class.getName())));

        suite.addTest(new TestSuite(ldr.loadClass(GridHadoopValidationSelfTest.class.getName())));

        suite.addTest(new TestSuite(ldr.loadClass(GridHadoopDefaultMapReducePlannerSelfTest.class.getName())));
        suite.addTest(new TestSuite(ldr.loadClass(GridHadoopJobTrackerSelfTest.class.getName())));

        suite.addTest(new TestSuite(ldr.loadClass(GridHadoopHashMapSelfTest.class.getName())));
        suite.addTest(new TestSuite(ldr.loadClass(GridHadoopDataStreamSelfTest.class.getName())));
        suite.addTest(new TestSuite(ldr.loadClass(GridHadoopConcurrentHashMultimapSelftest.class.getName())));

        suite.addTest(new TestSuite(ldr.loadClass(GridHadoopSkipListSelfTest.class.getName())));

        suite.addTest(new TestSuite(ldr.loadClass(GridHadoopTaskExecutionSelfTest.class.getName())));

        suite.addTest(new TestSuite(ldr.loadClass(GridHadoopV2JobSelfTest.class.getName())));

        suite.addTest(new TestSuite(ldr.loadClass(GridHadoopSerializationWrapperSelfTest.class.getName())));
        suite.addTest(new TestSuite(ldr.loadClass(GridHadoopSplitWrapperSelfTest.class.getName())));

        suite.addTest(new TestSuite(ldr.loadClass(GridHadoopTasksV1Test.class.getName())));
        suite.addTest(new TestSuite(ldr.loadClass(GridHadoopTasksV2Test.class.getName())));

        suite.addTest(new TestSuite(ldr.loadClass(GridHadoopMapReduceTest.class.getName())));

        suite.addTest(new TestSuite(ldr.loadClass(GridHadoopMapReduceEmbeddedSelfTest.class.getName())));

        //TODO: GG-8936 Fix and uncomment ExternalExecution tests
        //suite.addTest(new TestSuite(ldr.loadClass(GridHadoopExternalTaskExecutionSelfTest.class.getName())));
        suite.addTest(new TestSuite(ldr.loadClass(GridHadoopExternalCommunicationSelfTest.class.getName())));

        suite.addTest(new TestSuite(ldr.loadClass(GridHadoopSortingTest.class.getName())));

        //TODO: GG-8936 Fix and uncomment ExternalExecution tests
        //suite.addTest(new TestSuite(ldr.loadClass(GridHadoopSortingExternalTest.class.getName())));

        suite.addTest(new TestSuite(ldr.loadClass(GridHadoopGroupingTest.class.getName())));

        suite.addTest(new TestSuite(ldr.loadClass(GridHadoopClientProtocolSelfTest.class.getName())));
        suite.addTest(new TestSuite(ldr.loadClass(GridHadoopClientProtocolEmbeddedSelfTest.class.getName())));

        return suite;
    }

    /**
     * @throws Exception If failed.
     */
    public static void downloadHadoop() throws Exception {
        String hadoopHome = GridSystemProperties.getString("HADOOP_HOME");

        if (!F.isEmpty(hadoopHome) && new File(hadoopHome).isDirectory()) {
            X.println("HADOOP_HOME is set to: " + hadoopHome);

            return;
        }

        String ver = GridSystemProperties.getString("hadoop.version", "2.4.1");

        X.println("Will use Hadoop version: " + ver);

        String path = "hadoop-" + ver + "/hadoop-" + ver + ".tar.gz";

        List<String> urls = F.asList(
            "http://apache-mirror.rbc.ru/pub/apache/hadoop/common/",
            "http://www.eu.apache.org/dist/hadoop/common/",
            "http://www.us.apache.org/dist/hadoop/common/");

        String tmpPath = System.getProperty("java.io.tmpdir");

        X.println("tmp: " + tmpPath);

        File install = new File(tmpPath + File.separatorChar + "__hadoop");

        File home = new File(install, "hadoop-" + ver);

        X.println("Setting HADOOP_HOME to " + home.getAbsolutePath());

        System.setProperty("HADOOP_HOME", home.getAbsolutePath());

        if (home.exists()) {
            X.println("Destination directory already exists.");

            return;
        }

        for (String url : urls) {
            if (!install.mkdirs())
                throw new IOException("Failed to create directory: " + install.getAbsolutePath());

            URL u = new URL(url + path);

            X.println("Attempting to download from: " + u);

            try {
                URLConnection c = u.openConnection();

                c.connect();

                try (TarArchiveInputStream in = new TarArchiveInputStream(new GzipCompressorInputStream(
                    new BufferedInputStream(c.getInputStream(), 32 * 1024)))) {

                    TarArchiveEntry entry;

                    while ((entry = in.getNextTarEntry()) != null) {
                        File dest = new File(install, entry.getName());

                        if (entry.isDirectory()) {
                            if (!dest.mkdirs())
                                throw new IllegalStateException();
                        }
                        else {
                            X.print(" [" + dest);

                            try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(dest, false),
                                    128 * 1024)) {
                                U.copy(in, out);

                                out.flush();
                            }

                            X.println("]");
                        }
                    }
                }

                return;
            }
            catch (Exception e) {
                e.printStackTrace();

                U.delete(install);
            }
        }

        throw new IllegalStateException("Failed to install Hadoop.");
    }
}
