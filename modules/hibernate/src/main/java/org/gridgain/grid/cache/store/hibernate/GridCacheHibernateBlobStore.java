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

package org.gridgain.grid.cache.store.hibernate;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.cache.store.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.marshaller.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.grid.util.tostring.*;
import org.hibernate.*;
import org.hibernate.cfg.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * {@link GridCacheStore} implementation backed by Hibernate. This implementation
 * stores objects in underlying database in {@code BLOB} format.
 * <h2 class="header">Configuration</h2>
 * Either {@link #setSessionFactory(SessionFactory)} or
 * {@link #setHibernateConfigurationPath(String)} or
 * {@link #setHibernateProperties(Properties)} should be set.
 * <p>
 * If session factory is provided it should contain
 * {@link GridCacheHibernateBlobStoreEntry} persistent class (via provided
 * mapping file {@code GridCacheHibernateStoreEntry.hbm.xml} or by
 * adding {@link GridCacheHibernateBlobStoreEntry} to annotated classes
 * of session factory.
 * <p>
 * Path to hibernate configuration may be either an URL or a file path or
 * a classpath resource. This configuration file should include provided
 * mapping {@code GridCacheHibernateStoreEntry.hbm.xml} or include annotated
 * class {@link GridCacheHibernateBlobStoreEntry}.
 * <p>
 * If hibernate properties are provided, mapping
 * {@code GridCacheHibernateStoreEntry.hbm.xml} is included automatically.
 *
 * <h2 class="header">Java Example</h2>
 * In this example existing session factory is provided.
 * <pre name="code" class="java">
 *     ...
 *     GridCacheHibernateBlobStore&lt;String, String&gt; store = new GridCacheHibernateBlobStore&lt;String, String&gt;();
 *
 *     store.setSessionFactory(sesFactory);
 *     ...
 * </pre>
 *
 * <h2 class="header">Spring Example (using Spring ORM)</h2>
 * <pre name="code" class="xml">
 *   ...
 *   &lt;bean id=&quot;cache.hibernate.store&quot;
 *       class=&quot;org.gridgain.grid.cache.store.hibernate.GridCacheHibernateBlobStore&quot;&gt;
 *       &lt;property name=&quot;sessionFactory&quot;&gt;
 *           &lt;bean class=&quot;org.springframework.orm.hibernate3.LocalSessionFactoryBean&quot;&gt;
 *               &lt;property name=&quot;hibernateProperties&quot;&gt;
 *                   &lt;value&gt;
 *                       connection.url=jdbc:h2:mem:
 *                       show_sql=true
 *                       hbm2ddl.auto=true
 *                       hibernate.dialect=org.hibernate.dialect.H2Dialect
 *                   &lt;/value&gt;
 *               &lt;/property&gt;
 *               &lt;property name=&quot;mappingResources&quot;&gt;
 *                   &lt;list&gt;
 *                       &lt;value&gt;
 *                           org/gridgain/grid/cache/store/hibernate/GridCacheHibernateBlobStoreEntry.hbm.xml
 *                       &lt;/value&gt;
 *                   &lt;/list&gt;
 *               &lt;/property&gt;
 *           &lt;/bean&gt;
 *       &lt;/property&gt;
 *   &lt;/bean&gt;
 *   ...
 * </pre>
 *
 * <h2 class="header">Spring Example (using Spring ORM and persistent annotations)</h2>
 * <pre name="code" class="xml">
 *     ...
 *     &lt;bean id=&quot;cache.hibernate.store1&quot;
 *         class=&quot;org.gridgain.grid.cache.store.hibernate.GridCacheHibernateBlobStore&quot;&gt;
 *         &lt;property name=&quot;sessionFactory&quot;&gt;
 *             &lt;bean class=&quot;org.springframework.orm.hibernate3.annotation.AnnotationSessionFactoryBean&quot;&gt;
 *                 &lt;property name=&quot;hibernateProperties&quot;&gt;
 *                     &lt;value&gt;
 *                         connection.url=jdbc:h2:mem:
 *                         show_sql=true
 *                         hbm2ddl.auto=true
 *                         hibernate.dialect=org.hibernate.dialect.H2Dialect
 *                     &lt;/value&gt;
 *                 &lt;/property&gt;
 *                 &lt;property name=&quot;annotatedClasses&quot;&gt;
 *                     &lt;list&gt;
 *                         &lt;value&gt;
 *                             org.gridgain.grid.cache.store.hibernate.GridCacheHibernateBlobStoreEntry
 *                         &lt;/value&gt;
 *                     &lt;/list&gt;
 *                 &lt;/property&gt;
 *             &lt;/bean&gt;
 *         &lt;/property&gt;
 *     &lt;/bean&gt;
 *     ...
 * </pre>
 *
 * <h2 class="header">Spring Example</h2>
 * <pre name="code" class="xml">
 *     ...
 *     &lt;bean id=&quot;cache.hibernate.store2&quot;
 *         class=&quot;org.gridgain.grid.cache.store.hibernate.GridCacheHibernateBlobStore&quot;&gt;
 *         &lt;property name=&quot;hibernateProperties&quot;&gt;
 *             &lt;props&gt;
 *                 &lt;prop key=&quot;connection.url&quot;&gt;jdbc:h2:mem:&lt;/prop&gt;
 *                 &lt;prop key=&quot;hbm2ddl.auto&quot;&gt;update&lt;/prop&gt;
 *                 &lt;prop key=&quot;show_sql&quot;&gt;true&lt;/prop&gt;
 *             &lt;/props&gt;
 *         &lt;/property&gt;
 *     &lt;/bean&gt;
 *     ...
 * </pre>
 * <p>
 * <img src="http://www.gridgain.com/images/spring-small.png">
 * <br>
 * For information about Spring framework visit <a href="http://www.springframework.org/">www.springframework.org</a>
 */
public class GridCacheHibernateBlobStore<K, V> extends GridCacheStoreAdapter<K, V> {
    /**
     * Default connection URL
     * (value is <tt>jdbc:h2:mem:hibernateCacheStore;DB_CLOSE_DELAY=-1;DEFAULT_LOCK_TIMEOUT=5000</tt>).
     */
    public static final String DFLT_CONN_URL = "jdbc:h2:mem:hibernateCacheStore;DB_CLOSE_DELAY=-1;" +
        "DEFAULT_LOCK_TIMEOUT=5000";

    /** Default show SQL property value (value is <tt>true</tt>). */
    public static final String DFLT_SHOW_SQL = "true";

    /** Default <tt>hibernate.hbm2ddl.auto</tt> property value (value is <tt>true</tt>). */
    public static final String DFLT_HBM2DDL_AUTO = "update";

    /** Session attribute name. */
    private static final String ATTR_SES = "HIBERNATE_STORE_SESSION";

    /** Name of Hibarname mapping resource. */
    private static final String MAPPING_RESOURCE = "org/gridgain/grid/cache/store/hibernate/GridCacheHibernateBlobStoreEntry.hbm.xml";

    /** Init guard. */
    @GridToStringExclude
    private final AtomicBoolean initGuard = new AtomicBoolean();

    /** Init latch. */
    @GridToStringExclude
    private final CountDownLatch initLatch = new CountDownLatch(1);

    /** Hibernate properties. */
    @GridToStringExclude
    private Properties hibernateProps;

    /** Session factory. */
    @GridToStringExclude
    private SessionFactory sesFactory;

    /** Path to hibernate configuration file. */
    private String hibernateCfgPath;

    /** Log. */
    @GridLoggerResource
    private GridLogger log;

    /** Marshaller. */
    @GridMarshallerResource
    private GridMarshaller marsh;

    /** {@inheritDoc} */
    @SuppressWarnings({"unchecked", "RedundantTypeArguments"})
    @Override public V load(@Nullable GridCacheTx tx, K key) throws GridException {
        init();

        if (log.isDebugEnabled())
            log.debug("Store load [key=" + key + ", tx=" + tx + ']');

        Session ses = session(tx);

        try {
            GridCacheHibernateBlobStoreEntry entry = (GridCacheHibernateBlobStoreEntry)
                ses.get(GridCacheHibernateBlobStoreEntry.class, toBytes(key));

            if (entry == null)
                return null;

            return fromBytes(entry.getValue());
        }
        catch (HibernateException e) {
            rollback(ses, tx);

            throw new GridException("Failed to load value from cache store with key: " + key, e);
        }
        finally {
            end(ses, tx);
        }
    }

    /** {@inheritDoc} */
    @Override public void put(@Nullable GridCacheTx tx, K key, @Nullable V val)
        throws GridException {
        init();

        if (log.isDebugEnabled())
            log.debug("Store put [key=" + key + ", val=" + val + ", tx=" + tx + ']');

        if (val == null) {
            remove(tx, key);

            return;
        }

        Session ses = session(tx);

        try {
            GridCacheHibernateBlobStoreEntry entry = new GridCacheHibernateBlobStoreEntry(toBytes(key), toBytes(val));

            ses.saveOrUpdate(entry);
        }
        catch (HibernateException e) {
            rollback(ses, tx);

            throw new GridException("Failed to put value to cache store [key=" + key + ", val" + val + "]", e);
        }
        finally {
            end(ses, tx);
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings({"JpaQueryApiInspection", "JpaQlInspection"})
    @Override public void remove(@Nullable GridCacheTx tx, K key) throws GridException {
        init();

        if (log.isDebugEnabled())
            log.debug("Store remove [key=" + key + ", tx=" + tx + ']');

        Session ses = session(tx);

        try {
            Object obj = ses.get(GridCacheHibernateBlobStoreEntry.class, toBytes(key));

            if (obj != null)
                ses.delete(obj);
        }
        catch (HibernateException e) {
            rollback(ses, tx);

            throw new GridException("Failed to remove value from cache store with key: " + key, e);
        }
        finally {
            end(ses, tx);
        }
    }

    /**
     * Rolls back hibernate session.
     *
     * @param ses Hibernate session.
     * @param tx Cache ongoing transaction.
     */
    private void rollback(SharedSessionContract ses, GridCacheTx tx) {
        // Rollback only if there is no cache transaction,
        // otherwise txEnd() will do all required work.
        if (tx == null) {
            Transaction hTx = ses.getTransaction();

            if (hTx != null && hTx.isActive())
                hTx.rollback();
        }
    }

    /**
     * Ends hibernate session.
     *
     * @param ses Hibernate session.
     * @param tx Cache ongoing transaction.
     */
    private void end(Session ses, GridCacheTx tx) {
        // Commit only if there is no cache transaction,
        // otherwise txEnd() will do all required work.
        if (tx == null) {
            Transaction hTx = ses.getTransaction();

            if (hTx != null && hTx.isActive())
                hTx.commit();

            ses.close();
        }
    }

    /** {@inheritDoc} */
    @Override public void txEnd(GridCacheTx tx, boolean commit) throws GridException {
        init();

        Session ses = tx.removeMeta(ATTR_SES);

        if (ses != null) {
            Transaction hTx = ses.getTransaction();

            if (hTx != null) {
                try {
                    if (commit) {
                        ses.flush();

                        hTx.commit();
                    }
                    else
                        hTx.rollback();

                    if (log.isDebugEnabled())
                        log.debug("Transaction ended [xid=" + tx.xid() + ", commit=" + commit + ']');
                }
                catch (HibernateException e) {
                    throw new GridException("Failed to end transaction [xid=" + tx.xid() +
                        ", commit=" + commit + ']', e);
                }
                finally {
                    ses.close();
                }
            }
        }
    }

    /**
     * Gets Hibernate session.
     *
     * @param tx Cache transaction.
     * @return Session.
     */
    Session session(@Nullable GridCacheTx tx) {
        Session ses;

        if (tx != null) {
            ses = tx.meta(ATTR_SES);

            if (ses == null) {
                ses = sesFactory.openSession();

                ses.beginTransaction();

                // Store session in transaction metadata, so it can be accessed
                // for other operations on the same transaction.
                tx.addMeta(ATTR_SES, ses);

                if (log.isDebugEnabled())
                    log.debug("Hibernate session open [ses=" + ses + ", tx=" + tx.xid() + "]");
            }
        }
        else {
            ses = sesFactory.openSession();

            ses.beginTransaction();
        }

        return ses;
    }

    /**
     * Sets session factory.
     *
     * @param sesFactory Session factory.
     */
    public void setSessionFactory(SessionFactory sesFactory) {
        this.sesFactory = sesFactory;
    }

    /**
     * Sets hibernate configuration path.
     * <p>
     * This may be either URL or file path or classpath resource.
     *
     * @param hibernateCfgPath URL or file path or classpath resource
     *      pointing to hibernate configuration XML file.
     */
    public void setHibernateConfigurationPath(String hibernateCfgPath) {
        this.hibernateCfgPath = hibernateCfgPath;
    }

    /**
     * Sets Hibernate properties.
     *
     * @param hibernateProps Hibernate properties.
     */
    public void setHibernateProperties(Properties hibernateProps) {
        this.hibernateProps = hibernateProps;
    }

    /**
     * Initializes store.
     *
     * @throws GridException If failed to initialize.
     */
    private void init() throws GridException {
        if (initGuard.compareAndSet(false, true)) {
            if (log.isDebugEnabled())
                log.debug("Initializing cache store.");

            try {
                if (sesFactory != null)
                    // Session factory has been provided - nothing to do.
                    return;

                if (!F.isEmpty(hibernateCfgPath)) {
                    try {
                        URL url = new URL(hibernateCfgPath);

                        sesFactory = new Configuration().configure(url).buildSessionFactory();

                        if (log.isDebugEnabled())
                            log.debug("Configured session factory using URL: " + url);

                        // Session factory has been successfully initialized.
                        return;
                    }
                    catch (MalformedURLException e) {
                        if (log.isDebugEnabled())
                            log.debug("Caught malformed URL exception: " + e.getMessage());
                    }

                    // Provided path is not a valid URL. File?
                    File cfgFile = new File(hibernateCfgPath);

                    if (cfgFile.exists()) {
                        sesFactory = new Configuration().configure(cfgFile).buildSessionFactory();

                        if (log.isDebugEnabled())
                            log.debug("Configured session factory using file: " + hibernateCfgPath);

                        // Session factory has been successfully initialized.
                        return;
                    }

                    // Provided path is not a file. Classpath resource?
                    sesFactory = new Configuration().configure(hibernateCfgPath).buildSessionFactory();

                    if (log.isDebugEnabled())
                        log.debug("Configured session factory using classpath resource: " + hibernateCfgPath);
                }
                else {
                    if (hibernateProps == null) {
                        U.warn(log, "No Hibernate configuration has been provided for store (will use default).");

                        hibernateProps = new Properties();

                        hibernateProps.setProperty("hibernate.connection.url", DFLT_CONN_URL);
                        hibernateProps.setProperty("hibernate.show_sql", DFLT_SHOW_SQL);
                        hibernateProps.setProperty("hibernate.hbm2ddl.auto", DFLT_HBM2DDL_AUTO);
                    }

                    Configuration cfg = new Configuration();

                    cfg.setProperties(hibernateProps);

                    assert resourceAvailable(MAPPING_RESOURCE) : MAPPING_RESOURCE;

                    cfg.addResource(MAPPING_RESOURCE);

                    sesFactory = cfg.buildSessionFactory();

                    if (log.isDebugEnabled())
                        log.debug("Configured session factory using properties: " + hibernateProps);
                }
            }
            catch (HibernateException e) {
                throw new GridException("Failed to initialize store.", e);
            }
            finally {
                initLatch.countDown();
            }
        }
        else if (initLatch.getCount() > 0)
            U.await(initLatch);

        if (sesFactory == null)
            throw new GridException("Cache store was not properly initialized.");
    }

    /**
     * Checks availability of a classpath resource.
     *
     * @param name Resource name.
     * @return {@code true} if resource is available and ready for read, {@code false} otherwise.
     */
    private boolean resourceAvailable(String name) {
        InputStream cfgStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(name);

        if (cfgStream == null) {
            log.error("Classpath resource not found: " + name);

            return false;
        }

        try {
            // Read a single byte to force actual content access by JVM.
            cfgStream.read();

            return true;
        }
        catch (IOException e) {
            log.error("Failed to read classpath resource: " + name, e);

            return false;
        }
        finally {
            U.close(cfgStream, log);
        }
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridCacheHibernateBlobStore.class, this);
    }

    /**
     * Serialize object to byte array using marshaller.
     *
     * @param obj Object to convert to byte array.
     * @return Byte array.
     * @throws GridException If failed to convert.
     */
    protected byte[] toBytes(Object obj) throws GridException {
        return marsh.marshal(obj);
    }

    /**
     * Deserialize object from byte array using marshaller.
     *
     * @param bytes Bytes to deserialize.
     * @param <X> Result object type.
     * @return Deserialized object.
     * @throws GridException If failed.
     */
    protected <X> X fromBytes(byte[] bytes) throws GridException {
        if (bytes == null || bytes.length == 0)
            return null;

        return marsh.unmarshal(bytes, getClass().getClassLoader());
    }
}
