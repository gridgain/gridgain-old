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
import org.gridgain.grid.lang.*;

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

        // Projection (view) for remote nodes that have cache running.
        GridProjection rmts = g.forCache(CACHE_NAME).forRemotes();

        // If no other cache nodes are started.
        if (rmts.nodes().isEmpty()) {
            System.out.println(">>> Need to start remote nodes to complete example.");

            return;
        }

        // Get and print out values on all remote nodes.
        rmts.compute().broadcast(new GridCallable<Object>() {
            @Override public Object call() throws GridException {
                for (int i = 0; i < keyCnt; i++)
                    System.out.println("Got [key=" + i + ", val=" + cache.get(i) + ']');

                return null;
            }
        }).get();
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

        // Projection (view) for remote nodes that have cache running.
        GridProjection rmts = g.forCache(CACHE_NAME).forRemotes();

        // If no other cache nodes are started.
        if (rmts.nodes().isEmpty()) {
            System.out.println(">>> Need to start remote nodes to complete example.");

            return;
        }

        final Collection<Integer> keys = new ArrayList<>(batch.keySet());

        // Get values from all remote cache nodes.
        Collection<Map<Integer, String>> retMaps = rmts.compute().broadcast(
            new GridCallable<Map<Integer, String>>() {
                @Override public Map<Integer, String> call() throws GridException {
                    Map<Integer, String> vals = cache.getAll(keys);

                    for (Map.Entry<Integer, String> e : vals.entrySet())
                        System.out.println("Got entry [key=" + e.getKey() + ", val=" + e.getValue() + ']');

                    return vals;
                }
            }).get();

        System.out.println(">>> Got all entries from all remote nodes.");

        // Since we get the same keys on all nodes, values should be equal to the initial batch.
        for (Map<Integer, String> map : retMaps)
            assert map.equals(batch);
    }
}
