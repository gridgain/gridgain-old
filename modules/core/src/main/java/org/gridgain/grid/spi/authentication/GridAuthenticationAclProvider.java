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

import java.util.*;

/**
 * Access control list provider. Specific SPI implementation may use this
 * interface for declarative user permission specifications.
 * <p>
 * Abstracting access control specification through a provider allows users
 * to implement custom stores for per-user access control specifications,
 * e.g. encrypting them or storing in a separate file.
 */
public interface GridAuthenticationAclProvider {
    /**
     * Gets per-user access control map.
     *
     * @return Per-user access control map.
     * @throws GridException If failed.
     */
    public Map<GridSecurityCredentials, GridSecurityPermissionSet> acl() throws GridException;
}
