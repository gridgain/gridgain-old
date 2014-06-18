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

import org.gridgain.grid.*;

/**
 * Basic implementation for {@link GridSecurityCredentialsProvider}. Use it
 * when custom logic for storing security credentials is not required and it
 * is OK to specify credentials directly in configuration.
 */
public class GridSecurityCredentialsBasicProvider implements GridSecurityCredentialsProvider {
    /** */
    private GridSecurityCredentials cred;

    /**
     * Constructs security credentials provider based on security credentials passed in.
     *
     * @param cred Security credentials.
     */
    public GridSecurityCredentialsBasicProvider(GridSecurityCredentials cred) {
        this.cred = cred;
    }

    /** {@inheritDoc} */
    @Override public GridSecurityCredentials credentials() throws GridException {
        return cred;
    }
}
