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

package org.gridgain.grid.kernal.processors.cache.distributed.replicated;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.events.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.grid.spi.swapspace.file.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.testframework.*;
import org.gridgain.testframework.junits.common.*;

import java.io.*;
import java.util.concurrent.*;

import static java.util.concurrent.TimeUnit.*;
import static org.gridgain.grid.cache.GridCacheMode.*;
import static org.gridgain.grid.events.GridEventType.*;

/**
 * Advanced promote test for replicated cache.
 */
public class GridCacheReplicatedUnswapAdvancedSelfTest extends GridCommonAbstractTest {
    /** IP finder. */
    private static final GridTcpDiscoveryIpFinder IP_FINDER = new GridTcpDiscoveryVmIpFinder(true);

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        cfg.setPeerClassLoadingLocalClassPathExclude(GridCacheReplicatedUnswapAdvancedSelfTest.class.getName(),
            TestClass.class.getName());

        GridTcpDiscoverySpi discoSpi = new GridTcpDiscoverySpi();

        discoSpi.setIpFinder(IP_FINDER);

        cfg.setDiscoverySpi(discoSpi);

        GridCacheConfiguration cacheCfg = defaultCacheConfiguration();

        cacheCfg.setCacheMode(REPLICATED);
        cacheCfg.setSwapEnabled(true);

        cfg.setCacheConfiguration(cacheCfg);

        cfg.setSwapSpaceSpi(new GridFileSwapSpaceSpi());

        return cfg;
    }

    /**
     * @throws Exception If failed.
     */
    public void testUnswapAdvanced() throws Exception {
        Grid g1 = startGrid(1);
        Grid g2 = startGrid(2);

        assert g1.nodes().size() > 1 : "This test needs at least two grid nodes started.";

        GridCache<Object, Object> cache1 = g1.cache(null);
        GridCache<Object, Object> cache2 = g2.cache(null);

        try {
            ClassLoader ldr = new GridTestClassLoader(
                GridCacheReplicatedUnswapAdvancedSelfTest.class.getName(),
                TestClass.class.getName());

            Object v = ldr.loadClass(TestClass.class.getName()).newInstance();

            info("v loader: " + v.getClass().getClassLoader());

            final CountDownLatch putLatch = new CountDownLatch(1);

            g2.events().localListen(new GridPredicate<GridEvent>() {
                @Override public boolean apply(GridEvent evt) {
                    assert evt.type() == EVT_CACHE_OBJECT_PUT;

                    putLatch.countDown();

                    return true;
                }
            }, EVT_CACHE_OBJECT_PUT);

            String key = null;

            for (int i = 0; i < 1000; i++) {
                String k = "key-" + i;

                if (cache1.affinity().isPrimary(g1.localNode(), k)) {
                    key = k;

                    break;
                }
            }

            assertNotNull(key);

            // Put value into cache of the first grid.
            cache1.put(key, v);

            assert putLatch.await(10, SECONDS);

            assert cache2.containsKey(key);

            Object v2 = cache2.get(key);

            info("v2 loader: " + v2.getClass().getClassLoader());

            assert v2 != null;
            assert v2.toString().equals(v.toString());
            assert !v2.getClass().getClassLoader().equals(getClass().getClassLoader());
            assert v2.getClass().getClassLoader().getClass().getName().contains("GridDeploymentClassLoader");

            // To swap storage.
            cache2.evict(key);

            v2 = cache2.promote(key);

            log.info("Unswapped entry value: " + v2);

            assert v2 != null;

            assert v2.getClass().getClassLoader().getClass().getName().contains("GridDeploymentClassLoader");
        }
        finally {
            stopGrid(1);
            stopGrid(2);
        }
    }
    /**
     * Test class.
     */
    @SuppressWarnings("PublicInnerClass")
    public static class TestClass implements Serializable {
        /** String value. */
        private String s = "Test string";

        /**
         * @return String value.
         */
        public String getStr() {
            return s;
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(TestClass.class, this);
        }
    }
}
