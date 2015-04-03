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

package org.gridgain.grid.kernal.processors.version.os;

import org.gridgain.grid.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.kernal.processors.*;
import org.gridgain.grid.kernal.processors.version.*;
import org.gridgain.grid.product.*;

import java.nio.*;
import java.util.*;

/**
 * No-op implementation for {@link GridVersionProcessor}.
 */
public class GridOsVersionProcessor extends GridProcessorAdapter implements GridVersionProcessor {
    /**
     * @param ctx Kernal context.
     */
    public GridOsVersionProcessor(GridKernalContext ctx) {
        super(ctx);
    }

    /** {@inheritDoc} */
    @Override public void onStart(Collection<GridNode> remoteNodes) {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override public void onNodeJoined(GridNode node) {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override public void onNodeLeft(GridNode node) {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override public boolean writeDelta(UUID nodeId, Class<?> msgCls,
        ByteBuffer buf) {
        return true;
    }

    /** {@inheritDoc} */
    @Override public boolean readDelta(UUID nodeId, Class<?> msgCls,
        ByteBuffer buf) {
        return true;
    }

    /** {@inheritDoc} */
    @Override public void registerLocal(Class<?> cls, Class<? extends GridVersionConverter> converterCls,
        GridProductVersion fromVer) throws GridException {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override public void addConvertersToAttributes(Map<String, Object> attrs) {
        // No-op.
    }
}
