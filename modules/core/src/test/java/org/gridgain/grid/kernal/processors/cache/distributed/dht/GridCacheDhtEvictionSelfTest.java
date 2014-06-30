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
import org.gridgain.grid.cache.affinity.consistenthash.*;
import org.gridgain.grid.cache.eviction.fifo.*;
import org.gridgain.grid.events.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.kernal.processors.cache.distributed.near.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.testframework.junits.common.*;

import java.util.*;
import java.util.concurrent.atomic.*;

import static org.gridgain.grid.cache.GridCacheAtomicityMode.*;
import static org.gridgain.grid.cache.GridCacheMode.*;
import static org.gridgain.grid.cache.GridCacheDistributionMode.*;
import static org.gridgain.grid.cache.GridCachePreloadMode.*;
import static org.gridgain.grid.events.GridEventType.*;

/**
 * Tests for dht cache eviction.
 */
public class GridCacheDhtEvictionSelfTest extends GridCommonAbstractTest {
    /** */
    private static final int GRID_CNT = 2;

    /** */
    private GridTcpDiscoveryIpFinder ipFinder = new GridTcpDiscoveryVmIpFinder(true);

    /** Default constructor. */
    public GridCacheDhtEvictionSelfTest() {
        super(false /* don't start grid. */);
    }

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        GridTcpDiscoverySpi disco = new GridTcpDiscoverySpi();

        disco.setIpFinder(ipFinder);

        cfg.setDiscoverySpi(disco);

        GridCacheConfiguration cacheCfg = defaultCacheConfiguration();

        cacheCfg.setCacheMode(PARTITIONED);
        cacheCfg.setPreloadMode(NONE);
        cacheCfg.setWriteSynchronizationMode(GridCacheWriteSynchronizationMode.FULL_SYNC);
        cacheCfg.setSwapEnabled(false);
        cacheCfg.setEvictSynchronized(true);
        cacheCfg.setEvictNearSynchronized(true);
        cacheCfg.setAtomicityMode(TRANSACTIONAL);
        cacheCfg.setDistributionMode(NEAR_PARTITIONED);
        cacheCfg.setBackups(1);

        // Set eviction queue size explicitly.
        cacheCfg.setEvictMaxOverflowRatio(0);
        cacheCfg.setEvictSynchronizedKeyBufferSize(1);
        cacheCfg.setEvictionPolicy(new GridCacheFifoEvictionPolicy(10000));
        cacheCfg.setNearEvictionPolicy(new GridCacheFifoEvictionPolicy(10000));

        cfg.setCacheConfiguration(cacheCfg);

