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

package org.gridgain.grid.kernal.processors.cache.distributed.near;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.cache.query.*;
import org.gridgain.grid.kernal.processors.cache.*;
import org.gridgain.grid.util.lang.*;
import org.gridgain.grid.util.typedef.*;
import org.jetbrains.annotations.*;

import java.util.*;

import static org.gridgain.grid.cache.GridCacheMode.*;

/**
 * Tests for partitioned cache queries.
 */
public class GridCachePartitionedQuerySelfTest extends GridCacheAbstractQuerySelfTest {
    /** {@inheritDoc} */
    @Override protected int gridCount() {
        return 3;
    }

    /** {@inheritDoc} */
    @Override protected GridCacheMode cacheMode() {
        return PARTITIONED;
    }

    /**
     * JUnit.
     *
     * @throws Exception If failed.
     */
    public void testSingleNodeQuery() throws Exception {
        Person p1 = new Person("Jon", 1500);
        Person p2 = new Person("Jane", 2000);
        Person p3 = new Person("Mike", 1800);
        Person p4 = new Person("Bob", 1900);

        GridCache<UUID, Person> cache0 = grid(0).cache(null);

        cache0.put(p1.id(), p1);
        cache0.put(p2.id(), p2);
        cache0.put(p3.id(), p3);
        cache0.put(p4.id(), p4);

        assertEquals(4, cache0.size());

        GridCacheQuery<Map.Entry<UUID, Person>> qry = cache0.queries().createSqlQuery(Person.class,
            "salary < 2000").projection(grid(0).forLocal());

        // Include backup entries.
        qry.includeBackups(true);

        // In order to get accumulated result from all queried nodes.
        qry.keepAll(true);

        Collection<Map.Entry<UUID, Person>> entries = qry.execute().get();

        assert entries != null;

        info("Queried persons: " + F.viewReadOnly(entries, F.<Person>mapEntry2Value()));

        assertEquals(3, entries.size());

        checkResult(entries, p1, p3, p4);
    }

    /**
     * @throws Exception If failed.
     */
    public void testFieldsQuery() throws Exception {
        Person p1 = new Person("Jon", 1500);
        Person p2 = new Person("Jane", 2000);
        Person p3 = new Person("Mike", 1800);
        Person p4 = new Person("Bob", 1900);

        Grid grid0 = grid(0);

        GridCache<UUID, Person> cache0 = grid0.cache(null);

        cache0.put(p1.id(), p1);
        cache0.put(p2.id(), p2);
        cache0.put(p3.id(), p3);
        cache0.put(p4.id(), p4);

        assertEquals(4, cache0.size());

        // Fields query
        GridCacheQuery<List<?>> qry = cache0.queries().createSqlFieldsQuery("select name from Person where salary > ?").
            projection(grid0);

        Collection<List<?>> res = qry.execute(1600).get();

        assertEquals(3, res.size());

        // Fields query count(*)
        qry = cache0.queries().createSqlFieldsQuery("select count(*) from Person").projection(grid0);

        res = qry.execute().get();

        int cnt = 0;

        for (List<?> row : res)
            cnt += (Long)row.get(0);

        assertEquals(4, cnt);
    }

    /**
     * @throws Exception If failed.
     */
    public void testMultipleNodesQuery() throws Exception {
        Person p1 = new Person("Jon", 1500);
        Person p2 = new Person("Jane", 2000);
        Person p3 = new Person("Mike", 1800);
        Person p4 = new Person("Bob", 1900);

        GridCache<UUID, Person> cache0 = grid(0).cache(null);

        cache0.put(p1.id(), p1);
        cache0.put(p2.id(), p2);
        cache0.put(p3.id(), p3);
        cache0.put(p4.id(), p4);

        assertEquals(4, cache0.size());

        assert grid(0).nodes().size() == gridCount();

        GridCacheQuery<Map.Entry<UUID, Person>> qry = cache0.queries().createSqlQuery(Person.class,
            "salary < 2000");

        // Include backup entries and disable de-duplication.
        qry.includeBackups(true);
        qry.enableDedup(false);

        // In order to get accumulated result from all queried nodes.
        qry.keepAll(true);

        // Execute on full projection, duplicates are expected.
        Collection<Map.Entry<UUID, Person>> entries = qry.execute().get();

        assert entries != null;

        info("Queried entries: " + entries);

        info("Queried persons: " + F.viewReadOnly(entries, F.<Person>mapEntry2Value()));

        // Expect result including backup persons.
        assertEquals(3 * gridCount(), entries.size());

        checkResult(entries, p1, p3, p4);

        // Now do the same filtering but using projection.
        qry = cache0.projection(F.<UUID, Person>cachePrimary()).queries().createSqlQuery(Person.class,
            "salary < 2000");

        qry.keepAll(true);

        entries = qry.execute().get();

        assert entries != null;

        info("Queried persons: " + F.viewReadOnly(entries, F.<Person>mapEntry2Value()));

        // Expect result including backup persons.
        assertEquals(3, entries.size());

        checkResult(entries, p1, p3, p4);
    }

