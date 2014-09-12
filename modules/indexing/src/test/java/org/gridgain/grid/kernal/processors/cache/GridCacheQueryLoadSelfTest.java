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
import org.gridgain.grid.cache.query.*;
import org.gridgain.grid.cache.store.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.kernal.processors.cache.query.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.testframework.junits.common.*;
import org.jetbrains.annotations.*;

import java.util.*;

import static org.gridgain.grid.cache.GridCacheMode.*;
import static org.gridgain.grid.cache.GridCacheWriteSynchronizationMode.*;

/**
 * Test that entries are indexed on load/reload methods.
 */
public class GridCacheQueryLoadSelfTest extends GridCommonAbstractTest {
    /** IP finder. */
    private static final GridTcpDiscoveryIpFinder IP_FINDER = new GridTcpDiscoveryVmIpFinder(true);

    /** Puts count. */
    private static final int PUT_CNT = 10;

    /** Store map. */
    private static final Map<Integer, ValueObject> STORE_MAP = new HashMap<>();

    /** */
    public GridCacheQueryLoadSelfTest() {
        super(true);
    }

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        GridCacheConfiguration cache = defaultCacheConfiguration();

        cache.setCacheMode(REPLICATED);
        cache.setStore(new TestStore());
        cache.setWriteSynchronizationMode(FULL_SYNC);

        cfg.setCacheConfiguration(cache);

        GridTcpDiscoverySpi disco = new GridTcpDiscoverySpi();

        disco.setIpFinder(IP_FINDER);

        cfg.setDiscoverySpi(disco);

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        cache().removeAll();

        assert cache().isEmpty();
        assert size(ValueObject.class) == 0;

