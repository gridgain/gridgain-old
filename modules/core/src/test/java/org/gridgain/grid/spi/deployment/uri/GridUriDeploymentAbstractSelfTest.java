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
import org.gridgain.testframework.config.*;
import org.gridgain.testframework.junits.spi.*;

/**
 *
 */
public abstract class GridUriDeploymentAbstractSelfTest extends GridSpiAbstractTest<GridUriDeploymentSpi> {
    /** {@inheritDoc} */
    @Override protected void afterTestsStopped() throws Exception {
        getSpi().setListener(null);
    }

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        getSpi().setListener(new GridDeploymentListener() {
            @Override public void onUnregistered(ClassLoader ldr) {
                // No-op.
            }
        });
    }

    /**
     * @return Temporary directory to be used in test.
     */
    @GridSpiTestConfig
    public String getTemporaryDirectoryPath() {
        String path = GridTestProperties.getProperty("deploy.uri.tmpdir");

        assert path != null;

        return path;
    }

    /**
     * @param taskName Name of available task.
     * @throws Exception if failed.
     */
    protected void checkTask(String taskName) throws Exception {
        assert taskName != null;

        GridDeploymentResource task = getSpi().findResource(taskName);

        assert task != null;

        info("Deployed task [task=" + task + ']');
    }

    /**
     * @param taskName name of unavailable task.
     * @throws Exception if failed.
     */
    protected void checkNoTask(String taskName) throws Exception {
        assert taskName != null;

        GridDeploymentResource task = getSpi().findResource(taskName);

        assert task == null;

        info("Not deployed task [task=" + task + ']');
    }
}
