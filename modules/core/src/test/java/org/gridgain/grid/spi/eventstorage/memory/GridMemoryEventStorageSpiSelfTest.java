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
import org.gridgain.grid.lang.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.testframework.junits.spi.*;

import java.util.*;

import static org.gridgain.grid.events.GridEventType.*;

/**
 * Tests for {@link GridMemoryEventStorageSpi}.
 */
@GridSpiTest(spi = GridMemoryEventStorageSpi.class, group = "Event Storage SPI")
public class GridMemoryEventStorageSpiSelfTest extends GridSpiAbstractTest<GridMemoryEventStorageSpi> {
    /** */
    private static final int EXPIRE_CNT = 100;

    /**
     * @return Maximum events queue size.
     */
    @GridSpiTestConfig
    public long getExpireCount() {
        return EXPIRE_CNT;
    }

    /**
     * @return Events expiration time.
     */
    @GridSpiTestConfig
    public long getExpireAgeMs() {
        return 1000;
    }

    /**
     * @throws Exception If failed.
     */
    public void testMemoryEventStorage() throws Exception {
        GridMemoryEventStorageSpi spi = getSpi();

        GridPredicate<GridEvent> filter = F.alwaysTrue();

        // Get all events.
        Collection<GridEvent> evts = spi.localEvents(filter);

        // Check.
        assert evts != null : "Events can't be null.";
        assert evts.isEmpty() : "Invalid events count.";

        // Store.
        spi.record(createEvent());

        // Get all events.
        evts = spi.localEvents(filter);

        // Check stored events.
        assert evts != null : "Events can't be null.";
        assert evts.size() == 1 : "Invalid events count.";

        // Sleep a bit more than expire age configuration property.
        Thread.sleep(getExpireAgeMs() * 2);

        // Get all events.
        evts = spi.localEvents(filter);

        // Check expired by age.
        assert evts != null : "Events can't be null.";
        assert evts.isEmpty() : "Invalid events count.";

        // Clear.
        spi.clearAll();

        // Get all events.
        evts = spi.localEvents(filter);

        // Check events cleared.
        assert evts != null : "Events can't be null.";
        assert evts.isEmpty() : "Invalid events count.";
    }

    /**
     * @throws Exception If failed.
     */
    @SuppressWarnings({"NullableProblems"})
    public void testFilter() throws Exception {
        GridMemoryEventStorageSpi spi = getSpi();

        try {
            spi.clearAll();

            spi.setFilter(F.<GridEvent>alwaysFalse());

            // This event should not record.
            spi.record(createEvent());

            spi.setFilter(null);

            spi.record(createEvent());

            // Get all events.
            Collection<GridEvent> evts = spi.localEvents(F.<GridEvent>alwaysTrue());

            assert evts != null : "Events can't be null.";
            assert evts.size() == 1 : "Invalid events count: " + evts.size();
        }
        finally {
            if (spi != null)
                spi.clearAll();
        }
    }

    /**
     * @return Discovery event.
     * @throws Exception If error occurred.
     */
    private GridEvent createEvent() throws Exception {
        return new GridDiscoveryEvent(null, "Test Event", EVT_NODE_METRICS_UPDATED, null);
    }
}

