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
import org.gridgain.grid.cache.eviction.lru.*;
import org.gridgain.grid.marshaller.optimized.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.grid.spi.swapspace.file.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.testframework.junits.common.*;
import org.junit.*;

import java.util.*;

import static java.lang.String.*;
import static org.gridgain.grid.cache.GridCacheWriteSynchronizationMode.*;

/**
 * Memory model self test.
 */
@SuppressWarnings("deprecation")
public class GridCacheMemoryModeSelfTest extends GridCommonAbstractTest {
    /** */
    private GridTcpDiscoveryIpFinder ipFinder;

    /** */
    private boolean swapEnabled;

    /** */
    private GridCacheMode mode;

    /** */
    private GridCacheMemoryMode memoryMode;

    /** */
    private int maxOnheapSize;

    /** */
    private long offheapSize;

    /** */
    private GridCacheAtomicityMode atomicity;

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        GridTcpDiscoverySpi disco = new GridTcpDiscoverySpi();

        disco.setIpFinder(ipFinder);

        cfg.setDiscoverySpi(disco);

        cfg.setNetworkTimeout(2000);

        cfg.setSwapSpaceSpi(new GridFileSwapSpaceSpi());

        GridCacheConfiguration cacheCfg = defaultCacheConfiguration();

        cacheCfg.setWriteSynchronizationMode(FULL_SYNC);

        cacheCfg.setSwapEnabled(swapEnabled);
        cacheCfg.setCacheMode(mode);
        cacheCfg.setMemoryMode(memoryMode);
        cacheCfg.setEvictionPolicy(maxOnheapSize == Integer.MAX_VALUE ? null :
            new GridCacheLruEvictionPolicy(maxOnheapSize));
        cacheCfg.setAtomicityMode(atomicity);
        cacheCfg.setOffHeapMaxMemory(offheapSize);
        cacheCfg.setQueryIndexEnabled(memoryMode != GridCacheMemoryMode.OFFHEAP_VALUES);
        cacheCfg.setPortableEnabled(portableEnabled());

        cfg.setCacheConfiguration(cacheCfg);
        cfg.setMarshaller(new GridOptimizedMarshaller(false));

