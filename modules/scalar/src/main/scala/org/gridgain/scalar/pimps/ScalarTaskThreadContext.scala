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
import org.gridgain.scalar._
import org.jetbrains.annotations._

/**
 * This trait provide mixin for properly typed version of `GridProjection#with...()` methods.
 *
 * Method on `GridProjection` always returns an instance of type `GridProjection` even when
 * called on a sub-class. This trait's methods return the instance of the same type
 * it was called on.
 */
trait ScalarTaskThreadContext[T <: GridProjection] extends ScalarConversions { this: PimpedType[T] =>
    /**
     * Properly typed version of `GridCompute#withName(...)` method.
     *
     * @param taskName Name of the task.
     */
    def withName$(@Nullable taskName: String): T =
        value.compute().withName(taskName).asInstanceOf[T]

    /**
     * Properly typed version of `GridCompute#withNoFailover()` method.
     */
    def withNoFailover$(): T =
        value.compute().withNoFailover().asInstanceOf[T]
}
