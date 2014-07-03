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

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.kernal.processors.cache.*;

import java.util.concurrent.*;

import static org.gridgain.grid.cache.GridCacheMode.*;

/**
 * Test for asynchronous cache entry lock with timeout.
 */
public class GridCachePartitionedEntryLockSelfTest extends GridCacheAbstractSelfTest {
    /** {@inheritDoc} */
    @Override protected int gridCount() {
        return 3;
    }

    /** {@inheritDoc} */
    @Override protected GridCacheMode cacheMode() {
        return PARTITIONED;
    }

    /**
     * @throws Exception If failed.
     */
    @SuppressWarnings("BusyWait")
    public void testLockAsyncWithTimeout() throws Exception {
        cache().put("key", 1);

        for (int i = 0; i < gridCount(); i++) {
            final GridCacheEntry<String, Integer> e = cache(i).entry("key");

            if (e.backup()) {
                assert !e.isLocked();

                e.lockAsync(2000).get();

                assert e.isLocked();



                GridFuture<Boolean> f = grid(i).forLocal().compute().call(new Callable<Boolean>() {
                    @Override public Boolean call() throws Exception {
                        GridFuture<Boolean> f = e.lockAsync(1000);

                        try {
                            f.get(100);

                            fail();
                        }
                        catch (GridFutureTimeoutException ex) {
                            info("Caught expected exception: " + ex);
                        }

                        try {
                            assert f.get();
                        }
                        finally {
                            e.unlock();
                        }

                        return true;
                    }
                });

                // Let another thread start.
                Thread.sleep(300);

                assert e.isLocked();
                assert e.isLockedByThread();

                cache().unlock("key");

                assert f.get();

                for (int j = 0; j < 100; j++)
                    if (cache().isLocked("key") || cache().isLockedByThread("key"))
                        Thread.sleep(10);
                    else
                        break;

                assert !cache().isLocked("key");
                assert !cache().isLockedByThread("key");

                break;
            }
        }
    }
}
