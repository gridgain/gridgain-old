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

package org.gridgain.grid.kernal.managers.securesession;

import org.gridgain.grid.*;
import org.gridgain.grid.kernal.managers.*;
import org.gridgain.grid.kernal.managers.security.*;
import org.gridgain.grid.security.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * This interface defines a grid secure session manager.
 */
public interface GridSecureSessionManager extends GridManager {
    /**
     * @param subjType Subject type.
     * @param subjId Subject ID.
     * @param tok Token.
     * @param params Parameters.
     * @return Validated secure session or {@code null} if session is not valid.
     * @throws GridException If error occurred.
     */
    public GridSecureSession validateSession(GridSecuritySubjectType subjType, UUID subjId, @Nullable byte[] tok,
        @Nullable Object params) throws GridException;

    /**
     * Generates secure session token.
     *
     * @param subjType Subject type.
     * @param subjId Subject ID.
     * @param subjCtx Authentication subject context.
     * @param params Params.
     * @return Generated session token.
     */
    public byte[] updateSession(GridSecuritySubjectType subjType, UUID subjId, GridSecurityContext subjCtx,
        @Nullable Object params) throws GridException;
}
