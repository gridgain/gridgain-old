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
import org.gridgain.grid.kernal.managers.security.*;
import org.gridgain.grid.security.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.discovery.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.testframework.junits.spi.*;

import java.util.*;

/**
 * Grid TCP discovery SPI start stop self test.
 */
@GridSpiTest(spi = GridTcpDiscoverySpi.class, group = "Discovery SPI")
public class GridTcpDiscoverySpiStartStopSelfTest extends GridSpiStartStopAbstractTest<GridTcpDiscoverySpi> {
    /**
     * @return IP finder.
     */
    @GridSpiTestConfig
    public GridTcpDiscoveryIpFinder getIpFinder() {
        return new GridTcpDiscoveryVmIpFinder(true);
    }

    /**
     * @return Discovery data collector.
     */
    @GridSpiTestConfig
    public GridDiscoverySpiDataExchange getDataExchange() {
        return new GridDiscoverySpiDataExchange() {
            @Override public List<Object> collect(UUID nodeId) {
                return null;
            }

            @Override public void onExchange(List<Object> data) {
                // No-op.
            }
        };
    }

    /**
     * Discovery SPI authenticator.
     *
     * @return Authenticator.
     */
    @GridSpiTestConfig
    public GridDiscoverySpiNodeAuthenticator getAuthenticator() {
        return new GridDiscoverySpiNodeAuthenticator() {
            @Override public GridSecurityContext authenticateNode(GridNode n, GridSecurityCredentials cred) {
                GridSecuritySubjectAdapter subj = new GridSecuritySubjectAdapter(
                    GridSecuritySubjectType.REMOTE_NODE, n.id());

                subj.permissions(new GridAllowAllPermissionSet());

                return new GridSecurityContext(subj);
            }

            @Override public boolean isGlobalNodeAuthentication() {
                return false;
            }
        };
    }
}
