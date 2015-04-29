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
import org.gridgain.grid.cache.affinity.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.kernal.processors.cache.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.testframework.junits.common.*;

import java.util.*;

import static org.gridgain.grid.cache.GridCacheAtomicityMode.*;

/**
 *
 */
public class GridCachePartitionedLockBackupUpdateSelfTest extends GridCommonAbstractTest {
    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        GridCacheConfiguration ccfg = new GridCacheConfiguration();

        ccfg.setAtomicityMode(TRANSACTIONAL);
        ccfg.setCacheMode(GridCacheMode.PARTITIONED);
        ccfg.setWriteSynchronizationMode(GridCacheWriteSynchronizationMode.FULL_SYNC);
        ccfg.setBackups(1);

        cfg.setCacheConfiguration(ccfg);

        return cfg;
    }

    /**
     * @throws Exception If failed.
     */
    public void testBackupUpdateInImplicitLock() throws Exception {
        startGrids(4);

        try {
            String key = "1";

            assertTrue(grid(0).cache(null).putxIfAbsent(key, 0));

            Grid primary = primary(key);

            info("Update from primary...");

            update(primary, key, 0, 1);

            checkValues(key, 1);

            Grid backup = backup(key);

            info("Update from backup...");

            update(backup, key, 1, 2);

            checkValues(key, 2);

            Grid near = near(key);

            info("Update from near...");

            update(near, key, 2, 3);

            checkValues(key, 3);

            info("Update from primary...");

            update(primary, key, 3, 4);

            checkValues(key, 4);
        }
        finally {
            stopAllGrids();
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testBackupUpdateInImplicitLockMultiKeys() throws Exception {
        startGrids(4);

        try {
            Collection<String> keys = Arrays.asList("1", "2", "3");

            for (String key : keys)
                assertTrue(grid(0).cache(null).putxIfAbsent(key, 0));

            int prev = 0;

            for (int i = 0; i < 4; i++) {
                update(grid(0), keys, prev, prev + 1, false);

                prev++;

                checkValues(keys, prev);
            }

            for (int i = 0; i < 4; i++) {
                update(grid(0), keys, prev, prev + 1, true);

                prev++;

                checkValues(keys, prev);
            }
        }
        finally {
            stopAllGrids();
        }
    }

    /**
     * @param grid Grid to update from.
     * @param key Key to use.
     * @param prevVal Previous expected value.
     * @param val Update value.
     * @throws Exception
     */
    private void update(Grid grid, String key, int prevVal, int val) throws Exception {
        GridCache<Object, Object> cache = grid.cache(null);

        assertTrue(cache.lock(key, 9999));

        try {
            Object oldVal = cache.get(key);

            assertEquals(prevVal, oldVal);

            cache.putx(key, val);
        }
        finally {
            cache.unlock(key);
        }
    }

    /**
     * @param grid Grid to update from.
     * @param keys Keys to use.
     * @param prevVal Previous expected value.
     * @param val Update value.
     * @throws Exception If failed.
     */
    private void update(Grid grid, Collection<String> keys, int prevVal, int val, boolean lockOnlyFirst) throws Exception {
        GridCache<Object, Object> cache = grid.cache(null);

        Collection<String> toLock = lockOnlyFirst ? Collections.singletonList(F.first(keys)) : keys;

        assertTrue(cache.lockAll(toLock, 9999));

        try {
            Map<String, Integer> putMap = new LinkedHashMap<>(keys.size(), 1.f);

            for (String key : keys) {
                assertEquals(prevVal, cache.get(key));

                putMap.put(key, val);
            }

            cache.putAll(putMap);
        }
        finally {
            cache.unlockAll(toLock);
        }
    }

    /**
     * @throws Exception If failed.
     */
    private void checkValues(String key, int expVal) throws Exception {
        for (Grid grid : GridGain.allGrids()) {
            GridCacheAdapter<Object, Object> cache = ((GridKernal)grid).internalCache();

            boolean primary = cache.affinity().isPrimary(grid.localNode(), key);
            boolean backup = cache.affinity().isBackup(grid.localNode(), key);

            GridCacheEntryEx<Object, Object> entry = cache.peekEx(key);

            if (entry != null)
                assertEquals("Invalid value on node [nodeId=" + grid.localNode().id() +
                    ", primary=" + primary + ", backup=" + backup + ']', expVal, entry.rawGetOrUnmarshal(false));
        }
    }

    /**
     * @throws Exception If failed.
     */
    private void checkValues(Iterable<String> keys, int expVal) throws Exception {
        for (String key : keys)
            checkValues(key, expVal);
    }

    /**
     * Gets primary node grid for given key.
     *
     * @param key Key to check.
     * @return Primary node.
     */
    private Grid primary(String key) {
        GridCacheAffinity<Object> aff = grid(0).cache(null).affinity();

        UUID nodeId = aff.mapKeyToNode(key).id();

        return GridGain.grid(nodeId);
    }

    /**
     * Gets backup node grid for given key.
     *
     * @param key Key to check.
     * @return Backup node.
     */
    private Grid backup(String key) {
        GridCacheAffinity<Object> aff = grid(0).cache(null).affinity();

        Iterator<GridNode> it = aff.mapKeyToPrimaryAndBackups(key).iterator();

        it.next();

        return GridGain.grid(it.next().id());
    }

    /**
     * Gets backup node grid for given key.
     *
     * @param key Key to check.
     * @return Backup node.
     */
    private Grid near(String key) {
        GridCacheAffinity<Object> aff = grid(0).cache(null).affinity();

        for (Grid grid : GridGain.allGrids()) {
            if (!aff.isPrimaryOrBackup(grid.localNode(), key))
                return grid;
        }

        throw new IllegalStateException();
    }
}
