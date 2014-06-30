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

package org.gridgain.client;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.cache.store.*;
import org.gridgain.grid.lang.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * Simple HashMap based cache store emulation.
 */
public class GridHashMapStore extends GridCacheStoreAdapter {
    /** Map for cache store. */
    private final Map<Object, Object> map = new HashMap<>();

    /** {@inheritDoc} */
    @Override public void loadCache(GridBiInClosure c, Object... args)
        throws GridException {
        for (Map.Entry e : map.entrySet())
            c.apply(e.getKey(), e.getValue());
    }

    /** {@inheritDoc} */
    @Override public Object load(@Nullable GridCacheTx tx, Object key)
        throws GridException {
        return map.get(key);
    }

    /** {@inheritDoc} */
    @Override public void put(@Nullable GridCacheTx tx, Object key,
        @Nullable Object val) throws GridException {
        map.put(key, val);
    }

    /** {@inheritDoc} */
    @Override public void remove(@Nullable GridCacheTx tx, Object key)
        throws GridException {
        map.remove(key);
    }
}
