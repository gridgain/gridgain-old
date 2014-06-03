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

import java.util.*;

/**
 * Entry set backed by cache itself.
 */
public class GridCacheEntrySet<K, V> extends AbstractSet<GridCacheEntry<K, V>> {
    /** Cache context. */
    private final GridCacheContext<K, V> ctx;

    /** Filter. */
    private final GridPredicate<GridCacheEntry<K, V>>[] filter;

    /** Base set. */
    private final Set<GridCacheEntry<K, V>> set;

    /**
     * @param ctx Cache context.
     * @param c Entry collection.
     * @param filter Filter.
     */
    public GridCacheEntrySet(GridCacheContext<K, V> ctx, Collection<? extends GridCacheEntry<K, V>> c,
        @Nullable GridPredicate<GridCacheEntry<K, V>>... filter) {
        set = new HashSet<>(c.size(), 1.0f);

        assert ctx != null;

        this.ctx = ctx;
        this.filter = filter;

        for (GridCacheEntry<K, V> e : c) {
            if (e != null)
                set.add(e);
        }
    }

    /** {@inheritDoc} */
    @Override public Iterator<GridCacheEntry<K, V>> iterator() {
        return new GridCacheIterator<>(set, F.<GridCacheEntry<K, V>>identity(), filter);
    }

    /** {@inheritDoc} */
    @Override public void clear() {
        ctx.cache().clearAll0(F.viewReadOnly(set, F.<K>mapEntry2Key(), filter), CU.<K, V>empty());

        set.clear();
    }

    /** {@inheritDoc} */
    @SuppressWarnings({"unchecked"})
    @Override public boolean remove(Object o) {
        if (!(o instanceof GridCacheEntryImpl)) {
            return false;
        }

        GridCacheEntry<K, V> e = (GridCacheEntry<K,V>)o;

        if (F.isAll(e, filter) && set.remove(e)) {
            try {
                e.removex();
            }
            catch (GridException ex) {
                throw new GridRuntimeException(ex);
            }

            return true;
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override public int size() {
        return F.size(set, filter);
    }

    /** {@inheritDoc} */
    @SuppressWarnings({"unchecked"})
    @Override public boolean contains(Object o) {
        if (!(o instanceof GridCacheEntryImpl)) {
            return false;
        }

        GridCacheEntry<K,V> e = (GridCacheEntry<K, V>)o;

        return F.isAll(e, filter) && set.contains(e);
    }
}
