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
import org.gridgain.grid.cache.query.*;
import org.gridgain.grid.lang.*;

import java.util.*;

/**
 * This examples demonstrates continuous query API.
 * <p>
 * Remote nodes should always be started with special configuration file which
 * enables P2P class loading: {@code 'ggstart.{sh|bat} examples/config/example-cache.xml'}.
 * <p>
 * Alternatively you can run {@link CacheNodeStartup} in another JVM which will
 * start GridGain node with {@code examples/config/example-cache.xml} configuration.
 */
public class CacheContinuousQueryExample {
    /** Cache name. */
    private static final String CACHE_NAME = "partitioned";

    /**
     * Executes example.
     *
     * @param args Command line arguments, none required.
     * @throws GridException If example execution failed.
     */
    public static void main(String[] args) throws GridException, InterruptedException {
        try (Grid g = GridGain.start("examples/config/example-cache.xml")) {
            System.out.println();
            System.out.println(">>> Cache continuous query example started.");

            GridCache<Integer, String> cache = g.cache(CACHE_NAME);

            // Clean up caches on all nodes before run.
            cache.globalClearAll(0);

            int keyCnt = 20;

            for (int i = 0; i < keyCnt; i++)
                cache.putx(i, Integer.toString(i));

            // Create new continuous query.
            try (GridCacheContinuousQuery<Integer, String> qry = cache.queries().createContinuousQuery()) {
                // Callback that is called locally when update notifications are received.
                qry.localCallback(
                    new GridBiPredicate<UUID, Collection<GridCacheContinuousQueryEntry<Integer, String>>>() {
                        @Override public boolean apply(
                            UUID nodeId,
                            Collection<GridCacheContinuousQueryEntry<Integer, String>> entries
                        ) {
                            for (GridCacheContinuousQueryEntry<Integer, String> e : entries)
                                System.out.println("Queried entry [key=" + e.getKey() + ", val=" + e.getValue() + ']');

                            return true; // Return true to continue listening.
                        }
                    });

                // This filter will be evaluated remotely on all nodes
                // Entry that pass this filter will be sent to the caller.
                qry.remoteFilter(new GridPredicate<GridCacheContinuousQueryEntry<Integer, String>>() {
                    @Override public boolean apply(GridCacheContinuousQueryEntry<Integer, String> e) {
                        return e.getKey() > 15;
                    }
                });

                // Execute query.
                qry.execute();

                // Add a few more keys and watch more query notifications.
                for (int i = keyCnt; i < keyCnt + 5; i++)
                    cache.putx(i, Integer.toString(i));

                // Wait for a while while callback is notified about remaining puts.
                Thread.sleep(2000);
            }
        }
    }
}
