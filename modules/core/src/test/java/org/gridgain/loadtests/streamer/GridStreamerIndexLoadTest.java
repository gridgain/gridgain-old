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

package org.gridgain.loadtests.streamer;

import org.gridgain.grid.*;
import org.gridgain.grid.streamer.index.*;
import org.gridgain.grid.streamer.index.hash.*;
import org.gridgain.grid.streamer.index.tree.*;
import org.gridgain.grid.streamer.window.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;

import java.util.*;
import java.util.concurrent.atomic.*;

import static org.gridgain.loadtests.util.GridLoadTestArgs.*;
import static org.gridgain.testframework.GridTestUtils.*;

/**
 * Load test for streamer index.
 */
public class GridStreamerIndexLoadTest {
    /**
     * Window index configuration.
     */
    private enum IndexConfiguration {
        /**
         * Tree index with non-unique elements and no event tracking.
         */
        TREE_INDEX_NOT_UNIQUE {
            /** {@inheritDoc} */
            @Override GridStreamerIndexProvider<Integer, Integer, Long> indexProvider() {
                GridStreamerTreeIndexProvider<Integer, Integer, Long> idx = new GridStreamerTreeIndexProvider<>();

                idx.setUpdater(new IndexUpdater());
                idx.setUnique(false);
                idx.setPolicy(GridStreamerIndexPolicy.EVENT_TRACKING_OFF);

                return idx;
            }
        },

        /**
         * Hash index with non-unique elements and no event tracking.
         */
        HASH_INDEX_NOT_UNIQUE {
            /** {@inheritDoc} */
            @Override GridStreamerIndexProvider<Integer, Integer, Long> indexProvider() {
                GridStreamerHashIndexProvider<Integer, Integer, Long> idx = new GridStreamerHashIndexProvider<>();

                idx.setUpdater(new IndexUpdater());
                idx.setUnique(false);
                idx.setPolicy(GridStreamerIndexPolicy.EVENT_TRACKING_OFF);

                return idx;
            }
        };

        /**
         * @return Index provider for this index configuration.
         */
        abstract GridStreamerIndexProvider<Integer, Integer, Long> indexProvider();
    }

    /**
     * @param args Command line arguments.
     * @throws Exception If error occurs.
     */
    public static void main(String[] args) throws Exception {
        for (IndexConfiguration idxCfg : EnumSet.allOf(IndexConfiguration.class)) {
            X.println(">>> Running benchmark for configuration: " + idxCfg);

            runBenchmark(idxCfg);
        }
    }

    /**
     * Runs the benchmark for the specified index configuration.
     *
     * @param idxCfg Index configuration.
     * @throws Exception If error occurs.
     */
    public static void runBenchmark(IndexConfiguration idxCfg) throws Exception {
        int thrCnt = getIntProperty(THREADS_CNT, 1);
        int dur = getIntProperty(TEST_DUR_SEC, 60);
        int winSize = getIntProperty("GG_WIN_SIZE", 5000);

        dumpProperties(System.out);

        final GridStreamerBoundedSizeWindow<Integer> win = new GridStreamerBoundedSizeWindow<>();

        win.setMaximumSize(winSize);
        win.setIndexes(idxCfg.indexProvider());

        win.start();

        final AtomicLong enqueueCntr = new AtomicLong();

        GridFuture<Long> enqueueFut = runMultiThreadedAsync(new CAX() {
            @Override public void applyx() throws GridException {
                Random rnd = new Random();

                while (!Thread.currentThread().isInterrupted()) {
                    win.enqueue(rnd.nextInt());

                    enqueueCntr.incrementAndGet();
                }
            }
        }, thrCnt, "generator");

        final AtomicLong evictCntr = new AtomicLong();

        GridFuture<Long> evictFut = runMultiThreadedAsync(new CAX() {
            @Override public void applyx() throws GridException {
                while (!Thread.currentThread().isInterrupted()) {
                    win.pollEvicted();

                    evictCntr.incrementAndGet();
                }
            }
        }, thrCnt, "evictor");

        GridFuture<Long> collFut = runMultiThreadedAsync(new CAX() {
            @Override public void applyx() {
                int nSec = 0;
                long prevEnqueue = enqueueCntr.get();
                long prevEvict = evictCntr.get();

                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        U.sleep(1000);
                        nSec++;

                        long curEnqueue = enqueueCntr.get();
                        long curEvict = evictCntr.get();

                        X.println("Stats [enqueuePerSec=" + (curEnqueue - prevEnqueue) +
                            ", evictPerSec=" + (curEvict - prevEvict) + ']');

                        prevEnqueue = curEnqueue;
                        prevEvict = curEvict;
                    }
                }
                catch (GridInterruptedException ignored) {
                    // No-op.
                }

                X.println("Final results [enqueuePerSec=" + (enqueueCntr.get() / nSec) +
                    ", evictPerSec=" + (evictCntr.get() / nSec) + ']');
            }
        }, 1, "collector");

        U.sleep(dur * 1000);

        X.println("Finishing test.");

        collFut.cancel();
        enqueueFut.cancel();
        evictFut.cancel();
    }
}
