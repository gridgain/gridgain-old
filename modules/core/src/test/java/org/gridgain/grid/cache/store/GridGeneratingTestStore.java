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
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.resources.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * Test store that generates objects on demand.
 */
public class GridGeneratingTestStore implements GridCacheStore<String, String> {
    /** Number of entries to be generated. */
    private static final int DFLT_GEN_CNT = 100;

    /** */
    @GridCacheNameResource
    private String cacheName;

    /** {@inheritDoc} */
    @Override public String load(@Nullable GridCacheTx tx, String key)
        throws GridException {
        return null;
    }

    /** {@inheritDoc} */
    @Override public void loadCache(GridBiInClosure<String, String> clo,
        @Nullable Object... args) throws GridException {
        if (args.length > 0) {
            try {
                int cnt = ((Number)args[0]).intValue();
                int postfix = ((Number)args[1]).intValue();

                for (int i = 0; i < cnt; i++)
                    clo.apply("key" + i, "val." + cacheName + "." + postfix);
            }
            catch (Exception e) {
                X.println("Unexpected exception in loadAll: " + e);

                throw new GridException(e);
            }
        }
        else {
            for (int i = 0; i < DFLT_GEN_CNT; i++)
                clo.apply("key" + i, "val." + cacheName + "." + i);
        }
    }

    /** {@inheritDoc} */
    @Override public void loadAll(@Nullable GridCacheTx tx,
        @Nullable Collection<? extends String> keys, GridBiInClosure<String, String> c) throws GridException {
        for (String key : keys)
            c.apply(key, "val" + key);
    }

    /** {@inheritDoc} */
    @Override public void put(@Nullable GridCacheTx tx, String key, @Nullable String val)
        throws GridException {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override public void putAll(@Nullable GridCacheTx tx,
        @Nullable Map<? extends String, ? extends String> map) throws GridException {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override public void remove(@Nullable GridCacheTx tx, String key)
        throws GridException {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override public void removeAll(@Nullable GridCacheTx tx,
        @Nullable Collection<? extends String> keys) throws GridException {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override public void txEnd(GridCacheTx tx, boolean commit) throws GridException {
        // No-op.
    }
}
