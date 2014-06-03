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

package org.gridgain.grid.spi.eventstorage.memory;

import org.gridgain.grid.events.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.testframework.*;
import org.gridgain.testframework.junits.spi.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Memory event storage load test.
 */
@GridSpiTest(spi = GridMemoryEventStorageSpi.class, group = "EventStorage SPI")
public class GridMemoryEventStorageMultiThreadedSelfTest extends GridSpiAbstractTest<GridMemoryEventStorageSpi> {
    /**
     * @throws Exception If test failed
     */
    public void testMultiThreaded() throws Exception {
        GridTestUtils.runMultiThreaded(new Callable<Object>() {
            @Override public Object call() throws Exception {
                for (int i = 0; i < 100000; i++)
                    getSpi().record(new GridDiscoveryEvent(null, "Test event", 1, null));

                return null;
            }
        }, 10, "event-thread");

        Collection<GridEvent> evts = getSpi().localEvents(F.<GridEvent>alwaysTrue());

        info("Events count in memory: " + evts.size());

        assert evts.size() <= 10000 : "Incorrect number of events: " + evts.size();
    }
}
