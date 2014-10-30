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

package org.gridgain.client.router.impl;

import org.gridgain.client.*;
import org.gridgain.client.router.*;
import org.gridgain.client.ssl.*;
import org.gridgain.grid.*;
import org.gridgain.grid.kernal.processors.rest.client.message.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.logger.java.*;
import org.gridgain.grid.util.nio.*;
import org.gridgain.grid.util.nio.ssl.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.jetbrains.annotations.*;

import javax.management.*;
import javax.net.ssl.*;
import java.lang.management.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.*;
import java.util.*;

/**
 * Wrapper class for router process.
 */
public class GridTcpRouterImpl implements GridTcpRouter, GridTcpRouterMBean, GridLifecycleAware {
    /** */
    private static final String ENT_NIO_LSNR_CLS = "org.gridgain.client.router.impl.GridTcpRouterNioListenerEntImpl";

    /** Id. */
    private final UUID id = UUID.randomUUID();

    /** Configuration. */
    private final GridTcpRouterConfiguration cfg;

    /** Logger. */
    private final GridLogger log;

    /** Server. */
    private GridNioServer<GridClientMessage> srv;

    /** Client. */
    private GridRouterClientImpl client;

    /** MBean name. */
    private ObjectName mbeanName;

    /** NIO parser. */
    private volatile GridTcpRouterNioParser parser;

    /** Host where server was actually bound. */
    private volatile String bindHost;

    /** Port where server was actually bound. */
    private volatile int bindPort;

    /**
     * Creates new router instance.
     *
     * @param cfg Router configuration.
     */
    public GridTcpRouterImpl(GridTcpRouterConfiguration cfg) {
        this.cfg = cfg;

        log = cfg.getLogger() != null ?
            cfg.getLogger().getLogger(getClass()) : new GridJavaLogger().getLogger(getClass());
    }

    /**
     * Starts router.
     *
     * @throws GridException If failed.
     */
    @Override public void start() throws GridException {
        try {
            client = createClient(cfg);
        }
        catch (GridClientException e) {
            throw new GridException("Failed to initialise embedded client.", e);
        }

        GridNioServerListener<GridClientMessage> lsnr;

        try {
            Class<?> cls = Class.forName(ENT_NIO_LSNR_CLS);

            Constructor<?> cons = cls.getDeclaredConstructor(GridLogger.class, GridRouterClientImpl.class);

            cons.setAccessible(true);

            lsnr = (GridNioServerListener<GridClientMessage>)cons.newInstance(log, client);
        }
        catch (ClassNotFoundException ignored) {
            lsnr = new GridTcpRouterNioListenerOsImpl(log, client);
        }
        catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new GridException("Failed to create NIO listener.", e);
        }

        parser = new GridTcpRouterNioParser();

        final InetAddress hostAddr;

        try {
            hostAddr = InetAddress.getByName(cfg.getHost());
        }
        catch (UnknownHostException e) {
            throw new GridException("Failed to resolve grid address for configured host: " + cfg.getHost(), e);
        }

        SSLContext sslCtx;

        try {
            GridSslContextFactory sslCtxFactory = cfg.getSslContextFactory();

            sslCtx = sslCtxFactory == null ? null : sslCtxFactory.createSslContext();
        }
        catch (SSLException e) {
            throw new GridException("Failed to create SSL context.", e);
        }

        for (int port = cfg.getPort(), last = port + cfg.getPortRange(); port <= last; port++) {
            if (startTcpServer(hostAddr, port, lsnr, parser, cfg.isNoDelay(), sslCtx, cfg.isSslClientAuth(),
                cfg.isSslClientAuth())) {
                if (log.isInfoEnabled())
                    log.info("TCP router successfully started for endpoint: " + hostAddr.getHostAddress() + ":" + port);

                bindPort = port;
                bindHost = hostAddr.getHostName();

                break;
            }
            else
                U.warn(log, "TCP REST router failed to start on endpoint: " + hostAddr.getHostAddress() + ":" + port +
                    ". Will try next port within allowed port range.");
        }

        if (bindPort == 0)
            throw new GridException("Failed to bind TCP router server (possibly all ports in range " +
                "are in use) [firstPort=" + cfg.getPort() + ", lastPort=" + (cfg.getPort() + cfg.getPortRange()) +
                ", addr=" + hostAddr + ']');

