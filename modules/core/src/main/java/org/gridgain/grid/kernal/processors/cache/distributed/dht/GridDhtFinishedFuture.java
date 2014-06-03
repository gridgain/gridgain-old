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

package org.gridgain.grid.kernal.processors.cache.distributed.dht;

import org.gridgain.grid.kernal.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.grid.util.future.*;

import java.io.*;
import java.util.*;

/**
 * Finished DHT future.
 */
public class GridDhtFinishedFuture<T> extends GridFinishedFuture<T> implements GridDhtFuture<T> {
    /** */
    private static final long serialVersionUID = 0L;

    /**
     * Empty constructor required by {@link Externalizable}.
     */
    public GridDhtFinishedFuture() {
        // No-op.
    }

    /**
     * @param ctx Context.
     * @param t Result.
     */
    public GridDhtFinishedFuture(GridKernalContext ctx, T t) {
        super(ctx, t);
    }

    /**
     * @param ctx Context.
     * @param err Error.
     */
    public GridDhtFinishedFuture(GridKernalContext ctx, Throwable err) {
        super(ctx, err);
    }

    /** {@inheritDoc} */
    @Override public Collection<Integer> invalidPartitions() {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridDhtFinishedFuture.class, this, super.toString());
    }
}
