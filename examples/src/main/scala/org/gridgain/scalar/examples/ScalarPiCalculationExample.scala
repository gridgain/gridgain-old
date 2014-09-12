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

import org.gridgain.scalar._
import scalar._
import scala.math._

/**
 * This example calculates Pi number in parallel on the grid. Note that these few
 * lines of code work on one node, two nodes or hundreds of nodes without any changes
 * or any explicit deployment.
 * <p>
 * Remote nodes should always be started with special configuration file which
 * enables P2P class loading: `'ggstart.{sh|bat} examples/config/example-compute.xml'`.
 */
object ScalarPiCalculationExample {
    /** Number of iterations per node. */
    private val N = 10000

    def main(args: Array[String]) {
        scalar("examples/config/example-compute.xml") {
            val jobs = for (i <- 0 until grid$.nodes().size()) yield () => calcPi(i * N)

            println("Pi estimate: " + grid$.reduce$[Double, Double](jobs, _.sum, null))
        }
    }

    /**
      * Calculates Pi range starting with given number.
      *
      * @param start Start the of the `{start, start + N}` range.
      * @return Range calculation.
      */
    def calcPi(start: Int): Double =
        // Nilakantha algorithm.
        ((max(start, 1) until (start + N)) map (i => 4.0 * (2 * (i % 2) - 1) / (2 * i) / (2 * i + 1) / (2 * i + 2)))
            .sum + (if (start == 0) 3 else 0)
}
