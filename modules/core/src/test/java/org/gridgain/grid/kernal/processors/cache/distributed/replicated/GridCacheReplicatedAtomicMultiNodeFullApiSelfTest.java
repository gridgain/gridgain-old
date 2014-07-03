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

import org.gridgain.grid.cache.*;

import static org.gridgain.grid.cache.GridCacheAtomicWriteOrderMode.*;
import static org.gridgain.grid.cache.GridCacheAtomicityMode.*;

/**
 * Multi-node tests for replicated atomic cache.
 */
public class GridCacheReplicatedAtomicMultiNodeFullApiSelfTest extends GridCacheReplicatedMultiNodeFullApiSelfTest {
    /**
     * @return Write order mode for atomic cache.
     */
    protected GridCacheAtomicWriteOrderMode atomicWriteOrderMode() {
        return CLOCK;
    }

    /** {@inheritDoc} */
    @Override protected GridCacheAtomicityMode atomicityMode() {
        return ATOMIC;
    }

    /** {@inheritDoc} */
    @Override protected boolean txEnabled() {
        return false;
    }

    /** {@inheritDoc} */
    @Override protected boolean lockingEnabled() {
        return false;
    }

    /** {@inheritDoc} */
    @Override protected GridCacheConfiguration cacheConfiguration(String gridName) throws Exception {
        GridCacheConfiguration ccfg = super.cacheConfiguration(gridName);

        ccfg.setAtomicWriteOrderMode(atomicWriteOrderMode());

        return ccfg;
    }
}
