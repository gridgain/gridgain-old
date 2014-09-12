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

package org.gridgain.examples.datagrid.store;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;

import java.util.*;

/**
 * Demonstrates usage of cache with underlying persistent store configured.
 * <p>
 * Remote nodes should always be started using {@link CacheNodeWithStoreStartup}.
 * Also you can change type of underlying store modifying configuration in the
 * {@link CacheNodeWithStoreStartup#configure()} method.
 */
public class CacheStoreExample {
    /** Global person ID to use across entire example. */
    private static final Long id = Math.abs(UUID.randomUUID().getLeastSignificantBits());

    /**
     * Executes example.
     *
     * @param args Command line arguments, none required.
     * @throws GridException If example execution failed.
     */
    public static void main(String[] args) throws Exception {
        GridConfiguration cfg = CacheNodeWithStoreStartup.configure();

        // To start grid with desired configuration uncomment the appropriate line.
        try (Grid g = GridGain.start(cfg)) {
            System.out.println();
            System.out.println(">>> Cache store example started.");

            GridCache<Long, Person> cache = g.cache(null);

            // Clean up caches on all nodes before run.
            cache.globalClearAll(0);

            try (GridCacheTx tx = cache.txStart()) {
                Person val = cache.get(id);

                System.out.println("Read value: " + val);

                val = cache.put(id, person(id, "Isaac", "Newton"));

                System.out.println("Overwrote old value: " + val);

                val = cache.get(id);

                System.out.println("Read value: " + val);

                tx.commit();
            }

            System.out.println("Read value after commit: " + cache.get(id));
        }
    }

    /**
     * Creates person.
     *
     * @param id ID.
     * @param firstName First name.
     * @param lastName Last name.
     * @return Newly created person.
     */
    private static Person person(long id, String firstName, String lastName) {
        return new Person(id, firstName, lastName);
    }
}
