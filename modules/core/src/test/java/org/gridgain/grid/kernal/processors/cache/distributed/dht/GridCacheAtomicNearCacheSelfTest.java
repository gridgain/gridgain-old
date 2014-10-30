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
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.kernal.processors.cache.*;
import org.gridgain.grid.kernal.processors.cache.distributed.near.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.testframework.junits.common.*;
import org.jetbrains.annotations.*;

import java.util.*;

import static org.gridgain.grid.cache.GridCacheAtomicWriteOrderMode.*;
import static org.gridgain.grid.cache.GridCacheAtomicityMode.*;
import static org.gridgain.grid.cache.GridCacheDistributionMode.*;
import static org.gridgain.grid.cache.GridCacheMode.*;
import static org.gridgain.grid.cache.GridCachePreloadMode.*;
import static org.gridgain.grid.cache.GridCacheWriteSynchronizationMode.*;
import static org.gridgain.grid.kernal.GridNodeAttributes.*;

/**
 * Tests near cache with various atomic cache configuration.
 */
public class GridCacheAtomicNearCacheSelfTest extends GridCommonAbstractTest {
    /** */
    private static final int GRID_CNT = 4;

    /** */
    private static final int PRIMARY = 0;

    /** */
    private static final int BACKUP = 1;

    /** */
    private static final int NOT_PRIMARY_AND_BACKUP = 2;

    /** */
    private int backups;

    /** */
    private GridCacheAtomicWriteOrderMode writeOrderMode;

    /** */
    private int lastKey;

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        GridCacheConfiguration ccfg = new GridCacheConfiguration();

        ccfg.setCacheMode(PARTITIONED);
        ccfg.setAtomicityMode(ATOMIC);
        ccfg.setDistributionMode(NEAR_PARTITIONED);
        ccfg.setWriteSynchronizationMode(FULL_SYNC);
        ccfg.setPreloadMode(SYNC);

        assert writeOrderMode != null;

        ccfg.setAtomicWriteOrderMode(writeOrderMode);
        ccfg.setBackups(backups);

