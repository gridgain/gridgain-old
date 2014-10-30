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

package org.gridgain.grid.kernal.processors.rest.protocols.http.jetty;

import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.*;
import org.eclipse.jetty.util.log.*;
import org.eclipse.jetty.util.thread.*;
import org.eclipse.jetty.xml.*;
import org.gridgain.grid.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.kernal.processors.rest.*;
import org.gridgain.grid.kernal.processors.rest.protocols.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.jetbrains.annotations.*;
import org.xml.sax.*;

import java.io.*;
import java.net.*;
import java.util.*;

import static org.gridgain.grid.GridSystemProperties.*;
import static org.gridgain.grid.spi.GridPortProtocol.*;

/**
 * Jetty REST protocol implementation.
 */
public class GridJettyRestProtocol extends GridRestProtocolAdapter {
    /**
     *
     */
    static {
        if (!GridSystemProperties.getBoolean(GG_JETTY_LOG_NO_OVERRIDE)) {
            Properties p = new Properties();

            p.setProperty("org.eclipse.jetty.LEVEL", "WARN");
            p.setProperty("org.eclipse.jetty.util.log.LEVEL", "OFF");
            p.setProperty("org.eclipse.jetty.util.component.LEVEL", "OFF");

            StdErrLog.setProperties(p);

            try {
                Class<?> logCls = Class.forName("org.apache.log4j.Logger");

                String ctgrJetty = "org.eclipse.jetty";                         // WARN for this category.
                String ctgrJettyUtil = "org.eclipse.jetty.util.log";            // ERROR for this...
                String ctgrJettyUtilComp = "org.eclipse.jetty.util.component";  // ...and this.

                Object logJetty = logCls.getMethod("getLogger", String.class).invoke(logCls, ctgrJetty);
                Object logJettyUtil = logCls.getMethod("getLogger", String.class).invoke(logCls, ctgrJettyUtil);
                Object logJettyUtilComp = logCls.getMethod("getLogger", String.class).invoke(logCls, ctgrJettyUtilComp);

                Class<?> lvlCls = Class.forName("org.apache.log4j.Level");

                Object warnLvl = lvlCls.getField("WARN").get(null);
                Object errLvl = lvlCls.getField("ERROR").get(null);

                logJetty.getClass().getMethod("setLevel", lvlCls).invoke(logJetty, warnLvl);
                logJettyUtil.getClass().getMethod("setLevel", lvlCls).invoke(logJetty, errLvl);
                logJettyUtilComp.getClass().getMethod("setLevel", lvlCls).invoke(logJetty, errLvl);
            }
            catch (Exception ignored) {
                // No-op.
            }
        }
    }

    /** Jetty handler. */
    private GridJettyRestHandler jettyHnd;

    /** HTTP server. */
    private Server httpSrv;

    /**
     * @param ctx Context.
     */
    public GridJettyRestProtocol(GridKernalContext ctx) {
        super(ctx);
    }

    /** {@inheritDoc} */
    @Override public String name() {
        return "Jetty REST";
    }

