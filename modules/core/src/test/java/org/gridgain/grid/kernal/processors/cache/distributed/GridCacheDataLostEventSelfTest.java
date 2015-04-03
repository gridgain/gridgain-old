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
import org.gridgain.grid.events.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.testframework.junits.common.*;

import java.util.*;

/**
 *
 */
public class GridCacheDataLostEventSelfTest extends GridCommonAbstractTest {
    /** */
    private static final GridTcpDiscoveryIpFinder IP_FINDER = new GridTcpDiscoveryVmIpFinder(true);

    /** */
    private int backupCnt;

    /** */
    private GridCacheDistributionMode distr;

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

        GridCacheConfiguration cacheCfg = new GridCacheConfiguration();

        cacheCfg.setCacheMode(GridCacheMode.PARTITIONED);
        cacheCfg.setBackups(backupCnt);

        cacheCfg.setDistributionMode(distr);

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
     *
     */
    public void testPrimaryAndBackupDead() throws Exception {
        backupCnt = 1;

        // grid0 has partitioned only cache
        distr = GridCacheDistributionMode.PARTITIONED_ONLY;

        startGrid(0);

        PartitionNotFullyLoadedListener lsnr0 = new PartitionNotFullyLoadedListener();
        grid(0).events().localListen(lsnr0, GridEventType.EVT_CACHE_DATA_LOST);

        // grid1 has near_partitioned cache
        distr = GridCacheDistributionMode.NEAR_PARTITIONED;

        startGrid(1);

        PartitionNotFullyLoadedListener lsnr1 = new PartitionNotFullyLoadedListener();
        grid(1).events().localListen(lsnr1, GridEventType.EVT_CACHE_DATA_LOST);

        // grid2 has near_partitioned cache
        distr = GridCacheDistributionMode.CLIENT_ONLY;

        startGrid(2);

        PartitionNotFullyLoadedListener lsnr2 = new PartitionNotFullyLoadedListener();
        grid(2).events().localListen(lsnr2, GridEventType.EVT_CACHE_DATA_LOST);

        // grid3 has near_partitioned cache
        distr = GridCacheDistributionMode.NEAR_ONLY;

        startGrid(3);

        PartitionNotFullyLoadedListener lsnr3 = new PartitionNotFullyLoadedListener();
        grid(3).events().localListen(lsnr3, GridEventType.EVT_CACHE_DATA_LOST);


        assert lsnr0.cacheNames.isEmpty();
        assert lsnr1.cacheNames.isEmpty();
        assert lsnr2.cacheNames.isEmpty();
        assert lsnr3.cacheNames.isEmpty();

        stopGrid(0);

        assert lsnr1.cacheNames.isEmpty();
        assert lsnr2.cacheNames.isEmpty();
        assert lsnr3.cacheNames.isEmpty();

        stopGrid(1);

        assertEquals(Collections.<String>singleton(null), lsnr2.cacheNames);

        assertEquals(Collections.<String>singleton(null), lsnr3.cacheNames);
    }

    /**
     *
     */
    private static class PartitionNotFullyLoadedListener implements GridPredicate<GridEvent> {
        /** */
        private Set<String> cacheNames = Collections.synchronizedSet(new HashSet<String>());

        /** {@inheritDoc} */
        @Override public boolean apply(GridEvent evt) {
            cacheNames.add(((GridCacheEvent)evt).cacheName());

            return true;
        }
    }
}
