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
import org.gridgain.grid.kernal.processors.cache.*;
import org.gridgain.grid.lang.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * Per-projection queries object returned to user.
 */
public class GridCacheQueriesProxy<K, V> implements GridCacheQueriesEx<K, V> {
    /** */
    private GridCacheGateway<K, V> gate;

    /** */
    private GridCacheProjectionImpl<K, V> prj;

    /** */
    private GridCacheQueriesEx<K, V> delegate;

    /**
     * Create cache queries implementation.
     *
     * @param ctx Сontext.
     * @param prj Optional cache projection.
     * @param delegate Delegate object.
     */
    public GridCacheQueriesProxy(GridCacheContext<K, V> ctx, @Nullable GridCacheProjectionImpl<K, V> prj,
        GridCacheQueriesEx<K, V> delegate) {
        assert ctx != null;
        assert delegate != null;

        gate = ctx.gate();

        this.prj = prj;
        this.delegate = delegate;
    }

    /** {@inheritDoc} */
    @Override public GridCacheQuery<Map.Entry<K, V>> createSqlQuery(Class<?> cls, String clause) {
        GridCacheProjectionImpl<K, V> prev = gate.enter(prj);

        try {
            return delegate.createSqlQuery(cls, clause);
        }
        finally {
            gate.leave(prev);
        }
    }

    /** {@inheritDoc} */
    @Override public GridCacheQuery<List<?>> createSqlFieldsQuery(String qry) {
        GridCacheProjectionImpl<K, V> prev = gate.enter(prj);

        try {
            return delegate.createSqlFieldsQuery(qry);
        }
        finally {
            gate.leave(prev);
        }
    }

    /** {@inheritDoc} */
    @Override public GridCacheQuery<Map.Entry<K, V>> createFullTextQuery(Class<?> cls, String search) {
        GridCacheProjectionImpl<K, V> prev = gate.enter(prj);

        try {
            return delegate.createFullTextQuery(cls, search);
        }
        finally {
            gate.leave(prev);
        }
    }

    /** {@inheritDoc} */
    @Override public GridCacheQuery<Map.Entry<K, V>> createScanQuery(@Nullable GridBiPredicate<K, V> filter) {
        GridCacheProjectionImpl<K, V> prev = gate.enter(prj);

        try {
            return delegate.createScanQuery(filter);
        }
        finally {
            gate.leave(prev);
        }
    }

    /** {@inheritDoc} */
    @Override public GridCacheContinuousQuery<K, V> createContinuousQuery() {
        GridCacheProjectionImpl<K, V> prev = gate.enter(prj);

        try {
            return delegate.createContinuousQuery();
        }
        finally {
            gate.leave(prev);
        }
    }

    /** {@inheritDoc} */
    @Override public GridFuture<?> rebuildIndexes(Class<?> cls) {
        GridCacheProjectionImpl<K, V> prev = gate.enter(prj);

        try {
            return delegate.rebuildIndexes(cls);
        }
        finally {
            gate.leave(prev);
        }
    }

    /** {@inheritDoc} */
    @Override public GridFuture<?> rebuildAllIndexes() {
        GridCacheProjectionImpl<K, V> prev = gate.enter(prj);

        try {
            return delegate.rebuildAllIndexes();
        }
        finally {
            gate.leave(prev);
        }
    }

    /** {@inheritDoc} */
    @Override public GridCacheQueryMetrics metrics() {
        GridCacheProjectionImpl<K, V> prev = gate.enter(prj);

        try {
            return delegate.metrics();
        }
        finally {
            gate.leave(prev);
        }
    }

    /** {@inheritDoc} */
    @Override public void resetMetrics() {
        GridCacheProjectionImpl<K, V> prev = gate.enter(prj);

        try {
            delegate.resetMetrics();
        }
        finally {
            gate.leave(prev);
        }
    }

    /** {@inheritDoc} */
    @Override public Collection<GridCacheSqlMetadata> sqlMetadata() throws GridException {
        GridCacheProjectionImpl<K, V> prev = gate.enter(prj);

        try {
            return delegate.sqlMetadata();
        }
        finally {
            gate.leave(prev);
        }
    }

    /** {@inheritDoc} */
    @Override public GridCacheQuery<List<?>> createSqlFieldsQuery(String qry, boolean incMeta) {
        GridCacheProjectionImpl<K, V> prev = gate.enter(prj);

        try {
            return delegate.createSqlFieldsQuery(qry, incMeta);
        }
        finally {
            gate.leave(prev);
        }
    }
}