    /** {@inheritDoc} */
    @SuppressWarnings("BusyWait")
    @Override public void start(GridRestProtocolHandler hnd) throws GridException {
        assert ctx.config().getClientConnectionConfiguration() != null;

        InetAddress locHost;

        try {
            locHost = U.resolveLocalHost(ctx.config().getLocalHost());
        }
        catch (IOException e) {
            throw new GridException("Failed to resolve local host to bind address: " + ctx.config().getLocalHost(), e);
        }

        System.setProperty(GG_JETTY_HOST, locHost.getHostAddress());

        jettyHnd = new GridJettyRestHandler(hnd, new C1<String, Boolean>() {
            @Override public Boolean apply(String tok) {
                return F.isEmpty(secretKey) || authenticate(tok);
            }
        }, log);

        String jettyPath = config().getRestJettyPath();

        final URL cfgUrl;

        if (jettyPath == null) {
            cfgUrl = null;

            if (log.isDebugEnabled())
                log.debug("Jetty configuration file is not provided, using defaults.");
        }
        else {
            cfgUrl = U.resolveGridGainUrl(jettyPath);

            if (cfgUrl == null)
                throw new GridSpiException("Invalid Jetty configuration file: " + jettyPath);
            else if (log.isDebugEnabled())
                log.debug("Jetty configuration file: " + cfgUrl);
        }

        loadJettyConfiguration(cfgUrl);

        AbstractNetworkConnector connector = getJettyConnector();

        try {
            host = InetAddress.getByName(connector.getHost());
        }
        catch (UnknownHostException e) {
            throw new GridException("Failed to resolve Jetty host address: " + connector.getHost(), e);
        }

        int initPort = connector.getPort();

        int lastPort = initPort + config().getRestPortRange() - 1;

        for (port = initPort; port <= lastPort; port++) {
            connector.setPort(port);

            if (startJetty()) {
                if (log.isInfoEnabled())
                    log.info(startInfo());

                return;
            }
        }

        U.warn(log, "Failed to start Jetty REST server (possibly all ports in range are in use) " +
            "[firstPort=" + initPort + ", lastPort=" + lastPort + ']');
    }

    /**
     * Checks {@link GridSystemProperties#GG_JETTY_PORT} system property
     * and overrides default connector port if it present.
     * Then initializes {@code port} with the found value.
     *
     * @param con Jetty connector.
     */
    private void override(AbstractNetworkConnector con) {
        String host = System.getProperty(GG_JETTY_HOST);

        if (!F.isEmpty(host))
            con.setHost(host);

        int currPort = con.getPort();

        Integer overridePort = Integer.getInteger(GG_JETTY_PORT);

        if (overridePort != null && overridePort != 0)
            currPort = overridePort;

        con.setPort(currPort);
        port = currPort;
    }

    /**
     * @throws GridException If failed.
     * @return {@code True} if Jetty started.
     */
    @SuppressWarnings("IfMayBeConditional")
    private boolean startJetty() throws GridException {
        try {
            httpSrv.start();

            if (httpSrv.isStarted()) {
                for (Connector con : httpSrv.getConnectors()) {
                    int connPort = ((NetworkConnector)con).getPort();

                    if (connPort > 0)
                        ctx.ports().registerPort(connPort, TCP, getClass());
                }

                return true;
            }

            return  false;
        }
        catch (SocketException ignore) {
            if (log.isDebugEnabled())
                log.debug("Failed to bind HTTP server to configured port.");

            stopJetty();

            return false;
        }
        catch (MultiException e) {
            if (log.isDebugEnabled())
                log.debug("Caught multi exception: " + e);

            for (Object obj : e.getThrowables())
                if (!(obj instanceof SocketException))
                    throw new GridException("Failed to start Jetty HTTP server.", e);

            if (log.isDebugEnabled())
                log.debug("Failed to bind HTTP server to configured port.");

            stopJetty();

            return false;
        }
        catch (Exception e) {
            throw new GridException("Failed to start Jetty HTTP server.", e);
        }
    }

