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

/**
 * Demonstrates new functional APIs.
 * <p>
 * Remote nodes should always be started with special configuration file which
 * enables P2P class loading: {@code 'ggstart.{sh|bat} examples/config/example-compute.xml'}.
 * <p>
 * Alternatively you can run {@link ComputeNodeStartup} in another JVM which will start GridGain node
 * with {@code examples/config/example-compute.xml} configuration.
 */
public class ComputeProjectionExample {
    /**
     * Executes example.
     *
     * @param args Command line arguments, none required.
     * @throws GridException If example execution failed.
     */
    public static void main(String[] args) throws Exception {
        try (Grid grid = GridGain.start("examples/config/example-compute.xml")) {
            if (!ExamplesUtils.checkMinTopologySize(grid, 2))
                return;

            System.out.println();
            System.out.println("Compute projection example started.");

            // Say hello to all nodes in the grid, including local node.
            // Note, that Grid itself also implements GridProjection.
            sayHello(grid);

            // Say hello to all remote nodes.
            sayHello(grid.forRemotes());

            // Pick random node out of remote nodes.
            GridProjection randomNode = grid.forRemotes().forRandom();

            // Say hello to a random node.
            sayHello(randomNode);

            // Say hello to all nodes residing on the same host with random node.
            sayHello(grid.forHost(randomNode.node()));

            // Say hello to all nodes that have current CPU load less than 50%.
            sayHello(grid.forPredicate(new GridPredicate<GridNode>() {
                @Override public boolean apply(GridNode n) {
                    return n.metrics().getCurrentCpuLoad() < 0.5;
                }
            }));
        }
    }

    /**
     * Print 'Hello' message on remote grid nodes.
     *
     * @param prj Grid projection.
     * @throws GridException If failed.
     */
    private static void sayHello(final GridProjection prj) throws GridException {
        // Print out hello message on all projection nodes.
        prj.compute().broadcast(
            new GridRunnable() {
                @Override public void run() {
                    // Print ID of remote node on remote node.
                    System.out.println(">>> Hello Node: " + prj.grid().localNode().id());
                }
            }
        ).get();
    }
}
