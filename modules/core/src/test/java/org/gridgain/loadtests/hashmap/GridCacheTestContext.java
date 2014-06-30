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

package org.gridgain.loadtests.hashmap;

import org.gridgain.grid.kernal.processors.cache.*;
import org.gridgain.grid.kernal.processors.cache.datastructures.*;
import org.gridgain.grid.kernal.processors.cache.dr.os.*;
import org.gridgain.grid.kernal.processors.cache.jta.*;
import org.gridgain.grid.kernal.processors.cache.query.*;
import org.gridgain.grid.kernal.processors.cache.query.continuous.*;
import org.gridgain.testframework.junits.*;
import org.gridgain.testframework.junits.logger.*;

import static org.gridgain.testframework.junits.GridAbstractTest.*;

/**
 * Cache test context.
 */
public class GridCacheTestContext<K, V> extends GridCacheContext<K, V> {
    /**
     */
    @SuppressWarnings("NullableProblems")
    public GridCacheTestContext() {
        super(
            new GridTestKernalContext(new GridTestLog4jLogger()),
            defaultCacheConfiguration(),
            new GridCacheMvccManager<K, V>(),
            new GridCacheVersionManager<K, V>(),
            new GridCacheEventManager<K, V>(),
            new GridCacheSwapManager<K, V>(false),
            new GridCacheStoreManager<K, V>(null),
            new GridCacheDeploymentManager<K, V>(),
            new GridCacheEvictionManager<K, V>(),
            new GridCacheIoManager<K, V>(),
            new GridCacheLocalQueryManager<K, V>(),
            new GridCacheContinuousQueryManager<K, V>(),
            new GridCacheDgcManager<K, V>(),
            new GridCacheAffinityManager<K, V>(),
            new GridCacheTxManager<K, V>(),
            new GridCacheDataStructuresManager<K, V>(),
            new GridCacheTtlManager<K, V>(),
            new GridOsCacheDrManager<K, V>(),
            new GridCacheNoopJtaManager<K, V>());
    }
}
