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

package org.gridgain.grid.p2p;

import org.gridgain.grid.*;
import org.gridgain.testframework.*;
import org.gridgain.testframework.config.*;
import org.gridgain.testframework.junits.common.*;
import java.net.*;
import java.util.*;

/**
 * Test P2P deployment tasks which loaded from different class loaders.
 */
@SuppressWarnings({"ProhibitedExceptionDeclared", "ProhibitedExceptionThrown"})
@GridCommonTest(group = "P2P")
public class GridP2PDifferentClassLoaderSelfTest extends GridCommonAbstractTest {
    /**
     * Class Name of task 1.
     */
    private static final String TEST_TASK1_NAME = "org.gridgain.grid.tests.p2p.GridP2PTestTaskExternalPath1";

    /**
     * Class Name of task 2.
     */
    private static final String TEST_TASK2_NAME = "org.gridgain.grid.tests.p2p.GridP2PTestTaskExternalPath2";

    /**
     * URL of classes.
     */
    private static final URL[] URLS;

    /**
     * Current deployment mode. Used in {@link #getConfiguration(String)}.
     */
    private GridDeploymentMode depMode;

    /**
     * Initialize URLs.
     */
    static {
        try {
            URLS = new URL[] {new URL(GridTestProperties.getProperty("p2p.uri.cls"))};
        }
        catch (MalformedURLException e) {
            throw new RuntimeException("Define property p2p.uri.cls", e);
        }
    }

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        cfg.setDeploymentMode(depMode);

        return cfg;
    }

    /**
     * Test.
     * @param isSameTask whether load same task or different task
     * @param expectEquals whether expected
     * @throws Exception if error occur
     */
    @SuppressWarnings({"ObjectEquality", "unchecked"})
    private void processTest(boolean isSameTask, boolean expectEquals) throws Exception {
        try {
            Grid grid1 = startGrid(1);
            Grid grid2 = startGrid(2);

            Class task1;
            Class task2;

            if (isSameTask) {
                ClassLoader ldr1 = new URLClassLoader(URLS, getClass().getClassLoader());
                ClassLoader ldr2 = new URLClassLoader(URLS, getClass().getClassLoader());

                task1 = ldr1.loadClass(TEST_TASK1_NAME);
                task2 = ldr2.loadClass(TEST_TASK1_NAME);
            }
            else {
                ClassLoader ldr1 = new GridTestExternalClassLoader(URLS, TEST_TASK2_NAME);
                ClassLoader ldr2 = new GridTestExternalClassLoader(URLS, TEST_TASK1_NAME);

                task1 = ldr1.loadClass(TEST_TASK1_NAME);
                task2 = ldr2.loadClass(TEST_TASK2_NAME);
            }

            // Execute task1 and task2 from node1 on node2 and make sure that they reuse same class loader on node2.
            int[] res1 = (int[])grid1.compute().execute(task1, grid2.localNode().id()).get();
            int[] res2 = (int[])grid1.compute().execute(task2, grid2.localNode().id()).get();

            if (expectEquals)
                assert Arrays.equals(res1, res2);
            else
                assert isNotSame(res1, res2);
        }
        finally {
            stopGrid(1);
            stopGrid(2);
        }
    }

    /**
     * Test GridDeploymentMode.PRIVATE mode.
     *
     * @throws Exception if error occur.
     */
    public void testPrivateMode() throws Exception {
        depMode = GridDeploymentMode.PRIVATE;

        processTest(false, false);
    }

    /**
     * Test GridDeploymentMode.ISOLATED mode.
     *
     * @throws Exception if error occur.
     */
    public void testIsolatedMode() throws Exception {
        depMode = GridDeploymentMode.ISOLATED;

        processTest(false, false);
    }

    /**
     * Test {@link GridDeploymentMode#CONTINUOUS} mode.
     *
     * @throws Exception if error occur.
     */
    public void testContinuousMode() throws Exception {
        depMode = GridDeploymentMode.CONTINUOUS;

        processTest(false, false);
    }

    /**
     * Test GridDeploymentMode.SHARED mode.
     *
     * @throws Exception if error occur.
     */
    public void testSharedMode() throws Exception {
        depMode = GridDeploymentMode.SHARED;

        processTest(false, false);
    }

    /**
     * Test GridDeploymentMode.PRIVATE mode.
     *
     * @throws Exception if error occur.
     */
    public void testRedeployPrivateMode() throws Exception {
        depMode = GridDeploymentMode.PRIVATE;

        processTest(true, false);
    }

    /**
     * Test GridDeploymentMode.ISOLATED mode.
     *
     * @throws Exception if error occur.
     */
    public void testRedeployIsolatedMode() throws Exception {
        depMode = GridDeploymentMode.ISOLATED;

        processTest(true, false);
    }

    /**
     * Test GridDeploymentMode.CONTINUOUS mode.
     *
     * @throws Exception if error occur.
     */
    public void testRedeployContinuousMode() throws Exception {
        depMode = GridDeploymentMode.CONTINUOUS;

        processTest(true, false);
    }

    /**
     * Test GridDeploymentMode.SHARED mode.
     *
     * @throws Exception if error occur.
     */
    public void testRedeploySharedMode() throws Exception {
        depMode = GridDeploymentMode.SHARED;

        processTest(true, false);
    }

    /**
     * Return true if and only if all elements of array are different.
     *
     * @param m1 array 1.
     * @param m2 array 2.
     * @return true if all elements of array are different.
     */
    private boolean isNotSame(int[] m1, int[] m2) {
        assert m1.length == m2.length;
        assert m1.length == 2;

        return m1[0] != m2[0] && m1[1] != m2[1];
    }
}
