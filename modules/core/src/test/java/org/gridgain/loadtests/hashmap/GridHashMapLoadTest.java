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

package org.gridgain.loadtests.hashmap;

import org.gridgain.grid.kernal.processors.cache.*;
import org.gridgain.testframework.junits.common.*;

import java.util.*;
import java.util.concurrent.*;

/**
 * Tests hashmap load.
 */
@SuppressWarnings("InfiniteLoopStatement")
public class GridHashMapLoadTest extends GridCommonAbstractTest {
    public void testHashMapLoad() {
        Map<Integer, Integer> map = new HashMap<>(5 * 1024 * 1024);

        int i = 0;

        while (true) {
            map.put(i++, i++);

            if (i % 400000 == 0)
                info("Inserted objects: " + i / 2);
        }
    }

    public void testConcurrentHashMapLoad() {
        Map<Integer, Integer> map = new ConcurrentHashMap<>(5 * 1024 * 1024);

        int i = 0;

        while (true) {
            map.put(i++, i++);

            if (i % 400000 == 0)
                info("Inserted objects: " + i / 2);
        }
    }

    public void testMapEntry() {
        Map<Integer, GridCacheMapEntry<Integer, Integer>> map =
            new HashMap<>(5 * 1024 * 1024);

        int i = 0;

        GridCacheTestContext<Integer, Integer> ctx = new GridCacheTestContext<>();

        while (true) {
            Integer key = i++;
            Integer val = i++;

            map.put(key, new GridCacheMapEntry<Integer, Integer>(ctx, key,
                key.hashCode(), val, null, 0, 1) {
                @Override public boolean tmLock(GridCacheTxEx<Integer, Integer> tx, long timeout) {
                    return false;
                }

                @Override public void txUnlock(GridCacheTxEx<Integer, Integer> tx) {
                    // No-op.
                }

                @Override public boolean removeLock(GridCacheVersion ver) {
                    return false;
                }
            });

            if (i % 100000 == 0)
                info("Inserted objects: " + i / 2);
        }
    }
}
