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

import org.gridgain.grid.cache._

/**
 * ==Overview==
 * Defines Scalar "pimp" for `GridCache` on Java side.
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
class ScalarCachePimp[K, V] extends ScalarCacheProjectionPimp[K, V] with Ordered[GridCache[K, V]] {
    /**
     * Compares this cache name to the given cache name.
     *
     * @param that Another cache instance to compare names with.
     */
    def compare(that: GridCache[K, V]): Int = that.name.compareTo(value.name)
}

/**
 * Companion object.
 */
object ScalarCachePimp {
    /**
     * Creates new Scalar cache pimp with given Java-side implementation.
     *
     * @param impl Java-side implementation.
     */
    def apply[K, V](impl: GridCache[K, V]) = {
        if (impl == null)
            throw new NullPointerException("impl")

        val pimp = new ScalarCachePimp[K, V]

        pimp.impl = impl

        pimp
    }
}