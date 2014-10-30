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

package org.gridgain.examples.messaging;

import org.gridgain.examples.*;
import org.gridgain.grid.*;
import org.gridgain.grid.messaging.*;

import java.util.*;
import java.util.concurrent.*;

/**
 * Demonstrates messaging with {@link GridMessagingListenActor} convenience adapter.
 * <p>
 * To run this example you must have at least one remote node started.
 * <p>
 * Remote nodes should always be started with special configuration file which
 * enables P2P class loading: {@code 'ggstart.{sh|bat} examples/config/example-compute.xml'}.
 * <p>
 * Alternatively you can run {@link ComputeNodeStartup} in another JVM which will start GridGain node
 * with {@code examples/config/example-compute.xml} configuration.
 */
public class MessagingPingPongListenActorExample {
    /**
     * Executes example.
     *
     * @param args Command line arguments, none required.
     * @throws GridException If example execution failed.
     */
    public static void main(String[] args) throws GridException {
        // Game is played over the default grid.
        try (Grid g = GridGain.start("examples/config/example-compute.xml")) {
            if (!ExamplesUtils.checkMinTopologySize(g, 2))
                return;

            System.out.println();
            System.out.println(">>> Messaging ping-pong listen actor example started.");

            // Pick first remote node as a partner.
            Collection<GridNode> rmtNodes = g.forRemotes().nodes();

            GridProjection nodeB = g.forNode(rmtNodes.iterator().next());

            // Note that both nodeA and nodeB will always point to
            // same nodes regardless of whether they were implicitly
            // serialized and deserialized on another node as part of
            // anonymous closure's state during its remote execution.

            // Set up remote player.
            nodeB.message().remoteListen(null, new GridMessagingListenActor<String>() {
                @Override public void receive(UUID nodeId, String rcvMsg) throws GridException {
                    System.out.println(rcvMsg);

                    if ("PING".equals(rcvMsg))
                        respond("PONG");
                    else if ("STOP".equals(rcvMsg))
                        stop();
                }
            }).get();

            int MAX_PLAYS = 10;

            final CountDownLatch cnt = new CountDownLatch(MAX_PLAYS);

            // Set up local player.
            g.message().localListen(null, new GridMessagingListenActor<String>() {
                @Override protected void receive(UUID nodeId, String rcvMsg) throws GridException {
                    System.out.println(rcvMsg);

                    if (cnt.getCount() == 1)
                        stop("STOP");
                    else if ("PONG".equals(rcvMsg))
                        respond("PING");

                    cnt.countDown();
                }
            });

            // Serve!
            nodeB.message().send(null, "PING");

            // Wait til the game is over.
            try {
                cnt.await();
            }
            catch (InterruptedException e) {
                System.err.println("Hm... let us finish the game!\n" + e);
            }
        }
    }
}
