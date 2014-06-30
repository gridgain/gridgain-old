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
import org.gridgain.testframework.junits.common.*;

import java.io.*;
import java.math.*;
import java.net.*;
import java.sql.*;
import java.util.Date;

import static java.sql.Types.*;
import static org.gridgain.grid.cache.GridCacheMode.*;
import static org.gridgain.grid.cache.GridCacheWriteSynchronizationMode.*;

/**
 * Prepared statement test.
 */
public class GridJdbcPreparedStatementSelfTest extends GridCommonAbstractTest {
    /** IP finder. */
    private static final GridTcpDiscoveryIpFinder IP_FINDER = new GridTcpDiscoveryVmIpFinder(true);

    /** URL. */
    private static final String URL = "jdbc:gridgain://127.0.0.1/";

    /** Connection. */
    private Connection conn;

    /** Statement. */
    private PreparedStatement stmt;

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

        TestObject o = new TestObject(1);

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
        o.dateVal = new Date(1);
        o.timeVal = new Time(1);
        o.tsVal = new Timestamp(1);
        o.urlVal = new URL("http://abc.com/");

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
        conn = DriverManager.getConnection(URL);

        assert conn != null;
        assert !conn.isClosed();
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        if (stmt != null) {
            stmt.close();

            assert stmt.isClosed();
        }