        cfg.setCacheConfiguration(ccfg);

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        stopAllGrids();
    }

    /**
     * @throws Exception If failed.
     */
    public void testNoBackupsPrimaryWriteOrder() throws Exception {
        startGrids(0, GridCacheAtomicWriteOrderMode.PRIMARY);

        checkNearCache();
    }

    /**
     * @throws Exception If failed.
     */
    public void testNoBackupsClockWriteOrder() throws Exception {
        startGrids(0, CLOCK);

        checkNearCache();
    }

    /**
     * @throws Exception If failed.
     */
    public void testWithBackupsPrimaryWriteOrder() throws Exception {
        startGrids(2, GridCacheAtomicWriteOrderMode.PRIMARY);

        checkNearCache();
    }


    /**
     * @throws Exception If failed.
     */
    public void testWithBackupsClockWriteOrder() throws Exception {
        startGrids(2, CLOCK);

        checkNearCache();
    }

    /**
     * @throws Exception If failed.
     */
    @SuppressWarnings("ZeroLengthArrayAllocation")
    private void checkNearCache() throws Exception {
        checkPut();

        checkPutAll();

        checkTransform();

        checkTransformAll();

        checkRemove();

        checkReaderEvict();

        checkReaderRemove();

        checkPutRemoveGet();
    }

    /**
     * @throws Exception If failed.
     */
    @SuppressWarnings("ZeroLengthArrayAllocation")
    private void checkPutAll() throws Exception {
        log.info("Check putAll.");

        Grid grid0 = grid(0);

        GridCache<Integer, Integer> cache0 = grid0.cache(null);

        GridCacheAffinity<Integer> aff = cache0.affinity();

        UUID id0 = grid0.localNode().id();

        Map<Integer, Integer> primaryKeys = new HashMap<>();

        for (int i = 0; i < 10; i++)
            primaryKeys.put(key(grid0, PRIMARY), 1);

        log.info("PutAll from primary.");

        cache0.putAll(primaryKeys);

        for (int i = 0; i < GRID_CNT; i++) {
            for (Integer primaryKey : primaryKeys.keySet())
                checkEntry(grid(i), primaryKey, 1, false);
        }

        if (backups > 0) {
            Map<Integer, Integer> backupKeys = new HashMap<>();

            for (int i = 0; i < 10; i++)
                backupKeys.put(key(grid0, BACKUP), 2);

            log.info("PutAll from backup.");

            cache0.putAll(backupKeys);

            for (int i = 0; i < GRID_CNT; i++) {
                for (Integer backupKey : backupKeys.keySet())
                    checkEntry(grid(i), backupKey, 2, false);
            }
        }

        Map<Integer, Integer> nearKeys = new HashMap<>();

        for (int i = 0; i < 30; i++)
            nearKeys.put(key(grid0, NOT_PRIMARY_AND_BACKUP), 3);

        log.info("PutAll from near.");

        cache0.putAll(nearKeys);

        for (int i = 0; i < GRID_CNT; i++) {
            for (Integer nearKey : nearKeys.keySet()) {
                UUID[] expReaders = aff.isPrimary(grid(i).localNode(), nearKey) ? new UUID[]{id0} : new UUID[]{};

                checkEntry(grid(i), nearKey, 3, i == 0, expReaders);
            }
        }

        Map<Integer, Collection<UUID>> readersMap = new HashMap<>();

        for (Integer key : nearKeys.keySet())
            readersMap.put(key, new HashSet<UUID>());

        int val = 4;

        for (int i = 0; i < GRID_CNT; i++) {
            delay();

            GridCache<Integer, Integer> cache = grid(i).cache(null);

            for (Integer key : nearKeys.keySet())
                nearKeys.put(key, val);

            log.info("PutAll [grid=" + grid(i).name() + ", val=" + val + ']');

            cache.putAll(nearKeys);

            for (Integer key : nearKeys.keySet()) {
                if (!aff.isPrimaryOrBackup(grid(i).localNode(), key))
                    readersMap.get(key).add(grid(i).localNode().id());
            }

            for (int j = 0; j < GRID_CNT; j++) {
                for (Integer key : nearKeys.keySet()) {
                    boolean primaryNode = aff.isPrimary(grid(j).localNode(), key);

                    Collection<UUID> readers = readersMap.get(key);

                    UUID[] expReaders = primaryNode ? U.toArray(readers, new UUID[readers.size()]) : new UUID[]{};

                    checkEntry(grid(j), key, val, readers.contains(grid(j).localNode().id()), expReaders);
                }
            }

            val++;
        }
    }

    /**
     * @throws Exception If failed.
     */
    @SuppressWarnings("ZeroLengthArrayAllocation")
    private void checkTransform() throws Exception {
        log.info("Check transform.");

        Grid grid0 = grid(0);

        GridCache<Integer, Integer> cache0 = grid0.cache(null);

        GridCacheAffinity<Integer> aff = cache0.affinity();

        UUID id0 = grid0.localNode().id();

        Integer primaryKey = key(grid0, PRIMARY);

        log.info("Transform from primary.");

        cache0.transform(primaryKey, new TransformClosure(primaryKey));

        for (int i = 0; i < GRID_CNT; i++)
            checkEntry(grid(i), primaryKey, primaryKey, false);

        if (backups > 0) {
            Integer backupKey = key(grid0, BACKUP);

            log.info("Transform from backup.");

            cache0.transform(backupKey, new TransformClosure(backupKey));

            for (int i = 0; i < GRID_CNT; i++)
                checkEntry(grid(i), backupKey, backupKey, false);
        }

        Integer nearKey = key(grid0, NOT_PRIMARY_AND_BACKUP);

        log.info("Transform from near.");

        cache0.transform(nearKey, new TransformClosure(nearKey));

        for (int i = 0; i < GRID_CNT; i++) {
            UUID[] expReaders = aff.isPrimary(grid(i).localNode(), nearKey) ? new UUID[]{id0} : new UUID[]{};

            checkEntry(grid(i), nearKey, nearKey, i == 0, expReaders);
        }

        Collection<UUID> readers = new HashSet<>();

        readers.add(id0);

        int val = nearKey + 1;

        for (int i = 0; i < GRID_CNT; i++) {
            delay();

            GridCache<Integer, Integer> cache = grid(i).cache(null);

            log.info("Transform [grid=" + grid(i).name() + ", val=" + val + ']');

            cache.transform(nearKey, new TransformClosure(val));

            if (!aff.isPrimaryOrBackup(grid(i).localNode(), nearKey))
                readers.add(grid(i).localNode().id());

            for (int j = 0; j < GRID_CNT; j++) {
                boolean primaryNode = aff.isPrimary(grid(j).localNode(), nearKey);

                UUID[] expReaders = primaryNode ? U.toArray(readers, new UUID[readers.size()]) : new UUID[]{};

                checkEntry(grid(j), nearKey, val, readers.contains(grid(j).localNode().id()), expReaders);
            }

            val++;
        }
    }

    /**
     * @throws Exception If failed.
     */
    @SuppressWarnings("ZeroLengthArrayAllocation")
    private void checkTransformAll() throws Exception {
        log.info("Check transformAll.");

        Grid grid0 = grid(0);

        GridCache<Integer, Integer> cache0 = grid0.cache(null);

        GridCacheAffinity<Integer> aff = cache0.affinity();

        UUID id0 = grid0.localNode().id();

        Map<Integer, TransformClosure> primaryKeys = new HashMap<>();

        for (int i = 0; i < 10; i++)
            primaryKeys.put(key(grid0, PRIMARY), new TransformClosure(1));

        log.info("TransformAll from primary.");

        cache0.transformAll(primaryKeys);

        for (int i = 0; i < GRID_CNT; i++) {
            for (Integer primaryKey : primaryKeys.keySet())
                checkEntry(grid(i), primaryKey, 1, false);
        }

        if (backups > 0) {
            Map<Integer, TransformClosure> backupKeys = new HashMap<>();

            for (int i = 0; i < 10; i++)
                backupKeys.put(key(grid0, BACKUP), new TransformClosure(2));

            log.info("TransformAll from backup.");

            cache0.transformAll(backupKeys);

            for (int i = 0; i < GRID_CNT; i++) {
                for (Integer backupKey : backupKeys.keySet())
                    checkEntry(grid(i), backupKey, 2, false);
            }
        }

        Map<Integer, TransformClosure> nearKeys = new HashMap<>();

        for (int i = 0; i < 30; i++)
            nearKeys.put(key(grid0, NOT_PRIMARY_AND_BACKUP), new TransformClosure(3));

        log.info("TransformAll from near.");

        cache0.transformAll(nearKeys);

        for (int i = 0; i < GRID_CNT; i++) {
            for (Integer nearKey : nearKeys.keySet()) {
                UUID[] expReaders = aff.isPrimary(grid(i).localNode(), nearKey) ? new UUID[]{id0} : new UUID[]{};

                checkEntry(grid(i), nearKey, 3, i == 0, expReaders);
            }
        }

        Map<Integer, Collection<UUID>> readersMap = new HashMap<>();

        for (Integer key : nearKeys.keySet())
            readersMap.put(key, new HashSet<UUID>());

        int val = 4;

        for (int i = 0; i < GRID_CNT; i++) {
            delay();

            GridCache<Integer, Integer> cache = grid(i).cache(null);

            for (Integer key : nearKeys.keySet())
                nearKeys.put(key, new TransformClosure(val));

            log.info("TransformAll [grid=" + grid(i).name() + ", val=" + val + ']');

            cache.transformAll(nearKeys);

            for (Integer key : nearKeys.keySet()) {
                if (!aff.isPrimaryOrBackup(grid(i).localNode(), key))
                    readersMap.get(key).add(grid(i).localNode().id());
            }

            for (int j = 0; j < GRID_CNT; j++) {
                for (Integer key : nearKeys.keySet()) {
                    boolean primaryNode = aff.isPrimary(grid(j).localNode(), key);

                    Collection<UUID> readers = readersMap.get(key);

                    UUID[] expReaders = primaryNode ? U.toArray(readers, new UUID[readers.size()]) : new UUID[]{};

                    checkEntry(grid(j), key, val, readers.contains(grid(j).localNode().id()), expReaders);
                }
            }

            val++;
        }
    }

    /**
     * @throws Exception If failed.
     */
    private void checkPutRemoveGet() throws Exception {
        Grid grid0 = grid(0);

        GridCache<Integer, Integer> cache0 = grid0.cache(null);

        Integer key = key(grid0, NOT_PRIMARY_AND_BACKUP);

        cache0.put(key, key);

        for (int i = 0; i < GRID_CNT; i++)
            grid(i).cache(null).get(key);

        cache0.remove(key);

        cache0.put(key, key);

        for (int i = 0; i < GRID_CNT; i++)
            grid(i).cache(null).get(key);

        cache0.remove(key);
    }

    /**
     * @throws Exception If failed.
     */
    private void checkPut() throws Exception {
        checkPut(0);

        checkPut(GRID_CNT - 1);
    }

    /**
     * @param grid Grid Index.
     * @throws Exception If failed.
     */
    @SuppressWarnings("ZeroLengthArrayAllocation")
    private void checkPut(int grid) throws Exception {
        log.info("Check put, grid: " + grid);

        Grid grid0 = grid(grid);

        GridCache<Integer, Integer> cache0 = grid0.cache(null);

        GridCacheAffinity<Integer> aff = cache0.affinity();

        UUID id0 = grid0.localNode().id();

        Integer primaryKey = key(grid0, PRIMARY);

        log.info("Put from primary.");

        cache0.put(primaryKey, primaryKey);

        for (int i = 0; i < GRID_CNT; i++)
            checkEntry(grid(i), primaryKey, primaryKey, false);

        if (backups > 0) {
            Integer backupKey = key(grid0, BACKUP);

            log.info("Put from backup.");

            cache0.put(backupKey, backupKey);

            for (int i = 0; i < GRID_CNT; i++)
                checkEntry(grid(i), backupKey, backupKey, false);
        }

        Integer nearKey = key(grid0, NOT_PRIMARY_AND_BACKUP);

        log.info("Put from near.");

        cache0.put(nearKey, nearKey);

        for (int i = 0; i < GRID_CNT; i++) {
            UUID[] expReaders = aff.isPrimary(grid(i).localNode(), nearKey) ? new UUID[]{id0} : new UUID[]{};

            checkEntry(grid(i), nearKey, nearKey, i == grid, expReaders);
        }

        Collection<UUID> readers = new HashSet<>();

        readers.add(id0);

        int val = nearKey + 1;

        for (int i = 0; i < GRID_CNT; i++) {
            delay();

            GridCache<Integer, Integer> cache = grid(i).cache(null);

            log.info("Put [grid=" + grid(i).name() + ", val=" + val + ']');

            cache.put(nearKey, val);

            if (!aff.isPrimaryOrBackup(grid(i).localNode(), nearKey))
                readers.add(grid(i).localNode().id());

            for (int j = 0; j < GRID_CNT; j++) {
                boolean primaryNode = aff.isPrimary(grid(j).localNode(), nearKey);

                UUID[] expReaders = primaryNode ? U.toArray(readers, new UUID[readers.size()]) : new UUID[]{};

                checkEntry(grid(j), nearKey, val, readers.contains(grid(j).localNode().id()), expReaders);
            }

            val++;
        }
    }

    /**
     * @throws Exception If failed.
     */
    @SuppressWarnings("ZeroLengthArrayAllocation")
    private void checkRemove() throws Exception {
        log.info("Check remove.");

        Grid grid0 = grid(0);

        GridCache<Integer, Integer> cache0 = grid0.cache(null);

        Integer primaryKey = key(grid0, PRIMARY);

        log.info("Put from primary.");

        cache0.put(primaryKey, primaryKey);

        for (int i = 0; i < GRID_CNT; i++)
            checkEntry(grid(i), primaryKey, primaryKey, false);

        log.info("Remove from primary.");

        cache0.remove(primaryKey);

        for (int i = 0; i < GRID_CNT; i++)
            checkEntry(grid(i), primaryKey, null, false);

        if (backups > 0) {
            Integer backupKey = key(grid0, BACKUP);

            log.info("Put from backup.");

            cache0.put(backupKey, backupKey);

            for (int i = 0; i < GRID_CNT; i++)
                checkEntry(grid(i), backupKey, backupKey, false);

            log.info("Remove from backup.");

            cache0.remove(backupKey);

            for (int i = 0; i < GRID_CNT; i++)
                checkEntry(grid(i), backupKey, null, false);
        }
    }

    /**
     * @throws Exception If failed.
     */
    @SuppressWarnings("ZeroLengthArrayAllocation")
    private void checkReaderEvict() throws Exception {
        log.info("Check evict.");

        Grid grid0 = grid(0);

        GridCache<Integer, Integer> cache0 = grid0.cache(null);

        GridCacheAffinity<Integer> aff = cache0.affinity();

        UUID id0 = grid0.localNode().id();

        Integer nearKey = key(grid0, NOT_PRIMARY_AND_BACKUP);

        cache0.put(nearKey, 1); // Put should create near entry on grid0.

        for (int i = 0; i < GRID_CNT; i++) {
            UUID[] expReaders = aff.isPrimary(grid(i).localNode(), nearKey) ? new UUID[]{id0} : new UUID[]{};

            checkEntry(grid(i), nearKey, 1, i == 0, expReaders);
        }

        cache0.evict(nearKey); // Remove near entry on grid0.

        for (int i = 0; i < GRID_CNT; i++) {
            UUID[] expReaders = aff.isPrimary(grid(i).localNode(), nearKey) ? new UUID[]{id0} : new UUID[]{};

            checkEntry(grid(i), nearKey, 1, false, expReaders);
        }

        GridCache<Integer, Integer> primaryCache = G.grid(
            (String)aff.mapKeyToNode(nearKey).attribute(ATTR_GRID_NAME)).cache(null);

        delay();

        primaryCache.put(nearKey, 2); // This put should see that near entry evicted on grid0 and remove reader.

        for (int i = 0; i < GRID_CNT; i++)
            checkEntry(grid(i), nearKey, 2, false);

        assertEquals((Integer)2, cache0.get(nearKey)); // Get should again create near entry on grid0.

        for (int i = 0; i < GRID_CNT; i++) {
            UUID[] expReaders = aff.isPrimary(grid(i).localNode(), nearKey) ? new UUID[]{id0} : new UUID[]{};

            checkEntry(grid(i), nearKey, 2, i == 0, expReaders);
        }
    }

    /**
     * @throws Exception If failed.
     */
    @SuppressWarnings("ZeroLengthArrayAllocation")
    private void checkReaderRemove() throws Exception {
        Grid grid0 = grid(0);

        GridCache<Integer, Integer> cache0 = grid0.cache(null);

        GridCacheAffinity<Integer> aff = cache0.affinity();

        UUID id0 = grid0.localNode().id();

        Integer nearKey = key(grid0, NOT_PRIMARY_AND_BACKUP);

        cache0.put(nearKey, 1); // Put should create near entry on grid0.

        for (int i = 0; i < GRID_CNT; i++) {
            UUID[] expReaders = aff.isPrimary(grid(i).localNode(), nearKey) ? new UUID[]{id0} : new UUID[]{};

            checkEntry(grid(i), nearKey, 1, i == 0, expReaders);
        }

        cache0.remove(nearKey); // Remove from grid0, this should remove readers on primary node.

        for (int i = 0; i < GRID_CNT; i++)
            checkEntry(grid(i), nearKey, null, i == 0);

        Grid primaryNode = G.grid((String)aff.mapKeyToNode(nearKey).attribute(ATTR_GRID_NAME));

        delay();

        GridCache<Integer, Integer> primaryCache = primaryNode.cache(null);

        primaryCache.put(nearKey, 2); // Put from primary, check there are no readers.

        checkEntry(primaryNode, nearKey, 2, false);
    }

    /**
     * @param grid Node.
     * @param key Key.
     * @param val Expected value.
     * @param expectNear If {@code true} then near cache entry is expected.
     * @param expReaders Expected readers.
     * @throws Exception If failed.
     */
    @SuppressWarnings("ConstantConditions")
    private void checkEntry(Grid grid, Integer key, @Nullable Integer val, boolean expectNear, UUID... expReaders)
        throws Exception {
        GridCacheAdapter<Integer, Integer> near = ((GridKernal)grid).internalCache();

        assertTrue(near.isNear());

        GridCacheEntryEx<Integer, Integer> nearEntry = near.peekEx(key);

        if (expectNear) {
            assertNotNull("No near entry for: " + key + ", grid: " + grid.name(), nearEntry);

            assertEquals("Unexpected value for grid: " + grid.name(), val, nearEntry.info().value());
        }
        else
            assertNull("Unexpected near entry: " + nearEntry + ", grid: " + grid.name(), nearEntry);

        GridDhtCacheAdapter<Integer, Integer> dht = ((GridNearCacheAdapter<Integer, Integer>)near).dht();

        GridDhtCacheEntry<Integer, Integer> dhtEntry = (GridDhtCacheEntry<Integer, Integer>)dht.peekEx(key);

        boolean expectDht = near.affinity().isPrimaryOrBackup(grid.localNode(), key);

        if (expectDht) {
            assertNotNull("No dht entry for: " + key + ", grid: " + grid.name(), dhtEntry);

            Collection<UUID> readers = dhtEntry.readers();

            assertEquals(expReaders.length, readers.size());

            for (UUID reader : expReaders)
                assertTrue(readers.contains(reader));

            assertEquals("Unexpected value for grid: " + grid.name(), val, dhtEntry.info().value());
        }
        else
            assertNull("Unexpected dht entry: " + dhtEntry + ", grid: " + grid.name(), dhtEntry);
    }

    /**
     * @param grid Grid.
     * @param mode One of {@link #PRIMARY}, {@link #BACKUP} or {@link #NOT_PRIMARY_AND_BACKUP}.
     * @return Key with properties specified by the given mode.
     */
    private Integer key(Grid grid, int mode) {
        GridCache<Integer, Integer> cache = grid.cache(null);

        GridCacheAffinity<Integer> aff = cache.affinity();

        Integer key = null;

        for (int i = lastKey + 1; i < 1_000_000; i++) {
            boolean pass = false;

            switch(mode) {
                case PRIMARY: pass = aff.isPrimary(grid.localNode(), i); break;

                case BACKUP: pass = aff.isBackup(grid.localNode(), i); break;

                case NOT_PRIMARY_AND_BACKUP: pass = !aff.isPrimaryOrBackup(grid.localNode(), i); break;

                default: fail();
            }

            lastKey = i;

            if (pass) {
                key = i;

                break;
            }
        }

        assertNotNull(key);

        return key;
    }

    /**
     * @throws GridException If failed.
     */
    private void delay() throws GridException {
        if (writeOrderMode == CLOCK)
            U.sleep(100);
    }

    /**
     * @param backups Backups number.
     * @param writeOrderMode Write order mode.
     * @throws Exception If failed.
     */
    private void startGrids(int backups, GridCacheAtomicWriteOrderMode writeOrderMode) throws Exception {
        this.backups = backups;

        this.writeOrderMode = writeOrderMode;

        startGrids(GRID_CNT);

        awaitPartitionMapExchange();

        log.info("Grids: ");

        for (int i = 0; i < GRID_CNT; i++)
            log.info(grid(i).name() + ": " + grid(i).localNode().id());
    }

    /**
     */
    private static class TransformClosure implements GridClosure<Integer, Integer> {
        /** */
        private final Integer newVal;

        /**
         * @param newVal New value.
         */
        private TransformClosure(Integer newVal) {
            this.newVal = newVal;
        }

        /** {@inheritDoc} */
        @Override public Integer apply(Integer val) {
            return newVal;
        }
    }
}
