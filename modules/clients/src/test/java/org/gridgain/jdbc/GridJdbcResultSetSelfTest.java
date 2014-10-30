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

package org.gridgain.jdbc;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.cache.query.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.testframework.*;
import org.gridgain.testframework.junits.common.*;

import java.io.*;
import java.math.*;
import java.net.*;
import java.sql.Date;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

import static org.gridgain.grid.cache.GridCacheMode.*;
import static org.gridgain.grid.cache.GridCacheWriteSynchronizationMode.*;

/**
 * Result set test.
 */
@SuppressWarnings("FloatingPointEquality")
public class GridJdbcResultSetSelfTest extends GridCommonAbstractTest {
    /** IP finder. */
    private static final GridTcpDiscoveryIpFinder IP_FINDER = new GridTcpDiscoveryVmIpFinder(true);

    /** URL. */
    private static final String URL = "jdbc:gridgain://127.0.0.1/";

    /** SQL query. */
    private static final String SQL =
        "select id, boolVal, byteVal, shortVal, intVal, longVal, floatVal, " +
            "doubleVal, bigVal, strVal, arrVal, dateVal, timeVal, tsVal, urlVal, f1, f2, f3, _val " +
            "from TestObject where id = 1";

    /** Statement. */
    private Statement stmt;

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        GridCacheConfiguration cache = defaultCacheConfiguration();

        cache.setCacheMode(PARTITIONED);
        cache.setBackups(1);
        cache.setWriteSynchronizationMode(FULL_SYNC);

        cfg.setCacheConfiguration(cache);

        GridTcpDiscoverySpi disco = new GridTcpDiscoverySpi();

        disco.setIpFinder(IP_FINDER);

        cfg.setDiscoverySpi(disco);

        cfg.setRestEnabled(true);

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        startGridsMultiThreaded(3);

        GridCache<Integer, TestObject> cache = grid(0).cache(null);

        assert cache != null;

        TestObject o = createObjectWithData(1);

        cache.put(1, o);
        cache.put(2, new TestObject(2));

