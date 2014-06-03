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

package org.gridgain.grid.kernal;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.cache.affinity.consistenthash.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.marshaller.optimized.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.testframework.junits.common.*;

import java.util.*;
import java.util.concurrent.atomic.*;

import static org.gridgain.grid.cache.GridCacheMode.*;

/**
 * Tests for methods that run job locally with multiple arguments.
 */
public class GridProjectionLocalJobMultipleArgumentsSelfTest extends GridCommonAbstractTest {
    /** IP finder. */
    private static final GridTcpDiscoveryIpFinder IP_FINDER = new GridTcpDiscoveryVmIpFinder(true);

    /** */
    private static Collection<Integer> ids;

    /** */
    private static AtomicInteger res;

    /**
     * Starts grid.
     */
    public GridProjectionLocalJobMultipleArgumentsSelfTest() {
        super(true);
    }

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        cfg.setMarshaller(new GridOptimizedMarshaller(false));

        GridCacheConfiguration cache = defaultCacheConfiguration();

        cache.setCacheMode(PARTITIONED);
        cache.setBackups(1);

        cfg.setCacheConfiguration(cache);

        GridTcpDiscoverySpi disco = new GridTcpDiscoverySpi();

        disco.setIpFinder(IP_FINDER);

        cfg.setDiscoverySpi(disco);

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        ids = new GridConcurrentHashSet<>();
        res = new AtomicInteger();
    }

    /**
     * @throws Exception If failed.
     */
    public void testAffinityCall() throws Exception {
        Collection<Integer> res = new ArrayList<>();

        for (int i : F.asList(1, 2, 3)) {
            res.add(grid().compute().affinityCall(null, i, new GridCallable<Integer>() {
                @Override public Integer call() {
                    ids.add(System.identityHashCode(this));

                    return 10;
                }
            }).get());
        }

        assertEquals(30, F.sumInt(res));
        assertEquals(3, ids.size());
    }

    /**
     * @throws Exception If failed.
     */
    public void testAffinityRun() throws Exception {
        for (int i : F.asList(1, 2, 3)) {
            grid().compute().affinityRun(null, i, new GridRunnable() {
                @Override public void run() {
                    ids.add(System.identityHashCode(this));

                    res.addAndGet(10);
                }
            }).get();
        }

        assertEquals(30, res.get());
        assertEquals(3, ids.size());
    }

    /**
     * @throws Exception If failed.
     */
    public void testCall() throws Exception {
        Collection<Integer> res = grid().compute().apply(new C1<Integer, Integer>() {
            @Override public Integer apply(Integer arg) {

                ids.add(System.identityHashCode(this));

                return 10 + arg;
            }
        }, F.asList(1, 2, 3)).get();

        assertEquals(36, F.sumInt(res));
        assertEquals(3, ids.size());
    }

    /**
     * @throws Exception If failed.
     */
    public void testCallWithProducer() throws Exception {
        Collection<Integer> args = Arrays.asList(1, 2, 3);

        Collection<Integer> res = grid().compute().apply(new C1<Integer, Integer>() {
            @Override public Integer apply(Integer arg) {
                ids.add(System.identityHashCode(this));

                return 10 + arg;
            }
        }, args).get();

        assertEquals(36, F.sumInt(res));
        assertEquals(3, ids.size());
    }

    /**
     * @throws Exception If failed.
     */
    public void testReduce() throws Exception {
        int res = grid().compute().apply(new C1<Integer, Integer>() {
            @Override public Integer apply(Integer arg) {
                ids.add(System.identityHashCode(this));

                return 10 + arg;
            }
        }, F.asList(1, 2, 3), F.sumIntReducer()).get();

        assertEquals(36, res);
        assertEquals(3, ids.size());
    }

    /**
     * @throws Exception If failed.
     */
    public void testReduceWithProducer() throws Exception {
        Collection<Integer> args = Arrays.asList(1, 2, 3);

        int res = grid().compute().apply(new C1<Integer, Integer>() {
            @Override public Integer apply(Integer arg) {
                ids.add(System.identityHashCode(this));

                return 10 + arg;
            }
        }, args, F.sumIntReducer()).get();

        assertEquals(36, res);
        assertEquals(3, ids.size());
    }
}
