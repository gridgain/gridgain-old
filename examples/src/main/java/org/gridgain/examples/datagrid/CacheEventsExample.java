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
import org.gridgain.grid.events.*;
import org.gridgain.grid.lang.*;

import java.util.*;

import static org.gridgain.grid.events.GridEventType.*;

/**
 * This examples demonstrates events API. Note that grid events are disabled by default and
 * must be specifically enabled, just like in {@code examples/config/example-cache.xml} file.
 * <p>
 * Remote nodes should always be started with special configuration file which
 * enables P2P class loading: {@code 'ggstart.{sh|bat} examples/config/example-cache.xml'}.
 * <p>
 * Alternatively you can run {@link CacheNodeStartup} in another JVM which will
 * start GridGain node with {@code examples/config/example-cache.xml} configuration.
 */
public class CacheEventsExample {
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
            System.out.println(">>> Cache events example started.");

            final GridCache<Integer, String> cache = g.cache(CACHE_NAME);

            // Clean up caches on all nodes before run.
            cache.globalClearAll(0);

            // This optional local callback is called for each event notification
            // that passed remote predicate listener.
            GridBiPredicate<UUID, GridCacheEvent> locLsnr = new GridBiPredicate<UUID, GridCacheEvent>() {
                @Override public boolean apply(UUID uuid, GridCacheEvent evt) {
                    System.out.println("Received event [evt=" + evt.name() + ", key=" + evt.key() +
                        ", oldVal=" + evt.oldValue() + ", newVal=" + evt.newValue());

                    return true; // Continue listening.
                }
            };

            // Remote listener which only accepts events for keys that are
            // greater or equal than 10 and if event node is primary for this key.
            GridPredicate<GridCacheEvent> rmtLsnr = new GridPredicate<GridCacheEvent>() {
                @Override public boolean apply(GridCacheEvent evt) {
                    System.out.println("Cache event [name=" + evt.name() + ", key=" + evt.key() + ']');

                    int key = evt.key();

                    return key >= 10 && cache.affinity().isPrimary(g.localNode(), key);
                }
            };

            // Subscribe to specified cache events on all nodes that have cache running.
            // Cache events are explicitly enabled in examples/config/example-cache.xml file.
            GridFuture<UUID> fut = g.forCache(CACHE_NAME).events().remoteListen(locLsnr, rmtLsnr,
                EVT_CACHE_OBJECT_PUT, EVT_CACHE_OBJECT_READ, EVT_CACHE_OBJECT_REMOVED);

            // Wait until event listeners are subscribed on all nodes.
            fut.get();

            // Generate cache events.
            for (int i = 0; i < 20; i++)
                cache.putx(i, Integer.toString(i));

            // Wait for a while while callback is notified about remaining puts.
            Thread.sleep(2000);
        }
    }
}
