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

package org.gridgain.grid.kernal.processors.cache.distributed;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.cache.affinity.*;
import org.gridgain.grid.events.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.kernal.managers.communication.*;
import org.gridgain.grid.kernal.processors.cache.distributed.dht.preloader.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.communication.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.grid.util.direct.*;
import org.gridgain.testframework.*;
import org.gridgain.testframework.junits.common.*;

import java.util.*;
import java.util.concurrent.*;

import static java.util.concurrent.TimeUnit.*;
import static org.gridgain.grid.events.GridEventType.*;

/**
 *
 */
public class GridCachePartitionNotLoadedEventSelfTest extends GridCommonAbstractTest {
    /** */
    private static final GridTcpDiscoveryIpFinder IP_FINDER = new GridTcpDiscoveryVmIpFinder(true);

    /** */
    private int backupCnt;

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        if (gridName.matches(".*\\d")) {
            String idStr = UUID.randomUUID().toString();

            char[] chars = idStr.toCharArray();

            chars[chars.length - 3] = '0';
            chars[chars.length - 2] = '0';
            chars[chars.length - 1] = gridName.charAt(gridName.length() - 1);

            cfg.setNodeId(UUID.fromString(new String(chars)));
        }

        cfg.setCommunicationSpi(new TestTcpCommunicationSpi());

        GridCacheConfiguration cacheCfg = new GridCacheConfiguration();

        cacheCfg.setCacheMode(GridCacheMode.PARTITIONED);
        cacheCfg.setBackups(backupCnt);

        cfg.setCacheConfiguration(cacheCfg);

        GridTcpDiscoverySpi disco = new GridTcpDiscoverySpi();

        disco.setIpFinder(IP_FINDER);

        cfg.setDiscoverySpi(disco);

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        stopAllGrids();
    }

    /**
     * @throws Exception If failed.
     */
    public void testPrimaryAndBackupDead() throws Exception {
        backupCnt = 1;

        startGrid(0);
        startGrid(1);
        startGrid(2);

        PartitionNotFullyLoadedListener lsnr = new PartitionNotFullyLoadedListener();

        grid(2).events().localListen(lsnr, EVT_CACHE_PRELOAD_PART_DATA_LOST);

        GridCacheAffinity<Object> aff = grid(0).cache(null).affinity();

        int key = 0;

        while (!aff.isPrimary(grid(0).localNode(), key)
            || !aff.isBackup(grid(1).localNode(), key))
            key++;

        GridCache<Integer, Integer> cache = cache(2);

        cache.put(key, key);

        assert cache(0).containsKey(key);
        assert cache(1).containsKey(key);

        stopGrid(0);
        stopGrid(1);

        awaitPartitionMapExchange();

        assert !cache.containsKey(key);

        assert lsnr.latch.await(5, SECONDS);
    }

    /**
     * @throws Exception If failed.
     */
    public void testPrimaryDead() throws Exception {
        backupCnt = 0;

        startGrid(0);
        startGrid(1);

        PartitionNotFullyLoadedListener lsnr = new PartitionNotFullyLoadedListener();

        grid(1).events().localListen(lsnr, EVT_CACHE_PRELOAD_PART_DATA_LOST);

        GridCache<Integer, Integer> cache0 = cache(0);

        int key = primaryKey(cache0);

        cache(1).put(key, key);

        assert cache0.containsKey(key);

        stopGrid(0, true);

        awaitPartitionMapExchange();

        assert !cache(1).containsKey(key);

        assert lsnr.latch.await(5, SECONDS);
    }


    /**
     * @throws Exception If failed.
     */
    public void testStableTopology() throws Exception {
        backupCnt = 1;

        startGrid(1);

        awaitPartitionMapExchange();

        startGrid(0);

        PartitionNotFullyLoadedListener lsnr = new PartitionNotFullyLoadedListener();

        grid(1).events().localListen(lsnr, EVT_CACHE_PRELOAD_PART_DATA_LOST);

        GridCache<Integer, Integer> cache0 = cache(0);

        int key = primaryKey(cache0);

        cache(1).put(key, key);

        assert cache0.containsKey(key);

        stopGrid(0, true);

        awaitPartitionMapExchange();

        assert cache(1).containsKey(key);

        assert !lsnr.latch.await(5, SECONDS);
    }


    /**
     * @throws Exception If failed.
     */
    public void testMapPartitioned() throws Exception {
        backupCnt = 0;

        startGrid(0);

        startGrid(1);

        awaitPartitionMapExchange();

        PartitionNotFullyLoadedListener lsnr = new PartitionNotFullyLoadedListener();

        grid(1).events().localListen(lsnr, EVT_CACHE_PRELOAD_PART_DATA_LOST);

        TestTcpCommunicationSpi.skipMsgType(grid(0), GridDhtPartitionsFullMessage.class);

        GridFuture<Object> fut = GridTestUtils.runAsync(new Callable<Object>() {
            @Override public Object call() throws Exception {
                startGrid(2);

                return null;
            }
        });

        boolean timeout = false;

        try {
            fut.get(2, SECONDS);
        }
        catch (GridFutureTimeoutException e) {
            timeout = true;
        }

        assert timeout;

        stopGrid(0, true);

        assert lsnr.latch.await(5, SECONDS);
    }

    /**
     *
     */
    private static class PartitionNotFullyLoadedListener implements GridPredicate<GridEvent> {
        /** */
        private final CountDownLatch latch = new CountDownLatch(1);

        /** {@inheritDoc} */
        @Override public boolean apply(GridEvent evt) {
            latch.countDown();

            return true;
        }
    }

    /**
     * TcpCommunicationSpi with additional features needed for tests.
     */
    public static class TestTcpCommunicationSpi extends GridTcpCommunicationSpi {
        /** */
        private volatile Class ignoreMsg;

        /** {@inheritDoc} */
        @Override public void sendMessage(GridNode node, GridTcpCommunicationMessageAdapter msg)
            throws GridSpiException {
            if (ignoreMsg != null && ((GridIoMessage)msg).message().getClass().equals(ignoreMsg))
                return;

            super.sendMessage(node, msg);
        }

        /**
         *
         */
        public void stop(Class ignoreMsg) {
            this.ignoreMsg = ignoreMsg;
        }

        /**
         * Skip messages will not send anymore.
         */
        public static void skipMsgType(GridEx node, Class clazz) {
            ((TestTcpCommunicationSpi)node.configuration().getCommunicationSpi()).stop(clazz);
        }
    }

}
