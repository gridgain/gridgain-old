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

package org.gridgain.grid.events;

import org.gridgain.grid.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.util.typedef.internal.*;

import java.util.*;

/**
 * Grid checkpoint event.
 * <p>
 * Grid events are used for notification about what happens within the grid. Note that by
 * design GridGain keeps all events generated on the local node locally and it provides
 * APIs for performing a distributed queries across multiple nodes:
 * <ul>
 *      <li>
 *          {@link GridEvents#remoteQuery(GridPredicate, long, int...)} -
 *          asynchronously querying events occurred on the nodes specified, including remote nodes.
 *      </li>
 *      <li>
 *          {@link GridEvents#localQuery(GridPredicate, int...)} -
 *          querying only local events stored on this local node.
 *      </li>
 *      <li>
 *          {@link GridEvents#localListen(GridPredicate, int...)} -
 *          listening to local grid events (events from remote nodes not included).
 *      </li>
 * </ul>
 * User can also wait for events using method {@link GridEvents#waitForLocal(GridPredicate, int...)}.
 * <h1 class="header">Events and Performance</h1>
 * Note that by default all events in GridGain are enabled and therefore generated and stored
 * by whatever event storage SPI is configured. GridGain can and often does generate thousands events per seconds
 * under the load and therefore it creates a significant additional load on the system. If these events are
 * not needed by the application this load is unnecessary and leads to significant performance degradation.
 * <p>
 * It is <b>highly recommended</b> to enable only those events that your application logic requires
 * by using {@link GridConfiguration#getIncludeEventTypes()} method in GridGain configuration. Note that certain
 * events are required for GridGain's internal operations and such events will still be generated but not stored by
 * event storage SPI if they are disabled in GridGain configuration.
 * @see GridEventType#EVT_CHECKPOINT_LOADED
 * @see GridEventType#EVT_CHECKPOINT_REMOVED
 * @see GridEventType#EVT_CHECKPOINT_SAVED
 * @see GridEventType#EVTS_CHECKPOINT
 */
public class GridCheckpointEvent extends GridEventAdapter {
    /** */
    private static final long serialVersionUID = 0L;

    /** */
    private String cpKey;

    /**
     * No-arg constructor.
     */
    public GridCheckpointEvent() {
        // No-op.
    }

    /**
     * Creates new checkpoint event with given parameters.
     *
     * @param node Local node.
     * @param msg Optional event message.
     * @param type Event type.
     * @param cpKey Checkpoint key associated with this event.
     */
    public GridCheckpointEvent(GridNode node, String msg, int type, String cpKey) {
        super(node, msg, type);

        this.cpKey = cpKey;
    }

    /** {@inheritDoc} */
    @Override public String shortDisplay() {
        return name() + ": cpKey=" + cpKey;
    }

    /**
     * Gets checkpoint key associated with this event.
     *
     * @return Checkpoint key associated with this event.
     */
    public String key() {
        assert cpKey != null;

        return cpKey;
    }

    /**
     * Sets checkpoint key.
     *
     * @param cpKey Checkpoint key to set.
     */
    public void key(String cpKey) {
        assert cpKey != null;

        this.cpKey = cpKey;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridCheckpointEvent.class, this,
            "nodeId8", U.id8(node().id()),
            "msg", message(),
            "type", name(),
            "tstamp", timestamp());
    }
}
