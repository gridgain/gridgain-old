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

package org.gridgain.grid.spi.indexing.h2;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.query.*;
import org.gridgain.grid.kernal.processors.cache.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.marshaller.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.indexing.*;
import org.gridgain.grid.spi.indexing.h2.opt.*;
import org.gridgain.grid.spi.swapspace.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.offheap.unsafe.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.h2.api.*;
import org.h2.command.*;
import org.h2.constant.*;
import org.h2.index.*;
import org.h2.jdbc.*;
import org.h2.message.*;
import org.h2.mvstore.cache.*;
import org.h2.server.web.*;
import org.h2.table.*;
import org.h2.tools.*;
import org.h2.util.*;
import org.h2.value.*;
import org.jdk8.backport.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.lang.reflect.*;
import java.math.*;
import java.sql.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;

import static org.gridgain.grid.spi.indexing.GridIndexType.*;
import static org.gridgain.grid.spi.indexing.h2.opt.GridH2AbstractKeyValueRow.*;
import static org.h2.result.SortOrder.*;

/**
 * Indexing SPI implementation based on H2 database engine. In this implementation main query language is SQL,
 * fulltext indexing can be performed using Lucene or using embedded H2 mechanism. For each registered space
 * the SPI will create respective schema, for default space (where space name is null) schema
 * with name {@code PUBLIC} will be used. To avoid name conflicts user should not explicitly name
 * a schema {@code PUBLIC}.
 * <p>
 * For each registered {@link GridIndexingTypeDescriptor} this SPI will create respective SQL table with
 * {@code '_key'} and {@code '_val'} fields for key and value, and fields from
 * {@link GridIndexingTypeDescriptor#keyFields()} and {@link GridIndexingTypeDescriptor#valueFields()}.
 * For each table it will create indexes declared in {@link GridIndexingTypeDescriptor#indexes()}.
 * <p>
 * Note that you can monitor longer queries by setting {@link #setLongQueryExplain(boolean)} to {@code true}.
 * In this case a warning and execution plan are printed out if query exceeds certain time threshold. The
 * time threshold for long queries is configured via {@link #setLongQueryExecutionTimeout(long)} parameter.
 * <h1 class="header">Configuration</h1>
 * <h2 class="header">Mandatory</h2>
 * This SPI has no mandatory configuration parameters.
 * <h2 class="header">Optional</h2>
 * The following configuration parameters are optional:
 * <ul>
 * <li>Whether SPI will convert key type to SQL type or store keys in binary form
 * (see {@link #setDefaultIndexFixedTyping(boolean)})</li>
 * <li>Whether SPI will create tables for each primitive key-value pair
 * (see {@link #setDefaultIndexPrimitiveKey(boolean)})</li>
 * <li>Whether SPI will create indexes for each primitive value
 * (see {@link #setDefaultIndexPrimitiveValue(boolean)})</li>
 * <li>Search path for SQL objects (see {@link #setSearchPath(String...)})</li>
 * <li>Off-heap memory (see {@link #setMaxOffHeapMemory(long)})</li>
 * <li>Deserialized off-heap rows cache size (see {@link #setMaxOffheapRowsCacheSize(int)})</li>
 * <li>Name (see {@link #setName(String)})</li>
 * <li>SPI will issue a warning if query execution takes longer than specified period in milliseconds (see
 * {@link #setLongQueryExecutionTimeout(long)})</li>
 * <li>Whether SPI will print execution plan for slow queries (see {@link #setLongQueryExplain(boolean)})</li>
 * <li>Classes containing user defined functions (see {@link #setIndexCustomFunctionClasses(Class[])})</li>
 * <li>Per-space configurations (see {@link #setSpaceConfigurations(GridH2IndexingSpaceConfiguration...)})</li>
 * </ul>
 * <h1 class="header">Some important defaults.</h1>
 * <ul>
 *     <li>All the data will be kept in memory</li>
 *     <li>Primitive types will not be indexed (e.g. java types which can be directly converted to SQL types)</li>
 *     <li>
 *         Key types will be converted to SQL types, so it is impossible to store one value type with
 *         different key types
 *     </li>
 * </ul>
 * Here is a Java example on how to configure grid with {@code GridH2IndexingSpi}.
 * <pre name="code" class="java">
 * GridH2IndexingSpi spi = new GridH2IndexingSpi();
 *
 * // Set SPI name.
 * spi.setName(name);
 *
 * spi.setDefaultIndexPrimitiveKey(true);
 * spi.setDefaultIndexPrimitiveValue(true);
 *
 * GridConfiguration cfg = new GridConfiguration();
 *
 * // Overrides default indexing SPI.
 * cfg.setIndexingSpi(spi);
 *
 * // Starts grid.
 * G.start(cfg);
 * </pre>
 * Here is an example of how to configure {@code GridH2IndexingSpi} from Spring XML configuration file.
 * <pre name="code" class="xml">
 * &lt;property name=&quot;indexingSpi&quot;&gt;
 *     &lt;list&gt;
 *         &lt;bean class=&quot;org.gridgain.grid.spi.indexing.h2.GridH2IndexingSpi&quot;&gt;
 *             &lt;!-- Index primitives. --&gt;
 *             &lt;property name=&quot;defaultIndexPrimitiveKey&quot; value=&quot;true&quot;/&gt;
 *
 *             &lt;!-- Allow different key types for one value type. --&gt;
 *             &lt;property name=&quot;defaultIndexFixedTyping&quot; value=&quot;false&quot;/&gt;
 *         &lt;/bean&gt;
 *     &lt;/list&gt;
 * &lt;/property&gt;
 * </pre>
 * <p>
 * <img src="http://www.gridgain.com/images/spring-small.png">
 * <br>
 * For information about Spring framework visit <a href="http://www.springframework.org/">www.springframework.org</a>
 * @see GridIndexingSpi
 */
@GridSpiMultipleInstancesSupport(true)
@SuppressWarnings({"UnnecessaryFullyQualifiedName", "NonFinalStaticVariableUsedInClassInitialization"})
public class GridH2IndexingSpi extends GridSpiAdapter implements GridIndexingSpi, GridH2IndexingSpiMBean {
    /** Default query execution time interpreted as long query (3 seconds). */
    public static final long DFLT_LONG_QRY_EXEC_TIMEOUT = 3000;

    /** Default Index write lock wait time in milliseconds. */
    private static final long DFLT_IDX_WRITE_LOCK_WAIT_TIME = 100;

    /** Default DB name. */
    private static final String DFLT_DB_NAME = "gridgain_indexes";

    /** Default DB options. */
    private static final String DFLT_DB_OPTIONS = ";LOCK_MODE=3;MULTI_THREADED=1;DB_CLOSE_ON_EXIT=FALSE" +
        ";DEFAULT_LOCK_TIMEOUT=10000";

    /** Options for optimized mode to work properly. */
    private static final String OPTIMIZED_DB_OPTIONS = ";OPTIMIZE_REUSE_RESULTS=0;QUERY_CACHE_SIZE=0;" +
        "RECOMPILE_ALWAYS=1;MAX_OPERATION_MEMORY=0";

    /** Field name for key. */
    public static final String KEY_FIELD_NAME = "_key";

    /** Field name for value. */
    public static final String VAL_FIELD_NAME = "_val";

    /** Field name for string representation of value. */
    public static final String VAL_STR_FIELD_NAME = "_gg_val_str__";

    /** Field name for value version. */
    public static final String VER_FIELD_NAME = "_gg_ver__";

    /** Field name for value expiration time. */
    public static final String EXPIRATION_TIME_FIELD_NAME = "_gg_expires__";

    /** */
    private static final GridIndexingQueryFilter[] EMPTY_FILTER = new GridIndexingQueryFilter[0];

    /** */
    private static final Field COMMAND_FIELD;

    /**
     * Command in H2 prepared statement.
     */
    static {
        try {
            COMMAND_FIELD = JdbcPreparedStatement.class.getDeclaredField("command");

            COMMAND_FIELD.setAccessible(true);
        }
        catch (NoSuchFieldException e) {
            throw new IllegalStateException("Check H2 version in classpath.", e);
        }
    }

    /** */
    private static final ThreadLocal<GridH2IndexingSpi> localSpi = new ThreadLocal<>();

    /** */
    private Class<?>[] idxCustomFuncClss;

    /** */
    private String[] searchPath;

    /** */
    private volatile String cachedSearchPathCmd;

    /** */
    private String initScriptPath;

    /** */
    private boolean dfltIdxPrimitiveKey;

    /** */
    private boolean dfltIdxPrimitiveVal;

    /** */
    private boolean dfltIdxFixedTyping = true;

    /** */
    private long maxOffHeapMemory = -1;

    /** */
    private long longQryExecTimeout = DFLT_LONG_QRY_EXEC_TIMEOUT;

