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

package org.gridgain.grid.kernal.processors.ggfs;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.ggfs.*;
import org.gridgain.grid.kernal.processors.cache.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.grid.util.typedef.*;

import java.lang.reflect.Array;
import java.util.*;

import static org.gridgain.grid.cache.GridCacheAtomicityMode.*;
import static org.gridgain.grid.cache.GridCacheMode.*;
import static org.gridgain.grid.ggfs.GridGgfsMode.*;

/**
 * Tests for node validation logic in {@link GridGgfsProcessor}.
 * <p>
 * Tests starting with "testLocal" are checking
 * {@link GridGgfsProcessor#validateLocalGgfsConfigurations(GridGgfsConfiguration[])}.
 * <p>
 * Tests starting with "testRemote" are checking {@link GridGgfsProcessor#checkGgfsOnRemoteNode(GridNode)}.
 */
public class GridGgfsProcessorValidationSelfTest extends GridGgfsCommonAbstractTest {
    /** IP finder. */
    private static final GridTcpDiscoveryIpFinder IP_FINDER = new GridTcpDiscoveryVmIpFinder(true);

    /** Grid #1 config. */
    private GridConfiguration g1Cfg;

    /** Data cache 1 name. */
    private static final String dataCache1Name = "dataCache1";

    /** Data cache 2 name. */
    private static final String dataCache2Name = "dataCache2";

    /** Meta cache 1 name. */
    private static final String metaCache1Name = "metaCache1";

    /** Meta cache 2 name. */
    private static final String metaCache2Name = "metaCache2";

    /** First GGFS config in grid #1. */
    private GridGgfsConfiguration g1GgfsCfg1 = new GridGgfsConfiguration();

