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

package org.gridgain.grid.spi.authentication.jaas;

import org.gridgain.grid.spi.*;
import org.gridgain.grid.util.mbean.*;

/**
 * Management bean for {@link GridJaasAuthenticationSpi}.
 */
@GridMBeanDescription("MBean that provides access to Jaas-based authentication SPI configuration.")
public interface GridJaasAuthenticationSpiMBean extends GridSpiManagementMBean {
    /**
     * Gets login context name.
     *
     * @return Login context name.
     */
    @GridMBeanDescription("Login context name.")
    public String getLoginContextName();

    /**
     * Sets new login context name.
     *
     * @param loginCtxName New login context name.
     */
    @GridMBeanDescription("Sets login context name.")
    public void setLoginContextName(String loginCtxName);

    /**
     * Gets JAAS-authentication callback handler factory name.
     *
     * @return JAAS-authentication callback handler factory name.
     */
    @GridMBeanDescription("String presentation of JAAS-authentication callback handler factory.")
    public String getCallbackHandlerFactoryFormatted();
}
