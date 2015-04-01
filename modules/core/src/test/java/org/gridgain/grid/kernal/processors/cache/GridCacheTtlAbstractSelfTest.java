/* @java.file.header */

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
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.testframework.junits.common.*;

import java.util.*;

/**
 * TTL test.
 */
public abstract class GridCacheTtlAbstractSelfTest extends GridCommonAbstractTest {
    /** */
    private static final GridTcpDiscoveryIpFinder IP_FINDER = new GridTcpDiscoveryVmIpFinder(true);

    /** */
    private static final int MAX_CACHE_SIZE = 5;

    /** */
    private static final int SIZE = 11;

    /** */
    private static final long DEFAULT_TIME_TO_LIVE = 2000;

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        GridCacheConfiguration cache = new GridCacheConfiguration();

        cache.setCacheMode(cacheMode());
        cache.setAtomicityMode(atomicityMode());
        cache.setDistributionMode(GridCacheDistributionMode.PARTITIONED_ONLY);
        cache.setMemoryMode(memoryMode());
        cache.setOffHeapMaxMemory(0);
        cache.setDefaultTimeToLive(DEFAULT_TIME_TO_LIVE);
        cache.setEvictionPolicy(new GridCacheLruEvictionPolicy(MAX_CACHE_SIZE));

        cfg.setCacheConfiguration(cache);

        GridTcpDiscoverySpi disco = new GridTcpDiscoverySpi();

        disco.setIpFinder(IP_FINDER);

        cfg.setDiscoverySpi(disco);

        return cfg;
    }

    /**
     * @return Atomicity mode.
     */
    protected abstract GridCacheAtomicityMode atomicityMode();

    /**
     * @return Memory mode.
     */
    protected abstract GridCacheMemoryMode memoryMode();

    /**
     * @return Cache mode.
     */
    protected abstract GridCacheMode cacheMode();

    /**
     * @return GridCount
     */
    protected abstract int gridCount();

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        startGrids(gridCount());
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        stopAllGrids();
    }

    /**
     * @throws Exception If failed.
     */
    public void testTtl() throws Exception {
        GridCache<Integer, Integer> cache = cache(0);

        for (int i = 0; i < SIZE; i++) {
            GridCacheEntry<Integer, Integer> e = cache.entry(i);

            e.setx(i);
        }

        checkSizeBeforeLive(cache, SIZE);

        Thread.sleep(DEFAULT_TIME_TO_LIVE + 500);

        checkSizeAfterLive();
    }

    /**
     * @throws Exception If failed.
     */
    public void testDefaultTimeToLivePut() throws Exception {
        GridCache<Integer, Integer> cache = cache(0);

        cache.put(1, 1);

        checkSizeBeforeLive(cache, 1);

        Thread.sleep(DEFAULT_TIME_TO_LIVE + 500);

        checkSizeAfterLive();
    }

    /**
     * @throws Exception If failed.
     */
    public void testDefaultTimeToLivePutAll() throws Exception {
        GridCache<Integer, Integer> cache = cache(0);

        Map<Integer, Integer> entries = new HashMap<>();

        for (int i = 0; i < SIZE; ++i)
            entries.put(i, i);

        cache.putAll(entries);

        checkSizeBeforeLive(cache, SIZE);

        Thread.sleep(DEFAULT_TIME_TO_LIVE + 500);

        checkSizeAfterLive();
    }

    /**
     * @throws Exception If failed.
     */
    public void testTimeToLiveTtl() throws Exception {
        GridCache<Integer, Integer> cache = cache(0);

        long time = DEFAULT_TIME_TO_LIVE + 2000;

        for (int i = 0; i < SIZE; i++) {
            GridCacheEntry<Integer, Integer> e = cache.entry(i);

            e.timeToLive(time);

            e.setx(i);
        }

        checkSizeBeforeLive(cache, SIZE);

        Thread.sleep(DEFAULT_TIME_TO_LIVE + 500);

        checkSizeBeforeLive(cache, SIZE);

        Thread.sleep(time - DEFAULT_TIME_TO_LIVE + 500);

        checkSizeAfterLive();
    }

    /**
     * @throws Exception If failed.
     */
    private void checkSizeBeforeLive(GridCache<Integer, Integer> cache, int size) throws Exception {
        if (memoryMode() == GridCacheMemoryMode.OFFHEAP_TIERED) {
            assertEquals(0, cache.size());
            assertEquals(size, cache.offHeapEntriesCount());
        }
        else {
            assertEquals(size > MAX_CACHE_SIZE ? MAX_CACHE_SIZE : size, cache.size());
            assertEquals(size > MAX_CACHE_SIZE ? size - MAX_CACHE_SIZE : 0, cache.offHeapEntriesCount());
        }
    }

    /**
     * @throws Exception If failed.
     */
    private void checkSizeAfterLive() throws Exception {
        for (int i = 0; i < gridCount(); ++i) {
            GridCache<Integer, Integer> cache = cache(0);

            assertEquals(0, cache.size());
            assertEquals(0, cache.offHeapEntriesCount());
            assertEquals(0, cache.swapSize());
            assertEquals(0, cache.queries().createScanQuery(null).execute().get().size());
        }
    }
}
