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

package org.gridgain.examples.misc.client.memcache;

import net.spy.memcached.*;
import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.cache.datastructures.*;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * This example shows how to use Memcache client for manipulating GridGain cache.
 * <p>
 * GridGain implements Memcache binary protocol and it is available if
 * REST is enabled on the node.
 * Remote nodes should always be started using {@link MemcacheRestExampleNodeStartup}.
 */
public class MemcacheRestExample {
    /** Hostname for client connection. */
    private static final String host = "localhost";

    /** Port number for client connection. */
    private static final int port = 11211;

    /**
     * @param args Command line arguments.
     * @throws Exception In case of error.
     */
    public static void main(String[] args) throws Exception {
        MemcachedClient client = null;

        try (Grid g = GridGain.start(MemcacheRestExampleNodeStartup.configuration())) {
            System.out.println();
            System.out.println(">>> Memcache REST example started.");

            GridCache<String, Object> cache = g.cache(null);

            client = startMemcachedClient(host, port);

            // Put string value to cache using Memcache binary protocol.
            if (client.add("strKey", 0, "strVal").get())
                System.out.println(">>> Successfully put string value using Memcache client.");

            // Check that string value is actually in cache using traditional
            // GridGain API and Memcache binary protocol.
            System.out.println(">>> Getting value for 'strKey' using GridGain cache API: " + cache.get("strKey"));
            System.out.println(">>> Getting value for 'strKey' using Memcache client: " + client.get("strKey"));

            // Remove string value from cache using Memcache binary protocol.
            if (client.delete("strKey").get())
                System.out.println(">>> Successfully removed string value using Memcache client.");

            // Check that cache is empty.
            System.out.println(">>> Current cache size: " + cache.size() + " (expected: 0).");

            // Put integer value to cache using Memcache binary protocol.
            if (client.add("intKey", 0, 100).get())
                System.out.println(">>> Successfully put integer value using Memcache client.");

            // Check that integer value is actually in cache using traditional
            // GridGain API and Memcache binary protocol.
            System.out.println(">>> Getting value for 'intKey' using GridGain cache API: " + cache.get("intKey"));
            System.out.println(">>> Getting value for 'intKey' using Memcache client: " + client.get("intKey"));

            // Remove string value from cache using Memcache binary protocol.
            if (client.delete("intKey").get())
                System.out.println(">>> Successfully removed integer value using Memcache client.");

            // Check that cache is empty.
            System.out.println(">>> Current cache size: " + cache.size() + " (expected: 0).");

            // Create atomic long.
            GridCacheAtomicLong l = cache.dataStructures().atomicLong("atomicLong", 10, true);

            // Increment atomic long by 5 using Memcache client.
            if (client.incr("atomicLong", 5, 0) == 15)
                System.out.println(">>> Successfully incremented atomic long by 5.");

            // Increment atomic long using GridGain API and check that value is correct.
            System.out.println(">>> New atomic long value: " + l.incrementAndGet() + " (expected: 16).");

            // Decrement atomic long by 3 using Memcache client.
            if (client.decr("atomicLong", 3, 0) == 13)
                System.out.println(">>> Successfully decremented atomic long by 3.");

            // Decrement atomic long using GridGain API and check that value is correct.
            System.out.println(">>> New atomic long value: " + l.decrementAndGet() + " (expected: 12).");
        }
        finally {
            if (client != null)
                client.shutdown();
        }
    }

    /**
     * Creates Memcache client that uses binary protocol and connects to GridGain.
     *
     * @param host Hostname.
     * @param port Port number.
     * @return Client.
     * @throws IOException If connection failed.
     */
    private static MemcachedClient startMemcachedClient(String host, int port) throws IOException {
        assert host != null;
        assert port > 0;

        return new MemcachedClient(new BinaryConnectionFactory(), Arrays.asList(new InetSocketAddress(host, port)));
    }
}