        Class.forName("org.gridgain.jdbc.GridJdbcDriver");
    }

    /** {@inheritDoc} */
    @Override protected void afterTestsStopped() throws Exception {
        stopAllGrids();
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        stmt = DriverManager.getConnection(URL).createStatement();

        assert stmt != null;
        assert !stmt.isClosed();
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        if (stmt != null) {
            stmt.getConnection().close();
            stmt.close();

            assert stmt.isClosed();
        }
    }

    /**
     * @param id ID.
     * @return Object.
     * @throws MalformedURLException If URL in incorrect.
     */
    @SuppressWarnings("deprecation")
    private TestObject createObjectWithData(int id) throws MalformedURLException {
        TestObject o = new TestObject(id);

        o.boolVal = true;
        o.byteVal = 1;
        o.shortVal = 1;
        o.intVal = 1;
        o.longVal = 1L;
        o.floatVal = 1.0f;
        o.doubleVal = 1.0d;
        o.bigVal = new BigDecimal(1);
        o.strVal = "str";
        o.arrVal = new byte[] {1};
        o.dateVal = new Date(1, 1, 1);
        o.timeVal = new Time(1, 1, 1);
        o.tsVal = new Timestamp(1);
        o.urlVal = new URL("http://abc.com/");

        return o;
    }

    /**
     * @throws Exception If failed.
     */
    public void testBoolean() throws Exception {
        ResultSet rs = stmt.executeQuery(SQL);

        int cnt = 0;

        while (rs.next()) {
            if (cnt == 0) {
                assert rs.getBoolean("boolVal");
                assert rs.getBoolean(2);
            }

            cnt++;
        }

        assert cnt == 1;
    }

    /**
     * @throws Exception If failed.
     */
    public void testByte() throws Exception {
        ResultSet rs = stmt.executeQuery(SQL);

        int cnt = 0;

        while (rs.next()) {
            if (cnt == 0) {
                assert rs.getByte("byteVal") == 1;
                assert rs.getByte(3) == 1;
            }

            cnt++;
        }

        assert cnt == 1;
    }

    /**
     * @throws Exception If failed.
     */
    public void testShort() throws Exception {
        ResultSet rs = stmt.executeQuery(SQL);

        int cnt = 0;

        while (rs.next()) {
            if (cnt == 0) {
                assert rs.getShort("shortVal") == 1;
                assert rs.getShort(4) == 1;
            }

            cnt++;
        }

        assert cnt == 1;
    }

    /**
     * @throws Exception If failed.
     */
    public void testInteger() throws Exception {
        ResultSet rs = stmt.executeQuery(SQL);

        int cnt = 0;

        while (rs.next()) {
            if (cnt == 0) {
                assert rs.getInt("intVal") == 1;
                assert rs.getInt(5) == 1;
            }

            cnt++;
        }

        assert cnt == 1;
    }

    /**
     * @throws Exception If failed.
     */
    public void testLong() throws Exception {
        ResultSet rs = stmt.executeQuery(SQL);

        int cnt = 0;

        while (rs.next()) {
            if (cnt == 0) {
                assert rs.getLong("longVal") == 1;
                assert rs.getLong(6) == 1;
            }

            cnt++;
        }

        assert cnt == 1;
    }

    /**
     * @throws Exception If failed.
     */
    public void testFloat() throws Exception {
        ResultSet rs = stmt.executeQuery(SQL);

        int cnt = 0;

        while (rs.next()) {
            if (cnt == 0) {
                assert rs.getFloat("floatVal") == 1.0;
                assert rs.getFloat(7) == 1.0;
            }

            cnt++;
        }

        assert cnt == 1;
    }

    /**
     * @throws Exception If failed.
     */
    public void testDouble() throws Exception {
        ResultSet rs = stmt.executeQuery(SQL);

        int cnt = 0;

        while (rs.next()) {
            if (cnt == 0) {
                assert rs.getDouble("doubleVal") == 1.0;
                assert rs.getDouble(8) == 1.0;
            }

            cnt++;
        }

        assert cnt == 1;
    }

    /**
     * @throws Exception If failed.
     */
    public void testBigDecimal() throws Exception {
        ResultSet rs = stmt.executeQuery(SQL);

        int cnt = 0;

        while (rs.next()) {
            if (cnt == 0) {
                assert rs.getBigDecimal("bigVal").intValue() == 1;
                assert rs.getBigDecimal(9).intValue() == 1;
            }

            cnt++;
        }

        assert cnt == 1;
    }

    /**
     * @throws Exception If failed.
     */
    public void testString() throws Exception {
        ResultSet rs = stmt.executeQuery(SQL);

        int cnt = 0;

        while (rs.next()) {
            if (cnt == 0) {
                assert "str".equals(rs.getString("strVal"));
                assert "str".equals(rs.getString(10));
            }

            cnt++;
        }

        assert cnt == 1;
    }

    /**
     * @throws Exception If failed.
     */
    public void testArray() throws Exception {
        ResultSet rs = stmt.executeQuery(SQL);

        int cnt = 0;

        while (rs.next()) {
            if (cnt == 0) {
                assert Arrays.equals(rs.getBytes("arrVal"), new byte[] {1});
                assert Arrays.equals(rs.getBytes(11), new byte[] {1});
            }

            cnt++;
        }

        assert cnt == 1;
    }

    /**
     * @throws Exception If failed.
     */
    @SuppressWarnings("deprecation")
    public void testDate() throws Exception {
        ResultSet rs = stmt.executeQuery(SQL);

        int cnt = 0;

        while (rs.next()) {
            if (cnt == 0) {
                assert rs.getDate("dateVal").equals(new Date(1, 1, 1));
                assert rs.getDate(12).equals(new Date(1, 1, 1));
            }

            cnt++;
        }

        assert cnt == 1;
    }

    /**
     * @throws Exception If failed.
     */
    @SuppressWarnings("deprecation")
    public void testTime() throws Exception {
        ResultSet rs = stmt.executeQuery(SQL);

        int cnt = 0;

        while (rs.next()) {
            if (cnt == 0) {
                assert rs.getTime("timeVal").equals(new Time(1, 1, 1));
                assert rs.getTime(13).equals(new Time(1, 1, 1));
            }

            cnt++;
        }

        assert cnt == 1;
    }

    /**
     * @throws Exception If failed.
     */
    public void testTimestamp() throws Exception {
        ResultSet rs = stmt.executeQuery(SQL);

        int cnt = 0;

        while (rs.next()) {
            if (cnt == 0) {
                assert rs.getTimestamp("tsVal").getTime() == 1;
                assert rs.getTimestamp(14).getTime() == 1;
            }

            cnt++;
        }

        assert cnt == 1;
    }

    /**
     * @throws Exception If failed.
     */
    public void testUrl() throws Exception {
        ResultSet rs = stmt.executeQuery(SQL);

        int cnt = 0;

        while (rs.next()) {
            if (cnt == 0) {
                assert "http://abc.com/".equals(rs.getURL("urlVal").toString());
                assert "http://abc.com/".equals(rs.getURL(15).toString());
            }

            cnt++;
        }

        assert cnt == 1;
    }

    /**
     * @throws Exception If failed.
     */
    public void testObject() throws Exception {
        ResultSet rs = stmt.executeQuery(SQL);

        TestObjectField f1 = new TestObjectField(100, "AAAA");
        TestObjectField f2 = new TestObjectField(500, "BBBB");

        TestObject o = createObjectWithData(1);

        assertTrue(rs.next());

        assertEquals(f1.toString(), rs.getObject("f1"));
        assertEquals(f1.toString(), rs.getObject(16));

        assertEquals(f2.toString(), rs.getObject("f2"));
        assertEquals(f2.toString(), rs.getObject(17));

        assertNull(rs.getObject("f3"));
        assertTrue(rs.wasNull());
        assertNull(rs.getObject(18));
        assertTrue(rs.wasNull());

        assertEquals(o.toString(), rs.getObject("_val"));
        assertEquals(o.toString(), rs.getObject(19));

        assertFalse(rs.next());
    }


    /**
     * @throws Exception If failed.
     */
    public void testNavigation() throws Exception {
        ResultSet rs = stmt.executeQuery("select * from TestObject where id > 0");

        assert rs.isBeforeFirst();
        assert !rs.isAfterLast();
        assert !rs.isFirst();
        assert !rs.isLast();
        assert rs.getRow() == 0;

        assert rs.next();

        assert !rs.isBeforeFirst();
        assert !rs.isAfterLast();
        assert rs.isFirst();
        assert !rs.isLast();
        assert rs.getRow() == 1;

        assert rs.next();

        assert !rs.isBeforeFirst();
        assert !rs.isAfterLast();
        assert !rs.isFirst();
        assert rs.isLast();
        assert rs.getRow() == 2;

        assert !rs.next();

        assert !rs.isBeforeFirst();
        assert rs.isAfterLast();
        assert !rs.isFirst();
        assert !rs.isLast();
        assert rs.getRow() == 0;
    }

    /**
     * @throws Exception If failed.
     */
    public void testFindColumn() throws Exception {
        final ResultSet rs = stmt.executeQuery(SQL);

        assert rs != null;
        assert rs.next();

        assert rs.findColumn("id") == 1;

        GridTestUtils.assertThrows(
            log,
            new Callable<Object>() {
                @Override public Object call() throws Exception {
                    rs.findColumn("wrong");

                    return null;
                }
            },
            SQLException.class,
            "Column not found: wrong"
        );
    }

    /**
     * Test object.
     */
    @SuppressWarnings("UnusedDeclaration")
    private static class TestObject implements Serializable {
        /** */
        @GridCacheQuerySqlField
        private final int id;

        /** */
        @GridCacheQuerySqlField(index = false)
        private Boolean boolVal;

        /** */
        @GridCacheQuerySqlField(index = false)
        private Byte byteVal;

        /** */
        @GridCacheQuerySqlField(index = false)
        private Short shortVal;

        /** */
        @GridCacheQuerySqlField(index = false)
        private Integer intVal;

        /** */
        @GridCacheQuerySqlField(index = false)
        private Long longVal;

        /** */
        @GridCacheQuerySqlField(index = false)
        private Float floatVal;

        /** */
        @GridCacheQuerySqlField(index = false)
        private Double doubleVal;

        /** */
        @GridCacheQuerySqlField(index = false)
        private BigDecimal bigVal;

        /** */
        @GridCacheQuerySqlField(index = false)
        private String strVal;

        /** */
        @GridCacheQuerySqlField(index = false)
        private byte[] arrVal;

        /** */
        @GridCacheQuerySqlField(index = false)
        private Date dateVal;

        /** */
        @GridCacheQuerySqlField(index = false)
        private Time timeVal;

        /** */
        @GridCacheQuerySqlField(index = false)
        private Timestamp tsVal;

        /** */
        @GridCacheQuerySqlField(index = false)
        private URL urlVal;

        /** */
        @GridCacheQuerySqlField(index = false)
        private TestObjectField f1 = new TestObjectField(100, "AAAA");

        /** */
        @GridCacheQuerySqlField(index = false)
        private TestObjectField f2 = new TestObjectField(500, "BBBB");

        /** */
        @GridCacheQuerySqlField(index = false)
        private TestObjectField f3;

        /**
         * @param id ID.
         */
        private TestObject(int id) {
            this.id = id;
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(TestObject.class, this);
        }

        /** {@inheritDoc} */
        @SuppressWarnings({"BigDecimalEquals", "EqualsHashCodeCalledOnUrl", "RedundantIfStatement"})
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            TestObject that = (TestObject)o;

            if (id != that.id) return false;
            if (!Arrays.equals(arrVal, that.arrVal)) return false;
            if (bigVal != null ? !bigVal.equals(that.bigVal) : that.bigVal != null) return false;
            if (boolVal != null ? !boolVal.equals(that.boolVal) : that.boolVal != null) return false;
            if (byteVal != null ? !byteVal.equals(that.byteVal) : that.byteVal != null) return false;
            if (dateVal != null ? !dateVal.equals(that.dateVal) : that.dateVal != null) return false;
            if (doubleVal != null ? !doubleVal.equals(that.doubleVal) : that.doubleVal != null) return false;
            if (f1 != null ? !f1.equals(that.f1) : that.f1 != null) return false;
            if (f2 != null ? !f2.equals(that.f2) : that.f2 != null) return false;
            if (f3 != null ? !f3.equals(that.f3) : that.f3 != null) return false;
            if (floatVal != null ? !floatVal.equals(that.floatVal) : that.floatVal != null) return false;
            if (intVal != null ? !intVal.equals(that.intVal) : that.intVal != null) return false;
            if (longVal != null ? !longVal.equals(that.longVal) : that.longVal != null) return false;
            if (shortVal != null ? !shortVal.equals(that.shortVal) : that.shortVal != null) return false;
            if (strVal != null ? !strVal.equals(that.strVal) : that.strVal != null) return false;
            if (timeVal != null ? !timeVal.equals(that.timeVal) : that.timeVal != null) return false;
            if (tsVal != null ? !tsVal.equals(that.tsVal) : that.tsVal != null) return false;
            if (urlVal != null ? !urlVal.equals(that.urlVal) : that.urlVal != null) return false;

            return true;
        }

        /** {@inheritDoc} */
        @SuppressWarnings("EqualsHashCodeCalledOnUrl")
        @Override public int hashCode() {
            int res = id;

            res = 31 * res + (boolVal != null ? boolVal.hashCode() : 0);
            res = 31 * res + (byteVal != null ? byteVal.hashCode() : 0);
            res = 31 * res + (shortVal != null ? shortVal.hashCode() : 0);
            res = 31 * res + (intVal != null ? intVal.hashCode() : 0);
            res = 31 * res + (longVal != null ? longVal.hashCode() : 0);
            res = 31 * res + (floatVal != null ? floatVal.hashCode() : 0);
            res = 31 * res + (doubleVal != null ? doubleVal.hashCode() : 0);
            res = 31 * res + (bigVal != null ? bigVal.hashCode() : 0);
            res = 31 * res + (strVal != null ? strVal.hashCode() : 0);
            res = 31 * res + (arrVal != null ? Arrays.hashCode(arrVal) : 0);
            res = 31 * res + (dateVal != null ? dateVal.hashCode() : 0);
            res = 31 * res + (timeVal != null ? timeVal.hashCode() : 0);
            res = 31 * res + (tsVal != null ? tsVal.hashCode() : 0);
            res = 31 * res + (urlVal != null ? urlVal.hashCode() : 0);
            res = 31 * res + (f1 != null ? f1.hashCode() : 0);
            res = 31 * res + (f2 != null ? f2.hashCode() : 0);
            res = 31 * res + (f3 != null ? f3.hashCode() : 0);

            return res;
        }
    }

    /**
     * Test object field.
     */
    @SuppressWarnings("PackageVisibleField")
    private static class TestObjectField implements Serializable {
        /** */
        final int a;

        /** */
        final String b;

        /**
         * @param a A.
         * @param b B.
         */
        private TestObjectField(int a, String b) {
            this.a = a;
            this.b = b;
        }

        /** {@inheritDoc} */
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            TestObjectField that = (TestObjectField)o;

            return a == that.a && !(b != null ? !b.equals(that.b) : that.b != null);
        }

        /** {@inheritDoc} */
        @Override public int hashCode() {
            int res = a;

            res = 31 * res + (b != null ? b.hashCode() : 0);

            return res;
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(TestObjectField.class, this);
        }
    }
}
