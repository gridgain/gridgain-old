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

package org.gridgain.grid.kernal.processors.cache.distributed;

import org.gridgain.grid.cache.*;
import org.gridgain.grid.cache.affinity.consistenthash.*;

import static org.gridgain.grid.cache.GridCacheAtomicityMode.*;
import static org.gridgain.grid.cache.GridCacheMemoryMode.*;
import static org.gridgain.grid.cache.GridCacheMode.*;
import static org.gridgain.grid.cache.GridCacheWriteSynchronizationMode.*;

/**
 * Tests for byte array values in PARTITIONED caches.
 */
public abstract class GridCacheAbstractPartitionedByteArrayValuesSelfTest extends
    GridCacheAbstractDistributedByteArrayValuesSelfTest {
    /** {@inheritDoc} */
    @Override protected GridCacheConfiguration cacheConfiguration0() {
        GridCacheConfiguration cfg = new GridCacheConfiguration();

        cfg.setCacheMode(PARTITIONED);
        cfg.setAtomicityMode(TRANSACTIONAL);
        cfg.setDistributionMode(distributionMode());
        cfg.setBackups(1);
        cfg.setWriteSynchronizationMode(FULL_SYNC);
        cfg.setTxSerializableEnabled(true);
        cfg.setSwapEnabled(true);
        cfg.setEvictSynchronized(false);
        cfg.setEvictNearSynchronized(false);
        cfg.setPortableEnabled(portableEnabled());

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected GridCacheConfiguration offheapCacheConfiguration0() {
        GridCacheConfiguration cfg = new GridCacheConfiguration();

        cfg.setCacheMode(PARTITIONED);
        cfg.setAtomicityMode(TRANSACTIONAL);
        cfg.setDistributionMode(distributionMode());
        cfg.setBackups(1);
        cfg.setWriteSynchronizationMode(FULL_SYNC);
        cfg.setTxSerializableEnabled(true);
        cfg.setMemoryMode(OFFHEAP_VALUES);
        cfg.setOffHeapMaxMemory(100 * 1024 * 1024);
        cfg.setQueryIndexEnabled(false);
        cfg.setPortableEnabled(portableEnabled());

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected GridCacheConfiguration offheapTieredCacheConfiguration0() {
        GridCacheConfiguration cfg = new GridCacheConfiguration();

        cfg.setCacheMode(PARTITIONED);
        cfg.setAtomicityMode(TRANSACTIONAL);
        cfg.setDistributionMode(distributionMode());
        cfg.setBackups(1);
        cfg.setWriteSynchronizationMode(FULL_SYNC);
        cfg.setTxSerializableEnabled(true);
        cfg.setMemoryMode(OFFHEAP_TIERED);
        cfg.setOffHeapMaxMemory(100 * 1024 * 1024);
        cfg.setQueryIndexEnabled(false);
        cfg.setPortableEnabled(portableEnabled());

        return cfg;
    }

    /**
     * @return Distribution mode.
     */
    protected abstract GridCacheDistributionMode distributionMode();
}
