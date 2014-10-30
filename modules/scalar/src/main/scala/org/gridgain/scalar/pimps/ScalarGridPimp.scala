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

package org.gridgain.scalar.pimps

import org.gridgain.grid._
import org.jetbrains.annotations.Nullable
import org.gridgain.grid.scheduler.GridSchedulerFuture

/**
 * Companion object.
 */
object ScalarGridPimp {
    /**
     * Creates new Scalar grid pimp with given Java-side implementation.
     *
     * @param impl Java-side implementation.
     */
    def apply(impl: Grid) = {
        if (impl == null)
            throw new NullPointerException("impl")

        val pimp = new ScalarGridPimp

        pimp.impl = impl

        pimp
    }
}

/**
 * ==Overview==
 * Defines Scalar "pimp" for `Grid` on Java side.
 *
 * Essentially this class extends Java `GridProjection` interface with Scala specific
 * API adapters using primarily implicit conversions defined in `ScalarConversions` object. What
 * it means is that you can use functions defined in this class on object
 * of Java `GridProjection` type. Scala will automatically (implicitly) convert it into
 * Scalar's pimp and replace the original call with a call on that pimp.
 *
 * Note that Scalar provide extensive library of implicit conversion between Java and
 * Scala GridGain counterparts in `ScalarConversions` object
 *
 * ==Suffix '$' In Names==
 * Symbol `$` is used in names when they conflict with the names in the base Java class
 * that Scala pimp is shadowing or with Java package name that your Scala code is importing.
 * Instead of giving two different names to the same function we've decided to simply mark
 * Scala's side method with `$` suffix.
 */
class ScalarGridPimp extends ScalarProjectionPimp[Grid] with ScalarTaskThreadContext[Grid] {
    /**
     * Schedules closure for execution using local cron-based scheduling.
     *
     * @param s Closure to schedule to run as a background cron-based job.
     * @param ptrn  Scheduling pattern in UNIX cron format with optional prefix `{n1, n2}`
     *     where `n1` is delay of scheduling in seconds and `n2` is the number of execution. Both
     *     parameters are optional.
     */
    def scheduleLocalCall[R](@Nullable s: Call[R], ptrn: String): GridSchedulerFuture[R] = {
        assert(ptrn != null)

        value.grid().scheduler().scheduleLocal(toCallable(s), ptrn)
    }

    /**
     * Schedules closure for execution using local cron-based scheduling.
     *
     * @param s Closure to schedule to run as a background cron-based job.
     * @param ptrn  Scheduling pattern in UNIX cron format with optional prefix `{n1, n2}`
     *     where `n1` is delay of scheduling in seconds and `n2` is the number of execution. Both
     *     parameters are optional.
     */
    def scheduleLocalRun(@Nullable s: Run, ptrn: String): GridSchedulerFuture[_] = {
        assert(ptrn != null)

        value.grid().scheduler().scheduleLocal(toRunnable(s), ptrn)
    }
}
