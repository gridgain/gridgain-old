/* @java.file.header */

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.spi.communication.tcp;

import org.gridgain.grid.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.spi.communication.*;
import org.gridgain.grid.util.direct.*;
import org.gridgain.grid.util.lang.*;
import org.gridgain.grid.util.nio.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.testframework.*;
import org.gridgain.testframework.junits.*;
import org.gridgain.testframework.junits.spi.*;

import java.util.*;
import java.util.concurrent.atomic.*;

/**
 *
 */
@GridSpiTest(spi = GridTcpCommunicationSpi.class, group = "Communication SPI")
public class GridTcpCommunicationSpiRecoveryAckSelfTest<T extends GridCommunicationSpi> extends GridSpiAbstractTest<T> {
    /** */
    private static final Collection<GridTestResources> spiRsrcs = new ArrayList<>();

    /** */
    protected static final List<GridTcpCommunicationSpi> spis = new ArrayList<>();

    /** */
    protected static final List<GridNode> nodes = new ArrayList<>();

    /** */
    private static final int SPI_CNT = 2;

    /**
     *
     */
    static {
        GridTcpCommunicationMessageFactory.registerCustom(new GridTcpCommunicationMessageProducer() {
            @Override public GridTcpCommunicationMessageAdapter create(byte type) {
                return new GridTestMessage();
            }
        }, GridTestMessage.DIRECT_TYPE);
    }

    /** */
    @SuppressWarnings({"deprecation"})
    private class TestListener implements GridCommunicationListener<GridTcpCommunicationMessageAdapter> {
        /** */
        private Set<Long> msgIds = new HashSet<>();

        /** */
        private AtomicInteger rcvCnt = new AtomicInteger();

        /** {@inheritDoc} */
        @Override public void onMessage(UUID nodeId, GridTcpCommunicationMessageAdapter msg, GridRunnable msgC) {
            info("Test listener received message: " + msg);

            assertTrue("Unexpected message: " + msg, msg instanceof GridTestMessage);

            GridTestMessage msg0 = (GridTestMessage)msg;

            assertTrue("Duplicated message received: " + msg0, msgIds.add(msg0.getMsgId()));

            rcvCnt.incrementAndGet();

            msgC.run();
        }

        /** {@inheritDoc} */
        @Override public void onDisconnected(UUID nodeId) {
            // No-op.
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testAckOnIdle() throws Exception {
        checkAck(10, 2000, 9);
    }

    /**
     * @throws Exception If failed.
     */
    public void testAckOnCount() throws Exception {
        checkAck(10, 60_000, 10);
    }

    /**
     * @param ackCnt Recovery acknowledgement count.
     * @param idleTimeout Idle connection timeout.
     * @param msgPerIter Messages per iteration.
     * @throws Exception If failed.
     */
    private void checkAck(int ackCnt, int idleTimeout, int msgPerIter) throws Exception {
        createSpis(ackCnt, idleTimeout);

        try {
            GridTcpCommunicationSpi spi0 = spis.get(0);
            GridTcpCommunicationSpi spi1 = spis.get(1);

            GridNode node0 = nodes.get(0);

            GridNode node1 = nodes.get(1);

            int msgId = 0;

            int expMsgs = 0;

            for (int i = 0; i < 5; i++) {
                info("Iteration: " + i);

                for (int j = 0; j < msgPerIter; j++) {
                    spi0.sendMessage(node1, new GridTestMessage(node0.id(), ++msgId, 0));

                    spi1.sendMessage(node0, new GridTestMessage(node1.id(), ++msgId, 0));
                }

                expMsgs += msgPerIter;

                for (GridTcpCommunicationSpi spi : spis) {
                    GridNioServer srv = U.field(spi, "nioSrvr");

                    Collection<? extends GridNioSession> sessions = GridTestUtils.getFieldValue(srv, "sessions");

                    assertFalse(sessions.isEmpty());

                    boolean found = false;

                    for (GridNioSession ses : sessions) {
                        final GridNioRecoveryDescriptor recoveryDesc = ses.recoveryDescriptor();

                        if (recoveryDesc != null) {
                            found = true;

                            GridTestUtils.waitForCondition(new GridAbsPredicate() {
                                @Override public boolean apply() {
                                    return recoveryDesc.messagesFutures().isEmpty();
                                }
                            }, 10_000);

                            assertEquals("Unexpected messages: " + recoveryDesc.messagesFutures(), 0,
                                recoveryDesc.messagesFutures().size());

                            break;
                        }
                    }

                    assertTrue(found);
                }

                final int expMsgs0 = expMsgs;

                for (GridTcpCommunicationSpi spi : spis) {
                    final TestListener lsnr = (TestListener)spi.getListener();

                    GridTestUtils.waitForCondition(new GridAbsPredicate() {
                        @Override public boolean apply() {
                            return lsnr.rcvCnt.get() >= expMsgs0;
                        }
                    }, 5000);

                    assertEquals(expMsgs, lsnr.rcvCnt.get());
                }
            }
        }
        finally {
            stopSpis();
        }
    }

    /**
     * @param ackCnt Recovery acknowledgement count.
     * @param idleTimeout Idle connection timeout.
     * @return SPI instance.
     */
    protected GridTcpCommunicationSpi getSpi(int ackCnt, int idleTimeout) {
        GridTcpCommunicationSpi spi = new GridTcpCommunicationSpi();

        spi.setSharedMemoryPort(-1);
        spi.setLocalPort(GridTestUtils.getNextCommPort(getClass()));
        spi.setIdleConnectionTimeout(idleTimeout);
        spi.setTcpNoDelay(true);
        spi.setRecoveryAcknowledgementCount(ackCnt);

        return spi;
    }

    /**
     * @param ackCnt Recovery acknowledgement count.
     * @param idleTimeout Idle connection timeout.
     * @throws Exception If failed.
     */
    private void createSpis(int ackCnt, int idleTimeout) throws Exception {
        spis.clear();
        nodes.clear();
        spiRsrcs.clear();

        Map<GridNode, GridSpiTestContext> ctxs = new HashMap<>();

        for (int i = 0; i < SPI_CNT; i++) {
            GridTcpCommunicationSpi spi = getSpi(ackCnt, idleTimeout);

            GridTestUtils.setFieldValue(spi, "gridName", "grid-" + i);

            GridTestResources rsrcs = new GridTestResources();

            GridTestNode node = new GridTestNode(rsrcs.getNodeId());

            GridSpiTestContext ctx = initSpiContext();

            ctx.setLocalNode(node);

            spiRsrcs.add(rsrcs);

            rsrcs.inject(spi);

            spi.setListener(new TestListener());

            node.setAttributes(spi.getNodeAttributes());

            nodes.add(node);

            spi.spiStart(getTestGridName() + (i + 1));

            spis.add(spi);

            spi.onContextInitialized(ctx);

            ctxs.put(node, ctx);
        }

        // For each context set remote nodes.
        for (Map.Entry<GridNode, GridSpiTestContext> e : ctxs.entrySet()) {
            for (GridNode n : nodes) {
                if (!n.equals(e.getKey()))
                    e.getValue().remoteNodes().add(n);
            }
        }
    }

    /**
     * @throws Exception If failed.
     */
    private void stopSpis() throws Exception {
        for (GridCommunicationSpi<GridTcpCommunicationMessageAdapter> spi : spis) {
            spi.setListener(null);

            spi.spiStop();
        }

        for (GridTestResources rsrcs : spiRsrcs) {
            rsrcs.stopThreads();
        }

        spis.clear();
        nodes.clear();
        spiRsrcs.clear();
    }
}
