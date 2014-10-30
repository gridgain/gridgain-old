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

package org.gridgain.grid.util.gridify;

import org.gridgain.grid.*;
import org.gridgain.grid.compute.*;
import org.gridgain.grid.compute.gridify.*;

import java.lang.reflect.*;

/**
 * Convenience adapter for custom {@code gridify} jobs. In addition to
 * functionality provided in {@link GridComputeJobAdapter} adapter, this adapter
 * provides default implementation of {@link #execute()} method,
 * which reflectively executes grid-enabled method based on information provided
 * in {@link GridifyArgument} parameter.
 * <p>
 * Note this adapter is only useful when passing {@link GridifyArgument} to
 * remote jobs. In many cases, remote jobs will not require {@link GridifyArgument}
 * as they will execute their code without reflection, hence the regular
 * {@link GridComputeJobAdapter} should be used.
 * <p>
 * See {@link Gridify} documentation for more information about execution of
 * {@code gridified} methods.
 * @see Gridify
 */
public class GridifyJobAdapter extends GridComputeJobAdapter {
    /** */
    private static final long serialVersionUID = 0L;

    /**
     * Initializes job with argument.
     *
     * @param arg Job argument.
     */
    public GridifyJobAdapter(GridifyArgument arg) {
        super(arg);
    }

    /**
     * Provides default implementation for execution of grid-enabled methods.
     * This method assumes that argument passed in is of {@link GridifyArgument}
     * type. It attempts to reflectively execute a method based on information
     * provided in the argument and returns the return value of the method.
     * <p>
     * If some exception occurred during execution, then it will be thrown
     * out of this method.
     *
     * @return {@inheritDoc}
     * @throws GridException {@inheritDoc}
     */
    @Override public Object execute() throws GridException {
        GridifyArgument arg = argument(0);

        try {
            // Get public, package, protected, or private method.
            Method mtd = arg.getMethodClass().getDeclaredMethod(arg.getMethodName(), arg.getMethodParameterTypes());

            // Attempt to soften access control in case we grid-enabling
            // non-accessible method. Subject to security manager setting.
            if (!mtd.isAccessible())
                try {
                    mtd.setAccessible(true);
                }
                catch (SecurityException e) {
                    throw new GridException("Got security exception when attempting to soften access control for " +
                        "@Gridify method: " + mtd, e);
                }

            Object obj = null;

            // No need to create an instance for static methods.
            if (!Modifier.isStatic(mtd.getModifiers()))
                // Obtain instance to execute method on.
                obj = arg.getTarget();

            return mtd.invoke(obj, arg.getMethodParameters());
        }
        catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof GridException)
                throw (GridException)e.getTargetException();

            throw new GridException("Failed to invoke a method due to user exception.", e.getTargetException());
        }
        catch (IllegalAccessException e) {
            throw new GridException("Failed to access method for execution.", e);
        }
        catch (NoSuchMethodException e) {
            throw new GridException("Failed to find method for execution.", e);
        }
    }
}
