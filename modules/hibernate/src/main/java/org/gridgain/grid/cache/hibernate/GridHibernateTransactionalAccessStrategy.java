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
import org.hibernate.cache.spi.access.*;
import org.jetbrains.annotations.*;

/**
 * Implementation of {@link AccessType#TRANSACTIONAL} cache access strategy.
 * <p>
 * It is supposed that this strategy is used in JTA environment and Hibernate and
 * {@link GridCache} corresponding to the L2 cache region are configured to use the same transaction manager.
 * <p>
 * Configuration of L2 cache and per-entity cache access strategy can be set in the
 * Hibernate configuration file:
 * <pre name="code" class="xml">
 * &lt;hibernate-configuration&gt;
 *     &lt;!-- Enable L2 cache. --&gt;
 *     &lt;property name="cache.use_second_level_cache"&gt;true&lt;/property&gt;
 *
 *     &lt;!-- Use GridGain as L2 cache provider. --&gt;
 *     &lt;property name="cache.region.factory_class"&gt;org.gridgain.grid.cache.hibernate.GridHibernateRegionFactory&lt;/property&gt;
 *
 *     &lt;!-- Specify entity. --&gt;
 *     &lt;mapping class="com.example.Entity"/&gt;
 *
 *     &lt;!-- Enable L2 cache with transactional access strategy for entity. --&gt;
 *     &lt;class-cache class="com.example.Entity" usage="transactional"/&gt;
 * &lt;/hibernate-configuration&gt;
 * </pre>
 * Also cache access strategy can be set using annotations:
 * <pre name="code" class="java">
 * &#064;javax.persistence.Entity
 * &#064;javax.persistence.Cacheable
 * &#064;org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
 * public class Entity { ... }
 * </pre>
 */
public class GridHibernateTransactionalAccessStrategy extends GridHibernateAccessStrategyAdapter {
    /**
     * @param grid Grid.
     * @param cache Cache.
     */
    public GridHibernateTransactionalAccessStrategy(Grid grid, GridCache<Object, Object> cache) {
        super(grid, cache);
    }

    /** {@inheritDoc} */
    @Nullable @Override protected Object get(Object key) throws CacheException {
        try {
            return cache.get(key);
        }
        catch (GridException e) {
            throw new CacheException(e);
        }
    }

    /** {@inheritDoc} */
    @Override protected void putFromLoad(Object key, Object val) throws CacheException {
        try {
            cache.putx(key, val);
        }
        catch (GridException e) {
            throw new CacheException(e);
        }
    }

    /** {@inheritDoc} */
    @Override protected SoftLock lock(Object key) throws CacheException {
        return null;
    }

    /** {@inheritDoc} */
    @Override protected void unlock(Object key, SoftLock lock) throws CacheException {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override protected boolean update(Object key, Object val) throws CacheException {
        try {
            cache.putx(key, val);

            return true;
        }
        catch (GridException e) {
            throw new CacheException(e);
        }
    }

    /** {@inheritDoc} */
    @Override protected boolean afterUpdate(Object key, Object val, SoftLock lock) throws CacheException {
        return false;
    }

    /** {@inheritDoc} */
    @Override protected boolean insert(Object key, Object val) throws CacheException {
        try {
            cache.putx(key, val);

            return true;
        }
        catch (GridException e) {
            throw new CacheException(e);
        }
    }

    /** {@inheritDoc} */
    @Override protected boolean afterInsert(Object key, Object val) throws CacheException {
        return false;
    }

    /** {@inheritDoc} */
    @Override protected void remove(Object key) throws CacheException {
        try {
            cache.removex(key);
        }
        catch (GridException e) {
            throw new CacheException(e);
        }
    }
}
