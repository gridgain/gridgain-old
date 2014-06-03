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

package org.gridgain.loadtests.job;

import org.gridgain.grid.*;
import org.gridgain.grid.util.lang.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.loadtests.util.*;
import org.gridgain.testframework.*;
import org.jdk8.backport.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 *
 */
public class GridJobExecutionLoadTestClient implements Callable<Object> {
    /** Performance stats update interval in seconds. */
    private static final int UPDATE_INTERVAL_SEC = 10;

    /** Warm-up duration. */
    public static final int WARM_UP_DURATION = 60 * 1000;

    /** Grid. */
    private static Grid g;

    /** Transaction count. */
    private static LongAdder txCnt = new LongAdder();

    /** Finish flag. */
    private static volatile boolean finish;

    /** {@inheritDoc} */
    @SuppressWarnings("InfiniteLoopStatement")
    @Nullable @Override public Object call() throws Exception {
        GridProjection rmts = g.forRemotes();

        while (!finish) {
            try {
                rmts.compute().execute(GridJobExecutionLoadTestTask.class, null).get();

                txCnt.increment();
            }
            catch (GridException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    /**
     * @param args Args.
     * @throws Exception If failed.
     */
    public static void main(String[] args) throws Exception {
        GridFileLock fileLock = GridLoadTestUtils.fileLock();

        fileLock.lock();

        try {
            final int noThreads = args.length > 0 ? Integer.parseInt(args[0]) : 64;
            final int duration = args.length > 1 ? Integer.parseInt(args[1]) : 0;
            final String outputFileName = args.length > 2 ? args[2] : null;

            X.println("Thread count: " + noThreads);

            g = G.start("modules/tests/config/jobs-load-client.xml");

            warmUp(noThreads);

            final Thread collector = new Thread(new Runnable() {
                @SuppressWarnings("BusyWait")
                @Override public void run() {
                    GridCumulativeAverage avgTxPerSec = new GridCumulativeAverage();

                    try {
                        while (!finish) {
                            Thread.sleep(UPDATE_INTERVAL_SEC * 1000);

                            long txPerSec = txCnt.sumThenReset() / UPDATE_INTERVAL_SEC;

                            X.println(">>>");
                            X.println(">>> Transactions/s: " + txPerSec);

                            avgTxPerSec.update(txPerSec);
                        }
                    }
                    catch (InterruptedException ignored) {
                        X.println(">>> Interrupted.");

                        Thread.currentThread().interrupt();
                    }

                    X.println(">>> Average Transactions/s: " + avgTxPerSec);

                    if (outputFileName != null) {
                        try {
                            X.println("Writing results to file: " + outputFileName);

                            GridLoadTestUtils.appendLineToFile(
                                outputFileName,
                                "%s,%d",
                                GridLoadTestUtils.DATE_TIME_FORMAT.format(new Date()),
                                avgTxPerSec.get()
                            );
                        }
                        catch (IOException e) {
                            X.error("Failed to output results to file.", e);
                        }
                    }
                }
            });

            X.println("Running main test...");

            Thread timer = null;

            try {
                ExecutorService pool = Executors.newFixedThreadPool(noThreads);

                Collection<Callable<Object>> clients = new ArrayList<>(noThreads);

                for (int i = 0; i < noThreads; i++)
                    clients.add(new GridJobExecutionLoadTestClient());

                collector.start();

                if (duration > 0) {
                    timer = new Thread(new Runnable() {
                        @Override public void run() {
                            try {
                                Thread.sleep(duration * 1000);

                                finish = true;
                            }
                            catch (InterruptedException ignored) {
                                X.println(">>> Interrupted.");
                            }
                        }
                    });
                    timer.start();
                }

                pool.invokeAll(clients);

                collector.interrupt();

                pool.shutdown();
            }
            finally {
                if (collector != null && !collector.isInterrupted())
                    collector.interrupt();

                if (timer != null)
                    timer.interrupt();

                G.stopAll(true);
            }
        }
        finally {
            fileLock.close();
        }
    }

    /**
     * Warms the JVM up.
     *
     * @param noThreads Number of threads to use.
     */
    private static void warmUp(int noThreads) {
        X.println("Warming up...");

        final GridProjection rmts = g.forRemotes();

        GridLoadTestUtils.runMultithreadedInLoop(new Callable<Object>() {
            @Nullable @Override public Object call() {
                try {
                    rmts.compute().execute(GridJobExecutionLoadTestTask.class, null).get();
                }
                catch (GridException e) {
                    e.printStackTrace();
                }

                return null;
            }
        }, noThreads, WARM_UP_DURATION);

        // Run GC on all nodes.
        try {
            g.compute().run(new GridAbsClosure() {
                @Override public void apply() {
                    System.gc();
                }
            }).get();
        }
        catch (GridException e) {
            throw new IllegalStateException(e);
        }
    }
}
