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

package org.gridgain.client.integration;

import org.gridgain.client.*;
import org.gridgain.client.balancer.*;
import org.gridgain.client.ssl.*;
import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.compute.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.kernal.managers.communication.*;
import org.gridgain.grid.kernal.processors.cache.*;
import org.gridgain.grid.kernal.processors.cache.distributed.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.communication.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.grid.util.direct.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.testframework.*;
import org.gridgain.testframework.junits.common.*;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static java.util.concurrent.TimeUnit.*;
import static org.gridgain.grid.cache.GridCacheAtomicityMode.*;
import static org.gridgain.grid.cache.GridCacheDistributionMode.*;
import static org.gridgain.grid.cache.GridCacheMode.*;
import static org.gridgain.grid.cache.GridCacheWriteSynchronizationMode.*;

/**
 * Tests basic client behavior with multiple nodes.
 */
@SuppressWarnings("ThrowableResultOfMethodCallIgnored")
public abstract class GridClientAbstractMultiNodeSelfTest extends GridCommonAbstractTest {
    /** */
    private static final GridTcpDiscoveryIpFinder IP_FINDER = new GridTcpDiscoveryVmIpFinder(true);

    /** Partitioned cache name. */
    private static final String PARTITIONED_CACHE_NAME = "partitioned";

    /** Replicated cache name. */
    private static final String REPLICATED_CACHE_NAME = "replicated";

    /** Replicated async cache name. */
    private static final String REPLICATED_ASYNC_CACHE_NAME = "replicated_async";

    /** Nodes count. */
    public static final int NODES_CNT = 5;

    /**
     * Topology update frequency.
     * Set it longer than router's, so we wouldn't receive failures
     * caused only by obsolete topology on router.
     */
    static final int TOP_REFRESH_FREQ = 2500;

    /** Path to jetty config. */
    public static final String REST_JETTY_CFG = "modules/clients/src/test/resources/jetty/rest-jetty.xml";

    /** Path to jetty config with SSl enabled. */
    public static final String REST_JETTY_SSL_CFG = "modules/clients/src/test/resources/jetty/rest-jetty-ssl.xml";

    /** Host. */
    public static final String HOST = "127.0.0.1";

    /** Base for tcp rest ports. */
    public static final int REST_TCP_PORT_BASE = 12345;

    /** Base for http rest ports, defined in {@link #REST_JETTY_CFG}. */
    public static final int REST_HTTP_PORT_BASE = 11080;

    /** Base for https rest ports, defined in {@link #REST_JETTY_SSL_CFG}. */
    public static final int REST_HTTPS_PORT_BASE = 11443;

    /** */
    private static volatile boolean commSpiEnabled;

    /** Flag to enable REST in node configuration. */
    private boolean restEnabled = true;

    /** Client instance for each test. */
    private GridClient client;

    /**
     * @return Client protocol that should be used.
     */
    protected abstract GridClientProtocol protocol();

    /**
     * @return Server address to create first connection.
     */
    protected abstract String serverAddress();

    /**
     * @return SSL context factory to use if SSL or {@code null} to disable SSL usage.
     */
    @Nullable protected GridSslContextFactory sslContextFactory() {
        return null;
    }

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration c = super.getConfiguration(gridName);

        c.setLocalHost(HOST);

        assert c.getClientConnectionConfiguration() == null;

        if (restEnabled) {
            GridClientConnectionConfiguration clientCfg = new GridClientConnectionConfiguration();

            clientCfg.setRestTcpPort(REST_TCP_PORT_BASE);

            GridSslContextFactory sslCtxFactory = sslContextFactory();

            if (sslCtxFactory != null) {
                clientCfg.setRestTcpSslEnabled(true);
                clientCfg.setRestTcpSslContextFactory(sslCtxFactory);
            }

            c.setClientConnectionConfiguration(clientCfg);
        }

        GridTcpDiscoverySpi disco = new GridTcpDiscoverySpi();

        disco.setIpFinder(IP_FINDER);

        c.setDiscoverySpi(disco);

