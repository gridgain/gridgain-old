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

package org.gridgain.client;

import org.gridgain.client.marshaller.*;
import org.gridgain.client.marshaller.optimized.*;
import org.gridgain.grid.*;
import org.gridgain.grid.kernal.processors.rest.client.message.*;
import org.gridgain.grid.kernal.processors.rest.protocols.tcp.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.util.nio.*;
import org.jetbrains.annotations.*;

import java.net.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 *
 */
public class GridClientTestRestServer {
    /** */
    public static final int FIRST_SERVER_PORT = 11000;

    /** */
    public static final int SERVERS_CNT = 5;

    /** */
    private static final byte[] EMPTY_SES_TOKEN = new byte[] {};

    /** */
    private static final Collection<GridClientNodeBean> top = new ArrayList<>();

    /**
     *
     */
    static {
        for (int port = FIRST_SERVER_PORT; port < FIRST_SERVER_PORT + SERVERS_CNT; port++) {
            GridClientNodeBean node = new GridClientNodeBean();

            node.setNodeId(UUID.randomUUID());
            node.setConsistentId("127.0.0.1:" + port);
            node.setTcpPort(port);
            node.setTcpAddresses(Arrays.asList("127.0.0.1"));

            top.add(node);
        }
    }

    /** */
    private final int port;

    /** */
    private volatile boolean failOnConnect;

    /** */
    private final GridLogger log;

    /** */
    private final AtomicInteger connCnt = new AtomicInteger();

    /** */
    private final AtomicInteger succConnCnt = new AtomicInteger();

    /** */
    private final AtomicInteger disconnCnt = new AtomicInteger();

    /** */
    private GridNioServer<GridClientMessage> srv;

    /** */
    private volatile GridNioSession lastSes;

    /**
     * @param port Port to listen on.
     * @param failOnConnect If {@code true} than server will close connection immediately after connect.
     * @param log Log.
     */
    public GridClientTestRestServer(int port, boolean failOnConnect, GridLogger log) {
        this.port = port;
        this.failOnConnect = failOnConnect;
        this.log = log;
    }

    /**
     * @return Port number.
     */
    public int getPort() {
        return port;
    }

    /**
     * Starts the server.
     *
     * @throws GridException If failed.
     */
    public void start() throws GridException {
        try {
            String gridName = "test";

            srv = GridNioServer.<GridClientMessage>builder()
                .address(InetAddress.getByName("127.0.0.1"))
                .port(port)
                .listener(new TestListener())
                .logger(log)
                .selectorCount(2)
                .gridName(gridName)
                .byteOrder(ByteOrder.nativeOrder())
                .tcpNoDelay(true)
                .directBuffer(false)
                .filters(
                    new GridNioAsyncNotifyFilter(gridName, Executors.newFixedThreadPool(2), log),
                    new GridNioCodecFilter(new TestParser(), log, false)
                )
                .build();
        }
        catch (UnknownHostException e) {
            throw new GridException("Failed to determine localhost address.", e);
        }

        srv.start();
    }

    /**
     * Stops the server.
     */
    public void stop() {
        assert srv != null;

        srv.stop();
    }

    /**
     * @return Number of connections opened to this server.
     */
    public int getConnectCount() {
        return connCnt.get();
    }

    /**
     * @return Number of successful connections opened to this server.
     */
    public int getSuccessfulConnectCount() {
        return succConnCnt.get();
    }

    /**
     * @return Number of connections with this server closed by clients.
     */
    public int getDisconnectCount() {
        return disconnCnt.get();
    }

    /**
     * Closes all opened connections.
     */
    public void fail() {
        assert lastSes != null;

        lastSes.close();

        failOnConnect = true;

        resetCounters();
    }

    /**
     *
     */
    public void repair() {
        failOnConnect = false;
    }

    /**
     * Resets all counters.
     */
    public void resetCounters() {
        connCnt.set(0);
        succConnCnt.set(0);
        disconnCnt.set(0);
    }

    /**
     * Prepares response stub.
     * @param msg Mesage to respond to.
     * @return Response.
     */
    private static GridClientResponse makeResponseFor(GridClientMessage msg) {
        GridClientResponse res = new GridClientResponse();

        res.clientId(msg.clientId());
        res.requestId(msg.requestId());
        res.successStatus(GridClientResponse.STATUS_SUCCESS);
        res.sessionToken(EMPTY_SES_TOKEN);

        return res;
    }

    /**
     * Test listener.
     */
    private class TestListener extends GridNioServerListenerAdapter<GridClientMessage> {
        /** {@inheritDoc} */
        @Override public void onConnected(GridNioSession ses) {
            lastSes = ses;

            connCnt.incrementAndGet();

            if (failOnConnect)
                ses.close();
            else
                succConnCnt.incrementAndGet();
        }

        /** {@inheritDoc} */
        @Override public void onDisconnected(GridNioSession ses, @Nullable Exception e) {
            disconnCnt.incrementAndGet();
        }

        /** {@inheritDoc} */
        @Override public void onMessage(GridNioSession ses, GridClientMessage msg) {
            if (msg == GridClientPingPacket.PING_MESSAGE)
                ses.send(GridClientPingPacket.PING_MESSAGE);
            else if (msg instanceof GridClientAuthenticationRequest)
                ses.send(makeResponseFor(msg));
            else if (msg instanceof GridClientTopologyRequest) {
                GridClientResponse res = makeResponseFor(msg);

                res.result(top);

                ses.send(res);
            }
            else if (msg instanceof GridClientHandshakeRequest)
                ses.send(GridClientHandshakeResponse.OK);
        }

        /** {@inheritDoc} */
        @Override public void onSessionWriteTimeout(GridNioSession ses) {
            ses.close();
        }

        /** {@inheritDoc} */
        @Override public void onSessionIdleTimeout(GridNioSession ses) {
            ses.close();
        }
    }

    /**
     */
    private static class TestParser extends GridTcpRestParser {
        /** */
        private final GridClientMarshaller marsh = new GridClientOptimizedMarshaller();

        /** {@inheritDoc} */
        @Override protected GridClientMarshaller marshaller(GridNioSession ses) {
            return marsh;
        }
    }
}
