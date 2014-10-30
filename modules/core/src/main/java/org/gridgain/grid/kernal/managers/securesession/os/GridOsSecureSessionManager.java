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

package org.gridgain.grid.kernal.managers.securesession.os;

import org.gridgain.grid.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.kernal.managers.*;
import org.gridgain.grid.kernal.managers.securesession.*;
import org.gridgain.grid.kernal.managers.security.*;
import org.gridgain.grid.security.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * No-op implementation for {@link GridSecureSessionManager}.
 */
public class GridOsSecureSessionManager extends GridNoopManagerAdapter implements GridSecureSessionManager {
    /** Empty bytes. */
    private static final byte[] EMPTY_BYTES = new byte[0];

    /**
     * @param ctx Kernal context.
     */
    public GridOsSecureSessionManager(GridKernalContext ctx) {
        super(ctx);
    }

    /** {@inheritDoc} */
    @Override public GridSecureSession validateSession(GridSecuritySubjectType subjType, UUID subjId,
        @Nullable byte[] tok,
        @Nullable Object params) throws GridException {
        return null;
    }

    /** {@inheritDoc} */
    @Override public byte[] updateSession(GridSecuritySubjectType subjType, UUID subjId, GridSecurityContext subjCtx,
        @Nullable Object params) {
        return EMPTY_BYTES;
    }
}
