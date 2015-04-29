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

package org.gridgain.grid.kernal.processors.service;

import java.util.*;

/**
 * Method reflection key.
 */
class GridServiceMethodReflectKey {
    /** Method name. */
    private final String mtdName;

    /** Argument types. */
    private final Class<?>[] argTypes;

    /** Hash code. */
    private int hash;

    /**
     * @param mtdName Method name.
     * @param argTypes Argument types.
     */
    GridServiceMethodReflectKey(String mtdName, Class<?>[] argTypes) {
        assert mtdName != null;

        this.mtdName = mtdName;
        this.argTypes = argTypes;
    }

    /**
     * @return Method name.
     */
    String methodName() {
        return mtdName;
    }

    /**
     * @return Arg types.
     */
    Class<?>[] argTypes() {
        return argTypes;
    }

    /** {@inheritDoc} */
    @Override public boolean equals(Object o) {
        if (this == o)
            return true;

        GridServiceMethodReflectKey key = (GridServiceMethodReflectKey)o;

        return mtdName.equals(key.mtdName) && Arrays.equals(argTypes, key.argTypes);
    }

    /** {@inheritDoc} */
    @Override public int hashCode() {
        if (hash != 0)
            return hash;

        return hash = 31 * mtdName.hashCode() + Arrays.hashCode(argTypes);
    }
}
