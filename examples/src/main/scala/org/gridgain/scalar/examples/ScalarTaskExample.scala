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
import collection.JavaConversions._
import org.gridgain.grid.compute._
import java.util

/**
 * Demonstrates use of full grid task API using Scalar. Note that using task-based
 * grid enabling gives you all the advanced features of GridGain such as custom topology
 * and collision resolution, custom failover, mapping, reduction, load balancing, etc.
 * As a trade off in such cases the more code needs to be written vs. simple closure execution.
 * <p>
 * Remote nodes should always be started with special configuration file which
 * enables P2P class loading: `'ggstart.{sh|bat} examples/config/example-compute.xml'`.
 */
object ScalarTaskExample extends App {
    scalar("examples/config/example-compute.xml") {
        grid$.compute().execute(classOf[GridHelloWorld], "Hello Cloud World!").get
    }

    /**
     * This task encapsulates the logic of MapReduce.
     */
    class GridHelloWorld extends GridComputeTaskSplitAdapter[String, Void] {
        def split(gridSize: Int, arg: String): java.util.Collection[_ <: GridComputeJob] = {
            (for (w <- arg.split(" ")) yield toJob(() => println(w))).toSeq
        }

        def reduce(results: util.List[GridComputeJobResult]) = null
    }
}
