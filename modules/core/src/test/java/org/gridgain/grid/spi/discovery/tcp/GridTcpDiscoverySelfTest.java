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

package org.gridgain.grid.spi.discovery.tcp;

import org.gridgain.grid.*;
import org.gridgain.grid.events.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.kernal.processors.port.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.discovery.*;
import org.gridgain.grid.spi.discovery.tcp.internal.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.multicast.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.grid.spi.discovery.tcp.messages.*;
import org.gridgain.grid.spi.discovery.tcp.metricsstore.vm.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.testframework.*;
import org.gridgain.testframework.junits.common.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static java.util.concurrent.TimeUnit.*;
import static org.gridgain.grid.events.GridEventType.*;
import static org.gridgain.grid.spi.GridPortProtocol.*;

/**
 * Test for {@link GridTcpDiscoverySpi}.
 */
public class GridTcpDiscoverySelfTest extends GridCommonAbstractTest {
    /** */
    private GridTcpDiscoveryVmIpFinder ipFinder = new GridTcpDiscoveryVmIpFinder(true);

    /** */
    private GridTcpDiscoveryVmMetricsStore metricsStore = new GridTcpDiscoveryVmMetricsStore();

    /** */
    private Map<String, GridTcpDiscoverySpi> discoMap = new HashMap<>();

    /** */
    private UUID nodeId;

    /**
     * @throws Exception If fails.
     */
    public GridTcpDiscoverySelfTest() throws Exception {
        super(false);

        metricsStore.setMetricsExpireTime(2000);
    }

    /** {@inheritDoc} */
    @SuppressWarnings({"IfMayBeConditional", "deprecation"})
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        GridTcpDiscoverySpi spi;

        if (gridName.contains("FailBeforeNodeAddedSentSpi"))
            spi = new FailBeforeNodeAddedSentSpi();
        else if (gridName.contains("FailBeforeNodeLeftSentSpi"))
            spi = new FailBeforeNodeLeftSentSpi();
        else
            spi = new GridTcpDiscoverySpi();

        discoMap.put(gridName, spi);

        spi.setIpFinder(ipFinder);

        spi.setNetworkTimeout(2500);

        spi.setHeartbeatFrequency(1000);

        spi.setMaxMissedHeartbeats(3);

        spi.setStoresCleanFrequency(5000);

        spi.setJoinTimeout(5000);

        cfg.setDiscoverySpi(spi);

        cfg.setCacheConfiguration();

        cfg.setIncludeEventTypes(EVT_TASK_FAILED, EVT_TASK_FINISHED, EVT_JOB_MAPPED);

        cfg.setIncludeProperties();

        if (!gridName.contains("LoopbackProblemTest"))
            cfg.setLocalHost("127.0.0.1");

        if (gridName.contains("testFailureDetectionOnNodePing")) {
            spi.setReconnectCount(1); // To make test faster: on Windows 1 connect takes 1 second.
            spi.setHeartbeatFrequency(40000);
        }

        cfg.setRestEnabled(false);

        if (nodeId != null)
            cfg.setNodeId(nodeId);

        if (gridName.contains("NonSharedIpFinder")) {
            GridTcpDiscoveryVmIpFinder finder = new GridTcpDiscoveryVmIpFinder();

            finder.setAddresses(Arrays.asList("127.0.0.1:47501"));

            spi.setIpFinder(finder);
        }
        else if (gridName.contains("MulticastIpFinder")) {
            GridTcpDiscoveryMulticastIpFinder finder = new GridTcpDiscoveryMulticastIpFinder();

            finder.setAddressRequestAttempts(10);
            finder.setMulticastGroup(GridTestUtils.getNextMulticastGroup(getClass()));
            finder.setMulticastPort(GridTestUtils.getNextMulticastPort(getClass()));

            spi.setIpFinder(finder);

            // Loopback multicast discovery is not working on Mac OS
            // (possibly due to http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=7122846).
            if (U.isMacOs())
                spi.setLocalAddress(F.first(U.allLocalIps()));
        }

        if (gridName.contains("MetricsStore"))
            // SPI wants to use metrics store.
            spi.setMetricsStore(metricsStore);

