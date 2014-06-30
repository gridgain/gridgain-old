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

package org.gridgain.grid.spi.indexing.h2.opt;

import org.gridgain.grid.spi.indexing.*;
import org.gridgain.grid.util.lang.*;
import org.gridgain.grid.util.offheap.unsafe.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.h2.engine.*;
import org.h2.index.*;
import org.h2.message.*;
import org.h2.result.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * Index base.
 */
public abstract class GridH2IndexBase extends BaseIndex {
    /** */
    protected static final ThreadLocal<GridIndexingQueryFilter<?, ?>[]> filters =
        new ThreadLocal<>();

    /** */
    protected final int keyCol;

    /** */
    protected final int valCol;

    /**
     * @param keyCol Key column.
     * @param valCol Value column.
     */
    public GridH2IndexBase(int keyCol, int valCol) {
        this.keyCol = keyCol;
        this.valCol = valCol;
    }

    /**
     * Sets key filters for current thread.
     *
     * @param fs Filters.
     */
    public static void setFiltersForThread(GridIndexingQueryFilter<?, ?>[] fs) {
        filters.set(fs);
    }

    /**
     * If the index supports rebuilding it has to creates its own copy.
     *
     * @param memory Memory.
     * @return Rebuilt copy.
     * @throws InterruptedException If interrupted.
     */
    public GridH2IndexBase rebuild(GridUnsafeMemory memory) throws InterruptedException {
        return this;
    }

    /**
     * Put row if absent.
     *
     * @param row Row.
     * @param ifAbsent Put only if such a row does not exist.
     * @return Existing row or null.
     */
    public abstract GridH2Row put(GridH2Row row, boolean ifAbsent);

    /**
     * Remove row from index.
     *
     * @param row Row.
     * @return Removed row.
     */
    public abstract GridH2Row remove(SearchRow row);

    /**
     * Takes or sets existing snapshot to be used in current thread.
     *
     * @param s Optional existing snapshot to use.
     * @return Snapshot.
     */
    public Object takeSnapshot(@Nullable Object s) {
        return s;
    }

    /**
     * Releases snapshot for current thread.
     */
    public void releaseSnapshot() {
        // No-op.
    }

    /**
     * Filters rows from expired ones and using predicate.
     *
     * @param iter Iterator over rows.
     * @return Filtered iterator.
     */
    protected Iterator<GridH2Row> filter(Iterator<GridH2Row> iter) {
        return new FilteringIterator(iter, U.currentTimeMillis());
    }

    /** {@inheritDoc} */
    @Override public long getDiskSpaceUsed() {
        return 0;
    }

    /** {@inheritDoc} */
    @Override public void checkRename() {
        throw DbException.getUnsupportedException("rename");
    }

    /** {@inheritDoc} */
    @Override public void add(Session ses, Row row) {
        throw DbException.getUnsupportedException("add");
    }

    /** {@inheritDoc} */
    @Override public void remove(Session ses, Row row) {
        throw DbException.getUnsupportedException("remove row");
    }

    /** {@inheritDoc} */
    @Override public void remove(Session ses) {
        throw DbException.getUnsupportedException("remove index");
    }

    /** {@inheritDoc} */
    @Override public void truncate(Session ses) {
        throw DbException.getUnsupportedException("truncate");
    }

    /** {@inheritDoc} */
    @Override public boolean needRebuild() {
        return false;
    }

    /**
     * Iterator which filters by expiration time and predicate.
     */
    protected class FilteringIterator extends GridFilteredIterator<GridH2Row> {
        /** */
        private final GridIndexingQueryFilter<?, ?>[] fs = filters.get();

        /** */
        private final long time;

        /**
         * @param iter Iterator.
         * @param time Time for expired rows filtering.
         */
        protected FilteringIterator(Iterator<GridH2Row> iter, long time) {
            super(iter);

            this.time = time;
        }

        /**
         * @param row Row.
         * @return If this row was accepted.
         */
        @SuppressWarnings("unchecked")
        @Override protected boolean accept(GridH2Row row) {
            if (row instanceof GridH2AbstractKeyValueRow) {
                if (((GridH2AbstractKeyValueRow) row).expirationTime() <= time)
                    return false;
            }

            if (F.isEmpty(fs))
                return true;

            String spaceName = ((GridH2Table)getTable()).spaceName();

            Object key = row.getValue(keyCol).getObject();
            Object val = row.getValue(valCol).getObject();

            assert key != null;
            assert val != null;

            for (GridIndexingQueryFilter f : fs) {
                if (f != null && !f.apply(spaceName, key, val))
                    return false;
            }

            return true;
        }
    }
}