        return cfg;
    }

    /**
     * @return Portable enabled flag.
     */
    protected boolean portableEnabled() {
        return false;
    }

    /**
     * @throws Exception If failed.
     */
    public void testOnheap() throws Exception {
        mode = GridCacheMode.LOCAL;
        memoryMode = GridCacheMemoryMode.ONHEAP_TIERED;
        maxOnheapSize = Integer.MAX_VALUE;
        swapEnabled = false;
        atomicity = GridCacheAtomicityMode.ATOMIC;
        offheapSize = -1;

        doTestPutAndPutAll(1000, 0, true, true);
    }

    /**
     * @throws Exception If failed.
     */
    public void testOnheapSwap() throws Exception {
        mode = GridCacheMode.LOCAL;
        memoryMode = GridCacheMemoryMode.ONHEAP_TIERED;
        maxOnheapSize = 330;
        swapEnabled = true;
        atomicity = GridCacheAtomicityMode.ATOMIC;
        offheapSize = -1;

        doTestPutAndPutAll(330, 670, true, false);
    }

    /**
     * @throws Exception If failed.
     */
    public void testOffheap() throws Exception {
        mode = GridCacheMode.LOCAL;
        memoryMode = GridCacheMemoryMode.OFFHEAP_TIERED;
        maxOnheapSize = Integer.MAX_VALUE;
        swapEnabled = false;
        atomicity = GridCacheAtomicityMode.ATOMIC;
        offheapSize = -1; // Must be fixed in config validation.

        doTestPutAndPutAll(0, 1000, false, true);
    }

    /**
     * @throws Exception If failed.
     */
    public void testOffheapSwap() throws Exception {
        mode = GridCacheMode.LOCAL;
        memoryMode = GridCacheMemoryMode.OFFHEAP_TIERED;
        maxOnheapSize = Integer.MAX_VALUE;
        swapEnabled = true;
        atomicity = GridCacheAtomicityMode.ATOMIC;
        offheapSize = 1000; // Small for evictions from offheap to swap.

        doTestPutAndPutAll(0, 1000, false, false);
    }

    /**
     * @throws Exception If failed.
     */
    public void testTiered() throws Exception {
        mode = GridCacheMode.LOCAL;
        memoryMode = GridCacheMemoryMode.ONHEAP_TIERED;
        maxOnheapSize = 24;
        swapEnabled = true;
        atomicity = GridCacheAtomicityMode.ATOMIC;
        offheapSize = 1000; // Small for evictions from offheap to swap.

        doTestPutAndPutAll(24, 976, false, false);
    }

    /**
     * @throws Exception If failed.
     */
    public void testOffheapValuesConfigFixBackward() throws Exception {
        mode = GridCacheMode.LOCAL;
        memoryMode = GridCacheMemoryMode.OFFHEAP_VALUES;
        maxOnheapSize = 24;
        swapEnabled = true;
        atomicity = GridCacheAtomicityMode.ATOMIC;
        offheapSize = -1;

        Grid g = startGrid();

        GridCacheConfiguration cfg = g.cache(null).configuration();

        assertEquals(memoryMode, cfg.getMemoryMode());
        assertEquals(0, cfg.getOffHeapMaxMemory());
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        super.afterTest();

        stopAllGrids();
    }

    /**
     * @param cache In cache.
     * @param offheapSwap In swap and offheap.
     * @param offheapEmpty Offheap is empty.
     * @param swapEmpty Swap is empty.
     * @throws Exception If failed.
     */
    private void doTestPutAndPutAll(int cache, int offheapSwap, boolean offheapEmpty, boolean swapEmpty) throws Exception {
        final int all = cache + offheapSwap;

        // put
        doTest(cache, offheapSwap, offheapEmpty, swapEmpty, new CIX1<GridCache<String, Integer>>() {
            @Override public void applyx(GridCache<String, Integer> c) throws GridException {
                for (int i = 0; i < all; i++)
                    c.put(valueOf(i), i);
            }
        });

        //putAll
        doTest(cache, offheapSwap, offheapEmpty, swapEmpty, new CIX1<GridCache<String, Integer>>() {
            @Override public void applyx(GridCache<String, Integer> c) throws GridException {
                Map<String, Integer> m = new HashMap<>();

                for (int i = 0; i < all; i++)
                    m.put(valueOf(i), i);

                c.putAll(m);
            }
        });
    }

    /**
     * @param cache Cache size.
     * @param offheapSwap Offheap + swap size.
     * @param offheapEmpty Offheap is empty.
     * @param swapEmpty Swap is empty.
     * @param x Cache modifier.
     * @throws GridException If failed.
     */
    void doTest(int cache, int offheapSwap, boolean offheapEmpty, boolean swapEmpty, CIX1<GridCache<String, Integer>> x) throws Exception {
        ipFinder = new GridTcpDiscoveryVmIpFinder(true);

        startGrid();

        final GridCache<String, Integer> c = cache();

        x.applyx(c);

        assertEquals(cache, c.size());
        assertEquals(offheapSwap, c.offHeapEntriesCount() + c.swapKeys());

        if (offheapEmpty)
            Assert.assertEquals(0, c.offHeapEntriesCount());
        else
            Assert.assertNotEquals(0, c.offHeapEntriesCount());

        if (swapEmpty)
            Assert.assertEquals(0, c.swapKeys());
        else
            Assert.assertNotEquals(0, c.swapKeys());

        info("size: " + c.size());
        info("offheap: " + c.offHeapEntriesCount());
        info("swap: " + c.swapKeys());

        stopAllGrids();
    }
}
