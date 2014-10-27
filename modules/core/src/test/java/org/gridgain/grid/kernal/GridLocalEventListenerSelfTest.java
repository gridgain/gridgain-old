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

package org.gridgain.grid.kernal;

import org.gridgain.grid.*;
import org.gridgain.grid.events.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.util.lang.*;
import org.gridgain.testframework.*;
import org.gridgain.testframework.junits.common.*;

import java.util.*;
import java.util.concurrent.*;

/**
 * Test ensuring that event listeners are picked by started node.
 */
public class GridLocalEventListenerSelfTest extends GridCommonAbstractTest {
    /** Whether event fired. */
    private final CountDownLatch fired = new CountDownLatch(1);

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        int idx = getTestGridIndex(gridName);

        if (idx == 0) {
            Map<GridPredicate<? extends GridEvent>, int[]> lsnrs = new HashMap<>();

            lsnrs.put(new GridPredicate<GridEvent>() {
                @Override public boolean apply(GridEvent evt) {
                    fired.countDown();

                    return true;
                }
            }, new int[] { GridEventType.EVT_NODE_JOINED } );

            cfg.setLocalEventListeners(lsnrs);
        }

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        stopAllGrids(true);
    }

    /**
     * Test listeners notification.
     *
     * @throws Exception If failed.
     */
    public void testListener() throws Exception {
        startGrids(2);

        assert fired.await(5000, TimeUnit.MILLISECONDS);
    }
}
