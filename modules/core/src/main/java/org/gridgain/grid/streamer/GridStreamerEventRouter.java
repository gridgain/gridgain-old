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

package org.gridgain.grid.streamer;

import org.gridgain.grid.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * Streamer event router. Pluggable component that determines event execution flow across the grid.
 * Each time a group of events is submitted to streamer or returned to streamer by a stage, event
 * router will be used to select execution node for next stage.
 */
public interface GridStreamerEventRouter {
    /**
     * Selects a node for given event that should be processed by a stage with given name.
     *
     * @param ctx Streamer context.
     * @param stageName Stage name.
     * @param evt Event to route.
     * @return Node to route to. If this method returns {@code null} then the whole pipeline execution
     *      will be terminated. All running and ongoing stages for pipeline execution will be
     *      cancelled.
     */
    @Nullable public <T> GridNode route(GridStreamerContext ctx, String stageName, T evt);

    /**
     * Selects a node for given events that should be processed by a stage with given name.
     *
     * @param ctx Streamer context.
     * @param stageName Stage name to route events.
     * @param evts Events.
     * @return Events to node mapping. If this method returns {@code null} then the whole pipeline execution
     *      will be terminated. All running and ongoing stages for pipeline execution will be
     *      cancelled.
     */
    @Nullable public <T> Map<GridNode, Collection<T>> route(GridStreamerContext ctx, String stageName,
        Collection<T> evts);
}
