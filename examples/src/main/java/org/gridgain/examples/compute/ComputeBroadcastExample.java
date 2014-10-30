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

package org.gridgain.examples.compute;

import org.gridgain.examples.*;
import org.gridgain.grid.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.resources.*;

import java.util.*;

/**
 * Demonstrates broadcasting computations within grid projection.
 * <p>
 * Remote nodes should always be started with special configuration file which
 * enables P2P class loading: {@code 'ggstart.{sh|bat} examples/config/example-compute.xml'}.
 * <p>
 * Alternatively you can run {@link ComputeNodeStartup} in another JVM which will start GridGain node
 * with {@code examples/config/example-compute.xml} configuration.
 */
public class ComputeBroadcastExample {
    /**
     * Executes example.
     *
     * @param args Command line arguments, none required.
     * @throws GridException If example execution failed.
     */
    public static void main(String[] args) throws Exception {
        try (Grid grid = GridGain.start("examples/config/example-compute.xml")) {
            System.out.println();
            System.out.println(">>> Compute broadcast example started.");

            // Print hello message on all nodes.
            hello(grid);

            // Gather system info from all nodes.
            gatherSystemInfo(grid);
       }
    }

    /**
     * Print 'Hello' message on all grid nodes.
     *
     * @param g Grid instance.
     * @throws GridException If failed.
     */
    private static void hello(Grid g) throws GridException {
        // Print out hello message on all nodes.
        g.compute().broadcast(
            new GridRunnable() {
                @Override public void run() {
                    System.out.println();
                    System.out.println(">>> Hello Node! :)");
                }
            }
        ).get();

        System.out.println();
        System.out.println(">>> Check all nodes for hello message output.");
    }

    /**
     * Gather system info from all nodes and print it out.
     *
     * @param g Grid instance.
     * @throws GridException if failed.
     */
    private static void gatherSystemInfo(Grid g) throws GridException {
        // Gather system info from all nodes.
        Collection<String> res = g.compute().broadcast(
            new GridCallable<String>() {
                // Automatically inject grid instance.
                @GridInstanceResource
                private Grid grid;

                @Override public String call() {
                    System.out.println();
                    System.out.println("Executing task on node: " + grid.localNode().id());

                    return "Node ID: " + grid.localNode().id() + "\n" +
                        "OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version") + " " +
                        System.getProperty("os.arch") + "\n" +
                        "User: " + System.getProperty("user.name") + "\n" +
                        "JRE: " + System.getProperty("java.runtime.name") + " " +
                        System.getProperty("java.runtime.version");
                }
        }).get();

        // Print result.
        System.out.println();
        System.out.println("Nodes system information:");
        System.out.println();

        for (String r : res) {
            System.out.println(r);
            System.out.println();
        }
    }
}