    /** */
    private long idxWriteLockWaitTime = DFLT_IDX_WRITE_LOCK_WAIT_TIME;

    /** */
    private boolean longQryExplain;

    /** Cache for deserialized offheap rows. */
    private CacheLongKeyLIRS<GridH2KeyValueRowOffheap> rowCache = new CacheLongKeyLIRS<>(32 * 1024, 1, 128, 256);

    /** */
    private Map<String, GridH2IndexingSpaceConfiguration> spaceCfgs =
        new LinkedHashMap<>();

    /** Logger. */
    @GridLoggerResource
    private GridLogger log;

    /** Node ID. */
    @GridLocalNodeIdResource
    private UUID nodeId;

    /** */
    @GridMarshallerResource
    private GridMarshaller gridMarshaller;

    /** */
    private final CountDownLatch ctxInitLatch = new CountDownLatch(1);

    /** Marshaller. */
    private GridIndexingMarshaller marshaller;

    /** */
    private GridUnsafeMemory offheap;

    /** */
    private final Collection<String> schemaNames = new GridConcurrentHashSet<>();

    /** Collection of schemaNames and registered tables. */
    private final ConcurrentMap<String, Schema> schemas = new ConcurrentHashMap8<>();

    /** */
    private String dbUrl = "jdbc:h2:mem:";

    /** */
    private final Collection<Connection> conns = Collections.synchronizedCollection(new ArrayList<Connection>());

    /** */
    private ThreadLocal<ConnectionWrapper> connCache = new ThreadLocal<ConnectionWrapper>() {
        @Nullable @Override public ConnectionWrapper get() {
            ConnectionWrapper c = super.get();

            boolean reconnect = true;

            try {
                reconnect = c == null || c.connection().isClosed();
            }
            catch (SQLException e) {
                U.warn(log, "Failed to check connection status.", e);
            }

            if (reconnect) {
                c = initialValue();

                set(c);
            }

            return c;
        }

        @Nullable @Override protected ConnectionWrapper initialValue() {
            Connection c = null;

            try {
                c = DriverManager.getConnection(dbUrl);

                if (!F.isEmpty(searchPath)) {
                    try (Statement s = c.createStatement()) {
                        String cmd = cachedSearchPathCmd;

                        if (cmd == null) {
                            SB b = new SB("SET SCHEMA_SEARCH_PATH ");

                            for (int i = 0; i < searchPath.length; i++) {
                                if (i != 0)
                                    b.a(',');

                                b.a('"').a(schema(searchPath[i])).a('"');
                            }

                            cachedSearchPathCmd = cmd = b.toString();
                        }

                        s.executeUpdate(cmd);
                    }
                }

                conns.add(c);

                return new ConnectionWrapper(c);
            }
            catch (SQLException e) {
                U.close(c, log);

                throw new GridRuntimeException("Failed to initialize DB connection: " + dbUrl, e);
            }
        }
    };

    /**
     * Gets DB connection.
     *
     * @param schema Whether to set schema for connection or not.
     * @return DB connection.
     * @throws GridSpiException In case of error.
     */
    private Connection connectionForThread(@Nullable String schema) throws GridSpiException {
        ConnectionWrapper c = connCache.get();

        if (c == null)
            throw new GridSpiException("Failed to get DB connection for thread (check log for details).");

        if (schema != null && !F.eq(c.schema(), schema)) {
            Statement stmt = null;

            try {
                stmt = c.connection().createStatement();

                stmt.executeUpdate("SET SCHEMA \"" + schema + '"');

                if (log.isDebugEnabled())
                    log.debug("Initialized H2 schema for queries on space: " + schema);

                c.schema(schema);
            }
            catch (SQLException e) {
                throw new GridSpiException("Failed to set schema for DB connection for thread [schema=" +
                    schema + "]", e);
            }
            finally {
                U.close(stmt, log);
            }
        }

        return c.connection();
    }

    /**
     * Creates DB schema if it has not been created yet.
     *
     * @param schema Schema name.
     * @throws GridSpiException If failed to create db schema.
     */
    private void createSchemaIfAbsent(String schema) throws GridSpiException {
        executeStatement("CREATE SCHEMA IF NOT EXISTS \"" + schema + '"');

        if (log.isDebugEnabled())
            log.debug("Created H2 schema for index database: " + schema);
    }

    /**
     * @param sql SQL statement.
     * @throws GridSpiException If failed.
     */
    private void executeStatement(String sql) throws GridSpiException {
        Statement stmt = null;

        try {
            Connection c = connectionForThread(null);

            stmt = c.createStatement();

            stmt.executeUpdate(sql);
        }
        catch (SQLException e) {
            onSqlException();

            throw new GridSpiException("Failed to execute statement: " + sql, e);
        }
        finally {
            U.close(stmt, log);
        }
    }

