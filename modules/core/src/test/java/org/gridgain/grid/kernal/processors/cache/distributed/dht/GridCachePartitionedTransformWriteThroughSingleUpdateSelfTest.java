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

package org.gridgain.grid.kernal.processors.cache.distributed.dht;

import org.gridgain.grid.*;
import org.gridgain.testframework.*;

import java.util.concurrent.*;

import static org.gridgain.grid.cache.GridCacheTxConcurrency.OPTIMISTIC;

/**
 * Test for transform put with single update enabled.
 */
public class GridCachePartitionedTransformWriteThroughSingleUpdateSelfTest
    extends GridCacheAbstractTransformWriteThroughSelfTest {
    /** {@inheritDoc} */
    @Override protected boolean batchUpdate() {
        return false;
    }

    /** {@inheritDoc} */
    @Override public void testTransformOptimisticNearUpdate() throws Exception {
        GridTestUtils.assertThrows(log, new Callable<Object>() {
            @Override public Object call() throws Exception {
                checkTransform(OPTIMISTIC, NEAR_NODE, OP_UPDATE);

                return null;
            }
        }, GridException.class, null);

    }

    /** {@inheritDoc} */
    @Override public void testTransformOptimisticPrimaryUpdate() throws Exception {
        GridTestUtils.assertThrows(log, new Callable<Object>() {
            @Override public Object call() throws Exception {
                checkTransform(OPTIMISTIC, PRIMARY_NODE, OP_UPDATE);

                return null;
            }
        }, GridException.class, null);
    }

    /** {@inheritDoc} */
    @Override public void testTransformOptimisticBackupUpdate() throws Exception {
        GridTestUtils.assertThrows(log, new Callable<Object>() {
            @Override public Object call() throws Exception {
                checkTransform(OPTIMISTIC, BACKUP_NODE, OP_UPDATE);

                return null;
            }
        }, GridException.class, null);

    }

    /** {@inheritDoc} */
    @Override public void testTransformOptimisticNearDelete() throws Exception {
        GridTestUtils.assertThrows(log, new Callable<Object>() {
            @Override public Object call() throws Exception {
                checkTransform(OPTIMISTIC, NEAR_NODE, OP_DELETE);

                return null;
            }
        }, GridException.class, null);

    }

    /** {@inheritDoc} */
    @Override public void testTransformOptimisticPrimaryDelete() throws Exception {
        GridTestUtils.assertThrows(log, new Callable<Object>() {
            @Override public Object call() throws Exception {
                checkTransform(OPTIMISTIC, PRIMARY_NODE, OP_DELETE);

                return null;
            }
        },GridException.class, null);

    }

    /** {@inheritDoc} */
    @Override public void testTransformOptimisticBackupDelete() throws Exception {
        GridTestUtils.assertThrows(log, new Callable<Object>() {
            @Override public Object call() throws Exception {
                checkTransform(OPTIMISTIC, BACKUP_NODE, OP_DELETE);

                return null;
            }
        }, GridException.class, null);
    }
}
