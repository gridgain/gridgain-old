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

package org.gridgain.grid.kernal.processors.cache;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.testframework.junits.common.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static org.gridgain.grid.cache.GridCacheMode.*;
import static org.gridgain.grid.cache.GridCacheWriteSynchronizationMode.*;

/**
 * Grid cache concurrent hash map self test.
 */
public class GridCacheConcurrentMapSelfTest extends GridCommonAbstractTest {
    /** Ip finder. */
    private static final GridTcpDiscoveryIpFinder ipFinder = new GridTcpDiscoveryVmIpFinder(true);

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        GridCacheConfiguration cc = defaultCacheConfiguration();

        cc.setCacheMode(LOCAL);
        cc.setWriteSynchronizationMode(FULL_SYNC);
        cc.setStartSize(4);

        GridTcpDiscoverySpi disco = new GridTcpDiscoverySpi();

        disco.setIpFinder(ipFinder);

        cfg.setDiscoverySpi(disco);

        cfg.setCacheConfiguration(cc);

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        startGrid();
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        stopAllGrids();
    }

    /**
     * @throws Exception If failed.
     */
    public void testRehash() throws Exception {
        GridCache<Integer, String> c = grid().cache(null);

        int cnt = 100 * 1024;

        for (int i = 0; i < cnt; i++) {
            c.put(i, Integer.toString(i));

            if (i > 0 && i % 50000 == 0)
                info(">>> " + i + " puts completed");
        }

        for (int i = 0; i < cnt; i++)
            assertEquals(Integer.toString(i), c.get(i));

        assertEquals(cnt, c.size());

        int idx = 0;

        for (GridCacheEntry<Integer, String> e : c.entrySet()) {
            assertNotNull(e.peek());

            idx++;
        }

        assertEquals(cnt, idx);
    }

    /**
     * @throws Exception If failed.
     */
    public void testRehashRandom() throws Exception {
        GridCache<Integer, String> c = grid().cache(null);

        int cnt = 100 * 1024;

        Random rnd = new Random();

        Map<Integer, String> puts = new HashMap<>();

        for (int i = 0; i < cnt * 2; i++) {
            int key = rnd.nextInt(cnt);

            c.put(key, Integer.toString(key));

            puts.put(key, Integer.toString(key));

            if (i > 0 && i % 50000 == 0)
                info(">>> " + i + " puts completed");
        }

        for (Integer key : puts.keySet())
            assertEquals(Integer.toString(key), c.get(key));

        assertEquals(puts.size(), c.size());

        int idx = 0;

        for (GridCacheEntry<Integer, String> e : c.entrySet()) {
            assertNotNull(e.peek());

            idx++;
        }

        assertEquals(puts.size(), idx);
    }

    /**
     * @throws Exception If failed.
     */
    public void testRehashMultithreaded1() throws Exception {
        final AtomicInteger tidGen = new AtomicInteger();

        final Random rand = new Random();

        final int cnt = 100 * 1024;

        multithreaded(new Callable<Object>() {
            @SuppressWarnings("UnusedAssignment")
            @Override public Object call() throws Exception {
                GridCache<Integer, String> c = grid().cache(null);

                int tid = tidGen.getAndIncrement();

                int start = 2 * 1024 * tid;

                Iterator<String> it1 = null;
                Iterator<GridCacheEntry<Integer, String>> it2 = null;
                Iterator<Integer> it3 = null;

                boolean created = false;

                for (int i = start; i < start + cnt; i++) {
                    int key = i % cnt;

                    if (!created && i >= start + tid * 100) {
                        if (it1 == null)
                            it1 = c.values().iterator();

                        if (it2 == null)
                            it2 = c.entrySet().iterator();

                        if (it3 == null)
                            it3 = c.keySet().iterator();

                        created = true;
                    }

                    c.put(key, Integer.toString(key));

                    c.get(rand.nextInt(cnt));
                }

                // Go through iterators.
                while(it1.hasNext())
                    it1.next();

                while(it2.hasNext())
                    it2.next();

                while(it3.hasNext())
                    it3.next();

                // Make sure that hard references are gone.
                it1 = null;
                it2 = null;
                it3 = null;

                for (int i = start; i < start + cnt; i++) {
                    int key = i % cnt;

                    assertEquals(Integer.toString(key), c.get(key));
                }

                assertEquals(cnt, c.size());

                int idx = 0;

                for (GridCacheEntry<Integer, String> e : c.entrySet()) {
                    assertNotNull(e.peek());

                    idx++;
                }

                assertEquals(cnt, idx);

                System.gc();

                return null;
            }
        }, 10);

        cache().get(rand.nextInt(cnt));

        System.gc();

        Thread.sleep(1000);

        cache().get(rand.nextInt(cnt));

        assertEquals(0, local().map.iteratorMapSize());
    }

    /**
     * @throws Exception If failed.
     */
    public void testRehashMultithreaded2() throws Exception {
        final AtomicInteger tidGen = new AtomicInteger(0);

        final Random rand = new Random();

        final int cnt = 100 * 1024;

        multithreaded(new Callable<Object>() {
            @SuppressWarnings("UnusedAssignment")
            @Override public Object call() throws Exception {
                GridCache<Integer, String> c = grid().cache(null);

                int tid = tidGen.getAndIncrement();

                int start = 2 * 1024 * tid;

                Iterator<String> it1 = null;
                Iterator<GridCacheEntry<Integer, String>> it2 = null;
                Iterator<Integer> it3 = null;

                boolean forgot = false;

                for (int i = start; i < start + cnt; i++) {
                    int key = i % cnt;

                    if (!forgot && i >= start + tid * 100) {
                        if (it1 == null)
                            it1 = c.values().iterator();

                        if (it2 == null)
                            it2 = c.entrySet().iterator();

                        if (it3 == null)
                            it3 = c.keySet().iterator();
                    }

                    c.put(key, Integer.toString(key));

                    c.get(rand.nextInt(cnt));

                    if (!forgot && i == cnt) {
                        info("Forgetting iterators [it1=" + it1 + ", it2=" + it2 + ", it3=" + it3 + ']');

                        // GC
                        it1 = null;
                        it2 = null;
                        it3 = null;

                        forgot = true;
                    }
                }

                // Make sure that hard references are gone.
                it1 = null;
                it2 = null;
                it3 = null;

                for (int i = start; i < start + cnt; i++) {
                    int key = i % cnt;

                    assertEquals(Integer.toString(key), c.get(key));
                }

                assertEquals(cnt, c.size());

                int idx = 0;

                for (GridCacheEntry<Integer, String> e : c.entrySet()) {
                    assertNotNull(e.peek());

                    idx++;
                }

                assertEquals(cnt, idx);

                System.gc();

                return null;
            }
        }, 10);

        cache().get(rand.nextInt(cnt));

        System.gc();

        Thread.sleep(1000);

        cache().get(rand.nextInt(cnt));

        assertEquals(0, local().map.iteratorMapSize());
    }

    /**
     * @throws Exception If failed.
     */
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testEmptyWeakIterator() throws Exception {
        final GridCache<Integer, String> c = grid().cache(null);

        for (int i = 0; i < 100; i++) {
            multithreaded(new Callable<Object>() {
                @SuppressWarnings("UnusedAssignment")
                @Override public Object call() throws Exception {
                    Iterator<String> it1 = c.values().iterator();
                    Iterator<GridCacheEntry<Integer, String>> it2 = c.entrySet().iterator();
                    Iterator<Integer> it3 = c.keySet().iterator();

                    for (int i = 0; i < 1000; i++) {
                        c.put(i, String.valueOf(i));

                        if (i == 0) {
                            it1.hasNext();
                            it2.hasNext();
                            it3.hasNext();
                        }
                    }

                    // Make sure that hard references are gone.
                    it1 = null;
                    it2 = null;
                    it3 = null;

                    System.gc();

                    return null;
                }
            }, 10);

            for (int r = 0; r < 10; r++) {
                System.gc();

                c.get(100);

                if (local().map.iteratorMapSize() == 0)
                    break;
                else
                    U.sleep(500);
            }

            assertEquals(0, local().map.iteratorMapSize());
        }
    }
}
