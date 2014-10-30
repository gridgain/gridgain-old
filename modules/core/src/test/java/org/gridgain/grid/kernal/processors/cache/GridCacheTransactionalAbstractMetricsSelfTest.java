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

import static org.gridgain.grid.cache.GridCacheTxConcurrency.*;
import static org.gridgain.grid.cache.GridCacheTxIsolation.*;

/**
 * Transactional cache metrics test.
 */
public abstract class GridCacheTransactionalAbstractMetricsSelfTest extends GridCacheAbstractMetricsSelfTest {
    /** */
    private static final int TX_CNT = 3;

    /**
     * @throws Exception If failed.
     */
    public void testOptimisticReadCommittedCommits() throws Exception {
        testCommits(OPTIMISTIC, READ_COMMITTED, true);
    }

    /**
     * @throws Exception If failed.
     */
    public void testOptimisticReadCommittedCommitsNoData() throws Exception {
        testCommits(OPTIMISTIC, READ_COMMITTED, false);
    }

    /**
     * @throws Exception If failed.
     */
    public void testOptimisticRepeatableReadCommits() throws Exception {
        testCommits(OPTIMISTIC, REPEATABLE_READ, true);
    }

    /**
     * @throws Exception If failed.
     */
    public void testOptimisticRepeatableReadCommitsNoData() throws Exception {
        testCommits(OPTIMISTIC, REPEATABLE_READ, false);
    }

    /**
     * @throws Exception If failed.
     */
    public void testOptimisticSerializableCommits() throws Exception {
        testCommits(OPTIMISTIC, SERIALIZABLE, true);
    }

    /**
     * @throws Exception If failed.
     */
    public void testOptimisticSerializableCommitsNoData() throws Exception {
        testCommits(OPTIMISTIC, SERIALIZABLE, false);
    }

    /**
     * @throws Exception If failed.
     */
    public void testPessimisticReadCommittedCommits() throws Exception {
        testCommits(PESSIMISTIC, READ_COMMITTED, true);
    }

    /**
     * @throws Exception If failed.
     */
    public void testPessimisticReadCommittedCommitsNoData() throws Exception {
        testCommits(PESSIMISTIC, READ_COMMITTED, false);
    }

    /**
     * @throws Exception If failed.
     */
    public void testPessimisticRepeatableReadCommits() throws Exception {
        testCommits(PESSIMISTIC, REPEATABLE_READ, true);
    }

    /**
     * @throws Exception If failed.
     */
    public void testPessimisticRepeatableReadCommitsNoData() throws Exception {
        testCommits(PESSIMISTIC, REPEATABLE_READ, false);
    }

    /**
     * @throws Exception If failed.
     */
    public void testPessimisticSerializableCommits() throws Exception {
        testCommits(PESSIMISTIC, SERIALIZABLE, true);
    }

    /**
     * @throws Exception If failed.
     */
    public void testPessimisticSerializableCommitsNoData() throws Exception {
        testCommits(PESSIMISTIC, SERIALIZABLE, false);
    }

    /**
     * @throws Exception If failed.
     */
    public void testOptimisticReadCommittedRollbacks() throws Exception {
        testRollbacks(OPTIMISTIC, READ_COMMITTED, true);
    }

    /**
     * @throws Exception If failed.
     */
    public void testOptimisticReadCommittedRollbacksNoData() throws Exception {
        testRollbacks(OPTIMISTIC, READ_COMMITTED, false);
    }

    /**
     * @throws Exception If failed.
     */
    public void testOptimisticRepeatableReadRollbacks() throws Exception {
        testRollbacks(OPTIMISTIC, REPEATABLE_READ, true);
    }

    /**
     * @throws Exception If failed.
     */
    public void testOptimisticRepeatableReadRollbacksNoData() throws Exception {
        testRollbacks(OPTIMISTIC, REPEATABLE_READ, false);
    }

    /**
     * @throws Exception If failed.
     */
    public void testOptimisticSerializableRollbacks() throws Exception {
        testRollbacks(OPTIMISTIC, SERIALIZABLE, true);
    }

    /**
     * @throws Exception If failed.
     */
    public void testOptimisticSerializableRollbacksNoData() throws Exception {
        testRollbacks(OPTIMISTIC, SERIALIZABLE, false);
    }

    /**
     * @throws Exception If failed.
     */
    public void testPessimisticReadCommittedRollbacks() throws Exception {
        testRollbacks(PESSIMISTIC, READ_COMMITTED, true);
    }

    /**
     * @throws Exception If failed.
     */
    public void testPessimisticReadCommittedRollbacksNoData() throws Exception {
        testRollbacks(PESSIMISTIC, READ_COMMITTED, false);
    }

    /**
     * @throws Exception If failed.
     */
    public void testPessimisticRepeatableReadRollbacks() throws Exception {
        testRollbacks(PESSIMISTIC, REPEATABLE_READ, true);
    }

    /**
     * @throws Exception If failed.
     */
    public void testPessimisticRepeatableReadRollbacksNoData() throws Exception {
        testRollbacks(PESSIMISTIC, REPEATABLE_READ, false);
    }

    /**
     * @throws Exception If failed.
     */
    public void testPessimisticSerializableRollbacks() throws Exception {
        testRollbacks(PESSIMISTIC, SERIALIZABLE, true);
    }

    /**
     * @throws Exception If failed.
     */
    public void testPessimisticSerializableRollbacksNoData() throws Exception {
        testRollbacks(PESSIMISTIC, SERIALIZABLE, false);
    }

    /**
     * @param concurrency Concurrency control.
     * @param isolation Isolation level.
     * @param put Put some data if {@code true}.
     * @throws Exception If failed.
     */
    private void testCommits(GridCacheTxConcurrency concurrency, GridCacheTxIsolation isolation, boolean put)
        throws Exception {
        GridCache<Integer, Integer> cache = grid(0).cache(null);

        for (int i = 0; i < TX_CNT; i++) {
            GridCacheTx tx = cache.txStart(concurrency, isolation);

            if (put)
                for (int j = 0; j < keyCount(); j++)
                    cache.put(j, j);

            tx.commit();
        }

        for (int i = 0; i < gridCount(); i++) {
            GridCacheMetrics metrics = grid(i).cache(null).metrics();

            if (i == 0)
                assertEquals(TX_CNT, metrics.txCommits());
            else
                assertEquals(0, metrics.txCommits());

            assertEquals(0, metrics.txRollbacks());
        }
    }

    /**
     * @param concurrency Concurrency control.
     * @param isolation Isolation level.
     * @param put Put some data if {@code true}.
     * @throws Exception If failed.
     */
    private void testRollbacks(GridCacheTxConcurrency concurrency, GridCacheTxIsolation isolation,
        boolean put) throws Exception {
        GridCache<Integer, Integer> cache = grid(0).cache(null);

        for (int i = 0; i < TX_CNT; i++) {
            GridCacheTx tx = cache.txStart(concurrency ,isolation);

            if (put)
                for (int j = 0; j < keyCount(); j++)
                    cache.put(j, j);

            tx.rollback();
        }

        for (int i = 0; i < gridCount(); i++) {
            GridCacheMetrics metrics = grid(i).cache(null).metrics();

            assertEquals(0, metrics.txCommits());

            if (i == 0)
                assertEquals(TX_CNT, metrics.txRollbacks());
            else
                assertEquals(0, metrics.txRollbacks());
        }
    }
}
