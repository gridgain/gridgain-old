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

package org.gridgain.grid.kernal;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.events.*;
import org.gridgain.grid.kernal.managers.discovery.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.product.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.lang.*;
import org.gridgain.testframework.junits.common.*;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static java.util.concurrent.TimeUnit.*;
import static org.gridgain.grid.product.GridProductVersion.*;
import static org.gridgain.grid.events.GridEventType.*;

/**
 *  GridDiscovery self test.
 */
public class GridDiscoverySelfTest extends GridCommonAbstractTest {
    /** IP finder. */
    private static final GridTcpDiscoveryIpFinder IP_FINDER = new GridTcpDiscoveryVmIpFinder(true);

    /** */
    private static Grid grid;

    /** Nodes count. */
    private static final int NODES_CNT = 5;

    /** Maximum timeout when remote nodes join/left the topology */
    private static final int MAX_TIMEOUT_IN_MINS = 5;

    /** */
    public GridDiscoverySelfTest() {
        super(/*start grid*/true);
    }

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        GridTcpDiscoverySpi discoSpi = new GridTcpDiscoverySpi();

        discoSpi.setIpFinder(IP_FINDER);

        cfg.setDiscoverySpi(discoSpi);

        GridCacheConfiguration cacheCfg = defaultCacheConfiguration();

        //cacheCfg.setName(null);
        cacheCfg.setCacheMode(GridCacheMode.PARTITIONED);