    /**
     * Loads jetty configuration from the given URL.
     *
     * @param cfgUrl URL to load configuration from.
     * @throws GridException if load failed.
     */
    private void loadJettyConfiguration(@Nullable URL cfgUrl) throws GridException {
        if (cfgUrl == null) {
            HttpConfiguration httpCfg = new HttpConfiguration();

            httpCfg.setSecureScheme("https");
            httpCfg.setSecurePort(8443);
            httpCfg.setSendServerVersion(true);
            httpCfg.setSendDateHeader(true);

            String srvPortStr = System.getProperty(GG_JETTY_PORT, "8080");

            int srvPort;

            try {
                srvPort = Integer.valueOf(srvPortStr);
            }
            catch (NumberFormatException ignore) {
                throw new GridException("Failed to start Jetty server because GRIDGAIN_JETTY_PORT system property " +
                    "cannot be cast to integer: " + srvPortStr);
            }

            httpSrv = new Server(new QueuedThreadPool(20, 200));

            ServerConnector srvConn = new ServerConnector(httpSrv, new HttpConnectionFactory(httpCfg));

            srvConn.setHost(System.getProperty(GG_JETTY_HOST, "localhost"));
            srvConn.setPort(srvPort);
            srvConn.setIdleTimeout(30000L);
            srvConn.setReuseAddress(true);

            httpSrv.addConnector(srvConn);

            httpSrv.setStopAtShutdown(false);
        }
        else {
            XmlConfiguration cfg;

            try {
                cfg = new XmlConfiguration(cfgUrl);
            }
            catch (FileNotFoundException e) {
                throw new GridSpiException("Failed to find configuration file: " + cfgUrl, e);
            }
            catch (SAXException e) {
                throw new GridSpiException("Failed to parse configuration file: " + cfgUrl, e);
            }
            catch (IOException e) {
                throw new GridSpiException("Failed to load configuration file: " + cfgUrl, e);
            }
            catch (Exception e) {
                throw new GridSpiException("Failed to start HTTP server with configuration file: " + cfgUrl, e);
            }

            try {
                httpSrv = (Server)cfg.configure();
            }
            catch (Exception e) {
                throw new GridException("Failed to start Jetty HTTP server.", e);
            }
        }

        assert httpSrv != null;

        httpSrv.setHandler(jettyHnd);

        override(getJettyConnector());
    }

    /**
     * Checks that the only connector configured for the current jetty instance
     * and returns it.
     *
     * @return Connector instance.
     * @throws GridException If no or more than one connectors found.
     */
    private AbstractNetworkConnector getJettyConnector() throws GridException {
        if (httpSrv.getConnectors().length == 1) {
            Connector connector = httpSrv.getConnectors()[0];

            if (!(connector instanceof AbstractNetworkConnector))
                throw new GridException("Error in jetty configuration. Jetty connector should extend " +
                    "AbstractNetworkConnector class." );

            return (AbstractNetworkConnector)connector;
        }
        else
            throw new GridException("Error in jetty configuration [connectorsFound=" +
                httpSrv.getConnectors().length + "connectorsExpected=1]");
    }

    /**
     * Stops Jetty.
     */
    private void stopJetty() {
        // Jetty does not really stop the server if port is busy.
        try {
            if (httpSrv != null) {
                // If server was successfully started, deregister ports.
                if (httpSrv.isStarted())
                    ctx.ports().deregisterPorts(getClass());

                // Record current interrupted status of calling thread.
                boolean interrupted = Thread.interrupted();

                try {
                    httpSrv.stop();
                }
                finally {
                    // Reset interrupted flag on calling thread.
                    if (interrupted)
                        Thread.currentThread().interrupt();
                }
            }
        }
        catch (InterruptedException ignored) {
            if (log.isDebugEnabled())
                log.debug("Thread has been interrupted.");

            Thread.currentThread().interrupt();
        }
        catch (Exception e) {
            U.error(log, "Failed to stop Jetty HTTP server.", e);
        }
    }

    /** {@inheritDoc} */
    @Override public void stop() {
        stopJetty();

        httpSrv = null;
        jettyHnd = null;

        if (log.isInfoEnabled())
            log.info(stopInfo());
    }

    /** {@inheritDoc} */
    @Override protected String getAddressPropertyName() {
        return GridNodeAttributes.ATTR_REST_JETTY_ADDRS;
    }

    /** {@inheritDoc} */
    @Override protected String getHostNamePropertyName() {
        return GridNodeAttributes.ATTR_REST_JETTY_HOST_NAMES;
    }

    /** {@inheritDoc} */
    @Override protected String getPortPropertyName() {
        return GridNodeAttributes.ATTR_REST_JETTY_PORT;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridJettyRestProtocol.class, this);
    }
}
