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

package org.gridgain.grid.kernal.managers.security;

import org.gridgain.grid.*;
import org.gridgain.grid.kernal.managers.*;
import org.gridgain.grid.security.*;
import org.gridgain.grid.spi.authentication.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * This interface defines a grid authentication manager.
 */
public interface GridSecurityManager extends GridManager {
    /**
     * Checks if security check is enabled.
     *
     * @return {@code True} if authentication check is enabled.
     */
    public boolean securityEnabled();

    /**
     * Authenticates grid node with it's attributes via underlying {@link GridAuthenticationSpi}s.
     *
     * @param node Node id to authenticate.
     * @param cred Security credentials.
     * @return {@code True} if succeeded, {@code false} otherwise.
     * @throws GridException If error occurred.
     */
    public GridSecurityContext authenticateNode(GridNode node, GridSecurityCredentials cred) throws GridException;

    /**
     * Authenticates subject via underlying {@link GridAuthenticationSpi}s.
     *
     * @param ctx Authentication context.
     * @return {@code True} if succeeded, {@code false} otherwise.
     * @throws GridException If error occurred.
     */
    public GridSecurityContext authenticate(GridAuthenticationContext ctx) throws GridException;

    /**
     * Gets collection of authenticated nodes.
     *
     * @return Collection of authenticated nodes.
     * @throws GridException If error occurred.
     */
    public Collection<GridSecuritySubject> authenticatedSubjects() throws GridException;

    /**
     * Gets authenticated node subject.
     *
     * @param subjId Subject ID.
     * @return Security subject.
     * @throws GridException If error occurred.
     */
    public GridSecuritySubject authenticatedSubject(UUID subjId) throws GridException;

    /**
     * Authorizes grid operation.
     *
     * @param name Cache name or task class name.
     * @param perm Permission to authorize.
     * @param securityCtx Optional security context.
     * @throws GridSecurityException If security check failed.
     */
    public void authorize(String name, GridSecurityPermission perm, @Nullable GridSecurityContext securityCtx)
        throws GridSecurityException;

    /**
     * Callback invoked when subject session got expired.
     *
     * @param subjId Subject ID.
     */
    public void onSessionExpired(UUID subjId);
}
