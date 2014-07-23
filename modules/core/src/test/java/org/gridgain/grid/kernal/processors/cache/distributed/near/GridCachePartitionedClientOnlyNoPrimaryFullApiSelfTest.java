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
import org.gridgain.grid.lang.*;
import org.gridgain.grid.util.typedef.*;

import java.util.*;

/**
 * Tests for local cache.
 */
public class GridCachePartitionedClientOnlyNoPrimaryFullApiSelfTest extends GridCachePartitionedFullApiSelfTest {
    /** {@inheritDoc} */
    @Override protected GridCacheDistributionMode distributionMode() {
        return GridCacheDistributionMode.CLIENT_ONLY;
    }

    /**
     *
     */
    public void testMapKeysToNodes() {
        cache().affinity().mapKeysToNodes(Arrays.asList("1", "2"));
    }

    /**
     *
     */
    public void testMapKeyToNode() {
        assert cache().affinity().mapKeyToNode("1") == null;
    }

    /** {@inheritDoc} */
    @Override public void testAffinityPerformance() throws Exception {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override public void testAffinityPerformanceMultithreaded() throws Exception {
        // No-op.
    }

    /**
     * @return Handler that discards grid exceptions.
     */
    @Override protected GridClosure<Throwable, Throwable> errorHandler() {
        return new GridClosure<Throwable, Throwable>() {
            @Override public Throwable apply(Throwable e) {
                if (e instanceof GridException || X.hasCause(e, GridTopologyException.class)) {
                    info("Discarding exception: " + e);

                    return null;
                }
                else
                    return e;
            }
        };
    }
}
