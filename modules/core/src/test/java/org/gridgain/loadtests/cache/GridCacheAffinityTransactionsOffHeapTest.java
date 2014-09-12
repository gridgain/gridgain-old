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

package org.gridgain.loadtests.cache;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.cache.affinity.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;

import java.io.*;
import java.util.*;

import static org.gridgain.grid.cache.GridCacheAtomicityMode.*;
import static org.gridgain.grid.cache.GridCacheMemoryMode.*;
import static org.gridgain.grid.cache.GridCacheMode.*;
import static org.gridgain.grid.cache.GridCacheTxConcurrency.*;
import static org.gridgain.grid.cache.GridCacheTxIsolation.*;
import static org.gridgain.grid.cache.GridCacheWriteSynchronizationMode.*;

/**
 */
public class GridCacheAffinityTransactionsOffHeapTest {
    /** */
    private static final GridTcpDiscoveryIpFinder IP_FINDER = new GridTcpDiscoveryVmIpFinder(true);

    /** */
    private static final int NODE_CNT = 4;

    /** */
    private static final int THREAD_CNT = 1;

    /** */
    private static final int KEY_CNT = 10;

    /**
     * @param args Command line arguments.
     * @throws Exception In case of error.
     */
    public static void main(String[] args) throws Exception {
        startNodes();

        for (int i = 0; i < KEY_CNT; i++) {
            GridCache<Object, Integer> c = cache(i);

            c.putx((long)i, 0);
            c.putx(new UserKey(i, 0), 0);
            c.putx(new UserKey(i, 1), 0);
            c.putx(new UserKey(i, 2), 0);
        }

        assert cache(5).get(5L) != null;

        long key = 5;

        GridCache<Object, Integer> c = cache(key);

        try (GridCacheTx tx = c.txStartAffinity(key, PESSIMISTIC, REPEATABLE_READ, 0, 0)) {
            Integer val = c.get(key);
            Integer userVal1 = c.get(new UserKey(key, 0));
            Integer userVal2 = c.get(new UserKey(key, 1));
            Integer userVal3 = c.get(new UserKey(key, 2));

            assert val != null;
            assert userVal1 != null;
            assert userVal2 != null;
            assert userVal3 != null;

            assert userVal1.equals(val);
            assert userVal2.equals(val);
            assert userVal3.equals(val);

            int newVal = val + 1;

            c.putx(key, newVal);
            c.putx(new UserKey(key, 0), newVal);
            c.putx(new UserKey(key, 1), newVal);
            c.putx(new UserKey(key, 2), newVal);

            tx.commit();
        }

//        final AtomicLong txCnt = new AtomicLong();
//
//        GridTestUtils.runMultiThreaded(
//            new Callable<Object>() {
//                @Override public Object call() throws Exception {
//                    Random rnd = new Random();
//
//                    while (!Thread.currentThread().isInterrupted()) {
//                        long key = rnd.nextInt(KEY_CNT);
//
//                        GridCache<Object, Integer> c = cache(key);
//
//                        try (GridCacheTx tx = c.txStartAffinity(key, PESSIMISTIC, REPEATABLE_READ, 0, 0)) {
//                            Integer val = c.get(key);
//                            Integer userVal1 = c.get(new UserKey(key, 0));
//                            Integer userVal2 = c.get(new UserKey(key, 1));
//                            Integer userVal3 = c.get(new UserKey(key, 2));
//
//                            assert val != null;
//                            assert userVal1 != null;
//                            assert userVal2 != null;
//                            assert userVal3 != null;
//
//                            assert userVal1.equals(val);
//                            assert userVal2.equals(val);
//                            assert userVal3.equals(val);
//
//                            int newVal = val + 1;
//
//                            c.putx(key, newVal);
//                            c.putx(new UserKey(key, 0), newVal);
//                            c.putx(new UserKey(key, 1), newVal);
//                            c.putx(new UserKey(key, 2), newVal);
//
//                            tx.commit();
//                        }
//
//                        long txDone = txCnt.incrementAndGet();
//
//                        if (txDone % 1000 == 0)
//                            System.out.println("Transactions done: " + txDone);
//                    }
//
//                    return null;
//                }
//            },
//            THREAD_CNT,
//            "test-thread"
//        );
    }

    /**
     * @param key Key.
     * @return Cache.
     */
    private static GridCache<Object, Integer> cache(long key) {
        UUID id = GridGain.grid("grid-0").cache(null).affinity().mapKeyToNode(key).id();

        return GridGain.grid(id).cache(null);
    }

    /**
     * @throws GridException In case of error.
     */
    private static void startNodes() throws GridException {
        for (int i = 0; i < NODE_CNT; i++)
            GridGain.start(getConfiguration("grid-" + i));
    }

    /**
     * @param name Grid name.
     * @return Configuration.
     */
    private static GridConfiguration getConfiguration(String name) {
        GridConfiguration cfg = new GridConfiguration();

        cfg.setGridName(name);

        GridCacheConfiguration cacheCfg = new GridCacheConfiguration();

        cacheCfg.setCacheMode(PARTITIONED);
        cacheCfg.setAtomicityMode(TRANSACTIONAL);
        cacheCfg.setWriteSynchronizationMode(FULL_SYNC);
        cacheCfg.setMemoryMode(OFFHEAP_TIERED);
        cacheCfg.setOffHeapMaxMemory(0);
        cacheCfg.setBackups(1);

        cfg.setCacheConfiguration(cacheCfg);

        GridTcpDiscoverySpi disco = new GridTcpDiscoverySpi();

        disco.setIpFinder(IP_FINDER);

        cfg.setDiscoverySpi(disco);

        return cfg;
    }

    /**
     */
    private static class UserKey implements Externalizable {
        /** */
        @GridCacheAffinityKeyMapped
        private long affKey;

        /** */
        private int idx;

        /**
         */
        public UserKey() {
            // No-op.
        }

        /**
         * @param affKey Affinity key.
         * @param idx Index.
         */
        private UserKey(long affKey, int idx) {
            this.affKey = affKey;
            this.idx = idx;
        }

        /** {@inheritDoc} */
        @Override public void writeExternal(ObjectOutput out) throws IOException {
            out.writeLong(affKey);
            out.writeInt(idx);
        }

        /** {@inheritDoc} */
        @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            affKey = in.readLong();
            idx = in.readInt();
        }

        /** {@inheritDoc} */
        @Override public boolean equals(Object o) {
            if (this == o)
                return true;

            if (o == null || getClass() != o.getClass())
                return false;

            UserKey key = (UserKey)o;

            return affKey == key.affKey && idx == key.idx;
        }

        /** {@inheritDoc} */
        @Override public int hashCode() {
            int result = (int)(affKey ^ (affKey >>> 32));

            result = 31 * result + idx;

            return result;
        }
    }
}
