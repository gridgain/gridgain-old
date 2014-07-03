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

import java.util.*;

/**
 * Implementation of {@link Region}. This interface defines base contract for all L2 cache regions.
 */
public class GridHibernateRegion implements Region {
    /** */
    protected final GridHibernateRegionFactory factory;

    /** */
    private final String name;

    /** Cache instance. */
    protected final GridCache<Object, Object> cache;

    /** Grid instance. */
    protected Grid grid;

    /**
     * @param factory Region factory.
     * @param name Region name.
     * @param grid Grid.
     * @param cache Region cache.
     */
    public GridHibernateRegion(GridHibernateRegionFactory factory, String name, Grid grid,
        GridCache<Object, Object> cache) {
        this.factory = factory;
        this.name = name;
        this.grid = grid;
        this.cache = cache;
    }

    /** {@inheritDoc} */
    @Override public String getName() {
        return name;
    }

    /** {@inheritDoc} */
    @Override public void destroy() throws CacheException {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override public boolean contains(Object key) {
        return cache.containsKey(key);
    }

    /** {@inheritDoc} */
    @Override public long getSizeInMemory() {
        return -1;
    }

    /** {@inheritDoc} */
    @Override public long getElementCountInMemory() {
        return cache.size();
    }

    /** {@inheritDoc} */
    @Override public long getElementCountOnDisk() {
        return -1;
    }

    /** {@inheritDoc} */
    @Override public Map toMap() {
        return cache.toMap();
    }

    /** {@inheritDoc} */
    @Override public long nextTimestamp() {
        return System.currentTimeMillis();
    }

    /** {@inheritDoc} */
    @Override public int getTimeout() {
        return 0;
    }
}