    /**
     * Removes entry with specified key from any tables (if exist).
     *
     * @param spaceName Space name.
     * @param k Key entity.
     * @param tblToUpdate Table to update.
     * @throws GridSpiException In case of error.
     */
    private <K> void removeKey(@Nullable String spaceName, GridIndexingEntity<K> k, TableDescriptor tblToUpdate)
        throws GridSpiException {
        K key = k.value();

        try {
            Collection<TableDescriptor> tbls = tables(schema(spaceName));

            if (tbls.size() > 1) {
                boolean fixedTyping = isIndexFixedTyping(spaceName);

                for (TableDescriptor tbl : tbls) {
                    if (tbl != tblToUpdate && (tbl.type().keyClass().equals(key.getClass()) ||
                        !fixedTyping)) {
                        if (tbl.tbl.update(key, null, 0)) {
                            if (tbl.luceneIdx != null)
                                tbl.luceneIdx.remove(k);

                            return;
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            throw new GridSpiException("Failed to remove key: " + key, e);
        }
    }

    /**
     * Binds object to prepared statement.
     *
     * @param stmt SQL statement.
     * @param idx Index.
     * @param obj Value to store.
     * @throws SQLException In case of errors.
     */
    private void bindObject(PreparedStatement stmt, int idx, @Nullable Object obj)
        throws SQLException {
        if (obj == null)
            stmt.setNull(idx, Types.VARCHAR);
        else
            stmt.setObject(idx, obj);
    }

    /**
     * Handles SQL exception.
     */
    private void onSqlException() {
        Connection conn = connCache.get().connection();

        connCache.set(null);

        if (conn != null) {
            conns.remove(conn);

            // Reset connection to receive new one at next call.
            U.close(conn, log);
        }
    }

    /** {@inheritDoc} */
    @Override public <K, V> void store(@Nullable String spaceName, GridIndexingTypeDescriptor type,
        GridIndexingEntity<K> k, GridIndexingEntity<V> v, byte[] ver, long expirationTime)
        throws GridSpiException {
        TableDescriptor tbl = tableDescriptor(spaceName, type);

        if (tbl == null)
            return; // Type was rejected.

        localSpi.set(this);

        try {
            removeKey(spaceName, k, tbl);

            if (expirationTime == 0)
                expirationTime = Long.MAX_VALUE;

            tbl.tbl.update(k.value(), v.value(), expirationTime);

            if (tbl.luceneIdx != null)
                tbl.luceneIdx.store(k, v, ver, expirationTime);
        }
        finally {
            localSpi.remove();
        }
    }

    /** {@inheritDoc} */
    @Override public <K> boolean remove(@Nullable String spaceName, GridIndexingEntity<K> k) throws GridSpiException {
        assert k != null;

        K key = k.value();

        if (log.isDebugEnabled())
            log.debug("Removing key from cache query index [locId=" + nodeId + ", key=" + key + ']');

        localSpi.set(this);

        try {
            for (TableDescriptor tbl : tables(schema(spaceName))) {
                if (tbl.type().keyClass().equals(key.getClass()) || !isIndexFixedTyping(spaceName)) {
                    if (tbl.tbl.update(key, null, 0)) {
                        if (tbl.luceneIdx != null)
                            tbl.luceneIdx.remove(k);

                        return true;
                    }
                }
            }
        }
        finally {
            localSpi.remove();
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override protected void onContextInitialized0(GridSpiContext spiCtx) throws GridSpiException {
        ctxInitLatch.countDown();

        if (log.isDebugEnabled())
            log.debug("Context has been initialized.");
    }

    /** {@inheritDoc} */
    @Override protected GridSpiContext getSpiContext() {
        if (ctxInitLatch.getCount() != 0) {
            try {
                U.await(ctxInitLatch);
            }
            catch (GridInterruptedException ignored) {
                U.warn(log, "Thread has been interrupted while waiting for SPI context initialization.");
            }
        }

        return super.getSpiContext();
    }

    /** {@inheritDoc} */
    @Override public <K> void onSwap(@Nullable String spaceName, String swapSpaceName, K key) throws GridSpiException {
        Schema schema = schemas.get(schema(spaceName));

        if (schema == null)
            return;

        schema.swapSpaceName = swapSpaceName;

        localSpi.set(this);

        try {
            for (TableDescriptor tbl : schema.values()) {
                if (tbl.type().keyClass().equals(key.getClass()) || !isIndexFixedTyping(spaceName)) {
                    try {
                        if (tbl.tbl.onSwap(key))
                            return;
                    }
                    catch (GridException e) {
                        throw new GridSpiException(e);
                    }
                }
            }
        }
        finally {
            localSpi.remove();
        }
    }

    /** {@inheritDoc} */
    @Override public <K, V> void onUnswap(@Nullable String spaceName, K key, V val, byte[] valBytes)
        throws GridSpiException {
        localSpi.set(this);

        try {
            for (TableDescriptor tbl : tables(schema(spaceName))) {
                if (tbl.type().keyClass().equals(key.getClass()) || !isIndexFixedTyping(spaceName)) {
                    try {
                        if (tbl.tbl.onUnswap(key, val))
                            return;
                    }
                    catch (GridException e) {
                        throw new GridSpiException(e);
                    }
                }
            }
        }
        finally {
            localSpi.remove();
        }
    }

    /**
     * Drops table form h2 database and clear all related indexes (h2 text, lucene).
     *
     * @param tbl Table to unregister.
     * @throws GridSpiException If failed to unregister.
     */
    private void removeTable(TableDescriptor tbl) throws GridSpiException {
        assert tbl != null;

        if (log.isDebugEnabled())
            log.debug("Removing query index table: " + tbl.fullTableName());

        Connection c = connectionForThread(null);

        Statement stmt = null;

        try {
            // NOTE: there is no method dropIndex() for lucene engine correctly working.
            // So we have to drop all lucene index.
            // FullTextLucene.dropAll(c); TODO: GG-4015: fix this

            stmt = c.createStatement();

            String sql = "DROP TABLE IF EXISTS " + tbl.fullTableName();

            if (log.isDebugEnabled())
                log.debug("Dropping database index table with SQL: " + sql);

            stmt.executeUpdate(sql);
        }
        catch (SQLException e) {
            onSqlException();

            throw new GridSpiException("Failed to drop database index table [type=" + tbl.type().name() +
                ", table=" + tbl.fullTableName() + "]", e);
        }
        finally {
            U.close(stmt, log);
        }

        tbl.tbl.close();

        if (tbl.luceneIdx != null)
            U.closeQuiet(tbl.luceneIdx);

        ConcurrentMap<String, TableDescriptor> tbls = schemas.get(tbl.schema());

        if (!F.isEmpty(tbls))
            tbls.remove(tbl.name());
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override public <K, V> GridSpiCloseableIterator<GridIndexingKeyValueRow<K, V>> queryText(
        @Nullable String spaceName, String qry, GridIndexingTypeDescriptor type,
        GridIndexingQueryFilter<K, V>... filters) throws GridSpiException {
        TableDescriptor tbl = tableDescriptor(spaceName, type);

        if (tbl != null && tbl.luceneIdx != null)
            return tbl.luceneIdx.query(qry, filters);

        return new GridEmptyCloseableIterator<>();
    }

    /** {@inheritDoc} */
    @Override public void unregisterType(@Nullable String spaceName, GridIndexingTypeDescriptor type)
        throws GridSpiException {
        TableDescriptor tbl = tableDescriptor(spaceName, type);

        if (tbl != null)
            removeTable(tbl);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override public <K, V> GridIndexingFieldsResult queryFields(@Nullable String spaceName, String qry,
        @Nullable Collection<Object> params, GridIndexingQueryFilter<K, V>... filters) throws GridSpiException {
        localSpi.set(this);

        try {
            Connection conn = connectionForThread(schema(spaceName));

            setFilters(filters != null ? filters : EMPTY_FILTER);

            T2<PreparedStatement, ResultSet> res = executeSqlQueryWithTimer(conn, qry, params);

            if (res == null)
                return new GridIndexingFieldsResultAdapter(null,
                    new GridEmptyCloseableIterator<List<GridIndexingEntity<?>>>());

            ResultSet rs = res.get2();

            List<GridIndexingFieldMetadata> meta;

            try {
                ResultSetMetaData rsMeta = rs.getMetaData();

                meta = new ArrayList<>(rsMeta.getColumnCount());

                for (int i = 1; i <= rsMeta.getColumnCount(); i++) {
                    String schemaName = rsMeta.getSchemaName(i);
                    String typeName = rsMeta.getTableName(i);
                    String name = rsMeta.getColumnLabel(i);
                    String type = rsMeta.getColumnClassName(i);

                    meta.add(new SqlFieldMetadata(schemaName, typeName, name, type));
                }
            }
            catch (SQLException e) {
                throw new GridSpiException("Failed to get meta data.", e);
            }

            return new GridIndexingFieldsResultAdapter(meta, new FieldsIterator(rs, res.get1()));
        }
        catch (SQLException e) {
            onSqlException();

            throw new GridSpiException("Failed to query fields: " + qry, e);
        }
        finally {
            setFilters(null);

            localSpi.remove();
        }
    }

    /**
     * @param stmt Prepared statement.
     * @return Command type.
     */
    private static int commandType(PreparedStatement stmt) {
        try {
            return ((CommandInterface)COMMAND_FIELD.get(stmt)).getCommandType();
        }
        catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Executes sql query.
     *
     * @param conn Connection,.
     * @param sql Sql query.
     * @param params Parameters.
     * @return Result.
     * @throws SQLException If failed.
     * @throws GridSpiException If failed.
     */
    @Nullable private T2<PreparedStatement, ResultSet> executeSqlQuery(Connection conn, String sql,
        @Nullable Collection<Object> params) throws SQLException, GridSpiException {
        PreparedStatement stmt;

        try {
            stmt = conn.prepareStatement(sql);
        }
        catch (SQLException e) {
            if (e.getErrorCode() == ErrorCode.TABLE_OR_VIEW_NOT_FOUND_1)
                return null;

            throw new GridSpiException("Failed to parse query: " + sql, e);
        }

        switch (commandType(stmt)) {
            case CommandInterface.SELECT:
            case CommandInterface.CALL:
            case CommandInterface.EXPLAIN:
            case CommandInterface.ANALYZE:
                break;
            default:
                throw new GridSpiException("Failed to execute non-query SQL statement: " + sql);
        }

        bindParameters(stmt, params);

        return new T2<>(stmt, stmt.executeQuery());
    }

    /**
     * Executes sql query and prints warning if query is too slow..
     *
     * @param conn Connection,.
     * @param sql Sql query.
     * @param params Parameters.
     * @return Result.
     * @throws SQLException If failed.
     * @throws GridSpiException If failed.
     */
    private T2<PreparedStatement, ResultSet> executeSqlQueryWithTimer(Connection conn, String sql,
        @Nullable Collection<Object> params) throws SQLException, GridSpiException {
        long start = U.currentTimeMillis();

        T2<PreparedStatement, ResultSet> res = executeSqlQuery(conn, sql, params);

        long time = U.currentTimeMillis() - start;

        if (time > longQryExecTimeout) {
            String msg = "Query execution is too long (" + time + " ms): " + sql;

            String longMsg = msg;

            if (longQryExplain) {
                T2<PreparedStatement, ResultSet> t2 = executeSqlQuery(conn, "EXPLAIN " + sql, params);

                if (t2 == null)
                    longMsg = "Failed to explain plan because required table does not exist: " + sql;
                else {
                    ResultSet planRs = t2.get2();

                    planRs.next();

                    // Add SQL explain result message into log.
                    longMsg = "Query execution is too long [time=" + time + " ms, sql='" + sql + '\'' +
                            ", plan=" + U.nl() + planRs.getString(1) + U.nl() + ", parameters=" + params + "]";
                }
            }

            LT.warn(log, null, longMsg, msg);
        }

        return res;
    }

    /**
     * Executes query.
     *
     * @param qry Query.
     * @param params Query parameters.
     * @param tbl Target table of query to generate select.
     * @return Result set.
     * @throws GridSpiException If failed.
     * @throws SQLException If failed.
     */
    private T2<PreparedStatement, ResultSet> executeQuery(String qry, @Nullable Collection<Object> params,
        @Nullable TableDescriptor tbl) throws GridSpiException, SQLException {
        Connection conn = connectionForThread(tbl != null ? tbl.schema() : "PUBLIC");

        String sql = generateQuery(qry, tbl);

        return executeSqlQueryWithTimer(conn, sql, params);
    }

    /**
     * Binds parameters to prepared statement.
     *
     * @param stmt Prepared statement.
     * @param params Parameters collection.
     * @throws SQLException If failed.
     */
    private void bindParameters(PreparedStatement stmt, @Nullable Collection<Object> params) throws SQLException {
        if (!F.isEmpty(params)) {
            int idx = 1;

            for (Object arg : params)
                bindObject(stmt, idx++, arg);
        }
    }

    /**
     * Executes regular query.
     * Note that SQL query can not refer to table alias, so use full table name instead.
     *
     * @param spaceName Space name.
     * @param qry Query.
     * @param params Query parameters.
     * @param type Query return type.
     * @param filters Space name and key filters.
     * @return Queried rows.
     * @throws GridSpiException If failed.
     */
    @SuppressWarnings("unchecked")
    @Override public <K, V> GridSpiCloseableIterator<GridIndexingKeyValueRow<K, V>> query(@Nullable String spaceName,
        String qry, @Nullable Collection<Object> params, GridIndexingTypeDescriptor type,
        GridIndexingQueryFilter<K, V>... filters) throws GridSpiException {
        TableDescriptor tbl = tableDescriptor(spaceName, type);

        if (tbl == null)
            return new GridEmptyCloseableIterator<>();

        setFilters(filters != null ? filters : EMPTY_FILTER);

        localSpi.set(this);

        try {
            T2<PreparedStatement, ResultSet> t = executeQuery(qry, params, tbl);

            return t != null ? new KeyValIterator<K, V>(t.get2(), t.get1()) :
                new GridEmptyCloseableIterator<GridIndexingKeyValueRow<K, V>>();
        }
        catch (SQLException e) {
            if (e.getErrorCode() != ErrorCode.TABLE_OR_VIEW_NOT_FOUND_1) {
                onSqlException();

                throw new GridSpiException("Failed to query entries: " + qry, e);
            }

            return new GridEmptyCloseableIterator<>();
        }
        finally {
            setFilters(null);

            localSpi.remove();
        }
    }

    /**
     * Sets filters for current thread. Must be set to not null value
     * before executeQuery and reset to null after in finally block since it signals
     * to table that it should return content without expired values.
     *
     * @param filters Filters.
     */
    private void setFilters(@Nullable GridIndexingQueryFilter<?, ?>[] filters) {
        GridH2IndexBase.setFiltersForThread(filters);
    }

    /**
     * Prepares statement for query.
     *
     * @param qry Query string.
     * @param tbl Table to use.
     * @return Prepared statement.
     * @throws GridSpiException In case of error.
     */
    private String generateQuery(String qry, @Nullable TableDescriptor tbl) throws GridSpiException {
        boolean needSelect = tbl != null;

        String str = qry.trim().toUpperCase();

        if (!str.startsWith("FROM")) {
            if (str.startsWith("SELECT")) {
                if (needSelect) {
                    StringTokenizer st = new StringTokenizer(str, " ");

                    String errMsg = "Wrong query format, query must start with 'select * from' " +
                        "or 'from' or without such keywords.";

                    if (st.countTokens() > 3) {
                        st.nextToken();
                        String wildcard = st.nextToken();
                        String from = st.nextToken();

                        if (!"*".equals(wildcard) || !"FROM".equals(from))
                            throw new GridSpiException(errMsg);

                        needSelect = false;
                    }
                    else
                        throw new GridSpiException(errMsg);
                }
            }
            else {
                boolean needWhere = !str.startsWith("ORDER") && !str.startsWith("LIMIT");

                qry = needWhere ? "FROM " + tbl.fullTableName() + " WHERE " + qry :
                    "FROM " + tbl.fullTableName() + ' ' + qry;
            }
        }

        GridStringBuilder ptrn = new SB("SELECT {0}.").a(KEY_FIELD_NAME);

        ptrn.a(", {0}.").a(VAL_FIELD_NAME);

        return needSelect ? MessageFormat.format(ptrn.toString(), tbl.fullTableName()) + ' ' + qry : qry;
    }

    /**
     * Registers new class description.
     *
     * This implementation doesn't support type reregistration.
     *
     * @param type Type description.
     * @throws GridSpiException In case of error.
     */
    @Override public boolean registerType(@Nullable String spaceName, GridIndexingTypeDescriptor type)
        throws GridSpiException {
        if (!validateTypeDescriptor(spaceName, type))
            return false;

        for (TableDescriptor table : tables(schema(spaceName)))
            // Need to compare class names rather than classes to define
            // whether a class was previously undeployed.
            if (table.type().valueClass().getClass().getName().equals(type.valueClass().getName()))
                throw new GridSpiException("Failed to register type in query index because" +
                    " class is already registered (most likely that class with the same name" +
                    " was not properly undeployed): " + type);

        TableDescriptor tbl = new TableDescriptor(spaceName, type);

        Statement stmt = null;

        try {
            Connection conn = connectionForThread(null);

            stmt = conn.createStatement();

            Schema schema = schemas.get(tbl.schema());

            if (schema == null) {
                schema = new Schema(spaceName);

                Schema existing = schemas.putIfAbsent(tbl.schema(), schema);

                if (existing != null)
                    schema = existing;
            }

            createTable(schema, tbl, conn);

            schema.put(tbl.name(), tbl);
        }
        catch (SQLException e) {
            onSqlException();

            throw new GridSpiException("Failed to register query type: " + type, e);
        }
        finally {
            U.close(stmt, log);
        }

        return true;
    }

    /**
     * @param cls Class.
     * @return True if given class has primitive respective sql type.
     */
    private boolean isPrimitive(Class<?> cls) {
        DBTypeEnum valType = DBTypeEnum.fromClass(cls);

        return valType != DBTypeEnum.BINARY && valType != DBTypeEnum.OTHER &&
            valType != DBTypeEnum.ARRAY;
    }

    /**
     * Validates properties described by query types.
     *
     * @param spaceName Space name.
     * @param type Type descriptor.
     * @return True if type is valid.
     * @throws GridSpiException If validation failed.
     */
    private boolean validateTypeDescriptor(@Nullable String spaceName, GridIndexingTypeDescriptor type)
        throws GridSpiException {
        assert type != null;

        boolean keyPrimitive = isPrimitive(type.keyClass());
        boolean valPrimitive = isPrimitive(type.valueClass());

        // Do not register if value is not primitive and
        // there are no indexes or fields defined.
        if (!type.valueTextIndex() && type.indexes().isEmpty() &&
            type.keyFields().isEmpty() && type.valueFields().isEmpty()) {
            return keyPrimitive && isIndexPrimitiveKey(spaceName) || valPrimitive && isIndexPrimitiveValue(spaceName);
        }

        Collection<String> names = new HashSet<>();

        names.addAll(type.keyFields().keySet());
        names.addAll(type.valueFields().keySet());

        if (names.size() < type.keyFields().size() + type.valueFields().size())
            throw new GridSpiException("Found duplicated properties with the same name [keyType=" +
                type.keyClass().getName() + ", valueType=" + type.valueClass().getName() + "]");

        String ptrn = "Name ''{0}'' is reserved and cannot be used as a field name [class=" + type + "]";

        for (String name : names) {
            if (name.equals(KEY_FIELD_NAME) ||
                name.equals(VAL_FIELD_NAME) ||
                name.equals(VAL_STR_FIELD_NAME) ||
                name.equals(VER_FIELD_NAME) ||
                name.equals(EXPIRATION_TIME_FIELD_NAME))
                throw new GridSpiException(MessageFormat.format(ptrn, name));
        }

        return true;
    }

    /**
     * Escapes name to be valid SQL identifier. Currently just replaces '.' and '$' sign with '_'.
     *
     * @param name Name
     * @return Escaped name.
     */
    private static String escapeName(String name) {
        SB sb = null;

        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);

            if (!Character.isLetter(ch) && !Character.isDigit(ch) && ch != '_') {
                // Class name can also contain '$' or '.' - these should be escaped.
                assert ch == '$' || ch == '.';

                if (sb == null)
                    sb = new SB();

                sb.a(name.substring(sb.length(), i));

                // Replace illegal chars with '_'.
                sb.a('_');
            }
        }

        if (sb == null)
            return name;

        sb.a(name.substring(sb.length(), name.length()));

        return sb.toString();
    }

    /**
     * Create db table by using given table descriptor.
     *
     * @param schema Schema.
     * @param tbl Table descriptor.
     * @param conn Connection.
     * @throws SQLException If failed to create db table.
     */
    private void createTable(Schema schema, TableDescriptor tbl, Connection conn) throws SQLException {
        assert tbl != null;

        boolean keyAsObj = !isIndexFixedTyping(schema.spaceName);

        String keyType = keyAsObj ? "OTHER" : dbTypeFromClass(tbl.type().keyClass());
        String valTypeStr = dbTypeFromClass(tbl.type().valueClass());

        SB sql = new SB();

        sql.a("CREATE TABLE ").a(tbl.fullTableName()).a(" (")
            .a(KEY_FIELD_NAME).a(' ').a(keyType).a(" NOT NULL");

        sql.a(',').a(VAL_FIELD_NAME).a(' ').a(valTypeStr).a(',').a(VAL_STR_FIELD_NAME).a(' ').a("VARCHAR");

        for (Map.Entry<String, Class<?>> e: tbl.type().keyFields().entrySet())
            sql.a(',').a(escapeName(e.getKey())).a(' ').a(dbTypeFromClass(e.getValue()));

        for (Map.Entry<String, Class<?>> e: tbl.type().valueFields().entrySet())
            sql.a(',').a(escapeName(e.getKey())).a(' ').a(dbTypeFromClass(e.getValue()));

        sql.a(')');

        if (log.isDebugEnabled())
            log.debug("Creating DB table with SQL: " + sql);

        GridH2RowDescriptor desc = new RowDescriptor(tbl.type(), schema, keyAsObj);

        GridH2Table.Engine.createTable(conn, sql.toString(), desc, tbl, tbl.spaceName);
    }

    /**
     * Gets corresponding DB type from java class.
     *
     * @param cls Java class.
     * @return DB type name.
     */
    private String dbTypeFromClass(Class<?> cls) {
        return DBTypeEnum.fromClass(cls).dBTypeAsString();
    }

    /**
     * Gets table descriptor by value type.
     *
     * @param spaceName Space name.
     * @param type Value type descriptor.
     * @return Table descriptor or {@code null} if not found.
     */
    @Nullable private TableDescriptor tableDescriptor(@Nullable String spaceName, GridIndexingTypeDescriptor type) {
        return tableDescriptor(type.name(), spaceName);
    }

    /**
     * Gets table descriptor by type and space names.
     *
     * @param type Type name.
     * @param space Space name.
     * @return Table descriptor.
     */
    @Nullable private TableDescriptor tableDescriptor(String type, @Nullable String space) {
        ConcurrentMap<String, TableDescriptor> tbls = schemas.get(schema(space));

        if (tbls == null)
            return null;

        return tbls.get(type);
    }

    /**
     * Gets collection of table for given schema name.
     *
     * @param schema Schema name.
     * @return Collection of table descriptors.
     */
    private Collection<TableDescriptor> tables(String schema) {
        ConcurrentMap<String, TableDescriptor> tbls = schemas.get(schema);

        if (tbls == null)
            return Collections.emptySet();

        return tbls.values();
    }

    /**
     * Gets database schema from space.
     *
     * @param space Space name.
     * @return Schema name.
     */
    private static String schema(@Nullable String space) {
        if (F.isEmpty(space))
            return "PUBLIC";

        return space;
    }

    /** {@inheritDoc} */
    @Override public void rebuildIndexes(@Nullable String spaceName, GridIndexingTypeDescriptor type) {
        if (offheap != null)
            throw new UnsupportedOperationException("Index rebuilding is not supported when off-heap memory is used");

        TableDescriptor tbl = tableDescriptor(spaceName, type);

        if (tbl == null)
            return;

        tbl.tbl.rebuildIndexes();
    }

    /** {@inheritDoc} */
    @Override public long size(@Nullable String spaceName, GridIndexingTypeDescriptor type) throws GridSpiException {
        Connection conn = connectionForThread(null);

        TableDescriptor tbl = tableDescriptor(spaceName, type);

        if (tbl == null)
            return -1;

        try {
            ResultSet rs = executeSqlQueryWithTimer(conn, "SELECT COUNT(*) FROM " + tbl.fullTableName(), null).get2();

            rs.next();

            return rs.getInt(1);
        }
        catch (SQLException e) {
            U.rollbackConnection(conn, log);

            onSqlException();

            throw new GridSpiException("Failed to get table size: " + tbl.fullTableName(), e);
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("NonThreadSafeLazyInitialization")
    @Override public void spiStart(@Nullable String gridName) throws GridSpiException {
        if (log.isDebugEnabled())
            log.debug("Starting cache query index...");

        assertParameter(idxWriteLockWaitTime > 0, "'idxWriteLockWaitTime' must be positive.");

        startStopwatch();

        System.setProperty("h2.serializeJavaObject", "false");

        if (SysProperties.serializeJavaObject) {
            U.warn(log, "Serialization of Java objects in H2 was enabled.");

            SysProperties.serializeJavaObject = false;
        }

        if (maxOffHeapMemory != -1) {
            assert maxOffHeapMemory >= 0 : maxOffHeapMemory;

            offheap = new GridUnsafeMemory(maxOffHeapMemory);
        }

        SB opt = new SB();

        opt.a(DFLT_DB_OPTIONS).a(OPTIMIZED_DB_OPTIONS);

        String dbName = UUID.randomUUID() + "_" + gridName + "_" + getName();

        dbUrl = "jdbc:h2:mem:" + DFLT_DB_NAME + "_" + dbName + opt;

        try {
            Class.forName("org.h2.Driver");
        }
        catch (ClassNotFoundException e) {
            throw new GridSpiException("Failed to find org.h2.Driver class", e);
        }

        setH2Serializer();

        for (String schema : schemaNames)
            createSchemaIfAbsent(schema);

        try {
            createSqlFunctions();
            runInitScript();

            if (X.getSystemOrEnv(GridSystemProperties.GG_H2_DEBUG_CONSOLE) != null) {
                Connection c = DriverManager.getConnection(dbUrl);

                WebServer webSrv = new WebServer();
                Server web = new Server(webSrv, "-webPort", "0");
                web.start();
                String url = webSrv.addSession(c);

                try {
                    Server.openBrowser(url);
                }
                catch (Exception e) {
                    U.warn(log, "Failed to open browser: " + e.getMessage());
                }
            }
        }
        catch (SQLException e) {
            throw new GridSpiException(e);
        }

        registerMBean(gridName, this, GridH2IndexingSpiMBean.class);

        if (log.isDebugEnabled())
            log.debug("Cache query index started [grid=" + gridName + ", cache=" + getName() + "]");
    }

    /**
     * Runs initial script.
     *
     * @throws GridSpiException If failed.
     * @throws SQLException If failed.
     */
    private void runInitScript() throws GridSpiException, SQLException {
        if (initScriptPath == null)
            return;

        try (PreparedStatement p = connectionForThread(null).prepareStatement("RUNSCRIPT FROM ? CHARSET 'UTF-8'")) {
            p.setString(1, initScriptPath);

            p.execute();
        }
    }

    /**
     * @throws GridSpiException If failed.
     */
    private void setH2Serializer() throws GridSpiException {
        executeStatement("SET JAVA_OBJECT_SERIALIZER '" + H2Serializer.class.getName() + "'");
    }

    /**
     * Registers SQL functions.
     *
     * @throws SQLException If failed.
     * @throws GridSpiException If failed.
     */
    private void createSqlFunctions() throws SQLException, GridSpiException {
        if (F.isEmpty(idxCustomFuncClss))
            return;

        for (Class<?> cls : idxCustomFuncClss) {
            for (Method m : cls.getDeclaredMethods()) {
                GridCacheQuerySqlFunction ann = m.getAnnotation(GridCacheQuerySqlFunction.class);

                if (ann != null) {
                    int modifiers = m.getModifiers();

                    if (!Modifier.isStatic(modifiers) || !Modifier.isPublic(modifiers))
                        throw new GridSpiException("Method " + m.getName() + " must be public static.");

                    String alias = ann.alias().isEmpty() ? m.getName() : ann.alias();

                    String clause = "CREATE ALIAS " + alias + (ann.deterministic() ? " DETERMINISTIC FOR \"" :
                        " FOR \"") + cls.getName() + '.' + m.getName() + '"';

                    Collection<String> schemas = new ArrayList<>(schemaNames);

                    if (!schemaNames.contains(schema(null)))
                        schemas.add(schema(null));

                    for (String schema : schemas) {
                        Connection c = connectionForThread(schema);

                        Statement s = c.createStatement();

                        s.execute(clause);

                        s.close();
                    }
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override public void spiStop() throws GridSpiException {
        if (log.isDebugEnabled())
            log.debug("Stopping cache query index...");

        if (ctxInitLatch.getCount() != 0)
            ctxInitLatch.countDown();

        unregisterMBean();

        Connection conn = connectionForThread(null);

        for (ConcurrentMap<String, TableDescriptor> m : schemas.values()) {
            for (TableDescriptor desc : m.values()) {
                desc.tbl.close();

                if (desc.luceneIdx != null)
                    U.closeQuiet(desc.luceneIdx);
            }
        }

        if (conn != null) {
            Statement stmt = null;

            try {
                stmt = conn.createStatement();

                stmt.execute("DROP ALL OBJECTS DELETE FILES");
                stmt.execute("SHUTDOWN");
            }
            catch (SQLException e) {
                throw new GridSpiException("Failed to shutdown database.", e);
            }
            finally {
                U.close(stmt, log);
            }
        }

        for (Connection c : conns)
            U.close(c, log);

        conns.clear();
        schemas.clear();
        rowCache.clear();

        if (log.isDebugEnabled())
            log.debug("Cache query index stopped [cache=" + getName() + "]");
    }

    /**
     * Sets classes with methods annotated by {@link GridCacheQuerySqlFunction}
     * to be used as user-defined functions from SQL queries.
     *
     * @param idxCustomFuncClss List of classes.
     */
    public void setIndexCustomFunctionClasses(Class<?>... idxCustomFuncClss) {
        this.idxCustomFuncClss = idxCustomFuncClss;
    }

    /** {@inheritDoc} */
    @Override public String getSpaceNames() {
        return StringUtils.arrayCombine(spaceCfgs.keySet().toArray(new String[spaceCfgs.size()]), ',');
    }

    /** {@inheritDoc} */
    @Override public long getLongQueryExecutionTimeout() {
        return longQryExecTimeout;
    }

    /**
     * Set query execution time threshold. If queries exceed this threshold,
     * then a warning will be printed out. If {@link #setLongQueryExplain(boolean)} is
     * set to {@code true}, then execution plan will be printed out as well.
     * <p>
     * If not provided, default value is defined by {@link #DFLT_LONG_QRY_EXEC_TIMEOUT}.
     *
     * @param longQryExecTimeout Long query execution timeout.
     * @see #setLongQueryExplain(boolean)
     */
    @GridSpiConfiguration(optional = true)
    public void setLongQueryExecutionTimeout(long longQryExecTimeout) {
        this.longQryExecTimeout = longQryExecTimeout;
    }

    /** {@inheritDoc} */
    @Override public boolean isLongQueryExplain() {
        return longQryExplain;
    }

    /**
     * If {@code true}, SPI will print SQL execution plan for long queries (explain SQL query).
     * The time threshold of long queries is controlled via {@link #setLongQueryExecutionTimeout(long)}
     * parameter.
     * <p>
     * If not provided, default value is {@code false}.
     *
     * @param longQryExplain Flag marking SPI should print SQL execution plan for long queries (explain SQL query).
     * @see #setLongQueryExecutionTimeout(long)
     */
    @GridSpiConfiguration(optional = true)
    public void setLongQueryExplain(boolean longQryExplain) {
        this.longQryExplain = longQryExplain;
    }

    /** {@inheritDoc} */
    @Override public boolean isIndexPrimitiveKey(@Nullable String spaceName) {
        GridH2IndexingSpaceConfiguration cfg = spaceCfgs.get(spaceName);

        if (cfg != null)
            return cfg.isIndexPrimitiveKey();

        return dfltIdxPrimitiveKey;
    }

    /** {@inheritDoc} */
    @Override public boolean isIndexPrimitiveValue(String spaceName) {
        GridH2IndexingSpaceConfiguration cfg = spaceCfgs.get(spaceName);

        if (cfg != null)
            return cfg.isIndexPrimitiveValue();

        return dfltIdxPrimitiveVal;
    }

    /** {@inheritDoc} */
    @Override public boolean isIndexFixedTyping(String spaceName) {
        GridH2IndexingSpaceConfiguration cfg = spaceCfgs.get(spaceName);

        if (cfg != null)
            return cfg.isIndexFixedTyping();

        return dfltIdxFixedTyping;
    }

    /**
     * Sets if SPI will index primitive key-value pairs by value. Makes sense only if
     * {@link #setDefaultIndexPrimitiveKey(boolean)} set to true.
     *
     * @param dfltIdxPrimitiveVal Flag value.
     */
    @GridSpiConfiguration(optional = true)
    public void setDefaultIndexPrimitiveValue(boolean dfltIdxPrimitiveVal) {
        this.dfltIdxPrimitiveVal = dfltIdxPrimitiveVal;
    }

    /**
     * Gets if SPI will index primitive key-value pairs by value. Makes sense only if
     * {@link #setDefaultIndexPrimitiveKey(boolean)} set to true.
     *
     * @return Flag value.
     */
    @Override public boolean isDefaultIndexPrimitiveValue() {
        return dfltIdxPrimitiveVal;
    }

    /**
     * Sets if SPI will index primitive key-value pairs by key.
     *
     * @param dfltIdxPrimitiveKey Flag value.
     */
    @GridSpiConfiguration(optional = true)
    public void setDefaultIndexPrimitiveKey(boolean dfltIdxPrimitiveKey) {
        this.dfltIdxPrimitiveKey = dfltIdxPrimitiveKey;
    }

    /** {@inheritDoc} */
    @Override public boolean isDefaultIndexPrimitiveKey() {
        return dfltIdxPrimitiveKey;
    }

    /** {@inheritDoc} */
    @Override public boolean isDefaultIndexFixedTyping() {
        return dfltIdxFixedTyping;
    }

    /**
     * This flag essentially controls whether all values of the same type have
     * identical key type.
     * <p>
     * If {@code false}, SPI will store all keys in BINARY form to make it possible to store
     * the same value type with different key types. If {@code true}, key type will be converted
     * to respective SQL type if it is possible, hence, improving performance of queries.
     * <p>
     * Setting this value to {@code false} also means that {@code '_key'} column cannot be indexed and
     * cannot participate in query where clauses. The behavior of using '_key' column in where
     * clauses with this flag set to {@code false} is undefined.
     * <p>
     * This property can be overridden on per-space level via {@link GridH2IndexingSpaceConfiguration}.
     *
     * @param dfltIdxFixedTyping FLag value.
     */
    @GridSpiConfiguration(optional = true)
    public void setDefaultIndexFixedTyping(boolean dfltIdxFixedTyping) {
        this.dfltIdxFixedTyping = dfltIdxFixedTyping;
    }

    /**
     * Sets maximum amount of memory available to off-heap storage. Possible values are
     * <ul>
     * <li>{@code -1} - Means that off-heap storage is disabled.</li>
     * <li>
     *     {@code 0} - GridGain will not limit off-heap storage (it's up to user to properly
     *     add and remove entries from cache to ensure that off-heap storage does not grow
     *     indefinitely.
     * </li>
     * <li>Any positive value specifies the limit of off-heap storage in bytes.</li>
     * </ul>
     * Default value is {@code -1}, which means that off-heap storage is disabled by default.
     * <p>
     * Use off-heap storage to load gigabytes of data in memory without slowing down
     * Garbage Collection. Essentially in this case you should allocate very small amount
     * of memory to JVM and GridGain will cache most of the data in off-heap space
     * without affecting JVM performance at all.
     *
     * @param maxOffHeapMemory Maximum memory in bytes available to off-heap memory space.
     */
    @GridSpiConfiguration(optional = true)
    public void setMaxOffHeapMemory(long maxOffHeapMemory) {
        this.maxOffHeapMemory = maxOffHeapMemory;
    }

    /** {@inheritDoc} */
    @Override public long getMaxOffHeapMemory() {
        return maxOffHeapMemory;
    }

    /**
     * Sets index write lock wait time in milliseconds. This parameter can affect query performance under high
     * thread contention. Default value is {@code 100}.
     *
     * @param idxWriteLockWaitTime Index write lock wait time.
     */
    public void setIndexWriteLockWaitTime(long idxWriteLockWaitTime) {
        this.idxWriteLockWaitTime = idxWriteLockWaitTime;
    }

    /** {@inheritDoc} */
    @Override public long getIndexWriteLockWaitTime() {
        return idxWriteLockWaitTime;
    }

    /**
     * Specifies max allowed size of cache for deserialized offheap rows to avoid deserialization costs for most
     * frequently used ones. In general performance is better with greater cache size. Must be more than 128 items.
     *
     * @param size Cache size in items.
     */
    @GridSpiConfiguration(optional = true)
    public void setMaxOffheapRowsCacheSize(int size) {
        A.ensure(size >= 128, "Offheap rows cache size must be not less than 128.");

        rowCache = new CacheLongKeyLIRS<>(size, 1, 128, 256);
    }

    /**
     * Sets the optional search path consisting of space names to search SQL schema objects. Useful for cross cache
     * queries to avoid writing fully qualified table names.
     *
     * @param searchPath Search path.
     */
    @GridSpiConfiguration(optional = true)
    public void setSearchPath(String... searchPath) {
        this.searchPath = searchPath;
    }

    /** {@inheritDoc} */
    @Nullable @Override public String[] getSearchPath() {
        return searchPath;
    }

    /** {@inheritDoc} */
    @Nullable @Override public String getInitialScriptPath() {
        return initScriptPath;
    }

    /**
     * Sets script path to be ran against H2 database after opening.
     * The script must be UTF-8 encoded file.
     *
     * @param initScriptPath Script path.
     */
    @GridSpiConfiguration(optional = true)
    public void setInitialScriptPath(String initScriptPath) {
        this.initScriptPath = initScriptPath;
    }

    /** {@inheritDoc} */
    @Override public int getMaxOffheapRowsCacheSize() {
        return (int)rowCache.getMaxMemory();
    }

    /** {@inheritDoc} */
    @Override public int getOffheapRowsCacheSize() {
        return (int)rowCache.getUsedMemory();
    }

    /** {@inheritDoc} */
    @Override public long getAllocatedOffHeapMemory() {
        return offheap == null ? -1 : offheap.allocatedSize();
    }

    /** {@inheritDoc} */
    @Override public void registerMarshaller(GridIndexingMarshaller marshaller) {
        this.marshaller = marshaller;
    }

    /** {@inheritDoc} */
    @Override public void registerSpace(String spaceName) throws GridSpiException {
        schemaNames.add(schema(spaceName));

        if (!spaceCfgs.containsKey(spaceName))
            spaceCfgs.put(spaceName, null);
    }

    /**
     * Sets per-space configurations.
     *
     * @param spaceCfgs Space configurations list.
     */
    @GridSpiConfiguration(optional = true)
    public void setSpaceConfigurations(GridH2IndexingSpaceConfiguration... spaceCfgs) {
        Map<String , GridH2IndexingSpaceConfiguration> map = new HashMap<>();

        for (GridH2IndexingSpaceConfiguration cfg : spaceCfgs) {
            GridH2IndexingSpaceConfiguration old = map.put(cfg.getName(), cfg);

            assert old != null : "Space configured twice: " + cfg.getName();
        }

        this.spaceCfgs = map;
    }

    /**
     * Wrapper to store connection and flag is schema set or not.
     */
    private static class ConnectionWrapper {
        /** */
        private Connection conn;

        /** */
        private volatile String schema;

        /**
         * @param conn Connection to use.
         */
        ConnectionWrapper(Connection conn) {
            this.conn = conn;
        }

        /**
         * @return Schema name if schema is set, null otherwise.
         */
        public String schema() {
            return schema;
        }

        /**
         * @param schema Schema name set on this connection.
         */
        public void schema(@Nullable String schema) {
            this.schema = schema;
        }

        /**
         * @return Connection.
         */
        public Connection connection() {
            return conn;
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(ConnectionWrapper.class, this);
        }
    }

    /**
     * Enum that helps to map java types to database types.
     */
    private enum DBTypeEnum {
        /** */
        INT("INT"),

        /** */
        BOOL("BOOL"),

        /** */
        TINYINT("TINYINT"),

        /** */
        SMALLINT("SMALLINT"),

        /** */
        BIGINT("BIGINT"),

        /** */
        DECIMAL("DECIMAL"),

        /** */
        DOUBLE("DOUBLE"),

        /** */
        REAL("REAL"),

        /** */
        TIME("TIME"),

        /** */
        TIMESTAMP("TIMESTAMP"),

        /** */
        DATE("DATE"),

        /** */
        VARCHAR("VARCHAR"),

        /** */
        CHAR("CHAR"),

        /** */
        BINARY("BINARY"),

        /** */
        UUID("UUID"),

        /** */
        ARRAY("ARRAY"),

        /** */
        GEOMETRY("GEOMETRY"),

        /** */
        OTHER("OTHER");

        /** Map of Class to enum. */
        private static final Map<Class<?>, DBTypeEnum> map = new HashMap<>();

        /**
         * Initialize map of DB types.
         */
        static {
            map.put(int.class, INT);
            map.put(Integer.class, INT);
            map.put(boolean.class, BOOL);
            map.put(Boolean.class, BOOL);
            map.put(byte.class, TINYINT);
            map.put(Byte.class, TINYINT);
            map.put(short.class, SMALLINT);
            map.put(Short.class, SMALLINT);
            map.put(long.class, BIGINT);
            map.put(Long.class, BIGINT);
            map.put(BigDecimal.class, DECIMAL);
            map.put(double.class, DOUBLE);
            map.put(Double.class, DOUBLE);
            map.put(float.class, REAL);
            map.put(Float.class, REAL);
            map.put(Time.class, TIME);
            map.put(Timestamp.class, TIMESTAMP);
            map.put(java.util.Date.class, TIMESTAMP);
            map.put(java.sql.Date.class, DATE);
            map.put(char.class, CHAR);
            map.put(Character.class, CHAR);
            map.put(String.class, VARCHAR);
            map.put(UUID.class, UUID);
            map.put(byte[].class, BINARY);
        }

        /** */
        private final String dbType;

        /**
         * Constructs new instance.
         *
         * @param dbType DB type name.
         */
        DBTypeEnum(String dbType) {
            this.dbType = dbType;
        }

        /**
         * Resolves enum by class.
         *
         * @param cls Class.
         * @return Enum value.
         */
        public static DBTypeEnum fromClass(Class<?> cls) {
            DBTypeEnum res = map.get(cls);

            if (res != null)
                return res;

            if (DataType.isGeometryClass(cls))
                return GEOMETRY;

            return cls.isArray() && !cls.getComponentType().isPrimitive() ? ARRAY : OTHER;
        }

        /**
         * Gets DB type name.
         *
         * @return DB type name.
         */
        public String dBTypeAsString() {
            return dbType;
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(DBTypeEnum.class, this);
        }
    }

    /**
     * Information about table in database.
     */
    private class TableDescriptor implements GridH2Table.IndexesFactory {
        /** */
        private final String fullTblName;

        /** */
        private final GridIndexingTypeDescriptor type;

        /** */
        private final String spaceName;

        /** */
        private final String schema;

        /** */
        private GridH2Table tbl;

        /** */
        private GridLuceneIndex luceneIdx;

        /**
         * @param spaceName Space name.
         * @param type Type descriptor.
         */
        TableDescriptor(@Nullable String spaceName, GridIndexingTypeDescriptor type) {
            this.spaceName = spaceName;
            this.type = type;

            schema = GridH2IndexingSpi.schema(spaceName);

            fullTblName = '\"' + schema + "\"." + escapeName(type.name());
        }

        /**
         * @return Schema name.
         */
        public String schema() {
            return schema;
        }

        /**
         * @return Database table name.
         */
        String fullTableName() {
            return fullTblName;
        }

        /**
         * @return Database table name.
         */
        String name() {
            return type.name();
        }

        /**
         * @return Type.
         */
        GridIndexingTypeDescriptor type() {
            return type;
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(TableDescriptor.class, this);
        }

        /** {@inheritDoc} */
        @Override public ArrayList<Index> createIndexes(GridH2Table tbl) {
            this.tbl = tbl;

            ArrayList<Index> idxs = new ArrayList<>();

            idxs.add(new GridH2TreeIndex("_key_PK", tbl, true, KEY_COL, VAL_COL,
                offheap, tbl.indexColumn(0, ASCENDING)));

            if (type().valueClass() == String.class) {
                try {
                    luceneIdx = new GridLuceneIndex(marshaller, offheap, spaceName, type, true);
                }
                catch (GridSpiException e1) {
                    throw new GridRuntimeException(e1);
                }
            }

            for (Map.Entry<String, GridIndexDescriptor> e : type.indexes().entrySet()) {
                String name = e.getKey();
                GridIndexDescriptor idx = e.getValue();

                if (idx.type() == FULLTEXT) {
                    try {
                        luceneIdx = new GridLuceneIndex(marshaller, offheap, spaceName, type, true);
                    }
                    catch (GridSpiException e1) {
                        throw new GridRuntimeException(e1);
                    }
                }
                else {
                    IndexColumn[] cols = new IndexColumn[idx.fields().size()];

                    int i = 0;

                    for (String field : idx.fields()) {
                        Column col = tbl.getColumn(field.toUpperCase());

                        cols[i++] = tbl.indexColumn(col.getColumnId(), idx.descending(field) ? DESCENDING : ASCENDING);
                    }

                    if (idx.type() == SORTED)
                        idxs.add(new GridH2TreeIndex(name, tbl, false, KEY_COL, VAL_COL, offheap, cols));
                    else if (idx.type() == GEO_SPATIAL)
                        idxs.add(new GridH2SpatialIndex(tbl, name, cols, KEY_COL, VAL_COL));
                    else
                        throw new IllegalStateException();
                }
            }

            return idxs;
        }
    }

    /**
     * Special field set iterator based on database result set.
     */
    private class FieldsIterator extends GridH2ResultSetIterator<List<GridIndexingEntity<?>>> {
        /** */
        private static final long serialVersionUID = 0L;

        /**
         * @param rs Result set.
         * @param stmt Statement to close at the end (if provided).
         */
        protected FieldsIterator(ResultSet rs, Statement stmt) {
            super(rs, stmt);
        }

        /** {@inheritDoc} */
        @Override protected void onSqlException(SQLException e) {
            GridH2IndexingSpi.this.onSqlException();
        }

        /**
         * Loads row from result set.
         *
         * @return Object associated with row of the result set.
         * @throws SQLException In case of SQL error.
         */
        @Override protected List<GridIndexingEntity<?>> loadRow()
            throws SQLException, GridSpiException, IOException {
            ResultSetMetaData m = rs.getMetaData();

            int cnt = m.getColumnCount();

            List<GridIndexingEntity<?>> row = new ArrayList<>(cnt);

            for (int i = 1; i <= cnt; i++) {
                Object val = rs.getObject(i);

                row.add(val instanceof GridIndexingEntity ? (GridIndexingEntity<?>)val :
                    new GridIndexingEntityAdapter<>(val, null));
            }

            return row;
        }
    }

    /**
     * Special key/value iterator based on database result set.
     */
    private class KeyValIterator<K, V> extends GridH2ResultSetIterator<GridIndexingKeyValueRow<K, V>> {
        /** */
        private static final long serialVersionUID = 0L;

        /**
         * @param rs   Result set.
         * @param stmt Statement to close at the end (if provided).
         */
        protected KeyValIterator(ResultSet rs, Statement stmt) {
            super(rs, stmt);
        }

        /** {@inheritDoc} */
        @Override protected void onSqlException(SQLException e) {
            GridH2IndexingSpi.this.onSqlException();
        }

        /**
         * Loads row from result set.
         *
         * @return Object associated with row of the result set.
         * @throws SQLException In case of SQL error.
         * @throws GridSpiException In case of error.
         */
        @Override protected GridIndexingKeyValueRow<K, V> loadRow()
            throws SQLException, GridSpiException, IOException {
            K key = (K)rs.getObject(KEY_FIELD_NAME);
            V val = (V)rs.getObject(VAL_FIELD_NAME);

            return new GridIndexingKeyValueRowAdapter<>(new GridIndexingEntityAdapter<>(key, null),
                new GridIndexingEntityAdapter<>(val, null), null);
        }
    }

    /**
     * Field descriptor.
     */
    private static class SqlFieldMetadata implements GridIndexingFieldMetadata {
        /** */
        private static final long serialVersionUID = 0L;

        /** Schema name. */
        private String schemaName;

        /** Type name. */
        private String typeName;

        /** Name. */
        private String name;

        /** Type. */
        private String type;

        /**
         * Required by {@link Externalizable}.
         */
        public SqlFieldMetadata() {
            // No-op
        }

        /**
         * @param schemaName Schema name.
         * @param typeName Type name.
         * @param name Name.
         * @param type Type.
         */
        SqlFieldMetadata(@Nullable String schemaName, @Nullable String typeName, String name, String type) {
            assert name != null;
            assert type != null;

            this.schemaName = schemaName;
            this.typeName = typeName;
            this.name = name;
            this.type = type;
        }

        /** {@inheritDoc} */
        @Override public String schemaName() {
            return schemaName;
        }

        /** {@inheritDoc} */
        @Override public String typeName() {
            return typeName;
        }

        /** {@inheritDoc} */
        @Override public String fieldName() {
            return name;
        }

        /** {@inheritDoc} */
        @Override public String fieldTypeName() {
            return type;
        }

        /** {@inheritDoc} */
        @Override public void writeExternal(ObjectOutput out) throws IOException {
            U.writeString(out, schemaName);
            U.writeString(out, typeName);
            U.writeString(out, name);
            U.writeString(out, type);
        }

        /** {@inheritDoc} */
        @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            schemaName = U.readString(in);
            typeName = U.readString(in);
            name = U.readString(in);
            type = U.readString(in);
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(SqlFieldMetadata.class, this);
        }
    }

    /**
     * Database schema object.
     */
    private static class Schema extends ConcurrentHashMap8<String, TableDescriptor> {
        /** */
        private static final long serialVersionUID = 0L;

        /** */
        private final String spaceName;

        /** */
        private volatile String swapSpaceName;

        /**
         * @param spaceName Space name.
         */
        private Schema(@Nullable String spaceName) {
            this.spaceName = spaceName;
        }
    }

    /**
     * Row descriptor.
     */
    private class RowDescriptor implements GridH2RowDescriptor {
        /** */
        private final GridIndexingTypeDescriptor type;

        /** */
        private final String[] fields;

        /** */
        private final int[] fieldTypes;

        /** */
        private final int keyType;

        /** */
        private final int valType;

        /** */
        private final Schema schema;

        /** */
        private final ClassLoader valClsLdr;

        /** */
        private final int keyCols;

        /**
         * @param type Type descriptor.
         * @param schema Schema.
         * @param keyAsObj Store key as java object.
         */
        RowDescriptor(GridIndexingTypeDescriptor type, Schema schema, boolean keyAsObj) {
            assert type != null;
            assert schema != null;

            this.type = type;
            this.schema = schema;

            keyCols = type.keyFields().size();

            valClsLdr = type.valueClass().getClassLoader();

            Map<String, Class<?>> allFields = new LinkedHashMap<>();

            allFields.putAll(type.keyFields());
            allFields.putAll(type.valueFields());

            fields = allFields.keySet().toArray(new String[allFields.size()]);

            fieldTypes = new int[fields.length];

            Class[] classes = allFields.values().toArray(new Class[fields.length]);

            for (int i = 0; i < fieldTypes.length; i++)
                fieldTypes[i] = DataType.getTypeFromClass(classes[i]);

            keyType = keyAsObj ? Value.JAVA_OBJECT : DataType.getTypeFromClass(type.keyClass());
            valType = DataType.getTypeFromClass(type.valueClass());
        }

        /** {@inheritDoc} */
        @Override public void cache(GridH2KeyValueRowOffheap row) {
            long ptr = row.pointer();

            assert ptr > 0 : ptr;

            rowCache.put(ptr, row);
        }

        /** {@inheritDoc} */
        @Override public void uncache(long ptr) {
            rowCache.remove(ptr);
        }

        /** {@inheritDoc} */
        @Override public GridUnsafeMemory memory() {
            return offheap;
        }

        /** {@inheritDoc} */
        @Override public GridH2IndexingSpi spi() {
            return GridH2IndexingSpi.this;
        }

        /** {@inheritDoc} */
        @Override public GridH2AbstractKeyValueRow createRow(Object key, @Nullable Object val, long expirationTime)
            throws GridSpiException {
            try {
                return offheap == null ?
                    new GridH2KeyValueRowOnheap(this, key, keyType, val, valType, expirationTime) :
                    new GridH2KeyValueRowOffheap(this, key, keyType, val, valType, expirationTime);
            }
            catch (ClassCastException e) {
                throw new GridSpiException("Failed to convert key to SQL type. " +
                    "Please make sure that you always store each value type with the same key type or disable " +
                    "'defaultIndexFixedTyping' property on GridH2IndexingSpi.", e);
            }
        }

        /** {@inheritDoc} */
        @Override public Object readFromSwap(Object key) throws GridException {
            GridSpiContext ctx = getSpiContext();

            int part = ctx.partition(schema.spaceName, key);

            byte[] keyBytes = gridMarshaller.marshal(key);

            GridCacheSwapEntry swapEntry = ctx.readFromOffheap(schema.swapSpaceName, part, key, keyBytes, valClsLdr);

            if (swapEntry == null) {
                swapEntry = ctx.readFromSwap(schema.swapSpaceName, new GridSwapKey(key, part, keyBytes), valClsLdr);

                if (swapEntry == null)
                    return null;
            }

            return gridMarshaller.unmarshal(swapEntry.valueBytes(), valClsLdr);
        }

        /** {@inheritDoc} */
        @Override public int valueType() {
            return valType;
        }

        /** {@inheritDoc} */
        @Override public int fieldsCount() {
            return fields.length;
        }

        /** {@inheritDoc} */
        @Override public int fieldType(int col) {
            return fieldTypes[col];
        }

        /** {@inheritDoc} */
        @Override public Object columnValue(Object obj, int col) {
            try {
                return type.value(obj, fields[col]);
            }
            catch (GridSpiException e) {
                throw DbException.convert(e);
            }
        }

        /** {@inheritDoc} */
        @Override public boolean isKeyColumn(int col) {
            return keyCols > col;
        }

        /** {@inheritDoc} */
        @Override public boolean valueToString() {
            return type.valueTextIndex();
        }

        /** {@inheritDoc} */
        @Override public GridH2KeyValueRowOffheap createPointer(long ptr) {
            GridH2KeyValueRowOffheap row = rowCache.get(ptr);

            if (row != null) {
                assert row.pointer() == ptr : ptr + " " + row.pointer();

                return row;
            }

            return new GridH2KeyValueRowOffheap(this, ptr);
        }
    }

    /**
     * Object serializer.
     */
    private static class H2Serializer implements JavaObjectSerializer {
        /** {@inheritDoc} */
        @Override public byte[] serialize(Object o) throws Exception {
            return localSpi.get().marshaller.marshal(new GridIndexingEntityAdapter<>(o, null));
        }

        /** {@inheritDoc} */
        @Override public Object deserialize(byte[] bytes) throws Exception {
            return localSpi.get().marshaller.unmarshal(bytes).value();
        }
    }
}
