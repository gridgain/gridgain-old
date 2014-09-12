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

/**
 * In-memory database (cache) preloading event. Preload event happens every time there is a change
 * in grid topology, which means that a node has either joined or left the grid.
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
 * @see GridEventType#EVT_CACHE_PRELOAD_PART_LOADED
 * @see GridEventType#EVT_CACHE_PRELOAD_PART_UNLOADED
 * @see GridEventType#EVT_CACHE_PRELOAD_STARTED
 * @see GridEventType#EVT_CACHE_PRELOAD_STOPPED
 */
public class GridCachePreloadingEvent extends GridEventAdapter {
    /** */
    private static final long serialVersionUID = 0L;

    /** Cache name. */
    private String cacheName;

    /** Partition for the event. */
    private int part;

    /** Discovery node. */
    private GridNode discoNode;

    /** Discovery event type. */
    private int discoEvtType;

    /** Discovery event time. */
    private long discoTs;

    /**
     * Constructs cache event.
     *
     * @param cacheName Cache name.
     * @param node Event node.
     * @param msg Event message.
     * @param type Event type.
     * @param part Partition for the event (usually the partition the key belongs to).
     * @param discoNode Node that triggered this preloading event.
     * @param discoEvtType Discovery event type that triggered this preloading event.
     * @param discoTs Timestamp of discovery event that triggered this preloading event.
     */
    public GridCachePreloadingEvent(String cacheName, GridNode node, String msg, int type, int part,
        GridNode discoNode, int discoEvtType, long discoTs) {
        super(node, msg, type);
        this.cacheName = cacheName;
        this.part = part;
        this.discoNode = discoNode;
        this.discoEvtType = discoEvtType;
        this.discoTs = discoTs;
    }

    /**
     * Gets cache name.
     *
     * @return Cache name.
     */
    public String cacheName() {
        return cacheName;
    }

    /**
     * Gets partition for the event.
     *
     * @return Partition for the event.
     */
    public int partition() {
        return part;
    }

    /**
     * Gets shadow of the node that triggered this preloading event.
     *
     * @return Shadow of the node that triggered this preloading event.
     */
    public GridNode discoveryNode() {
        return discoNode;
    }

    /**
     * Gets type of discovery event that triggered this preloading event.
     *
     * @return Type of discovery event that triggered this preloading event.
     * @see GridDiscoveryEvent#type()
     */
    public int discoveryEventType() {
        return discoEvtType;
    }

    /**
     * Gets name of discovery event that triggered this preloading event.
     *
     * @return Name of discovery event that triggered this preloading event.
     * @see GridDiscoveryEvent#name()
     */
    public String discoveryEventName() {
        return U.gridEventName(discoEvtType);
    }

    /**
     * Gets timestamp of discovery event that caused this preloading event.
     *
     * @return Timestamp of discovery event that caused this preloading event.
     */
    public long discoveryTimestamp() {
        return discoTs;
    }

    /** {@inheritDoc} */
    @Override public String shortDisplay() {
        return name() + ": cache=" + CU.mask(cacheName) + ", cause=" +
            discoveryEventName();
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridCachePreloadingEvent.class, this,
            "discoEvtName", discoveryEventName(),
            "nodeId8", U.id8(node().id()),
            "msg", message(),
            "type", name(),
            "tstamp", timestamp());
    }
}