        STORE_MAP.clear();
    }

    /**
     * Number of objects of given type in index.
     *
     * @param cls Value type.
     * @return Objects number.
     * @throws GridException If failed.
     */
    private long size(Class<?> cls) throws GridException {
        GridCacheQueryManager<Object, Object> qryMgr = ((GridKernal)grid()).internalCache().context().queries();

        assert qryMgr != null;

        return qryMgr.size(cls);
    }

    /**
     * @throws Exception If failed.
     */
    public void testLoadCache() throws Exception {
        GridCache<Integer, ValueObject> cache = cache();

        cache.loadCache(null, 0);

        assert cache.size() == PUT_CNT;

        Collection<Map.Entry<Integer, ValueObject>> res =
            cache.queries().createSqlQuery(ValueObject.class, "val >= 0").execute().get();

        assertNotNull(res);
        assertEquals(PUT_CNT, res.size());
        assertEquals(PUT_CNT, size(ValueObject.class));
    }

    /**
     * @throws Exception If failed.
     */
    public void testLoadCacheAsync() throws Exception {
        GridCache<Integer, ValueObject> cache = cache();

        cache.loadCacheAsync(null, 0).get();

        assert cache.size() == PUT_CNT;

        Collection<Map.Entry<Integer, ValueObject>> res =
            cache.queries().createSqlQuery(ValueObject.class, "val >= 0").execute().get();

        assert res != null;
        assert res.size() == PUT_CNT;
        assert size(ValueObject.class) == PUT_CNT;
    }

    /**
     * @throws Exception If failed.
     */
    public void testLoadCacheFiltered() throws Exception {
        GridCache<Integer, ValueObject> cache = cache();

        cache.loadCache(new P2<Integer, ValueObject>() {
            @Override public boolean apply(Integer key, ValueObject val) {
                return key >= 5;
            }
        }, 0);

        assert cache.size() == PUT_CNT - 5;

        Collection<Map.Entry<Integer, ValueObject>> res =
            cache.queries().createSqlQuery(ValueObject.class, "val >= 0").execute().get();

        assert res != null;
        assert res.size() == PUT_CNT - 5;
        assert size(ValueObject.class) == PUT_CNT - 5;
    }

    /**
     * @throws Exception If failed.
     */
    public void testLoadCacheAsyncFiltered() throws Exception {
        GridCache<Integer, ValueObject> cache = cache();

        cache.loadCacheAsync(new P2<Integer, ValueObject>() {
            @Override public boolean apply(Integer key, ValueObject val) {
                return key >= 5;
            }
        }, 0).get();

        assert cache.size() == PUT_CNT - 5;

        Collection<Map.Entry<Integer, ValueObject>> res =
            cache.queries().createSqlQuery(ValueObject.class, "val >= 0").execute().get();

        assert res != null;
        assert res.size() == PUT_CNT - 5;
        assert size(ValueObject.class) == PUT_CNT - 5;
    }

    /**
     * @throws Exception If failed.
     */
    public void testReload() throws Exception {
        STORE_MAP.put(1, new ValueObject(1));

        GridCache<Integer, ValueObject> cache = cache();

        ValueObject vo = cache.reload(1);

        assertNotNull(vo);

        assertEquals(1, vo.value());
        assertEquals(1, cache.size());

        Collection<Map.Entry<Integer, ValueObject>> res =
            cache.queries().createSqlQuery(ValueObject.class, "val >= 0").execute().get();

        assert res != null;
        assert res.size() == 1;
        assert size(ValueObject.class) == 1;
    }

    /**
     * @throws Exception If failed.
     */
    public void testReloadAsync() throws Exception {
        STORE_MAP.put(1, new ValueObject(1));

        GridCache<Integer, ValueObject> cache = cache();

        assert cache.reloadAsync(1).get().value() == 1;

        assert cache.size() == 1;

        Collection<Map.Entry<Integer, ValueObject>> res =
            cache.queries().createSqlQuery(ValueObject.class, "val >= 0").execute().get();

        assert res != null;
        assert res.size() == 1;
        assert size(ValueObject.class) == 1;
    }

    /**
     * @throws Exception If failed.
     */
    public void testReloadAll() throws Exception {
        for (int i = 0; i < PUT_CNT; i++)
            STORE_MAP.put(i, new ValueObject(i));

        GridCache<Integer, ValueObject> cache = cache();

        Integer[] keys = new Integer[PUT_CNT - 5];

        for (int i = 0; i < PUT_CNT - 5; i++)
            keys[i] = i + 5;

        cache.reloadAll(F.asList(keys));

        assert cache.size() == PUT_CNT - 5;

        Collection<Map.Entry<Integer, ValueObject>> res =
            cache.queries().createSqlQuery(ValueObject.class, "val >= 0").execute().get();

        assert res != null;
        assert res.size() == PUT_CNT - 5;
        assert size(ValueObject.class) == PUT_CNT - 5;

        for (Integer key : keys)
            cache.clear(key);

        assert cache.isEmpty();
        assertEquals(0, cache.size());

        cache.reloadAll(Arrays.asList(keys));

        assertEquals(PUT_CNT - 5, cache.size());

        res = cache.queries().createSqlQuery(ValueObject.class, "val >= 0").execute().get();

        assert res != null;
        assert res.size() == PUT_CNT - 5;
        assert size(ValueObject.class) == PUT_CNT - 5;
    }

    /**
     * @throws Exception If failed.
     */
    public void testReloadAllAsync() throws Exception {
        for (int i = 0; i < PUT_CNT; i++)
            STORE_MAP.put(i, new ValueObject(i));

        GridCache<Integer, ValueObject> cache = cache();

        Integer[] keys = new Integer[PUT_CNT - 5];

        for (int i = 0; i < PUT_CNT - 5; i++)
            keys[i] = i + 5;

        cache.reloadAllAsync(F.asList(keys)).get();

        assert cache.size() == PUT_CNT - 5;

        Collection<Map.Entry<Integer, ValueObject>> res =
            cache.queries().createSqlQuery(ValueObject.class, "val >= 0").execute().get();

        assert res != null;
        assert res.size() == PUT_CNT - 5;
        assert size(ValueObject.class) == PUT_CNT - 5;

        // Invalidate will remove entries.
        for (Integer key : keys)
            cache.clear(key);

        assert cache.isEmpty();
        assertEquals(0, cache.size());

        cache.reloadAllAsync(Arrays.asList(keys)).get();

        assertEquals(PUT_CNT - 5, cache.size());

        res = cache.queries().createSqlQuery(ValueObject.class, "val >= 0").execute().get();

        assert res != null;
        assert res.size() == PUT_CNT - 5;
        assert size(ValueObject.class) == PUT_CNT - 5;
    }

    /**
     * @throws Exception If failed.
     */
    public void testReloadAllFiltered() throws Exception {
        GridCache<Integer, ValueObject> cache = cache();

        for (int i = 0; i < PUT_CNT; i++)
            assert cache.putx(i, new ValueObject(i));

        assert cache.size() == PUT_CNT;

        Integer[] keys = new Integer[PUT_CNT];

        for (int i = 0; i < PUT_CNT; i++)
            keys[i] = i;

        for (Integer key : keys)
            cache.clear(key);

        assert cache.isEmpty();
        assertEquals(0, cache.size());

        cache.projection(new P1<GridCacheEntry<Integer, ValueObject>>() {
            @Override public boolean apply(GridCacheEntry<Integer, ValueObject> e) {
                return e.getKey() >= 5;
            }
        }).reloadAll(Arrays.asList(keys));

        assert cache.size() == PUT_CNT - 5;

        Collection<Map.Entry<Integer, ValueObject>> res =
            cache.queries().createSqlQuery(ValueObject.class, "val >= 0").execute().get();

        assert res != null;
        assert res.size() == PUT_CNT - 5;
        assert size(ValueObject.class) == PUT_CNT - 5;
    }

    /**
     * @throws Exception If failed.
     */
    public void testReloadAllAsyncFiltered() throws Exception {
        GridCache<Integer, ValueObject> cache = cache();

        for (int i = 0; i < PUT_CNT; i++)
            assert cache.putx(i, new ValueObject(i));

        assert cache.size() == PUT_CNT;

        Integer[] keys = new Integer[PUT_CNT];

        for (int i = 0; i < PUT_CNT; i++)
            keys[i] = i;

        for (Integer key : keys)
            cache.clear(key);

        assert cache.isEmpty();
        assertEquals(0, cache.size());

        cache.projection(new P1<GridCacheEntry<Integer, ValueObject>>() {
            @Override public boolean apply(GridCacheEntry<Integer, ValueObject> e) {
                return e.getKey() >= 5;
            }
        }).reloadAllAsync(Arrays.asList(keys)).get();

        assertEquals(5, cache.size());

        Collection<Map.Entry<Integer, ValueObject>> res =
            cache.queries().createSqlQuery(ValueObject.class, "val >= 0").execute().get();

        assert res != null;
        assert res.size() == PUT_CNT - 5;
        assert size(ValueObject.class) == PUT_CNT - 5;
    }

    /**
     * Test store.
     */
    private static class TestStore extends GridCacheStoreAdapter<Integer, ValueObject> {
        /** {@inheritDoc} */
        @Override public void loadCache(GridBiInClosure<Integer, ValueObject> clo,
            @Nullable Object... args) throws GridException {
            assert clo != null;

            for (int i = 0; i < PUT_CNT; i++)
                clo.apply(i, new ValueObject(i));
        }

        /** {@inheritDoc} */
        @Override public ValueObject load(@Nullable GridCacheTx tx,
            Integer key) throws GridException {
            assert key != null;

            return STORE_MAP.get(key);
        }

        /** {@inheritDoc} */
        @Override public void put(@Nullable GridCacheTx tx,
            Integer key, ValueObject val) throws GridException {
            assert key != null;
            assert val != null;

            STORE_MAP.put(key, val);
        }

        /** {@inheritDoc} */
        @Override public void remove(@Nullable GridCacheTx tx,
            Integer key) throws GridException {
            assert key != null;

            STORE_MAP.remove(key);
        }
    }

    /**
     * Value object class.
     */
    private static class ValueObject {
        /** Value. */
        @GridCacheQuerySqlField
        private final int val;

        /**
         * @param val Value.
         */
        ValueObject(int val) {
            this.val = val;
        }

        /**
         * @return Value.
         */
        int value() {
            return val;
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(ValueObject.class, this);
        }
    }
}
