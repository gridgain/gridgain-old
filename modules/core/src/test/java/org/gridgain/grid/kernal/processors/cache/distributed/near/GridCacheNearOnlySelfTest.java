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

package org.gridgain.grid.kernal.processors.cache.distributed.near;

import org.gridgain.grid.cache.*;
import org.gridgain.grid.kernal.processors.cache.distributed.*;

import static org.gridgain.grid.cache.GridCacheDistributionMode.*;

/**
 * Near only self test.
 */
public class GridCacheNearOnlySelfTest extends GridCacheClientModesAbstractSelfTest {
    /** {@inheritDoc} */
    @Override protected GridCacheDistributionMode distributionMode() {
        return NEAR_PARTITIONED;
    }

    /** {@inheritDoc} */
    @Override protected boolean clientOnly() {
        return false;
    }

    /**
     * @throws Exception If failed.
     */
    public void testUpdateNearOnlyReader() throws Exception {
        GridCache<Object, Object> dhtCache = dhtCache();

        final int keyCnt = 100;

        for (int i = 0; i < keyCnt; i++)
            dhtCache.put(i, i);

        GridCache<Object, Object> nearOnlyCache = nearOnlyCache();

        for (int i = 0; i < keyCnt; i++) {
            assertNull(nearOnlyCache.peek(i));

            assertEquals(i, nearOnlyCache.get(i));
            assertEquals(i, nearOnlyCache.peek(i));
        }

        for (int i = 0; i < keyCnt; i++)
            dhtCache.put(i, i * i);

        for (int i = 0; i < keyCnt; i++) {
            assertEquals(i * i, nearOnlyCache.peek(i));

            assertEquals(i * i, nearOnlyCache.get(i));
        }
    }
}
