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
import org.gridgain.grid.kernal.managers.securesession.*;
import org.gridgain.grid.security.*;

import java.util.*;

/**
 * Implementation of grid security interface.
 */
public class GridSecurityImpl implements GridSecurity {
    /** Security manager. */
    private GridSecurityManager secMgr;

    /** Secure session manager. */
    private GridSecureSessionManager secSesMgr;

    /**
     * @param secMgr Security manager.
     * @param secSesMgr Secure session manager.
     */
    public GridSecurityImpl(GridSecurityManager secMgr, GridSecureSessionManager secSesMgr) {
        this.secMgr = secMgr;
        this.secSesMgr = secSesMgr;
    }

    /** {@inheritDoc} */
    @Override public Collection<GridSecuritySubject> authenticatedSubjects() throws GridException {
        Collection<GridSecuritySubject> res = new ArrayList<>();

        // First add all nodes.
        res.addAll(secMgr.authenticatedNodes());
        res.addAll(secSesMgr.authenticatedClients());

        return res;
    }

    /** {@inheritDoc} */
    @Override public GridSecuritySubject authenticatedSubject(UUID subjId) throws GridException {
        GridSecuritySubject subj = secMgr.authenticatedNode(subjId);

        if (subj != null)
            return subj;

        return secSesMgr.authenticatedClient(subjId);
    }
}
