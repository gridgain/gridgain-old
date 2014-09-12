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

import org.gridgain.client.balancer.*;
import org.gridgain.client.impl.*;
import org.gridgain.client.ssl.*;
import org.gridgain.grid.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.testframework.*;
import org.gridgain.testframework.junits.common.*;

import javax.net.ssl.*;
import java.security.cert.*;
import java.util.*;
import java.util.concurrent.atomic.*;

/**
 * Tests
 */
public class GridClientTcpSslAuthenticationSelfTest extends GridCommonAbstractTest {
    /** REST TCP port. */
    private static final int REST_TCP_PORT = 12121;

    /** Test trust manager for server. */
    private MockX509TrustManager srvTrustMgr = new MockX509TrustManager();

    /** Test trust manager for client. */
    private MockX509TrustManager clientTrustMgr = new MockX509TrustManager();

    /** Whether server should check clients. */
    private volatile boolean checkClient;

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        assertEquals(0, srvTrustMgr.serverCheckCallCount());
        assertEquals(0, clientTrustMgr.clientCheckCallCount());
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        srvTrustMgr.reset();
        clientTrustMgr.reset();
    }

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration c = super.getConfiguration(gridName);

        c.setLocalHost(getTestResources().getLocalHost());

        assert c.getClientConnectionConfiguration() == null;

        GridClientConnectionConfiguration clientCfg = new GridClientConnectionConfiguration();

        clientCfg.setRestTcpPort(REST_TCP_PORT);
        clientCfg.setRestTcpSslEnabled(true);

        clientCfg.setRestTcpSslClientAuth(checkClient);
        clientCfg.setRestTcpSslClientAuth(checkClient);

        GridSslBasicContextFactory factory = (GridSslBasicContextFactory)GridTestUtils.sslContextFactory();

        factory.setTrustManagers(srvTrustMgr);

        clientCfg.setRestTcpSslContextFactory(factory);

        c.setClientConnectionConfiguration(clientCfg);

        return c;
    }

    /**
     * Creates client that will try to connect to only first node in grid.
     *
     * @return Client.
     * @throws Exception If failed to create client.
     */
    private GridClientImpl createClient() throws Exception {
        GridClientConfiguration cfg = new GridClientConfiguration();

        cfg.setServers(Arrays.asList(U.getLocalHost().getHostAddress() + ":" + REST_TCP_PORT));
        cfg.setBalancer(new GridClientRoundRobinBalancer());

        GridSslBasicContextFactory factory = (GridSslBasicContextFactory)GridTestUtils.sslContextFactory();

        factory.setTrustManagers(clientTrustMgr);

        cfg.setSslContextFactory(factory);

        return (GridClientImpl)GridClientFactory.start(cfg);
    }

    /**
     * @throws Exception If failed.
     */
    public void testServerAuthenticated() throws Exception {
        checkServerAuthenticatedByClient(false);
    }

    /**
     * @throws Exception If failed.
     */
    public void testServerNotAuthenticatedByClient() throws Exception {
        try {
            checkServerAuthenticatedByClient(true);
        }
        catch (GridClientDisconnectedException e) {
            assertTrue(X.hasCause(e, GridServerUnreachableException.class));
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testClientAuthenticated() throws Exception {
        checkClientAuthenticatedByServer(false);
    }

    /**
     * @throws Exception If failed.
     */
    public void testClientNotAuthenticated() throws Exception {
        try {
            checkServerAuthenticatedByClient(true);
        }
        catch (GridClientDisconnectedException e) {
            assertTrue(X.hasCause(e, GridServerUnreachableException.class));
        }
    }

    /**
     * @param fail Should client trust manager fail.
     * @throws Exception If failed.
     */
    private void checkServerAuthenticatedByClient(boolean fail) throws Exception {
        checkClient = false;
        srvTrustMgr.shouldFail(false);
        clientTrustMgr.shouldFail(fail);

        startGrid();

        try {
            try (GridClientImpl c = createClient()) {
                c.compute().refreshTopology(false, false);
            }
        }
        finally {
            G.stopAll(false);
        }

        assertEquals(0, srvTrustMgr.clientCheckCallCount());
        assertEquals(1, clientTrustMgr.serverCheckCallCount());
    }

    /**
     * @param fail Should server trust manager fail.
     * @throws Exception If failed.
     */
    private void checkClientAuthenticatedByServer(boolean fail) throws Exception {
        checkClient = true;
        srvTrustMgr.shouldFail(fail);
        clientTrustMgr.shouldFail(false);

        startGrid();

        try {
            try (GridClientImpl c = createClient()) {
                c.compute().refreshTopology(false, false);
            }
        }
        finally {
            G.stopAll(false);
        }

        assertEquals(1, srvTrustMgr.clientCheckCallCount());
        assertEquals(1, clientTrustMgr.serverCheckCallCount());
    }

    /**
     * Test trust manager to emulate certificate check failures.
     */
    private static class MockX509TrustManager implements X509TrustManager {
        /** Empty array. */
        private static final X509Certificate[] EMPTY = new X509Certificate[0];

        /** Whether checks should fail. */
        private volatile boolean shouldFail;

        /** Client check call count. */
        private AtomicInteger clientCheckCallCnt = new AtomicInteger();

        /** Server check call count. */
        private AtomicInteger srvCheckCallCnt = new AtomicInteger();

        /**
         * @param shouldFail Whether checks should fail.
         */
        private void shouldFail(boolean shouldFail) {
            this.shouldFail = shouldFail;
        }

        /** {@inheritDoc} */
        @Override public void checkClientTrusted(X509Certificate[] x509Certificates, String s)
            throws CertificateException {
            clientCheckCallCnt.incrementAndGet();

            if (shouldFail)
                throw new CertificateException("Client check failed.");
        }

        /** {@inheritDoc} */
        @Override public void checkServerTrusted(X509Certificate[] x509Certificates, String s)
            throws CertificateException {
            srvCheckCallCnt.incrementAndGet();

            if (shouldFail)
                throw new CertificateException("Server check failed.");
        }

        /** {@inheritDoc} */
        @Override public X509Certificate[] getAcceptedIssuers() {
            return EMPTY;
        }

        /**
         * @return Call count to checkClientTrusted method.
         */
        public int clientCheckCallCount() {
            return clientCheckCallCnt.get();
        }

        /**
         * @return Call count to checkServerTrusted method.
         */
        public int serverCheckCallCount() {
            return srvCheckCallCnt.get();
        }

        /**
         * Clears should fail flag and resets call counters.
         */
        public void reset() {
            shouldFail = false;
            clientCheckCallCnt.set(0);
            srvCheckCallCnt.set(0);
        }
    }
}
