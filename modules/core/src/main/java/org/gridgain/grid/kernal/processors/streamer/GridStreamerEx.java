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

package org.gridgain.grid.kernal.processors.streamer;

import org.gridgain.grid.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.streamer.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * Extended streamer context with methods intended for internal use.
 */
public interface GridStreamerEx extends GridStreamer {
    /**
     * @return Kernal context.
     */
    public GridKernalContext kernalContext();

    /**
     * Gets streamer default window (the first one in configuration list).
     *
     * @return Streamer window.
     */
    public <E> GridStreamerWindow<E> window();

    /**
     * Gets streamer window by window name.
     *
     * @param windowName Window name.
     * @return Streamer window.
     */
    @Nullable public <E> GridStreamerWindow<E> window(String windowName);

    /**
     * Called before execution requests are sent to remote nodes or scheduled for local execution.
     *
     * @param fut Future.
     */
    public void onFutureMapped(GridStreamerStageExecutionFuture fut);

    /**
     * Called when future is completed and parent should be notified, if any.
     *
     * @param fut Future.
     */
    public void onFutureCompleted(GridStreamerStageExecutionFuture fut);

    /**
     * @return Streamer event router.
     */
    public GridStreamerEventRouter eventRouter();

    /**
     * Schedules batch executions either on local or on remote nodes.
     *
     * @param fut Future.
     * @param execs Executions grouped by node ID.
     * @throws GridException If failed.
     */
    public void scheduleExecutions(GridStreamerStageExecutionFuture fut, Map<UUID, GridStreamerExecutionBatch> execs)
        throws GridException;

    /**
     * Callback for undeployed class loaders. All deployed events will be removed from window and local storage.
     *
     * @param leftNodeId Left node ID which caused undeployment.
     * @param undeployedLdr Undeployed class loader.
     */
    public void onUndeploy(UUID leftNodeId, ClassLoader undeployedLdr);

    /**
     * Callback executed when streamer query completes.
     *
     * @param time Consumed time.
     * @param nodes Participating nodes count.
     */
    public void onQueryCompleted(long time, int nodes);
}
