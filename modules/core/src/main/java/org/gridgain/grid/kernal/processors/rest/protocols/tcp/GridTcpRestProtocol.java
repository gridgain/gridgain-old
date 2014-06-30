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

package org.gridgain.grid.kernal.processors.rest.protocols.tcp;

import org.gridgain.client.marshaller.*;
import org.gridgain.client.ssl.*;
import org.gridgain.grid.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.kernal.processors.rest.*;
import org.gridgain.grid.kernal.processors.rest.client.message.*;
import org.gridgain.grid.kernal.processors.rest.protocols.*;
import org.gridgain.grid.marshaller.*;
import org.gridgain.grid.marshaller.jdk.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.util.direct.*;
import org.gridgain.grid.util.nio.*;
import org.gridgain.grid.util.nio.ssl.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.jetbrains.annotations.*;

import javax.net.ssl.*;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;

import static org.gridgain.grid.util.nio.GridNioSessionMetaKey.*;

/**
 * TCP binary protocol implementation.
 */
public class GridTcpRestProtocol extends GridRestProtocolAdapter {
    /** Server. */
    private GridNioServer<GridClientMessage> srv;

    /** JDK marshaller. */
    private final GridMarshaller jdkMarshaller = new GridJdkMarshaller();

    /** Protobuf marshaller. */
    private final GridClientMarshaller protobufMarshaller;

    /** Message reader. */
    private final GridNioMessageReader msgReader = new GridNioMessageReader() {
        @Override public boolean read(@Nullable UUID nodeId, GridTcpCommunicationMessageAdapter msg, ByteBuffer buf) {
            assert msg != null;
            assert buf != null;

            msg.messageReader(this, nodeId);

            return msg.readFrom(buf);
        }
    };

    /** Message writer. */
    private final GridNioMessageWriter msgWriter = new GridNioMessageWriter() {
        @Override public boolean write(@Nullable UUID nodeId, GridTcpCommunicationMessageAdapter msg, ByteBuffer buf) {
            assert msg != null;
            assert buf != null;

            msg.messageWriter(this, nodeId);

            return msg.writeTo(buf);
        }

        @Override public int writeFully(@Nullable UUID nodeId, GridTcpCommunicationMessageAdapter msg, OutputStream out,
            ByteBuffer buf) throws IOException {
            assert msg != null;
            assert out != null;
            assert buf != null;
            assert buf.hasArray();

            msg.messageWriter(this, nodeId);

            boolean finished = false;
            int cnt = 0;

            while (!finished) {
                finished = msg.writeTo(buf);

                out.write(buf.array(), 0, buf.position());

                cnt += buf.position();

                buf.clear();
            }

            return cnt;
        }
    };

    /** @param ctx Context. */
    public GridTcpRestProtocol(GridKernalContext ctx) {
        super(ctx);

        protobufMarshaller = U.createProtobufMarshaller(log);
    }

    /**
     * @return JDK marshaller.
     */
    GridMarshaller jdkMarshaller() {
        return jdkMarshaller;
    }

    /**
     * Returns marshaller from session, if no marshaller found - init it with default.
     *
     * @param ses Current session.
     * @return Current session's marshaller.
     * @throws GridException If marshaller can't be found.
     */
    GridClientMarshaller marshaller(GridNioSession ses) throws GridException {
        GridClientMarshaller marsh = ses.meta(MARSHALLER.ordinal());

        if (marsh == null) {
            U.warn(log, "No marshaller defined for NIO session, using PROTOBUF as default [ses=" + ses + ']');

            if (protobufMarshaller == null)
                throw new GridException("Failed to use Protobuf marshaller (session will be closed). " +
                    "Is gridgain-protobuf module added to classpath?");

            marsh = protobufMarshaller;

            ses.addMeta(MARSHALLER.ordinal(), marsh);
        }

        return marsh;
    }

    /** {@inheritDoc} */
    @Override public String name() {
        return "TCP binary";
    }

