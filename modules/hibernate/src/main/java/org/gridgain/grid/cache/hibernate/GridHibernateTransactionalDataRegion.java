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
import org.hibernate.cache.spi.access.*;

import static org.gridgain.grid.cache.GridCacheAtomicityMode.*;

/**
 * Implementation of {@link TransactionalDataRegion} (transactional means that
 * data in the region is updated in connection with database transaction).
 * This interface defines base contract for {@link EntityRegion}, {@link CollectionRegion}
 * and {@link NaturalIdRegion}.
 */
public class GridHibernateTransactionalDataRegion extends GridHibernateRegion implements TransactionalDataRegion {
    /** */
    private final CacheDataDescription dataDesc;

    /**
     * @param factory Region factory.
     * @param name Region name.
     * @param grid Grid.
     * @param cache Region cache.
     * @param dataDesc Region data description.
     */
    public GridHibernateTransactionalDataRegion(GridHibernateRegionFactory factory, String name,
        Grid grid, GridCache<Object, Object> cache, CacheDataDescription dataDesc) {
        super(factory, name, grid, cache);

        this.dataDesc = dataDesc;
    }

    /** {@inheritDoc} */
    @Override public boolean isTransactionAware() {
        return false; // This method is not used by Hibernate.
    }

    /** {@inheritDoc} */
    @Override public CacheDataDescription getCacheDataDescription() {
        return dataDesc;
    }

    /**
     * @param accessType Hibernate L2 cache access type.
     * @return Access strategy for given access type.
     */
    protected GridHibernateAccessStrategyAdapter createAccessStrategy(AccessType accessType) {
        switch (accessType) {
            case READ_ONLY:
                return new GridHibernateReadOnlyAccessStrategy(grid, cache);

            case NONSTRICT_READ_WRITE:
                return new GridHibernateNonStrictAccessStrategy(grid, cache, factory.threadLocalForCache(cache.name()));

            case READ_WRITE:
                if (cache.configuration().getAtomicityMode() != TRANSACTIONAL)
                    throw new CacheException("Hibernate READ-WRITE access strategy must have GridGain cache with " +
                        "'TRANSACTIONAL' atomicity mode: " + cache.name());

                return new GridHibernateReadWriteAccessStrategy(grid, cache, factory.threadLocalForCache(cache.name()));

            case TRANSACTIONAL:
                if (cache.configuration().getAtomicityMode() != TRANSACTIONAL)
                    throw new CacheException("Hibernate TRANSACTIONAL access strategy must have GridGain cache with " +
                        "'TRANSACTIONAL' atomicity mode: " + cache.name());

                if (cache.configuration().getTransactionManagerLookupClassName() == null)
                    throw new CacheException("Hibernate TRANSACTIONAL access strategy must have GridGain cache with " +
                        "TransactionManagerLookup configured: " + cache.name());

                return new GridHibernateTransactionalAccessStrategy(grid, cache);

            default:
                throw new IllegalArgumentException("Unknown Hibernate access type: " + accessType);
        }
    }
}
