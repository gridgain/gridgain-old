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
import org.gridgain.grid.marshaller.jdk.*;
import org.gridgain.grid.marshaller.optimized.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.testframework.junits.common.*;

/**
 * Test for {@link GridTcpDiscoverySpi}.
 */
public class GridTcpDiscoveryMarshallerCheckSelfTest extends GridCommonAbstractTest {
    /** */
    private static boolean sameMarsh;

    /** */
    private static boolean flag;

    /** */
    private static GridTcpDiscoveryIpFinder ipFinder = new GridTcpDiscoveryVmIpFinder(true);

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg =  super.getConfiguration(gridName);

        GridTcpDiscoverySpi discoSpi = new GridTcpDiscoverySpi();

        discoSpi.setIpFinder(ipFinder);

        cfg.setDiscoverySpi(discoSpi);

        cfg.setLocalHost("127.0.0.1");

        if (flag)
            cfg.setMarshaller(new GridJdkMarshaller());
        else
            cfg.setMarshaller(sameMarsh ? new GridJdkMarshaller() : new GridOptimizedMarshaller());

        // Flip flag.
        flag = !flag;

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        stopAllGrids();

        flag = false;
    }

    /**
     * @throws Exception If failed.
     */
    public void testMarshallerInConsistency() throws Exception {
        sameMarsh = false;

        startGrid(1);

        try {
            startGrid(2);

            fail("Expected SPI exception was not thrown.");
        }
        catch (GridException e) {
            Throwable ex = e.getCause().getCause();

            assertTrue(ex instanceof GridSpiException);
            assertTrue(ex.getMessage().contains("Local node's marshaller differs from remote node's marshaller"));
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testMarshallerConsistency() throws Exception {
        sameMarsh = true;

        startGrid(1);
        startGrid(2);
    }
}
