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

package org.gridgain.loadtests.cache;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.testframework.*;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Benchmark for cache {@code putx()} and {@code get()} operations.
 */
public class GridCacheBenchmark {
    /** Warm up time. */
    public static final long WARM_UP_TIME = Long.getLong("TEST_WARMUP_TIME", 20000);

    /** Number of puts. */
    private static final long PUT_CNT = Integer.getInteger("TEST_PUT_COUNT", 3000000);

    /** Thread count. */
    private static final int THREADS = Integer.getInteger("TEST_THREAD_COUNT", 16);

    /** Test write or read operations. */
    private static boolean testWrite = Boolean.getBoolean("TEST_WRITE");

    /** Cache name. */
    private static final String CACHE = "partitioned";

    /** Counter. */
    private static final AtomicLong cntr = new AtomicLong();

    /** */
    private static final int LOG_MOD = 500000;

    /**
     * @param args Arguments.
     * @throws Exception If failed.
     */
    @SuppressWarnings("BusyWait")
    public static void main(String[] args) throws Exception {
        GridFileLock fileLock = GridLoadTestUtils.fileLock();

        fileLock.lock();

        try {
            final String outputFileName = args.length > 0 ? args[0] : null;

            // try (Grid g = G.start("modules/core/src/test/config/load/cache-client-benchmark.xml")) {
            try (Grid g = G.start("modules/core/src/test/config/load/cache-benchmark.xml")) {
                X.println("warmupTime=" + WARM_UP_TIME);
                X.println("putCnt=" + PUT_CNT);
                X.println("threadCnt=" + THREADS);
                X.println("testWrite=" + testWrite);

                final GridCache<Long, Long> cache = g.cache(CACHE);

                assert cache != null;

                cntr.set(0);

                final AtomicLong opCnt = new AtomicLong();

                X.println("Warming up (putx)...");

                GridLoadTestUtils.runMultithreadedInLoop(new Callable<Object>() {
                    @Nullable @Override public Object call() throws Exception {
                        long keyVal = cntr.incrementAndGet();

                        cache.putx(keyVal % 100000, keyVal);

                        long ops = opCnt.incrementAndGet();

                        if (ops % LOG_MOD == 0)
                            X.println(">>> Performed " + ops + " operations.");

                        return null;
                    }
                }, THREADS, WARM_UP_TIME);

                cntr.set(0);

                opCnt.set(0);

                X.println("Warming up (get)...");

                GridLoadTestUtils.runMultithreadedInLoop(new Callable<Object>() {
                    @Nullable @Override public Object call() throws Exception {
                        long keyVal = cntr.incrementAndGet();

                        Long old = cache.get(keyVal % 100000);

                        long ops = opCnt.incrementAndGet();

                        if (ops % LOG_MOD == 0)
                            X.println(">>> Performed " + ops + " operations, old=" + old + ", keyval=" + keyVal);

                        return null;
                    }
                }, THREADS, WARM_UP_TIME);

                cache.clearAll();

                System.gc();

                cntr.set(0);

                opCnt.set(0);

                X.println("Starting GridGain cache putx() benchmark...");

                long durPutx = GridLoadTestUtils.measureTime(new Callable<Object>() {
                    @Nullable @Override public Object call() throws Exception {
                        while (true) {
                            long keyVal = cntr.incrementAndGet();

                            if (keyVal >= PUT_CNT)
                                break;

                            cache.putx(keyVal % 100000, keyVal);

                            long ops = opCnt.incrementAndGet();

                            if (ops % LOG_MOD == 0)
                                X.println(">>> Performed " + ops + " operations.");
                        }

                        return null;
                    }
                }, THREADS);

                X.println(">>>");
                X.println(">> GridGain cache putx() benchmark results [duration=" + durPutx + " ms, tx/sec=" +
                    (opCnt.get() * 1000 / durPutx) + ", total=" + opCnt.get() + ']');
                X.println(">>>");

                System.gc();

                cntr.set(0);

                opCnt.set(0);

                X.println("Starting GridGain cache get() benchmark...");

                long durGet = GridLoadTestUtils.measureTime(new Callable<Object>() {
                    @Nullable @Override public Object call() throws Exception {
                        while (true) {
                            long keyVal = cntr.incrementAndGet();

                            if (keyVal >= PUT_CNT)
                                break;

                            Long old = cache.get(keyVal % 100000);

                            long ops = opCnt.incrementAndGet();

                            if (ops % LOG_MOD == 0)
                                X.println(">>> Performed " + ops + " operations, old=" + old + ", keyval=" + keyVal);
                        }

                        return null;
                    }
                }, THREADS);

                X.println(">>>");
                X.println(">> GridGain cache get() benchmark results [duration=" + durGet + " ms, tx/sec=" +
                    (opCnt.get() * 1000 / durGet) + ", total=" + opCnt.get() + ']');
                X.println(">>>");

                if (outputFileName != null)
                    GridLoadTestUtils.appendLineToFile(
                        outputFileName,
                        "%s,%d,%d",
                        GridLoadTestUtils.DATE_TIME_FORMAT.format(new Date()),
                        durPutx,
                        durGet);
            }
        }
        finally {
            fileLock.close();
        }
    }
}
