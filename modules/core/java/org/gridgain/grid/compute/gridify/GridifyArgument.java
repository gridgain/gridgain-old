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

import org.gridgain.grid.compute.gridify.aop.*;
import java.io.*;

/**
 * Gridify task argument created by the system for task execution. It contains
 * all information needed to reflectively execute a method remotely.
 * <p>
 * Use {@link GridifyArgumentAdapter} convenience adapter for creating gridify
 * arguments when implementing custom gridify jobs.
 * <p>
 * See {@link Gridify} documentation for more information about execution of
 * {@code gridified} methods.
 * @see Gridify
 */
public interface GridifyArgument extends Serializable {
    /**
     * Gets class to which the executed method belongs.
     *
     * @return Class to which method belongs.
     */
    public Class<?> getMethodClass();

    /**
     * Gets method name.
     *
     * @return Method name.
     */
    public String getMethodName();

    /**
     * Gets method parameter types in the same order they appear in method
     * signature.
     *
     * @return Method parameter types.
     */
    public Class<?>[] getMethodParameterTypes();

    /**
     * Gets method parameters in the same order they appear in method
     * signature.
     *
     * @return Method parameters.
     */
    public Object[] getMethodParameters();

    /**
     * Gets target object to execute method on. {@code Null} for static methods.
     *
     * @return Execution state (possibly {@code null}), required for remote
     *      object creation.
     */
    public Object getTarget();
}