        try {
            ObjectName objName = U.registerMBean(
                ManagementFactory.getPlatformMBeanServer(),
                "Router",
                "TCP Router " + id,
                getClass().getSimpleName(),
                this,
                GridTcpRouterMBean.class);

            if (log.isDebugEnabled())
                log.debug("Registered MBean: " + objName);

            mbeanName = objName;
        }
        catch (JMException e) {
            U.error(log, "Failed to register MBean.", e);
        }
    }

    /**
     * Stops this router.
     */
    @Override public void stop() {
        if (srv != null)
            srv.stop();

        if (client != null)
            client.stop(true);

        if (mbeanName != null)
            try {
                ManagementFactory.getPlatformMBeanServer().unregisterMBean(mbeanName);

                if (log.isDebugEnabled())
                    log.debug("Unregistered MBean: " + mbeanName);
            }
            catch (JMException e) {
                U.error(log, "Failed to unregister MBean.", e);
            }

        if (log.isInfoEnabled())
            log.info("TCP router successfully stopped.");
    }

    /**
     * Tries to start server with given parameters.
     *
     * @param hostAddr Host on which server should be bound.
     * @param port Port on which server should be bound.
     * @param lsnr Server message listener.
     * @param parser Server message parser.
     * @param tcpNoDelay Flag indicating whether TCP_NODELAY flag should be set for accepted connections.
     * @param sslCtx SSL context in case if SSL is enabled.
     * @param wantClientAuth Whether client will be requested for authentication.
     * @param needClientAuth Whether client is required to be authenticated.
     * @return {@code True} if server successfully started, {@code false} if port is used and
     *      server was unable to start.
     */
    private boolean startTcpServer(InetAddress hostAddr, int port, GridNioServerListener<GridClientMessage> lsnr,
        GridNioParser parser, boolean tcpNoDelay, @Nullable SSLContext sslCtx, boolean wantClientAuth,
        boolean needClientAuth) {
        try {
            GridNioFilter codec = new GridNioCodecFilter(parser, log, false);

            // This name is required to be unique in order to avoid collisions with
            // ThreadWorkerGroups running in the same JVM by other routers/nodes.
            String gridName = "router-" + id;

            GridNioFilter[] filters;

            if (sslCtx != null) {
                GridNioSslFilter sslFilter = new GridNioSslFilter(sslCtx, log);

                sslFilter.wantClientAuth(wantClientAuth);

                sslFilter.needClientAuth(needClientAuth);

                filters = new GridNioFilter[] { codec, sslFilter };
            }
            else
                filters = new GridNioFilter[] { codec };

            srv = GridNioServer.<GridClientMessage>builder()
                .address(hostAddr)
                .port(port)
                .listener(lsnr)
                .logger(log)
                .selectorCount(Runtime.getRuntime().availableProcessors())
                .gridName(gridName)
                .tcpNoDelay(tcpNoDelay)
                .directBuffer(false)
                .byteOrder(ByteOrder.nativeOrder())
                .socketSendBufferSize(0)
                .socketReceiveBufferSize(0)
                .sendQueueLimit(0)
                .filters(filters)
                .idleTimeout(cfg.getIdleTimeout())
                .build();

            srv.start();

            return true;
        }
        catch (GridException e) {
            if (log.isDebugEnabled())
                log.debug("Failed to start TCP router protocol on port " + port + ": " + e.getMessage());

            srv = null;

            return false;
        }
    }

    /**
     * Creates a client for forwarding messages to the grid.
     *
     * @param routerCfg Router configuration.
     * @return Client instance.
     * @throws GridClientException If client creation failed.
     */
    private GridRouterClientImpl createClient(GridTcpRouterConfiguration routerCfg) throws GridClientException {
        UUID clientId = UUID.randomUUID();

        return new GridRouterClientImpl(clientId, routerCfg);
    }

    /** {@inheritDoc} */
    @Override public String getHost() {
        return bindHost;
    }

    /** {@inheritDoc} */
    @Override public int getPort() {
        return bindPort;
    }

    /** {@inheritDoc} */
    @Override public boolean isSslEnabled() {
        return cfg.getSslContextFactory() != null;
    }

    /** {@inheritDoc} */
    @Override public boolean isSslClientAuth() {
        return cfg.isSslClientAuth();
    }

    /** {@inheritDoc} */
    @Override public Collection<String> getServers() {
        return cfg.getServers();
    }

    /** {@inheritDoc} */
    @Override public UUID id() {
        return id;
    }

    /** {@inheritDoc} */
    @Override public GridTcpRouterConfiguration configuration() {
        return cfg;
    }

    /** {@inheritDoc} */
    @Override public long getReceivedCount() {
        return parser != null ? parser.getReceivedCount() : 0;
    }

    /** {@inheritDoc} */
    @Override public long getSendCount() {
        return parser != null ? parser.getSendCount() : 0;
    }

    /** {@inheritDoc} */
    @Override public int hashCode() {
        return id.hashCode();
    }

    /** {@inheritDoc} */
    @Override public boolean equals(Object o) {
        if (o == this)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        GridTcpRouterImpl that = (GridTcpRouterImpl)o;

        return id.equals(that.id);
    }
}
