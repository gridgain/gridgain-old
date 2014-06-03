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

package org.gridgain.grid.cache.query;

import org.gridgain.grid.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * Cache query future returned by query execution.
 * Refer to {@link GridCacheQuery} documentation for more information.
 */
public interface GridCacheQueryFuture<T> extends GridFuture<Collection<T>> {
    /**
     * Returns number of elements that are already fetched and can
     * be returned from {@link #next()} method without blocking.
     *
     * @return Number of fetched elements which are available immediately.
     * @throws GridException In case of error.
     */
    public int available() throws GridException;

    /**
     * Returns next element from result set.
     * <p>
     * This is a blocking call which will wait if there are no
     * elements available immediately.
     *
     * @return Next fetched element or {@code null} if all the elements.
     * @throws GridException If failed.
     */
    @Nullable public T next() throws GridException;

    /**
     * Checks if all data is fetched by the query.
     *
     * @return {@code True} if all data is fetched, {@code false} otherwise.
     */
    @Override public boolean isDone();

    /**
     * Cancels this query future and stop receiving any further results for the query
     * associated with this future.
     *
     * @return {@inheritDoc}
     * @throws GridException {@inheritDoc}
     */
    @Override public boolean cancel() throws GridException;
}
