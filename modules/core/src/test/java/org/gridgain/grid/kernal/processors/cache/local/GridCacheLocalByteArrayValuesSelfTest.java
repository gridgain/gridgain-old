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

package org.gridgain.grid.kernal.processors.cache.local;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.kernal.processors.cache.*;
import org.gridgain.grid.spi.swapspace.file.*;
import org.gridgain.grid.util.typedef.*;
import org.jetbrains.annotations.*;

import java.util.*;

import static org.gridgain.grid.cache.GridCacheAtomicityMode.*;
import static org.gridgain.grid.cache.GridCacheMemoryMode.*;
import static org.gridgain.grid.cache.GridCacheMode.*;
import static org.gridgain.grid.cache.GridCacheWriteSynchronizationMode.*;
import static org.gridgain.grid.cache.GridCacheTxConcurrency.*;
import static org.gridgain.grid.cache.GridCacheTxIsolation.*;

/**
 * Byte values test for LOCAL cache.
 */
public class GridCacheLocalByteArrayValuesSelfTest extends GridCacheAbstractByteArrayValuesSelfTest {
    /** Grid. */
    private static Grid grid;

    /** Regular cache. */
    private static GridCache<Integer, Object> cache;

    /** Offheap cache. */
    private static GridCache<Integer, Object> cacheOffheap;

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration c = super.getConfiguration(gridName);

        GridCacheConfiguration cc1 = new GridCacheConfiguration();

        cc1.setName(CACHE_REGULAR);
        cc1.setAtomicityMode(TRANSACTIONAL);
        cc1.setCacheMode(LOCAL);
        cc1.setWriteSynchronizationMode(FULL_SYNC);
        cc1.setTxSerializableEnabled(true);
        cc1.setSwapEnabled(true);
        cc1.setEvictSynchronized(false);
        cc1.setEvictNearSynchronized(false);

        GridCacheConfiguration cc2 = new GridCacheConfiguration();

        cc2.setName(CACHE_OFFHEAP);
        cc2.setAtomicityMode(TRANSACTIONAL);
        cc2.setCacheMode(LOCAL);
        cc2.setWriteSynchronizationMode(FULL_SYNC);
        cc2.setTxSerializableEnabled(true);
        cc2.setMemoryMode(OFFHEAP_VALUES);
        cc2.setOffHeapMaxMemory(100 * 1024 * 1024);
        cc2.setQueryIndexEnabled(false);

        c.setCacheConfiguration(cc1, cc2);

        c.setSwapSpaceSpi(new GridFileSwapSpaceSpi());

        return c;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        grid = startGrid(1);

        cache = grid.cache(CACHE_REGULAR);
        cacheOffheap = grid.cache(CACHE_OFFHEAP);
    }

    /** {@inheritDoc} */
    @Override protected void afterTestsStopped() throws Exception {
        super.afterTestsStopped();

        cache = null;
        cacheOffheap = null;

        grid = null;
    }

    /**
     * Check whether cache with byte array entry works correctly in PESSIMISTIC transaction.
     *
     * @throws Exception If failed.
     */
    public void testPessimistic() throws Exception {
        testTransaction(cache, PESSIMISTIC, KEY_1, wrap(1));
    }

    /**
     * Check whether cache with byte array entry works correctly in PESSIMISTIC transaction.
     *
     * @throws Exception If failed.
     */
    public void testPessimisticMixed() throws Exception {
        testTransactionMixed(cache, PESSIMISTIC, KEY_1, wrap(1), KEY_2, 1);
    }

    /**
     * Check whether offheap cache with byte array entry works correctly in PESSIMISTIC transaction.
     *
     * @throws Exception If failed.
     */
    public void testPessimisticOffheap() throws Exception {
        testTransaction(cacheOffheap, PESSIMISTIC, KEY_1, wrap(1));
    }

    /**
     * Check whether offheap cache with byte array entry works correctly in PESSIMISTIC transaction.
     *
     * @throws Exception If failed.
     */
    public void testPessimisticOffheapMixed() throws Exception {
        testTransactionMixed(cacheOffheap, PESSIMISTIC, KEY_1, wrap(1), KEY_2, 1);
    }

    /**
     * Check whether cache with byte array entry works correctly in OPTIMISTIC transaction.
     *
     * @throws Exception If failed.
     */
    public void testOptimistic() throws Exception {
        testTransaction(cache, OPTIMISTIC, KEY_1, wrap(1));
    }

    /**
     * Check whether cache with byte array entry works correctly in OPTIMISTIC transaction.
     *
     * @throws Exception If failed.
     */
    public void testOptimisticMixed() throws Exception {
        testTransactionMixed(cache, OPTIMISTIC, KEY_1, wrap(1), KEY_2, 1);
    }

    /**
     * Check whether offheap cache with byte array entry works correctly in OPTIMISTIC transaction.
     *
     * @throws Exception If failed.
     */
    public void testOptimisticOffheap() throws Exception {
        testTransaction(cacheOffheap, OPTIMISTIC, KEY_1, wrap(1));
    }

    /**
     * Check whether offheap cache with byte array entry works correctly in OPTIMISTIC transaction.
     *
     * @throws Exception If failed.
     */
    public void testOptimisticOffheapMixed() throws Exception {
        testTransactionMixed(cacheOffheap, OPTIMISTIC, KEY_1, wrap(1), KEY_2, 1);
    }

    /**
     * Test byte array entry swapping.
     *
     * @throws Exception If failed.
     */
    @SuppressWarnings("TooBroadScope")
    public void testSwap() throws Exception {
        assert cache.configuration().isSwapEnabled();

        byte[] val1 = wrap(1);
        Object val2 = 2;

        cache.put(KEY_1, val1);
        cache.put(KEY_2, val2);

        assert Arrays.equals(val1, (byte[])cache.get(KEY_1));
        assert F.eq(val2, cache.get(KEY_2));

        assert cache.evict(KEY_1);
        assert cache.evict(KEY_2);

        assert cache.peek(KEY_1) == null;
        assert cache.peek(KEY_2) == null;

        assert Arrays.equals(val1, (byte[])cache.promote(KEY_1));
        assert F.eq(val2, cache.promote(KEY_2));
    }

    /**
     * Test transaction behavior.
     *
     * @param cache Cache.
     * @param concurrency Concurrency.
     * @param key Key.
     * @param val Value.
     * @throws Exception If failed.
     */
    private void testTransaction(GridCache<Integer, Object> cache, GridCacheTxConcurrency concurrency,
        Integer key, byte[] val) throws Exception {
        testTransactionMixed(cache, concurrency, key, val, null, null);
    }

    /**
     * Test transaction behavior.
     *
     * @param cache Cache.
     * @param concurrency Concurrency.
     * @param key1 Key 1.
     * @param val1 Value 1.
     * @param key2 Key 2.
     * @param val2 Value 2.
     * @throws Exception If failed.
     */
    private void testTransactionMixed(GridCache<Integer, Object> cache, GridCacheTxConcurrency concurrency,
        Integer key1, byte[] val1, @Nullable Integer key2, @Nullable Object val2) throws Exception {

        GridCacheTx tx = cache.txStart(concurrency, REPEATABLE_READ);

        try {
            cache.put(key1, val1);

            if (key2 != null)
                cache.put(key2, val2);

            tx.commit();
        }
        finally {
            tx.close();
        }

        tx = cache.txStart(concurrency, REPEATABLE_READ);

        try {
            assert Arrays.equals(val1, (byte[])cache.get(key1));

            if (key2 != null)
                assert F.eq(val2, cache.get(key2));

            tx.commit();
        }
        finally {
            tx.close();
        }
    }
}
