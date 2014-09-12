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
import org.gridgain.grid.events.*;
import org.gridgain.grid.kernal.processors.cache.distributed.dht.preloader.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.testframework.junits.common.*;

import java.util.*;

import static org.gridgain.grid.GridDeploymentMode.*;
import static org.gridgain.grid.cache.GridCacheAtomicityMode.*;
import static org.gridgain.grid.cache.GridCacheMode.*;
import static org.gridgain.grid.cache.GridCacheDistributionMode.*;
import static org.gridgain.grid.cache.GridCachePreloadMode.*;
import static org.gridgain.grid.events.GridEventType.*;

/**
 * Test cases for partitioned cache {@link GridDhtPreloader preloader}.
 */
public class GridCacheDhtPreloadDisabledSelfTest extends GridCommonAbstractTest {
    /** Flat to print preloading events. */
    private static final boolean DEBUG = false;

    /** */
    private static final long TEST_TIMEOUT = 5 * 60 * 1000;

    /** Default backups. */
    private static final int DFLT_BACKUPS = 1;

    /** Partitions. */
    private static final int DFLT_PARTITIONS = 521;

    /** Number of key backups. Each test method can set this value as required. */
    private int backups = DFLT_BACKUPS;

    /** Number of partitions. */
    private int partitions = DFLT_PARTITIONS;

    /** IP finder. */
    private GridTcpDiscoveryIpFinder ipFinder = new GridTcpDiscoveryVmIpFinder(true);

    /**
     *
     */
    public GridCacheDhtPreloadDisabledSelfTest() {
        super(false /*start grid. */);
    }

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        GridCacheConfiguration cacheCfg = defaultCacheConfiguration();

        cacheCfg.setCacheMode(PARTITIONED);
        cacheCfg.setWriteSynchronizationMode(GridCacheWriteSynchronizationMode.FULL_ASYNC);
        cacheCfg.setPreloadMode(NONE);
        cacheCfg.setAffinity(new GridCacheConsistentHashAffinityFunction(false, partitions));
        cacheCfg.setBackups(backups);
        cacheCfg.setAtomicityMode(TRANSACTIONAL);
        cacheCfg.setDistributionMode(NEAR_PARTITIONED);
        //cacheCfg.setPreloadThreadPoolSize(1);

        GridTcpDiscoverySpi disco = new GridTcpDiscoverySpi();

        disco.setIpFinder(ipFinder);

