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

package org.gridgain.grid.kernal.processors.interop;

import org.gridgain.grid.*;
import org.gridgain.grid.kernal.processors.*;
import org.jetbrains.annotations.*;

/**
 * Interop processor.
 */
public interface GridInteropProcessor extends GridProcessor {
    /**
     * Release start latch.
     */
    public void releaseStart();

    /**
     * Await start on native side.
     *
     * @throws GridException If failed.
     */
    public void awaitStart() throws GridException;

    /**
     * @return Environment pointer.
     */
    public long environmentPointer() throws GridException;

    /**
     * @return Grid name.
     */
    public String gridName();

    /**
     * Gets native wrapper for default Grid projection.
     *
     * @return Native compute wrapper.
     * @throws GridException If failed.
     */
    public GridInteropTarget projection() throws GridException;

    /**
     * Gets native wrapper for cache with the given name.
     *
     * @param name Cache name ({@code null} for default cache).
     * @return Native cache wrapper.
     * @throws GridException If failed.
     */
    public GridInteropTarget cache(@Nullable String name) throws GridException;

    /**
     * Gets native wrapper for data loader for cache with the given name.
     *
     * @param cacheName Cache name ({@code null} for default cache).
     * @return Native data loader wrapper.
     * @throws GridException If failed.
     */
    public GridInteropTarget dataLoader(@Nullable String cacheName) throws GridException;

    /**
     * Stops grid.
     *
     * @param cancel Cancel flag.
     */
    public void close(boolean cancel);
}
