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

package org.gridgain.grid.kernal.processors.continuous;

import org.gridgain.grid.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.messaging.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.testframework.*;
import org.gridgain.testframework.junits.common.*;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static java.util.concurrent.TimeUnit.*;

/**
 * Message listen test.
 */
public class GridMessageListenSelfTest extends GridCommonAbstractTest {
    /** */
    private static final int GRID_CNT = 3;

    /** */
    private static final String INC_ATTR = "include";

    /** */
    private static final String MSG = "Message";

    /** */
    private static final String TOPIC = "Topic";

    /** */
    private static final int MSG_CNT = 3;

    /** */
    private static final String TOPIC_CLS_NAME = "org.gridgain.grid.tests.p2p.GridTestMessageTopic";

    /** */
    private static final String LSNR_CLS_NAME = "org.gridgain.grid.tests.p2p.GridTestMessageListener";

    /** */
    private static boolean include;

    /** */
    private static final List<UUID> allNodes = new ArrayList<>();

    /** */
    private static final List<UUID> rmtNodes = new ArrayList<>();

    /** */
    private static final List<UUID> incNodes = new ArrayList<>();

    /** */
    private static final Collection<UUID> nodes = new GridConcurrentHashSet<>();

    /** */
    private static final AtomicInteger cnt = new AtomicInteger();

    /** */
    private static CountDownLatch latch;

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        if (include)
            cfg.setUserAttributes(F.asMap(INC_ATTR, true));

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        nodes.clear();
        cnt.set(0);

        include = true;

        startGridsMultiThreaded(GRID_CNT - 1);

        include = false;

        Thread.sleep(500);

        startGrid(GRID_CNT - 1);

        allNodes.clear();
        rmtNodes.clear();
        incNodes.clear();

        for (int i = 0; i < GRID_CNT; i++) {
            UUID id = grid(i).localNode().id();

            allNodes.add(id);

            if (i != 0)
                rmtNodes.add(id);

            if (i != GRID_CNT - 1)
                incNodes.add(id);
        }

