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

import static org.gridgain.grid.cache.GridCacheAtomicityMode.*;
import static org.gridgain.grid.cache.GridCacheMode.*;
import static org.gridgain.grid.cache.GridCacheDistributionMode.*;
import static org.gridgain.grid.cache.GridCachePreloadMode.*;
import static org.gridgain.grid.events.GridEventType.*;

/**
 * Tests for dht cache eviction.
 */
public class GridCacheDhtEvictionNearReadersSelfTest extends GridCommonAbstractTest {
    /** */
    private static final int GRID_CNT = 4;

    /** */
    private GridTcpDiscoveryIpFinder ipFinder = new GridTcpDiscoveryVmIpFinder(true);

    /** Default constructor. */
    public GridCacheDhtEvictionNearReadersSelfTest() {
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
        cacheCfg.setWriteSynchronizationMode(GridCacheWriteSynchronizationMode.FULL_SYNC);
        cacheCfg.setSwapEnabled(false);
        cacheCfg.setEvictSynchronized(true);
        cacheCfg.setEvictNearSynchronized(true);
        cacheCfg.setPreloadMode(SYNC);
        cacheCfg.setAtomicityMode(atomicityMode());
        cacheCfg.setDistributionMode(NEAR_PARTITIONED);
        cacheCfg.setBackups(1);

        // Set eviction queue size explicitly.
        cacheCfg.setEvictSynchronizedKeyBufferSize(1);
        cacheCfg.setEvictMaxOverflowRatio(0);
        cacheCfg.setEvictionPolicy(new GridCacheFifoEvictionPolicy(10));
        cacheCfg.setNearEvictionPolicy(new GridCacheFifoEvictionPolicy(10));

        cfg.setCacheConfiguration(cacheCfg);

        return cfg;
    }

    /**
     * @return Atomicity mode.
     */
    public GridCacheAtomicityMode atomicityMode() {
        return TRANSACTIONAL;
    }

    /** {@inheritDoc} */
    @SuppressWarnings({"ConstantConditions"})
    @Override protected void beforeTestsStarted() throws Exception {
        super.beforeTestsStarted();

        if (GRID_CNT < 2)
            throw new GridException("GRID_CNT must not be less than 2.");

        startGridsMultiThreaded(GRID_CNT);
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
    public void testReaders() throws Exception {
        Integer key = 1;

        Collection<GridNode> nodes = new ArrayList<>(keyNodes(key));

        GridNode primary = F.first(nodes);

        assert primary != null;

        nodes.remove(primary);

        GridNode backup = F.first(nodes);

        assert backup != null;

        // Now calculate other node that doesn't own the key.
        nodes = new ArrayList<>(grid(0).nodes());

        nodes.remove(primary);
        nodes.remove(backup);

        GridNode other = F.first(nodes);

        assert !F.eqNodes(primary, backup);
        assert !F.eqNodes(primary, other);
        assert !F.eqNodes(backup, other);

        info("Primary node: " + primary.id());
        info("Backup node: " + backup.id());
        info("Other node: " + other.id());

        GridNearCacheAdapter<Integer, String> nearPrimary = near(grid(primary));
        GridDhtCacheAdapter<Integer, String> dhtPrimary = dht(grid(primary));

        GridNearCacheAdapter<Integer, String> nearBackup = near(grid(backup));
        GridDhtCacheAdapter<Integer, String> dhtBackup = dht(grid(backup));

        GridNearCacheAdapter<Integer, String> nearOther = near(grid(other));
        GridDhtCacheAdapter<Integer, String> dhtOther = dht(grid(other));

        String val = "v1";

        // Put on primary node.
        nearPrimary.put(key, val);

        GridDhtCacheEntry<Integer, String> entryPrimary = dhtPrimary.peekExx(key);
        GridDhtCacheEntry<Integer, String> entryBackup = dhtBackup.peekExx(key);

        assert entryPrimary != null;
        assert entryBackup != null;
        assert nearOther.peekExx(key) == null;
        assert dhtOther.peekExx(key) == null;

        GridFuture<GridEvent> futOther =
            grid(other).events().waitForLocal(nodeEvent(other.id()), EVT_CACHE_ENTRY_EVICTED);

        GridFuture<GridEvent> futBackup =
            grid(backup).events().waitForLocal(nodeEvent(backup.id()), EVT_CACHE_ENTRY_EVICTED);

        GridFuture<GridEvent> futPrimary =
            grid(primary).events().waitForLocal(nodeEvent(primary.id()), EVT_CACHE_ENTRY_EVICTED);

        // Get value on other node, it should be loaded to near cache.
        assertEquals(val, nearOther.get(key, true, null));

        entryPrimary = dhtPrimary.peekExx(key);
        entryBackup = dhtBackup.peekExx(key);

        assert entryPrimary != null;
        assert entryBackup != null;

        assertEquals(val, nearOther.peek(key));

        assertTrue(!entryPrimary.readers().isEmpty());

        // Evict on primary node.
        // It will trigger dht eviction and eviction on backup node.
        grid(primary).cache(null).evict(key);

        futOther.get(3000);
        futBackup.get(3000);
        futPrimary.get(3000);

        assertNull(dhtPrimary.peek(key));
        assertNull(nearPrimary.peek(key));

        assertNull(dhtBackup.peek(key));
        assertNull(nearBackup.peek(key));

        assertNull(dhtOther.peek(key));
        assertNull(nearOther.peek(key));
    }
}
