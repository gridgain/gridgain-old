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

package org.gridgain.grid.kernal.processors.cache.datastructures;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.datastructures.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.testframework.junits.common.*;

import java.util.*;

/**
 * AtomicReference and AtomicStamped multi node tests.
 */
public abstract class GridCacheAtomicReferenceMultiNodeAbstractTest extends GridCommonAbstractTest {
    /** */
    protected static final int GRID_CNT = 4;

    /** */
    protected static GridTcpDiscoveryIpFinder ipFinder = new GridTcpDiscoveryVmIpFinder(true);

    /**
     * Constructs test.
     */
    protected GridCacheAtomicReferenceMultiNodeAbstractTest() {
        super(/* don't start grid */ false);
    }

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        super.beforeTestsStarted();

        for (int i = 0; i < GRID_CNT; i++)
            startGrid(i);

        assert G.allGrids().size() == GRID_CNT;
    }

    /** {@inheritDoc} */
    @Override protected void afterTestsStopped() throws Exception {
        super.afterTestsStopped();

        stopAllGrids();
    }

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        GridTcpDiscoverySpi spi = new GridTcpDiscoverySpi();

        spi.setIpFinder(ipFinder);

        cfg.setDiscoverySpi(spi);

        return cfg;
    }

    /**
     * JUnit.
     *
     * @throws Exception If failed.
     */
    public void testAtomicReference() throws Exception {
        // Get random name of reference.
        final String refName = UUID.randomUUID().toString();
        // Get random value of atomic reference.
        final String val = UUID.randomUUID().toString();
        // Get random new value of atomic reference.
        final String newVal = UUID.randomUUID().toString();

        // Initialize atomicReference in cache.
        GridCacheAtomicReference<String> ref = grid(0).cache(null).dataStructures().atomicReference(refName, val, true);

        final Grid grid = grid(0);

        // Execute task on all grid nodes.
        grid.compute().call(new GridCallable<Object>() {
            @Override public String call() throws GridException {
                GridCacheAtomicReference<String> ref = grid.cache(null).dataStructures().atomicReference(refName, val, true);

                assertEquals(val, ref.get());

                return ref.get();
            }
        });

        ref.compareAndSet("WRONG EXPECTED VALUE", newVal);

        // Execute task on all grid nodes.
        grid.compute().call(new GridCallable<String>() {
            @Override public String call() throws GridException {
                GridCacheAtomicReference<String> ref = grid.cache(null).dataStructures().atomicReference(refName, val, true);

                assertEquals(val, ref.get());

                return ref.get();
            }
        });

        ref.compareAndSet(val, newVal);

        // Execute task on all grid nodes.
        grid.compute().call(new GridCallable<String>() {
            @Override public String call() throws GridException {
                GridCacheAtomicReference<String> ref = grid.cache(null).dataStructures().atomicReference(refName, val, true);

                assertEquals(newVal, ref.get());

                return ref.get();
            }
        });
    }

    /**
     * JUnit.
     *
     * @throws Exception If failed.
     */
    public void testAtomicStamped() throws Exception {
        // Get random name of stamped.
        final String stampedName = UUID.randomUUID().toString();
        // Get random value of atomic stamped.
        final String val = UUID.randomUUID().toString();
        // Get random value of atomic stamped.
        final String stamp = UUID.randomUUID().toString();
        // Get random new value of atomic stamped.
        final String newVal = UUID.randomUUID().toString();
        // Get random new stamp of atomic stamped.
        final String newStamp = UUID.randomUUID().toString();

        // Initialize atomicStamped in cache.
        GridCacheAtomicStamped<String, String> stamped = grid(0).cache(null).dataStructures()
            .atomicStamped(stampedName, val, stamp, true);

        final Grid grid = grid(0);

        // Execute task on all grid nodes.
        grid.compute().call(new GridCallable<String>() {
            @Override public String call() throws GridException {
                GridCacheAtomicStamped<String, String> stamped = grid.cache(null).dataStructures()
                    .atomicStamped(stampedName, val, stamp, true);

                assertEquals(val, stamped.value());
                assertEquals(stamp, stamped.stamp());

                return stamped.value();
            }
        });

        stamped.compareAndSet("WRONG EXPECTED VALUE", newVal, "WRONG EXPECTED STAMP", newStamp);

        // Execute task on all grid nodes.
        grid.compute().call(new GridCallable<String>() {
            @Override public String call() throws GridException {
                GridCacheAtomicStamped<String, String> stamped = grid.cache(null).dataStructures()
                    .atomicStamped(stampedName, val, stamp, true);

                assertEquals(val, stamped.value());
                assertEquals(stamp, stamped.stamp());

                return stamped.value();
            }
        });

        stamped.compareAndSet(val, newVal, stamp, newStamp);

        // Execute task on all grid nodes.
        grid.compute().call(new GridCallable<String>() {
            @Override public String call() throws GridException {
                GridCacheAtomicStamped<String, String> stamped = grid.cache(null).dataStructures()
                    .atomicStamped(stampedName, val, stamp, true);

                assertEquals(newVal, stamped.value());
                assertEquals(newStamp, stamped.stamp());

                return stamped.value();
            }
        });
    }
}
