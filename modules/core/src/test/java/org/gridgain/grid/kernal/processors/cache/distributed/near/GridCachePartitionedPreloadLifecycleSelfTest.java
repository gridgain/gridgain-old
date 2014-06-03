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

package org.gridgain.grid.kernal.processors.cache.distributed.near;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.cache.affinity.consistenthash.*;
import org.gridgain.grid.cache.query.*;
import org.gridgain.grid.kernal.processors.cache.distributed.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.util.typedef.*;

import java.util.*;

import static org.gridgain.grid.cache.GridCacheMode.*;
import static org.gridgain.grid.cache.GridCachePreloadMode.*;
import static org.gridgain.grid.cache.GridCacheWriteSynchronizationMode.*;

/**
 * Tests for replicated cache preloader.
 */
@SuppressWarnings({"PublicInnerClass"})
public class GridCachePartitionedPreloadLifecycleSelfTest extends GridCachePreloadLifecycleAbstractTest {
    /** Grid count. */
    private int gridCnt = 5;

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration c = super.getConfiguration(gridName);

        GridCacheConfiguration cc1 = defaultCacheConfiguration();

        cc1.setName("one");
        cc1.setCacheMode(PARTITIONED);
        cc1.setBackups(1);
        cc1.setWriteSynchronizationMode(FULL_SYNC);
        cc1.setPreloadMode(preloadMode);
        cc1.setEvictionPolicy(null);
        cc1.setDefaultTxConcurrency(GridCacheTxConcurrency.OPTIMISTIC);
        cc1.setDefaultTxIsolation(GridCacheTxIsolation.READ_COMMITTED);
        cc1.setSwapEnabled(false);
        cc1.setStore(null);
        cc1.setEvictionPolicy(null);
        cc1.setNearEvictionPolicy(null);

        // Identical configuration.
        GridCacheConfiguration cc2 = new GridCacheConfiguration(cc1);

        cc2.setName("two");

        c.setCacheConfiguration(cc1, cc2);

        return c;
    }

    /**
     * @param keys Keys.
     * @return Lifecycle bean.
     */
    private GridLifecycleBean lifecycleBean(final Object[] keys) {
        return new GridLifecycleBean() {
            @GridInstanceResource
            private Grid grid;

            @Override public void onLifecycleEvent(GridLifecycleEventType evt) throws GridException {
                switch (evt) {
                    case AFTER_GRID_START: {
                        GridCache<Object, MyValue> c1 = grid.cache("one");
                        GridCache<Object, MyValue> c2 = grid.cache("two");

                        if (!grid.name().contains("Test0")) {
                            info("Keys already in cache:");

                            for (Object k : c1.keySet())
                                info("Cache1: " + k.toString());

                            for (Object k : c2.keySet())
                                info("Cache2: " + k.toString());

                            return;
                        }

                        info("Populating cache data...");

                        int i = 0;

                        for (Object key : keys) {
                            c1.put(key, new MyValue(value(key)));

                            if (i++ % 2 == 0)
                                c2.put(key, new MyValue(value(key)));
                        }

                        assert c1.size() == keys.length : "Invalid cache1 size [size=" + c1.size() +
                            ", entries=" + c1.entrySet() + ']';
                        assert c2.size() == keys.length / 2 : "Invalid cache2 size [size=" + c2.size() +
                            ", entries=" + c2.entrySet() + ']';

                        break;
                    }

                    case BEFORE_GRID_START:
                    case BEFORE_GRID_STOP:
                    case AFTER_GRID_STOP: {
                        info("Lifecycle event: " + evt);

                        break;
                    }
                }
            }
        };
    }

    /**
     * @param keys Keys.
     * @throws Exception If failed.
     */
    public void checkCache(Object[] keys) throws Exception {
        preloadMode = SYNC;

        lifecycleBean = lifecycleBean(keys);

        for (int i = 0; i < gridCnt; i++) {
            startGrid(i);

            info("Checking '" + (i + 1) + "' nodes...");

            for (int j = 0; j < G.allGrids().size(); j++) {
                GridCache<Object, MyValue> c1 = grid(j).cache("one");
                GridCache<Object, MyValue> c2 = grid(j).cache("two");

                int k = 0;

                for (Object key : keys) {
                    assertNotNull(c1.get(key));

                    if (k++ % 2 == 0)
                        assertNotNull("Value is null for key: " + key, c2.get(key));
                }
            }
        }
    }

    /**
     * @param keys Keys.
     * @throws Exception If failed.
     */
    public void checkScanQuery(Object[] keys) throws Exception {
        preloadMode = SYNC;

        lifecycleBean = lifecycleBean(keys);

        for (int i = 0; i < gridCnt; i++) {
            startGrid(i);

            info("Checking '" + (i + 1) + "' nodes...");

            for (int j = 0; j < G.allGrids().size(); j++) {
                GridCache<Object, MyValue> c2 = grid(j).cache("two");

                GridCacheQuery<Map.Entry<Object, MyValue>> qry = c2.queries().createScanQuery(null);

                int totalCnt = F.sumInt(qry.execute(new GridReducer<Map.Entry<Object, MyValue>, Integer>() {
                    @GridInstanceResource
                    private Grid grid;

                    private int cnt;

                    @Override public boolean collect(Map.Entry<Object, MyValue> e) {
                        Object key = e.getKey();

                        assertNotNull(e.getValue());

                        try {
                            Object v1 = e.getValue();
                            Object v2 = grid.cache("one").get(key);

                            assertNotNull(v2);
                            assertEquals(v1, v2);
                        }
                        catch (GridException e1) {
                            e1.printStackTrace();

                            assert false;
                        }

                        cnt++;

                        return true;
                    }

                    @Override public Integer reduce() {
                        return cnt;
                    }
                }).get());

                info("Total entry count [grid=" + j + ", totalCnt=" + totalCnt + ']');

                assertEquals(keys.length / 2, totalCnt);
            }
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testLifecycleBean1() throws Exception {
        checkCache(keys(true, DFLT_KEYS.length, DFLT_KEYS));
    }

    /**
     * @throws Exception If failed.
     */
    public void testLifecycleBean2() throws Exception {
        checkCache(keys(false, DFLT_KEYS.length, DFLT_KEYS));
    }

    /**
     * @throws Exception If failed.
     */
    public void testLifecycleBean3() throws Exception {
        checkCache(keys(true, 500));
    }

    /**
     * @throws Exception If failed.
     */
    public void testLifecycleBean4() throws Exception {
        checkCache(keys(false, 500));
    }

    /**
     * @throws Exception If failed.
     */
    public void testScanQuery1() throws Exception {
        checkScanQuery(keys(true, DFLT_KEYS.length, DFLT_KEYS));
    }

    /**
     * @throws Exception If failed.
     */
    public void testScanQuery2() throws Exception {
        checkScanQuery(keys(false, DFLT_KEYS.length, DFLT_KEYS));
    }

    /**
     * @throws Exception If failed.
     */
    public void testScanQuery3() throws Exception {
        checkScanQuery(keys(true, 500));
    }

    /**
     * @throws Exception If failed.
     */
    public void testScanQuery4() throws Exception {
        checkScanQuery(keys(false, 500));
    }
}