        return cfg;
    }

    /** {@inheritDoc} */
    @SuppressWarnings({"ConstantConditions"})
    @Override protected void beforeTestsStarted() throws Exception {
        super.beforeTestsStarted();

        if (GRID_CNT < 2)
            throw new GridException("GRID_CNT must not be less than 2.");

        startGrids(GRID_CNT);
    }

    /** {@inheritDoc} */
    @Override protected void afterTestsStopped() throws Exception {
        super.afterTestsStopped();

        stopAllGrids();
    }

    /** {@inheritDoc} */
    @SuppressWarnings({"SizeReplaceableByIsEmpty"})
    @Override protected void beforeTest() throws Exception {
        for (int i = 0; i < GRID_CNT; i++) {
            assert near(grid(i)).size() == 0;
            assert dht(grid(i)).size() == 0;

            assert near(grid(i)).isEmpty();
            assert dht(grid(i)).isEmpty();
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings({"unchecked"})
    @Override protected void afterTest() throws Exception {
        for (int i = 0; i < GRID_CNT; i++) {
            near(grid(i)).removeAll(new GridPredicate[] {F.alwaysTrue()});

            assert near(grid(i)).isEmpty() : "Near cache is not empty [idx=" + i + "]";
            assert dht(grid(i)).isEmpty() : "Dht cache is not empty [idx=" + i + "]";
        }
    }

    /**
     * @param node Node.
     * @return Grid for the given node.
     */
    private Grid grid(GridNode node) {
        return G.grid(node.id());
    }

    /**
     * @param g Grid.
     * @return Near cache.
     */
    @SuppressWarnings({"unchecked"})
    private GridNearCacheAdapter<Integer, String> near(Grid g) {
        return (GridNearCacheAdapter)((GridKernal)g).internalCache();
    }

    /**
     * @param g Grid.
     * @return Dht cache.
     */
    @SuppressWarnings({"unchecked", "TypeMayBeWeakened"})
    private GridDhtCacheAdapter<Integer, String> dht(Grid g) {
        return ((GridNearCacheAdapter)((GridKernal)g).internalCache()).dht();
    }

    /**
     * @param idx Index.
     * @return Affinity.
     */
    private GridCacheConsistentHashAffinityFunction affinity(int idx) {
        return (GridCacheConsistentHashAffinityFunction)grid(idx).cache(null).configuration().getAffinity();
    }

    /**
     * @param key Key.
     * @return Primary node for the given key.
     */
    private Collection<GridNode> keyNodes(Object key) {
        GridCacheConsistentHashAffinityFunction aff = affinity(0);

        return aff.nodes(aff.partition(key), grid(0).nodes(), 1);
    }

    /**
     * @param nodeId Node id.
     * @return Predicate for events belonging to specified node.
     */
    private GridPredicate<GridEvent> nodeEvent(final UUID nodeId) {
        assert nodeId != null;

        return new P1<GridEvent>() {
            @Override public boolean apply(GridEvent e) {
                info("Predicate called [e.nodeId()=" + e.node().id() + ", nodeId=" + nodeId + ']');

                return e.node().id().equals(nodeId);
            }
        };
    }

    /**
     * JUnit.
     *
     * @throws Exception If failed.
     */
    @SuppressWarnings("NullArgumentToVariableArgMethod")
    public void testSingleKey() throws Exception {
        Integer key = 1;

        Collection<GridNode> nodes = new ArrayList<>(keyNodes(key));

        GridNode primary = F.first(nodes);

        assert primary != null;

        nodes.remove(primary);

        GridNode backup = F.first(nodes);

        assert backup != null;

        assert !F.eqNodes(primary, backup);

        info("Key primary node: " + primary.id());
        info("Key backup node: " + backup.id());

        GridNearCacheAdapter<Integer, String> nearPrimary = near(grid(primary));
        GridDhtCacheAdapter<Integer, String> dhtPrimary = dht(grid(primary));

        GridNearCacheAdapter<Integer, String> nearBackup = near(grid(backup));
        GridDhtCacheAdapter<Integer, String> dhtBackup = dht(grid(backup));

        String val = "v1";

        // Put on primary node.
        nearPrimary.put(key, val, null);

        assertEquals(val, nearPrimary.peek(key));
        assertEquals(val, dhtPrimary.peek(key));

        assertEquals(val, nearBackup.peek(key));
        assertEquals(val, dhtBackup.peek(key));

        GridDhtCacheEntry<Integer, String> entryPrimary = dhtPrimary.peekExx(key);
        GridDhtCacheEntry<Integer, String> entryBackup = dhtBackup.peekExx(key);

        assert entryPrimary != null;
        assert entryBackup != null;

        assertTrue(entryPrimary.readers().isEmpty());
        assertTrue(entryBackup.readers().isEmpty());

        GridFuture<GridEvent> futBackup =
            grid(backup).events().waitForLocal(nodeEvent(backup.id()), EVT_CACHE_ENTRY_EVICTED);

        GridFuture<GridEvent> futPrimary =
            grid(primary).events().waitForLocal(nodeEvent(primary.id()), EVT_CACHE_ENTRY_EVICTED);

        // Evict on primary node.
        // It should trigger dht eviction and eviction on backup node.
        assert grid(primary).cache(null).evict(key);

        // Give 5 seconds for eviction event to occur on backup and primary node.
        futBackup.get(3000);
        futPrimary.get(3000);

        assertEquals(0, nearPrimary.size());

        assertNull(nearPrimary.peekExx(key));
        assertNull(dhtPrimary.peekExx(key));

        assertNull(nearBackup.peekExx(key));
        assertNull(dhtBackup.peekExx(key));
    }

    /**
     * JUnit.
     *
     * @throws Exception If failed.
     */
    @SuppressWarnings("NullArgumentToVariableArgMethod")
    public void testMultipleKeys() throws Exception {
        final int keyCnt = 1000;

        final Grid primaryGrid = grid(0);
        final Grid backupGrid = grid(1);

        GridNearCacheAdapter<Integer, String> nearPrimary = near(primaryGrid);
        GridDhtCacheAdapter<Integer, String> dhtPrimary = dht(primaryGrid);

        GridNearCacheAdapter<Integer, String> nearBackup = near(backupGrid);
        GridDhtCacheAdapter<Integer, String> dhtBackup = dht(backupGrid);

        Collection<Integer> keys = new ArrayList<>(keyCnt);

        for (int key = 0; keys.size() < keyCnt; key++)
            if (F.eqNodes(primaryGrid.localNode(), F.first(keyNodes(key))))
                keys.add(key++);

        info("Test keys: " + keys);

        // Put on primary node.
        for (Integer key : keys)
            nearPrimary.put(key, "v" + key, null);

        for (Integer key : keys) {
            String val = "v" + key;

            assertEquals(val, nearPrimary.peek(key));
            assertEquals(val, dhtPrimary.peek(key));

            assertEquals(val, nearBackup.peek(key));
            assertEquals(val, dhtBackup.peek(key));
        }

        final AtomicInteger cntBackup = new AtomicInteger();

        GridFuture<GridEvent> futBackup = backupGrid.events().waitForLocal(new P1<GridEvent>() {
            @Override public boolean apply(GridEvent e) {
                return e.node().id().equals(backupGrid.localNode().id()) && cntBackup.incrementAndGet() == keyCnt;
            }
        }, EVT_CACHE_ENTRY_EVICTED);

        final AtomicInteger cntPrimary = new AtomicInteger();

        GridFuture<GridEvent> futPrimary = primaryGrid.events().waitForLocal(new P1<GridEvent>() {
            @Override public boolean apply(GridEvent e) {
                return e.node().id().equals(primaryGrid.localNode().id()) && cntPrimary.incrementAndGet() == keyCnt;
            }
        }, EVT_CACHE_ENTRY_EVICTED);

        // Evict on primary node.
        // Eviction of the last key should trigger queue processing.
        for (Integer key : keys) {
            boolean evicted = primaryGrid.cache(null).evict(key);

            assert evicted;
        }

        // Give 5 seconds for eviction events to occur on backup and primary node.
        futBackup.get(3000);
        futPrimary.get(3000);

        info("nearBackupSize: " + nearBackup.size());
        info("dhtBackupSize: " + dhtBackup.size());
        info("nearPrimarySize: " + nearPrimary.size());
        info("dhtPrimarySize: " + dhtPrimary.size());

        // Check backup node first.
        for (Integer key : keys) {
            String msg = "Failed key: " + key;

            assertNull(msg, nearBackup.peek(key));
            assertNull(msg, dhtBackup.peek(key));
            assertNull(msg, nearBackup.peekExx(key));
            assertNull(msg, dhtBackup.peekExx(key));
        }

        for (Integer key : keys) {
            String msg = "Failed key: " + key;

            assertNull(msg, nearPrimary.peek(key));
            assertNull(msg, dhtPrimary.peek(key));
            assertNull(msg, nearPrimary.peekExx(key));
            assertNull(dhtPrimary.peekExx(key));
        }
    }
}
