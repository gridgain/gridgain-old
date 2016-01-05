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

package org.gridgain.grid.kernal.processors.cache;

import org.gridgain.grid.cache.*;
import org.gridgain.grid.kernal.*;
import org.jetbrains.annotations.*;

import static org.gridgain.grid.cache.GridCacheAtomicWriteOrderMode.*;
import static org.gridgain.grid.cache.GridCacheAtomicityMode.*;
import static org.gridgain.grid.cache.GridCacheDistributionMode.*;
import static org.gridgain.grid.cache.GridCacheMode.*;
import static org.gridgain.grid.cache.GridCacheTxConcurrency.*;
import static org.gridgain.grid.cache.GridCacheTxIsolation.*;

/**
 *
 */
public class GridCacheVersionMultinodeTest extends GridCacheAbstractSelfTest {
    /** */
    private GridCacheAtomicityMode atomicityMode;

    /** */
    private GridCacheAtomicWriteOrderMode atomicWriteOrder;

    /** {@inheritDoc} */
    @Override protected int gridCount() {
        return 3;
    }

    /** {@inheritDoc} */
    @Override protected GridCacheConfiguration cacheConfiguration(String gridName) throws Exception {
        GridCacheConfiguration ccfg = super.cacheConfiguration(gridName);

        assert atomicityMode != null;

        ccfg.setAtomicityMode(atomicityMode);

        if (atomicityMode == null) {
            assert atomicityMode != null;

            ccfg.setAtomicWriteOrderMode(atomicWriteOrder);
        }

        return ccfg;
    }

    /** {@inheritDoc} */
    @Override protected GridCacheMode cacheMode() {
        return PARTITIONED;
    }

    /** {@inheritDoc} */
    @Override protected GridCacheDistributionMode distributionMode() {
        return PARTITIONED_ONLY;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        stopAllGrids();
    }

    /**
     * @throws Exception If failed.
     */
    public void testVersionTx() throws Exception {
        atomicityMode = TRANSACTIONAL;

        checkVersion();
    }

    /**
     * @throws Exception If failed.
     */
    public void testVersionAtomicClock() throws Exception {
        atomicityMode = ATOMIC;

        atomicWriteOrder = CLOCK;

        checkVersion();
    }

    /**
     * @throws Exception If failed.
     */
    public void testVersionAtomicPrimary() throws Exception {
        atomicityMode = ATOMIC;

        atomicWriteOrder = PRIMARY;

        checkVersion();
    }

    /**
     * @throws Exception If failed.
     */
    private void checkVersion() throws Exception {
        super.beforeTestsStarted();

        for (int i = 0; i < 100; i++) {
            checkVersion(String.valueOf(i), null); // Create.

            checkVersion(String.valueOf(i), null); // Update.
        }

        if (atomicityMode == TRANSACTIONAL) {
            for (int i = 100; i < 200; i++) {
                checkVersion(String.valueOf(i), PESSIMISTIC); // Create.

                checkVersion(String.valueOf(i), PESSIMISTIC); // Update.
            }

            for (int i = 200; i < 300; i++) {
                checkVersion(String.valueOf(i), OPTIMISTIC); // Create.

                checkVersion(String.valueOf(i), OPTIMISTIC); // Update.
            }
        }
    }

    /**
     * @param key Key.
     * @param txMode Non null tx mode if explicit transaction should be started.
     * @throws Exception If failed.
     */
    private void checkVersion(String key, @Nullable GridCacheTxConcurrency txMode) throws Exception {
        GridCache<String, Integer> cache = cache();

        GridCacheTx tx = null;

        if (txMode != null)
            tx = cache.txStart(txMode, REPEATABLE_READ);

        try {
            cache.put(key, 1);

            if (tx != null)
                tx.commit();
        }
        finally {
            if (tx != null)
                tx.close();
        }

        checkEntryVersion(key);
    }

    /**
     * @param key Key.
     * @throws Exception If failed.
     */
    private void checkEntryVersion(String key) throws Exception {
        GridCacheVersion ver = null;

        boolean verified = false;

        for (int i = 0; i < gridCount(); i++) {
            GridKernal grid = (GridKernal)grid(i);

            GridCacheAdapter<Object, Object> cache = grid.context().cache().internalCache();

            if (cache.affinity().isPrimaryOrBackup(grid.localNode(), key)) {
                GridCacheEntryEx<Object, Object> e = cache.peekEx(key);

                assertNotNull(e);

                if (ver != null) {
                    assertEquals("Non-equal versions for key " + key, ver, e.version());

                    verified = true;
                }
                else
                    ver = e.version();
            }
        }

        assertTrue(verified);
    }
}
