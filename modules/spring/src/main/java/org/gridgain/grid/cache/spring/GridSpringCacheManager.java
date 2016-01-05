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

package org.gridgain.grid.cache.spring;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.util.typedef.*;
import org.springframework.beans.factory.*;
import org.springframework.cache.*;

import java.util.*;

/**
 * Implementation of Spring cache abstraction based on GridGain cache.
 * <h1 class="header">Overview</h1>
 * Spring cache abstraction allows to enable caching for Java methods
 * so that the result of a method execution is stored in some storage. If
 * later the same method is called with the same set of parameters,
 * the result will be retrieved from that storage instead of actually
 * executing the method. For more information, refer to
 * <a href="http://docs.spring.io/spring/docs/current/spring-framework-reference/html/cache.html">
 * Spring Cache Abstraction documentation</a>.
 * <h1 class="header">How To Enable Caching</h1>
 * To enable caching based on GridGain cache in your Spring application,
 * you will need to do the following:
 * <ul>
 *     <li>
 *         Start a GridGain node with configured cache in the same JVM
 *         where you application is running.
 *     </li>
 *     <li>
 *         Configure {@code GridSpringCacheManager} as a cache provider
 *         in Spring application context.
 *     </li>
 * </ul>
 * {@code GridSpringCacheManager} can start a node itself on its startup
 * based on provided GridGain configuration. You can provide path to a
 * Spring configuration XML file, like below (path can be absolute or
 * relative to {@code GRIDGAIN_HOME}):
 * <pre name="code" class="xml">
 * &lt;beans xmlns="http://www.springframework.org/schema/beans"
 *        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
 *        xmlns:cache="http://www.springframework.org/schema/cache"
 *        xsi:schemaLocation="
 *         http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd
 *         http://www.springframework.org/schema/cache http://www.springframework.org/schema/cache/spring-cache.xsd"&gt;
 *     &lt;-- Provide configuration file path. --&gt;
 *     &lt;bean id="cacheManager" class="org.gridgain.grid.cache.spring.GridSpringCacheManager"&gt;
 *         &lt;property name="configurationPath" value="examples/config/spring-cache.xml"/&gt;
 *     &lt;/bean>
 *
 *     &lt;-- Use annotation-driven caching configuration. --&gt;
 *     &lt;cache:annotation-driven/&gt;
 * &lt;/beans&gt;
 * </pre>
 * Or you can provide a {@link GridConfiguration} bean, like below:
 * <pre name="code" class="xml">
 * &lt;beans xmlns="http://www.springframework.org/schema/beans"
 *        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
 *        xmlns:cache="http://www.springframework.org/schema/cache"
 *        xsi:schemaLocation="
 *         http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd
 *         http://www.springframework.org/schema/cache http://www.springframework.org/schema/cache/spring-cache.xsd"&gt;
 *     &lt;-- Provide configuration bean. --&gt;
 *     &lt;bean id="cacheManager" class="org.gridgain.grid.cache.spring.GridSpringCacheManager"&gt;
 *         &lt;property name="configuration"&gt;
 *             &lt;bean id="gridCfg" class="org.gridgain.grid.GridConfiguration"&gt;
 *                 ...
 *             &lt;/bean&gt;
 *         &lt;/property&gt;
 *     &lt;/bean&gt;
 *
 *     &lt;-- Use annotation-driven caching configuration. --&gt;
 *     &lt;cache:annotation-driven/&gt;
 * &lt;/beans&gt;
 * </pre>
 * Note that providing both configuration path and configuration bean is illegal
 * and results in {@link IllegalArgumentException}.
 * <p>
 * If you already have GridGain node running within your application,
 * simply provide correct Grid name, like below (if there is no Grid
 * instance with such name, exception will be thrown):
 * <pre name="code" class="xml">
 * &lt;beans xmlns="http://www.springframework.org/schema/beans"
 *        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
 *        xmlns:cache="http://www.springframework.org/schema/cache"
 *        xsi:schemaLocation="
 *         http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd
 *         http://www.springframework.org/schema/cache http://www.springframework.org/schema/cache/spring-cache.xsd"&gt;
 *     &lt;-- Provide Grid name. --&gt;
 *     &lt;bean id="cacheManager" class="org.gridgain.grid.cache.spring.GridSpringCacheManager"&gt;
 *         &lt;property name="gridName" value="myGrid"/&gt;
 *     &lt;/bean>
 *
 *     &lt;-- Use annotation-driven caching configuration. --&gt;
 *     &lt;cache:annotation-driven/&gt;
 * &lt;/beans&gt;
 * </pre>
 * This can be used, for example, when you are running your application
 * in a J2EE Web container and use {@gglink org.gridgain.grid.startup.servlet.GridServletContextListenerStartup}
 * for node startup.
 * <p>
 * If neither {@link #setConfigurationPath(String) configurationPath},
 * {@link #setConfiguration(GridConfiguration) configuration}, nor
 * {@link #setGridName(String) gridName} are provided, cache manager
 * will try to use default Grid instance (the one with the {@code null}
 * name). If it doesn't exist, exception will be thrown.
 * <h1>Starting Remote Nodes</h1>
 * Remember that the node started inside your application is an entry point
 * to the whole topology it connects to. You can start as many remote standalone
 * nodes as you need using {@code bin/ggstart.{sh|bat}} scripts provided in
 * GridGain distribution, and all these nodes will participate
 * in caching data.
 */
