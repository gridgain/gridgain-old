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

package org.gridgain.testframework;

import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.testframework.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.text.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Utility class for load tests.
 */
public class GridLoadTestUtils {
    /** Date and time format. */
    public static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

    /** Lock file. */
    public static final File LOCK_FILE = new File(System.getProperty("user.home"), ".gg-loadtest-lock");

    /** */
    private GridLoadTestUtils() {
        // No-op.
    }

    /**
     * Appends a line to a file. Creates parent directories if any of those do not
     * exist.
     *
     * @param fileName Name of a file to append to.
     * @param format A line format.
     * @param vals A line format args.
     * @throws IOException If IO error occurs.
     */
    public static void appendLineToFile(String fileName, String format, Object... vals) throws IOException {
        new File(fileName).getParentFile().mkdirs();

        try (Writer out = new BufferedWriter(new FileWriter(fileName, true))) {
            out.write(String.format(format + '\n', vals));
        }
    }

    /**
     * Runs a given callable in loop in multiple threads for specified period of time.
     *
     * @param c Callable to run.
     * @param threadCnt Thread count.
     * @param dur Run duration in milliseconds.
     */
    public static void runMultithreadedInLoop(final Callable<?> c, int threadCnt, long dur) {
        ExecutorService pool = Executors.newFixedThreadPool(threadCnt);
        final AtomicBoolean finish = new AtomicBoolean();

        for (int i = 0; i < threadCnt; i++)
            pool.submit(new Callable<Object>() {
                @Nullable @Override public Object call() throws Exception {
                    while (!finish.get())
                        c.call();

                    return null;
                }
            });

        try {
            Thread.sleep(dur);
        }
        catch (InterruptedException ignored) {
            // No-op.
        }

        finish.set(true);

        pool.shutdown();
    }

    /**
     * Performs operation, measuring it's time.
     *
     * @param c Operation to perform.
     * @param threadCnt Number of parallel threads to perform operation (>= 1).
     * @return Number of milliseconds, in which the operation was completed.
     * @throws Exception If provided callable has thrown exception.
     */
    public static long measureTime(Callable<Object> c, int threadCnt) throws Exception {
        A.ensure(threadCnt >= 1, "threadCnt should be >= 1");

        long start = System.currentTimeMillis();

        if (threadCnt == 1)
            c.call();
        else
            GridTestUtils.runMultiThreaded(c, threadCnt, "test-worker");

        return System.currentTimeMillis() - start;
    }

    /**
     * Starts a daemon thread.
     *
     * @param r Thread runnable.
     * @return Started daemon thread.
     */
    public static Thread startDaemon(Runnable r) {
        Thread t = new Thread(r);

        t.setDaemon(true);

        t.start();

        return t;
    }

    /**
     * Returns a new instance of {@link GridFileLock}, that
     * can be used to synchronize on a lock file, which resides
     * in a temporary directory.
     * <p>
     * If a lock file does not exist, it is created.
     * <p>
     * If a temporary directory does not exist, an exception is
     * thrown.
     *
     * @return An file lock instance. Calling {@link GridFileLock#lock()}
     *         will perform actual locking.
     * @throws RuntimeException If a temporary directory is not found.
     */
    public static GridFileLock fileLock() {
        try {
            return new GridFileLock(LOCK_FILE);
        }
        catch (FileNotFoundException e) {
            throw new RuntimeException("tmp.dir not found", e);
        }
    }
}