    /** {@inheritDoc} */
    @SuppressWarnings("BusyWait")
    @Override public void start(final GridRestProtocolHandler hnd) throws GridException {
        assert hnd != null;

        GridConfiguration cfg = ctx.config();

        GridNioServerListener<GridClientMessage> lsnr =
            new GridTcpRestNioListener(log, this, hnd, ctx, protobufMarshaller);

        GridNioParser parser = new GridTcpRestDirectParser(this, msgReader);

        try {
            host = resolveRestTcpHost(cfg);

            SSLContext sslCtx = null;

            if (cfg.isRestTcpSslEnabled()) {
                GridSslContextFactory factory = cfg.getRestTcpSslContextFactory();

                if (factory == null)
                    // Thrown SSL exception instead of GridException for writing correct warning message into log.
                    throw new SSLException("SSL is enabled, but SSL context factory is not specified.");

                sslCtx = factory.createSslContext();
            }

            int lastPort = cfg.getRestTcpPort() + cfg.getRestPortRange() - 1;

            for (port = cfg.getRestTcpPort(); port <= lastPort; port++) {
                if (startTcpServer(host, port, lsnr, parser, sslCtx, cfg)) {
                    if (log.isInfoEnabled())
                        log.info(startInfo());

                    return;
                }
            }

            U.warn(log, "Failed to start TCP binary REST server (possibly all ports in range are in use) " +
                "[firstPort=" + cfg.getRestTcpPort() + ", lastPort=" + lastPort + ", host=" + host + ']');
        }
        catch (SSLException e) {
            U.warn(log, "Failed to start " + name() + " protocol on port " + port + ": " + e.getMessage(),
                "Failed to start " + name() + " protocol on port " + port + ". Check if SSL context factory is " +
                    "properly configured.");
        }
        catch (IOException e) {
            U.warn(log, "Failed to start " + name() + " protocol on port " + port + ": " + e.getMessage(),
                "Failed to start " + name() + " protocol on port " + port + ". " +
                    "Check restTcpHost configuration property.");
        }
    }

    /** {@inheritDoc} */
    @Override public void stop() {
        if (srv != null) {
            ctx.ports().deregisterPorts(getClass());

            srv.stop();
        }

        if (log.isInfoEnabled())
            log.info(stopInfo());
    }

    /**
     * Resolves host for REST TCP server using grid configuration.
     *
     * @param cfg Grid configuration.
     * @return REST host.
     * @throws IOException If failed to resolve REST host.
     */
    private InetAddress resolveRestTcpHost(GridConfiguration cfg) throws IOException {
        String host = cfg.getRestTcpHost();

        if (host == null)
            host = cfg.getLocalHost();

        return U.resolveLocalHost(host);
    }

    /**
     * Tries to start server with given parameters.
     *
     * @param hostAddr Host on which server should be bound.
     * @param port Port on which server should be bound.
     * @param lsnr Server message listener.
     * @param parser Server message parser.
     * @param sslCtx SSL context in case if SSL is enabled.
     * @param cfg Configuration for other parameters.
     * @return {@code True} if server successfully started, {@code false} if port is used and
     *      server was unable to start.
     */
    private boolean startTcpServer(InetAddress hostAddr, int port, GridNioServerListener<GridClientMessage> lsnr,
        GridNioParser parser, @Nullable SSLContext sslCtx, GridConfiguration cfg) {
        try {
            GridNioFilter codec = new GridNioCodecFilter(parser, log, true);

            GridNioFilter[] filters;

            if (sslCtx != null) {
                GridNioSslFilter sslFilter = new GridNioSslFilter(sslCtx, log);

                sslFilter.directMode(true);

                boolean auth = cfg.isRestTcpSslClientAuth();

                sslFilter.wantClientAuth(auth);

                sslFilter.needClientAuth(auth);

                filters = new GridNioFilter[] {
                    codec,
                    sslFilter
                };
            }
            else
                filters = new GridNioFilter[] { codec };

            srv = GridNioServer.<GridClientMessage>builder()
                .address(hostAddr)
                .port(port)
                .listener(lsnr)
                .logger(log)
                .selectorCount(cfg.getRestTcpSelectorCount())
                .gridName(ctx.gridName())
                .tcpNoDelay(cfg.isRestTcpNoDelay())
                .directBuffer(cfg.isRestTcpDirectBuffer())
                .byteOrder(ByteOrder.nativeOrder())
                .socketSendBufferSize(cfg.getRestTcpSendBufferSize())
                .socketReceiveBufferSize(cfg.getRestTcpReceiveBufferSize())
                .sendQueueLimit(cfg.getRestTcpSendQueueLimit())
                .filters(filters)
                .directMode(true)
                .messageWriter(msgWriter)
                .build();

            srv.idleTimeout(cfg.getRestIdleTimeout());

            srv.start();

            ctx.ports().registerPort(port, GridPortProtocol.TCP, getClass());

            return true;
        }
        catch (GridException e) {
            if (log.isDebugEnabled())
                log.debug("Failed to start " + name() + " protocol on port " + port + ": " + e.getMessage());

            return false;
        }
    }

    /** {@inheritDoc} */
    @Override protected String getAddressPropertyName() {
        return GridNodeAttributes.ATTR_REST_TCP_ADDRS;
    }

    /** {@inheritDoc} */
    @Override protected String getHostNamePropertyName() {
        return GridNodeAttributes.ATTR_REST_TCP_HOST_NAMES;
    }

    /** {@inheritDoc} */
    @Override protected String getPortPropertyName() {
        return GridNodeAttributes.ATTR_REST_TCP_PORT;
    }
}
