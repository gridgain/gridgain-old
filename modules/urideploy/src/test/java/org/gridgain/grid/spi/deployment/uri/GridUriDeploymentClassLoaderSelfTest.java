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

import org.gridgain.grid.spi.deployment.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.testframework.config.*;
import org.gridgain.testframework.junits.spi.*;

import java.net.*;
import java.util.*;

/**
 * Grid URI deployment class loader test.
 */
@GridSpiTest(spi = GridUriDeploymentSpi.class, group = "Deployment SPI")
public class GridUriDeploymentClassLoaderSelfTest extends GridUriDeploymentAbstractSelfTest {
    /**
     * @throws Exception If failed.
     */
    public void testNestedJarClassloading() throws Exception {
        ClassLoader ldr = getGarClassLoader();

        // Load class from nested JAR file
        assert ldr.loadClass("javax.mail.Service") != null;
    }

    /**
     * @throws Exception If failed.
     */
    public void testClasspathResourceLoading() throws Exception {
        ClassLoader ldr = getGarClassLoader();

        // Get resource from GAR file
        URL rsrcUrl = ldr.getResource("org/gridgain/test/test.properties");

        assert rsrcUrl != null;
    }

    /**
     * @return Test GAR's class loader
     * @throws Exception if test GAR wasn't deployed
     */
    private ClassLoader getGarClassLoader() throws Exception {
        GridDeploymentResource task = getSpi().findResource("GridUriDeploymentTestWithNameTask7");

        assert task != null;

        return task.getClassLoader();
    }

    /**
     * @return List of URIs to use in this test.
     */
    @GridSpiTestConfig
    public List<String> getUriList() {
        return Collections.singletonList(GridTestProperties.getProperty("ant.urideployment.gar.uri").
            replace("EXTDATA", U.resolveGridGainPath("modules/extdata").getAbsolutePath()));
    }
}
