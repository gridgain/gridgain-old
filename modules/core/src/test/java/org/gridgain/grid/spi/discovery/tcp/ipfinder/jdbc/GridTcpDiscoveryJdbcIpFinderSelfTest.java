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

package org.gridgain.grid.spi.discovery.tcp.ipfinder.jdbc;

import com.mchange.v2.c3p0.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;

/**
 * JDBC IP finder self test.
 */
public class GridTcpDiscoveryJdbcIpFinderSelfTest extends
    GridTcpDiscoveryIpFinderAbstractSelfTest<GridTcpDiscoveryJdbcIpFinder> {
    /** */
    private ComboPooledDataSource dataSrc;

    /** */
    private boolean initSchema = true;

    /**
     * Constructor.
     *
     * @throws Exception If failed.
     */
    public GridTcpDiscoveryJdbcIpFinderSelfTest() throws Exception {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override protected GridTcpDiscoveryJdbcIpFinder ipFinder() throws Exception {
        GridTcpDiscoveryJdbcIpFinder finder = new GridTcpDiscoveryJdbcIpFinder();

        assert finder.isShared() : "IP finder should be shared by default.";

        dataSrc = new ComboPooledDataSource();
        dataSrc.setDriverClass("org.h2.Driver");

        if (initSchema)
            dataSrc.setJdbcUrl("jdbc:h2:mem");
        else {
            dataSrc.setJdbcUrl("jdbc:h2:mem:jdbc_ipfinder_not_initialized_schema");

            finder.setInitSchema(false);
        }

        finder.setDataSource(dataSrc);

        return finder;
    }

    /**
     * @throws Exception If failed.
     */
    public void testInitSchemaFlag() throws Exception {
        initSchema = false;

        try {
            ipFinder().getRegisteredAddresses();

            fail("IP finder didn't throw expected exception.");
        }
        catch (GridSpiException e) {
            assertTrue(e.getMessage().contains("IP finder has not been properly initialized"));
        }
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        super.afterTest();

        initSchema = true;

        dataSrc.close();
    }
}
