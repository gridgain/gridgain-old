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

import static org.gridgain.grid.cache.GridCacheWriteSynchronizationMode.*;

/**
 * Tests value read inside transaction.
 */
public abstract class GridCacheAbstractTxReadTest extends GridCacheAbstractSelfTest {
    /** {@inheritDoc} */
    @SuppressWarnings("NullableProblems")
    @Override protected GridCacheConfiguration cacheConfiguration(String gridName) throws Exception {
        GridCacheConfiguration cfg = super.cacheConfiguration(gridName);

        cfg.setWriteSynchronizationMode(FULL_SYNC);

        cfg.setTxSerializableEnabled(true);

        cfg.setStore(null);

        return cfg;
    }

    /**
     * @throws GridException If failed
     */
    public void testTxReadOptimisticReadCommitted() throws GridException {
        checkTransactionalRead(GridCacheTxConcurrency.OPTIMISTIC, GridCacheTxIsolation.READ_COMMITTED);
    }

    /**
     * @throws GridException If failed
     */
    public void testTxReadOptimisticRepeatableRead() throws GridException {
        checkTransactionalRead(GridCacheTxConcurrency.OPTIMISTIC, GridCacheTxIsolation.REPEATABLE_READ);
    }

    /**
     * @throws GridException If failed
     */
    public void testTxReadOptimisticSerializable() throws GridException {
        checkTransactionalRead(GridCacheTxConcurrency.OPTIMISTIC, GridCacheTxIsolation.SERIALIZABLE);
    }

    /**
     * @throws GridException If failed
     */
    public void testTxReadPessimisticReadCommitted() throws GridException {
        checkTransactionalRead(GridCacheTxConcurrency.PESSIMISTIC, GridCacheTxIsolation.READ_COMMITTED);
    }

    /**
     * @throws GridException If failed
     */
    public void testTxReadPessimisticRepeatableRead() throws GridException {
        checkTransactionalRead(GridCacheTxConcurrency.PESSIMISTIC, GridCacheTxIsolation.REPEATABLE_READ);
    }

    /**
     * @throws GridException If failed
     */
    public void testTxReadPessimisticSerializable() throws GridException {
        checkTransactionalRead(GridCacheTxConcurrency.PESSIMISTIC, GridCacheTxIsolation.SERIALIZABLE);
    }

    /**
     * Tests sequential value write and read inside transaction.
     * @param concurrency Transaction concurrency.
     * @param isolation Transaction isolation.
     * @throws GridException If failed
     */
    protected void checkTransactionalRead(GridCacheTxConcurrency concurrency, GridCacheTxIsolation isolation)
        throws GridException {
        GridCache<String, Integer> cache = cache(0);

        cache.clearAll();

        GridCacheTx tx = cache.txStart(concurrency, isolation);

        try {
            cache.put("key", 1);

            assertEquals("Invalid value after put", 1, cache.get("key").intValue());

            tx.commit();
        }
        finally {
            tx.close();
        }

        assertEquals("Invalid cache size after put", 1, cache.size());

        try {
            tx = cache.txStart(concurrency, isolation);

            assertEquals("Invalid value inside transactional read", Integer.valueOf(1), cache.get("key"));

            tx.commit();
        }
        finally {
            tx.close();
        }
    }

    /** {@inheritDoc} */
    @Override protected int gridCount() {
        return 1;
    }
}
