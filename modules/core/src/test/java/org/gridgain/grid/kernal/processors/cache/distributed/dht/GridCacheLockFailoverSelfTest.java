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
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.kernal.processors.cache.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.testframework.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static org.gridgain.grid.cache.GridCacheDistributionMode.*;

/**
 *
 */
public class GridCacheLockFailoverSelfTest extends GridCacheAbstractSelfTest {
    /** {@inheritDoc} */
    @Override protected int gridCount() {
        return 2;
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        super.afterTest();

        stopAllGrids();
    }

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        startGridsMultiThreaded(gridCount());
    }

    /** {@inheritDoc} */
    @Override protected GridCacheConfiguration cacheConfiguration(String gridName) throws Exception {
        GridCacheConfiguration ccfg = super.cacheConfiguration(gridName);

        ccfg.setDgcFrequency(0);

        return ccfg;
    }

    /** {@inheritDoc} */
    @Override protected GridCacheDistributionMode distributionMode() {
        return PARTITIONED_ONLY;
    }

    /** {@inheritDoc} */
    @Override protected long getTestTimeout() {
        return 2 * 60_000;
    }

    /**
     * @throws Exception If failed.
     */
    public void testLockFailover() throws Exception {
        GridCache<Integer, Integer> cache = grid(0).cache(null);

        Integer key = backupKey(cache);

        final AtomicBoolean stop = new AtomicBoolean();

        GridFuture<?> restartFut = GridTestUtils.runAsync(new Callable<Object>() {
            @Override public Object call() throws Exception {
                while (!stop.get()) {
                    stopGrid(1);

                    U.sleep(500);

                    startGrid(1);
                }
                return null;
            }
        });

        try {
            long end = System.currentTimeMillis() + 60_000;

            long iter = 0;

            while (System.currentTimeMillis() < end) {
                if (iter % 100 == 0)
                    log.info("Iteration: " + iter);

                iter++;

                GridFuture<Boolean> fut = null;

                try {
                    fut = cache.lockAsync(key, 0);

                    fut.get(30_000);

                    U.sleep(1);
                }
                catch (GridFutureTimeoutException e) {
                    GridCacheAdapter<Object, Object> ca = ((GridKernal) grid(0)).internalCache(null);

                    info("Entry: " + ca.peekEx(key));

                    fail("Lock timeout [fut=" + fut + ", err=" + e + ']');
                }
                catch (GridException e) {
                    log.error("Error: " + e);
                }
                finally {
                    cache.unlock(key);
                }
            }
        }
        finally {
            stop.set(true);

            restartFut.get();
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testUnlockPrimaryLeft() throws Exception {
        GridCache<Integer, Integer> cache = grid(0).cache(null);

        Integer key = backupKey(cache);

        cache.lock(key, 0);

        stopGrid(1);

        cache.unlock(key);

        GridCacheAdapter<Object, Object> ca = ((GridKernal) grid(0)).internalCache(null);

        GridCacheEntryEx<Object, Object> entry = ca.peekEx(key);

        assertTrue("Remote MVCC is not empty: " + entry, entry == null || entry.remoteMvccSnapshot().isEmpty());

        startGrid(1);
    }
}
