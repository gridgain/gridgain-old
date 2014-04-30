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

package org.gridgain.loadtests.direct.newnodes;

import org.gridgain.grid.*;
import org.gridgain.grid.spi.discovery.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.testframework.junits.common.*;

/**
 *
 */
@GridCommonTest(group = "Load Test")
public class GridSingleSplitsNewNodesMulticastLoadTest extends GridSingleSplitsNewNodesAbstractLoadTest {
    /** {@inheritDoc} */
    @Override protected GridDiscoverySpi getDiscoverySpi(GridConfiguration cfg) {
        GridDiscoverySpi discoSpi = cfg.getDiscoverySpi();

        assert discoSpi instanceof GridTcpDiscoverySpi: "Wrong default SPI implementation.";

        ((GridTcpDiscoverySpi)discoSpi).setHeartbeatFrequency(getHeartbeatFrequency());

        return discoSpi;
    }

    /** {@inheritDoc} */
    @Override protected int getHeartbeatFrequency() {
        return 3000;
    }
}
