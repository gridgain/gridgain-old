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

/*
 * ________               ______                    ______   _______
 * __  ___/_____________ ____  /______ _________    __/__ \  __  __ \
 * _____ \ _  ___/_  __ `/__  / _  __ `/__  ___/    ____/ /  _  / / /
 * ____/ / / /__  / /_/ / _  /  / /_/ / _  /        _  __/___/ /_/ /
 * /____/  \___/  \__,_/  /_/   \__,_/  /_/         /____/_(_)____/
 *
 */

package org.gridgain.scalar.examples

import org.gridgain.scalar.scalar
import scalar._
import java.util.UUID
import java.util.concurrent.CountDownLatch
import org.gridgain.grid.messaging._

/**
 * Demonstrates simple protocol-based exchange in playing a ping-pong between
 * two nodes. It is analogous to `GridMessagingPingPongExample` on Java side.
 * <p>
 * Remote nodes should always be started with special configuration file which
 * enables P2P class loading: `'ggstart.{sh|bat} examples/config/example-compute.xml'`.
 */
object ScalarPingPongExample extends App {
    scalar("examples/config/example-compute.xml") {
        pingPong()
        //pingPong2()
    }

    /**
     * Implements Ping Pong example between local and remote node.
     */
    def pingPong() {
        val g = grid$

        if (g.nodes().size < 2) {
            println(">>>")
            println(">>> I need a partner to play a ping pong!")
            println(">>>")

            return
        }
        else {
            // Pick first remote node as a partner.
            val nodeB = g.forNode(g.remoteNodes$().head)

            // Set up remote player: configure remote node 'rmt' to listen
            // for messages from local node 'loc'.
            nodeB.message().remoteListen(null, new GridMessagingListenActor[String]() {
                def receive(nodeId: UUID, msg: String) {
                    println(msg)

                    msg match {
                        case "PING" => respond("PONG")
                        case "STOP" => stop()
                    }
                }
            }).get()

            val latch = new CountDownLatch(10)

            // Set up local player: configure local node 'loc'
            // to listen for messages from remote node 'rmt'.
            grid$.message().localListen(null, new GridMessagingListenActor[String]() {
                def receive(nodeId: UUID, msg: String) {
                    println(msg)

                    if (latch.getCount == 1)
                        stop("STOP")
                    else // We know it's 'PONG'.
                        respond("PING")

                    latch.countDown()
                }
            })

            // Serve!
            nodeB.send$("PING", null)

            // Wait til the match is over.
            latch.await()
        }
    }

    /**
     * Implements Ping Pong example between two remote nodes.
     */
    def pingPong2() {
        val g = grid$

        if (g.forRemotes().nodes().size() < 2) {
            println(">>>")
            println(">>> I need at least two remote nodes!")
            println(">>>")
        }
        else {
            // Pick two remote nodes.
            val n1 = g.forRemotes().head
            val n2 = g.forRemotes().tail.head

            val n1p = g.forNode(n1)
            val n2p = g.forNode(n2)

            // Configure remote node 'n1' to receive messages from 'n2'.
            n1p.message().remoteListen(null, new GridMessagingListenActor[String] {
                def receive(nid: UUID, msg: String) {
                    println(msg)

                    msg match {
                        case "PING" => respond("PONG")
                        case "STOP" => stop()
                    }
                }
            }).get

            // Configure remote node 'n2' to receive messages from 'n1'.
            n2p.message().remoteListen(null, new GridMessagingListenActor[String] {
                // Get local count down latch.
                private lazy val latch: CountDownLatch = g.nodeLocalMap().get("latch")

                def receive(nid: UUID, msg: String) {
                    println(msg)

                    latch.getCount match {
                        case 1 => stop("STOP")
                        case _ => respond("PING")
                    }

                    latch.countDown()
                }
            }).get

            // 1. Sets latch into node local storage so that local actor could use it.
            // 2. Sends first 'PING' to 'n1'.
            // 3. Waits until all messages are exchanged between two remote nodes.
            n2p.run$(() => {
                val latch = new CountDownLatch(10)

                g.nodeLocalMap[String, CountDownLatch].put("latch", latch)

                n1p.send$("PING", null)

                latch.await()
            }, null)
        }
    }
}
