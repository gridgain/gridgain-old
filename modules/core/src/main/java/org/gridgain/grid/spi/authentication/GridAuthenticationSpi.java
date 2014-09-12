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

package org.gridgain.grid.spi.authentication;

import org.gridgain.grid.*;
import org.gridgain.grid.security.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.authentication.noop.*;

/**
 * Authentication SPI used for authenticating grid nodes and remote clients. This SPI
 * supports only {@code authentication} and does not provide any {@code authorization}
 * functionality.
 * <p>
 * The default authentication SPI is {@link GridNoopAuthenticationSpi}
 * which permits any request.
 * <p>
 * Gridgain provides the following {@code GridAuthenticationSpi} implementations:
 * <ul>
 * <li>
 *     {@link GridNoopAuthenticationSpi} - permits any request.
 * </li>
 * <li>
 *     {@code GridPasscodeAuthenticationSpi} -
 *     validates authentication with passcode phrase.
 * </li>
 * <li>
 *     {@code GridJaasAuthenticationSpi} -
 *     validates authentication with JAAS Java extension.
 * </li>
 * </ul>
 * <p>
 * <b>NOTE:</b> multiple authentication SPIs may be started on the same grid node. In this case
 * GridGain will differentiate between them based on {@link #supported(GridSecuritySubjectType)}
 * value. The first SPI which returns {@code true} for a given subject type will be used for
 * authentication.
 * <p>
 * <b>NOTE:</b> this SPI (i.e. methods in this interface) should never be used directly. SPIs provide
 * internal view on the subsystem and is used internally by GridGain kernal. In rare use cases when
 * access to a specific implementation of this SPI is required - an instance of this SPI can be obtained
 * via {@link Grid#configuration()} method to check its configuration properties or call other non-SPI
 * methods. Note again that calling methods from this interface on the obtained instance can lead
 * to undefined behavior and explicitly not supported.
 */
@GridSpiConsistencyChecked(optional = false, checkDaemon = true)
public interface GridAuthenticationSpi extends GridSpi {
    /**
     * Checks if given subject is supported by this SPI. If not, then next authentication SPI
     * in the list will be checked.
     *
     * @param subjType Subject type.
     * @return {@code True} if subject type is supported, {@code false} otherwise.
     */
    public boolean supported(GridSecuritySubjectType subjType);

    /**
     * Authenticates a given subject (either node or remote client).
     *
     * @param authCtx Authentication context. Contains all necessary information required to authenticate
     *      the subject.
     * @return Authenticated subject context or {@code null} if authentication did not pass.
     * @throws GridSpiException If authentication resulted in system error.
     *      Note that bad credentials should not cause this exception.
     */
    public GridSecuritySubject authenticate(GridAuthenticationContext authCtx) throws GridSpiException;
}
