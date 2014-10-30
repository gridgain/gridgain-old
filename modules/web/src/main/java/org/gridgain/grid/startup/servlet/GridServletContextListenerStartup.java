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

package org.gridgain.grid.startup.servlet;

import org.gridgain.grid.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.kernal.processors.resource.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;

import javax.servlet.*;
import java.net.*;
import java.util.*;

/**
 * This class defines GridGain startup based on servlet context listener.
 * This startup can be used to start GridGain inside any web container.
 * <p>
 * This startup must be defined in {@code web.xml} file.
 * <pre name="code" class="xml">
 * &lt;listener&gt;
 *     &lt;listener-class&gt;org.gridgain.grid.startup.servlet.GridServletContextListenerStartup&lt;/listener-class&gt;
 * &lt;/listener&gt;
 *
 * &lt;context-param&gt;
 *     &lt;param-name&gt;GridGainConfigurationFilePath&lt;/param-name&gt;
 *     &lt;param-value&gt;config/default-config.xml&lt;/param-value&gt;
 * &lt;/context-param&gt;
 * </pre>
 * <p>
 * Servlet context listener based startup may be used in any web container like Tomcat, Jetty and etc.
 * Depending on the way this startup is deployed the GridGain instance can be accessed
 * by either all web applications or by only one. See web container class loading architecture:
 * <ul>
 * <li><a target=_blank href="http://tomcat.apache.org/tomcat-7.0-doc/class-loader-howto.html">http://tomcat.apache.org/tomcat-7.0-doc/class-loader-howto.html</a></li>
 * <li><a target=_blank href="http://docs.codehaus.org/display/JETTY/Classloading">http://docs.codehaus.org/display/JETTY/Classloading</a></li>
 * </ul>
 * <p>
 * <h2 class="header">Tomcat</h2>
 * There are two ways to start GridGain on Tomcat.
 * <ul>
 * <li>GridGain started when web container starts and GridGain instance is accessible only to all web applications.
 *     <ol>
 *     <li>Add GridGain libraries in Tomcat common loader.
 *         Add in file {@code $TOMCAT_HOME/conf/catalina.properties} for property {@code common.loader}
 *         the following {@code $GRIDGAIN_HOME/*.jar,$GRIDGAIN_HOME/libs/*.jar}
 *         (replace {@code $GRIDGAIN_HOME} with absolute path).
 *     </li>
 *     <li>Configure this startup in {@code $TOMCAT_HOME/conf/web.xml}
 *         <pre name="code" class="xml">
 *         &lt;listener&gt;
 *             &lt;listener-class&gt;org.gridgain.grid.startup.servlet.GridServletContextListenerStartup&lt;/listener-class&gt;
 *         &lt;/listener&gt;
 *
 *         &lt;context-param&gt;
 *             &lt;param-name&gt;GridGainConfigurationFilePath&lt;/param-name&gt;
 *             &lt;param-value&gt;config/default-config.xml&lt;/param-value&gt;
 *         &lt;/context-param&gt;
 *         </pre>
 *     </li>
 *     </ol>
 * </li>
 * <li>
 * GridGain started from WAR-file and GridGain instance is accessible only to that web application.
 * Difference with approach described above is that {@code web.xml} file and all libraries should
 * be added in WAR file without changes in Tomcat configuration files.
 * </li>
 * </ul>
 */
public class GridServletContextListenerStartup implements ServletContextListener {
    /** Configuration file path parameter name. */
    public static final String GRIDGAIN_CFG_FILE_PATH_PARAM = "GridGainConfigurationFilePath";

    /** Names of started grids. */
    private final Collection<String> gridNames = new ArrayList<>();

    /** {@inheritDoc} */
    @Override public void contextInitialized(ServletContextEvent evt) {
        ServletContext ctx = evt.getServletContext();

        String cfgFile = ctx.getInitParameter(GRIDGAIN_CFG_FILE_PATH_PARAM);

        Collection<GridConfiguration> cfgs;
        GridSpringResourceContext rsrcCtx = null;

        if (cfgFile != null) {
            URL cfgUrl = null;

            try {
                cfgUrl = evt.getServletContext().getResource("/META-INF/" + cfgFile);
            }
            catch (MalformedURLException ignored) {
                // Ignore, we still need to try with GRIDGAIN_HOME.
            }

            if (cfgUrl == null)
                // Try with GRIDGAIN_HOME and with context class loader.
                cfgUrl = U.resolveGridGainUrl(cfgFile);

            if (cfgUrl == null)
                throw new GridRuntimeException("Failed to find Spring configuration file (path provided should be " +
                    "either absolute, relative to GRIDGAIN_HOME, or relative to META-INF folder): " + cfgFile);

            GridBiTuple<Collection<GridConfiguration>, ? extends GridSpringResourceContext> t;

            try {
                t = GridGainEx.loadConfigurations(cfgUrl);
            }
            catch (GridException e) {
                throw new GridRuntimeException("Failed to load GridGain configuration.", e);
            }

            cfgs = t.get1();
            rsrcCtx  = t.get2();

            if (cfgs.isEmpty())
                throw new GridRuntimeException("Can't find grid factory configuration in: " + cfgUrl);
        }
        else
            cfgs = Collections.<GridConfiguration>singleton(new GridConfiguration());

        try {
            assert !cfgs.isEmpty();

            for (GridConfiguration cfg : cfgs) {
                assert cfg != null;

                Grid grid;

                synchronized (GridServletContextListenerStartup.class) {
                    try {
                        grid = G.grid(cfg.getGridName());
                    }
                    catch (GridIllegalStateException ignored) {
                        grid = GridGainEx.start(new GridConfiguration(cfg), rsrcCtx);
                    }
                }

                // Check if grid is not null - started properly.
                if (grid != null)
                    gridNames.add(grid.name());
            }
        }
        catch (GridException e) {
            // Stop started grids only.
            for (String name : gridNames)
                G.stop(name, true);

            throw new GridRuntimeException("Failed to start GridGain.", e);
        }
    }

    /** {@inheritDoc} */
    @Override public void contextDestroyed(ServletContextEvent evt) {
        // Stop started grids only.
        for (String name: gridNames)
            G.stop(name, true);
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridServletContextListenerStartup.class, this);
    }
}
