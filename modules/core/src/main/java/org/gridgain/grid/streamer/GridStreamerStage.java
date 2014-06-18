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
 * Streamer stage is a component that determines event processing flow. User logic related to
 * any particular event processing is implemented by streamer stage. A stage takes events as
 * an input and returns groups of events mapped to different stages as an output. Events for
 * every returned stage will be passed to {@link GridStreamerEventRouter} which will determine
 * on which node the stage should be executed.
 * <p>
 * Generally, event stage execution graph if fully controlled by return values of
 * this method, while node execution graph is controlled by
 * {@link GridStreamerEventRouter#route(GridStreamerContext, String, Object)} method.
 */
public interface GridStreamerStage<IN> {
    /**
     * Gets streamer stage name.
     *
     * @return Name of the stage.
     */
    public String name();

    /**
     * Stage execution routine. After the passed in events are processed, stage can emit
     * another set of events to be processed. The returned events can be mapped to different
     * stages. Events for every returned stage will be passed to {@link GridStreamerEventRouter}
     * which will determine on which node the stage should be executed.
     * <p>
     * Generally, event stage execution graph if fully controlled by return values of
     * this method, while node execution graph is controlled by
     * {@link GridStreamerEventRouter#route(GridStreamerContext, String, Object)} method.
     *
     * @param ctx Streamer context.
     * @param evts Input events.
     * @return Map of stage name to collection of events.
     * @throws GridException If failed.
     */
    @Nullable public Map<String, Collection<?>> run(GridStreamerContext ctx, Collection<IN> evts)
        throws GridException;
}
