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

package org.gridgain.loadtests.capacity;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.util.typedef.*;
import org.springframework.context.support.*;

import java.lang.management.*;

/**
 * Continuous mapper load test.
 */
public class GridCapacityLoadTest {
    /** Heap usage. */
    private static final MemoryMXBean mem = ManagementFactory.getMemoryMXBean();

    /**
     * Main method.
     *
     * @param args Parameters.
     * @throws Exception If failed.
     */
    public static void main(String[] args) throws Exception {
        // Initialize Spring factory.
        ClassPathXmlApplicationContext ctx =
            new ClassPathXmlApplicationContext("org/gridgain/loadtests/capacity/spring-capacity-cache.xml");

        GridConfiguration cfg = (GridConfiguration)ctx.getBean("grid.cfg");

        try (Grid g = G.start(cfg)) {
            GridCache<Integer, Integer> c = g.cache(null);

            long init = mem.getHeapMemoryUsage().getUsed();

            printHeap(init);

            int cnt = 0;

            for (; cnt < 3000000; cnt++) {
                c.put(cnt, cnt);

                if (cnt % 10000 == 0) {
                    X.println("Stored count: " + cnt);

                    printHeap(init);

                    if (cnt > 2100000 &&  cnt % 100000 == 0)
                        System.gc();
                }
            }

            System.gc();

            Thread.sleep(1000);

            printHeap(init);

            MemoryUsage heap = mem.getHeapMemoryUsage();

            long used = heap.getUsed() - init;

            long entrySize = cnt > 0 ? used / cnt : 0;

            X.println("Average entry size: " + entrySize);
        }
    }

    private static void printHeap(long init) {
        MemoryUsage heap = mem.getHeapMemoryUsage();

        long max = heap.getMax() - init;
        long used = heap.getUsed() - init;
        long left = max - used;

        X.println("Heap left: " + (left / (1024 * 1024)) + "MB");
    }
}