        TestCommunicationSpi spi = new TestCommunicationSpi();

        spi.setLocalPort(GridTestUtils.getNextCommPort(getClass()));

        c.setCommunicationSpi(spi);

        c.setCacheConfiguration(cacheConfiguration(null), cacheConfiguration(PARTITIONED_CACHE_NAME),
            cacheConfiguration(REPLICATED_CACHE_NAME), cacheConfiguration(REPLICATED_ASYNC_CACHE_NAME));

        ThreadPoolExecutor exec = new ThreadPoolExecutor(
            40,
            40,
            0,
            MILLISECONDS,
            new LinkedBlockingQueue<Runnable>());

        exec.prestartAllCoreThreads();

        c.setExecutorService(exec);

        c.setExecutorServiceShutdown(true);

        ThreadPoolExecutor sysExec = new ThreadPoolExecutor(
            40,
            40,
            0,
            MILLISECONDS,
            new LinkedBlockingQueue<Runnable>());

        sysExec.prestartAllCoreThreads();

        c.setSystemExecutorService(sysExec);

        c.setSystemExecutorServiceShutdown(true);

        return c;
    }

    /**
     * @param cacheName Cache name.
     * @return Cache configuration.
     * @throws Exception In case of error.
     */
    private GridCacheConfiguration cacheConfiguration(@Nullable String cacheName) throws Exception {
        GridCacheConfiguration cfg = defaultCacheConfiguration();

        cfg.setAtomicityMode(TRANSACTIONAL);
        cfg.setDistributionMode(NEAR_PARTITIONED);

        if (cacheName == null)
            cfg.setCacheMode(LOCAL);
        else if (PARTITIONED_CACHE_NAME.equals(cacheName)) {
            cfg.setCacheMode(PARTITIONED);

            cfg.setBackups(0);
        }
        else
            cfg.setCacheMode(REPLICATED);

        cfg.setName(cacheName);

        cfg.setWriteSynchronizationMode(REPLICATED_ASYNC_CACHE_NAME.equals(cacheName) ? FULL_ASYNC : FULL_SYNC);

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        startGridsMultiThreaded(NODES_CNT);
    }

    /** {@inheritDoc} */
    @Override protected void afterTestsStopped() throws Exception {
        info("Stopping grids.");

        stopAllGrids();
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        client = GridClientFactory.start(clientConfiguration());
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        if (client != null) {
            GridClientFactory.stop(client.id(), false);

            client = null;
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testSyncCommitRollbackFlags() throws Exception {
        commSpiEnabled = true;

        try {
            GridClientData data = client.data(REPLICATED_ASYNC_CACHE_NAME);

            info("Before put x1");

            data.put("x1", "y1");

            info("Before put x2");

            data.flagsOn(GridClientCacheFlag.SYNC_COMMIT).put("x2", "y2");

            info("Before put x3");

            data.put("x3", "y3");

            info("Before put x4");

            data.flagsOn(GridClientCacheFlag.SYNC_COMMIT).put("x4", "y4");
        }
        finally {
            commSpiEnabled = false;
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testEmptyProjections() throws Exception {
        final GridClientCompute dflt = client.compute();

        Collection<? extends GridClientNode> nodes = dflt.nodes();

        assertEquals(NODES_CNT, nodes.size());

        Iterator<? extends GridClientNode> iter = nodes.iterator();

        final GridClientCompute singleNodePrj = dflt.projection(Collections.singletonList(iter.next()));

        final GridClientNode second = iter.next();

        final GridClientPredicate<GridClientNode> noneFilter = new GridClientPredicate<GridClientNode>() {
            @Override public boolean apply(GridClientNode node) {
                return false;
            }
        };

        final GridClientPredicate<GridClientNode> targetFilter = new GridClientPredicate<GridClientNode>() {
            @Override public boolean apply(GridClientNode node) {
                return node.nodeId().equals(second.nodeId());
            }
        };

        GridTestUtils.assertThrows(log(), new Callable<Object>() {
            @Override public Object call() throws Exception {
                return dflt.projection(noneFilter).log(-1, -1);
            }
        }, GridServerUnreachableException.class, null);

        GridTestUtils.assertThrows(log(), new Callable<Object>() {
            @Override public Object call() throws Exception {
                return singleNodePrj.projection(second);
            }
        }, GridClientException.class, null);

        GridTestUtils.assertThrows(log(), new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return singleNodePrj.projection(targetFilter);
            }
        }, GridClientException.class, null);
    }

    /**
     * @throws Exception If failed.
     */
    public void testProjectionRun() throws Exception {
        GridClientCompute dflt = client.compute();

        Collection<? extends GridClientNode> nodes = dflt.nodes();

        assertEquals(NODES_CNT, nodes.size());

        for (int i = 0; i < NODES_CNT; i++) {
            Grid g = grid(i);

            assert g != null;

            GridClientNode clientNode = dflt.node(g.localNode().id());

            assertNotNull("Client node for " + g.localNode().id() + " was not found", clientNode);

            GridClientCompute prj = dflt.projection(clientNode);

            String res = prj.execute(TestTask.class.getName(), null);

            assertNotNull(res);

            assertEquals(g.localNode().id().toString(), res);
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testAffinityExecute() throws Exception {
        GridClientCompute dflt = client.compute();

        GridClientData data = client.data(PARTITIONED_CACHE_NAME);

        Collection<? extends GridClientNode> nodes = dflt.nodes();

        assertEquals(NODES_CNT, nodes.size());

        for (int i = 0; i < NODES_CNT; i++) {
            Grid g = grid(i);

            assert g != null;

            int affinityKey = -1;

            for (int key = 0; key < 10000; key++) {
                if (g.localNode().id().equals(data.affinity(key))) {
                    affinityKey = key;

                    break;
                }
            }

            if (affinityKey == -1)
                throw new Exception("Unable to found key for which node is primary: " + g.localNode().id());

            GridClientNode clientNode = dflt.node(g.localNode().id());

            assertNotNull("Client node for " + g.localNode().id() + " was not found", clientNode);

            String res = dflt.affinityExecute(TestTask.class.getName(), PARTITIONED_CACHE_NAME, affinityKey, null);

            assertNotNull(res);

            assertEquals(g.localNode().id().toString(), res);
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testInvalidateFlag() throws Exception {
        GridEx g0 = grid(0);

        GridCache<String, String> cache = g0.cache(PARTITIONED_CACHE_NAME);

        String key = null;

        for (int i = 0; i < 10_000; i++) {
            if (!cache.affinity().isPrimaryOrBackup(g0.localNode(), String.valueOf(i))) {
                key = String.valueOf(i);

                break;
            }
        }

        assertNotNull(key);

        cache.put(key, key); // Create entry in near cache, it is invalidated if INVALIDATE flag is set.

        assertNotNull(cache.peek(key));

        GridClientData d = client.data(PARTITIONED_CACHE_NAME);

        d.flagsOn(GridClientCacheFlag.INVALIDATE).put(key, "zzz");

        for (Grid g : G.allGrids()) {
            cache = g.cache(PARTITIONED_CACHE_NAME);

            if (cache.affinity().isPrimaryOrBackup(g.localNode(), key))
                assertEquals("zzz", cache.peek(key));
            else
                assertNull(cache.peek(key));
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testClientAffinity() throws Exception {
        GridClientData partitioned = client.data(PARTITIONED_CACHE_NAME);

        Collection<Object> keys = new ArrayList<>();

        keys.addAll(Arrays.asList(
            Boolean.TRUE,
            Boolean.FALSE,
            1,
            Integer.MAX_VALUE
        ));

        Random rnd = new Random();
        StringBuilder sb = new StringBuilder();

        // Generate some random strings.
        for (int i = 0; i < 100; i++) {
            sb.setLength(0);

            for (int j = 0; j < 255; j++)
                // Only printable ASCII symbols for test.
                sb.append((char)(rnd.nextInt(0x7f - 0x20) + 0x20));

            keys.add(sb.toString());
        }

        // Generate some more keys to achieve better coverage.
        for (int i = 0; i < 100; i++)
            keys.add(UUID.randomUUID());

        for (Object key : keys) {
            UUID nodeId = grid(0).mapKeyToNode(PARTITIONED_CACHE_NAME, key).id();

            UUID clientNodeId = partitioned.affinity(key);

            assertEquals("Invalid affinity mapping for REST response for key: " + key, nodeId, clientNodeId);
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testTopologyListener() throws Exception {
        final Collection<UUID> added = new ArrayList<>(1);
        final Collection<UUID> rmvd = new ArrayList<>(1);

        final CountDownLatch addedLatch = new CountDownLatch(1);
        final CountDownLatch rmvLatch = new CountDownLatch(1);

        assertEquals(NODES_CNT, client.compute().refreshTopology(false, false).size());

        GridClientTopologyListener lsnr = new GridClientTopologyListener() {
            @Override public void onNodeAdded(GridClientNode node) {
                added.add(node.nodeId());

                addedLatch.countDown();
            }

            @Override public void onNodeRemoved(GridClientNode node) {
                rmvd.add(node.nodeId());

                rmvLatch.countDown();
            }
        };

        client.addTopologyListener(lsnr);

        try {
            Grid g = startGrid(NODES_CNT + 1);

            UUID id = g.localNode().id();

            assertTrue(addedLatch.await(2 * TOP_REFRESH_FREQ, MILLISECONDS));

            assertEquals(1, added.size());
            assertEquals(id, F.first(added));

            stopGrid(NODES_CNT + 1);

            assertTrue(rmvLatch.await(2 * TOP_REFRESH_FREQ, MILLISECONDS));

            assertEquals(1, rmvd.size());
            assertEquals(id, F.first(rmvd));
        }
        finally {
            client.removeTopologyListener(lsnr);

            stopGrid(NODES_CNT + 1);
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testDisabledRest() throws Exception {
        restEnabled = false;

        final Grid g = startGrid("disabled-rest");

        try {
            Thread.sleep(2 * TOP_REFRESH_FREQ);

            // As long as we have round robin load balancer this will cause every node to be queried.
            for (int i = 0; i < NODES_CNT + 1; i++)
                assertEquals(NODES_CNT + 1, client.compute().refreshTopology(false, false).size());

            final GridClientData data = client.data(PARTITIONED_CACHE_NAME);

            // Check rest-disabled node is unavailable.
            try {
                String affKey;

                do {
                    affKey = UUID.randomUUID().toString();
                } while (!data.affinity(affKey).equals(g.localNode().id()));

                data.put(affKey, "asdf");

                assertEquals("asdf", cache(0, PARTITIONED_CACHE_NAME).get(affKey));
            }
            catch (GridServerUnreachableException e) {
                // Thrown for direct client-node connections.
                assertTrue("Unexpected exception message: " + e.getMessage(),
                    e.getMessage().startsWith("No available endpoints to connect (is rest enabled for this node?)"));
            }
            catch (GridClientException e) {
                // Thrown for routed client-router-node connections.
                String msg = e.getMessage();

                assertTrue("Unexpected exception message: " + msg, protocol() == GridClientProtocol.TCP ?
                    msg.contains("No available endpoints to connect (is rest enabled for this node?)") : // TCP router.
                    msg.startsWith("No available nodes on the router for destination node ID"));         // HTTP router.
            }

            // Check rest-enabled nodes are available.
            String affKey;

            do {
                affKey = UUID.randomUUID().toString();
            } while (data.affinity(affKey).equals(g.localNode().id()));

            data.put(affKey, "fdsa");

            assertEquals("fdsa", cache(0, PARTITIONED_CACHE_NAME).get(affKey));
        }
        finally {
            restEnabled = true;

            G.stop(g.name(), true);
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testAffinityPut() throws Exception {
        Thread.sleep(2 * TOP_REFRESH_FREQ);

        assertEquals(NODES_CNT, client.compute().refreshTopology(false, false).size());

        Map<UUID, Grid> gridsByLocNode = new HashMap<>(NODES_CNT);

        GridClientData partitioned = client.data(PARTITIONED_CACHE_NAME);

        GridClientCompute compute = client.compute();

        for (int i = 0; i < NODES_CNT; i++)
            gridsByLocNode.put(grid(i).localNode().id(), grid(i));

        for (int i = 0; i < 100; i++) {
            String key = "key" + i;

            UUID primaryNodeId = grid(0).mapKeyToNode(PARTITIONED_CACHE_NAME, key).id();

            assertEquals("Affinity mismatch for key: " + key, primaryNodeId, partitioned.affinity(key));

            assertEquals(primaryNodeId, partitioned.affinity(key));

            // Must go to primary node only. Since backup count is 0, value must present on
            // primary node only.
            partitioned.put(key, "val" + key);

            for (Map.Entry<UUID, Grid> entry : gridsByLocNode.entrySet()) {
                Object val = entry.getValue().cache(PARTITIONED_CACHE_NAME).peek(key);

                if (primaryNodeId.equals(entry.getKey()))
                    assertEquals("val" + key, val);
                else
                    assertNull(val);
            }
        }

        // Now check that we will see value in near cache in pinned mode.
        for (int i = 100; i < 200; i++) {
            String pinnedKey = "key" + i;

            UUID primaryNodeId = grid(0).mapKeyToNode(PARTITIONED_CACHE_NAME, pinnedKey).id();

            UUID pinnedNodeId = F.first(F.view(gridsByLocNode.keySet(), F.notEqualTo(primaryNodeId)));

            GridClientNode node = compute.node(pinnedNodeId);

            partitioned.pinNodes(node).put(pinnedKey, "val" + pinnedKey);

            for (Map.Entry<UUID, Grid> entry : gridsByLocNode.entrySet()) {
                Object val = entry.getValue().cache(PARTITIONED_CACHE_NAME).peek(pinnedKey);

                if (primaryNodeId.equals(entry.getKey()) || pinnedNodeId.equals(entry.getKey()))
                    assertEquals("val" + pinnedKey, val);
                else
                    assertNull(val);
            }
        }
    }

    /**
     * @return Client configuration for the test.
     */
    protected GridClientConfiguration clientConfiguration() throws GridClientException {
        GridClientConfiguration cfg = new GridClientConfiguration();

        cfg.setBalancer(getBalancer());

        cfg.setTopologyRefreshFrequency(TOP_REFRESH_FREQ);

        cfg.setProtocol(protocol());
        cfg.setServers(Arrays.asList(serverAddress()));
        cfg.setSslContextFactory(sslContextFactory());

        GridClientDataConfiguration loc = new GridClientDataConfiguration();

        GridClientDataConfiguration partitioned = new GridClientDataConfiguration();

        partitioned.setName(PARTITIONED_CACHE_NAME);
        partitioned.setAffinity(new GridClientPartitionAffinity());

        GridClientDataConfiguration replicated = new GridClientDataConfiguration();
        replicated.setName(REPLICATED_CACHE_NAME);

        GridClientDataConfiguration replicatedAsync = new GridClientDataConfiguration();
        replicatedAsync.setName(REPLICATED_ASYNC_CACHE_NAME);

        cfg.setDataConfigurations(Arrays.asList(loc, partitioned, replicated, replicatedAsync));

        return cfg;
    }

    /**
     * Gets client load balancer.
     *
     * @return Load balancer.
     */
    protected GridClientLoadBalancer getBalancer() {
        return new GridClientRoundRobinBalancer();
    }

    /**
     * Test task. Returns a tuple in which first component is id of node that has split the task,
     * and second component is count of nodes that executed jobs.
     */
    private static class TestTask extends GridComputeTaskSplitAdapter<Object, String> {
        /** */
        @GridInstanceResource
        private Grid grid;

        /** Count of tasks this job was split to. */
        private int gridSize;

        /** {@inheritDoc} */
        @Override protected Collection<? extends GridComputeJob> split(int gridSize, Object arg)
            throws GridException {
            Collection<GridComputeJobAdapter> jobs = new ArrayList<>(gridSize);

            this.gridSize = gridSize;

            final String locNodeId = grid.localNode().id().toString();

            for (int i = 0; i < gridSize; i++) {
                jobs.add(new GridComputeJobAdapter() {
                    @SuppressWarnings("OverlyStrongTypeCast")
                    @Override public Object execute() {
                        try {
                            Thread.sleep(1000);
                        }
                        catch (InterruptedException ignored) {
                            Thread.currentThread().interrupt();
                        }

                        return new GridBiTuple<>(locNodeId, 1);
                    }
                });
            }

            return jobs;
        }

        /** {@inheritDoc} */
        @Override public String reduce(List<GridComputeJobResult> results) throws GridException {
            int sum = 0;

            String locNodeId = null;

            for (GridComputeJobResult res : results) {
                GridBiTuple<String, Integer> part = res.getData();

                if (locNodeId == null)
                    locNodeId = part.get1();

                Integer i = part.get2();

                if (i != null)
                    sum += i;
            }

            assert gridSize == sum;

            return locNodeId;
        }
    }

    /**
     * Communication SPI which checks cache flags.
     */
    @SuppressWarnings("unchecked")
    private static class TestCommunicationSpi extends GridTcpCommunicationSpi {
        /** Node ID. */
        @GridLocalNodeIdResource
        private UUID nodeId;

        /** {@inheritDoc} */
        @Override public void sendMessage(GridNode node, GridTcpCommunicationMessageAdapter msg)
            throws GridSpiException {
            checkSyncFlags((GridIoMessage)msg);

            super.sendMessage(node, msg);
        }

        /**
         * Check if flags in correct state.
         *
         * @param msg Message.
         */
        private void checkSyncFlags(GridIoMessage msg) {
            if (!commSpiEnabled)
                return;

            Object o = msg.message();

            if (!(o instanceof GridDistributedLockRequest))
                return;

            GridKernal g = (GridKernal)G.grid(nodeId);

            GridCacheTxManager<Object, Object> tm = g.internalCache(REPLICATED_ASYNC_CACHE_NAME).context().tm();

            GridCacheVersion v = ((GridCacheVersionable)o).version();

            GridCacheTxEx t = tm.tx(v);

            if (t.hasWriteKey("x1"))
                assertFalse(t.syncCommit());
            else if (t.hasWriteKey("x2"))
                assertTrue(t.syncCommit());
            else if (t.hasWriteKey("x3"))
                assertFalse(t.syncCommit());
            else if (t.hasWriteKey("x4"))
                assertTrue(t.syncCommit());
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testMultithreadedCommand() throws Exception {
        final GridClientData data = client.data(PARTITIONED_CACHE_NAME);
        final GridClientCompute compute = client.compute();
        final AtomicInteger cnt = new AtomicInteger(0);

        multithreaded(new Callable<Object>() {
            @Override public Object call() throws Exception {
                for (int i = 0; i < 20; i++) {
                    String key = UUID.randomUUID().toString();
                    String val = UUID.randomUUID().toString();

                    switch (cnt.incrementAndGet() % 4) {
                        case 0: {
                            assertTrue(data.put(key, val));
                            assertEquals(val, data.get(key));
                            assertTrue(data.remove(key));

                            break;
                        }

                        case 1: {
                            assertNotNull(data.metrics());

                            break;
                        }

                        case 2: {
                            String nodeId = compute.execute(TestTask.class.getName(), null);

                            assertNotNull(nodeId);
                            assertNotNull(compute.refreshNode(UUID.fromString(nodeId), true, true));

                            break;
                        }

                        case 3: {
                            assertEquals(NODES_CNT, compute.refreshTopology(true, true).size());

                            break;
                        }
                    }
                }

                return null;
            }
        }, 50, "multithreaded-client-access");
    }
}
