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
import org.gridgain.grid.cache.query.*;
import org.gridgain.grid.kernal.processors.cache.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.util.*;

import static org.gridgain.grid.kernal.processors.cache.query.GridCacheQueryType.*;

/**
 * {@link GridCacheQueries} implementation.
 */
public class GridCacheQueriesImpl<K, V> implements GridCacheQueriesEx<K, V>, Externalizable {
    /** */
    private static final long serialVersionUID = 0L;

    /** */
    private GridCacheContext<K, V> ctx;

    /** */
    private GridCacheProjectionImpl<K, V> prj;

    /**
     * Required by {@link Externalizable}.
     */
    public GridCacheQueriesImpl() {
        // No-op.
    }

    /**
     * @param ctx Context.
     * @param prj Projection.
     */
    public GridCacheQueriesImpl(GridCacheContext<K, V> ctx, @Nullable GridCacheProjectionImpl<K, V> prj) {
        assert ctx != null;

        this.ctx = ctx;
        this.prj = prj;
    }

    /** {@inheritDoc} */
    @Override public GridCacheQuery<Map.Entry<K, V>> createSqlQuery(Class<?> cls, String clause) {
        A.notNull(cls, "cls");
        A.notNull(clause, "clause");

        return new GridCacheQueryAdapter<>(ctx,
            SQL,
            filter(),
            ctx.kernalContext().indexing().typeName(U.box(cls)),
            clause,
            null,
            false,
            prj != null && prj.isKeepPortable());
    }

    /** {@inheritDoc} */
    @Override public GridCacheQuery<Map.Entry<K, V>> createSqlQuery(String clsName, String clause) {
        A.notNull("clsName", clsName);
        A.notNull("clause", clause);

        return new GridCacheQueryAdapter<>(ctx,
            SQL,
            filter(),
            clsName,
            clause,
            null,
            false,
            prj != null && prj.isKeepPortable());
    }

    /** {@inheritDoc} */
    @Override public GridCacheQuery<List<?>> createSqlFieldsQuery(String qry) {
        A.notNull(qry, "qry");

        return new GridCacheQueryAdapter<>(ctx,
            SQL_FIELDS,
            filter(),
            null,
            qry,
            null,
            false,
            prj != null && prj.isKeepPortable());
    }

    /** {@inheritDoc} */
    @Override public GridCacheQuery<Map.Entry<K, V>> createFullTextQuery(Class<?> cls, String search) {
        A.notNull(cls, "cls");
        A.notNull(search, "search");

        return new GridCacheQueryAdapter<>(ctx,
            TEXT,
            filter(),
            ctx.kernalContext().indexing().typeName(U.box(cls)),
            search,
            null,
            false,
            prj != null && prj.isKeepPortable());
    }

    /** {@inheritDoc} */
    @Override public GridCacheQuery<Map.Entry<K, V>> createFullTextQuery(String clsName, String search) {
        A.notNull("clsName", clsName);
        A.notNull("search", search);

        return new GridCacheQueryAdapter<>(ctx,
            TEXT,
            filter(),
            clsName,
            search,
            null,
            false,
            prj != null && prj.isKeepPortable());
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override public GridCacheQuery<Map.Entry<K, V>> createScanQuery(@Nullable GridBiPredicate<K, V> filter) {
        return new GridCacheQueryAdapter<>(ctx,
            SCAN,
            filter(),
            null,
            null,
            (GridBiPredicate<Object, Object>)filter,
            false,
            prj != null && prj.isKeepPortable());
    }

    /** {@inheritDoc} */
    @Override public GridCacheContinuousQuery<K, V> createContinuousQuery() {
        return ctx.continuousQueries().createQuery(prj == null ? null : prj.predicate());
    }

    /** {@inheritDoc} */
    @Override public GridFuture<?> rebuildIndexes(Class<?> cls) {
        A.notNull(cls, "cls");

        return ctx.queries().rebuildIndexes(cls);
    }

    /** {@inheritDoc} */
    @Override public GridFuture<?> rebuildIndexes(String typeName) {
        A.notNull("typeName", typeName);

        return ctx.queries().rebuildIndexes(typeName);
    }

    /** {@inheritDoc} */
    @Override public GridFuture<?> rebuildAllIndexes() {
        return ctx.queries().rebuildAllIndexes();
    }

    /** {@inheritDoc} */
    @Override public GridCacheQueryMetrics metrics() {
        return ctx.queries().metrics();
    }

    /** {@inheritDoc} */
    @Override public void resetMetrics() {
        ctx.queries().resetMetrics();
    }

    /** {@inheritDoc} */
    @Override public Collection<GridCacheSqlMetadata> sqlMetadata() throws GridException {
        return ctx.queries().sqlMetadata();
    }

    /** {@inheritDoc} */
    @Override public GridCacheQuery<List<?>> createSqlFieldsQuery(String qry, boolean incMeta) {
        assert qry != null;

        return new GridCacheQueryAdapter<>(ctx,
            SQL_FIELDS,
            filter(),
            null,
            qry,
            null,
            incMeta,
            prj != null && prj.isKeepPortable());
    }

    /**
     * @return Optional projection filter.
     */
    @SuppressWarnings("unchecked")
    @Nullable private GridPredicate<GridCacheEntry<Object, Object>> filter() {
        return prj == null ? null : ((GridCacheProjectionImpl<Object, Object>)prj).predicate();
    }

    /** {@inheritDoc} */
    @Override public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(prj);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        prj = (GridCacheProjectionImpl<K, V>)in.readObject();
    }

    /**
     * Reconstructs object on unmarshalling.
     *
     * @return Reconstructed object.
     * @throws ObjectStreamException Thrown in case of unmarshalling error.
     */
    private Object readResolve() throws ObjectStreamException {
        return prj.queries();
    }
}