    /**
     * @throws Exception If failed.
     */
    public void testIncludeBackupsAndEnableDedup() throws Exception {
        Person p1 = new Person("Jon", 1500);
        Person p2 = new Person("Jane", 2000);
        Person p3 = new Person("Mike", 1800);
        Person p4 = new Person("Bob", 1900);

        GridCache<UUID, Person> cache0 = grid(0).cache(null);

        cache0.put(p1.id(), p1);
        cache0.put(p2.id(), p2);
        cache0.put(p3.id(), p3);
        cache0.put(p4.id(), p4);

        // Retry several times.
        for (int i = 0; i < 10; i++) {
            GridCacheQuery<Map.Entry<UUID, Person>> qry = cache0.queries().createSqlQuery(Person.class,
                "salary < 2000");

            // Include backup entries and disable de-duplication.
            qry.includeBackups(true);
            qry.enableDedup(false);

            Collection<Map.Entry<UUID, Person>> entries = qry.execute().get();

            info("Entries: " + entries);

            assertEquals(gridCount() * 3, entries.size());

            // Recreate query since we cannot use the old one.
            qry = cache0.queries().createSqlQuery(Person.class, "salary < 2000");

            // Exclude backup entries and enable de-duplication.
            qry.includeBackups(false);
            qry.enableDedup(true);

            entries = qry.execute().get();

            assertEquals(3, entries.size());
        }
    }

    /**
     * @throws Exception If failed.
     */
    @SuppressWarnings("FloatingPointEquality")
    public void testScanReduceQuery() throws Exception {
        GridCache<UUID, Person> c = grid.cache(null);

        Person p1 = new Person("Bob White", 1000);
        Person p2 = new Person("Tom White", 2000);
        Person p3 = new Person("Mike Green", 20000);

        c.put(p1.id(), p1);
        c.put(p2.id(), p2);
        c.put(p3.id(), p3);

        GridCacheQuery<Map.Entry<UUID, Person>> q = c.queries().createScanQuery(new P2<UUID, Person>() {
            @Override public boolean apply(UUID k, Person p) {
                return p.salary() < 20000;
            }
        });

        R1<GridPair<Integer>, Double> locRdc = new R1<GridPair<Integer>, Double>() {
            private double sum;

            private int cnt;

            @Override public boolean collect(GridPair<Integer> p) {
                sum += p.get1();
                cnt += p.get2();

                return true;
            }

            @Override public Double reduce() {
                return sum / cnt;
            }
        };

        Collection<GridPair<Integer>> res = q.execute(new R1<Map.Entry<UUID, Person>, GridPair<Integer>>() {
            private int sum;

            private int cnt;

            @Override public boolean collect(Map.Entry<UUID, Person> e) {
                sum += e.getValue().salary();
                cnt++;

                return true;
            }

            @Override public GridPair<Integer> reduce() {
                return new GridPair<>(sum, cnt);
            }
        }).get();

        assertEquals(1500., F.reduce(res, locRdc));
    }

    /**
     * @throws Exception If failed.
     */
    @SuppressWarnings("FloatingPointEquality")
    public void testSqlReduceQuery() throws Exception {
        GridCache<UUID, Person> c = grid.cache(null);

        Person p1 = new Person("Bob White", 1000);
        Person p2 = new Person("Tom White", 2000);
        Person p3 = new Person("Mike Green", 20000);

        c.put(p1.id(), p1);
        c.put(p2.id(), p2);
        c.put(p3.id(), p3);

        GridCacheQuery<Map.Entry<UUID, Person>> q = c.queries().createSqlQuery(Person.class, "salary < 20000");

        R1<GridPair<Integer>, Double> locRdc = new R1<GridPair<Integer>, Double>() {
            private double sum;

            private int cnt;

            @Override public boolean collect(GridPair<Integer> p) {
                sum += p.get1();
                cnt += p.get2();

                return true;
            }

            @Override public Double reduce() {
                return sum / cnt;
            }
        };

        Collection<GridPair<Integer>> res = q.execute(new R1<Map.Entry<UUID, Person>, GridPair<Integer>>() {
            private int sum;

            private int cnt;

            @Override public boolean collect(Map.Entry<UUID, Person> e) {
                sum += e.getValue().salary();
                cnt++;

                return true;
            }

            @Override public GridPair<Integer> reduce() {
                return new GridPair<>(sum, cnt);
            }
        }).get();

        assert F.reduce(res, locRdc) == 1500;
    }

