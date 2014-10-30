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

package org.gridgain.grid.kernal.processors.cache;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.jetbrains.annotations.*;
import org.gridgain.grid.util.*;

import java.util.*;

/**
 * Value collection based on provided entries with all remove operations backed
 * by underlying cache.
 */
public class GridCacheValueCollection<K, V> extends GridSerializableCollection<V> {
    /** */
    private static final long serialVersionUID = 0L;

    /** Cache context. */
    private final GridCacheContext<K, V> ctx;

    /** Filter. */
    private final GridPredicate<GridCacheEntry<K, V>>[] filter;

    /** Base map. */
    private final Map<K, GridCacheEntry<K, V>> map;

    /**
     * @param ctx Cache context.
     * @param c Entry collection.
     * @param filter Filter.
     */
    public GridCacheValueCollection(GridCacheContext<K, V> ctx, Collection<? extends GridCacheEntry<K, V>> c,
        @Nullable GridPredicate<GridCacheEntry<K, V>>[] filter) {
        map = new HashMap<>(c.size(), 1.0f);

        assert ctx != null;

        this.ctx = ctx;
        this.filter = filter;

        for (GridCacheEntry<K, V> e : c) {
            if (e != null)
                map.put(e.getKey(), e);
        }
    }

    /** {@inheritDoc} */
    @Override public Iterator<V> iterator() {
        return new GridCacheIterator<K, V, V>(
            map.values(),
            F.<K, V>cacheEntry2Get(),
            ctx.vararg(F0.and(filter, F.<K, V>cacheHasPeekValue()))
        ) {
            {
                advance();
            }

            private V next;

            private void advance() {
                if (next != null)
                    return;

                boolean has;

                while (has = super.hasNext()) {
                    next = super.next();

                    if (next != null)
                        break;
                }

                if (!has)
                    next = null;
            }

            @SuppressWarnings( {"IteratorHasNextCallsIteratorNext"})
            @Override public boolean hasNext() {
                advance();

                return next != null;
            }

            @Override public V next() {
                advance();

                if (next == null)
                    throw new NoSuchElementException();

                V v = next;

                next = null;

                return v;
            }
        };
    }

    /** {@inheritDoc} */
    @Override public void clear() {
        ctx.cache().clearAll0(F.viewReadOnly(map.values(), F.<K>mapEntry2Key(), filter), CU.<K, V>empty());

        map.clear();
    }

    /** {@inheritDoc} */
    @Override public boolean remove(Object o) {
        A.notNull(o, "o");

        boolean rmv = false;

        for (Iterator<GridCacheEntry<K, V>> it = map.values().iterator(); it.hasNext();) {
            GridCacheEntry<K, V> e = it.next();

            if (F.isAll(e, filter) && F.eq(o, e.getValue())) {
                it.remove();

                try {
                    e.removex();
                }
                catch (GridException ex) {
                    throw new GridRuntimeException(ex);
                }

                rmv = true;
            }
        }

        return rmv;
    }

    /** {@inheritDoc} */
    @Override public int size() {
        return F.size(map.values(), filter);
    }

    /** {@inheritDoc} */
    @Override public boolean contains(Object o) {
        A.notNull(o, "o");

        for (GridCacheEntry<K, V> e : map.values())
            if (F.isAll(e, filter) && F.eq(e.getValue(), o))
                return true;

        return false;
    }
}
