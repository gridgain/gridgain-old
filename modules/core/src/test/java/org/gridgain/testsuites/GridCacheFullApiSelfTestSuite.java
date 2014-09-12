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

package org.gridgain.testsuites;

import junit.framework.*;
import org.gridgain.grid.kernal.processors.cache.distributed.dht.*;
import org.gridgain.grid.kernal.processors.cache.distributed.near.*;
import org.gridgain.grid.kernal.processors.cache.distributed.replicated.*;
import org.gridgain.grid.kernal.processors.cache.local.*;
import org.gridgain.grid.kernal.processors.dr.cache.*;

/**
 * Test suite for cache API.
 */
public class GridCacheFullApiSelfTestSuite extends TestSuite {
    /**
     * @return Cache API test suite.
     * @throws Exception If failed.
     */
    public static TestSuite suite() throws Exception {
        TestSuite suite = new TestSuite("Gridgain Cache Full API Test Suite");

        // One node.
        suite.addTestSuite(GridCacheLocalFullApiSelfTest.class);
        suite.addTestSuite(GridCacheLocalAtomicFullApiSelfTest.class);
        suite.addTestSuite(GridCacheReplicatedFullApiSelfTest.class);
        suite.addTestSuite(GridCachePartitionedFullApiSelfTest.class);
        suite.addTestSuite(GridCacheAtomicFullApiSelfTest.class);
        suite.addTestSuite(GridCacheAtomicPrimaryWriteOrderFullApiSelfTest.class);
        suite.addTestSuite(GridCachePartitionedNearDisabledFullApiSelfTest.class);
        suite.addTestSuite(GridCachePartitionedFilteredPutSelfTest.class);
        suite.addTestSuite(GridCacheReplicatedAtomicFullApiSelfTest.class);
        suite.addTestSuite(GridCacheAtomicNearEnabledFullApiSelfTest.class);
        suite.addTestSuite(GridCacheAtomicNearEnabledPrimaryWriteOrderFullApiSelfTest.class);

        // No primary.
        suite.addTestSuite(GridCachePartitionedClientOnlyNoPrimaryFullApiSelfTest.class);
        suite.addTestSuite(GridCachePartitionedNearOnlyNoPrimaryFullApiSelfTest.class);

        // One node with off-heap values.
        suite.addTestSuite(GridCacheLocalOffHeapFullApiSelfTest.class);
        suite.addTestSuite(GridCacheLocalAtomicOffHeapFullApiSelfTest.class);
        suite.addTestSuite(GridCacheReplicatedOffHeapFullApiSelfTest.class);
        suite.addTestSuite(GridCachePartitionedOffHeapFullApiSelfTest.class);
        suite.addTestSuite(GridCacheAtomicOffHeapFullApiSelfTest.class);
        suite.addTestSuite(GridCacheAtomicPrimaryWriteOrderOffHeapFullApiSelfTest.class);
        suite.addTestSuite(GridCachePartitionedNearDisabledOffHeapFullApiSelfTest.class);

        // Multi-node.
        suite.addTestSuite(GridCacheReplicatedMultiNodeFullApiSelfTest.class);
        suite.addTestSuite(GridCacheReplicatedMultiNodeP2PDisabledFullApiSelfTest.class);
        suite.addTestSuite(GridCacheReplicatedAtomicMultiNodeFullApiSelfTest.class);
        suite.addTestSuite(GridCacheReplicatedAtomicPrimaryWriteOrderMultiNodeFullApiSelfTest.class);
        suite.addTestSuite(GridCacheReplicatedRendezvousAffinityMultiNodeFullApiSelfTest.class);

        suite.addTestSuite(GridCachePartitionedMultiNodeFullApiSelfTest.class);
        suite.addTestSuite(GridCacheAtomicMultiNodeFullApiSelfTest.class);
        suite.addTestSuite(GridCacheAtomicPrimaryWriteOrderMultiNodeFullApiSelfTest.class);
        suite.addTestSuite(GridCachePartitionedMultiNodeP2PDisabledFullApiSelfTest.class);
        suite.addTestSuite(GridCacheAtomicMultiNodeP2PDisabledFullApiSelfTest.class);
        suite.addTestSuite(GridCacheAtomicPrimaryWriteOrderMultiNodeP2PDisabledFullApiSelfTest.class);
        suite.addTestSuite(GridCacheAtomicNearEnabledMultiNodeFullApiSelfTest.class);
        suite.addTestSuite(GridCacheAtomicNearEnabledPrimaryWriteOrderMultiNodeFullApiSelfTest.class);

        suite.addTestSuite(GridCachePartitionedNearDisabledMultiNodeFullApiSelfTest.class);
        suite.addTestSuite(GridCachePartitionedNearDisabledMultiNodeP2PDisabledFullApiSelfTest.class);

        suite.addTestSuite(GridCacheNearOnlyMultiNodeFullApiSelfTest.class);
        suite.addTestSuite(GridCacheNearOnlyMultiNodeP2PDisabledFullApiSelfTest.class);
        suite.addTestSuite(GridCacheReplicatedNearOnlyMultiNodeFullApiSelfTest.class);

        suite.addTestSuite(GridCacheAtomicClientOnlyMultiNodeFullApiSelfTest.class);
        suite.addTestSuite(GridCacheAtomicClientOnlyMultiNodeP2PDisabledFullApiSelfTest.class);

        suite.addTestSuite(GridCacheAtomicNearOnlyMultiNodeFullApiSelfTest.class);
        suite.addTestSuite(GridCacheAtomicNearOnlyMultiNodeP2PDisabledFullApiSelfTest.class);

        suite.addTestSuite(GridCacheNearReloadAllSelfTest.class);
        suite.addTestSuite(GridCacheColocatedReloadAllSelfTest.class);
        suite.addTestSuite(GridCacheAtomicReloadAllSelfTest.class);
        suite.addTestSuite(GridCacheAtomicPrimaryWriteOrderReloadAllSelfTest.class);
        suite.addTestSuite(GridCacheNearTxMultiNodeSelfTest.class);
        suite.addTestSuite(GridCachePartitionedMultiNodeCounterSelfTest.class);

        // Multi-node with off-heap values.
        suite.addTestSuite(GridCacheReplicatedOffHeapMultiNodeFullApiSelfTest.class);
        suite.addTestSuite(GridCachePartitionedOffHeapMultiNodeFullApiSelfTest.class);
        suite.addTestSuite(GridCacheAtomicOffHeapMultiNodeFullApiSelfTest.class);
        suite.addTestSuite(GridCacheAtomicPrimaryWrityOrderOffHeapMultiNodeFullApiSelfTest.class);
        suite.addTestSuite(GridCachePartitionedNearDisabledOffHeapMultiNodeFullApiSelfTest.class);

        // Private cache API.
        suite.addTestSuite(GridCacheExLocalFullApiSelfTest.class);
        suite.addTestSuite(GridCacheExReplicatedFullApiSelfTest.class);
        suite.addTestSuite(GridCacheExNearFullApiSelfTest.class);
        suite.addTestSuite(GridCacheExColocatedFullApiSelfTest.class);

        // Multithreaded // TODO: GG-708
//        suite.addTestSuite(GridCacheLocalFullApiMultithreadedSelfTest.class);
//        suite.addTestSuite(GridCacheReplicatedFullApiMultithreadedSelfTest.class);
//        suite.addTestSuite(GridCachePartitionedFullApiMultithreadedSelfTest.class);

        // Cache API tests for cache configured as DR target.
        // Multi node.
        suite.addTest(new TestSuite(GridDrCachePartitionedNearDisabledMultiNodeFullApiSelfTest.class));
        suite.addTest(new TestSuite(GridDrCachePartitionedAtomicMultiNodeFullApiSelfTest.class));
        suite.addTest(new TestSuite(GridDrCachePartitionedAtomicPrimaryWriteOrderMultiNodeFullApiSelfTest.class));

        // Single node.
        suite.addTest(new TestSuite(GridDrCachePartitionedFullApiSelfTest.class));
        suite.addTest(new TestSuite(GridDrCachePartitionedNearDisabledFullApiSelfTest.class));
        suite.addTest(new TestSuite(GridDrCachePartitionedAtomicFullApiSelfTest.class));
        suite.addTest(new TestSuite(GridDrCachePartitionedAtomicPrimaryWriteOrderFullApiSelfTest.class));

        return suite;
    }
}
