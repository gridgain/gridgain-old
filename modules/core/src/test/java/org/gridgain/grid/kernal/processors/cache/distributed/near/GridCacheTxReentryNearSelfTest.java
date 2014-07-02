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

import java.util.*;

import static org.gridgain.grid.cache.GridCacheMode.*;

/**
 *
 */
public class GridCacheTxReentryNearSelfTest extends GridCacheTxReentryAbstractSelfTest {
    /** {@inheritDoc} */
    @Override protected GridCacheMode cacheMode() {
        return PARTITIONED;
    }

    /** {@inheritDoc} */
    @Override protected int gridCount() {
        return 3;
    }

    /** {@inheritDoc} */
    @Override protected int testKey() {
        int key = 0;

        GridCache<Object, Object> cache = grid(0).cache(null);

        while (true) {
            Collection<GridNode> nodes = cache.affinity().mapKeyToPrimaryAndBackups(key);

            if (nodes.contains(grid(0).localNode()))
                key++;
            else
                break;
        }

        return key;
    }

    /** {@inheritDoc} */
    @Override protected int expectedNearLockRequests() {
        return 1;
    }

    /** {@inheritDoc} */
    @Override protected int expectedDhtLockRequests() {
        return 0;
    }

    /** {@inheritDoc} */
    @Override protected int expectedDistributedLockRequests() {
        return 1;
    }

    /** {@inheritDoc} */
    @Override protected boolean nearEnabled() {
        return true;
    }
}
