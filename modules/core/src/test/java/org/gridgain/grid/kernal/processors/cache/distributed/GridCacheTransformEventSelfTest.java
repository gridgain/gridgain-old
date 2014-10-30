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

import org.eclipse.jetty.util.*;
import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.events.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.testframework.junits.common.*;

import java.io.*;
import java.util.*;

import static org.gridgain.grid.cache.GridCacheAtomicWriteOrderMode.*;
import static org.gridgain.grid.cache.GridCacheAtomicityMode.*;
import static org.gridgain.grid.cache.GridCacheMode.*;
import static org.gridgain.grid.cache.GridCacheTxConcurrency.*;
import static org.gridgain.grid.cache.GridCacheTxIsolation.*;
import static org.gridgain.grid.cache.GridCacheWriteSynchronizationMode.*;
import static org.gridgain.grid.events.GridEventType.*;

/**
 * Test for TRANSFORM events recording.
 */
@SuppressWarnings("ConstantConditions")
public class GridCacheTransformEventSelfTest extends GridCommonAbstractTest {
    /** Nodes count. */
    private static final int GRID_CNT = 3;

    /** Backups count for partitioned cache. */
    private static final int BACKUP_CNT = 1;

    /** Cache name. */
    private static final String CACHE_NAME = "cache";

    /** Closure name. */
    private static final String CLO_NAME = Transformer.class.getName();

    /** Key 1. */
    private Integer key1;

    /** Key 2. */
    private Integer key2;

    /** Two keys in form of a set. */
    private Set<Integer> keys;

    /** IP finder. */
    private static final GridTcpDiscoveryIpFinder IP_FINDER = new GridTcpDiscoveryVmIpFinder(true);

    /** Nodes. */
    private Grid[] grids;

    /** Node IDs. */
    private UUID[] ids;

    /** Caches. */
    private GridCache<Integer, Integer>[] caches;

    /** Recorded events.*/
    private ConcurrentHashSet<GridCacheEvent> evts;

    /** Cache mode. */
    private GridCacheMode cacheMode;

    /** Atomicity mode. */
    private GridCacheAtomicityMode atomicityMode;

    /** TX concurrency. */
    private GridCacheTxConcurrency txConcurrency;

    /** TX isolation. */
    private GridCacheTxIsolation txIsolation;

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        GridTcpDiscoverySpi discoSpi = new GridTcpDiscoverySpi();

        discoSpi.setIpFinder(IP_FINDER);

        GridCacheConfiguration ccfg = new GridCacheConfiguration();

        ccfg.setName(CACHE_NAME);

        ccfg.setCacheMode(cacheMode);
        ccfg.setAtomicityMode(atomicityMode);
        ccfg.setWriteSynchronizationMode(FULL_SYNC);
        ccfg.setAtomicWriteOrderMode(PRIMARY);
        ccfg.setDefaultTxConcurrency(txConcurrency);
        ccfg.setDefaultTxIsolation(txIsolation);
        ccfg.setDistributionMode(GridCacheDistributionMode.PARTITIONED_ONLY);

        if (cacheMode == PARTITIONED)
            ccfg.setBackups(BACKUP_CNT);

        cfg.setDiscoverySpi(discoSpi);
        cfg.setCacheConfiguration(ccfg);
        cfg.setLocalHost("127.0.0.1");
        cfg.setIncludeEventTypes(EVT_CACHE_OBJECT_READ);

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        stopAllGrids();

        grids = null;
        ids = null;
        caches = null;

        evts = null;

