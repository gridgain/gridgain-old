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

package org.gridgain.grid.spi.communication.tcp;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.kernal.managers.communication.*;
import org.gridgain.grid.kernal.processors.cache.distributed.near.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.marshaller.*;
import org.gridgain.grid.marshaller.jdk.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.spi.communication.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.grid.util.direct.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.testframework.*;
import org.gridgain.testframework.junits.common.*;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.concurrent.*;

import static org.gridgain.grid.cache.GridCachePreloadMode.*;
import static org.gridgain.grid.cache.GridCacheWriteSynchronizationMode.*;

/**
 * Special cases for GG-2329.
 */
public class GridCacheDhtLockBackupSelfTest extends GridCommonAbstractTest {
    /** Ip-finder. */
    private static GridTcpDiscoveryIpFinder ipFinder = new GridTcpDiscoveryVmIpFinder(true);

    /** Communication spi for grid start. */
    private GridCommunicationSpi commSpi;

    /** Marshaller used in test. */
    private GridMarshaller marsh = new GridJdkMarshaller();

    /**
     *
     */
    public GridCacheDhtLockBackupSelfTest() {
        super(false /*start grid. */);
    }

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        GridTcpDiscoverySpi disco = new GridTcpDiscoverySpi();

        disco.setIpFinder(ipFinder);

        cfg.setDiscoverySpi(disco);

        cfg.setCacheConfiguration(cacheConfiguration());

        cfg.setMarshaller(marsh);

        assert commSpi != null;

        cfg.setCommunicationSpi(commSpi);

        return cfg;
    }

    /**
     * @return Cache configuration.
     */
    protected GridCacheConfiguration cacheConfiguration() {
        GridCacheConfiguration cacheCfg = defaultCacheConfiguration();

        cacheCfg.setCacheMode(GridCacheMode.PARTITIONED);
        cacheCfg.setWriteSynchronizationMode(FULL_ASYNC);
        cacheCfg.setPreloadMode(SYNC);
        cacheCfg.setDgcFrequency(0);

        return cacheCfg;
    }

    /**
     * @throws Exception If test failed.
     */
    @SuppressWarnings({"TooBroadScope"})
    public void testLock() throws Exception {
        final int kv = 1;

        Grid grid1 = startGridWithSpi(1, new TestCommunicationSpi(GridNearUnlockRequest.class, 1000));

        Grid grid2  = startGridWithSpi(2, new TestCommunicationSpi(GridNearUnlockRequest.class, 1000));

        if (!grid1.mapKeyToNode(null, kv).id().equals(grid1.localNode().id())) {
            Grid tmp = grid1;
            grid1 = grid2;
            grid2 = tmp;
        }

        // Now, grid1 is always primary node for key 1.
        final GridCache<Integer, String> cache1 = grid1.cache(null);
        final GridCache<Integer, String> cache2 = grid2.cache(null);

        info(">>> Primary: " + grid1.localNode().id());
        info(">>>  Backup: " + grid2.localNode().id());

        final CountDownLatch l1 = new CountDownLatch(1);

        Thread t1 = new GridTestThread(new Callable<Object>() {
            @Nullable @Override public Object call() throws Exception {
                info("Before lock for key: " + kv);

                assert cache1.lock(kv, 0L);

                info("After lock for key: " + kv);

                try {
                    assert cache1.isLocked(kv);
                    assert cache1.isLockedByThread(kv);

                    l1.countDown();

                    info("Let thread2 proceed.");

                    cache1.put(kv, Integer.toString(kv));

                    info("Put " + kv + '=' + Integer.toString(kv) + " key pair into cache.");
                }
                finally {
                    Thread.sleep(1000);

                    cache1.unlockAll(Collections.singleton(kv));

                    info("Unlocked key in thread 1: " + kv);
                }

                assert !cache1.isLockedByThread(kv);

                return null;
            }
        });

        Thread t2 = new GridTestThread(new Callable<Object>() {
            @Nullable @Override public Object call() throws Exception {
                info("Waiting for latch1...");

                l1.await();

                assert cache2.lock(kv, 0L);

                try {
                    String v = cache2.get(kv);

                    assert v != null : "Value is null for key: " + kv;
                    assertEquals(Integer.toString(kv), v);
                }
                finally {
                    cache2.unlockAll(Collections.singleton(kv));

                    info("Unlocked key in thread 2: " + kv);
                }

                assert !cache2.isLockedByThread(kv);

                return null;
            }
        });

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        info("Before remove all");

        cache1.flagsOn(GridCacheFlag.SYNC_COMMIT).removeAll();

        info("Remove all completed");

        if (!cache2.isEmpty()) {
            String failMsg = cache2.entrySet().toString();

            long start = System.currentTimeMillis();

            while (!cache2.isEmpty())
                U.sleep(100);

            long clearDuration = System.currentTimeMillis() - start;

            assertTrue("Cache on backup is not empty (was cleared in " + clearDuration + "ms): " + failMsg,
                clearDuration < 3000);
        }
    }

    /**
     * Starts grid with given communication spi set in configuration.
     *
     * @param idx Grid index.
     * @param commSpi Communication spi.
     * @return Started grid.
     * @throws Exception If grid start failed.
     */
    private Grid startGridWithSpi(int idx, GridCommunicationSpi commSpi) throws Exception {
        this.commSpi = commSpi;

        try {
            return startGrid(idx);
        }
        finally {
            this.commSpi = null;
        }
    }

    /**
     * Test communication spi that delays message sending.
     */
    private class TestCommunicationSpi extends GridTcpCommunicationSpi {
        /** Class of delayed messages. */
        private Class<?> delayedMsgCls;

        /** */
        private int delayTime;

        /** */
        @GridMarshallerResource
        private GridMarshaller marsh;

        /**
         * Creates test communication spi.
         *
         * @param delayedMsgCls Messages of this class will be delayed.
         * @param delayTime Time to be delayed.
         */
        private TestCommunicationSpi(Class delayedMsgCls, int delayTime) {
            this.delayedMsgCls = delayedMsgCls;
            this.delayTime = delayTime;
        }

        /**
         * Checks message and awaits when message is allowed to be sent if it is a checked message.
         *
         * @param obj Message being  sent.
         * @param srcNodeId Sender node id.
         */
        private void checkAwaitMessageType(GridTcpCommunicationMessageAdapter obj, UUID srcNodeId) {
            try {
                GridIoMessage plainMsg = (GridIoMessage)obj;

                Object msg = plainMsg.message();

                if (delayedMsgCls.isAssignableFrom(msg.getClass())) {
                    info(getSpiContext().localNode().id() + " received message from " + srcNodeId);

                    U.sleep(delayTime);
                }
            }
            catch (GridException e) {
                U.error(log, "Cannot process incoming message", e);
            }
        }

        /** {@inheritDoc} */
        @Override protected void notifyListener(UUID sndId, GridTcpCommunicationMessageAdapter msg,
            GridRunnable msgC) {
            checkAwaitMessageType(msg, sndId);

            super.notifyListener(sndId, msg, msgC);
        }
    }
}
