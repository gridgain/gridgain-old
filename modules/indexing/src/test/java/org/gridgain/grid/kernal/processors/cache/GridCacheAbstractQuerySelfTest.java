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
import org.gridgain.grid.marshaller.optimized.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.grid.spi.indexing.h2.*;
import org.gridgain.grid.spi.swapspace.file.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.testframework.*;
import org.gridgain.testframework.junits.common.*;
import org.jetbrains.annotations.*;
import org.junit.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import static org.gridgain.grid.cache.GridCacheAtomicityMode.*;
import static org.gridgain.grid.cache.GridCacheMode.*;
import static org.gridgain.grid.cache.GridCacheDistributionMode.*;
import static org.gridgain.grid.cache.GridCachePreloadMode.*;
import static org.gridgain.grid.cache.GridCacheWriteSynchronizationMode.*;

/**
 * Various tests for cache queries.
 */
public abstract class GridCacheAbstractQuerySelfTest extends GridCommonAbstractTest {
    /** Cache store. */
    private static TestStore store = new TestStore();

    /** */
    private static final GridTcpDiscoveryIpFinder ipFinder = new GridTcpDiscoveryVmIpFinder(true);

    /** */
    protected Grid grid;

    /**
     * @return Grid count.
     */
    protected abstract int gridCount();

    /**
     * @return Cache mode.
     */
    protected abstract GridCacheMode cacheMode();

    /**
     * @return Atomicity mode.
     */
    protected GridCacheAtomicityMode atomicityMode() {
        return TRANSACTIONAL;
    }

    /**
     * @return Distribution.
     */
    protected GridCacheDistributionMode distributionMode() {
        return NEAR_PARTITIONED;
    }

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration c = super.getConfiguration(gridName);

        GridTcpDiscoverySpi disco = new GridTcpDiscoverySpi();

        disco.setIpFinder(ipFinder);

        c.setDiscoverySpi(disco);

        GridH2IndexingSpi indexing = new GridH2IndexingSpi();

        indexing.setDefaultIndexPrimitiveKey(true);
        indexing.setIndexCustomFunctionClasses(SqlFunctions.class);

        c.setIndexingSpi(indexing);

        // Otherwise noop swap space will be chosen on Windows.
        c.setSwapSpaceSpi(new GridFileSwapSpaceSpi());

        c.setMarshaller(new GridOptimizedMarshaller(false));

        GridCacheConfiguration[] ccs = new GridCacheConfiguration[2];

        for (int i = 0; i < ccs.length; i++) {
            GridCacheConfiguration cc = defaultCacheConfiguration();

            if (i > 0)
                cc.setName("c" + i);

            cc.setCacheMode(cacheMode());
            cc.setAtomicityMode(atomicityMode());
            cc.setDistributionMode(distributionMode());
            cc.setWriteSynchronizationMode(FULL_SYNC);
            cc.setStore(store);
            cc.setPreloadMode(SYNC);
            cc.setSwapEnabled(true);
            cc.setEvictNearSynchronized(false);

            // Explicitly set number of backups equal to number of grids.
            if (cacheMode() == GridCacheMode.PARTITIONED)
                cc.setBackups(gridCount());

            ccs[i] = cc;
        }

        c.setCacheConfiguration(ccs);

        return c;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        grid = startGridsMultiThreaded(gridCount());
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        stopAllGrids();

        store.reset();

