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

package org.gridgain.grid.security;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Security subject representing authenticated node or client with a set of permissions.
 * List of authenticated subjects can be retrieved from {@link GridSecurity#authenticatedSubjects()} method.
 */
public interface GridSecuritySubject extends Serializable {
    /**
     * Gets subject ID.
     *
     * @return Subject ID.
     */
    public UUID id();

    /**
     * Gets subject type, either node or client.
     *
     * @return Subject type.
     */
    public GridSecuritySubjectType type();

    /**
     * Login provided via subject security credentials.
     *
     * @return Login object.
     */
    public Object login();

    /**
     * Gets subject connection address. Usually {@link InetSocketAddress} representing connection IP and port.
     *
     * @return Subject connection address.
     */
    public InetSocketAddress address();

    /**
     * Authorized permission set for the subject.
     *
     * @return Authorized permission set for the subject.
     */
    public GridSecurityPermissionSet permissions();
}