        cfg.setCacheConfiguration(cacheCfg);

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        grid = G.grid(getTestGridName());
    }

    /**
     * @throws Exception If failed.
     */
    public void testGetRemoteNodes() throws Exception {
        Collection<GridNode> nodes = grid.forRemotes().nodes();

        printNodes(nodes);
    }

    /**
     * @throws Exception If failed.
     */
    public void testGetAllNodes() throws Exception {
        Collection<GridNode> nodes = grid.nodes();

        printNodes(nodes);

        assert nodes != null;
        assert !nodes.isEmpty();
    }

    /**
     * @throws Exception If failed.
     */
    public void testGetTopologyHash() throws Exception {
        int hashCnt = 5000;

        Random rand = new Random();

        Collection<Long> hashes = new HashSet<>(hashCnt, 1.0f);

        for (int i = 0; i < hashCnt; i++) {
            // Max topology of 10 nodes.
            int size = rand.nextInt(10) + 1;

            Collection<GridNode> nodes = new ArrayList<>(size);

            for (int j = 0; j < size; j++)
                nodes.add(new GridDiscoveryTestNode());

            @SuppressWarnings("deprecation")
            long hash = ((GridKernal)grid).context().discovery().topologyHash(nodes);

            boolean isHashed = hashes.add(hash);

            assert isHashed : "Duplicate hash [hash=" + hash + ", topSize=" + size + ", iteration=" + i + ']';
        }

        info("No duplicates found among '" + hashCnt + "' hashes.");
    }

    /**
     * @throws Exception If failed.
     */
    @SuppressWarnings({"SuspiciousMethodCalls"})
    public void testGetLocalNode() throws Exception {
        GridNode node = grid.localNode();

        assert node != null;

        Collection<GridNode> nodes = grid.nodes();

        assert nodes != null;
        assert nodes.contains(node);
    }

    /**
     * @throws Exception If failed.
     */
    public void testPingNode() throws Exception {
        GridNode node = grid.localNode();

        assert node != null;

        boolean pingRes = grid.pingNode(node.id());

        assert pingRes : "Failed to ping local node.";
    }

    /**
     * @throws Exception If failed.
     */
    public void testDiscoveryListener() throws Exception {
        GridNode node = grid.localNode();

        assert node != null;

        final AtomicInteger cnt = new AtomicInteger();

        /** Joined nodes counter. */
        final CountDownLatch joinedCnt = new CountDownLatch(NODES_CNT);

        /** Left nodes counter. */
        final CountDownLatch leftCnt = new CountDownLatch(NODES_CNT);

        GridPredicate<GridEvent> lsnr = new GridPredicate<GridEvent>() {
            @Override public boolean apply(GridEvent evt) {
                if (EVT_NODE_JOINED == evt.type()) {
                    cnt.incrementAndGet();

                    joinedCnt.countDown();
                }
                else if (EVT_NODE_LEFT == evt.type()) {
                    int i = cnt.decrementAndGet();

                    assert i >= 0;

                    leftCnt.countDown();
                }
                else
                    assert false;

                return true;
            }
        };

        grid.events().localListen(lsnr, EVT_NODE_LEFT, EVT_NODE_JOINED);

        try {
            for (int i = 0; i < NODES_CNT; i++)
                startGrid(i);

            joinedCnt.await(MAX_TIMEOUT_IN_MINS, MINUTES);

            assert cnt.get() == NODES_CNT;

            for (int i = 0; i < NODES_CNT; i++)
                stopGrid(i);

            leftCnt.await(MAX_TIMEOUT_IN_MINS, MINUTES);

            assert cnt.get() == 0;

            grid.events().stopLocalListen(lsnr);

            assert cnt.get() == 0;
        }
        finally {
            for (int i = 0; i < NODES_CNT; i++)
                stopAndCancelGrid(i);
        }
    }

    /**
     * Test cache nodes resolved correctly from topology history.
     *
     * @throws Exception In case of any exception.
     */
    public void testCacheNodes() throws Exception {
        // Validate only original node is available.
        GridDiscoveryManager discoMgr = ((GridKernal)grid).context().discovery();

        Collection<GridNode> nodes = discoMgr.allNodes();

        assert nodes.size() == 1 : "Expects only original node is available: " + nodes;

        final long topVer0 = discoMgr.topologyVersion();

        assert topVer0 > 0 : "Unexpected initial topology version: " + topVer0;

        List<UUID> uuids = new ArrayList<>(NODES_CNT);

        UUID locId = grid.localNode().id();

        try {
            // Start nodes.
            for (int i = 0; i < NODES_CNT; i++)
                uuids.add(startGrid(i).localNode().id());

            // Stop nodes.
            for (int i = 0; i < NODES_CNT; i++)
                stopGrid(i);

            final long topVer = discoMgr.topologyVersion();

            assert topVer == topVer0 + NODES_CNT * 2 : "Unexpected topology version: " + topVer;

            for (long ver = topVer0; ver <= topVer; ver++) {
                Collection<UUID> exp = new ArrayList<>();

                exp.add(locId);

                for (int i = 0; i < NODES_CNT && i < ver - topVer0; i++)
                    exp.add(uuids.get(i));

                for (int i = 0; i < ver - topVer0 - NODES_CNT; i++)
                    exp.remove(uuids.get(i));

                // Cache nodes by topology version (e.g. NODE_CNT == 3).
                //            0 1 2 3 (node id)
                // 1 (topVer) +       - only local node
                // 2          + +
                // 3          + + +
                // 4          + + + +
                // 5          +   + +
                // 6          +     +
                // 7          +       - only local node

                Collection<GridNode> cacheNodes = discoMgr.cacheNodes(null, ver);

                Collection<UUID> act = new ArrayList<>(F.viewReadOnly(cacheNodes, new C1<GridNode, UUID>() {
                    @Override public UUID apply(GridNode n) {
                        return n.id();
                    }
                }));

                assertEquals("Expects correct cache nodes for topology version: " + ver, exp, act);
            }
        }
        finally {
            for (int i = 0; i < NODES_CNT; i++)
                stopAndCancelGrid(i);
        }
    }

    /**
     * @param nodes Nodes.
     */
    private void printNodes(Collection<GridNode> nodes) {
        StringBuilder buf = new StringBuilder();

        if (nodes != null && !nodes.isEmpty()) {
            buf.append("Found nodes [nodes={");

            int i = 0;

            for (Iterator<GridNode> iter = nodes.iterator(); iter.hasNext(); i++) {
                GridNode node = iter.next();

                buf.append(node.id());

                if (i + 1 != nodes.size())
                    buf.append(", ");
            }

            buf.append("}]");
        }
        else
            buf.append("Found no nodes.");

        if (log().isDebugEnabled())
            log().debug(buf.toString());
    }

    /**
     *
     */
    private static class GridDiscoveryTestNode extends GridMetadataAwareAdapter implements GridNode {
        /** */
        private static AtomicInteger consistentIdCtr = new AtomicInteger();

        /** */
        private UUID nodeId = UUID.randomUUID();

        /** */
        private Object consistentId = consistentIdCtr.incrementAndGet();

        /** {@inheritDoc} */
        @Override public long order() {
            return -1;
        }

        /** {@inheritDoc} */
        @Override public GridProductVersion version() {
            return fromString("99.99.99");
        }

        /** {@inheritDoc} */
        @Override public UUID id() {
            return nodeId;
        }

        /** {@inheritDoc} */
        @Override public Object consistentId() {
            return consistentId;
        }

        /** {@inheritDoc} */
        @SuppressWarnings("unchecked")
        @Nullable @Override public <T> T attribute(String name) {
            return null;
        }

        /** {@inheritDoc} */
        @Nullable @Override public GridNodeMetrics metrics() {
            return null;
        }

        /** {@inheritDoc} */
        @Nullable @Override public Map<String, Object> attributes() {
            return null;
        }

        /** {@inheritDoc} */
        @Override public Collection<String> addresses() {
            return null;
        }

        /** {@inheritDoc} */
        @Override public boolean isLocal() {
            return false;
        }

        /** {@inheritDoc} */
        @Override public boolean isDaemon() {
            return false;
        }

        /** {@inheritDoc} */
        @Override public Collection<String> hostNames() {
            return null;
        }

        /** {@inheritDoc} */
        @Override public boolean equals(Object o) {
            return F.eqNodes(this, o);
        }

        /** {@inheritDoc} */
        @Override public int hashCode() {
            return id().hashCode();
        }
    }
}