        key1 = null;
        key2 = null;
        keys = null;
    }

    /**
     * Initialization routine.
     *
     * @param cacheMode Cache mode.
     * @param atomicityMode Atomicity mode.
     * @param txConcurrency TX concurrency.
     * @param txIsolation TX isolation.
     * @throws Exception If failed.
     */
    @SuppressWarnings("unchecked")
    private void initialize(GridCacheMode cacheMode, GridCacheAtomicityMode atomicityMode,
        GridCacheTxConcurrency txConcurrency, GridCacheTxIsolation txIsolation) throws Exception {
        this.cacheMode = cacheMode;
        this.atomicityMode = atomicityMode;
        this.txConcurrency = txConcurrency;
        this.txIsolation = txIsolation;

        evts = new ConcurrentHashSet<>();

        startGrids(GRID_CNT);

        grids = new Grid[GRID_CNT];
        ids = new UUID[GRID_CNT];
        caches = new GridCache[GRID_CNT];

        for (int i = 0; i < GRID_CNT; i++) {
            grids[i] = grid(i);

            ids[i] = grids[i].localNode().id();

            caches[i] = grids[i].cache(CACHE_NAME);

            grids[i].events().localListen(new GridPredicate<GridEvent>() {
                @Override public boolean apply(GridEvent evt) {
                    GridCacheEvent evt0 = (GridCacheEvent)evt;

                    if (evt0.closureClassName() != null) {
                        System.out.println("ADDED: [nodeId=" + evt0.node() + ", evt=" + evt0 + ']');

                        evts.add(evt0);
                    }

                    return true;
                }
            }, EVT_CACHE_OBJECT_READ);
        }

        int key = 0;

        while (true) {
            if (cacheMode != PARTITIONED || (caches[0].entry(key).primary() && caches[1].entry(key).backup())) {
                key1 = key++;

                break;
            }
            else
                key++;
        }

        while (true) {
            if (cacheMode != PARTITIONED || (caches[0].entry(key).primary() && caches[1].entry(key).backup())) {
                key2 = key;

                break;
            }
            else
                key++;
        }

        keys = new HashSet<>();

        keys.add(key1);
        keys.add(key2);

        caches[0].put(key1, 1);
        caches[0].put(key2, 2);

        for (int i = 0; i < GRID_CNT; i++) {
            grids[i].events().localListen(new GridPredicate<GridEvent>() {
                @Override public boolean apply(GridEvent evt) {
                    GridCacheEvent evt0 = (GridCacheEvent)evt;

                    if (evt0.closureClassName() != null)
                        evts.add(evt0);

                    return true;
                }
            }, EVT_CACHE_OBJECT_READ);
        }
    }

    /**
     * Test TRANSACTIONAL LOCAL cache with OPTIMISTIC/REPEATABLE_READ transaction.
     *
     * @throws Exception If failed.
     */
    public void testTxLocalOptimisticRepeatableRead() throws Exception {
        checkTx(LOCAL, OPTIMISTIC, REPEATABLE_READ);
    }

    /**
     * Test TRANSACTIONAL LOCAL cache with OPTIMISTIC/READ_COMMITTED transaction.
     *
     * @throws Exception If failed.
     */
    public void testTxLocalOptimisticReadCommitted() throws Exception {
        checkTx(LOCAL, OPTIMISTIC, READ_COMMITTED);
    }

    /**
     * Test TRANSACTIONAL LOCAL cache with OPTIMISTIC/SERIALIZABLE transaction.
     *
     * @throws Exception If failed.
     */
    public void testTxLocalOptimisticSerializable() throws Exception {
        checkTx(LOCAL, OPTIMISTIC, SERIALIZABLE);
    }

    /**
     * Test TRANSACTIONAL LOCAL cache with PESSIMISTIC/REPEATABLE_READ transaction.
     *
     * @throws Exception If failed.
     */
    public void testTxLocalPessimisticRepeatableRead() throws Exception {
        checkTx(LOCAL, PESSIMISTIC, REPEATABLE_READ);
    }

    /**
     * Test TRANSACTIONAL LOCAL cache with PESSIMISTIC/READ_COMMITTED transaction.
     *
     * @throws Exception If failed.
     */
    public void testTxLocalPessimisticReadCommitted() throws Exception {
        checkTx(LOCAL, PESSIMISTIC, READ_COMMITTED);
    }

    /**
     * Test TRANSACTIONAL LOCAL cache with PESSIMISTIC/SERIALIZABLE transaction.
     *
     * @throws Exception If failed.
     */
    public void testTxLocalPessimisticSerializable() throws Exception {
        checkTx(LOCAL, PESSIMISTIC, SERIALIZABLE);
    }

    /**
     * Test TRANSACTIONAL PARTITIONED cache with OPTIMISTIC/REPEATABLE_READ transaction.
     *
     * @throws Exception If failed.
     */
    public void testTxPartitionedOptimisticRepeatableRead() throws Exception {
        checkTx(PARTITIONED, OPTIMISTIC, REPEATABLE_READ);
    }

    /**
     * Test TRANSACTIONAL PARTITIONED cache with OPTIMISTIC/READ_COMMITTED transaction.
     *
     * @throws Exception If failed.
     */
    public void testTxPartitionedOptimisticReadCommitted() throws Exception {
        checkTx(PARTITIONED, OPTIMISTIC, READ_COMMITTED);
    }

    /**
     * Test TRANSACTIONAL PARTITIONED cache with OPTIMISTIC/SERIALIZABLE transaction.
     *
     * @throws Exception If failed.
     */
    public void testTxPartitionedOptimisticSerializable() throws Exception {
        checkTx(PARTITIONED, OPTIMISTIC, SERIALIZABLE);
    }

    /**
     * Test TRANSACTIONAL PARTITIONED cache with PESSIMISTIC/REPEATABLE_READ transaction.
     *
     * @throws Exception If failed.
     */
    public void testTxPartitionedPessimisticRepeatableRead() throws Exception {
        checkTx(PARTITIONED, PESSIMISTIC, REPEATABLE_READ);
    }

    /**
     * Test TRANSACTIONAL PARTITIONED cache with PESSIMISTIC/READ_COMMITTED transaction.
     *
     * @throws Exception If failed.
     */
    public void testTxPartitionedPessimisticReadCommitted() throws Exception {
        checkTx(PARTITIONED, PESSIMISTIC, READ_COMMITTED);
    }

    /**
     * Test TRANSACTIONAL PARTITIONED cache with PESSIMISTIC/SERIALIZABLE transaction.
     *
     * @throws Exception If failed.
     */
    public void testTxPartitionedPessimisticSerializable() throws Exception {
        checkTx(PARTITIONED, PESSIMISTIC, SERIALIZABLE);
    }

    /**
     * Test TRANSACTIONAL REPLICATED cache with OPTIMISTIC/REPEATABLE_READ transaction.
     *
     * @throws Exception If failed.
     */
    public void testTxReplicatedOptimisticRepeatableRead() throws Exception {
        checkTx(REPLICATED, OPTIMISTIC, REPEATABLE_READ);
    }

    /**
     * Test TRANSACTIONAL REPLICATED cache with OPTIMISTIC/READ_COMMITTED transaction.
     *
     * @throws Exception If failed.
     */
    public void testTxReplicatedOptimisticReadCommitted() throws Exception {
        checkTx(REPLICATED, OPTIMISTIC, READ_COMMITTED);
    }

    /**
     * Test TRANSACTIONAL REPLICATED cache with OPTIMISTIC/SERIALIZABLE transaction.
     *
     * @throws Exception If failed.
     */
    public void testTxReplicatedOptimisticSerializable() throws Exception {
        checkTx(REPLICATED, OPTIMISTIC, SERIALIZABLE);
    }

    /**
     * Test TRANSACTIONAL REPLICATED cache with PESSIMISTIC/REPEATABLE_READ transaction.
     *
     * @throws Exception If failed.
     */
    public void testTxReplicatedPessimisticRepeatableRead() throws Exception {
        checkTx(REPLICATED, PESSIMISTIC, REPEATABLE_READ);
    }

    /**
     * Test TRANSACTIONAL REPLICATED cache with PESSIMISTIC/READ_COMMITTED transaction.
     *
     * @throws Exception If failed.
     */
    public void testTxReplicatedPessimisticReadCommitted() throws Exception {
        checkTx(REPLICATED, PESSIMISTIC, READ_COMMITTED);
    }

    /**
     * Test TRANSACTIONAL REPLICATED cache with PESSIMISTIC/SERIALIZABLE transaction.
     *
     * @throws Exception If failed.
     */
    public void testTxReplicatedPessimisticSerializable() throws Exception {
        checkTx(REPLICATED, PESSIMISTIC, SERIALIZABLE);
    }

    /**
     * Test ATOMIC LOCAL cache.
     *
     * @throws Exception If failed.
     */
    public void testAtomicLocal() throws Exception {
        checkAtomic(LOCAL);
    }

    /**
     * Test ATOMIC PARTITIONED cache.
     *
     * @throws Exception If failed.
     */
    public void testAtomicPartitioned() throws Exception {
        checkAtomic(PARTITIONED);
    }

    /**
     * Test ATOMIC REPLICATED cache.
     *
     * @throws Exception If failed.
     */
    public void testAtomicReplicated() throws Exception {
        checkAtomic(REPLICATED);
    }

    /**
     * Check ATOMIC cache.
     *
     * @param cacheMode Cache mode.
     * @throws Exception If failed.
     */
    private void checkAtomic(GridCacheMode cacheMode) throws Exception {
        initialize(cacheMode, ATOMIC, null, null);

        caches[0].transform(key1, new Transformer());

        checkEventNodeIdsStrict(primaryIdsForKeys(key1));

        assert evts.isEmpty();

        caches[0].transformAll(keys, new Transformer());

        checkEventNodeIdsStrict(primaryIdsForKeys(key1, key2));
    }

    /**
     * Check TRANSACTIONAL cache.
     *
     * @param cacheMode Cache mode.
     * @param txConcurrency TX concurrency.
     * @param txIsolation TX isolation.
     *
     * @throws Exception If failed.
     */
    private void checkTx(GridCacheMode cacheMode, GridCacheTxConcurrency txConcurrency,
        GridCacheTxIsolation txIsolation) throws Exception {
        initialize(cacheMode, TRANSACTIONAL, txConcurrency, txIsolation);

        System.out.println("BEFORE: " + evts.size());

        caches[0].transform(key1, new Transformer());

        System.out.println("AFTER: " + evts.size());

        checkEventNodeIdsStrict(idsForKeys(key1));

        assert evts.isEmpty();

        caches[0].transformAll(keys, new Transformer());

        checkEventNodeIdsStrict(idsForKeys(key1, key2));
    }

    /**
     * Get node IDs where the given keys must reside.
     *
     * @param keys Keys.
     * @return Node IDs.
     */
    private UUID[] idsForKeys(int... keys) {
        return idsForKeys(false, keys);
    }

    /**
     * Get primary node IDs where the given keys must reside.
     *
     * @param keys Keys.
     * @return Node IDs.
     */
    private UUID[] primaryIdsForKeys(int... keys) {
        return idsForKeys(true, keys);
    }

    /**
     * Get node IDs where the given keys must reside.
     *
     * @param primaryOnly Primary only flag.
     * @param keys Keys.
     * @return Node IDs.
     */
    @SuppressWarnings("UnusedDeclaration")
    private UUID[] idsForKeys(boolean primaryOnly, int... keys) {
        List<UUID> res = new ArrayList<>();

        if (cacheMode == LOCAL) {
            for (int key : keys)
                res.add(ids[0]); // Perform PUTs from the node with index 0.
        }
        else if (cacheMode == PARTITIONED) {
            for (int key : keys) {
                for (int i = 0; i < GRID_CNT; i++) {
                    GridCacheEntry<Integer, Integer> entry = caches[i].entry(key);

                    if (entry.primary() || (!primaryOnly && entry.backup()))
                        res.add(ids[i]);
                }
            }
        }
        else if (cacheMode == REPLICATED) {
            for (int key : keys) {
                if (primaryOnly)
                    res.add(caches[0].affinity().mapKeyToNode(key).id());
                else
                    res.addAll(Arrays.asList(ids));
            }
        }

        return res.toArray(new UUID[res.size()]);
    }

    /**
     * Ensure that events were recorded on the given nodes.
     *
     * @param ids Event IDs.
     */
    private void checkEventNodeIdsStrict(UUID... ids) {
        if (ids == null)
            assertTrue(evts.isEmpty());
        else {
            assertEquals(ids.length, evts.size());

            for (UUID id : ids) {
                GridCacheEvent foundEvt = null;

                for (GridCacheEvent evt : evts) {
                    if (F.eq(id, evt.node().id())) {
                        assertEquals(CLO_NAME, evt.closureClassName());

                        foundEvt = evt;

                        break;
                    }
                }

                if (foundEvt == null) {
                    GridCache<Integer, Integer> affectedCache = null;

                    for (int i = 0; i < GRID_CNT; i++) {
                        if (F.eq(this.ids[i], id)) {
                            affectedCache = caches[i];

                            break;
                        }
                    }

                    GridCacheEntry<Integer, Integer> entry1 = affectedCache.entry(key1);
                    GridCacheEntry<Integer, Integer> entry2 = affectedCache.entry(key2);

                    fail("Expected transform event was not triggered on the node [nodeId=" + id +
                        ", key1Primary=" + entry1.primary() + ", key1Backup=" + entry1.backup() +
                        ", key2Primary=" + entry2.primary() + ", key2Backup=" + entry2.backup() + ']');
                }
                else
                    evts.remove(foundEvt);
            }
        }
    }

    /**
     * Transform closure.
     */
    private static class Transformer implements GridClosure<Integer, Integer>, Serializable {
        /** {@inheritDoc} */
        @Override public Integer apply(Integer val) {
            return ++val;
        }
    }
}
