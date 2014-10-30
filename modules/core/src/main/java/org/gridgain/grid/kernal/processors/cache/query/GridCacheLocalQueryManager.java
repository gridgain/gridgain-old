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
import org.gridgain.grid.cache.query.*;
import org.gridgain.grid.spi.indexing.*;
import org.jetbrains.annotations.*;

import java.util.*;

import static org.gridgain.grid.cache.GridCacheMode.*;

/**
 * Local query manager.
 */
public class GridCacheLocalQueryManager<K, V> extends GridCacheQueryManager<K, V> {
    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override protected boolean onPageReady(
        boolean loc,
        GridCacheQueryInfo qryInfo,
        Collection<?> data,
        boolean finished, Throwable e) {
        GridCacheQueryFutureAdapter fut = qryInfo.localQueryFuture();

        assert fut != null;

        if (e != null)
            fut.onPage(null, null, e, true);
        else
            fut.onPage(null, data, null, finished);

        return true;
    }

    /** {@inheritDoc} */
    @Override protected boolean onFieldsPageReady(boolean loc,
        GridCacheQueryInfo qryInfo,
        @Nullable List<GridIndexingFieldMetadata> metaData,
        @Nullable Collection<List<GridIndexingEntity<?>>> entities,
        @Nullable Collection<?> data,
        boolean finished,
        @Nullable Throwable e) {
        assert qryInfo != null;

        GridCacheLocalFieldsQueryFuture fut = (GridCacheLocalFieldsQueryFuture)qryInfo.localQueryFuture();

        assert fut != null;

        if (e != null)
            fut.onPage(null, null, null, e, true);
        else
            fut.onPage(null, metaData, data, null, finished);

        return true;
    }

    /** {@inheritDoc} */
    @Override public void start0() throws GridException {
        super.start0();

        assert cctx.config().getCacheMode() == LOCAL;
    }

    /** {@inheritDoc} */
    @Override public GridCacheQueryFuture<?> queryLocal(GridCacheQueryBean qry) {
        assert cctx.config().getCacheMode() == LOCAL;

        if (log.isDebugEnabled())
            log.debug("Executing query on local node: " + qry);

        GridCacheLocalQueryFuture<K, V, ?> fut = new GridCacheLocalQueryFuture<>(cctx, qry);

        try {
            qry.query().validate();

            fut.execute();
        }
        catch (GridException e) {
            fut.onDone(e);
        }

        return fut;
    }

    /** {@inheritDoc} */
    @Override public GridCacheQueryFuture<?> queryDistributed(GridCacheQueryBean qry, Collection<GridNode> nodes) {
        assert cctx.config().getCacheMode() == LOCAL;

        throw new GridRuntimeException("Distributed queries are not available for local cache " +
            "(use 'GridCacheQuery.execute(grid.forLocal())' instead) [cacheName=" + cctx.name() + ']');
    }

    /** {@inheritDoc} */
    @Override public void loadPage(long id, GridCacheQueryAdapter<?> qry, Collection<GridNode> nodes, boolean all) {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override public GridCacheQueryFuture<?> queryFieldsLocal(GridCacheQueryBean qry) {
        assert cctx.config().getCacheMode() == LOCAL;

        if (log.isDebugEnabled())
            log.debug("Executing query on local node: " + qry);

        GridCacheLocalFieldsQueryFuture fut = new GridCacheLocalFieldsQueryFuture(cctx, qry);

        try {
            qry.query().validate();

            fut.execute();
        }
        catch (GridException e) {
            fut.onDone(e);
        }

        return fut;
    }

    /** {@inheritDoc} */
    @Override public GridCacheQueryFuture<?> queryFieldsDistributed(GridCacheQueryBean qry,
        Collection<GridNode> nodes) {
        assert cctx.config().getCacheMode() == LOCAL;

        throw new GridRuntimeException("Distributed queries are not available for local cache " +
            "(use 'GridCacheQuery.execute(grid.forLocal())' instead) [cacheName=" + cctx.name() + ']');
    }
}
