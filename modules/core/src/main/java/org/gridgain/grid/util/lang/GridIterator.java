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
import org.gridgain.grid.util.*;

import java.util.*;

/**
 * Defines "rich" iterator interface that is also acts like lambda function and iterable.
 * @see GridIterable
 */
public interface GridIterator<T> extends Iterable<T>, GridSerializableIterator<T> {
    /**
     * This method is the same as {@link #hasNext()}, but allows for failure
     * with exception. Often iterators are used to iterate through values
     * that have not or have partially been received from remote nodes,
     * and need to account for possible network failures, rather than
     * just returning {@code false} out of {@link #hasNext()} method.
     *
     * @return {@code True} if iterator contains more elements.
     * @throws GridException If no more elements can be returned due
     *      to some failure, like a network error for example.
     * @see Iterator#hasNext()
     */
    public boolean hasNextX() throws GridException;

    /**
     * This method is the same as {@link #next()}, but allows for failure
     * with exception. Often iterators are used to iterate through values
     * that have not or have partially been received from remote nodes,
     * and need to account for possible network failures, rather than
     * throwing {@link NoSuchElementException} runtime exception.s
     *
     * @return {@code True} if iterator contains more elements.
     * @throws NoSuchElementException If there are no more elements to
     *      return.
     * @throws GridException If no more elements can be returned due
     *      to some failure, like a network error for example.
     * @see Iterator#next()
     */
    public T nextX() throws GridException;

    /**
     * This method is the same as {@link #remove()}, but allows for failure
     * with exception.
     *
     * @throws GridException If failed.
     */
    public void removeX() throws GridException;
}
