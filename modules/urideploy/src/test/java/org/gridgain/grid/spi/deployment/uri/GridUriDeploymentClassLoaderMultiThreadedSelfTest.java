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

package org.gridgain.grid.spi.deployment.uri;

import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.testframework.config.*;
import org.gridgain.testframework.junits.common.*;
import org.jetbrains.annotations.*;

import java.net.*;
import java.util.concurrent.*;

/**
 * Grid URI deployment class loader self test.
 */
public class GridUriDeploymentClassLoaderMultiThreadedSelfTest extends GridCommonAbstractTest {
    /**
     * @throws Exception If failed.
     */
    public void testMultiThreadedClassLoading() throws Exception {
        for (int i = 0; i < 50; i++)
            doTest();
    }

    /**
     * @throws Exception If failed.
     */
    private void doTest() throws Exception {
        final GridUriDeploymentClassLoader ldr = new GridUriDeploymentClassLoader(
            new URL[] { U.resolveGridGainUrl(GridTestProperties.getProperty("ant.urideployment.gar.file")) },
                getClass().getClassLoader());

        multithreaded(
            new Callable<Object>() {
                @Nullable @Override public Object call() throws Exception {
                    ldr.loadClass("org.gridgain.grid.spi.deployment.uri.tasks.GridUriDeploymentTestTask0");

                    return null;
                }
            },
            500
        );

        final GridUriDeploymentClassLoader ldr0 = new GridUriDeploymentClassLoader(
            new URL[] { U.resolveGridGainUrl(GridTestProperties.getProperty("ant.urideployment.gar.file")) },
            getClass().getClassLoader());

        multithreaded(
            new Callable<Object>() {
                @Nullable @Override public Object call() throws Exception {
                    ldr0.loadClassGarOnly("org.gridgain.grid.spi.deployment.uri.tasks.GridUriDeploymentTestTask0");

                    return null;
                }
            },
            500
        );
    }
}
