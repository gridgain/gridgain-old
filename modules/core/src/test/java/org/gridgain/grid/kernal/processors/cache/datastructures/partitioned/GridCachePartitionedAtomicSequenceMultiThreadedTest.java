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

package org.gridgain.grid.kernal.processors.cache.datastructures.partitioned;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.cache.affinity.consistenthash.*;
import org.gridgain.grid.cache.datastructures.*;
import org.gridgain.grid.kernal.processors.cache.datastructures.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.testframework.junits.common.*;

import java.util.*;

import static org.gridgain.grid.cache.GridCacheAtomicityMode.*;
import static org.gridgain.grid.cache.GridCacheMode.*;
import static org.gridgain.grid.cache.GridCacheDistributionMode.*;

/**
 * Cache partitioned multi-threaded tests.
 */
public class GridCachePartitionedAtomicSequenceMultiThreadedTest extends GridCommonAbstractTest {
    /** IP finder. */
    private static final GridTcpDiscoveryIpFinder IP_FINDER = new GridTcpDiscoveryVmIpFinder(true);

    /** Number of threads for multithreaded test. */
    private static final int THREAD_NUM = 30;

    /** Number of iterations per thread for multithreaded test. */
    private static final int ITERATION_NUM = 4000;

    /** Constructor. Starts grid. */
    public GridCachePartitionedAtomicSequenceMultiThreadedTest() {
        super(true /** Start grid. */);
    }

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        GridTcpDiscoverySpi disco = new GridTcpDiscoverySpi();

        disco.setIpFinder(IP_FINDER);

        cfg.setDiscoverySpi(disco);

        // Default cache configuration.
        GridCacheConfiguration dfltCacheCfg = defaultCacheConfiguration();

        dfltCacheCfg.setCacheMode(PARTITIONED);
        dfltCacheCfg.setBackups(1);
        dfltCacheCfg.setWriteSynchronizationMode(GridCacheWriteSynchronizationMode.FULL_SYNC);
        dfltCacheCfg.setAtomicSequenceReserveSize(10);
        dfltCacheCfg.setAtomicityMode(TRANSACTIONAL);
        dfltCacheCfg.setDistributionMode(NEAR_PARTITIONED);

        cfg.setCacheConfiguration(dfltCacheCfg);

