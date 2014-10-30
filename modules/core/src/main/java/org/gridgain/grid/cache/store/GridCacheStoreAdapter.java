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

package org.gridgain.grid.cache.store;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.lang.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * Cache storage convenience adapter. It provides default implementation for bulk operations, such
 * as {@link #loadAll(GridCacheTx, Collection, GridBiInClosure)},
 * {@link #putAll(GridCacheTx, Map)}, abd {@link #removeAll(GridCacheTx, Collection)}
 * by sequentially calling corresponding {@link #load(GridCacheTx, Object)},
 * {@link #put(GridCacheTx, Object, Object)}, and {@link #remove(GridCacheTx, Object)}
 * operations. Use this adapter whenever such behaviour is acceptable. However in many cases
 * it maybe more preferable to take advantage of database batch update functionality, and therefore
 * default adapter implementation may not be the best option.
 * <p>
 * Note that method {@link #loadCache(GridBiInClosure, Object...)} has empty
 * implementation because it is essentially up to the user to invoke it with
 * specific arguments.
 */
public abstract class GridCacheStoreAdapter<K, V> implements GridCacheStore<K, V> {
    /**
     * Default empty implementation. This method needs to be overridden only if
     * {@link GridCache#loadCache(GridBiPredicate, long, Object...)} method
     * is explicitly called.
     *
     * @param clo {@inheritDoc}
     * @param args {@inheritDoc}
     * @throws GridException {@inheritDoc}
     */
    @Override public void loadCache(GridBiInClosure<K, V> clo, Object... args)
        throws GridException {
        /* No-op. */
    }

    /** {@inheritDoc} */
    @Override public void loadAll(@Nullable GridCacheTx tx, Collection<? extends K> keys,
        GridBiInClosure<K, V> c) throws GridException {
        assert keys != null;

        for (K key : keys)
            c.apply(key, load(tx, key));
    }

    /** {@inheritDoc} */
    @Override public void putAll(GridCacheTx tx, Map<? extends K, ? extends V> map)
        throws GridException {
        assert map != null;

        for (Map.Entry<? extends K, ? extends V> e : map.entrySet())
            put(tx, e.getKey(), e.getValue());
    }

    /** {@inheritDoc} */
    @Override public void removeAll(GridCacheTx tx, Collection<? extends K> keys)
        throws GridException {
        assert keys != null;

        for (K key : keys)
            remove(tx, key);
    }

    /**
     * Default empty implementation for ending transactions. Note that if explicit cache
     * transactions are not used, then transactions do not have to be explicitly ended -
     * for all other cases this method should be overridden with custom commit/rollback logic.
     *
     * @param tx {@inheritDoc}
     * @param commit {@inheritDoc}
     * @throws GridException {@inheritDoc}
     */
    @Override public void txEnd(GridCacheTx tx, boolean commit) throws GridException {
        // No-op.
    }
}
