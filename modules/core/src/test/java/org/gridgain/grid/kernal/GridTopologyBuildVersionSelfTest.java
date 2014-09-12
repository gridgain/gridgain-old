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

package org.gridgain.grid.kernal;

import org.gridgain.grid.*;
import org.gridgain.grid.product.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.testframework.junits.common.*;

import java.util.*;
import java.util.concurrent.atomic.*;

/**
 * Tests build version setting into discovery maps.
 */
public class GridTopologyBuildVersionSelfTest extends GridCommonAbstractTest {
    /** IP finder. */
    private static final GridTcpDiscoveryIpFinder IP_FINDER = new GridTcpDiscoveryVmIpFinder(true);

    /** Counter. */
    private static final AtomicInteger cnt = new AtomicInteger();

    /** Test compatible versions. */
    private static final Collection<String> COMPATIBLE_VERS =
        F.asList("1.0.0-ent", "2.0.0-ent", "3.0.0-ent", "4.0.0-ent");

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        final int idx = cnt.incrementAndGet();

        // Override node attributes in discovery spi.
        GridTcpDiscoverySpi discoSpi = new GridTcpDiscoverySpi() {
            @Override public void setNodeAttributes(Map<String, Object> attrs, GridProductVersion ver) {
                super.setNodeAttributes(attrs, ver);

                attrs.put(GridNodeAttributes.ATTR_BUILD_VER, idx + ".0.0" + "-ent");

                if (idx < 3)
                    attrs.remove(GridNodeAttributes.ATTR_BUILD_DATE);
                else
                    attrs.put(GridNodeAttributes.ATTR_BUILD_DATE, "1385099743");

                attrs.put(GridNodeAttributes.ATTR_COMPATIBLE_VERS, COMPATIBLE_VERS);
            }
        };

        discoSpi.setIpFinder(IP_FINDER);

        cfg.setDiscoverySpi(discoSpi);

        return cfg;
    }

    /**
     * @throws Exception If failed.
     */
    public void testVersioning() throws Exception {
        startGrids(4);

        try {
            for (int i = 3; i >= 0; i--) {
                GridKernal g = (GridKernal)grid(i);

                NavigableMap<GridProductVersion, Collection<GridNode>> verMap = g.context().discovery()
                    .topologyVersionMap();

                assertEquals(4, verMap.size());

                // Now check the map itself.
                assertEquals(4, verMap.get(GridProductVersion.fromString("1.0.0")).size());
                assertEquals(3, verMap.get(GridProductVersion.fromString("2.0.0")).size());
                assertEquals(2, verMap.get(GridProductVersion.fromString("3.0.0-ent-1385099743")).size());
                assertEquals(1, verMap.get(GridProductVersion.fromString("4.0.0-ent-1385099743")).size());
            }
        }
        finally {
            stopAllGrids();
        }
    }
}