        return cfg;
    }

    /** @throws Exception If failed. */
    public void testValues() throws Exception {
        String seqName = UUID.randomUUID().toString();

        final GridCacheAtomicSequenceImpl seq = (GridCacheAtomicSequenceImpl)grid().cache(null)
            .dataStructures().atomicSequence(seqName, 0, true);

        // Local reservations.
        assertEquals(1, seq.incrementAndGet());
        assertEquals(1, seq.getAndIncrement()); // Seq = 2
        assertEquals(3L, seq.incrementAndGet());
        assertEquals(3L, seq.getAndIncrement()); // Seq=4

        assertEquals(4, seq.getAndAdd(3));
        assertEquals(9, seq.addAndGet(2));

        assertEquals(9L, U.field(seq, "locVal"));
        assertEquals(9L, U.field(seq, "upBound"));

        // Cache calls.
        assertEquals(10, seq.incrementAndGet());

        assertEquals(10L, U.field(seq, "locVal"));
        assertEquals(19L, U.field(seq, "upBound"));

        seq.addAndGet(9);

        assertEquals(19L, U.field(seq, "locVal"));
        assertEquals(19L, U.field(seq, "upBound"));

        assertEquals(20L, seq.incrementAndGet());

        assertEquals(20L, U.field(seq, "locVal"));
        assertEquals(29L, U.field(seq, "upBound"));

        seq.addAndGet(9);

        assertEquals(29L, U.field(seq, "locVal"));
        assertEquals(29L, U.field(seq, "upBound"));

        assertEquals(29, seq.getAndIncrement());

        assertEquals(30L, U.field(seq, "locVal"));
        assertEquals(39L, U.field(seq, "upBound"));

        seq.addAndGet(9);

        assertEquals(39L, U.field(seq, "locVal"));
        assertEquals(39L, U.field(seq, "upBound"));

        assertEquals(39L, seq.getAndIncrement());

        assertEquals(40L, U.field(seq, "locVal"));
        assertEquals(49L, U.field(seq, "upBound"));

        seq.addAndGet(9);

        assertEquals(49L, U.field(seq, "locVal"));
        assertEquals(49L, U.field(seq, "upBound"));

        assertEquals(50, seq.addAndGet(1));

        assertEquals(50L, U.field(seq, "locVal"));
        assertEquals(59L, U.field(seq, "upBound"));

        seq.addAndGet(9);

        assertEquals(59L, U.field(seq, "locVal"));
        assertEquals(59L, U.field(seq, "upBound"));

        assertEquals(59, seq.getAndAdd(1));

        assertEquals(60L, U.field(seq, "locVal"));
        assertEquals(69L, U.field(seq, "upBound"));
    }

    /** @throws Exception If failed. */
    public void testUpdatedSync() throws Exception {
        checkUpdate(true);
    }

    /** @throws Exception If failed. */
    public void testPreviousSync() throws Exception {
        checkUpdate(false);
    }

    /** @throws Exception If failed. */
    public void testIncrementAndGet() throws Exception {
        // Random sequence names.
        String seqName = UUID.randomUUID().toString();

        final GridCacheAtomicSequence seq = grid().cache(null).dataStructures().atomicSequence(seqName, 0L, true);

        runSequenceClosure(new GridInUnsafeClosure<GridCacheAtomicSequence>() {
            @Override public void apply(GridCacheAtomicSequence t) throws GridException {
                t.incrementAndGet();
            }
        }, seq, ITERATION_NUM, THREAD_NUM);

        assertEquals(ITERATION_NUM * THREAD_NUM, seq.get());
    }

    /** @throws Exception If failed. */
    public void testIncrementAndGetAsync() throws Exception {
        // Random sequence names.
        String seqName = UUID.randomUUID().toString();

        final GridCacheAtomicSequence seq = grid().cache(null).dataStructures().atomicSequence(seqName, 0L, true);

        runSequenceClosure(new GridInUnsafeClosure<GridCacheAtomicSequence>() {
            @Override public void apply(GridCacheAtomicSequence t) throws GridException {
                t.incrementAndGet();
            }
        }, seq, ITERATION_NUM, THREAD_NUM);

        assertEquals(ITERATION_NUM * THREAD_NUM, seq.get());
    }

    /** @throws Exception If failed. */
    public void testGetAndIncrement() throws Exception {
        // Random sequence names.
        String seqName = UUID.randomUUID().toString();

        final GridCacheAtomicSequence seq = grid().cache(null).dataStructures().atomicSequence(seqName, 0L, true);

        runSequenceClosure(new GridInUnsafeClosure<GridCacheAtomicSequence>() {
            @Override public void apply(GridCacheAtomicSequence t) throws GridException {
                t.getAndIncrement();
            }
        }, seq, ITERATION_NUM, THREAD_NUM);

        assertEquals(ITERATION_NUM * THREAD_NUM, seq.get());
    }

    /** @throws Exception If failed. */
    public void testGetAndIncrementAsync() throws Exception {
        // Random sequence names.
        String seqName = UUID.randomUUID().toString();

        final GridCacheAtomicSequence seq = grid().cache(null).dataStructures().atomicSequence(seqName, 0L, true);

        runSequenceClosure(new GridInUnsafeClosure<GridCacheAtomicSequence>() {
            @Override public void apply(GridCacheAtomicSequence t) throws GridException {
                t.getAndIncrement();
            }
        }, seq, ITERATION_NUM, THREAD_NUM);

        assertEquals(ITERATION_NUM * THREAD_NUM, seq.get());
    }

    /** @throws Exception If failed. */
    public void testAddAndGet() throws Exception {
        // Random sequence names.
        String seqName = UUID.randomUUID().toString();

        final GridCacheAtomicSequence seq = grid().cache(null).dataStructures().atomicSequence(seqName, 0L, true);

        runSequenceClosure(new GridInUnsafeClosure<GridCacheAtomicSequence>() {
            @Override public void apply(GridCacheAtomicSequence t) throws GridException {
                t.addAndGet(5);
            }
        }, seq, ITERATION_NUM, THREAD_NUM);

        assertEquals(5 * ITERATION_NUM * THREAD_NUM, seq.get());
    }

    /** @throws Exception If failed. */
    public void testGetAndAdd() throws Exception {
        // Random sequence names.
        String seqName = UUID.randomUUID().toString();

        final GridCacheAtomicSequence seq = grid().cache(null).dataStructures().atomicSequence(seqName, 0L, true);

        runSequenceClosure(new GridInUnsafeClosure<GridCacheAtomicSequence>() {
            @Override public void apply(GridCacheAtomicSequence t) throws GridException {
                t.getAndAdd(5);
            }
        }, seq, ITERATION_NUM, THREAD_NUM);

        assertEquals(5 * ITERATION_NUM * THREAD_NUM, seq.get());
    }

    /** @throws Exception If failed. */
    public void testMixed1() throws Exception {
        // Random sequence names.
        String seqName = UUID.randomUUID().toString();

        final GridCacheAtomicSequence seq = grid().cache(null).dataStructures().atomicSequence(seqName, 0L, true);

        runSequenceClosure(new GridInUnsafeClosure<GridCacheAtomicSequence>() {
            @Override public void apply(GridCacheAtomicSequence t) throws GridException {
                t.incrementAndGet();
                t.getAndIncrement();
                t.incrementAndGet();
                t.getAndIncrement();
                t.getAndAdd(3);
                t.addAndGet(3);
            }
        }, seq, ITERATION_NUM, THREAD_NUM);

        assertEquals(10 * ITERATION_NUM * THREAD_NUM, seq.get());
    }

    /** @throws Exception If failed. */
    public void testMixed2() throws Exception {
        // Random sequence names.
        String seqName = UUID.randomUUID().toString();

        final GridCacheAtomicSequence seq = grid().cache(null).dataStructures().atomicSequence(seqName, 0L, true);

        runSequenceClosure(new GridInUnsafeClosure<GridCacheAtomicSequence>() {
            @Override public void apply(GridCacheAtomicSequence t) throws GridException {
                t.getAndAdd(2);
                t.addAndGet(3);
                t.addAndGet(5);
                t.getAndAdd(7);
            }
        }, seq, ITERATION_NUM, THREAD_NUM);

        assertEquals(17 * ITERATION_NUM * THREAD_NUM, seq.get());
    }

    /**
     * Executes given closure in a given number of threads given number of times.
     *
     * @param c Closure to execute.
     * @param seq Sequence to pass into closure.
     * @param cnt Count of iterations per thread.
     * @param threadCnt Thread count.
     * @throws Exception If failed.
     */
    protected void runSequenceClosure(final GridInUnsafeClosure<GridCacheAtomicSequence> c,
        final GridCacheAtomicSequence seq, final int cnt, final int threadCnt) throws Exception {
        multithreaded(new Runnable() {
            @Override public void run() {
                try {
                    for (int i = 0; i < cnt; i++)
                        c.apply(seq);
                }
                catch (GridException e) {
                    throw new RuntimeException(e);
                }
            }
        }, threadCnt);
    }

    /**
     * @param updated Whether use updated values.
     * @throws Exception If failed.
     */
    @SuppressWarnings("IfMayBeConditional")
    private void checkUpdate(boolean updated) throws Exception {
        String seqName = UUID.randomUUID().toString();

        final GridCacheAtomicSequence seq = grid().cache(null).dataStructures().atomicSequence(seqName, 0L, true);

        long curVal = 0;

        Random r = new Random();

        for (int i = 0; i < ITERATION_NUM; i++) {
            long delta = r.nextInt(10) + 1;

            long retVal = updated ? seq.addAndGet(delta) : seq.getAndAdd(delta);

            assertEquals(updated ? curVal + delta : curVal, retVal);

            curVal += delta;
        }
    }

    /**
     * Closure that throws exception.
     *
     * @param <E> Closure argument type.
     */
    private abstract static class GridInUnsafeClosure<E> {
        public abstract void apply(E p) throws GridException;
    }
}
