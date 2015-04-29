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

package org.gridgain.grid.compute.gridify;

import java.lang.annotation.*;

/**
 * This annotation can be applied to method parameter for grid-enabled method. This annotation can be used
 * when using when using annotations {@link GridifySetToValue} and {@link GridifySetToSet}.
 * Note that annotations {@link GridifySetToValue} and {@link GridifySetToSet} can be applied
 * to methods with a different number of parameters of the following types
 * <ul>
 * <li>java.util.Collection</li>
 * <li>java.util.Iterator</li>
 * <li>java.util.Enumeration</li>
 * <li>java.lang.CharSequence</li>
 * <li>java array</li>
 * </ul>
 * If grid-enabled method contains several parameters with types described above
 * then GridGain searches for parameters with {@link GridifyInput} annotation.
 * <b>Only one</b> method parameter with {@link GridifyInput} annotation allowed.
 * @see GridifySetToValue
 * @see GridifySetToSet
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface GridifyInput {
    // No-op.
}
