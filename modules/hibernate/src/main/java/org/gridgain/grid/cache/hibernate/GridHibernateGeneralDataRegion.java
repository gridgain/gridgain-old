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

package org.gridgain.grid.cache.hibernate;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.hibernate.cache.*;
import org.hibernate.cache.spi.*;
import org.jetbrains.annotations.*;

/**
 * Implementation of {@link GeneralDataRegion}. This interface defines common contract for {@link QueryResultsRegion}
 * and {@link TimestampsRegion}.
 */
public class GridHibernateGeneralDataRegion extends GridHibernateRegion implements GeneralDataRegion {
    /**
     * @param factory Region factory.
     * @param name Region name.
     * @param grid Grid.
     * @param cache Region cache.
     */
    public GridHibernateGeneralDataRegion(GridHibernateRegionFactory factory, String name,
        Grid grid, GridCache<Object, Object> cache) {
        super(factory, name, grid, cache);
    }

    /** {@inheritDoc} */
    @Nullable @Override public Object get(Object key) throws CacheException {
        try {
            return cache.get(key);
        } catch (GridException e) {
            throw new CacheException(e);
        }
    }

    /** {@inheritDoc} */
    @Override public void put(Object key, Object val) throws CacheException {
        try {
            cache.putx(key, val);
        } catch (GridException e) {
            throw new CacheException(e);
        }
    }

    /** {@inheritDoc} */
    @Override public void evict(Object key) throws CacheException {
        GridHibernateAccessStrategyAdapter.evict(grid, cache, key);
    }

    /** {@inheritDoc} */
    @Override public void evictAll() throws CacheException {
        GridHibernateAccessStrategyAdapter.evictAll(cache);
    }
}
