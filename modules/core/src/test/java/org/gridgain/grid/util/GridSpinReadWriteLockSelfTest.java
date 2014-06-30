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

package org.gridgain.grid.util;

import org.gridgain.grid.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.testframework.junits.common.*;

import java.util.concurrent.*;

/**
 *
 */
public class GridSpinReadWriteLockSelfTest extends GridCommonAbstractTest {
    /** Constructor. */
    public GridSpinReadWriteLockSelfTest() {
        super(false);
    }

    /**
     * @throws Exception If any error occurs.
     */
    public void testWriteLockReentry() throws Exception {
        GridSpinReadWriteLock lock = new GridSpinReadWriteLock();

        lock.writeLock();

        lock.writeLock();

        boolean b = lock.tryWriteLock();

        assert b;
    }

    /**
     * @throws Exception If any error occurs.
     */
    public void testReadLockReentry() throws Exception {
        final GridSpinReadWriteLock lock = new GridSpinReadWriteLock();

        lock.readLock();

        final CountDownLatch latch = new CountDownLatch(1);

        GridFuture<?> f = multithreadedAsync(
            new Callable<Object>() {
                @Override public Object call() throws Exception {
                    assert !lock.tryWriteLock();

                    info("Before write lock.");

                    latch.countDown();

                    lock.writeLock();

                    info("After write lock.");

                    return null;
                }
            }, 1);

        latch.await();

        U.sleep(100);

        lock.readLock();

        assert lock.tryReadLock();

        lock.readUnlock();
        lock.readUnlock();
        lock.readUnlock();

        f.get();
    }

    /**
     * @throws Exception If any error occurs.
     */
    public void testLockDowngrade() throws Exception {
        GridSpinReadWriteLock lock = new GridSpinReadWriteLock();

        // Read lock while holding write lock.
        lock.writeLock();

        lock.readLock();

        lock.readUnlock();

        lock.writeUnlock();

        // Downgrade from write to read lock.
        lock.writeLock();

        lock.readLock();

        lock.writeUnlock();

        assert !lock.tryWriteLock();

        lock.readUnlock();

        // Test that we can operate with write locks now.
        lock.writeLock();
        lock.writeUnlock();
    }

    /**
     * @throws Exception If any error occurs.
     */
    public void testMonitorState() throws Exception {
        GridSpinReadWriteLock lock = new GridSpinReadWriteLock();

        try {
            lock.readUnlock();
        }
        catch (IllegalMonitorStateException e) {
            info("Caught expected exception: " + e);
        }

        try {
            lock.writeUnlock();
        }
        catch (IllegalMonitorStateException e) {
            info("Caught expected exception: " + e);
        }
    }
}
