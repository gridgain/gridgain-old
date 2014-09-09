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

package org.gridgain.loadtests.mapper;

import org.gridgain.grid.*;
import org.gridgain.grid.dataload.*;
import org.gridgain.grid.util.typedef.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Continuous mapper load test.
 */
public class GridContinuousMapperLoadTest2 {
    /**
     * Main method.
     *
     * @param args Parameters.
     * @throws Exception If failed.
     */
    public static void main(String[] args) throws Exception {
        final AtomicInteger jobIdGen = new AtomicInteger();
        final AtomicInteger sentJobs = new AtomicInteger();

        final LinkedBlockingQueue<Integer> queue = new LinkedBlockingQueue<>(10);

        /** Worker thread. */
        Thread t = new Thread("mapper-worker") {
            @Override public void run() {
                try {
                    while (!Thread.currentThread().isInterrupted())
                        queue.put(jobIdGen.incrementAndGet());
                }
                catch (InterruptedException ignore) {
                    // No-op.
                }
            }
        };

        Grid g = G.start("examples/config/example-cache.xml");

        try {
            int max = 20000;

            GridDataLoader<Integer, TestObject> ldr = g.dataLoader("replicated");

            for (int i = 0; i < max; i++)
                ldr.addData(i, new TestObject(i, "Test object: " + i));

            // Wait for loader to complete.
            ldr.close(false);

            X.println("Populated replicated cache.");

            t.start();

            while (sentJobs.get() < max) {
                int[] jobIds = new int[10];

                for (int i = 0; i < jobIds.length; i++)
                    jobIds[i] = queue.take();

                sentJobs.addAndGet(10);

                g.compute().execute(new GridContinuousMapperTask2(), jobIds).get();
            }
        }
        finally {
            t.interrupt();

            t.join();

            G.stopAll(false);
        }
    }
}
