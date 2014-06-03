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

package org.gridgain.grid.loadtest.swapspace;

import org.gridgain.grid.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.swapspace.*;
import org.gridgain.grid.spi.swapspace.file.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.loadtests.util.*;
import org.gridgain.testframework.*;
import org.gridgain.testframework.junits.common.*;
import org.jdk8.backport.*;

import java.util.*;
import java.util.concurrent.atomic.*;

/**
 * Runs concurrent operations on File Swap Space SPI
 * in multiple threads.
 */
public class GridFileSwapSpaceSpiMultithreadedLoadTest extends GridCommonAbstractTest {
    /** Number of threads. */
    private static final int N_THREADS = 8;

    /** Space name. */
    private static final String SPACE_NAME = "grid-mt-bm-space";

    /** Batch size. */
    private static final int BATCH_SIZE = 200;

    /** Max entries to store. */
    private static final long MAX_ENTRIES = 9000000;

    /** Test duration. */
    private static final long DURATION = 10 * 60 * 1000;

    /** Swap context. */
    private final GridSwapContext swapCtx = new GridSwapContext();

    /** SPI to test. */
    private GridSwapSpaceSpi spi;

    /**
     * Starts the daemon thread.
     *
     * @param runnable Thread runnable.
     */
    private static void startDaemon(Runnable runnable) {
        Thread t = new Thread(runnable);

        t.setDaemon(true);

        t.start();
    }

    /**
     * @return An SPI instance to test.
     */
    private GridSwapSpaceSpi spi() {
        GridFileSwapSpaceSpi spi = new GridFileSwapSpaceSpi();

//        spi.setConcurrencyLevel(N_THREADS);
//        spi.setWriterThreadsCount(N_THREADS);

        return spi;
    }

    /**
     * @return Swap context for swap operations.
     */
    @SuppressWarnings("ConstantConditions")
    private GridSwapContext context() {
        return swapCtx;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        spi = spi();

        getTestResources().inject(spi);

        spi.spiStart("");

        spi.clear(SPACE_NAME);
    }

    /** @throws Exception If failed. */
    @Override protected void afterTest() throws Exception {
        spi.spiStop();
    }

    /**
     * Tests concurrent batch evict-promote.
     *
     * @throws Exception If error occurs.
     */
    public void testBatchEvictUnswap() throws Exception {
        final AtomicInteger storedEntriesCnt = new AtomicInteger();

        final AtomicBoolean done = new AtomicBoolean();

        startDaemon(new Runnable() {
            @SuppressWarnings("BusyWait")
            @Override public void run() {
                int curCnt = storedEntriesCnt.get();

                GridCumulativeAverage avg = new GridCumulativeAverage();

                try {
                    while (!done.get()) {
                        Thread.sleep(1000);

                        int newCnt = storedEntriesCnt.get();

                        int entPerSec = newCnt - curCnt;

                        X.println(">>> Storing " + entPerSec + " entries/second");

                        avg.update(entPerSec);

                        curCnt = newCnt;
                    }
                }
                catch (InterruptedException ignored) {
                    // No-op.
                }
                finally {
                    X.println(">>> Average store speed: " + avg + " entries/second");
                }
            }
        });

        GridFuture<?> evictFut = GridTestUtils.runMultiThreadedAsync(new Runnable() {
            @Override public void run() {
                try {
                    ThreadLocalRandom8 rnd = ThreadLocalRandom8.current();

                    Map<GridSwapKey, byte[]> entries = new HashMap<>(BATCH_SIZE);

                    while (!done.get()) {
                        long l = rnd.nextLong(0, MAX_ENTRIES);

                        entries.put(new GridSwapKey(l), Long.toString(l).getBytes());

                        if (entries.size() == BATCH_SIZE) {
                            spi.storeAll(SPACE_NAME, entries, context());

                            storedEntriesCnt.addAndGet(BATCH_SIZE);

                            entries.clear();
                        }
                    }
                }
                catch (GridSpiException e) {
                    e.printStackTrace();

                    throw new GridRuntimeException(e);
                }
            }
        }, N_THREADS, "store");

        final AtomicInteger readRmvKeys = new AtomicInteger();

        startDaemon(new Runnable() {
            @SuppressWarnings("BusyWait")
            @Override public void run() {
                int curCnt = readRmvKeys.get();

                GridCumulativeAverage avg = new GridCumulativeAverage();

                try {
                    while (!done.get()) {
                        Thread.sleep(1000);

                        int newCnt = readRmvKeys.get();

                        int entPerSec = newCnt - curCnt;

                        X.println(">>> Read-and-removed " + entPerSec + " entries/second");

                        avg.update(entPerSec);

                        curCnt = newCnt;
                    }
                }
                catch (InterruptedException ignored) {
                    //No-op.
                }
                finally {
                    X.println(">>> Average read-and-remove speed: " + avg + " entries/second");
                }
            }
        });

        GridFuture<?> unswapFut = GridTestUtils.runMultiThreadedAsync(new Runnable() {
            @Override public void run() {
                try {
                    ThreadLocalRandom8 rnd = ThreadLocalRandom8.current();

                    Collection<GridSwapKey> keys = new ArrayList<>(BATCH_SIZE);

                    while (!done.get()) {
                        keys.add(new GridSwapKey(rnd.nextLong(0, MAX_ENTRIES)));

                        if (keys.size() == BATCH_SIZE) {
                            spi.readAll(SPACE_NAME, keys, context());

                            spi.removeAll(SPACE_NAME, keys, null, context());

                            readRmvKeys.addAndGet(BATCH_SIZE);

                            keys.clear();
                        }
                    }
                }
                catch (GridException e) {
                    e.printStackTrace();
                }
            }
        }, N_THREADS, "read-remove");

        Thread.sleep(DURATION);

        done.set(true);

        evictFut.get();

        unswapFut.get();
    }
}
