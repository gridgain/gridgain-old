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
import scala.util.control.Breaks._
import java.util

/**
 * Prime Number calculation example based on Scalar.
 *
 * ==Starting Remote Nodes==
 * To try this example you should (but don't have to) start remote grid instances.
 * You can start as many as you like by executing the following script:
 * `{GRIDGAIN_HOME}/bin/ggstart.{bat|sh} examples/config/example-compute.xml`
 *
 * Once remote instances are started, you can execute this example from
 * Eclipse, IntelliJ IDEA, or NetBeans (and any other Java IDE) by simply hitting run
 * button. You will see that all nodes discover each other and
 * all of the nodes will participate in task execution (check node
 * output).
 *
 * Note that when running this example on a multi-core box, simply
 * starting additional grid node on the same box will speed up
 * prime number calculation by a factor of 2.
 */
object ScalarPrimeExample {
    /**
     * Main entry point to application. No arguments required.
     *
     * @param args Command like argument (not used).
     */
    def main(args: Array[String]){
        scalar("examples/config/example-compute.xml") {
            val start = System.currentTimeMillis

            // Values we want to check for prime.
            val checkVals = Array(32452841L, 32452843L, 32452847L, 32452849L, 236887699L, 217645199L)

            println(">>>")
            println(">>> Starting to check the following numbers for primes: " + util.Arrays.toString(checkVals))

            val g = grid$

            checkVals.foreach(checkVal => {
                val divisor = g.reduce$[Option[Long], Option[Option[Long]]](
                    closures(g.nodes().size(), checkVal), _.find(_.isDefined), null)

                if (!divisor.isDefined)
                    println(">>> Value '" + checkVal + "' is a prime number")
                else
                    println(">>> Value '" + checkVal + "' is divisible by '" + divisor.get.get + '\'')
            })

            val totalTime = System.currentTimeMillis - start

            println(">>> Total time to calculate all primes (milliseconds): " + totalTime)
            println(">>>")
        }
    }

    /**
     * Creates closures for checking passed in value for prime.
     *
     * Every closure gets a range of divisors to check. The lower and
     * upper boundaries of this range are passed into closure.
     * Closures checks if the value passed in is divisible by any of
     * the divisors in the range.
     *
     * @param gridSize Size of the grid.
     * @param checkVal Value to check.
     * @return Collection of closures.
     */
    private def closures(gridSize: Int, checkVal: Long): Seq[() => Option[Long]] = {
        var cls = Seq.empty[() => Option[Long]]

        val taskMinRange = 2L
        val numbersPerTask = if (checkVal / gridSize < 10) 10L else checkVal / gridSize

        var minRange = 0L
        var maxRange = 0L

        var i = 0

        while (maxRange < checkVal) {
            minRange = i * numbersPerTask + taskMinRange
            maxRange = (i + 1) * numbersPerTask + taskMinRange - 1

            if (maxRange > checkVal)
                maxRange = checkVal

            val min = minRange
            val max = maxRange

            cls +:= (() => {
                var divisor: Option[Long] = None

                breakable {
                    (min to max).foreach(d => {
                        if (d != 1 && d != checkVal && checkVal % d == 0) {
                             divisor = Some(d)

                             break()
                        }
                    })
                }

                divisor
            })

            i += 1
        }

        cls
    }
}
