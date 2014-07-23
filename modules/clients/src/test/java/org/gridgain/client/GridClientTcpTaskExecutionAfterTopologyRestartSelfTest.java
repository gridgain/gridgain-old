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

package org.gridgain.client;

import org.gridgain.grid.*;
import org.gridgain.testframework.junits.common.*;

import java.util.*;

/**
 * Ensures
 */
public class GridClientTcpTaskExecutionAfterTopologyRestartSelfTest extends GridCommonAbstractTest {
    /** Port. */
    private static final int PORT = 11211;

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg =  super.getConfiguration(gridName);

        cfg.setLocalHost("127.0.0.1");

        assert cfg.getClientConnectionConfiguration() == null;

        GridClientConnectionConfiguration clientCfg = new GridClientConnectionConfiguration();

        clientCfg.setRestTcpPort(PORT);

        cfg.setClientConnectionConfiguration(clientCfg);

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        stopAllGrids();
    }

    /**
     * @throws Exception If failed.
     */
    public void testTaskAfterRestart() throws Exception {
        startGrids(1);

        GridClientConfiguration cfg = new GridClientConfiguration();

        cfg.setProtocol(GridClientProtocol.TCP);
        cfg.setServers(Collections.singleton("127.0.0.1:" + PORT));

        GridClient cli = GridClientFactory.start(cfg);

        cli.compute().execute(GridClientTcpTask.class.getName(), Collections.singletonList("arg"));

        stopAllGrids();

        startGrid();

        cli.compute().execute(GridClientTcpTask.class.getName(), Collections.singletonList("arg"));
    }
}