    /**
     * @throws Exception If failed.
     */
    @SuppressWarnings("FloatingPointEquality")
    public void testLuceneReduceQuery() throws Exception {
        GridCache<UUID, Person> c = grid.cache(null);

        Person p1 = new Person("Bob White", 1000);
        Person p2 = new Person("Tom White", 2000);
        Person p3 = new Person("Mike Green", 20000);

        c.put(p1.id(), p1);
        c.put(p2.id(), p2);
        c.put(p3.id(), p3);

        GridCacheQuery<Map.Entry<UUID, Person>> q = c.queries().createFullTextQuery(Person.class, "White");

        R1<GridPair<Integer>, Double> locRdc = new R1<GridPair<Integer>, Double>() {
            private double sum;

            private int cnt;

            @Override public boolean collect(GridPair<Integer> p) {
                sum += p.get1();
                cnt += p.get2();

                return true;
            }

            @Override public Double reduce() {
                return sum / cnt;
            }
        };

        Collection<GridPair<Integer>> res = q.execute(new R1<Map.Entry<UUID, Person>, GridPair<Integer>>() {
            private int sum;

            private int cnt;

            @Override public boolean collect(Map.Entry<UUID, Person> e) {
                sum += e.getValue().salary();
                cnt++;

                return true;
            }

            @Override public GridPair<Integer> reduce() {
                return new GridPair<>(sum, cnt);
            }
        }).get();

        assert F.reduce(res, locRdc) == 1500;
    }

    /**
     * @throws Exception If failed.
     */
    public void testPaginationGet0() throws Exception {
        int key = 0;

        for (int i = 0; i < gridCount(); i++) {
            int cnt = 0;

            while (true) {
                if (grid(i).cache(null).affinity().mapKeyToNode(key).equals(grid(i).localNode())) {
                    assertTrue(grid(i).cache(null).putx(key, key));

                    cnt++;
                }

                key++;

                if (cnt == (i == 1 ? 2 : 3))
                    break;
            }
        }

        for (int i = 0; i < gridCount(); i++)
            assertEquals(i == 1 ? 2 : 3, grid(i).cache(null).primarySize());

        GridCache<Integer, Integer> cache = grid.cache(null);

        GridCacheQuery<Map.Entry<Integer, Integer>> q = cache.queries().createSqlQuery(Integer.class, "_key >= 0");

        q.pageSize(2);
        q.includeBackups(false);
        q.enableDedup(true);

        Collection<Map.Entry<Integer, Integer>> res = q.execute().get();

        assertEquals(gridCount() * 3 - 1, res.size());
    }

    /**
     * @throws Exception If failed.
     */
    public void testReduceWithPagination() throws Exception {
        GridCache<Integer, Integer> c = grid(0).cache(null);

        for (int i = 0; i < 50; i++)
            assertTrue(c.putx(i, 10));

        GridCacheQuery<Map.Entry<Integer, Integer>> q = c.queries().createSqlQuery(Integer.class, "_key >= 0");

        q.pageSize(10);

        int res = F.sumInt(q.execute(new R1<Map.Entry<Integer, Integer>, Integer>() {
            private int sum;

            @Override public boolean collect(@Nullable Map.Entry<Integer, Integer> e) {
                sum += e.getValue();

                return true;
            }

            @Override public Integer reduce() {
                return sum;
            }
        }).get());

        assertEquals(500, res);
    }

    /**
     * @param entries Queried result.
     * @param persons Persons that should be in the result.
     */
    private void checkResult(Iterable<Map.Entry<UUID, Person>> entries, Person... persons) {
        for (Map.Entry<UUID, Person> entry : entries) {
            assertEquals(entry.getKey(), entry.getValue().id());

            assert F.<Person>asList(persons).contains(entry.getValue());
        }
    }
}