    /** Second GGFS config in grid#1. */
    private GridGgfsConfiguration g1GgfsCfg2 = new GridGgfsConfiguration();

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        g1Cfg = getConfiguration("g1");
    }

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        GridTcpDiscoverySpi discoSpi = new GridTcpDiscoverySpi();

        discoSpi.setIpFinder(IP_FINDER);

        cfg.setDiscoverySpi(discoSpi);

        g1GgfsCfg1.setName("g1GgfsCfg1");
        g1GgfsCfg1.setDataCacheName(dataCache1Name);
        g1GgfsCfg1.setMetaCacheName(metaCache1Name);

        g1GgfsCfg2.setName("g1GgfsCfg2");
        g1GgfsCfg2.setDataCacheName(dataCache2Name);
        g1GgfsCfg2.setMetaCacheName(metaCache2Name);

        cfg.setGgfsConfiguration(g1GgfsCfg1, g1GgfsCfg2);

        cfg.setLocalHost("127.0.0.1");

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        stopAllGrids();
    }

    /**
     * Returns a new array that contains the concatenated contents of two arrays.
     *
     * @param first the first array of elements to concatenate.
     * @param second the second array of elements to concatenate.
     * @param cls
     * @return Concatenated array.
     */
    private <T> T[] concat(T[] first, T[] second, Class<?> cls) {
        Collection<T> res = new ArrayList<>();

        res.addAll(Arrays.asList(first));
        res.addAll(Arrays.asList(second));

        return res.toArray((T[]) Array.newInstance(cls, res.size()));
    }


    /**
     * @throws Exception If failed.
     */
    public void testLocalIfNoCacheIsConfigured() throws Exception {
        checkGridStartFails(g1Cfg, "Data cache is not configured locally for GGFS", true);
    }

    /**
     * @throws Exception If failed.
     */
    public void testLocalIfNoDataCacheIsConfigured() throws Exception {
        GridCacheConfiguration cc = defaultCacheConfiguration();

        cc.setQueryIndexEnabled(false);
        cc.setName("someName");

        g1Cfg.setCacheConfiguration(cc);

        checkGridStartFails(g1Cfg, "Data cache is not configured locally for GGFS", true);
    }

    /**
     * @throws Exception If failed.
     */
    public void testLocalIfNoMetadataCacheIsConfigured() throws Exception {
        GridCacheConfiguration cc = defaultCacheConfiguration();

        cc.setQueryIndexEnabled(false);
        cc.setName(dataCache1Name);

        g1Cfg.setCacheConfiguration(cc);

        checkGridStartFails(g1Cfg, "Metadata cache is not configured locally for GGFS", true);
    }

    /**
     * @throws Exception If failed.
     */
    public void testLocalIfAffinityMapperIsWrongClass() throws Exception {
        g1Cfg.setCacheConfiguration(concat(dataCaches(1024), metaCaches(), GridCacheConfiguration.class));

        for (GridCacheConfiguration cc : g1Cfg.getCacheConfiguration())
            cc.setAffinityMapper(new GridCacheDefaultAffinityKeyMapper());

        checkGridStartFails(g1Cfg, "Invalid GGFS data cache configuration (key affinity mapper class should be", true);
    }

    /**
     * @throws Exception If failed.
     */
    public void testLocalIfGgfsConfigsHaveDifferentNames() throws Exception {
        g1Cfg.setCacheConfiguration(concat(dataCaches(1024), metaCaches(), GridCacheConfiguration.class));

        String ggfsCfgName = "ggfs-cfg";

        g1GgfsCfg1.setName(ggfsCfgName);
        g1GgfsCfg2.setName(ggfsCfgName);

        checkGridStartFails(g1Cfg, "Duplicate GGFS name found (check configuration and assign unique name", true);
    }

    /**
     * @throws Exception If failed.
     */
    public void testLocalIfQueryIndexingEnabledForDataCache() throws Exception {
        GridCacheConfiguration[] dataCaches = dataCaches(1024);

        dataCaches[0].setQueryIndexEnabled(true);

        g1Cfg.setCacheConfiguration(concat(dataCaches, metaCaches(), GridCacheConfiguration.class));

        checkGridStartFails(g1Cfg, "GGFS data cache cannot start with enabled query indexing", true);
    }

    /**
     * @throws Exception If failed.
     */
    public void testLocalIfQueryIndexingEnabledForMetaCache() throws Exception {
        GridCacheConfiguration[] metaCaches = metaCaches();

        metaCaches[0].setQueryIndexEnabled(true);

        g1Cfg.setCacheConfiguration(concat(dataCaches(1024), metaCaches, GridCacheConfiguration.class));

        checkGridStartFails(g1Cfg, "GGFS metadata cache cannot start with enabled query indexing", true);
    }

    /**
     * @throws Exception If failed.
     */
    @SuppressWarnings("NullableProblems")
    public void testLocalNullGgfsNameIsSupported() throws Exception {
        g1Cfg.setCacheConfiguration(concat(dataCaches(1024), metaCaches(), GridCacheConfiguration.class));

        g1GgfsCfg1.setName(null);

        assertFalse(G.start(g1Cfg).nodes().isEmpty());
    }

    /**
     * @throws Exception If failed.
     */
    public void testLocalIfOffheapIsDisabledAndMaxSpaceSizeIsGreater() throws Exception {
        g1Cfg.setCacheConfiguration(concat(dataCaches(1024), metaCaches(), GridCacheConfiguration.class));

        g1GgfsCfg2.setMaxSpaceSize(999999999999999999L);

        checkGridStartFails(g1Cfg, "Maximum GGFS space size cannot be greater that size of available heap", true);
    }

    /**
     * @throws Exception If failed.
     */
    public void testLocalIfOffheapIsEnabledAndMaxSpaceSizeIsGreater() throws Exception {
        g1Cfg.setCacheConfiguration(concat(dataCaches(1024), metaCaches(), GridCacheConfiguration.class));

        for (GridCacheConfiguration cc : g1Cfg.getCacheConfiguration())
            cc.setOffHeapMaxMemory(1000000);

        g1GgfsCfg2.setMaxSpaceSize(999999999999999999L);

        checkGridStartFails(g1Cfg,
            "Maximum GGFS space size cannot be greater than size of available heap memory and offheap storage", true);
    }

    /**
     * @throws Exception If failed.
     */
    public void testLocalIfBackupsEnabled() throws Exception {
        g1Cfg.setCacheConfiguration(concat(dataCaches(1024), metaCaches(), GridCacheConfiguration.class));

        for (GridCacheConfiguration cc : g1Cfg.getCacheConfiguration()) {
            cc.setCacheMode(PARTITIONED);
            cc.setBackups(1);
        }

        checkGridStartFails(g1Cfg, "GGFS data cache cannot be used with backups", true);
    }

    /**
     * @throws Exception If failed.
     */
    public void testLocalIfNonPrimaryModeAndHadoopFileSystemUriIsNull() throws Exception {
        g1Cfg.setCacheConfiguration(concat(dataCaches(1024), metaCaches(), GridCacheConfiguration.class));

        g1GgfsCfg2.setDefaultMode(PROXY);

        checkGridStartFails(g1Cfg, "secondaryFileSystem cannot be null when mode is SECONDARY", true);
    }

    /**
     * @throws Exception If failed.
     */
    public void testRemoteIfDataBlockSizeDiffers() throws Exception {
        GridConfiguration g2Cfg = getConfiguration("g2");

        g1Cfg.setCacheConfiguration(concat(dataCaches(1024), metaCaches(), GridCacheConfiguration.class));
        g2Cfg.setCacheConfiguration(concat(dataCaches(1024), metaCaches(), GridCacheConfiguration.class));

        GridGgfsConfiguration g2GgfsCfg1 = new GridGgfsConfiguration(g1GgfsCfg1);

        g2GgfsCfg1.setBlockSize(g2GgfsCfg1.getBlockSize() + 100);

        g2Cfg.setGgfsConfiguration(g2GgfsCfg1, g1GgfsCfg2);

        G.start(g1Cfg);

        checkGridStartFails(g2Cfg, "Data block size should be the same on all nodes in grid for GGFS", false);
    }

    /**
     * @throws Exception If failed.
     */
    public void testRemoteIfAffinityMapperGroupSizeDiffers() throws Exception {
        GridConfiguration g2Cfg = getConfiguration("g2");

        g1Cfg.setCacheConfiguration(concat(dataCaches(1024), metaCaches(), GridCacheConfiguration.class));
        g2Cfg.setCacheConfiguration(concat(dataCaches(4021), metaCaches(), GridCacheConfiguration.class));

        G.start(g1Cfg);

        checkGridStartFails(g2Cfg, "Affinity mapper group size should be the same on all nodes in grid for GGFS",
            false);
    }

    /**
     * @throws Exception If failed.
     */
    public void testRemoteIfMetaCacheNameDiffers() throws Exception {
        GridConfiguration g2Cfg = getConfiguration("g2");

        GridGgfsConfiguration g2GgfsCfg1 = new GridGgfsConfiguration(g1GgfsCfg1);
        GridGgfsConfiguration g2GgfsCfg2 = new GridGgfsConfiguration(g1GgfsCfg2);

        g2GgfsCfg1.setMetaCacheName("g2MetaCache1");
        g2GgfsCfg2.setMetaCacheName("g2MetaCache2");

        g1Cfg.setCacheConfiguration(concat(dataCaches(1024), metaCaches(), GridCacheConfiguration.class));
        g2Cfg.setCacheConfiguration(concat(dataCaches(1024), metaCaches("g2MetaCache1", "g2MetaCache2"),
             GridCacheConfiguration.class));

        g2Cfg.setGgfsConfiguration(g2GgfsCfg1, g2GgfsCfg2);

        G.start(g1Cfg);

        checkGridStartFails(g2Cfg, "Meta cache name should be the same on all nodes in grid for GGFS", false);
    }

    /**
     * @throws Exception If failed.
     */
    public void testRemoteIfMetaCacheNameEquals() throws Exception {
        GridConfiguration g2Cfg = getConfiguration("g2");

        GridGgfsConfiguration g2GgfsCfg1 = new GridGgfsConfiguration(g1GgfsCfg1);
        GridGgfsConfiguration g2GgfsCfg2 = new GridGgfsConfiguration(g1GgfsCfg2);

        g2GgfsCfg1.setName("g2GgfsCfg1");
        g2GgfsCfg2.setName("g2GgfsCfg2");

        g2GgfsCfg1.setDataCacheName("g2DataCache1");
        g2GgfsCfg2.setDataCacheName("g2DataCache2");

        g1Cfg.setCacheConfiguration(concat(dataCaches(1024), metaCaches(), GridCacheConfiguration.class));
        g2Cfg.setCacheConfiguration(concat(dataCaches(1024, "g2DataCache1", "g2DataCache2"), metaCaches(),
             GridCacheConfiguration.class));

        g2Cfg.setGgfsConfiguration(g2GgfsCfg1, g2GgfsCfg2);

        G.start(g1Cfg);

        checkGridStartFails(g2Cfg, "Meta cache names should be different for different GGFS instances", false);
    }

    /**
     * @throws Exception If failed.
     */
    public void testRemoteIfDataCacheNameDiffers() throws Exception {
        GridConfiguration g2Cfg = getConfiguration("g2");

        GridGgfsConfiguration g2GgfsCfg1 = new GridGgfsConfiguration(g1GgfsCfg1);
        GridGgfsConfiguration g2GgfsCfg2 = new GridGgfsConfiguration(g1GgfsCfg2);

        g2GgfsCfg1.setDataCacheName("g2DataCache1");
        g2GgfsCfg2.setDataCacheName("g2DataCache2");

        g1Cfg.setCacheConfiguration(concat(dataCaches(1024), metaCaches(), GridCacheConfiguration.class));
        g2Cfg.setCacheConfiguration(concat(dataCaches(1024, "g2DataCache1", "g2DataCache2"), metaCaches(),
             GridCacheConfiguration.class));

        g2Cfg.setGgfsConfiguration(g2GgfsCfg1, g2GgfsCfg2);

        G.start(g1Cfg);

        checkGridStartFails(g2Cfg, "Data cache name should be the same on all nodes in grid for GGFS", false);
    }

    /**
     * @throws Exception If failed.
     */
    public void testRemoteIfDataCacheNameEquals() throws Exception {
        GridConfiguration g2Cfg = getConfiguration("g2");

        GridGgfsConfiguration g2GgfsCfg1 = new GridGgfsConfiguration(g1GgfsCfg1);
        GridGgfsConfiguration g2GgfsCfg2 = new GridGgfsConfiguration(g1GgfsCfg2);

        g2GgfsCfg1.setName("g2GgfsCfg1");
        g2GgfsCfg2.setName("g2GgfsCfg2");

        g2GgfsCfg1.setMetaCacheName("g2MetaCache1");
        g2GgfsCfg2.setMetaCacheName("g2MetaCache2");

        g1Cfg.setCacheConfiguration(concat(dataCaches(1024), metaCaches(), GridCacheConfiguration.class));
        g2Cfg.setCacheConfiguration(concat(dataCaches(1024), metaCaches("g2MetaCache1", "g2MetaCache2"),
             GridCacheConfiguration.class));

        g2Cfg.setGgfsConfiguration(g2GgfsCfg1, g2GgfsCfg2);

        G.start(g1Cfg);

        checkGridStartFails(g2Cfg, "Data cache names should be different for different GGFS instances", false);
    }

    /**
     * @throws Exception If failed.
     */
    public void testRemoteIfDefaultModeDiffers() throws Exception {
        GridConfiguration g2Cfg = getConfiguration("g2");

        GridGgfsConfiguration g2GgfsCfg1 = new GridGgfsConfiguration(g1GgfsCfg1);
        GridGgfsConfiguration g2GgfsCfg2 = new GridGgfsConfiguration(g1GgfsCfg2);

        g1GgfsCfg1.setDefaultMode(DUAL_ASYNC);
        g1GgfsCfg2.setDefaultMode(DUAL_ASYNC);

        g2GgfsCfg1.setDefaultMode(DUAL_SYNC);
        g2GgfsCfg2.setDefaultMode(DUAL_SYNC);

        g1Cfg.setCacheConfiguration(concat(dataCaches(1024), metaCaches(), GridCacheConfiguration.class));
        g2Cfg.setCacheConfiguration(concat(dataCaches(1024), metaCaches(), GridCacheConfiguration.class));

        g2Cfg.setGgfsConfiguration(g2GgfsCfg1, g2GgfsCfg2);

        G.start(g1Cfg);

        checkGridStartFails(g2Cfg, "Default mode should be the same on all nodes in grid for GGFS", false);
    }

    /**
     * @throws Exception If failed.
     */
    public void testRemoteIfPathModeDiffers() throws Exception {
        GridConfiguration g2Cfg = getConfiguration("g2");

        GridGgfsConfiguration g2GgfsCfg1 = new GridGgfsConfiguration(g1GgfsCfg1);
        GridGgfsConfiguration g2GgfsCfg2 = new GridGgfsConfiguration(g1GgfsCfg2);

        g2GgfsCfg1.setPathModes(Collections.singletonMap("/somePath", DUAL_SYNC));
        g2GgfsCfg2.setPathModes(Collections.singletonMap("/somePath", DUAL_SYNC));

        g1Cfg.setCacheConfiguration(concat(dataCaches(1024), metaCaches(), GridCacheConfiguration.class));
        g2Cfg.setCacheConfiguration(concat(dataCaches(1024), metaCaches(), GridCacheConfiguration.class));

        g2Cfg.setGgfsConfiguration(g2GgfsCfg1, g2GgfsCfg2);

        G.start(g1Cfg);

        checkGridStartFails(g2Cfg, "Path modes should be the same on all nodes in grid for GGFS", false);
    }

    /**
     * Checks that the given grid configuration will lead to {@link GridException} upon grid startup.
     *
     * @param cfg Grid configuration to check.
     * @param excMsgSnippet Root cause (assertion) exception message snippet.
     * @param testLoc {@code True} if checking is done for "testLocal" tests.
     */
    private void checkGridStartFails(GridConfiguration cfg, CharSequence excMsgSnippet, boolean testLoc) {
        assertNotNull(cfg);
        assertNotNull(excMsgSnippet);

        try {
            G.start(cfg);

            fail("No exception has been thrown.");
        }
        catch (GridException e) {
            if (testLoc) {
                if ("Failed to start processor: GridProcessorAdapter []".equals(e.getMessage()) &&
                    e.getCause().getMessage().contains(excMsgSnippet))
                    return; // Expected exception.
            }
            else if (e.getMessage().contains(excMsgSnippet))
                return; // Expected exception.

            error("Caught unexpected exception.", e);

            fail();
        }
    }

    /**
     * @param grpSize Group size to use in {@link GridGgfsGroupDataBlocksKeyMapper}.
     * @param cacheNames 2 Optional caches names.
     * @return 2 preconfigured data caches.
     */
    private GridCacheConfiguration[] dataCaches(int grpSize, String... cacheNames) {
        assertTrue(F.isEmpty(cacheNames) || cacheNames.length == 2);

        if (F.isEmpty(cacheNames))
            cacheNames = new String[] {dataCache1Name, dataCache2Name};

        GridCacheConfiguration[] res = new GridCacheConfiguration[cacheNames.length];

        for (int i = 0; i < cacheNames.length; i++) {
            GridCacheConfiguration dataCache = defaultCacheConfiguration();

            dataCache.setName(cacheNames[i]);
            dataCache.setAffinityMapper(new GridGgfsGroupDataBlocksKeyMapper(grpSize));
            dataCache.setAtomicityMode(TRANSACTIONAL);
            dataCache.setQueryIndexEnabled(false);

            res[i] = dataCache;
        }

        return res;
    }

    /**
     * @param cacheNames 2 Optional caches names.
     * @return 2 preconfigured meta caches.
     */
    private GridCacheConfiguration[] metaCaches(String... cacheNames) {
        assertTrue(F.isEmpty(cacheNames) || cacheNames.length == 2);

        if (F.isEmpty(cacheNames))
            cacheNames = new String[] {metaCache1Name, metaCache2Name};

        GridCacheConfiguration[] res = new GridCacheConfiguration[cacheNames.length];

        for (int i = 0; i < cacheNames.length; i++) {
            GridCacheConfiguration metaCache = defaultCacheConfiguration();

            metaCache.setName(cacheNames[i]);
            metaCache.setAtomicityMode(TRANSACTIONAL);
            metaCache.setQueryIndexEnabled(false);

            res[i] = metaCache;
        }

        return res;
    }
}
