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
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.lang.*;

import java.util.*;

/**
 * Cache-backed iterator.
 */
public class GridCacheIterator<K, V, T> implements GridSerializableIterator<T> {
    /** */
    private static final long serialVersionUID = 0L;

    /** Base iterator. */
    private final Iterator<? extends GridCacheEntry<K, V>> it;

    /** Transformer. */
    private final GridClosure<GridCacheEntry<K, V>, T> trans;

    /** Current element. */
    private GridCacheEntry<K, V> cur;

    /**
     * @param c Cache entry collection.
     * @param trans Transformer.
     * @param filter Filter.
     */
    public GridCacheIterator(Iterable<? extends GridCacheEntry<K, V>> c,
        GridClosure<GridCacheEntry<K, V>, T> trans,
        GridPredicate<GridCacheEntry<K, V>>[] filter) {
        it = F.iterator0(c, false, filter);

        this.trans = trans;
    }

    /** {@inheritDoc} */
    @Override public boolean hasNext() {
        if (!it.hasNext()) {
            cur = null;

            return false;
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override public T next() {
        return trans.apply(cur = it.next());
    }

    /** {@inheritDoc} */
    @Override public void remove() {
        it.remove();

        try {
            // Back remove operation by actual cache.
            cur.removex();
        }
        catch (GridException e) {
            throw new GridClosureException(e);
        }
    }
}
