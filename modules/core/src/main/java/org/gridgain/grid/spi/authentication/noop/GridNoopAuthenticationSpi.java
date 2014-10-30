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

package org.gridgain.grid.spi.authentication.noop;

import org.gridgain.grid.kernal.managers.security.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.security.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.authentication.*;
import org.gridgain.grid.util.tostring.*;
import org.gridgain.grid.util.typedef.internal.*;

/**
 * Default implementation of the authentication SPI which permits any request.
 * <p>
 * <h1 class="header">Configuration</h1>
 * <h2 class="header">Mandatory</h2>
 * This SPI has no mandatory configuration parameters.
 * <h2 class="header">Optional</h2>
 * This SPI has no optional configuration parameters.
 * <h2 class="header">Java Example</h2>
 * GridNoopAuthenticationSpi is used by default and has no parameters to be explicitly configured.
 * <pre name="code" class="java">
 * GridNoopAuthenticationSpi authSpi = new GridNoopAuthenticationSpi();
 *
 * GridConfiguration cfg = new GridConfiguration();
 *
 * // Override default authentication SPI.
 * cfg.setAuthenticationSpi(authSpi);
 *
 * // Start grid.
 * GridGain.start(cfg);
 * </pre>
 * <h2 class="header">Spring Example</h2>
 * GridNoopAuthenticationSpi can be configured from Spring XML configuration file:
 * <pre name="code" class="xml">
 * &lt;bean id="grid.custom.cfg" class="org.gridgain.grid.GridConfiguration" singleton="true"&gt;
 *         ...
 *         &lt;property name="authenticationSpi"&gt;
 *             &lt;bean class="org.gridgain.grid.spi.authentication.noop.GridNoopAuthenticationSpi"/&gt;
 *         &lt;/property&gt;
 *         ...
 * &lt;/bean&gt;
 * </pre>
 * <p>
 * <img src="http://www.gridgain.com/images/spring-small.png">
 * <br>
 * For information about Spring framework visit <a href="http://www.springframework.org/">www.springframework.org</a>
 */
@GridSpiNoop
@GridSpiMultipleInstancesSupport(true)
public class GridNoopAuthenticationSpi extends GridSpiAdapter
    implements GridAuthenticationSpi, GridNoopAuthenticationSpiMBean {
    /** Injected grid logger. */
    @GridLoggerResource
    @GridToStringExclude
    private GridLogger log;

    /** Always allow permission set. */
    private static final GridSecurityPermissionSet allowAll = new GridAllowAllPermissionSet();

    /** {@inheritDoc} */
    @Override public boolean supported(GridSecuritySubjectType subjType) {
        // If this SPI is configured, then authentication is disabled.
        return true;
    }

    /** {@inheritDoc} */
    @Override public GridSecuritySubject authenticate(GridAuthenticationContext authCtx) throws GridSpiException {
        GridSecuritySubjectAdapter subj = new GridSecuritySubjectAdapter(authCtx.subjectType(), authCtx.subjectId());

        subj.address(authCtx.address());
        subj.permissions(allowAll);

        if (authCtx.credentials() != null)
            subj.login(authCtx.credentials().getLogin());

        return subj;
    }

    /** {@inheritDoc} */
    @Override public void spiStart(String gridName) throws GridSpiException {
        // Start SPI start stopwatch.
        startStopwatch();

        registerMBean(gridName, this, GridNoopAuthenticationSpiMBean.class);

        // Ack ok start.
        if (log.isDebugEnabled())
            log.debug(startInfo());
    }

    /** {@inheritDoc} */
    @Override public void spiStop() throws GridSpiException {
        unregisterMBean();

        // Ack ok stop.
        if (log.isDebugEnabled())
            log.debug(stopInfo());
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridNoopAuthenticationSpi.class, this);
    }
}
