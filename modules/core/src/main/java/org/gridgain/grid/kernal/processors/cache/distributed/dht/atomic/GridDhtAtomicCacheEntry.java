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

package org.gridgain.grid.kernal.processors.cache.distributed.dht.atomic;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.kernal.processors.cache.*;
import org.gridgain.grid.kernal.processors.cache.distributed.*;
import org.gridgain.grid.kernal.processors.cache.distributed.dht.*;
import org.gridgain.grid.util.typedef.internal.*;

/**
 * DHT atomic cache entry.
 */
public class GridDhtAtomicCacheEntry<K, V> extends GridDhtCacheEntry<K, V> {
    /** */
    private static final long serialVersionUID = 0L;

    /**
     * @param ctx Cache context.
     * @param topVer Topology version at the time of creation (if negative, then latest topology is assumed).
     * @param key Cache key.
     * @param hash Key hash value.
     * @param val Entry value.
     * @param next Next entry in the linked list.
     * @param ttl Time to live.
     * @param hdrId Header id.
     */
    public GridDhtAtomicCacheEntry(GridCacheContext<K, V> ctx, long topVer, K key, int hash, V val,
        GridCacheMapEntry<K, V> next, long ttl, int hdrId) {
        super(ctx, topVer, key, hash, val, next, ttl, hdrId);
    }

    /** {@inheritDoc} */
    @Override public GridCacheEntry<K, V> wrap(boolean prjAware) {
        GridCacheProjectionImpl<K, V> prjPerCall = cctx.projectionPerCall();

        if (prjPerCall != null && prjAware)
            return new GridPartitionedCacheEntryImpl<>(prjPerCall, cctx, key, this);

        return new GridPartitionedCacheEntryImpl<>(null, cctx, key, this);
    }

    /** {@inheritDoc} */
    @Override protected String cacheName() {
        return CU.isNearEnabled(cctx) ? super.cacheName() : cctx.dht().name();
    }

    /** {@inheritDoc} */
    @Override public GridCacheEntry<K, V> wrapFilterLocked() throws GridException {
        assert Thread.holdsLock(this);

        return new GridCacheFilterEvaluationEntry<>(key, rawGetOrUnmarshal(true), this);
    }

    /** {@inheritDoc} */
    @Override public synchronized String toString() {
        return S.toString(GridDhtAtomicCacheEntry.class, this, super.toString());
    }
}
