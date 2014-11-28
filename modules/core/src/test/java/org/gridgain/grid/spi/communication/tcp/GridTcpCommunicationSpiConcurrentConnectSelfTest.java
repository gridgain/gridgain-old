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
import org.gridgain.grid.util.nio.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.testframework.*;
import org.gridgain.testframework.junits.*;
import org.gridgain.testframework.junits.spi.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 *
 */
@GridSpiTest(spi = GridTcpCommunicationSpi.class, group = "Communication SPI")
public class GridTcpCommunicationSpiConcurrentConnectSelfTest<T extends GridCommunicationSpi> extends GridSpiAbstractTest<T> {
    /** */
    private static final int SPI_CNT = 2;

    /** */
    private static final Collection<GridTestResources> spiRsrcs = new ArrayList<>();

    /** */
    protected static final List<GridCommunicationSpi<GridTcpCommunicationMessageAdapter>> spis = new ArrayList<>();

    /** */
    protected static final List<GridNode> nodes = new ArrayList<>();

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

    /**
     *
     */
    private static class MessageListener implements GridCommunicationListener<GridTcpCommunicationMessageAdapter> {
        /** */
        private final CountDownLatch latch;

        /**
         * @param latch Latch.
         */
        MessageListener(CountDownLatch latch) {
            this.latch = latch;
        }

        /** {@inheritDoc} */
        @Override public void onMessage(UUID nodeId, GridTcpCommunicationMessageAdapter msg, GridRunnable msgC) {
            msgC.run();

            if (msg instanceof GridTestMessage) {
                assertEquals(nodeId, ((GridTestMessage)msg).getSourceNodeId());

                latch.countDown();
            }
        }

        /** {@inheritDoc} */
        @Override public void onDisconnected(UUID nodeId) {
            // No-op.
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testTwoThreads() throws Exception {
        concurrentConnect(2, 10, 100, false);
    }

    /**
     * @throws Exception If failed.
     */
    public void testManyThreads() throws Exception {
        int threads = Runtime.getRuntime().availableProcessors() * 5;

        concurrentConnect(threads, 10, 100, false);
    }

    /**
     * @throws Exception If failed.
     */
    public void testRandomSleep() throws Exception {
        concurrentConnect(4, 1, 100, true);
    }

    /**
     * @param threads Number of threads.
     * @param msgPerThread Messages per thread.
     * @param iters Number of iterations.
     * @param sleep If {@code true} sleeps random time before starts send messages.
     * @throws Exception If failed.
     */
    private void concurrentConnect(final int threads,
        final int msgPerThread,
        final int iters,
        final boolean sleep) throws Exception {
        log.info("Concurrent connect [threads=" + threads +
            ", msgPerThread=" + msgPerThread +
            ", iters=" + iters +
            ", sleep=" + sleep + ']');

        for (int i = 0; i < iters; i++) {
            log.info("Iteration: " + i);

            CountDownLatch latch = new CountDownLatch(threads * msgPerThread);

            MessageListener lsnr = new MessageListener(latch);

            createSpis(lsnr);

            final AtomicInteger idx = new AtomicInteger();

            try {
                GridTestUtils.runMultiThreaded(new Callable<Void>() {
                    @Override public Void call() throws Exception {
                        int idx0 = idx.getAndIncrement();

                        GridCommunicationSpi<GridTcpCommunicationMessageAdapter> spi = spis.get(idx0 % 2);

                        GridNode srcNode = nodes.get(idx0 % 2);

                        GridNode dstNode = nodes.get((idx0 + 1) % 2);

                        if (sleep) {
                            ThreadLocalRandom rnd = ThreadLocalRandom.current();

                            long millis = rnd.nextLong(10);

                            if (millis > 0)
                                Thread.sleep(millis);
                        }

                        for (int i = 0; i < msgPerThread; i++)
                            spi.sendMessage(dstNode, new GridTestMessage(srcNode.id(), 0, 0));

                        return null;
                    }
                }, threads, "test");

                assertTrue(latch.await(10, TimeUnit.SECONDS));

                for (GridCommunicationSpi spi : spis) {
                    ConcurrentMap<UUID, GridTcpCommunicationClient> clients = U.field(spi, "clients");

                    assertEquals(1, clients.size());

                    GridNioServer srv = U.field(spi, "nioSrvr");

                    Collection sessions = U.field(srv, "sessions");

                    int expSessions = dualSocket() ? 2 : 1;

                    assertEquals(expSessions, sessions.size());
                }
            }
            finally {
                stopSpis();
            }
        }
    }

    /**
     * @return SPI.
     */
    private GridCommunicationSpi createSpi() {
        GridTcpCommunicationSpi spi = new GridTcpCommunicationSpi();

        spi.setSharedMemoryPort(-1);
        spi.setLocalPort(GridTestUtils.getNextCommPort(getClass()));
        spi.setIdleConnectionTimeout(60_000);
        spi.setTcpNoDelay(true);
        spi.setDualSocketConnection(dualSocket());

        return spi;
    }

    /**
     * @return Value for {@link GridTcpCommunicationSpi#isDualSocketConnection()} property.
     */
    protected boolean dualSocket() {
        return false;
    }

    /**
     * @param lsnr Message listener.
     * @throws Exception If failed.
     */
    private void createSpis(MessageListener lsnr) throws Exception {
        spis.clear();
        nodes.clear();
        spiRsrcs.clear();

        Map<GridNode, GridSpiTestContext> ctxs = new HashMap<>();

        for (int i = 0; i < SPI_CNT; i++) {
            GridCommunicationSpi<GridTcpCommunicationMessageAdapter> spi = createSpi();

            GridTestUtils.setFieldValue(spi, "gridName", "grid-" + i);

            GridTestResources rsrcs = new GridTestResources();

            GridTestNode node = new GridTestNode(rsrcs.getNodeId());

            node.order(i + 1);

            GridSpiTestContext ctx = initSpiContext();

            ctx.setLocalNode(node);

            info(">>> Initialized context: nodeId=" + ctx.localNode().id());

            spiRsrcs.add(rsrcs);

            rsrcs.inject(spi);

            spi.setListener(lsnr);

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
    }

}
