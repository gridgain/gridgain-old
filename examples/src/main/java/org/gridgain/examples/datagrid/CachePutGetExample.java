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

package org.gridgain.examples.datagrid;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;

import java.util.*;

/**
 * This example demonstrates very basic operations on cache, such as 'put' and 'get'.
 * <p>
 * Remote nodes should always be started with special configuration file which
 * enables P2P class loading: {@code 'ggstart.{sh|bat} examples/config/example-cache.xml'}.
 * <p>
 * Alternatively you can run {@link CacheNodeStartup} in another JVM which will
 * start GridGain node with {@code examples/config/example-cache.xml} configuration.
 */
public class CachePutGetExample {
    /** Cache name. */
    private static final String CACHE_NAME = "partitioned";

    /**
     * Executes example.
     *
     * @param args Command line arguments, none required.
     * @throws GridException If example execution failed.
     */
    public static void main(String[] args) throws Exception {
        try (Grid g = GridGain.start("examples/config/example-cache.xml")) {
            // Clean up caches on all nodes before run.
            g.cache(CACHE_NAME).globalClearAll(0);

            // Individual puts and gets.
            putGet();

            // Bulk puts and gets.
            putAllGetAll();
        }
    }

    /**
     * Execute individual puts and gets.
     *
     * @throws GridException If failed.
     */
    private static void putGet() throws GridException {
        System.out.println();
        System.out.println(">>> Cache put-get example started.");

        Grid g = GridGain.grid();

        final GridCache<Integer, String> cache = g.cache(CACHE_NAME);

        final int keyCnt = 20;

        // Store keys in cache.
        for (int i = 0; i < keyCnt; i++)
            cache.putx(i, Integer.toString(i));

        System.out.println(">>> Stored values in cache.");

        for (int i = 0; i < keyCnt; i++)
            System.out.println("Got [key=" + i + ", val=" + cache.get(i) + ']');
    }

    /**
     * Execute bulk {@code putAll(...)} and {@code getAll(...)} operations.
     *
     * @throws GridException If failed.
     */
    private static void putAllGetAll() throws GridException {
        System.out.println();
        System.out.println(">>> Starting putAll-getAll example.");

        Grid g = GridGain.grid();

        final GridCache<Integer, String> cache = g.cache(CACHE_NAME);

        final int keyCnt = 20;

        // Create batch.
        Map<Integer, String> batch = new HashMap<>(keyCnt);

        for (int i = 0; i < keyCnt; i++)
            batch.put(i, "bulk-" + Integer.toString(i));

        // Bulk-store entries in cache.
        cache.putAll(batch);

        System.out.println(">>> Bulk-stored values in cache.");

        // Bulk-get values from cache.
        Map<Integer, String> vals = cache.getAll(batch.keySet());

        for (Map.Entry<Integer, String> e : vals.entrySet())
            System.out.println("Got entry [key=" + e.getKey() + ", val=" + e.getValue() + ']');
    }
}
