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

package org.gridgain.grid.kernal.processors.cache.distributed.dht;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.cache.affinity.*;
import org.gridgain.grid.kernal.processors.cache.*;
import org.gridgain.testframework.*;

import java.util.*;
import java.util.concurrent.*;

import static org.gridgain.grid.cache.GridCacheMode.*;
import static org.gridgain.grid.cache.GridCacheTxConcurrency.*;
import static org.gridgain.grid.cache.GridCacheTxIsolation.*;

/**
 * Group lock abstract test for partitioned cache.
 */
public abstract class GridCacheGroupLockPartitionedAbstractSelfTest extends GridCacheGroupLockAbstractSelfTest {
    /** {@inheritDoc} */
    @Override protected GridCacheMode cacheMode() {
        return PARTITIONED;
    }

    /**
     * @throws Exception If failed.
     */
    public void testUpdateEntryPessimisticReadCommitted() throws Exception {
        checkUpdateEntry(PESSIMISTIC, READ_COMMITTED);
    }

    /**
     * @throws Exception If failed.
     */
    public void testUpdateEntryPessimisticRepeatableRead() throws Exception {
        checkUpdateEntry(PESSIMISTIC, REPEATABLE_READ);
    }

    /**
     * @throws Exception If failed.
     */
    public void testUpdateEntryOptimisticReadCommitted() throws Exception {
        checkUpdateEntry(OPTIMISTIC, READ_COMMITTED);
    }

    /**
     * @throws Exception If failed.
     */
    public void testUpdateEntryOptimisticRepeatableRead() throws Exception {
        checkUpdateEntry(OPTIMISTIC, REPEATABLE_READ);
    }

    /**
     * @throws Exception If failed.
     */
    private void checkUpdateEntry(GridCacheTxConcurrency concurrency, GridCacheTxIsolation isolation) throws Exception {
        UUID affinityKey = primaryKeyForCache(grid(0));

        GridCache<GridCacheAffinityKey<Integer>, Integer> cache = cache(0);

        assert cache.isEmpty();

        // Put initial values.
        for (int i = 0; i < 10; i++)
            cache.put(new GridCacheAffinityKey<>(i, affinityKey), i);

        for (int i = 0; i < 3; i++) {
            try (GridCacheTx tx = cache.txStartAffinity(affinityKey, concurrency, isolation, 0, 10)) {
                Set<GridCacheEntry<GridCacheAffinityKey<Integer>, Integer>> set =
                    cache.entrySet(cache(0).affinity().partition(affinityKey));

                for (GridCacheEntry<GridCacheAffinityKey<Integer>, Integer> entry : set) {
                    Integer old = entry.get();

                    if (old != null)
                        entry.set(old + 1);
                    else {
                        Object key = entry.getKey();

                        assert key.equals(affinityKey);
                    }
                }

                tx.commit();
            }
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testGroupLockWrongPartition() throws Exception {
        assert cacheMode() == PARTITIONED;

        final UUID affinityKey = primaryKeyForCache(grid(0));

        final GridCache<UUID, String> cache = grid(0).cache(null);

        try (GridCacheTx tx = cache.txStartPartition(cache.affinity().partition(affinityKey), PESSIMISTIC, REPEATABLE_READ,
            0, 2)) {
            GridTestUtils.assertThrows(log, new Callable<Object>() {
                @Override public Object call() throws Exception {
                    UUID key1;

                    do {
                        key1 = UUID.randomUUID();
                    }
                    while (cache.affinity().partition(key1) == cache.affinity().partition(affinityKey));

                    // Key with affinity key different from enlisted on tx start should raise exception.
                    cache.put(key1, "val1");

                    return null;
                }
            }, GridException.class, null);
        }
    }
}
