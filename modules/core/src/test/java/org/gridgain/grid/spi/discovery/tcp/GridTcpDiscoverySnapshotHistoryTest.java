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

package org.gridgain.grid.spi.discovery.tcp;

import org.gridgain.grid.*;
import org.gridgain.grid.spi.discovery.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.testframework.junits.common.*;

import static org.gridgain.grid.spi.discovery.tcp.GridTcpDiscoverySpi.*;

/**
 * Tests for topology snapshots history.
 */
public class GridTcpDiscoverySnapshotHistoryTest extends GridCommonAbstractTest {
    /** */
    public GridTcpDiscoverySnapshotHistoryTest() {
        super(false);
    }

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        cfg.setDiscoverySpi(new GridTcpDiscoverySpi());
        cfg.setCacheConfiguration();
        cfg.setLocalHost("127.0.0.1");
        cfg.setRestEnabled(false);

        return cfg;
    }

    /**
     * @throws Exception If any error occurs.
     */
    public void testHistorySupported() throws Exception {
        try {
            final Grid g = startGrid();

            GridDiscoverySpi spi = g.configuration().getDiscoverySpi();

            GridDiscoverySpiHistorySupport ann = U.getAnnotation(spi.getClass(), GridDiscoverySpiHistorySupport.class);

            assertNotNull("Spi does not have annotation for history support", ann);

            assertTrue("History support is disabled for current spi", ann.value());
        }
        finally {
            stopGrid();
        }
    }

    /**
     * @throws Exception If any error occurs.
     */
    public void testSettingNewTopologyHistorySize() throws Exception {
        try {
            final Grid g = startGrid();

            GridTcpDiscoverySpi spi = (GridTcpDiscoverySpi)g.configuration().getDiscoverySpi();

            assertEquals(DFLT_TOP_HISTORY_SIZE, spi.getTopHistorySize());

            spi.setTopHistorySize(DFLT_TOP_HISTORY_SIZE + 1);

            assertEquals(DFLT_TOP_HISTORY_SIZE + 1, spi.getTopHistorySize());

            spi.setTopHistorySize(1);

            assertEquals(DFLT_TOP_HISTORY_SIZE + 1, spi.getTopHistorySize());
        }
        finally {
            stopGrid();
        }
    }

    /**
     * @throws Exception If any error occurs.
     */
    public void testNodeAdded() throws Exception {
        try {
            // Add grid #1
            final Grid g1 = startGrid(1);

            assertTopVer(1, g1);

            assertEquals(1, g1.topologyVersion());

            // Add grid # 2
            final Grid g2 = startGrid(2);

            assertTopVer(2, g1, g2);

            for (int i = 1; i <= 2; i++)
                assertEquals(i, g2.topology(i).size());

            // Add grid # 3
            final Grid g3 = startGrid(3);

            assertTopVer(3, g1, g2, g3);

            for (int i = 1; i <= 3; i++)
                assertEquals(i, g3.topology(i).size());
        }
        finally {
            stopAllGrids();
        }
    }

    /**
     * @throws Exception If any error occurs.
     */
    public void testNodeAddedAndRemoved() throws Exception {
        try {
            // Add grid #1
            final Grid g1 = startGrid(1);

            assertTopVer(1, g1);

            assertEquals(1, g1.topologyVersion());

            // Add grid #2
            final Grid g2 = startGrid(2);

            assertTopVer(2, g1, g2);

            for (int i = 1; i <= 2; i++)
                assertEquals(i, g2.topology(i).size());

            // Add grid #3
            final Grid g3 = startGrid(3);

            assertTopVer(3, g1, g2, g3);

            for (int i = 1; i <= 3; i++)
                assertEquals(i, g3.topology(i).size());

            // Stop grid #3
            stopGrid(g3.name());

            assertTopVer(4, g1, g2);
        }
        finally {
            stopAllGrids();
        }
    }

    /**
     * Check if specified grid instances have unexpected topology version.
     *
     * @param expTopVer Expected topology version.
     * @param grids Grid instances for checking topology version.
     */
    private static void assertTopVer(long expTopVer, Grid... grids) {
        for (Grid g : grids)
            assertEquals("Grid has wrong topology version.", expTopVer, g.topologyVersion());
    }
}
