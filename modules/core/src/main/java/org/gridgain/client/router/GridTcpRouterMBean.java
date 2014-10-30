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

package org.gridgain.client.router;

import org.gridgain.grid.util.mbean.*;

import java.util.*;

/**
 * MBean interface for TCP router.
 */
@GridMBeanDescription("MBean for TCP router.")
public interface GridTcpRouterMBean {
    /**
     * Gets host for TCP binary protocol server.
     *
     * @return TCP host.
     */
    @GridMBeanDescription("Host for TCP binary protocol server.")
    public String getHost();

    /**
     * Gets port for TCP binary protocol server.
     *
     * @return TCP port.
     */
    @GridMBeanDescription("Port for TCP binary protocol server.")
    public int getPort();

    /**
     * Gets a flag indicating whether or not remote clients will be required to have a valid SSL certificate which
     * validity will be verified with trust manager.
     *
     * @return Whether or not client authentication is required.
     */
    @GridMBeanDescription("Flag indicating whether or not SSL is enabled for incoming connections.")
    public boolean isSslEnabled();

    /**
     * Gets a flag indicating whether or not remote clients will be required to have a valid SSL certificate which
     * validity will be verified with trust manager.
     *
     * @return Whether or not client authentication is required.
     */
    @GridMBeanDescription("Flag indicating whether or not remote clients are required to have a valid SSL certificate.")
    public boolean isSslClientAuth();

    /**
     * Gets list of server addresses where router's embedded client should connect.
     *
     * @return List of server addresses.
     */
    @GridMBeanDescription("Gets list of server addresses where router's embedded client should connect.")
    public Collection<String> getServers();

    /**
     * Returns number of messages received by this router.
     * Note that this parameter has approximate value.
     *
     * @return Number of messages received by this router.
     */
    @GridMBeanDescription("Number of messages received by this router.")
    public long getReceivedCount();

    /**
     * Returns number of responses returned by this router.
     * Note that this parameter has approximate value.
     *
     * @return Number of responses returned by this router.
     */
    @GridMBeanDescription("Number of responses returned by this router.")
    public long getSendCount();
}
