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

package org.gridgain.grid.util.future;

import org.gridgain.grid.*;
import org.gridgain.grid.lang.*;
import org.jdk8.backport.*;

import java.util.*;
import java.util.concurrent.*;

/**
 *
 */
public class GridFutureListenPerformanceTest {
    /** */
    private static volatile boolean done;

    /**
     * @param args Args.
     * @throws InterruptedException If failed.
     */
    public static void main(String[] args) throws InterruptedException {
        final LongAdder cnt = new LongAdder();

        final ConcurrentLinkedDeque8<GridFutureAdapter<Object>> futs = new ConcurrentLinkedDeque8<>();

        ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        Thread statThread = new Thread() {
            @SuppressWarnings("BusyWait")
            @Override public void run() {
                while (!done) {
                    try {
                        Thread.sleep(5000);
                    }
                    catch (InterruptedException ignored) {
                        return;
                    }

                    System.out.println(new Date() + " Notifications per sec: " + (cnt.sumThenReset() / 5));
                }
            }
        };

        statThread.setDaemon(true);

        statThread.start();

        for (int i = 0; i < Runtime.getRuntime().availableProcessors() ; i++) {
            pool.submit(new Callable<Object>() {
                @Override public Object call() throws Exception {
                    Random rnd = new Random();

                    while (!done) {
                        for (int j = 0; j < rnd.nextInt(10); j++) {
                            GridFutureAdapter<Object> fut = new GridFutureAdapter<>();

                            futs.add(fut);

                            for (int k = 1; k < rnd.nextInt(3); k++) {
                                    fut.listenAsync(new GridInClosure<GridFuture<Object>>() {
                                    @Override public void apply(GridFuture<Object> t) {
                                        try {
                                            t.get();
                                        }
                                        catch (GridException e) {
                                            e.printStackTrace();
                                        }

                                        cnt.increment();
                                    }
                                });
                            }
                        }

                        GridFutureAdapter<Object> fut;

                        while ((fut = futs.poll()) != null)
                            fut.onDone();
                    }

                    return null;
                }
            });
        }

        Thread.sleep(5 * 60 * 1000);

        done = true;
    }
}
