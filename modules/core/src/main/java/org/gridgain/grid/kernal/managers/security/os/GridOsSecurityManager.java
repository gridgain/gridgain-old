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

package org.gridgain.grid.kernal.managers.security.os;

import org.gridgain.grid.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.kernal.managers.*;
import org.gridgain.grid.kernal.managers.security.*;
import org.gridgain.grid.security.*;
import org.gridgain.grid.spi.authentication.*;
import org.gridgain.grid.util.typedef.*;
import org.jetbrains.annotations.*;

import java.net.*;
import java.util.*;

/**
 * No-op implementation for {@link GridSecurityManager}.
 */
public class GridOsSecurityManager extends GridNoopManagerAdapter implements GridSecurityManager {
    /**
     * @param ctx Kernal context.
     */
    public GridOsSecurityManager(GridKernalContext ctx) {
        super(ctx);
    }

    /** Allow all permissions. */
    private static final GridSecurityPermissionSet ALLOW_ALL = new GridAllowAllPermissionSet();

    /** {@inheritDoc} */
    @Override public GridSecurityContext authenticateNode(GridNode node, GridSecurityCredentials cred)
        throws GridException {
        GridSecuritySubjectAdapter s = new GridSecuritySubjectAdapter(GridSecuritySubjectType.REMOTE_NODE, node.id());

        s.address(new InetSocketAddress(F.first(node.addresses()), 0));

        s.permissions(ALLOW_ALL);

        return new GridSecurityContext(s);
    }

    /** {@inheritDoc} */
    @Override public boolean isGlobalNodeAuthentication() {
        return false;
    }

    /** {@inheritDoc} */
    @Override public GridSecurityContext authenticate(GridAuthenticationContext authCtx) throws GridException {
        GridSecuritySubjectAdapter s = new GridSecuritySubjectAdapter(authCtx.subjectType(), authCtx.subjectId());

        s.permissions(ALLOW_ALL);
        s.address(authCtx.address());

        if (authCtx.credentials() != null)
            s.login(authCtx.credentials().getLogin());

        return new GridSecurityContext(s);
    }

    /** {@inheritDoc} */
    @Override public Collection<GridSecuritySubject> authenticatedSubjects() {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override public GridSecuritySubject authenticatedSubject(UUID nodeId) {
        return null;
    }

    /** {@inheritDoc} */
    @Override public void authorize(String name, GridSecurityPermission perm, @Nullable GridSecurityContext securityCtx)
        throws GridSecurityException {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override public void onSessionExpired(UUID subjId) {
        // No-op.
    }
}