public class GridSpringCacheManager implements CacheManager, InitializingBean {
    /** Grid configuration file path. */
    private String cfgPath;

    /** Grid configuration. */
    private GridConfiguration cfg;

    /** Grid name. */
    private String gridName;

    /** Grid instance. */
    protected GridEx grid;

    /**
     * Gets configuration file path.
     *
     * @return Grid configuration file path.
     */
    public String getConfigurationPath() {
        return cfgPath;
    }

    /**
     * Sets configuration file path.
     *
     * @param cfgPath Grid configuration file path.
     */
    public void setConfigurationPath(String cfgPath) {
        this.cfgPath = cfgPath;
    }

    /**
     * Gets configuration bean.
     *
     * @return Grid configuration bean.
     */
    public GridConfiguration getConfiguration() {
        return cfg;
    }

    /**
     * Sets configuration bean.
     *
     * @param cfg Grid configuration bean.
     */
    public void setConfiguration(GridConfiguration cfg) {
        this.cfg = cfg;
    }

    /**
     * Gets grid name.
     *
     * @return Grid name.
     */
    public String getGridName() {
        return gridName;
    }

    /**
     * Sets grid name.
     *
     * @param gridName Grid name.
     */
    public void setGridName(String gridName) {
        this.gridName = gridName;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("IfMayBeConditional")
    @Override public void afterPropertiesSet() throws Exception {
        assert grid == null;

        if (cfgPath != null && cfg != null) {
            throw new IllegalArgumentException("Both 'configurationPath' and 'configuration' are " +
                "provided. Set only one of these properties if you need to start a GridGain node inside of " +
                "GridSpringCacheManager. If you already have a node running, omit both of them and set" +
                "'gridName' property.");
        }

        if (cfgPath != null)
            grid = (GridEx)GridGain.start(cfgPath);
        else if (cfg != null)
            grid = (GridEx)GridGain.start(cfg);
        else
            grid = (GridEx)GridGain.grid(gridName);
    }

    /** {@inheritDoc} */
    @Override public Cache getCache(String name) {
        assert grid != null;

        try {
            return new GridSpringCache(grid.log().getLogger(getClass()), name, grid.cache(name), null);
        }
        catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override public Collection<String> getCacheNames() {
        assert grid != null;

        return F.viewReadOnly(grid.caches(), new GridClosure<GridCache<?, ?>, String>() {
            @Override public String apply(GridCache<?, ?> c) {
                return c.name();
            }
        });
    }
}
