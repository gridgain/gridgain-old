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

package org.gridgain.scalar.lang

import org.gridgain.grid.lang.GridReducer
import collection._

/**
 * Peer deploy aware adapter for Java's `GridReducer`.
 */
class ScalarReducer[E, R](private val r: Seq[E] => R) extends GridReducer[E, R] {
    assert(r != null)

    private val buf = new mutable.ListBuffer[E]

    /**
     * Delegates to passed in function.
     */
    def reduce = r(buf.toSeq)

    /**
     * Collects given value.
     *
     * @param e Value to collect for later reduction.
     */
    def collect(e: E) = {
        buf += e

        true
    }
}
