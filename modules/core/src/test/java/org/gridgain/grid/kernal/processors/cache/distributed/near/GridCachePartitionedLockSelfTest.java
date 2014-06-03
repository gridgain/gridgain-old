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

package org.gridgain.grid.kernal.processors.cache.distributed.near;

import org.apache.log4j.*;
import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.kernal.processors.cache.*;
import org.gridgain.grid.kernal.processors.cache.distributed.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.testframework.*;

import java.util.*;
import java.util.concurrent.*;

import static org.gridgain.grid.cache.GridCacheMode.*;

/**
 * Test cases for multi-threaded tests.
 */
public class GridCachePartitionedLockSelfTest extends GridCacheLockAbstractTest {
    /** */
    private static final boolean CACHE_DEBUG = false;

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        if (CACHE_DEBUG)
            resetLog4j(Level.DEBUG, true, GridCacheProcessor.class.getPackage().getName());

        return super.getConfiguration(gridName);
    }

    /** {@inheritDoc} */
    @Override protected GridCacheMode cacheMode() {
        return PARTITIONED;
    }

    /** {@inheritDoc} */
    @Override protected boolean isPartitioned() {
        return true;
    }

    /**
     * @throws GridException If failed.
     */
    public void testLockAtomicCache() throws GridException {
        GridConfiguration cfg = new GridConfiguration();

        cfg.setGridName(getTestGridName(0));
        cfg.setRestEnabled(false);
        cfg.setCacheConfiguration(new GridCacheConfiguration());

        final Grid g0 = G.start(cfg);

        GridTestUtils.assertThrows(log, new Callable<Object>() {
            @Override public Object call() throws Exception {
                return g0.cache(null).lock(1, Long.MAX_VALUE);
            }
        }, GridException.class, "Locks are not supported");

        GridTestUtils.assertThrows(log, new Callable<Object>() {
            @Override public Object call() throws Exception {
                return g0.cache(null).lockAll(Arrays.asList(1), Long.MAX_VALUE);
            }
        }, GridException.class, "Locks are not supported");

        final GridFuture<Boolean> lockFut1 = g0.cache(null).lockAsync(1, Long.MAX_VALUE);

        GridTestUtils.assertThrows(log, new Callable<Object>() {
            @Override public Object call() throws Exception {
                return lockFut1.get();
            }
        }, GridException.class, "Locks are not supported");

        final GridFuture<Boolean> lockFut2 = g0.cache(null).lockAllAsync(Arrays.asList(1), Long.MAX_VALUE);

        GridTestUtils.assertThrows(log, new Callable<Object>() {
            @Override public Object call() throws Exception {
                return lockFut2.get();
            }
        }, GridException.class, "Locks are not supported");

    }
}