        return cfg;
    }

    /**
     * @throws Exception If any error occurs.
     */
    public void testSingleNodeStartStop() throws Exception {
        try {
            startGrid(1);
        }
        finally {
            stopGrid(1);
        }
    }

    /**
     * @throws Exception If any error occurs.
     */
    public void testThreeNodesStartStop() throws Exception {
        try {
            startGrid(1);
            startGrid(2);
            startGrid(3);
        }
        finally {
            stopAllGrids();
        }
    }

    /**
     * @throws Exception If any errors occur.
     */
    public void testNodeConnectMessageSize() throws Exception {
        try {
            Grid g1 = startGrid(1);

            final AtomicInteger gridNameIdx = new AtomicInteger(1);

            GridTestUtils.runMultiThreaded(new Callable<Object>() {
                @Nullable @Override public Object call() throws Exception {
                    startGrid(gridNameIdx.incrementAndGet());

                    return null;
                }
            }, 4, "grid-starter");

            Collection<GridTcpDiscoveryNode> nodes = discoMap.get(g1.name()).ring().allNodes();

            ByteArrayOutputStream bos = new ByteArrayOutputStream();

            g1.configuration().getMarshaller().marshal(nodes, bos);

            info(">>> Approximate node connect message size [topSize=" + nodes.size() +
                ", msgSize=" + bos.size() / 1024.0 + "KB]");
        }
        finally {
            stopAllGrids(false);
        }
    }

    /**
     * @throws Exception If any error occurs.
     */
    public void testPing() throws Exception {
        try {
            startGrid(1);
            startGrid(2);
            startGrid(3);

            info("Nodes were started");

            for (Map.Entry<String, GridTcpDiscoverySpi> e : discoMap.entrySet()) {
                GridDiscoverySpi spi = e.getValue();

                for (Grid g : G.allGrids()) {
                    boolean res = spi.pingNode(g.localNode().id());

                    assert res : e.getKey() + " failed to ping " + g.localNode().id() + " of " + g.name();

                    info(e.getKey() + " pinged " + g.localNode().id() + " of " + g.name());
                }
            }

            info("All nodes pinged successfully.");
        }
        finally {
            stopAllGrids();
        }
    }

    /**
     * @throws Exception If any error occurs.
     */
    public void testFailureDetectionOnNodePing1() throws Exception {
        try {
            Grid g1 = startGrid("testFailureDetectionOnNodePingCoordinator");
            startGrid("testFailureDetectionOnNodePing2");
            Grid g3 = startGrid("testFailureDetectionOnNodePing3");

            testFailureDetectionOnNodePing(g1, g3);
        }
        finally {
            stopAllGrids();
        }
    }

    /**
     * @throws Exception If any error occurs.
     */
    public void testFailureDetectionOnNodePing2() throws Exception {
        try {
            startGrid("testFailureDetectionOnNodePingCoordinator");
            Grid g2 = startGrid("testFailureDetectionOnNodePing2");
            Grid g3 = startGrid("testFailureDetectionOnNodePing3");

            testFailureDetectionOnNodePing(g3, g2);
        }
        finally {
            stopAllGrids();
        }
    }

    /**
     * @throws Exception If any error occurs.
     */
    public void testFailureDetectionOnNodePing3() throws Exception {
        try {
            Grid g1 = startGrid("testFailureDetectionOnNodePingCoordinator");
            Grid g2 = startGrid("testFailureDetectionOnNodePing2");
            startGrid("testFailureDetectionOnNodePing3");

            testFailureDetectionOnNodePing(g2, g1);
        }
        finally {
            stopAllGrids();
        }
    }

    /**
     * @throws Exception If any error occurs.
     */
    private void testFailureDetectionOnNodePing(Grid pingingNode, Grid failedNode) throws Exception {
        final CountDownLatch cnt = new CountDownLatch(1);

        pingingNode.events().localListen(
            new GridPredicate<GridEvent>() {
                @Override public boolean apply(GridEvent evt) {
                    cnt.countDown();

                    return true;
                }
            },
            GridEventType.EVT_NODE_FAILED
        );

        info("Nodes were started");

        discoMap.get(failedNode.name()).simulateNodeFailure();

        GridTcpDiscoverySpi spi = discoMap.get(pingingNode.name());

        boolean res = spi.pingNode(failedNode.localNode().id());

        assertFalse("Ping is ok for node " + failedNode.localNode().id() + ", but had to fail.", res);

        // Heartbeat interval is 40 seconds, but we should detect node failure faster.
        assert cnt.await(7, SECONDS);
    }

    /**
     * @throws Exception If any error occurs.
     */
    public void testNodeAdded() throws Exception {
        try {
            final Grid g1 = startGrid(1);

            final CountDownLatch cnt = new CountDownLatch(2);

            g1.events().localListen(
                new GridPredicate<GridEvent>() {
                    @Override public boolean apply(GridEvent evt) {
                        info("Node joined: " + evt.message());

                        GridDiscoveryEvent discoEvt = (GridDiscoveryEvent)evt;

                        GridTcpDiscoveryNode node = ((GridTcpDiscoveryNode)discoMap.get(g1.name()).
                            getNode(discoEvt.eventNode().id()));

                        assert node != null && node.visible();

                        cnt.countDown();

                        return true;
                    }
                },
                GridEventType.EVT_NODE_JOINED
            );

            startGrid(2);
            startGrid(3);

            info("Nodes were started");

            assert cnt.await(1, SECONDS);
        }
        finally {
            stopAllGrids();
        }
    }

    /**
     * @throws Exception If any error occurs.
     */
    public void testOrdinaryNodeLeave() throws Exception {
        try {
            Grid g1 = startGrid(1);
            startGrid(2);
            startGrid(3);

            final CountDownLatch cnt = new CountDownLatch(2);

            g1.events().localListen(
                new GridPredicate<GridEvent>() {
                    @Override public boolean apply(GridEvent evt) {
                        cnt.countDown();

                        return true;
                    }
                },
                EVT_NODE_LEFT
            );

            info("Nodes were started");

            stopGrid(3);
            stopGrid(2);

            boolean res = cnt.await(1, SECONDS);

            assert res;
        }
        finally {
            stopAllGrids();
        }
    }

    /**
     * @throws Exception If any error occurs.
     */
    public void testCoordinatorNodeLeave() throws Exception {
        try {
            startGrid(1);
            Grid g2 = startGrid(2);

            final CountDownLatch cnt = new CountDownLatch(1);

            g2.events().localListen(new GridPredicate<GridEvent>() {
                @Override public boolean apply(GridEvent evt) {
                    cnt.countDown();

                    return true;
                }
            }, EVT_NODE_LEFT);

            info("Nodes were started");

            stopGrid(1);

            assert cnt.await(1, SECONDS);

            // Start new grid, ensure that added to topology
            final CountDownLatch cnt2 = new CountDownLatch(1);

            g2.events().localListen(new GridPredicate<GridEvent>() {
                @Override public boolean apply(GridEvent evt) {
                    cnt2.countDown();

                    return true;
                }
            }, EVT_NODE_JOINED);

            startGrid(3);

            assert cnt2.await(1, SECONDS);
        }
        finally {
            stopAllGrids();
        }
    }

    /**
     * @throws Exception If any error occurs.
     */
    public void testOrdinaryNodeFailure() throws Exception {
        try {
            Grid g1 = startGrid(1);
            Grid g2 = startGrid(2);
            Grid g3 = startGrid(3);

            final CountDownLatch cnt = new CountDownLatch(2);

            g1.events().localListen(
                new GridPredicate<GridEvent>() {
                    @Override public boolean apply(GridEvent evt) {
                        cnt.countDown();

                        return true;
                    }
                },
                GridEventType.EVT_NODE_FAILED
            );

            info("Nodes were started");

            discoMap.get(g2.name()).simulateNodeFailure();
            discoMap.get(g3.name()).simulateNodeFailure();

            assert cnt.await(25, SECONDS);
        }
        finally {
            stopAllGrids();
        }
    }

    /**
     * @throws Exception If any error occurs.
     */
    public void testCoordinatorNodeFailure() throws Exception {
        try {
            Grid g1 = startGrid(1);
            Grid g2 = startGrid(2);

            final CountDownLatch cnt = new CountDownLatch(1);

            g2.events().localListen(new GridPredicate<GridEvent>() {
                @Override public boolean apply(GridEvent evt) {
                    cnt.countDown();

                    return true;
                }
            }, GridEventType.EVT_NODE_FAILED);

            info("Nodes were started");

            discoMap.get(g1.name()).simulateNodeFailure();

            assert cnt.await(20, SECONDS);
        }
        finally {
            stopAllGrids();
        }
    }

    /**
     * @throws Exception If any error occurs.
     */
    public void testMetricsSending() throws Exception {
        final AtomicBoolean stopping = new AtomicBoolean();

        try {
            final CountDownLatch latch1 = new CountDownLatch(1);

            final Grid g1 = startGrid(1);

            GridPredicate<GridEvent> lsnr1 = new GridPredicate<GridEvent>() {
                @Override public boolean apply(GridEvent evt) {
                    info(evt.message());

                    latch1.countDown();

                    return true;
                }
            };

            g1.events().localListen(lsnr1, EVT_NODE_METRICS_UPDATED);

            assert latch1.await(10, SECONDS);

            g1.events().stopLocalListen(lsnr1);

            final CountDownLatch latch1_1 = new CountDownLatch(1);
            final CountDownLatch latch1_2 = new CountDownLatch(1);
            final CountDownLatch latch2_1 = new CountDownLatch(1);
            final CountDownLatch latch2_2 = new CountDownLatch(1);

            final Grid g2 = startGrid(2);

            g2.events().localListen(
                new GridPredicate<GridEvent>() {
                    @Override public boolean apply(GridEvent evt) {
                        if (stopping.get())
                            return true;

                        info(evt.message());

                        UUID id = ((GridDiscoveryEvent) evt).eventNode().id();

                        if (id.equals(g1.localNode().id()))
                            latch2_1.countDown();
                        else if (id.equals(g2.localNode().id()))
                            latch2_2.countDown();
                        else
                            assert false : "Event fired for unknown node.";

                        return true;
                    }
                },
                EVT_NODE_METRICS_UPDATED
            );

            g1.events().localListen(new GridPredicate<GridEvent>() {
                @Override public boolean apply(GridEvent evt) {
                    if (stopping.get())
                        return true;

                    info(evt.message());

                    UUID id = ((GridDiscoveryEvent) evt).eventNode().id();

                    if (id.equals(g1.localNode().id()))
                        latch1_1.countDown();
                    else if (id.equals(g2.localNode().id()))
                        latch1_2.countDown();
                    else
                        assert false : "Event fired for unknown node.";

                    return true;
                }
            }, EVT_NODE_METRICS_UPDATED);

            assert latch1_1.await(10, SECONDS);
            assert latch1_2.await(10, SECONDS);
            assert latch2_1.await(10, SECONDS);
            assert latch2_2.await(10, SECONDS);
        }
        finally {
            stopping.set(true);

            stopAllGrids();
        }
    }

    /**
     * @throws Exception If any error occurs.
     */
    public void testMetricsStore() throws Exception {
        try {
            final CountDownLatch latch1 = new CountDownLatch(1);

            final Grid g1 = startGrid("MetricsStore-1");

            GridPredicate<GridEvent> lsnr1 = new GridPredicate<GridEvent>() {
                @Override public boolean apply(GridEvent evt) {
                    info(evt.message());

                    latch1.countDown();

                    return true;
                }
            };

            g1.events().localListen(lsnr1, EVT_NODE_METRICS_UPDATED);

            assert latch1.await(10, SECONDS);

            g1.events().stopLocalListen(lsnr1);

            final CountDownLatch latch1_1 = new CountDownLatch(1);
            final CountDownLatch latch1_2 = new CountDownLatch(1);
            final CountDownLatch latch2_1 = new CountDownLatch(1);
            final CountDownLatch latch2_2 = new CountDownLatch(1);

            final Grid g2 = startGrid("MetricsStore-2");

            g2.events().localListen(new GridPredicate<GridEvent>() {
                @Override public boolean apply(GridEvent evt) {
                    info(evt.message());

                    UUID id = ((GridDiscoveryEvent) evt).eventNode().id();

                    if (id.equals(g1.localNode().id()))
                        latch2_1.countDown();
                    else if (id.equals(g2.localNode().id()))
                        latch2_2.countDown();
                    else
                        assert false : "Event fired for unknown node.";

                    return true;
                }
            }, EVT_NODE_METRICS_UPDATED);

            g1.events().localListen(new GridPredicate<GridEvent>() {
                @Override public boolean apply(GridEvent evt) {
                    info(evt.message());

                    UUID id = ((GridDiscoveryEvent) evt).eventNode().id();

                    if (id.equals(g1.localNode().id()))
                        latch1_1.countDown();
                    else if (id.equals(g2.localNode().id()))
                        latch1_2.countDown();
                    else
                        assert false : "Event fired for unknown node.";

                    return true;
                }
            }, EVT_NODE_METRICS_UPDATED);

            assert latch1_1.await(10, SECONDS);
            assert latch1_2.await(10, SECONDS);
            assert latch2_1.await(10, SECONDS);
            assert latch2_2.await(10, SECONDS);
        }
        finally {
            stopAllGrids();
        }
    }

    /**
     * @throws Exception If any error occurs.
     */
    public void testOrdinaryNodeFailureMetricsStore() throws Exception {
        try {
            final CountDownLatch latch = new CountDownLatch(1);

            Grid g1 = startGrid("MetricsStore-1");

            g1.events().localListen(new GridPredicate<GridEvent>() {
                @Override public boolean apply(GridEvent evt) {
                    latch.countDown();

                    return true;
                }
            }, EVT_NODE_FAILED);

            Grid g2 = startGrid("MetricsStore-2");

            discoMap.get(g2.name()).simulateNodeFailure();

            assert latch.await(10, SECONDS);
        }
        finally {
            stopAllGrids();
        }
    }

    /**
     * @throws Exception If any error occurs.
     */
    public void testCoordinatorNodeFailureMetricsStore() throws Exception {
        try {
            final CountDownLatch latch = new CountDownLatch(1);

            Grid g1 = startGrid("MetricsStore-1");

            Grid g2 = startGrid("MetricsStore-2");

            g2.events().localListen(new GridPredicate<GridEvent>() {
                @Override public boolean apply(GridEvent evt) {
                    latch.countDown();

                    return true;
                }
            }, EVT_NODE_FAILED);

            discoMap.get(g1.name()).simulateNodeFailure();

            assert latch.await(20, SECONDS);
        }
        finally {
            stopAllGrids();
        }
    }

    /**
     * @throws Exception If any error occurs.
     */
    public void testFailBeforeNodeAddedSent() throws Exception {
        try {
            Grid g1 = startGrid(1);

            final CountDownLatch joinCnt = new CountDownLatch(2);
            final CountDownLatch failCnt = new CountDownLatch(1);

            g1.events().localListen(new GridPredicate<GridEvent>() {
                @Override public boolean apply(GridEvent evt) {
                    if (evt.type() == EVT_NODE_JOINED)
                        joinCnt.countDown();
                    else if (evt.type() == EVT_NODE_FAILED)
                        failCnt.countDown();
                    else
                        assert false : "Unexpected event type: " + evt;

                    return true;
                }
            }, EVT_NODE_JOINED, EVT_NODE_FAILED);

            startGrid("FailBeforeNodeAddedSentSpi");

            startGrid(3);

            assert joinCnt.await(10, SECONDS);
            assert failCnt.await(10, SECONDS);
        }
        finally {
            stopAllGrids();
        }
    }

    /**
     * @throws Exception If any error occurs.
     */
    public void testFailBeforeNodeLeftSent() throws Exception {
        try {
            startGrid(1);
            startGrid(2);

            startGrid("FailBeforeNodeLeftSentSpi");

            Grid g3 = startGrid(3);

            final CountDownLatch cnt = new CountDownLatch(1);

            g3.events().localListen(new GridPredicate<GridEvent>() {
                @Override public boolean apply(GridEvent evt) {
                    cnt.countDown();

                    return true;
                }
            }, EVT_NODE_FAILED);

            stopGrid(1);

            assert cnt.await(20, SECONDS);
        }
        finally {
            stopAllGrids();
        }
    }

    /**
     * @throws Exception If any error occurs.
     */
    public void testIpFinderCleaning() throws Exception {
        try {
            ipFinder.registerAddresses(Arrays.asList(new InetSocketAddress("host1", 1024),
                new InetSocketAddress("host2", 1024)));

            Grid g1 = startGrid(1);

            long timeout = (long)(discoMap.get(g1.name()).getStoresCleanFrequency() * 1.5);

            Thread.sleep(timeout);

            assert ipFinder.getRegisteredAddresses().size() == 1 : "ipFinder=" + ipFinder.getRegisteredAddresses();

            // Check that missing addresses are returned back.
            ipFinder.unregisterAddresses(ipFinder.getRegisteredAddresses()); // Unregister valid address.

            ipFinder.registerAddresses(Arrays.asList(new InetSocketAddress("host1", 1024),
                new InetSocketAddress("host2", 1024)));

            Thread.sleep(timeout);

            assert ipFinder.getRegisteredAddresses().size() == 1 : "ipFinder=" + ipFinder.getRegisteredAddresses();
        }
        finally {
            stopAllGrids();
        }
    }

    /**
     * @throws Exception If any error occurs.
     */
    public void testMetricsStoreCleaning() throws Exception {
        try {
            metricsStore.updateLocalMetrics(UUID.randomUUID(), new GridDiscoveryMetricsAdapter());
            metricsStore.updateLocalMetrics(UUID.randomUUID(), new GridDiscoveryMetricsAdapter());
            metricsStore.updateLocalMetrics(UUID.randomUUID(), new GridDiscoveryMetricsAdapter());

            Grid g1 = startGrid("MetricsStore-1");

            long timeout = (long)(discoMap.get(g1.name()).getStoresCleanFrequency() * 1.5);

            Thread.sleep(timeout);

            assert metricsStore.allNodeIds().isEmpty();

            startGrid("MetricsStore-2");

            Thread.sleep(discoMap.get(g1.name()).getHeartbeatFrequency() * 2);

            assert metricsStore.allNodeIds().size() == 2;
        }
        finally {
            stopAllGrids();
        }
    }

    /**
     * @throws Exception If any error occurs.
     */
    public void testNonSharedIpFinder() throws Exception {
        try {
            GridTestUtils.runMultiThreadedAsync(new Callable<Object>() {
                @Override public Object call() throws Exception {
                    Thread.sleep(4000);

                    return startGrid("NonSharedIpFinder-2");
                }
            }, 1, "grid-starter");

            // This node should wait until any node "from ipFinder" appears, see log messages.
            Grid g = startGrid("NonSharedIpFinder-1");

            assert g.localNode().order() == 2;
        }
        finally {
            stopAllGrids();
        }
    }

    /**
     * @throws Exception If any error occurs.
     */
    public void testMulticastIpFinder() throws Exception {
        try {
            for (int i = 0; i < 5; i++) {
                Grid g = startGrid("MulticastIpFinder-" + i);

                assertEquals(i + 1, g.nodes().size());

                GridTcpDiscoverySpi spi = (GridTcpDiscoverySpi)g.configuration().getDiscoverySpi();

                GridTcpDiscoveryMulticastIpFinder ipFinder = (GridTcpDiscoveryMulticastIpFinder)spi.getIpFinder();

                boolean found = false;

                for (GridPortRecord rec : ((GridKernal) g).context().ports().records()) {
                    if ((rec.protocol() == UDP) && rec.port() == ipFinder.getMulticastPort()) {
                        found = true;

                        break;
                    }
                }

                assertTrue("GridTcpDiscoveryMulticastIpFinder should register port." , found);
            }
        }
        finally {
            stopAllGrids();
        }
    }

    /**
     * @throws Exception If any error occurs.
     */
    public void testInvalidAddressIpFinder() throws Exception {
        ipFinder.setShared(false);

        ipFinder.setAddresses(Collections.singletonList("some-host"));

        try {
            GridTestUtils.assertThrows(
                log,
                new Callable<Object>() {
                    @Nullable @Override public Object call() throws Exception {
                        startGrid(1);

                        return null;
                    }
                },
                GridException.class,
                null);
        }
        finally {
            stopAllGrids();
        }
    }

    /**
     * @throws Exception If any error occurs.
     */
    public void testJoinTimeout() throws Exception {
        try {
            // This start will fail as expected.
            Throwable t = GridTestUtils.assertThrows(log, new Callable<Object>() {
                @Override public Object call() throws Exception {
                    startGrid("NonSharedIpFinder-1");

                    return null;
                }
            }, GridException.class, null);

            assert X.hasCause(t, GridSpiException.class) : "Unexpected exception: " + t;
        }
        finally {
            stopAllGrids();
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testDirtyIpFinder() throws Exception {
        try {
            // Dirty IP finder
            for (int i = 47500; i < 47520; i++)
                ipFinder.registerAddresses(Arrays.asList(new InetSocketAddress("127.0.0.1", i),
                    new InetSocketAddress("unknown-host", i)));

            assert ipFinder.isShared();

            startGrid(1);
        }
        finally {
            stopAllGrids();
        }
    }

    /**
     * @throws Exception If any error occurs.
     */
    public void testDuplicateId() throws Exception {
        try {
            // Random ID.
            startGrid(1);

            nodeId = UUID.randomUUID();

            startGrid(2);

            // Duplicate ID.
            GridTestUtils.assertThrows(
                log,
                new Callable<Object>() {
                    @Nullable @Override public Object call() throws Exception {
                        // Exception will be thrown and output to log.
                        startGrid(3);

                        return null;
                    }
                },
                GridException.class,
                null);
        }
        finally {
            stopAllGrids();
        }
    }

    /**
     * @throws Exception If any error occurs.
     */
    public void testLoopbackProblemFirstNodeOnLoopback() throws Exception {
        // On Windows and Mac machines two nodes can reside on the same port
        // (if one node has localHost="127.0.0.1" and another has localHost="0.0.0.0").
        // So two nodes do not even discover each other.
        if (U.isWindows() || U.isMacOs())
            return;

        try {
            startGridNoOptimize(1);

            GridTestUtils.assertThrows(
                log,
                new Callable<Object>() {
                    @Nullable @Override public Object call() throws Exception {
                        // Exception will be thrown because we start node which does not use loopback address,
                        // but the first node does.
                        startGridNoOptimize("LoopbackProblemTest");

                        return null;
                    }
                },
                GridException.class,
                null);
        }
        finally {
            stopAllGrids();
        }
    }

    /**
     * @throws Exception If any error occurs.
     */
    public void testLoopbackProblemSecondNodeOnLoopback() throws Exception {
        if (U.isWindows() || U.isMacOs())
            return;

        try {
            startGridNoOptimize("LoopbackProblemTest");

            GridTestUtils.assertThrows(
                log,
                new Callable<Object>() {
                    @Nullable @Override public Object call() throws Exception {
                        // Exception will be thrown because we start node which uses loopback address,
                        // but the first node does not.
                        startGridNoOptimize(1);

                        return null;
                    }
                },
                GridException.class,
                null);
        }
        finally {
            stopAllGrids();
        }
    }

    /**
     * @throws Exception If any error occurs.
     */
    public void testGridStartTime() throws Exception {
        try {
            startGridsMultiThreaded(5);

            Long startTime = null;

            GridKernal firstGrid = null;

            Collection<GridKernal> grids = new ArrayList<>();

            for (int i = 0; i < 5 ; i++) {
                GridKernal grid = (GridKernal)grid(i);

                assertTrue(grid.context().discovery().gridStartTime() > 0);

                if (i > 0)
                    assertEquals(startTime, (Long)grid.context().discovery().gridStartTime());
                else
                    startTime = grid.context().discovery().gridStartTime();

                if (grid.localNode().order() == 1)
                    firstGrid = grid;
                else
                    grids.add(grid);
            }

            assertNotNull(firstGrid);

            stopGrid(firstGrid.name());

            for (GridKernal grid : grids)
                assertEquals(startTime, (Long)grid.context().discovery().gridStartTime());

            grids.add((GridKernal) startGrid(5));

            for (GridKernal grid : grids)
                assertEquals(startTime, (Long)grid.context().discovery().gridStartTime());
        }
        finally {
            stopAllGrids();
        }
    }

    /**
     * Starts new grid with given index. Method optimize is not invoked.
     *
     * @param idx Index of the grid to start.
     * @return Started grid.
     * @throws Exception If anything failed.
     */
    private Grid startGridNoOptimize(int idx) throws Exception {
        return startGridNoOptimize(getTestGridName(idx));
    }

    /**
     * Starts new grid with given name. Method optimize is not invoked.
     *
     * @param gridName Grid name.
     * @return Started grid.
     * @throws Exception If failed.
     */
    private Grid startGridNoOptimize(String gridName) throws Exception {
        return G.start(getConfiguration(gridName));
    }

    /**
     *
     */
    private static class FailBeforeNodeAddedSentSpi extends GridTcpDiscoverySpi {
        /** */
        private int i;

        /** {@inheritDoc} */
        @Override void onBeforeMessageSentAcrossRing(Serializable msg) {
            if (msg instanceof GridTcpDiscoveryNodeAddedMessage)
                if (++i == 2) {
                    simulateNodeFailure();

                    throw new RuntimeException("Avoid message sending: " + msg.getClass());
                }
        }
    }

    /**
     *
     */
    private static class FailBeforeNodeLeftSentSpi extends GridTcpDiscoverySpi {
        /** {@inheritDoc} */
        @Override void onBeforeMessageSentAcrossRing(Serializable msg) {
            if (msg instanceof GridTcpDiscoveryNodeLeftMessage) {
                simulateNodeFailure();

                throw new RuntimeException("Avoid message sending: " + msg.getClass());
            }
        }
    }
}
