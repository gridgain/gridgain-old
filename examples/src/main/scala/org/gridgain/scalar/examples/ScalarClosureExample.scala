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
import org.gridgain.grid._

/**
 * Demonstrates various closure executions on the cloud using Scalar.
 * <p>
 * Remote nodes should always be started with special configuration file which
 * enables P2P class loading: `'ggstart.{sh|bat} examples/config/example-compute.xml'`.
 */
object ScalarClosureExample extends App {
    scalar("examples/config/example-compute.xml") {
        topology()
        helloWorld()
        helloWorld2()
        broadcast()
        println("Count of non-whitespace is: " + count("Scalar is cool!"))
        greetRemotes()
        greetRemotesAgain()
    }

    /**
     * Prints grid topology.
     */
    def topology() {
        grid$ foreach (n => println("Node: " + nid8$(n)))
    }

    /**
     * Obligatory example (2) - cloud enabled Hello World!
     */
    def helloWorld2() {
        // Notice the example usage of Java-side closure 'F.println(...)' and method 'scala'
        // that explicitly converts Java side object to a proper Scala counterpart.
        // This method is required since implicit conversion won't be applied here.
        grid$.run$(for (w <- "Hello World!".split(" ")) yield () => println(w), null)
    }

    /**
     * Obligatory example - cloud enabled Hello World!
     */
    def helloWorld() {
        grid$.run$("HELLO WORLD!".split(" ") map (w => () => println(w)), null)
    }

    /**
     * One way to execute closures on the grid.
     */
    def broadcast() {
        grid$.bcastRun(() => println("Broadcasting!!!"), null)
    }

    /**
     * Count non-whitespace characters by spreading workload to the cloud and reducing
     * on the local node.
     */
    // Same as 'count2' but with for-expression.
    def count(msg: String): Int =
        grid$.reduce$[Int, Int](for (w <- msg.split(" ")) yield () => w.length, _.sum, null)

    /**
     * Count non-whitespace characters by spreading workload to the cloud and reducing
     * on the local node.
     */
    // Same as 'count' but without for-expression.
    // Note that map's parameter type inference doesn't work in 2.9.0.
    def count2(msg: String): Int =
        grid$.reduce$[Int, Int](msg.split(" ") map (s => () => s.length), _.sum, null)

    /**
     *  Greats all remote nodes only.
     */
    def greetRemotes() {
        val me = grid$.localNode.id

        // Note that usage Java-based closure.
        grid$.forRemotes() match {
            case p if p.isEmpty => println("No remote nodes!")
            case p => p.bcastRun(() => println("Greetings from: " + me), null)
        }
    }

    /**
     * Same as previous greetings for all remote nodes but remote projection is created manually.
     */
    def greetRemotesAgain() {
        val me = grid$.localNode.id

        // Just show that we can create any projections we like...
        // Note that usage of Java-based closure via 'F' typedef.
        grid$.forPredicate((n: GridNode) => n.id != me) match {
            case p if p.isEmpty => println("No remote nodes!")
            case p => p.bcastRun(() => println("Greetings again from: " + me), null)
        }
    }
}
