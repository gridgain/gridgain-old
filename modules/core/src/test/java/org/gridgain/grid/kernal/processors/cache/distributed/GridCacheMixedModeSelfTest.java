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
import org.gridgain.grid.util.typedef.*;
import org.gridgain.testframework.junits.common.*;

/**
 * Tests cache puts in mixed mode.
 */
public class GridCacheMixedModeSelfTest extends GridCommonAbstractTest {
    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        cfg.setCacheConfiguration(cacheConfiguration(gridName));

        return cfg;
    }

    /**
     * @param gridName Grid name.
     * @return Cache configuration.
     */
    private GridCacheConfiguration cacheConfiguration(String gridName) {
        GridCacheConfiguration cfg = new GridCacheConfiguration();

        cfg.setCacheMode(GridCacheMode.PARTITIONED);

        if (F.eq(gridName, getTestGridName(0)))
            cfg.setDistributionMode(GridCacheDistributionMode.NEAR_ONLY);

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        startGrids(4);
    }

    /** {@inheritDoc} */
    @Override protected void afterTestsStopped() throws Exception {
        stopAllGrids();
    }

    /**
     * @throws Exception If failed.
     */
    public void testBasicOps() throws Exception {
        GridCache<Object, Object> cache = grid(0).cache(null);

        for (int i = 0; i < 1000; i++)
            cache.put(i, i);

        for (int i = 0; i < 1000; i++)
            assertEquals(i, cache.get(i));

        for (int i = 0; i < 1000; i++)
            assertEquals(i, cache.remove(i));

        for (int i = 0; i < 1000; i++)
            assertNull(cache.get(i));
    }
}