        cfg.setDiscoverySpi(disco);
        cfg.setCacheConfiguration(cacheCfg);
        cfg.setDeploymentMode(CONTINUOUS);

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        backups = DFLT_BACKUPS;
        partitions = DFLT_PARTITIONS;
    }

    /** {@inheritDoc} */
    @Override protected long getTestTimeout() {
        return TEST_TIMEOUT;
    }

    /**
     * @param i Grid index.
     * @return Topology.
     */
    private GridDhtPartitionTopology<Integer, String> topology(int i) {
        return near(grid(i).<Integer, String>cache(null)).dht().topology();
    }

    /** @throws Exception If failed. */
    public void testSamePartitionMap() throws Exception {
        backups = 1;
        partitions = 10;

        int nodeCnt = 4;

        startGridsMultiThreaded(nodeCnt);

        try {
            for (int p = 0; p < partitions; p++) {
                List<Collection<GridNode>> mappings = new ArrayList<>(nodeCnt);

                for (int i = 0; i < nodeCnt; i++) {
                    Collection<GridNode> nodes = topology(i).nodes(p, -1);
                    List<GridNode> owners = topology(i).owners(p);

                    int size = backups + 1;

                    assert owners.size() == size : "Size mismatch [nodeIdx=" + i + ", p=" + p + ", size=" + size +
                        ", owners=" + F.nodeIds(owners) + ']';
                    assert nodes.size() == size : "Size mismatch [nodeIdx=" + i + ", p=" + p + ", size=" + size +
                        ", nodes=" + F.nodeIds(nodes) + ']';

                    assert F.eqNotOrdered(nodes, owners);
                    assert F.eqNotOrdered(owners, nodes);

                    mappings.add(owners);
                }

                for (int i = 0; i < mappings.size(); i++) {
                    Collection<GridNode> m1 = mappings.get(i);

                    for (int j = 0; j != i && j < mappings.size(); j++) {
                        Collection<GridNode> m2 = mappings.get(j);

                        assert F.eqNotOrdered(m1, m2) : "Mappings are not equal [m1=" + F.nodeIds(m1) + ", m2=" +
                            F.nodeIds(m2) + ']';
                        assert F.eqNotOrdered(m2, m1) : "Mappings are not equal [m1=" + F.nodeIds(m1) + ", m2=" +
                            F.nodeIds(m2) + ']';
                    }
                }
            }
        }
        finally {
            stopAllGrids();
        }
    }

    /** @throws Exception If failed. */
    public void testDisabledPreloader() throws Exception {
        try {
            Grid grid1 = startGrid(0);

            GridCache<Integer, String> cache1 = grid1.cache(null);

            int keyCnt = 10;

            putKeys(cache1, keyCnt);

            for (int i = 0; i < keyCnt; i++) {
                assertNull(near(cache1).peekEx(i));
                assertNotNull((dht(cache1).peekEx(i)));

                assertEquals(Integer.toString(i), cache1.peek(i));
            }

            int nodeCnt = 3;

            List<Grid> grids = new ArrayList<>(nodeCnt);

            startGrids(nodeCnt, 1, grids);

            // Check all nodes.
            for (Grid g : grids) {
                GridCache<Integer, String> c = g.cache(null);

                for (int i = 0; i < keyCnt; i++)
                    assertNull(c.peek(i));
            }

            Collection<Integer> keys = new LinkedList<>();

            for (int i = 0; i < keyCnt; i++)
                if (cache1.affinity().mapKeyToNode(i).equals(grid1.localNode()))
                    keys.add(i);

            info(">>> Finished checking nodes [keyCnt=" + keyCnt + ", nodeCnt=" + nodeCnt + ", grids=" +
                U.grids2names(grids) + ']');

            for (Iterator<Grid> it = grids.iterator(); it.hasNext(); ) {
                Grid g = it.next();

                it.remove();

                stopGrid(g.name());

                // Check all nodes.
                for (Grid gg : grids) {
                    GridCache<Integer, String> c = gg.cache(null);

                    for (int i = 0; i < keyCnt; i++)
                        assertNull(c.peek(i));
                }
            }

            for (Integer i : keys)
                assertEquals(i.toString(), cache1.peek(i));
        }
        catch (Error | Exception e) {
            error("Test failed.", e);

            throw e;
        }
        finally {
            stopAllGrids();
        }
    }

    /**
     * @param cnt Number of grids.
     * @param startIdx Start node index.
     * @param list List of started grids.
     * @throws Exception If failed.
     */
    private void startGrids(int cnt, int startIdx, Collection<Grid> list) throws Exception {
        for (int i = 0; i < cnt; i++) {
            final Grid g = startGrid(startIdx++);

            if (DEBUG)
                g.events().localListen(new GridPredicate<GridEvent>() {
                    @Override public boolean apply(GridEvent evt) {
                        info("\n>>> Preload event [grid=" + g.name() + ", evt=" + evt + ']');

                        return true;
                    }
                }, EVTS_CACHE_PRELOAD);

            list.add(g);
        }
    }

    /** @param grids Grids to stop. */
    private void stopGrids(Iterable<Grid> grids) {
        for (Grid g : grids)
            stopGrid(g.name());
    }

    /**
     * @param c Cache.
     * @param cnt Key count.
     * @throws GridException If failed.
     */
    private void putKeys(GridCache<Integer, String> c, int cnt) throws GridException {
        for (int i = 0; i < cnt; i++)
            c.put(i, Integer.toString(i));
    }
}
