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

package org.gridgain.grid.util.lang;

import org.gridgain.grid.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.util.typedef.*;

/**
 * Convenient in-closure subclass that allows for thrown grid exception. This class
 * implements {@link #apply(Object)} method that calls {@link #applyx(Object)} method
 * and properly wraps {@link GridException} into {@link GridClosureException} instance.
 * @see CIX1
 */
public abstract class GridInClosureX<T> implements GridInClosure<T> {
    /** */
    private static final long serialVersionUID = 0L;

    /** {@inheritDoc} */
    @Override public void apply(T t) {
        try {
            applyx(t);
        }
        catch (GridException e) {
            throw F.wrap(e);
        }
    }

    /**
     * In-closure body that can throw {@link GridException}.
     *
     * @param t The variable the closure is called or closed on.
     * @throws GridException Thrown in case of any error condition inside of the closure.
     */
    public abstract void applyx(T t) throws GridException;
}
