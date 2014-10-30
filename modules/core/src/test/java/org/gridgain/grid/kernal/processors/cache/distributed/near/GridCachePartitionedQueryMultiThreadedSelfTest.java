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
import org.gridgain.grid.cache.affinity.consistenthash.*;
import org.gridgain.grid.cache.query.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.grid.util.tostring.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.testframework.*;
import org.gridgain.testframework.junits.common.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import static org.gridgain.grid.cache.GridCacheAtomicityMode.*;
import static org.gridgain.grid.cache.GridCacheMode.*;
import static org.gridgain.grid.cache.GridCacheDistributionMode.*;

/**
 * Tests for partitioned cache queries.
 */
public class GridCachePartitionedQueryMultiThreadedSelfTest extends GridCommonAbstractTest {
    /** */
    private static final boolean TEST_INFO = true;

    /** Number of test grids (nodes). Should not be less than 2. */
    private static final int GRID_CNT = 3;

    /** */
    private static GridTcpDiscoveryIpFinder ipFinder = new GridTcpDiscoveryVmIpFinder(true);

    /** Don't start grid by default. */
    public GridCachePartitionedQueryMultiThreadedSelfTest() {
        super(false);
    }

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration c = super.getConfiguration(gridName);

        GridTcpDiscoverySpi disco = new GridTcpDiscoverySpi();

        disco.setIpFinder(ipFinder);

        c.setDiscoverySpi(disco);

        GridCacheConfiguration cc = defaultCacheConfiguration();

        cc.setCacheMode(PARTITIONED);

        // Query should be executed without ongoing transactions.
        cc.setWriteSynchronizationMode(GridCacheWriteSynchronizationMode.FULL_SYNC);
        cc.setBackups(0);
        cc.setPreloadMode(GridCachePreloadMode.SYNC);
        cc.setAtomicityMode(TRANSACTIONAL);
        cc.setDistributionMode(NEAR_PARTITIONED);

        c.setCacheConfiguration(cc);

        return c;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        assert GRID_CNT >= 2 : "Constant GRID_CNT must be greater than or equal to 2.";

        startGridsMultiThreaded(GRID_CNT);
    }

    /** {@inheritDoc} */
    @Override protected void afterTestsStopped() throws Exception {
        stopAllGrids();
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        super.afterTest();

        // Clean up all caches.
        for (int i = 0; i < GRID_CNT; i++)
            grid(i).cache(null).removeAll(F.<GridCacheEntry<Object, Object>>alwaysTrue());
    }

    /** {@inheritDoc} */
    @Override protected void info(String msg) {
        if (TEST_INFO)
            super.info(msg);
    }

    /**
     * JUnit.
     *
     * @throws Exception If failed.
     */
    @SuppressWarnings({"TooBroadScope"})
    public void testLuceneAndSqlMultithreaded() throws Exception {
        // ---------- Test parameters ---------- //
        int luceneThreads = 10;
        int sqlThreads = 10;
        long duration = 10 * 1000;
        final int logMod = 100;

        final Person p1 = new Person("Jon", 1500, "Master");
        final Person p2 = new Person("Jane", 2000, "Master");
        final Person p3 = new Person("Mike", 1800, "Bachelor");
        final Person p4 = new Person("Bob", 1900, "Bachelor");

        final GridCache<UUID, Person> cache0 = grid(0).cache(null);

        cache0.put(p1.id(), p1);
        cache0.put(p2.id(), p2);
        cache0.put(p3.id(), p3);
        cache0.put(p4.id(), p4);

        assertEquals(4, cache0.size());

        assert grid(0).nodes().size() == GRID_CNT;

        final AtomicBoolean done = new AtomicBoolean();

        final AtomicLong luceneCnt = new AtomicLong();

        // Start lucene query threads.
        GridFuture<?> futLucene = GridTestUtils.runMultiThreadedAsync(new CAX() {
            @Override public void applyx() throws GridException {
                while (!done.get()) {
                    GridCacheQuery<Map.Entry<UUID, Person>> masters = cache0.queries().createFullTextQuery(
                        Person.class, "Master");

                    Collection<Map.Entry<UUID, Person>> entries = masters.execute().get();

                    checkResult(entries, p1, p2);

                    long cnt = luceneCnt.incrementAndGet();

                    if (cnt % logMod == 0)
                        info("Executed LUCENE queries: " + cnt);
                }
            }
        }, luceneThreads, "LUCENE-THREAD");

        final AtomicLong sqlCnt = new AtomicLong();

        // Start sql query threads.
        GridFuture<?> futSql = GridTestUtils.runMultiThreadedAsync(new CAX() {
            @Override public void applyx() throws GridException {
                while (!done.get()) {
                    GridCacheQuery<Map.Entry<UUID, Person>> bachelors =
                        cache0.queries().createSqlQuery(Person.class, "degree = 'Bachelor'");

                    Collection<Map.Entry<UUID, Person>> entries = bachelors.execute().get();

                    checkResult(entries, p3, p4);

                    long cnt = sqlCnt.incrementAndGet();

                    if (cnt % logMod == 0)
                        info("Executed SQL queries: " + cnt);
                }
            }
        }, sqlThreads, "SQL-THREAD");

        Thread.sleep(duration);

        done.set(true);

        futLucene.get();
        futSql.get();
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

    /** Test class. */
    private static class Person implements Externalizable {
        /** */
        @GridToStringExclude
        private UUID id = UUID.randomUUID();

        /** */
        @GridCacheQuerySqlField
        private String name;

        /** */
        @GridCacheQuerySqlField
        private int salary;

        /** */
        @GridCacheQuerySqlField
        @GridCacheQueryTextField
        private String degree;

        /** Required by {@link Externalizable}. */
        public Person() {
            // No-op.
        }

        /**
         * @param name Name.
         * @param salary Salary.
         * @param degree Degree.
         */
        Person(String name, int salary, String degree) {
            assert name != null;
            assert salary > 0;
            assert degree != null;

            this.name = name;
            this.salary = salary;
            this.degree = degree;
        }

        /** @return Id. */
        UUID id() {
            return id;
        }

        /** @return Name. */
        String name() {
            return name;
        }

        /** @return Salary. */
        double salary() {
            return salary;
        }

        /** @return Degree. */
        String degree() {
            return degree;
        }

        /** {@inheritDoc} */
        @Override public void writeExternal(ObjectOutput out) throws IOException {
            U.writeUuid(out, id);
            U.writeString(out, name);
            out.writeInt(salary);
            U.writeString(out, degree);
        }

        /** {@inheritDoc} */
        @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            id = U.readUuid(in);
            name = U.readString(in);
            salary = in.readInt();
            degree = U.readString(in);
        }

        /** {@inheritDoc} */
        @Override public int hashCode() {
            return id.hashCode() + 31 * name.hashCode() + 31 * 31 * salary;
        }

        /** {@inheritDoc} */
        @Override public boolean equals(Object obj) {
            if (obj == this)
                return true;

            if (!(obj instanceof Person))
                return false;

            Person that = (Person)obj;

            return that.id.equals(id) && that.name.equals(name) && that.salary == salary && that.degree.equals(degree);
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(Person.class, this);
        }
    }
}
