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

import java.util.concurrent.*;

/**
 * Future that does not depend on kernal context.
 */
public interface GridPlainFuture<R> {
    /**
     * Synchronously waits for completion and returns result.
     *
     * @return Completed future result.
     * @throws GridException In case of error.
     */
    public R get() throws GridException;

    /**
     * Synchronously waits for completion and returns result.
     *
     * @param timeout Timeout interval to wait future completes.
     * @param unit Timeout interval unit to wait future completes.
     * @return Completed future result.
     * @throws GridException In case of error.
     * @throws GridFutureTimeoutException If timed out before future finishes.
     */
    public R get(long timeout, TimeUnit unit) throws GridException;

    /**
     * Checks if future is done.
     *
     * @return Whether future is done.
     */
    public boolean isDone();

    /**
     * Register new listeners for notification when future completes.
     *
     * Note that current implementations are calling listeners in
     * the completing thread.
     *
     * @param lsnrs Listeners to be registered.
     */
    public void listenAsync(GridPlainInClosure<GridPlainFuture<R>>... lsnrs);

    /**
     * Removes listeners registered before.
     *
     * @param lsnrs Listeners to be removed.
     */
    public void stopListenAsync(GridPlainInClosure<GridPlainFuture<R>>... lsnrs);

    /**
     * Creates a future that will be completed after this future is completed. The result of
     * created future is value returned by {@code cb} closure invoked on this future.
     *
     * @param cb Callback closure.
     * @return Chained future.
     */
    public <T> GridPlainFuture<T> chain(GridPlainClosure<GridPlainFuture<R>, T> cb);
}
