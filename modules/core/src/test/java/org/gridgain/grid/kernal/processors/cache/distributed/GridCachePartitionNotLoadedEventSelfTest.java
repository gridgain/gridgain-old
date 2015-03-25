/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gridgain.grid.kernal.processors.cache.distributed;

import org.eclipse.jetty.util.*;
import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.cache.affinity.*;
import org.gridgain.grid.events.*;
import org.gridgain.grid.lang.*;
import org.gridgain.testframework.junits.common.*;

import java.util.*;

/**
 *
 */
public class GridCachePartitionNotLoadedEventSelfTest extends GridCommonAbstractTest {
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

        GridCacheConfiguration cacheCfg = new GridCacheConfiguration();

        cacheCfg.setCacheMode(GridCacheMode.PARTITIONED);
        cacheCfg.setBackups(backupCnt);

        cfg.setCacheConfiguration(cacheCfg);

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

        startGrid(0);
        startGrid(1);
        startGrid(2);

        PartitionNotFullyLoadedListener lsnr = new PartitionNotFullyLoadedListener();

        grid(2).events().localListen(lsnr, GridEventType.EVT_CACHE_PRELOAD_PART_DATA_LOST);

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

        assert !lsnr.lostParts.isEmpty();
    }

    /**
     *
     */
    public void testPrimaryDead() throws Exception {
        backupCnt = 0;

        startGrid(0);
        startGrid(1);

        PartitionNotFullyLoadedListener lsnr = new PartitionNotFullyLoadedListener();

        grid(1).events().localListen(lsnr, GridEventType.EVT_CACHE_PRELOAD_PART_DATA_LOST);

        GridCache<Integer, Integer> cache0 = cache(0);

        int key = primaryKey(cache0);

        cache(1).put(key, key);

        assert cache0.containsKey(key);

        stopGrid(0, true);

        awaitPartitionMapExchange();

        assert !cache(1).containsKey(key);

        assert !lsnr.lostParts.isEmpty();
    }

    /**
     *
     */
    private static class PartitionNotFullyLoadedListener implements GridPredicate<GridEvent> {
        /** */
        private Collection<Integer> lostParts = new ConcurrentHashSet<>();

        /** {@inheritDoc} */
        @Override public boolean apply(GridEvent evt) {
            lostParts.add(((GridCachePreloadingEvent)evt).partition());

            return true;
        }
    }
}
