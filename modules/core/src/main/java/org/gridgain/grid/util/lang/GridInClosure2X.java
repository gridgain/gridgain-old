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
 * implements {@link #apply(Object, Object)} method that calls {@link #applyx(Object, Object)}
 * method and properly wraps {@link GridException} into {@link GridClosureException} instance.
 * @see CIX2
 */
public abstract class GridInClosure2X<E1, E2> implements GridBiInClosure<E1, E2> {
    /** */
    private static final long serialVersionUID = 0L;

    /** {@inheritDoc} */
    @Override public void apply(E1 e1, E2 e2) {
        try {
            applyx(e1, e2);
        }
        catch (GridException e) {
            throw F.wrap(e);
        }
    }

    /**
     * In-closure body that can throw {@link GridException}.
     *
     * @param e1 First variable the closure is called or closed on.
     * @param e2 Second variable the closure is called or closed on.
     * @throws GridException Thrown in case of any error condition inside of the closure.
     */
    public abstract void applyx(E1 e1, E2 e2) throws GridException;
}