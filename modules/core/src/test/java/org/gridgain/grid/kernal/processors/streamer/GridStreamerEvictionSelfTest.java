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

package org.gridgain.grid.kernal.processors.streamer;

import org.gridgain.grid.*;
import org.gridgain.grid.marshaller.optimized.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.grid.streamer.*;
import org.gridgain.grid.streamer.window.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.testframework.junits.common.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static java.util.concurrent.TimeUnit.*;

/**
 * Tests for streamer eviction logic.
 */
public class GridStreamerEvictionSelfTest extends GridCommonAbstractTest {
    /** IP finder. */
    private static final GridTcpDiscoveryIpFinder IP_FINDER = new GridTcpDiscoveryVmIpFinder(true);

    /** Number of events used in test. */
    private static final int EVENTS_COUNT = 10;

    /** Test stages. */
    private Collection<GridStreamerStage> stages;

    /** Event router. */
    private GridStreamerEventRouter router;

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        cfg.setStreamerConfiguration(streamerConfiguration());

        GridTcpDiscoverySpi discoSpi = new GridTcpDiscoverySpi();

        discoSpi.setIpFinder(IP_FINDER);

        cfg.setDiscoverySpi(discoSpi);

        cfg.setMarshaller(new GridOptimizedMarshaller(false));

        return cfg;
    }

    /**
     * @return Streamer configuration.
     */
    private GridStreamerConfiguration streamerConfiguration() {
        GridStreamerConfiguration cfg = new GridStreamerConfiguration();

        cfg.setRouter(router);

        GridStreamerBoundedTimeWindow window = new GridStreamerBoundedTimeWindow();

        window.setName("window1");
        window.setTimeInterval(60000);

        cfg.setWindows(F.asList((GridStreamerWindow)window));

        cfg.setStages(stages);

        return cfg;
    }

    /**
     * @throws Exception If failed.
     */
    public void testContextNextStage() throws Exception {
        router = new GridTestStreamerEventRouter();

        final CountDownLatch finishLatch = new CountDownLatch(EVENTS_COUNT);
        final AtomicReference<AssertionError> err = new AtomicReference<>();

        SC stage = new SC() {
            @SuppressWarnings("unchecked")
            @Override public Map<String, Collection<?>> applyx(String stageName, GridStreamerContext ctx,
                Collection<Object> evts) throws GridException {
                assert evts.size() == 1;

                if (ctx.nextStageName() == null) {
                    finishLatch.countDown();

                    return null;
                }

                GridStreamerWindow win = ctx.window("window1");

                // Add new events to the window.
                win.enqueueAll(evts);

                try {
                    assertEquals(0, win.evictionQueueSize());
                }
                catch (AssertionError e) {
                    err.compareAndSet(null, e);
                }

                // Evict outdated events from the window.
                Collection evictedEvts = win.pollEvictedAll();

                try {
                    assertEquals(0, evictedEvts.size());
                }
                catch (AssertionError e) {
                    err.compareAndSet(null, e);
                }

                Integer val = (Integer)F.first(evts);

                return (Map)F.asMap(ctx.nextStageName(), F.asList(++val));
            }
        };

        stages = F.asList((GridStreamerStage)new GridTestStage("0", stage), new GridTestStage("1", stage));

        startGrids(2);

        try {
            GridTestStreamerEventRouter router = (GridTestStreamerEventRouter)this.router;

            router.put("0", grid(0).localNode().id());
            router.put("1", grid(1).localNode().id());

            for (int i = 0; i < EVENTS_COUNT; i++)
                grid(0).streamer(null).addEvent(i);

            boolean await = finishLatch.await(5, SECONDS);

            if (err.get() != null)
                throw err.get();

            if (!await)
                fail("Some events didn't finished.");
        }
        finally {
            stopAllGrids(false);
        }
    }
}
