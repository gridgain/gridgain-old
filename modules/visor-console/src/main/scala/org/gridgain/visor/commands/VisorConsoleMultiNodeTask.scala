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
 * ___    _________________________ ________
 * __ |  / /____  _/__  ___/__  __ \___  __ \
 * __ | / /  __  /  _____ \ _  / / /__  /_/ /
 * __ |/ /  __/ /   ____/ / / /_/ / _  _, _/
 * _____/   /___/   /____/  \____/  /_/ |_|
 *
 */

package org.gridgain.visor.commands

import java.util
import java.util.{HashMap => JavaHashMap}
import collection.JavaConversions._
import org.gridgain.grid._
import org.gridgain.grid.compute.GridComputeJobResultPolicy._
import org.gridgain.grid.compute.{GridComputeJobResultPolicy, GridComputeJobResult, GridComputeJob, GridComputeTask}
import org.gridgain.grid.kernal.processors.task.GridInternal

/**
 * Base class for task executing on several nodes.
 *
 * @tparam A Task argument type.
 * @tparam R Task result type.
 */
@GridInternal
trait VisorConsoleMultiNodeTask[A, R] extends GridComputeTask[A, R] {
    /**
     * Create job for specified argument.
     *
     * @param arg Job argument.
     * @return Job that will be mapped to nodes.
     */
    protected def job(arg: A): GridComputeJob

    override def map(subgrid: util.List[GridNode], arg: A): util.Map[_ <: GridComputeJob, GridNode] = {
        val map = new JavaHashMap[GridComputeJob, GridNode](subgrid.size)

        subgrid.foreach(map.put(job(arg), _))

        map
    }

    override def result(res: GridComputeJobResult, rcvd: util.List[GridComputeJobResult]): GridComputeJobResultPolicy = {
        // All Visor tasks should handle exceptions in reduce method.
        WAIT
    }
}
