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

package org.gridgain.grid.kernal.processors.cache.query;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.kernal.processors.cache.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.marshaller.*;
import org.gridgain.grid.util.lang.*;
import org.gridgain.grid.util.typedef.*;

import java.io.*;
import java.util.*;

/**
 * Local query future.
 */
public class GridCacheLocalQueryFuture<K, V, R> extends GridCacheQueryFutureAdapter<K, V, R> {
    /** */
    private static final long serialVersionUID = 0L;

    /** */
    private Runnable run;

    /** */
    private GridFuture<?> fut;

    /**
     * Required by {@link Externalizable}.
     */
    public GridCacheLocalQueryFuture() {
        // No-op.
    }

    /**
     * @param ctx Context.
     * @param qry Query.
     */
    protected GridCacheLocalQueryFuture(GridCacheContext<K, V> ctx, GridCacheQueryBean qry) {
        super(ctx, qry, true);

        run = new LocalQueryRunnable<>();
    }

    /**
     * Executes query runnable.
     */
    void execute() {
        fut = ctx.closure().runLocalSafe(run, true);
    }

    /** {@inheritDoc} */
    @Override protected void cancelQuery() throws GridException {
        if (fut != null)
            fut.cancel();
    }

    /** {@inheritDoc} */
    @Override protected boolean onPage(UUID nodeId, boolean last) {
        return last;
    }

    /** {@inheritDoc} */
    @Override protected void loadPage() {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override protected void loadAllPages() {
        // No-op.
    }

    /** */
    private class LocalQueryRunnable<K, V, R> implements GridPlainRunnable {
        /** {@inheritDoc} */
        @Override public void run() {
            try {
                qry.query().validate();

                if (fields())
                    cctx.queries().runFieldsQuery(localQueryInfo());
                else
                    cctx.queries().runQuery(localQueryInfo());
            }
            catch (Throwable e) {
                onDone(e);
            }
        }

        /**
         * @return Query info.
         * @throws GridException In case of error.
         */
        @SuppressWarnings({"unchecked"})
        private GridCacheQueryInfo localQueryInfo() throws GridException {
            GridCacheQueryBean qry = query();

            GridPredicate<GridCacheEntry<Object, Object>> prjPred = qry.query().projectionFilter() == null ?
                F.<GridCacheEntry<Object, Object>>alwaysTrue() : qry.query().projectionFilter();

            GridMarshaller marsh = cctx.marshaller();

            GridReducer<Object, Object> rdc = qry.reducer() != null ?
                marsh.<GridReducer<Object, Object>>unmarshal(marsh.marshal(qry.reducer()), null) : null;

            GridClosure<Object, Object> trans = qry.transform() != null ?
                marsh.<GridClosure<Object, Object>>unmarshal(marsh.marshal(qry.transform()), null) : null;

            return new GridCacheQueryInfo(
                true,
                prjPred,
                trans,
                rdc,
                qry.query(),
                GridCacheLocalQueryFuture.this,
                ctx.localNodeId(),
                -1,
                qry.query().includeMetadata(),
                true,
                qry.arguments()
            );
        }
    }
}