        if (conn != null) {
            conn.close();

            assert conn.isClosed();
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testBoolean() throws Exception {
        stmt = conn.prepareStatement("select * from TestObject where boolVal is not distinct from ?");

        stmt.setBoolean(1, true);

        ResultSet rs = stmt.executeQuery();

        int cnt = 0;

        while (rs.next()) {
            if (cnt == 0)
                assert rs.getInt("id") == 1;

            cnt++;
        }

        assert cnt == 1;

        stmt.setNull(1, BOOLEAN);

        rs = stmt.executeQuery();

        cnt = 0;

        while (rs.next()) {
            if (cnt == 0)
                assert rs.getInt("id") == 2;

            cnt++;
        }

        assert cnt == 1;
    }

    /**
     * @throws Exception If failed.
     */
    public void testByte() throws Exception {
        stmt = conn.prepareStatement("select * from TestObject where byteVal is not distinct from ?");

        stmt.setByte(1, (byte)1);

        ResultSet rs = stmt.executeQuery();

        int cnt = 0;

        while (rs.next()) {
            if (cnt == 0)
                assert rs.getInt("id") == 1;

            cnt++;
        }

        assert cnt == 1;

        stmt.setNull(1, TINYINT);

        rs = stmt.executeQuery();

        cnt = 0;

        while (rs.next()) {
            if (cnt == 0)
                assert rs.getInt("id") == 2;

            cnt++;
        }

        assert cnt == 1;
    }

    /**
     * @throws Exception If failed.
     */
    public void testShort() throws Exception {
        stmt = conn.prepareStatement("select * from TestObject where shortVal is not distinct from ?");

        stmt.setShort(1, (short)1);

        ResultSet rs = stmt.executeQuery();

        int cnt = 0;

        while (rs.next()) {
            if (cnt == 0)
                assert rs.getInt("id") == 1;

            cnt++;
        }

        assert cnt == 1;

        stmt.setNull(1, SMALLINT);

        rs = stmt.executeQuery();

        cnt = 0;

        while (rs.next()) {
            if (cnt == 0)
                assert rs.getInt("id") == 2;

            cnt++;
        }

        assert cnt == 1;
    }

    /**
     * @throws Exception If failed.
     */
    public void testInteger() throws Exception {
        stmt = conn.prepareStatement("select * from TestObject where intVal is not distinct from ?");

        stmt.setInt(1, 1);

        ResultSet rs = stmt.executeQuery();

        int cnt = 0;

        while (rs.next()) {
            if (cnt == 0)
                assert rs.getInt("id") == 1;

            cnt++;
        }

        assert cnt == 1;

        stmt.setNull(1, INTEGER);

        rs = stmt.executeQuery();

        cnt = 0;

        while (rs.next()) {
            if (cnt == 0)
                assert rs.getInt("id") == 2;

            cnt++;
        }

        assert cnt == 1;
    }

    /**
     * @throws Exception If failed.
     */
    public void testLong() throws Exception {
        stmt = conn.prepareStatement("select * from TestObject where longVal is not distinct from ?");

        stmt.setLong(1, 1L);

        ResultSet rs = stmt.executeQuery();

        int cnt = 0;

        while (rs.next()) {
            if (cnt == 0)
                assert rs.getInt("id") == 1;

            cnt++;
        }

        assert cnt == 1;

        stmt.setNull(1, BIGINT);

        rs = stmt.executeQuery();

        cnt = 0;

        while (rs.next()) {
            if (cnt == 0)
                assert rs.getInt("id") == 2;

            cnt++;
        }

        assert cnt == 1;
    }

    /**
     * @throws Exception If failed.
     */
    public void testFloat() throws Exception {
        stmt = conn.prepareStatement("select * from TestObject where floatVal is not distinct from ?");

        stmt.setFloat(1, 1.0f);

        ResultSet rs = stmt.executeQuery();

        int cnt = 0;

        while (rs.next()) {
            if (cnt == 0)
                assert rs.getInt("id") == 1;

            cnt++;
        }

        assert cnt == 1;

        stmt.setNull(1, FLOAT);

        rs = stmt.executeQuery();

        cnt = 0;

        while (rs.next()) {
            if (cnt == 0)
                assert rs.getInt("id") == 2;

            cnt++;
        }

        assert cnt == 1;
    }

    /**
     * @throws Exception If failed.
     */
    public void testDouble() throws Exception {
        stmt = conn.prepareStatement("select * from TestObject where doubleVal is not distinct from ?");

        stmt.setDouble(1, 1.0d);

        ResultSet rs = stmt.executeQuery();

        int cnt = 0;

        while (rs.next()) {
            if (cnt == 0)
                assert rs.getInt("id") == 1;

            cnt++;
        }

        assert cnt == 1;

        stmt.setNull(1, DOUBLE);

        rs = stmt.executeQuery();

        cnt = 0;

        while (rs.next()) {
            if (cnt == 0)
                assert rs.getInt("id") == 2;

            cnt++;
        }

        assert cnt == 1;
    }

    /**
     * @throws Exception If failed.
     */
    public void testBigDecimal() throws Exception {
        stmt = conn.prepareStatement("select * from TestObject where bigVal is not distinct from ?");

        stmt.setBigDecimal(1, new BigDecimal(1));

        ResultSet rs = stmt.executeQuery();

        int cnt = 0;

        while (rs.next()) {
            if (cnt == 0)
                assert rs.getInt("id") == 1;

            cnt++;
        }

        assert cnt == 1;

        stmt.setNull(1, OTHER);

        rs = stmt.executeQuery();

        cnt = 0;

        while (rs.next()) {
            if (cnt == 0)
                assert rs.getInt("id") == 2;

            cnt++;
        }

        assert cnt == 1;
    }

    /**
     * @throws Exception If failed.
     */
    public void testString() throws Exception {
        stmt = conn.prepareStatement("select * from TestObject where strVal is not distinct from ?");

        stmt.setString(1, "str");

        ResultSet rs = stmt.executeQuery();

        int cnt = 0;

        while (rs.next()) {
            if (cnt == 0)
                assert rs.getInt("id") == 1;

            cnt++;
        }

        assert cnt == 1;

        stmt.setNull(1, VARCHAR);

        rs = stmt.executeQuery();

        cnt = 0;

        while (rs.next()) {
            if (cnt == 0)
                assert rs.getInt("id") == 2;

            cnt++;
        }

        assert cnt == 1;
    }

    /**
     * @throws Exception If failed.
     */
    public void testArray() throws Exception {
        stmt = conn.prepareStatement("select * from TestObject where arrVal is not distinct from ?");

        stmt.setBytes(1, new byte[] {1});

        ResultSet rs = stmt.executeQuery();

        int cnt = 0;

        while (rs.next()) {
            if (cnt == 0)
                assert rs.getInt("id") == 1;

            cnt++;
        }

        assert cnt == 1;

        stmt.setNull(1, BINARY);

        rs = stmt.executeQuery();

        cnt = 0;

        while (rs.next()) {
            if (cnt == 0)
                assert rs.getInt("id") == 2;

            cnt++;
        }

        assert cnt == 1;
    }

    /**
     * @throws Exception If failed.
     */
    public void testDate() throws Exception {
        stmt = conn.prepareStatement("select * from TestObject where dateVal is not distinct from ?");

        stmt.setObject(1, new Date(1));

        ResultSet rs = stmt.executeQuery();

        int cnt = 0;

        while (rs.next()) {
            if (cnt == 0)
                assert rs.getInt("id") == 1;

            cnt++;
        }

        assert cnt == 1;

        stmt.setNull(1, DATE);

        rs = stmt.executeQuery();

        cnt = 0;

        while (rs.next()) {
            if (cnt == 0)
                assert rs.getInt("id") == 2;

            cnt++;
        }

        assert cnt == 1;
    }

    /**
     * @throws Exception If failed.
     */
    public void testTime() throws Exception {
        stmt = conn.prepareStatement("select * from TestObject where timeVal is not distinct from ?");

        stmt.setTime(1, new Time(1));

        ResultSet rs = stmt.executeQuery();

        int cnt = 0;

        while (rs.next()) {
            if (cnt == 0)
                assert rs.getInt("id") == 1;

            cnt++;
        }

        assert cnt == 1;

        stmt.setNull(1, TIME);

        rs = stmt.executeQuery();

        cnt = 0;

        while (rs.next()) {
            if (cnt == 0)
                assert rs.getInt("id") == 2;

            cnt++;
        }

        assert cnt == 1;
    }

    /**
     * @throws Exception If failed.
     */
    public void testTimestamp() throws Exception {
        stmt = conn.prepareStatement("select * from TestObject where tsVal is not distinct from ?");

        stmt.setTimestamp(1, new Timestamp(1));

        ResultSet rs = stmt.executeQuery();

        int cnt = 0;

        while (rs.next()) {
            if (cnt == 0)
                assert rs.getInt("id") == 1;

            cnt++;
        }

        assert cnt == 1;

        stmt.setNull(1, TIMESTAMP);

        rs = stmt.executeQuery();

        cnt = 0;

        while (rs.next()) {
            if (cnt == 0)
                assert rs.getInt("id") == 2;

            cnt++;
        }

        assert cnt == 1;
    }

    /**
     * @throws Exception If failed.
     */
    public void testUrl() throws Exception {
        stmt = conn.prepareStatement("select * from TestObject where urlVal is not distinct from ?");

        stmt.setURL(1, new URL("http://abc.com/"));

        ResultSet rs = stmt.executeQuery();

        int cnt = 0;

        while (rs.next()) {
            if (cnt == 0)
                assert rs.getInt("id") == 1;

            cnt++;
        }

        assert cnt == 1;

        stmt.setNull(1, DATALINK);

        rs = stmt.executeQuery();

        cnt = 0;

        while (rs.next()) {
            if (cnt == 0)
                assert rs.getInt("id") == 2;

            cnt++;
        }

        assert cnt == 1;
    }

    /**
     * Test object.
     */
    @SuppressWarnings("UnusedDeclaration")
    private static class TestObject implements Serializable {
        /** */
        @GridCacheQuerySqlField(index = false)
        private final int id;

        /** */
        @GridCacheQuerySqlField
        private Boolean boolVal;

        /** */
        @GridCacheQuerySqlField
        private Byte byteVal;

        /** */
        @GridCacheQuerySqlField
        private Short shortVal;

        /** */
        @GridCacheQuerySqlField
        private Integer intVal;

        /** */
        @GridCacheQuerySqlField
        private Long longVal;

        /** */
        @GridCacheQuerySqlField
        private Float floatVal;

        /** */
        @GridCacheQuerySqlField
        private Double doubleVal;

        /** */
        @GridCacheQuerySqlField
        private BigDecimal bigVal;

        /** */
        @GridCacheQuerySqlField
        private String strVal;

        /** */
        @GridCacheQuerySqlField
        private byte[] arrVal;

        /** */
        @GridCacheQuerySqlField
        private Date dateVal;

        /** */
        @GridCacheQuerySqlField
        private Time timeVal;

        /** */
        @GridCacheQuerySqlField
        private Timestamp tsVal;

        /** */
        @GridCacheQuerySqlField
        private URL urlVal;

        /**
         * @param id ID.
         */
        private TestObject(int id) {
            this.id = id;
        }
    }
}
