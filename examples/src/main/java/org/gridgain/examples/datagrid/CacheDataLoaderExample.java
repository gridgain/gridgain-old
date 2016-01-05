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

import org.gridgain.examples.*;
import org.gridgain.grid.*;
import org.gridgain.grid.dataload.*;

/**
 * Demonstrates how cache can be populated with data utilizing {@link GridDataLoader} API.
 * {@link GridDataLoader} is a lot more efficient to use than standard
 * {@code GridCacheProjection.put(...)} operation as it properly buffers cache requests
 * together and properly manages load on remote nodes.
 * <p>
 * Remote nodes should always be started with special configuration file which
 * enables P2P class loading: {@code 'ggstart.{sh|bat} examples/config/example-cache.xml'}.
 * <p>
 * Alternatively you can run {@link CacheNodeStartup} in another JVM which will
 * start GridGain node with {@code examples/config/example-cache.xml} configuration.
 */
public class CacheDataLoaderExample {
    /** Cache name. */
    private static final String CACHE_NAME = "partitioned";

    /** Number of entries to load. */
    private static final int ENTRY_COUNT = 500000;

    /** Heap size required to run this example. */
    public static final int MIN_MEMORY = 512 * 1024 * 1024;

    /**
     * Executes example.
     *
     * @param args Command line arguments, none required.
     * @throws GridException If example execution failed.
     */
    public static void main(String[] args) throws Exception {
        ExamplesUtils.checkMinMemory(MIN_MEMORY);

        try (Grid g = GridGain.start("examples/config/example-cache.xml")) {
            System.out.println();
            System.out.println(">>> Cache data loader example started.");

            // Clean up caches on all nodes before run.
            g.cache(CACHE_NAME).globalClearAll(0);

            System.out.println();
            System.out.println(">>> Cache clear finished.");

            long start = System.currentTimeMillis();

            try (GridDataLoader<Integer, String> ldr = g.dataLoader(CACHE_NAME)) {
                // Configure loader.
                ldr.perNodeBufferSize(1024);
                ldr.perNodeParallelLoadOperations(8);
                ldr.isolated(true);

                for (int i = 0; i < ENTRY_COUNT; i++) {
                    ldr.addData(i, Integer.toString(i));

                    // Print out progress while loading cache.
                    if (i > 0 && i % 10000 == 0)
                        System.out.println("Loaded " + i + " keys.");
                }
            }

            long end = System.currentTimeMillis();

            System.out.println(">>> Loaded " + ENTRY_COUNT + " keys in " + (end - start) + "ms.");
        }
    }
}
