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

package org.gridgain.grid.kernal.processors.cache;

import org.gridgain.grid.cache.*;
import org.gridgain.grid.events.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.testframework.*;

import java.util.concurrent.atomic.*;

import static org.gridgain.grid.cache.GridCacheDistributionMode.*;
import static org.gridgain.grid.cache.GridCacheTxConcurrency.*;
import static org.gridgain.grid.cache.GridCacheTxIsolation.*;
import static org.gridgain.grid.events.GridEventType.*;

/**
 * Abstract test for private cache interface.
 */
public abstract class GridCacheExAbstractFullApiSelfTest extends GridCacheAbstractSelfTest {
    /** {@inheritDoc} */
    @Override protected int gridCount() {
        return 4;
    }

    /** {@inheritDoc} */
    @Override protected GridCacheDistributionMode distributionMode() {
        return PARTITIONED_ONLY;
    }

    /**
     * @throws Exception If failed.
     */
    public void testGetOutTx() throws Exception {
        final AtomicInteger lockEvtCnt = new AtomicInteger();

        GridPredicate<GridEvent> lsnr = new GridPredicate<GridEvent>() {
            @Override public boolean apply(GridEvent evt) {
                lockEvtCnt.incrementAndGet();

                return true;
            }
        };

        try {
            grid(0).events().localListen(lsnr, EVT_CACHE_OBJECT_LOCKED, EVT_CACHE_OBJECT_UNLOCKED);

            GridCache<String, Integer> cache = cache();

            try (GridCacheTx tx = cache.txStart(PESSIMISTIC, REPEATABLE_READ)) {
                int key = 0;

                for (int i = 0; i < 1000; i++) {
                    if (cache.affinity().mapKeyToNode("key" + i).id().equals(grid(0).localNode().id())) {
                        key = i;

                        break;
                    }
                }

                cache.get("key" + key);

                for (int i = key + 1; i < 1000; i++) {
                    if (cache.affinity().mapKeyToNode("key" + i).id().equals(grid(0).localNode().id())) {
                        key = i;

                        break;
                    }
                }

                ((GridCacheProjectionEx<String, Integer>)cache).getAllOutTx(F.asList("key" + key));
            }

            assertTrue(GridTestUtils.waitForCondition(new PA() {
                @Override public boolean apply() {
                    info("Lock event count: " + lockEvtCnt.get());

                    return lockEvtCnt.get() == (nearEnabled() ? 4 : 2);
                }
            }, 15000));
        }
        finally {
            grid(0).events().stopLocalListen(lsnr, EVT_CACHE_OBJECT_LOCKED, EVT_CACHE_OBJECT_UNLOCKED);
        }
    }
}
