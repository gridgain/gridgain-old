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

package org.gridgain.grid.kernal.processors.interop.os;

import org.gridgain.grid.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.kernal.processors.interop.*;
import org.jetbrains.annotations.*;

/**
 * OS interop processor.
 */
public class GridOsInteropProcessor extends GridInteropProcessorAdapter {
    /** Common error message. */
    private static final String ERR_MSG = "Interop feature is not supported in OS edition.";

    /**
     * Constructor.
     *
     * @param ctx Context.
     */
    public GridOsInteropProcessor(GridKernalContext ctx) {
        super(ctx);
    }

    /** {@inheritDoc} */
    @Override public void releaseStart() {
        throw new UnsupportedOperationException(ERR_MSG);
    }

    /** {@inheritDoc} */
    @Override public void awaitStart() throws GridException {
        throw new UnsupportedOperationException(ERR_MSG);
    }

    /** {@inheritDoc} */
    @Override public long environmentPointer() throws GridException {
        throw new UnsupportedOperationException(ERR_MSG);
    }

    /** {@inheritDoc} */
    @Override public String gridName() {
        throw new UnsupportedOperationException(ERR_MSG);
    }

    /** {@inheritDoc} */
    @Override public void close(boolean cancel) {
        throw new UnsupportedOperationException(ERR_MSG);
    }

    /** {@inheritDoc} */
    @Override public GridInteropTarget projection() throws GridException {
        throw new UnsupportedOperationException(ERR_MSG);
    }

    /** {@inheritDoc} */
    @Override public GridInteropTarget cache(@Nullable String name) throws GridException {
        throw new UnsupportedOperationException(ERR_MSG);
    }

    /** {@inheritDoc} */
    @Override public GridInteropTarget dataLoader(@Nullable String cacheName) throws GridException {
        throw new UnsupportedOperationException(ERR_MSG);
    }
}
