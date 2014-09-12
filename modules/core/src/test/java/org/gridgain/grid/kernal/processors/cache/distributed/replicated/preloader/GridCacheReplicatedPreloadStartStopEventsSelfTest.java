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

package org.gridgain.grid.kernal.processors.cache.distributed.replicated.preloader;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.events.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.testframework.junits.common.*;

import java.util.concurrent.atomic.*;

import static org.gridgain.grid.cache.GridCacheMode.*;
import static org.gridgain.grid.events.GridEventType.*;

/**
 * Tests that preload start/preload stop events are fired only once for replicated cache.
 */
public class GridCacheReplicatedPreloadStartStopEventsSelfTest extends GridCommonAbstractTest {
    /** */
    private GridTcpDiscoveryIpFinder ipFinder = new GridTcpDiscoveryVmIpFinder(true);

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        super.afterTest();

        stopAllGrids();
    }

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        ((GridTcpDiscoverySpi)cfg.getDiscoverySpi()).setIpFinder(ipFinder);

        GridCacheConfiguration ccfg = new GridCacheConfiguration();

        ccfg.setCacheMode(REPLICATED);

        cfg.setCacheConfiguration(ccfg);

        return cfg;
    }

    /**
     * @throws Exception If failed.
     */
    public void testStartStopEvents() throws Exception {
        Grid grid = startGrid(0);

        final AtomicInteger preloadStartCnt = new AtomicInteger();
        final AtomicInteger preloadStopCnt = new AtomicInteger();

        grid.events().localListen(new GridPredicate<GridEvent>() {
            @Override public boolean apply(GridEvent e) {
                if (e.type() == EVT_CACHE_PRELOAD_STARTED)
                    preloadStartCnt.incrementAndGet();
                else if (e.type() == EVT_CACHE_PRELOAD_STOPPED)
                    preloadStopCnt.incrementAndGet();
                else
                    fail("Unexpected event type: " + e.type());

                return true;
            }
        }, EVT_CACHE_PRELOAD_STARTED, EVT_CACHE_PRELOAD_STOPPED);

        startGrid(1);

        startGrid(2);

        startGrid(3);

        assertTrue("Unexpected start count: " + preloadStartCnt.get(), preloadStartCnt.get() <= 1);
        assertTrue("Unexpected stop count: " + preloadStopCnt.get(), preloadStopCnt.get() <= 1);
    }
}