        grid = null;
    }

    /**
     * JUnit.
     *
     * @throws Exception In case of error.
     */
    public void testDifferentKeyTypes() throws Exception {
        GridCache<Object, Object> cache = grid.cache(null);

        cache.putx("key", "value");

        // Put the same value but for other key type.
        // Operation should succeed but with warning log message.
        cache.putx(1, "value");
    }

    /**
     * JUnit.
     *
     * @throws Exception In case of error.
     */
    public void testDifferentValueTypes() throws Exception {
        GridCache<Object, Object> cache = grid.cache(null);

        cache.putx("key", "value");

        // Put value of different type but for the same key type.
        // Operation should succeed but with warning log message.
        cache.putx("key", 1);
    }

    /**
     * JUnit.
     *
     * @throws Exception In case of error.
     */
    public void testStringType() throws Exception {
        GridCache<String, String> cache = grid.cache(null);

        cache.putx("tst", "test");

        GridCacheQuery<Map.Entry<String, String>> qry = cache.queries().createSqlQuery(String.class, "_val='test'");

        Map.Entry<String, String> entry = F.first(qry.execute().get());

        assert entry != null;
    }

    /**
     * JUnit.
     *
     * @throws Exception In case of error.
     */
    public void testIntegerType() throws Exception {
        GridCache<String, Integer> cache = grid.cache(null);

        String key = "k";

        int val = 2;

        cache.putx(key, val);

        GridCacheQuery<Map.Entry<String, Integer>> qry = cache.queries().createSqlQuery(Integer.class,
            "select * from Integer where _key = 'k' and _val > 1");

        Map.Entry<String, Integer> entry = F.first(qry.execute().get());

        assert entry != null;

        assertEquals(Integer.valueOf(val), entry.getValue());
    }

    /**
     * Tests UDFs.
     *
     * @throws GridException If failed.
     */
    public void testUserDefinedFunction() throws GridException {
        // Without alias.
        GridCacheQuery<List<?>> qry = grid.cache(null).queries().createSqlFieldsQuery("select square(1), square(2)").
            projection(grid);

        GridCacheQueryFuture<List<?>> fut = qry.execute();

        Collection<List<?>> res = fut.get();

        assertEquals(gridCount(), res.size());

        List<?> row = res.iterator().next();

        assertEquals(1, row.get(0));
        assertEquals(4, row.get(1));

        // With alias.
        qry = grid.cache(null).queries().createSqlFieldsQuery("select _cube_(1), _cube_(2)").projection(grid);

        fut = qry.execute();

        res = fut.get();

        assertEquals(gridCount(), res.size());

        row = res.iterator().next();

        assertEquals(1, row.get(0));
        assertEquals(8, row.get(1));

        // Not registered.
        final GridCacheQuery<List<?>> qry3 = grid.cache(null).queries().createSqlFieldsQuery("select no()");

        GridTestUtils.assertThrows(
            log,
            new Callable<Object>() {
                @Override public Object call() throws Exception {
                    qry3.execute().get();

                    return null;
                }
            },
            GridException.class,
            null
        );
    }

    /**
     * Expired entries are not included to result.
     *
     * @throws Exception If failed.
     */
    public void testExpiration() throws Exception {
        GridCache<String, Integer> cache = grid.cache(null);

        GridCacheEntry<String, Integer> entry = cache.entry("key1");

        assert entry != null;

        entry.timeToLive(1000);

        entry.set(1);

        assert entry.isCached();

        GridCacheQuery<Map.Entry<String, Integer>> qry = cache.queries().createSqlQuery(Integer.class, "1=1");

        Map.Entry<String, Integer> res = F.first(qry.execute().get());

        assertEquals(1, res.getValue().intValue());

        U.sleep(1020);

        res = F.first(qry.execute().get());

        assertNull(res);
    }

    /**
     * @throws Exception If failed.
     */
    public void testIllegalBounds() throws Exception {
        GridCache<Integer, Integer> cache = grid.cache(null);

        cache.put(1, 1);
        cache.put(2, 2);

        GridCacheQuery<Map.Entry<Integer, Integer>> qry = cache.queries().createSqlQuery(Integer.class,
            "_key between 2 and 1");

        assertTrue(qry.execute().get().isEmpty());
    }

    /**
     * JUnit.
     *
     * @throws Exception In case of error.
     */
    public void testComplexType() throws Exception {
        GridCache<Key, GridCacheQueryTestValue> cache = grid.cache(null);

        GridCacheQueryTestValue val1 = new GridCacheQueryTestValue();

        val1.setField1("field1");
        val1.setField2(1);
        val1.setField3(1L);

        GridCacheQueryTestValue val2 = new GridCacheQueryTestValue();

        val2.setField1("field2");
        val2.setField2(2);
        val2.setField3(2L);
        val2.setField6(null);

        cache.putx(new Key(100500), val1);
        cache.putx(new Key(100501), val2);

        GridCacheQuery<Map.Entry<Key, GridCacheQueryTestValue>> qry =
            cache.queries().createSqlQuery(GridCacheQueryTestValue.class,
                "fieldName='field1' and field2=1 and field3=1 and id=100500 and embeddedField2=11 and x=3");

        Map.Entry<Key, GridCacheQueryTestValue> entry = F.first(qry.execute().get());

        assertNotNull(entry);
        assertEquals(100500, entry.getKey().id);
        assertEquals(val1, entry.getValue());
    }

    /**
     * Complex key type.
     */
    private static class Key {
        /** */
        @GridCacheQuerySqlField
        private final long id;

        /**
         * @param id Id.
         */
        private Key(long id) {
            this.id = id;
        }

        /** {@inheritDoc} */
        @Override public boolean equals(Object o) {
            if (this == o)
                return true;

            if (o == null || getClass() != o.getClass())
                return false;

            Key key = (Key)o;

            return id == key.id;

        }

        /** {@inheritDoc} */
        @Override public int hashCode() {
            return (int)(id ^ (id >>> 32));
        }
    }

    /**
     * JUnit.
     *
     * @throws Exception In case of error.
     */
    public void testSelectQuery() throws Exception {
        GridCache<String, String> cache = grid.cache(null);

        cache.putx("key", "value");

        GridCacheQuery<Map.Entry<String, String>> qry = cache.queries().createSqlQuery(String.class,
            "select * from String");

        GridCacheQueryFuture<Map.Entry<String, String>> iter = qry.execute();

        assert iter != null;
        assert iter.next() != null;
    }

    /**
     * JUnit.
     *
     * @throws Exception In case of error.
     */
    public void testObjectQuery() throws Exception {
        GridCache<Integer, ObjectValue> cache = grid.cache(null);

        ObjectValue val = new ObjectValue("test", 0);

        cache.putx(1, val);

        GridCacheQuery<Map.Entry<Integer, ObjectValue>> qry =
            cache.queries().createSqlQuery(ObjectValue.class, "_val=?");

        GridCacheQueryFuture<Map.Entry<Integer, ObjectValue>> iter = qry.execute(val);

        assert iter != null;

        int expCnt = 1;

        for (int i = 0; i < expCnt; i++)
            assert iter.next() != null;

        assert iter.next() == null;

        qry = cache.queries().createFullTextQuery(ObjectValue.class, "test");

        iter = qry.execute();

        assert iter != null;

        for (int i = 0; i < expCnt; i++)
            assert iter.next() != null;

        assert iter.next() == null;
    }

    /**
     * JUnit.
     *
     * @throws Exception In case of error.
     */
    public void testTextQueryOnProjection() throws Exception {
        GridCache<Integer, ObjectValue> cache = grid.cache(null);

        cache.putx(1, new ObjectValue("test", 1));
        cache.putx(2, new ObjectValue("test", 2));

        P2<Integer, ObjectValue> p = new P2<Integer, ObjectValue>() {
            @Override public boolean apply(Integer key, ObjectValue val) {
                return val.intVal == 1;
            }
        };

        GridCacheProjection<Integer, ObjectValue> cachePrj = grid(0).<Integer, ObjectValue>cache(null).projection(p);

        GridCacheQuery<Map.Entry<Integer, ObjectValue>> qry =
            cachePrj.queries().createFullTextQuery(ObjectValue.class, "test");

        GridCacheQueryFuture<Map.Entry<Integer, ObjectValue>> iter = qry.execute();

        assert iter != null;

        int expCnt = 1;

        for (int i = 0; i < expCnt; i++)
            assert iter.next() != null;

        assert iter.next() == null;
    }

    /**
     * JUnit.
     *
     * @throws Exception In case of error.
     */
    public void testObjectQueryWithSwap() throws Exception {
        GridCache<Integer, ObjectValue> cache = grid.cache(null);

        boolean partitioned = cache.configuration().getCacheMode() == PARTITIONED;

        int cnt = 10;

        for (int i = 0; i < cnt; i++)
            cache.putx(i, new ObjectValue("test" + i, i));

        for (Grid g : G.allGrids()) {
            GridCache<Integer, ObjectValue> c = g.cache(null);

            for (int i = 0; i < cnt; i++) {
                if (i % 2 == 0) {
                    assertNotNull(c.peek(i));

                    c.evict(i); // Swap.

                    if (!partitioned || c.affinity().mapKeyToNode(i).isLocal()) {
                        ObjectValue peekVal = c.peek(i);

                        assertNull("Non-null value for peek [key=" + i + ", val=" + peekVal + ']', peekVal);
                    }
                }
            }
        }

        GridCacheQuery<Map.Entry<Integer, ObjectValue>> qry =
            cache.queries().createSqlQuery(ObjectValue.class, "intVal >= ? order by intVal");

        qry.enableDedup(true);

        GridCacheQueryFuture<Map.Entry<Integer, ObjectValue>> iter = qry.execute(0);

        assert iter != null;

        Collection<Integer> set = new HashSet<>(cnt);

        Map.Entry<Integer, ObjectValue> next;

        while ((next = iter.next()) != null) {
            ObjectValue v = next.getValue();

            assert !set.contains(v.intValue());

            set.add(v.intValue());
        }

        assert iter.next() == null;

        assertEquals(cnt, set.size());

        for (int i = 0; i < cnt; i++)
            assert set.contains(i);

        qry = cache.queries().createSqlQuery(ObjectValue.class, "MOD(intVal, 2) = ? order by intVal");

        qry.enableDedup(true);

        iter = qry.execute(0);

        assert iter != null;

        set.clear();

        while ((next = iter.next()) != null) {
            ObjectValue v = next.getValue();

            assert !set.contains(v.intValue());

            set.add(v.intValue());
        }

        assert iter.next() == null;

        assertEquals(cnt / 2, set.size());

        for (int i = 0; i < cnt; i++)
            if (i % 2 == 0)
                assert set.contains(i);
            else
                assert !set.contains(i);
    }

    /**
     * JUnit.
     *
     * @throws Exception In case of error.
     */
    public void testFullTextSearch() throws Exception {
        GridCache<Integer, ObjectValue> cache = grid.cache(null);

        // Try to execute on empty cache first.
        GridCacheQuery<Map.Entry<Integer, ObjectValue>> qry = cache.queries().createFullTextQuery(ObjectValue.class,
            "full");

        assert qry.execute().get().isEmpty();

        qry = cache.queries().createFullTextQuery(ObjectValue.class, "full");

        assert qry.execute().get().isEmpty();

        // Now put indexed values into cache.
        int key1 = 1;

        ObjectValue val1 = new ObjectValue("test full text", 0);

        cache.putx(key1, val1);

        int key2 = 2;

        ObjectValue val2 = new ObjectValue("test full text more", 0);

        cache.putx(key2, val2);

        qry = cache.queries().createFullTextQuery(ObjectValue.class, "full");

        Collection<Map.Entry<Integer, ObjectValue>> res = qry.execute().get();

        assert res != null;

        assert res.size() == 2;

        qry = cache.queries().createFullTextQuery(ObjectValue.class, "full");

        res = qry.execute().get();

        assert res != null;
        assert res.size() == 2;
    }

    /**
     * JUnit.
     *
     * @throws Exception In case of error.
     */
    public void testRemoveIndex() throws Exception {
        GridCache<Integer, ObjectValue> cache = grid.cache(null);
        GridCache<Integer, ObjectValue> cache1 = grid.cache("c1");

        ObjectValue val = new ObjectValue("test full text", 0);

        int key = 1;

        cache.putx(key, val);
        cache1.putx(key, val);

        GridCacheQueryManager<Object, Object> qryMgr = ((GridKernal)grid).internalCache().context().queries();
        GridCacheQueryManager<Object, Object> qryMgr1 = ((GridKernal)grid).internalCache("c1").context().queries();

        assert hasIndexTable(ObjectValue.class, qryMgr);
        assert hasIndexTable(ObjectValue.class, qryMgr1);

        assert qryMgr != null;

        qryMgr.onUndeploy(ObjectValue.class.getClassLoader());

        assert !hasIndexTable(ObjectValue.class, qryMgr);
        assert hasIndexTable(ObjectValue.class, qryMgr1);

        // Put again.
        cache.putx(key, val);

        assert hasIndexTable(ObjectValue.class, qryMgr);
        assert hasIndexTable(ObjectValue.class, qryMgr1);
    }

    /**
     * JUnit.
     *
     * @throws Exception In case of error.
     */
    public void testScanQuery() throws Exception {
        GridCache<String, String> c1 = grid.cache(null);

        c1.putx("key", "value");

        GridCacheQuery<Map.Entry<String, String>> qry1 = c1.queries().createScanQuery(null);

        GridCacheQueryFuture<Map.Entry<String, String>> iter = qry1.execute();

        assert iter != null;

        int expCnt = 1;

        for (int i = 0; i < expCnt; i++) {
            Map.Entry<String, String> e1 = iter.next();

            assertEquals("key", e1.getKey());
            assertEquals("value", e1.getValue());
        }

        assert iter.next() == null;
    }

    /**
     * JUnit.
     *
     * @throws Exception In case of error.
     */
    public void testTwoObjectsTextSearch() throws Exception {
        GridCache<Object, Object> c = grid.cache(null);

        c.put(1, new ObjectValue("ObjectValue str", 1));
        c.put("key", new ObjectValueOther("ObjectValueOther str"));

        Collection<Map.Entry<Object, Object>> res =
            c.queries().createFullTextQuery(ObjectValue.class, "str").execute().get();

        assert res != null;
        int expCnt = 1;
        assert res.size() == expCnt;
        assert F.first(res).getValue().getClass() == ObjectValue.class;

        res = c.queries().createFullTextQuery(ObjectValueOther.class, "str").execute().get();

        assert res != null;
        assert res.size() == expCnt;
        assert F.first(res).getValue().getClass() == ObjectValueOther.class;

        res = c.queries().createFullTextQuery(ObjectValue.class, "str").execute().get();

        assert res != null;
        assert res.size() == expCnt;
        assert F.first(res).getValue().getClass() == ObjectValue.class;

        res = c.queries().createFullTextQuery(ObjectValueOther.class, "str").execute().get();

        assert res != null;
        assert res.size() == expCnt;
        assert F.first(res).getValue().getClass() == ObjectValueOther.class;
    }

    /**
     * JUnit.
     * @throws Exception In case of error.
     */
    public void testTransformQuery() throws Exception {
        GridCache<UUID, Person> c = grid.cache(null);

        final Person p1 = new Person(UUID.randomUUID(), "Bob");
        final Person p2 = new Person(UUID.randomUUID(), "Tom");

        c.put(p1.id, p1);
        c.put(p2.id, p2);

        GridCacheQuery<Map.Entry<UUID, Person>> q = c.queries().createScanQuery(null);

        Collection<Map.Entry<UUID, String>> res = q.execute(new C1<Map.Entry<UUID, Person>, Map.Entry<UUID, String>>() {
            @Override public Map.Entry<UUID, String> apply(Map.Entry<UUID, Person> p) {
                return F.t(p.getKey(), p.getValue().name);
            }
        }).get();

        assert res != null;
        assert res.size() == 2;

        F.forEach(res, new CI1<Map.Entry<UUID, String>>() {
            @Override public void apply(Map.Entry<UUID, String> e) {
                if (p1.id.equals(e.getKey()))
                    assert "Bob".equals(e.getValue());
                else if (p2.id.equals(e.getKey()))
                    assert "Tom".equals(e.getValue());
                else
                    assert false : "Unexpected entry.";
            }
        });

        q = c.queries().createSqlQuery(Person.class, "id = ?");

        res = q.execute(new C1<Map.Entry<UUID, Person>, Map.Entry<UUID, String>>() {
            @Override public Map.Entry<UUID, String> apply(Map.Entry<UUID, Person> p) {
                return F.t(p.getKey(), p.getValue().name);
            }
        }, p1.id).get();

        assert res != null;
        assert res.size() == 1;

        F.forEach(res, new CI1<Map.Entry<UUID, String>>() {
            @Override public void apply(Map.Entry<UUID, String> e) {
                assert p1.id.equals(e.getKey());
                assert "Bob".equals(e.getValue());
            }
        });

        q = c.queries().createFullTextQuery(Person.class, "Bob");

        res = q.execute(new C1<Map.Entry<UUID, Person>, Map.Entry<UUID, String>>() {
            @Override public Map.Entry<UUID, String> apply(Map.Entry<UUID, Person> p) {
                return F.t(p.getKey(), p.getValue().name);
            }
        }).get();

        assertNotNull(res);

        assertEquals(1, res.size());

        F.forEach(res, new CI1<Map.Entry<UUID, String>>() {
            @Override public void apply(Map.Entry<UUID, String> e) {
                assert p1.id.equals(e.getKey());
                assert "Bob".equals(e.getValue());
            }
        });

        q = c.queries().createFullTextQuery(Person.class, "Bob");

        res = q.execute(new C1<Map.Entry<UUID, Person>, Map.Entry<UUID, String>>() {
            @Override public Map.Entry<UUID, String> apply(Map.Entry<UUID, Person> p) {
                return F.t(p.getKey(), p.getValue().name);
            }
        }).get();

        assert res != null;
        assert res.size() == 1;

        F.forEach(res, new CI1<Map.Entry<UUID, String>>() {
            @Override public void apply(Map.Entry<UUID, String> e) {
                assert p1.id.equals(e.getKey());
                assert "Bob".equals(e.getValue());
            }
        });
    }

    /**
     * @throws Exception If failed.
     */
    public void testReduceQuery() throws Exception {
        GridCache<String, Integer> c = grid.cache(null);

        assert c.putx("key1", 1);
        assert c.putx("key2", 2);
        assert c.putx("key3", 3);
        assert c.putx("key4", 4);
        assert c.putx("key5", 5);

        GridCacheQuery<Map.Entry<String, Integer>> qry = c.queries().createSqlQuery(Integer.class, "_val > 2");

        Collection<Integer> res1 = qry.execute(new SumRemoteReducer()).get();

        assert res1 != null;
        assert res1.size() == gridCount();
        assert F.sumInt(res1) == (cacheMode() == REPLICATED ? 12 * gridCount() : 12);
    }

    /**
     * @throws Exception If failed.
     */
    public void testReduceQueryOnProjection() throws Exception {
        GridCacheProjection<String, Integer> c = grid.cache(null);

        assert c.putx("key1", 1);
        assert c.putx("key2", 2);
        assert c.putx("key3", 3);
        assert c.putx("key4", 4);
        assert c.putx("key5", 5);

        // Filter values less than 3.
        P2<String, Integer> p = new P2<String, Integer>() {
            @Override public boolean apply(String key, Integer val) {
                return val > 3;
            }
        };

        GridCacheProjection<String, Integer> cachePrj = grid.<String, Integer>cache(null).projection(p);

        GridCacheQuery<Map.Entry<String, Integer>> q = cachePrj.queries().createSqlQuery(Integer.class, "_val > 2");

        Collection<Integer> res = q.execute(new SumRemoteReducer()).get();

        assertEquals(9, F.sumInt(res));
    }

    /**
     * @throws Exception If failed.
     */
    public void testEmptyObject() throws Exception {
        GridCache<EmptyObject, EmptyObject> cache = grid.cache(null);

        cache.putx(new EmptyObject(1), new EmptyObject(2));

        for (int i = 0; i < gridCount(); i++) {
            GridCacheQueryManager<Object, Object> qryMgr =
                ((GridKernal)grid(i)).internalCache().context().queries();

            assert !hasIndexTable(EmptyObject.class, qryMgr);
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testPrimitiveType() throws Exception {
        GridCache<Integer, Integer> cache = grid.cache(null);

        assert cache.putx(1, 1);
        assert cache.putx(2, 2);

        GridCacheQuery<Map.Entry<Integer, Integer>> q = cache.queries().createSqlQuery(int.class, "_val > 1");

        Collection<Map.Entry<Integer, Integer>> res = q.execute().get();

        assertEquals(1, res.size());

        for (Map.Entry<Integer, Integer> e : res) {
            assertEquals(2, (int)e.getKey());
            assertEquals(2, (int)e.getValue());
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testPaginationIteratorDefaultCache() throws Exception {
        testPaginationIterator(null);
    }

    /**
     * @throws Exception If failed.
     */
    public void testPaginationIteratorNamedCache() throws Exception {
        testPaginationIterator("c1");
    }

    /**
     * @param cacheName Cache name.
     * @throws Exception If failed.
     */
    private void testPaginationIterator(@Nullable String cacheName) throws Exception {
        GridCache<Integer, Integer> cache = grid.cache(cacheName);

        for (int i = 0; i < 50; i++)
            assertTrue(cache.putx(i, i));

        GridCacheQuery<Map.Entry<Integer, Integer>> q = cache.queries().createSqlQuery(Integer.class,
            "_key >= 0").projection(grid);

        q.pageSize(10);
        q.enableDedup(true);
        q.keepAll(false);

        GridCacheQueryFuture<Map.Entry<Integer, Integer>> f = q.execute();

        int cnt = 0;

        Map.Entry<Integer, Integer> e;

        while ((e = f.next()) != null) {
            assertTrue(e.getKey() >= 0 && e.getKey() < 50);
            assertTrue(e.getValue() >= 0 && e.getValue() < 50);

            cnt++;
        }

        assertEquals(50, cnt);

        assertTrue(f.isDone());

        if (cacheMode() != LOCAL)
            assertTrue(f.get().size() < 50);
    }

    /**
     * @throws Exception If failed.
     */
    public void testPaginationIteratorKeepAll() throws Exception {
        GridCache<Integer, Integer> cache = grid.cache(null);

        for (int i = 0; i < 50; i++)
            assertTrue(cache.putx(i, i));

        GridCacheQuery<Map.Entry<Integer, Integer>> q = cache.queries().createSqlQuery(Integer.class, "_key >= 0");

        q.pageSize(10);
        q.enableDedup(true);
        q.keepAll(true);

        GridCacheQueryFuture<Map.Entry<Integer, Integer>> f = q.execute();

        int cnt = 0;

        Map.Entry<Integer, Integer> e;

        while ((e = f.next()) != null) {
            assertTrue(e.getKey() >= 0 && e.getKey() < 50);
            assertTrue(e.getValue() >= 0 && e.getValue() < 50);

            cnt++;
        }

        assertEquals(50, cnt);

        assertTrue(f.isDone());

        List<Map.Entry<Integer, Integer>> list = new ArrayList<>(f.get());

        Collections.sort(list, new Comparator<Map.Entry<Integer, Integer>>() {
            @Override public int compare(Map.Entry<Integer, Integer> e1, Map.Entry<Integer, Integer> e2) {
                return e1.getKey().compareTo(e2.getKey());
            }
        });

        for (int i = 0; i < 50; i++) {
            Map.Entry<Integer, Integer> e0 = list.get(i);

            assertEquals(i, (int)e0.getKey());
            assertEquals(i, (int)e0.getValue());
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testPaginationGetDefaultCache() throws Exception {
        testPaginationGet(null);
    }

    /**
     * @throws Exception If failed.
     */
    public void testPaginationGetNamedCache() throws Exception {
        testPaginationGet("c1");
    }

    /**
     * @param cacheName Cache name.
     * @throws Exception If failed.
     */
    private void testPaginationGet(@Nullable String cacheName) throws Exception {
        GridCache<Integer, Integer> cache = grid.cache(cacheName);

        for (int i = 0; i < 50; i++)
            assertTrue(cache.putx(i, i));

        GridCacheQuery<Map.Entry<Integer, Integer>> q = cache.queries().createSqlQuery(Integer.class, "_key >= 0");

        q.pageSize(10);
        q.enableDedup(true);

        List<Map.Entry<Integer, Integer>> list = new ArrayList<>(q.execute().get());

        Collections.sort(list, new Comparator<Map.Entry<Integer, Integer>>() {
            @Override public int compare(Map.Entry<Integer, Integer> e1, Map.Entry<Integer, Integer> e2) {
                return e1.getKey().compareTo(e2.getKey());
            }
        });

        for (int i = 0; i < 50; i++) {
            Map.Entry<Integer, Integer> e = list.get(i);

            assertEquals(i, (int)e.getKey());
            assertEquals(i, (int)e.getValue());
        }
    }

//    /**
//     * @throws Exception If failed.
//     */
//    public void testSqlFilters() throws Exception {
//        GridCache<Integer, Integer> cache = grid.cache(null);
//
//        for (int i = 0; i < 50; i++)
//            assertTrue(cache.putx(i, i));
//
//        GridCacheQuery<Map.Entry<Integer, Integer>> q = cache.queries().createSqlQuery(Integer.class, "_key >= 10");
//
//        q.enableDedup(true);
//
//        q = q.remoteKeyFilter(
//            new P1<Integer>() {
//                @Override public boolean apply(Integer i) {
//                    assertNotNull(i);
//
//                    return i >= 20;
//                }
//            }
//        ).remoteValueFilter(
//            new P1<Integer>() {
//                @Override public boolean apply(Integer i) {
//                    assertNotNull(i);
//
//                    return i < 40;
//                }
//            });
//
//        List<Map.Entry<Integer, Integer>> list = new ArrayList<>(q.execute().get());
//
//        Collections.sort(list, new Comparator<Map.Entry<Integer, Integer>>() {
//            @Override public int compare(Map.Entry<Integer, Integer> e1, Map.Entry<Integer, Integer> e2) {
//                return e1.getKey().compareTo(e2.getKey());
//            }
//        });
//
//        assertEquals(20, list.size());
//
//        for (int i = 20; i < 40; i++) {
//            Map.Entry<Integer, Integer> e = list.get(i - 20);
//
//            assertEquals(i, (int)e.getKey());
//            assertEquals(i, (int)e.getValue());
//        }
//    }

    /**
     * @throws Exception If failed.
     */
    public void testScanFilters() throws Exception {
        GridCache<Integer, Integer> cache = grid.cache(null);

        for (int i = 0; i < 50; i++)
            assertTrue(cache.putx(i, i));

        GridCacheQuery<Map.Entry<Integer, Integer>> q = cache.queries().createScanQuery(
            new P2<Integer, Integer>() {
                @Override public boolean apply(Integer k, Integer v) {
                    assertNotNull(k);
                    assertNotNull(v);

                    return k >= 20 && v < 40;
                }
            });

        q.enableDedup(true);

        List<Map.Entry<Integer, Integer>> list = new ArrayList<>(q.execute().get());

        Collections.sort(list, new Comparator<Map.Entry<Integer, Integer>>() {
            @Override public int compare(Map.Entry<Integer, Integer> e1, Map.Entry<Integer, Integer> e2) {
                return e1.getKey().compareTo(e2.getKey());
            }
        });

        assertEquals(20, list.size());

        for (int i = 20; i < 40; i++) {
            Map.Entry<Integer, Integer> e = list.get(i - 20);

            assertEquals(i, (int)e.getKey());
            assertEquals(i, (int)e.getValue());
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testEmptyGrid() throws Exception {
        GridCache<String, Integer> cache = grid.cache(null);

        String key = "k";

        int val = 2;

        cache.putx(key, val);

        GridCacheQuery<Map.Entry<String, Integer>> qry = cache.queries().createSqlQuery(Integer.class,
            "select * from Integer where _key = 'k' and _val > 1");

        Map.Entry<String, Integer> entry = F.first(qry.execute().get());

        assert entry != null;

        assertEquals(Integer.valueOf(val), entry.getValue());
    }

    /**
     * @throws GridException if failed.
     */
    public void testBadHashObjectKey() throws GridException {
        GridCache<BadHashKeyObject, Integer> cache = grid.cache(null);

        cache.put(new BadHashKeyObject("test_key1"), 9);
        cache.put(new BadHashKeyObject("test_key0"), 1005001);
        cache.put(new BadHashKeyObject("test_key1"), 7);

        assertEquals(1005001, cache.queries().createSqlQuery(Integer.class, "_key = ?").execute(new BadHashKeyObject(
            "test_key0")).get().iterator().next().getValue().intValue());
    }

    /**
     * @throws GridException if failed.
     */
    public void testTextIndexedKey() throws GridException {
        GridCache<ObjectValue, Integer> cache = grid.cache(null);

        cache.put(new ObjectValue("test_key1", 10), 19);
        cache.put(new ObjectValue("test_key0", 11), 11005);
        cache.put(new ObjectValue("test_key1", 12), 17);

        assertEquals(11005,
            cache.queries().createFullTextQuery(Integer.class, "test_key0").execute().get().iterator().next()
                .getValue().intValue());
    }

    /**
     * @throws Exception If failed.
     */
    public void testAnonymousClasses() throws Exception {
        GridCache<Integer, Object> cache = grid.cache(null);

        Object val = new Object() {
            @Override public String toString() {
                return "Test anonymous object.";
            }
        };

        assertTrue(cache.putx(1, val));

        GridCacheQuery<Map.Entry<Integer, Object>> q = cache.queries().createSqlQuery(val.getClass(), "_key >= 0");

        q.enableDedup(true);

        Collection<Map.Entry<Integer, Object>> res = q.execute().get();

        assertEquals(1, res.size());
    }

    /**
     * @throws Exception If failed.
     */
    public void testOrderByOnly() throws Exception {
        GridCache<Integer, Integer> cache = grid.cache(null);

        for (int i = 0; i < 10; i++)
            assertTrue(cache.putx(i, i));

        GridCacheQuery<Map.Entry<Integer, Integer>> q = cache.queries().createSqlQuery(Integer.class, "order by _val");

        q.enableDedup(true);

        Collection<Map.Entry<Integer, Integer>> res = q.execute().get();

        assertEquals(10, res.size());

        if (cacheMode() != PARTITIONED) {
            Iterator<Map.Entry<Integer, Integer>> it = res.iterator();

            for (Integer i = 0; i < 10; i++) {
                assertTrue(it.hasNext());

                Map.Entry<Integer, Integer> e = it.next();

                assertEquals(i, e.getKey());
                assertEquals(i, e.getValue());
            }
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testLimitOnly() throws Exception {
        GridCache<Integer, Integer> cache = grid.cache(null);

        for (int i = 0; i < 10; i++)
            assertTrue(cache.putx(i, i));

        GridCacheQuery<Map.Entry<Integer, Integer>> q = cache.queries().createSqlQuery(Integer.class, "limit 5");

        q.enableDedup(true);

        Collection<Map.Entry<Integer, Integer>> res = q.execute().get();

        if (cacheMode() != PARTITIONED) {
            assertEquals(5, res.size());

            Iterator<Map.Entry<Integer, Integer>> it = res.iterator();

            for (Integer i = 0; i < 5; i++) {
                assertTrue(it.hasNext());

                Map.Entry<Integer, Integer> e = it.next();

                assertEquals(i, e.getKey());
                assertEquals(i, e.getValue());
            }
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testArray() throws Exception {
        GridCache<Integer, ArrayObject> cache = grid.cache(null);

        assertTrue(cache.putx(1, new ArrayObject(new Long[] {1L, null, 3L})));
        assertTrue(cache.putx(2, new ArrayObject(new Long[] {4L, 5L, 6L})));

        GridCacheQuery<Map.Entry<Integer, ArrayObject>> q =
            cache.queries().createSqlQuery(ArrayObject.class, "array_contains(arr, cast(4 as long))");

        q.enableDedup(true);

        Collection<Map.Entry<Integer, ArrayObject>> res = q.execute().get();

        assertEquals(1, res.size());

        Map.Entry<Integer, ArrayObject> e = F.first(res);

        assertEquals(2, (int)e.getKey());
        Assert.assertArrayEquals(new Long[] {4L, 5L, 6L}, e.getValue().arr);
    }

    /**
     * @param cls Class to check index table for.
     * @param qryMgr Query manager.
     * @return {@code true} if index has a table for given class.
     * @throws GridException If failed.
     */
    private boolean hasIndexTable(Class<?> cls, GridCacheQueryManager<Object, Object> qryMgr) throws GridException {
        return qryMgr.size(cls) != -1;
    }

    /**
     *
     */
    private static class ArrayObject implements Serializable {
        /** */
        @GridCacheQuerySqlField
        private Long[] arr;

        /**
         * @param arr Array.
         */
        private ArrayObject(Long[] arr) {
            this.arr = arr;
        }
    }

    /**
     *
     */
    private static class Person {
        /** */
        @GridCacheQuerySqlField
        private UUID id;

        /** */
        @GridCacheQueryTextField
        private String name;

        /**
         * @param id Id.
         */
        Person(UUID id) {
            this.id = id;
        }

        /**
         * @param id Id;
         * @param name Name.
         */
        private Person(UUID id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    /**
     * Test value object.
     */
    @SuppressWarnings("PublicInnerClass")
    public static class ObjectValue implements Serializable {
        /** String value. */
        @GridCacheQueryTextField
        private String strVal;

        /** Integer value. */
        @GridCacheQuerySqlField
        private int intVal;

        /**
         * Constructor.
         *
         * @param strVal String value.
         * @param intVal Integer value.
         */
        ObjectValue(String strVal, int intVal) {
            this.strVal = strVal;
            this.intVal = intVal;
        }

        /**
         * Gets value.
         *
         * @return Value.
         */
        public String getStringValue() {
            return strVal;
        }

        /**
         * @return Integer value.
         */
        public int intValue() {
            return intVal;
        }

        /** {@inheritDoc} */
        @Override public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            ObjectValue other = (ObjectValue)o;

            return strVal == null ? other.strVal == null : strVal.equals(other.strVal);

        }

        /** {@inheritDoc} */
        @Override public int hashCode() {
            return strVal != null ? strVal.hashCode() : 0;
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(ObjectValue.class, this);
        }
    }

    /**
     * Another test value object.
     */
    private static class ObjectValueOther {
        /** Value. */
        @GridCacheQueryTextField
        private String val;

        /**
         * @param val String value.
         */
        ObjectValueOther(String val) {
            this.val = val;
        }

        /**
         * Gets value.
         *
         * @return Value.
         */
        public String value() {
            return val;
        }

        /** {@inheritDoc} */
        @Override public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            ObjectValueOther other = (ObjectValueOther)o;

            return val == null ? other.val == null : val.equals(other.val);

        }

        /** {@inheritDoc} */
        @Override public int hashCode() {
            return val != null ? val.hashCode() : 0;
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(ObjectValueOther.class, this);
        }
    }

    /**
     * Empty test object.
     */
    @SuppressWarnings("UnusedDeclaration")
    private static class EmptyObject {
        /** */
        private int val;

        /**
         * @param val Value.
         */
        private EmptyObject(int val) {
            this.val = val;
        }

        /** {@inheritDoc} */
        @Override public int hashCode() {
            return val;
        }

        /** {@inheritDoc} */
        @Override public boolean equals(Object o) {
            if (this == o)
                return true;

            if (!(o instanceof EmptyObject))
                return false;

            EmptyObject that = (EmptyObject)o;

            return val == that.val;
        }
    }

    /**
     *
     */
    private static class BadHashKeyObject implements Serializable {
        /** */
        @GridCacheQuerySqlField(index = false)
        private final String str;

        /**
         * @param str String.
         */
        private BadHashKeyObject(String str) {
            this.str = str;
        }

        /** {@inheritDoc} */
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            BadHashKeyObject keyObj = (BadHashKeyObject) o;

            return str.equals(keyObj.str);
        }

        /** {@inheritDoc} */
        @Override public int hashCode() {
            return 10;
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(BadHashKeyObject.class, this);
        }
    }

    /**
     * Test store.
     */
    private static class TestStore extends GridCacheStoreAdapter<Object, Object> {
        /** */
        private Map<Object, Object> map = new ConcurrentHashMap<>();

        /** */
        void reset() {
            map.clear();
        }

        /** {@inheritDoc} */
        @Override public Object load(@Nullable GridCacheTx tx, Object key)
            throws GridException {
            return map.get(key);
        }

        /** {@inheritDoc} */
        @Override public void put(GridCacheTx tx, Object key, @Nullable Object val)
            throws GridException {
            map.put(key, val);
        }

        /** {@inheritDoc} */
        @Override public void remove(GridCacheTx tx, Object key) throws GridException {
            map.remove(key);
        }
    }

    /**
     * Functions for test.
     */
    @SuppressWarnings("PublicInnerClass")
    public static class SqlFunctions {
        /**
         * @param x Argument.
         * @return Square of given value.
         */
        @GridCacheQuerySqlFunction
        public static int square(int x) {
            return x * x;
        }

        /**
         * @param x Argument.
         * @return Cube of given value.
         */
        @GridCacheQuerySqlFunction(alias = "_cube_")
        public static int cube(int x) {
            return x * x * x;
        }

        /**
         * Method which should not be registered.
         * @return Nothing.
         */
        public static int no() {
            throw new IllegalStateException();
        }
    }

    /**
     * Sum remote reducer factory.
     */
    private static class SumRemoteReducer implements GridReducer<Map.Entry<String, Integer>, Integer> {
        /** */
        private int sum;

        @Override public boolean collect(Map.Entry<String, Integer> e) {
            sum += e.getValue();

            return true;
        }

        @Override public Integer reduce() {
            return sum;
        }
    }

    /**
     * Sum local reducer factory.
     */
    private static class SumLocalReducer implements GridReducer<Integer, Integer> {
        /** */
        private int sum;

        @Override public boolean collect(Integer e) {
            sum += e;

            return true;
        }

        @Override public Integer reduce() {
            return sum;
        }
    }
}
