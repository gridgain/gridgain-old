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

package org.gridgain.grid.kernal.managers.authentication;

import org.gridgain.grid.*;
import org.gridgain.grid.kernal.managers.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.authentication.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * This interface defines a grid authentication manager.
 */
public interface GridAuthenticationManager extends GridManager {
    /**
     * Checks if security check is enabled.
     *
     * @return {@code True} if authentication check is enabled.
     */
    public boolean securityEnabled();

    /**
     * Authenticates grid node with it's attributes via underlying {@link GridAuthenticationSpi}s.
     *
     * @param nodeId Node id to authenticate.
     * @param attrs Node attributes.
     * @return {@code True} if succeeded, {@code false} otherwise.
     * @throws GridException If error occurred.
     */
    public boolean authenticateNode(UUID nodeId, Map<String, Object> attrs) throws GridException;

    /**
     * Authenticates subject via underlying {@link GridAuthenticationSpi}s.
     *
     * @param subjType Subject type.
     * @param subjId Subject ID.
     * @param creds Credentials.
     * @return {@code True} if succeeded, {@code false} otherwise.
     * @throws GridException If error occurred.
     */
    public boolean authenticate(GridSecuritySubjectType subjType, byte[] subjId,
        @Nullable Object creds) throws GridException;
}
