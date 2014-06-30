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

package org.gridgain.examples.misc.client.router;

import org.gridgain.client.*;
import org.gridgain.client.router.*;
import org.gridgain.examples.misc.client.api.*;

import java.util.*;

/**
 * This example demonstrates use of Java client, connected to Grid through router.
 * To execute this example you should start an instance of
 * {@link ClientExampleNodeStartup} class which will start up a GridGain node.
 * And an instance of {@link RouterStartup} which will start up
 * a GridGain router.
 * <p>
 * Alternatively you can run node and router instances from command line.
 * To do so you need to execute commands
 * {@code GRIDGAIN_HOME/bin/ggstart.sh examples/config/example-cache.xml}
 * and {@code GRIDGAIN_HOME/bin/ggrouter.sh clients/java/config/router/default-router.xml}
 * For more details on how to configure standalone router instances please refer to
 * configuration file {@code GRIDGAIN_HOME/clients/java/config/router/default-router.xml}.
 * <p>
 * This example creates client, configured to work with router and runs an example task
 * calculating number of nodes in grid.
 * <p>
 * Note that different nodes and routers cannot share the same port for rest services.
 * If you want to start more than one node and/or router on the same physical machine
 * you must provide different configurations for each node and router.
 * <p>
 * Another option for routing is to use one of node as a router itself. You can
 * transparently pass node address in {@link GridClientConfiguration#getRouters()}
 * an it will be routing incoming requests.
 * <p>
 * Before running this example you must run {@link RouterStartup}.
 */
public class RouterExample {
    /** Grid node address to connect to. */
    private static final String ROUTER_ADDRESS = "127.0.0.1";

    /** Grid node port to connect to using binary protocol. */
    private static final int ROUTER_TCP_PORT = GridTcpRouterConfiguration.DFLT_TCP_PORT;

    // Run few nodes and uncomment the following line to use one of nodes as a TCP router.
    // private static final int ROUTER_TCP_PORT = GridConfiguration.DFLT_TCP_PORT;

    /**
     * Executes example.
     *
     * @param args Command line arguments, none required.
     * @throws GridClientException If example execution failed.
     */
    public static void main(String[] args) throws GridClientException {
        System.out.println();
        System.out.println(">>> Router example started.");

        try (GridClient tcpClient = createTcpClient()) {
            System.out.println(">>> TCP client created, current grid topology: " + tcpClient.compute().nodes());

            runExample(tcpClient);
        }
    }

    /**
     * Performs few cache operations on the given client.
     * <p>
     * Running this method should produce
     * {@code >>> Client job is run by: Router example.}
     * output on every node in grid.
     *
     * @param client Client to run example.
     * @throws GridClientException If failed.
     */
    private static void runExample(GridClient client) throws GridClientException {
        System.out.println(">>> Executing task...");
        System.out.println(">>> Task result: " + client.compute().
            execute(ClientExampleTask.class.getName(), "Router example."));
    }

    /**
     * This method will create a client configured to work with locally started router
     * on TCP REST protocol.
     *
     * @return Client instance.
     * @throws GridClientException If client could not be created.
     */
    private static GridClient createTcpClient() throws GridClientException {
        GridClientConfiguration cfg = new GridClientConfiguration();

        // Point client to a local TCP router.
        cfg.setRouters(Collections.singletonList(ROUTER_ADDRESS + ':' + ROUTER_TCP_PORT));

        return GridClientFactory.start(cfg);
    }
}