        Collections.sort(allNodes);
        Collections.sort(rmtNodes);
        Collections.sort(incNodes);
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        stopAllGrids();
    }

    /**
     * @throws Exception If failed.
     */
    public void testNullTopic() throws Exception {
        latch = new CountDownLatch(MSG_CNT * GRID_CNT);

        listen(grid(0), null, true);

        send();

        assert latch.await(2, SECONDS);

        Thread.sleep(500);

        assertEquals(MSG_CNT * GRID_CNT, cnt.get());

        checkNodes(allNodes);
    }

    /**
     * @throws Exception If failed.
     */
    public void testNonNullTopic() throws Exception {
        latch = new CountDownLatch(MSG_CNT * GRID_CNT);

        listen(grid(0), null, true);

        send();

        assert latch.await(2, SECONDS);

        Thread.sleep(500);

        assertEquals(MSG_CNT * GRID_CNT, cnt.get());

        checkNodes(allNodes);
    }

    /**
     * @throws Exception If failed.
     */
    public void testStopListen() throws Exception {
        latch = new CountDownLatch(GRID_CNT);

        listen(grid(0), null, false);

        send();

        assert latch.await(2, SECONDS);

        Thread.sleep(500);

        int expCnt = cnt.get();

        send();

        Thread.sleep(1000);

        assertEquals(expCnt, cnt.get());

        checkNodes(allNodes);
    }

    /**
     * @throws Exception If failed.
     */
    public void testProjection() throws Exception {
        latch = new CountDownLatch(MSG_CNT * (GRID_CNT - 1));

        listen(grid(0).forRemotes(), null, true);

        send();

        assert latch.await(2, SECONDS);

        Thread.sleep(500);

        assertEquals(MSG_CNT * (GRID_CNT - 1), cnt.get());

        checkNodes(rmtNodes);
    }

    /**
     * @throws Exception If failed.
     */
    public void testNodeJoin() throws Exception {
        latch = new CountDownLatch(MSG_CNT * (GRID_CNT + 1));

        listen(grid(0), null, true);

        try {
            Grid g = startGrid("anotherGrid");

            send();

            assert latch.await(2, SECONDS);

            Thread.sleep(500);

            assertEquals(MSG_CNT * (GRID_CNT + 1), cnt.get());

            List<UUID> allNodes0 = new ArrayList<>(allNodes);

            allNodes0.add(g.localNode().id());

            Collections.sort(allNodes0);

            checkNodes(allNodes0);
        }
        finally {
            stopGrid("anotherGrid");
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testNodeJoinWithProjection() throws Exception {
        latch = new CountDownLatch(MSG_CNT * GRID_CNT);

        listen(grid(0).forAttribute(INC_ATTR, null), null, true);

        try {
            include = true;

            Grid g = startGrid("anotherGrid1");

            include = false;

            startGrid("anotherGrid2");

            send();

            assert latch.await(2, SECONDS);

            Thread.sleep(500);

            assertEquals(MSG_CNT * GRID_CNT, cnt.get());

            List<UUID> incNodes0 = new ArrayList<>(incNodes);

            incNodes0.add(g.localNode().id());

            Collections.sort(incNodes0);

            checkNodes(incNodes0);
        }
        finally {
            stopGrid("anotherGrid1");
            stopGrid("anotherGrid2");
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testNullTopicWithDeployment() throws Exception {
        Class<?> cls = getExternalClassLoader().loadClass(LSNR_CLS_NAME);

        grid(0).message().remoteListen(null, (GridBiPredicate<UUID, Object>)cls.newInstance()).get();

        send();

        boolean s = GridTestUtils.waitForCondition(new PA() {
            @Override public boolean apply() {
                return checkDeployedListeners(GRID_CNT);
            }
        }, 2000);

        assertTrue(s);
    }

    /**
     * @throws Exception If failed.
     */
    public void testNonNullTopicWithDeployment() throws Exception {
        ClassLoader ldr = getExternalClassLoader();

        Class<?> topicCls = ldr.loadClass(TOPIC_CLS_NAME);
        Class<?> lsnrCls = ldr.loadClass(LSNR_CLS_NAME);

        Object topic = topicCls.newInstance();

        grid(0).message().remoteListen(topic, (GridBiPredicate<UUID, Object>)lsnrCls.newInstance()).get();

        send(topic);

        boolean s = GridTestUtils.waitForCondition(new PA() {
            @Override public boolean apply() {
                return checkDeployedListeners(GRID_CNT);
            }
        }, 2000);

        assertTrue(s);
    }

    /**
     * @throws Exception If failed.
     */
    public void testListenActor() throws Exception {
        latch = new CountDownLatch(MSG_CNT * (GRID_CNT + 1));

        grid(0).message().remoteListen(null, new Actor(grid(0))).get();

        try {
            Grid g = startGrid("anotherGrid");

            send();

            assert latch.await(2, SECONDS);

            Thread.sleep(500);

            assertEquals(MSG_CNT * (GRID_CNT + 1), cnt.get());

            List<UUID> allNodes0 = new ArrayList<>(allNodes);

            allNodes0.add(g.localNode().id());

            Collections.sort(allNodes0);

            checkNodes(allNodes0);
        }
        finally {
            stopGrid("anotherGrid");
        }
    }

    /**
     * @param prj Projection.
     * @param topic Topic.
     * @param ret Value returned from listener.
     * @throws Exception In case of error.
     */
    private void listen(final GridProjection prj, @Nullable Object topic, final boolean ret) throws Exception {
        assert prj != null;

        prj.message().remoteListen(topic, new Listener(prj, ret)).get();
    }

    /**
     * @throws Exception In case of error.
     */
    private void send() throws Exception {
        send(TOPIC);
    }

    /**
     * @param topic Non-null topic.
     * @throws Exception In case of error.
     */
    private void send(Object topic) throws Exception {
        assert topic != null;

        for (int i = 0; i < MSG_CNT; i++)
            grid(0).message().send(null, MSG);

        for (int i = 0; i < MSG_CNT; i++)
            grid(0).message().send(topic, MSG);
    }

    /**
     * @param expCnt Expected messages count.
     * @return If check passed.
     */
    private boolean checkDeployedListeners(int expCnt) {
        for (Grid g : G.allGrids()) {
            AtomicInteger cnt = g.<String, AtomicInteger>nodeLocalMap().get("msgCnt");

            if (cnt == null || cnt.get() != expCnt)
                return false;
        }

        return true;
    }

    /**
     * @param expNodes Expected nodes.
     */
    private void checkNodes(List<UUID> expNodes) {
        List<UUID> nodes0 = new ArrayList<>(nodes);

        Collections.sort(nodes0);

        assertEquals(expNodes, nodes0);
    }

    /** */
    private static class Listener implements P2<UUID, Object> {
        /** */
        private final GridProjection prj;

        /** */
        private final boolean ret;

        /** */
        @GridInstanceResource
        private Grid grid;

        /** */
        @GridLocalNodeIdResource
        private UUID locNodeId;

        /** */
        @GridExecutorServiceResource
        private ExecutorService exec;

        /**
         * @param prj Projection.
         * @param ret Return value.
         */
        private Listener(GridProjection prj, boolean ret) {
            this.prj = prj;
            this.ret = ret;
        }

        /** {@inheritDoc} */
        @Override public boolean apply(UUID nodeId, Object msg) {
            assertNotNull(grid);
            assertNotNull(locNodeId);
            assertNotNull(exec);

            X.println("Received message [nodeId=" + nodeId + ", locNodeId=" + grid.localNode().id() + ']');

            assertEquals(prj.grid().localNode().id(), nodeId);
            assertEquals(MSG, msg);

            nodes.add(locNodeId);
            cnt.incrementAndGet();
            latch.countDown();

            return ret;
        }
    }

    /** */
    private static class Actor extends GridMessagingListenActor<Object> {
        /** */
        private final GridProjection prj;

        /**
         * @param prj Projection.
         */
        private Actor(GridProjection prj) {
            this.prj = prj;
        }

        /** {@inheritDoc} */
        @Override protected void receive(UUID nodeId, Object msg) throws Throwable {
            assertNotNull(grid());

            UUID locNodeId = grid().localNode().id();

            X.println("Received message [nodeId=" + nodeId + ", locNodeId=" + locNodeId + ']');

            assertEquals(prj.grid().localNode().id(), nodeId);
            assertEquals(MSG, msg);

            nodes.add(locNodeId);
            cnt.incrementAndGet();
            latch.countDown();
        }
    }
}
