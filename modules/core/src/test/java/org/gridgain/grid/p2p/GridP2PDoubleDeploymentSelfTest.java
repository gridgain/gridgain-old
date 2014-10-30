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
import org.gridgain.grid.compute.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.testframework.*;
import org.gridgain.testframework.junits.common.*;

import java.util.*;

/**
 *
 */
@SuppressWarnings({"ProhibitedExceptionDeclared"})
@GridCommonTest(group = "P2P")
public class GridP2PDoubleDeploymentSelfTest extends GridCommonAbstractTest {
    /** Deployment mode. */
    private GridDeploymentMode depMode;

    /** IP finder. */
    private final GridTcpDiscoveryIpFinder ipFinder = new GridTcpDiscoveryVmIpFinder(true);

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        // Override P2P configuration to exclude Task and Job classes
        cfg.setPeerClassLoadingLocalClassPathExclude(GridP2PTestTask.class.getName(),
                GridP2PTestJob.class.getName());

        // Test requires SHARED mode to test local deployment priority over p2p.
        cfg.setDeploymentMode(depMode);

        GridTcpDiscoverySpi discoSpi = new GridTcpDiscoverySpi();

        discoSpi.setIpFinder(ipFinder);

        cfg.setDiscoverySpi(discoSpi);

        cfg.setCacheConfiguration();

        return cfg;
    }

    /**
     * @param depMode deployment mode.
     * @throws Exception If failed.
     */
    @SuppressWarnings("unchecked")
    private void processTestBothNodesDeploy(GridDeploymentMode depMode) throws Exception {
        try {
            this.depMode = depMode;

            Grid grid1 = startGrid(1);
            Grid grid2 = startGrid(2);

            ClassLoader ldr = new GridTestClassLoader(
                Collections.singletonMap("org/gridgain/grid/p2p/p2p.properties", "resource=loaded"),
                GridP2PTestTask.class.getName(),
                GridP2PTestJob.class.getName()
            );

            Class<? extends GridComputeTask<?, ?>> taskCls =
                (Class<? extends GridComputeTask<?, ?>>)ldr.loadClass(GridP2PTestTask.class.getName());

            grid1.compute().localDeployTask(taskCls, ldr);

            Integer res1 = (Integer)grid1.compute().execute(taskCls.getName(), 1).get();

            grid1.compute().undeployTask(taskCls.getName());

            // Wait here 1 sec before the deployment as we have async undeploy.
            Thread.sleep(1000);

            grid1.compute().localDeployTask(taskCls, ldr);
            grid2.compute().localDeployTask(taskCls, ldr);

            Integer res2 = (Integer)grid2.compute().execute(taskCls.getName(), 2).get();

            info("Checking results...");

            assert res1 == 10 : "Invalid res1 value: " + res1;
            assert res2 == 20 : "Invalid res1 value: " + res1;

            info("Tests passed.");
        }
        finally {
            stopGrid(2);
            stopGrid(1);
        }
    }

    /**
     * @throws Exception if error occur.
     */
    public void testPrivateMode() throws Exception {
        processTestBothNodesDeploy(GridDeploymentMode.PRIVATE);
    }

    /**
     * @throws Exception if error occur.
     */
    public void testIsolatedMode() throws Exception {
        processTestBothNodesDeploy(GridDeploymentMode.ISOLATED);
    }

    /**
     * @throws Exception if error occur.
     */
    public void testContinuousMode() throws Exception {
        processTestBothNodesDeploy(GridDeploymentMode.CONTINUOUS);
    }

    /**
     * @throws Exception if error occur.
     */
    public void testSharedMode() throws Exception {
        processTestBothNodesDeploy(GridDeploymentMode.SHARED);
    }
}
