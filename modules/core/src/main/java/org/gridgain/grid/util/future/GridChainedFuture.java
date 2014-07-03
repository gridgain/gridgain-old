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

package org.gridgain.grid.util.future;

import org.gridgain.grid.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.lang.*;

/**
 * Future which asynchronously resolves result from futures chained like {@code Future<Future<...Future<X>>>}.
 */
@SuppressWarnings("unchecked")
public class GridChainedFuture<X> extends GridFutureAdapter<X> implements GridInClosure<GridFuture<?>> {
    /** */
    private static final long serialVersionUID = 0L;

    /** Future completion callback. */
    private GridClosure<?, X> completionCb;

    /**
     *
     */
    public GridChainedFuture() {
        // No-op.
    }

    /**
     * @param ctx Kernal context.
     */
    public GridChainedFuture(GridKernalContext ctx) {
        super(ctx);
    }

    public GridChainedFuture(GridKernalContext ctx, GridClosure<?, X> completionCb) {
        super(ctx);

        this.completionCb = completionCb;
    }

    /**
     * @param ctx Kernal context.
     * @param syncNotify Synchronous notify flag.
     */
    public GridChainedFuture(GridKernalContext ctx, boolean syncNotify) {
        super(ctx, syncNotify);
    }

    /** {@inheritDoc} */
    @Override public void apply(GridFuture<?> fut) {
        try {
            Object res = fut.get();

            if (res instanceof GridFuture)
                ((GridFuture)res).listenAsync(this);
            else {
                GridClosure<Object, X> cb = (GridClosure<Object, X>)completionCb;

                if (cb == null)
                    onDone((X)res);
                else
                    onDone(cb.apply(res));
            }
        }
        catch (Throwable e) {
            onDone(e);
        }
    }
}
