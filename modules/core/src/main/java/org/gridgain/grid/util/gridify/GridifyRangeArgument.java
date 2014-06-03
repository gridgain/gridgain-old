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

import org.gridgain.grid.compute.gridify.*;
import org.gridgain.grid.compute.gridify.aop.*;
import org.gridgain.grid.util.typedef.internal.*;
import java.util.*;

/**
 * Convenience adapter for {@link GridifyArgument} interface.
 * This adapter used in grid task for {@link GridifySetToSet} and
 * {@link GridifySetToValue} annotations.
 * <p>
 * See {@link Gridify} documentation for more information about execution of
 * {@code gridified} methods.
 * @see GridifySetToValue
 * @see GridifySetToSet
 */
public class GridifyRangeArgument extends GridifyArgumentAdapter {
    /** */
    private static final long serialVersionUID = 0L;

    /** Identify where to find data in method signature. */
    private int paramIdx = -1;

    /** Method return type. */
    private Class<?> mtdReturnType;

    /**
     * Gets index identifies where to find data in method signature.
     *
     * @return Index.
     */
    public int getParamIndex() {
        return paramIdx;
    }

    /**
     * Sets index identifies where to find data in method signature.
     *
     * @param paramIdx Index.
     */
    public void setParamIndex(int paramIdx) {
        this.paramIdx = paramIdx;
    }

    /**
     * Gets method return type in the same order they appear in method
     * signature.
     *
     * @return Method return type.
     */
    public Class<?> getMethodReturnType() {
        return mtdReturnType;
    }

    /**
     * Sets method return type.
     *
     * @param mtdReturnType Method return type.
     */
    public void setMethodReturnType(Class<?> mtdReturnType) {
        this.mtdReturnType = mtdReturnType;
    }

    /**
     * Returns elements {@link Iterator} for current input argument.
     * @return Iterator.
     */
    public Iterator<?> getInputIterator() {
        return GridifyUtils.getIterator(getMethodParameters()[paramIdx]);
    }

    /**
     * Returns elements size for current input argument or
     * {@link GridifyUtils#UNKNOWN_SIZE} for unknown input size.
     *
     * @return Elements size for current input argument or {@link GridifyUtils#UNKNOWN_SIZE}.
     */
    public int getInputSize() {
        return GridifyUtils.getLength(getMethodParameters()[paramIdx]);
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridifyRangeArgument.class, this);
    }
}
