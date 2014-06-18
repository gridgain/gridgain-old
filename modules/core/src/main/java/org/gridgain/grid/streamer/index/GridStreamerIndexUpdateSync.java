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

package org.gridgain.grid.streamer.index;

import org.gridgain.grid.util.typedef.internal.*;

/**
 * Streamer index update synchronizer.
 * <p>
 * Used in {@link GridStreamerIndexProvider} to synchronize
 * operations on window index.
 *
 * @see GridStreamerIndexProvider
 *
 */
public class GridStreamerIndexUpdateSync {
    /** */
    private volatile int res;

    /**
     * Waits for a notification from another thread, which
     * should call {@link #finish(int)} with an operation result.
     * That result is returned by this method.
     *
     * @return Operation results, passed to {@link #finish(int)}.
     * @throws InterruptedException If wait was interrupted.
     */
    public int await() throws InterruptedException {
        int res0 = res;

        if (res0 == 0) {
            synchronized (this) {
                while ((res0 = res) == 0)
                    wait();
            }
        }

        assert res0 != 0;

        return res0;
    }

    /**
     * Notifies all waiting threads to finish waiting.
     *
     * @param res Operation result to return from {@link #await()}.
     */
    public void finish(int res) {
        assert res != 0;

        synchronized (this) {
            this.res = res;

            notifyAll();
        }
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridStreamerIndexUpdateSync.class, this);
    }
}
