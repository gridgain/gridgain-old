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

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.lang;

import org.gridgain.grid.compute.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.util.*;

/**
 * Defines generic reducer that collects multiple values and reduces them into one.
 * Reducers are useful in computations when results from multiple remote jobs need
 * to be reduced into one, e.g. {@link GridCompute#call(Collection, GridReducer)} method.
 *
 * @param <E> Type of collected values.
 * @param <R> Type of reduced value.
 */
public interface GridReducer<E, R> extends Serializable {
    /**
     * Collects given value. If this method returns {@code false} then {@link #reduce()}
     * will be called right away. Otherwise caller will continue collecting until all
     * values are processed.
     *
     * @param e Value to collect.
     * @return {@code true} to continue collecting, {@code false} to instruct caller to stop
     *      collecting and call {@link #reduce()} method.
     */
    public boolean collect(@Nullable E e);

    /**
     * Reduces collected values into one.
     *
     * @return Reduced value.
     */
    public R reduce();
}
