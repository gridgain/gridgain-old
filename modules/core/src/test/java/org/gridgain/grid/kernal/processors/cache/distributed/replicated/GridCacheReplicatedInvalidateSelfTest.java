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

package org.gridgain.grid.kernal.processors.cache.distributed.replicated;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.kernal.managers.communication.*;
import org.gridgain.grid.kernal.processors.clock.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.communication.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.grid.util.direct.*;
import org.gridgain.testframework.junits.common.*;

import java.util.*;

import static org.gridgain.grid.cache.GridCacheMode.*;
import static org.gridgain.grid.cache.GridCachePreloadMode.*;
import static org.gridgain.grid.cache.GridCacheTxConcurrency.*;
import static org.gridgain.grid.cache.GridCacheTxIsolation.*;
import static org.gridgain.grid.cache.GridCacheWriteSynchronizationMode.*;

/**
 *
 */
public class GridCacheReplicatedInvalidateSelfTest extends GridCommonAbstractTest {
    /** Random number generator. */
    private static final Random RAND = new Random();

    /** Grid count. */
    private static final int GRID_CNT = 3;

    /** */
    private static final Integer KEY = 1;

    /** */
    private static final String VAL = "test";

    /** */
    private static GridTcpDiscoveryIpFinder ipFinder = new GridTcpDiscoveryVmIpFinder(true);

    /**
     * Don't start grid by default.
     */
    public GridCacheReplicatedInvalidateSelfTest() {
        super(false);
    }

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration c = super.getConfiguration(gridName);

        GridTcpDiscoverySpi disco = new GridTcpDiscoverySpi();

        disco.setIpFinder(ipFinder);

        c.setDiscoverySpi(disco);

        c.setCommunicationSpi(new TestCommunicationSpi());

        GridCacheConfiguration cc = defaultCacheConfiguration();

        cc.setPreloadMode(NONE);
        cc.setCacheMode(REPLICATED);
        cc.setTxSerializableEnabled(true);
        cc.setWriteSynchronizationMode(FULL_SYNC);

        c.setCacheConfiguration(cc);

        return c;
    }

    /**
     * @throws Exception If failed.
     */
    @Override protected void beforeTestsStarted() throws Exception {
        for (int i = 0; i < GRID_CNT; i++)
            startGrid(i);
    }

    /**
     * @throws Exception If failed.
     */
    @Override protected void afterTestsStopped() throws Exception {
        stopAllGrids();
    }

    /**
     * @throws Exception If failed.
     */
    @Override protected void beforeTest() throws Exception {
        for (int i = 0; i < GRID_CNT; i++)
            ioSpi(i).clearCounts();
    }

    /**
     * @param i Index.
     * @return IO SPI.
     */
    private TestCommunicationSpi ioSpi(int i) {
        return (TestCommunicationSpi)grid(i).configuration().getCommunicationSpi();
    }

    /**
     * @throws GridException If test failed.
     */
    public void testOptimisticReadCommitted() throws Throwable {
        checkCommit(OPTIMISTIC, READ_COMMITTED);
    }

    /**
     * @throws GridException If test failed.
     */
    public void testOptimisticRepeatableRead() throws Throwable {
        checkCommit(OPTIMISTIC, REPEATABLE_READ);
    }

    /**
     * @throws GridException If test failed.
     */
    public void testOptimisticSerializable() throws Throwable {
        checkCommit(OPTIMISTIC, SERIALIZABLE);
    }

    /**
     * @param concurrency Concurrency.
     * @param isolation Isolation.
     * @throws Throwable If check failed.
     */
    private void checkCommit(GridCacheTxConcurrency concurrency,
        GridCacheTxIsolation isolation) throws Throwable {
        int idx = RAND.nextInt(GRID_CNT);

        GridCache<Integer, String> cache = cache(idx);

        GridCacheTx tx = cache.txStart(concurrency, isolation, 0, 0);

        try {
            cache.put(KEY, VAL);

            tx.commit();
        }
        catch (GridCacheTxOptimisticException e) {
            log.warning("Optimistic transaction failure (will rollback) [msg=" + e.getMessage() + ", tx=" + tx + ']');

            tx.rollback();

            assert concurrency == OPTIMISTIC && isolation == SERIALIZABLE;

            assert false : "Invalid optimistic failure: " + tx;
        }
        catch (Throwable e) {
            error("Transaction failed (will rollback): " + tx, e);

            tx.rollback();

            throw e;
        }

        TestCommunicationSpi ioSpi = ioSpi(idx);

        int checkIdx = RAND.nextInt(GRID_CNT);

        while (checkIdx == idx)
            checkIdx = RAND.nextInt(GRID_CNT);

        Grid checkGrid = grid(checkIdx);

        int msgCnt = ioSpi.getMessagesCount(checkGrid.localNode().id());

        info("Checked node: " + checkGrid.localNode().id());

        assertEquals("Invalid message count for grid: " + checkGrid.localNode().id(), 2, msgCnt);
    }

    /**
     *
     */
    private class TestCommunicationSpi extends GridTcpCommunicationSpi {
        /** */
        private final Map<UUID, Integer> msgCntMap = new HashMap<>();

        /**
         * @param destNodeId Node id to check.
         * @return Number of messages that was sent to node.
         */
        public int getMessagesCount(UUID destNodeId) {
            synchronized (msgCntMap) {
                Integer cnt = msgCntMap.get(destNodeId);

                return cnt == null ? 0 : cnt;
            }
        }

        /**
         *  Clear message counts.
         */
        public void clearCounts() {
            synchronized (msgCntMap) {
                msgCntMap.clear();
            }
        }

        /** {@inheritDoc} */
        @Override public void sendMessage(GridNode destNode, GridTcpCommunicationMessageAdapter msg)
            throws GridSpiException {
            Object msg0 = ((GridIoMessage)msg).message();

            if (!(msg0 instanceof GridClockDeltaSnapshotMessage)) {
                info("Sending message [locNodeId=" + getLocalNodeId() + ", destNodeId= " + destNode.id()
                    + ", msg=" + msg + ']');

                synchronized (msgCntMap) {
                    Integer cnt = msgCntMap.get(destNode.id());

                    msgCntMap.put(destNode.id(), cnt == null ? 1 : cnt + 1);
                }
            }

            super.sendMessage(destNode, msg);
        }
    }
}
