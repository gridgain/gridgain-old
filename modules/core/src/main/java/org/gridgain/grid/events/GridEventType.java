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
import org.gridgain.grid.compute.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.util.typedef.internal.*;

import java.util.*;

/**
 * Contains event type constants. The decision to use class and not enumeration
 * dictated by allowing users to create their own events and/or event types which
 * would be impossible with enumerations.
 * <p>
 * Note that this interface defines not only
 * individual type constants but arrays of types as well to be conveniently used with
 * {@link GridEvents#localListen(GridPredicate, int...)} method:
 * <ul>
 * <li>{@link #EVTS_CHECKPOINT}</li>
 * <li>{@link #EVTS_DEPLOYMENT}</li>
 * <li>{@link #EVTS_DISCOVERY}</li>
 * <li>{@link #EVTS_DISCOVERY_ALL}</li>
 * <li>{@link #EVTS_JOB_EXECUTION}</li>
 * <li>{@link #EVTS_TASK_EXECUTION}</li>
 * <li>{@link #EVTS_LICENSE}</li>
 * <li>{@link #EVTS_CACHE}</li>
 * <li>{@link #EVTS_CACHE_PRELOAD}</li>
 * <li>{@link #EVTS_CACHE_QUERY}</li>
 * <li>{@link #EVTS_SWAPSPACE}</li>
 * <li>{@link #EVTS_AUTHENTICATION}</li>
 * <li>{@link #EVTS_SECURE_SESSION}</li>
 * </ul>
 * <p>
 * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
 * internal GridGain events and should not be used by user-defined events.
 * <h1 class="header">Events and Performance</h1>
 * Note that by default all events in GridGain are enabled and therefore generated and stored
 * by whatever event storage SPI is configured. GridGain can and often does generate thousands events per seconds
 * under the load and therefore it creates a significant additional load on the system. If these events are
 * not needed by the application this load is unnecessary and leads to significant performance degradation.
 * <p>
 * It is <b>highly recommended</b> to enable only those events that your application logic requires
 * by using either {@link GridConfiguration#getIncludeEventTypes()} method in GridGain configuration. Note that certain
 * events are required for GridGain's internal operations and such events will still be generated but not stored by
 * event storage SPI if they are disabled in GridGain configuration.
 */
public interface GridEventType {
    /**
     * Built-in event type: checkpoint was saved.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @see GridCheckpointEvent
     */
    public static final int EVT_CHECKPOINT_SAVED = 1;

    /**
     * Built-in event type: checkpoint was loaded.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @see GridCheckpointEvent
     */
    public static final int EVT_CHECKPOINT_LOADED = 2;

    /**
     * Built-in event type: checkpoint was removed. Reasons are:
     * <ul>
     * <li>timeout expired, or
     * <li>or it was manually removed, or
     * <li>it was automatically removed by the task session
     * </ul>
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @see GridCheckpointEvent
     */
    public static final int EVT_CHECKPOINT_REMOVED = 3;

    /**
     * Built-in event type: node joined topology.
     * <br>
     * New node has been discovered and joined grid topology.
     * Note that even though a node has been discovered there could be
     * a number of warnings in the log. In certain situations GridGain
     * doesn't prevent a node from joining but prints warning messages into the log.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @see GridDiscoveryEvent
     */
    public static final int EVT_NODE_JOINED = 10;

    /**
     * Built-in event type: node has normally left topology.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @see GridDiscoveryEvent
     */
    public static final int EVT_NODE_LEFT = 11;

    /**
     * Built-in event type: node failed.
     * <br>
     * GridGain detected that node has presumably crashed and is considered failed.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @see GridDiscoveryEvent
     */
    public static final int EVT_NODE_FAILED = 12;

    /**
     * Built-in event type: node metrics updated.
     * <br>
     * Generated when node's metrics are updated. In most cases this callback
     * is invoked with every heartbeat received from a node (including local node).
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @see GridDiscoveryEvent
     */
    public static final int EVT_NODE_METRICS_UPDATED = 13;

    /**
     * Built-in event type: local node segmented.
     * <br>
     * Generated when node determines that it runs in invalid network segment.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @see GridDiscoveryEvent
     */
    public static final int EVT_NODE_SEGMENTED = 14;

    /**
     * Built-in event type: local node reconnected.
     * <br>
     * Generated when node reconnects to grid topology after being disconnected from.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @see GridDiscoveryEvent
     * @deprecated This event has no effect in current version of GridGain and
     *      will be removed in the next major release.
     */
    @Deprecated
    public static final int EVT_NODE_RECONNECTED = 15;

    /**
     * Built-in event type: task started.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @see GridTaskEvent
     */
    public static final int EVT_TASK_STARTED = 20;

    /**
     * Built-in event type: task finished.
     * <br>
     * Task got finished. This event is triggered every time
     * a task finished without exception.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @see GridTaskEvent
     */
    public static final int EVT_TASK_FINISHED = 21;

    /**
     * Built-in event type: task failed.
     * <br>
     * Task failed. This event is triggered every time a task finished with an exception.
     * Note that prior to this event, there could be other events recorded specific
     * to the failure.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @see GridTaskEvent
     */
    public static final int EVT_TASK_FAILED = 22;

    /**
     * Built-in event type: task timed out.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @see GridTaskEvent
     */
    public static final int EVT_TASK_TIMEDOUT = 23;

    /**
     * Built-in event type: task session attribute set.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @see GridTaskEvent
     */
    public static final int EVT_TASK_SESSION_ATTR_SET = 24;

    /**
     * Built-in event type: task reduced.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     */
    public static final int EVT_TASK_REDUCED = 25;

    /**
     * Built-in event type: non-task class deployed.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @see GridDeploymentEvent
     */
    public static final int EVT_CLASS_DEPLOYED = 30;

    /**
     * Built-in event type: non-task class undeployed.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @see GridDeploymentEvent
     */
    public static final int EVT_CLASS_UNDEPLOYED = 31;

    /**
     * Built-in event type: non-task class deployment failed.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @see GridDeploymentEvent
     */
    public static final int EVT_CLASS_DEPLOY_FAILED = 32;

    /**
     * Built-in event type: task deployed.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @see GridDeploymentEvent
     */
    public static final int EVT_TASK_DEPLOYED = 33;

    /**
     * Built-in event type: task undeployed.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @see GridDeploymentEvent
     */
    public static final int EVT_TASK_UNDEPLOYED = 34;

    /**
     * Built-in event type: task deployment failed.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @see GridDeploymentEvent
     */
    public static final int EVT_TASK_DEPLOY_FAILED = 35;

    /**
     * Built-in event type: grid job was mapped in
     * {@link GridComputeTask#map(List, Object)} method.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @see GridJobEvent
     */
    public static final int EVT_JOB_MAPPED = 40;

    /**
     * Built-in event type: grid job result was received by
     * {@link GridComputeTask#result(GridComputeJobResult, List)} method.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @see GridJobEvent
     */
    public static final int EVT_JOB_RESULTED = 41;

    /**
     * Built-in event type: grid job failed over.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @see GridJobEvent
     */
    public static final int EVT_JOB_FAILED_OVER = 43;

    /**
     * Built-in event type: grid job started.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @see GridJobEvent
     */
    public static final int EVT_JOB_STARTED = 44;

    /**
     * Built-in event type: grid job finished.
     * <br>
     * Job has successfully completed and produced a result which from the user perspective
     * can still be either negative or positive.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @see GridJobEvent
     */
    public static final int EVT_JOB_FINISHED = 45;

    /**
     * Built-in event type: grid job timed out.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @see GridJobEvent
     */
    public static final int EVT_JOB_TIMEDOUT = 46;

    /**
     * Built-in event type: grid job rejected during collision resolution.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @see GridJobEvent
     */
    public static final int EVT_JOB_REJECTED = 47;

    /**
     * Built-in event type: grid job failed.
     * <br>
     * Job has failed. This means that there was some error event during job execution
     * and job did not produce a result.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @see GridJobEvent
     */
    public static final int EVT_JOB_FAILED = 48;

    /**
     * Built-in event type: grid job queued.
     * <br>
     * Job arrived for execution and has been queued (added to passive queue during
     * collision resolution).
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @see GridJobEvent
     */
    public static final int EVT_JOB_QUEUED = 49;

    /**
     * Built-in event type: grid job cancelled.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @see GridJobEvent
     */
    public static final int EVT_JOB_CANCELLED = 50;

    /**
      * Built-in event type: entry created.
      * <p>
      * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
      * internal GridGain events and should not be used by user-defined events.
      */
     public static final int EVT_CACHE_ENTRY_CREATED = 60;

     /**
      * Built-in event type: entry destroyed.
      * <p>
      * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
      * internal GridGain events and should not be used by user-defined events.
      */
     public static final int EVT_CACHE_ENTRY_DESTROYED = 61;

    /**
     * Built-in event type: entry evicted.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     */
     public static final int EVT_CACHE_ENTRY_EVICTED = 62;

     /**
      * Built-in event type: object put.
      * <p>
      * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
      * internal GridGain events and should not be used by user-defined events.
      */
     public static final int EVT_CACHE_OBJECT_PUT = 63;

     /**
      * Built-in event type: object read.
      * <p>
      * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
      * internal GridGain events and should not be used by user-defined events.
      */
     public static final int EVT_CACHE_OBJECT_READ = 64;

     /**
      * Built-in event type: object removed.
      * <p>
      * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
      * internal GridGain events and should not be used by user-defined events.
      */
     public static final int EVT_CACHE_OBJECT_REMOVED = 65;

     /**
      * Built-in event type: object locked.
      * <p>
      * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
      * internal GridGain events and should not be used by user-defined events.
      */
     public static final int EVT_CACHE_OBJECT_LOCKED = 66;

     /**
      * Built-in event type: object unlocked.
      * <p>
      * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
      * internal GridGain events and should not be used by user-defined events.
      */
     public static final int EVT_CACHE_OBJECT_UNLOCKED = 67;

    /**
     * Built-in event type: cache object swapped from swap storage.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     */
    public static final int EVT_CACHE_OBJECT_SWAPPED = 68;

    /**
     * Built-in event type: cache object unswapped from swap storage.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     */
    public static final int EVT_CACHE_OBJECT_UNSWAPPED = 69;

    /**
     * Built-in event type: cache object was expired when reading it.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     */
    public static final int EVT_CACHE_OBJECT_EXPIRED = 70;

    /**
     * Built-in event type: swap space data read.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @see GridSwapSpaceEvent
     */
    public static final int EVT_SWAP_SPACE_DATA_READ = 71;

    /**
     * Built-in event type: swap space data stored.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @see GridSwapSpaceEvent
     */
    public static final int EVT_SWAP_SPACE_DATA_STORED = 72;

    /**
     * Built-in event type: swap space data removed.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @see GridSwapSpaceEvent
     */
    public static final int EVT_SWAP_SPACE_DATA_REMOVED = 73;

    /**
     * Built-in event type: swap space cleared.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @see GridSwapSpaceEvent
     */
    public static final int EVT_SWAP_SPACE_CLEARED = 74;

    /**
     * Built-in event type: swap space data evicted.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @see GridSwapSpaceEvent
     */
    public static final int EVT_SWAP_SPACE_DATA_EVICTED = 75;

    /**
     * Built-in event type: cache object stored in off-heap storage.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     */
    public static final int EVT_CACHE_OBJECT_TO_OFFHEAP = 76;

    /**
     * Built-in event type: cache object moved from off-heap storage back into memory.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     */
    public static final int EVT_CACHE_OBJECT_FROM_OFFHEAP = 77;

    /**
     * Built-in event type: cache preload started.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @see GridSwapSpaceEvent
     */
    public static final int EVT_CACHE_PRELOAD_STARTED = 80;

    /**
     * Built-in event type: cache preload stopped.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @see GridSwapSpaceEvent
     */
    public static final int EVT_CACHE_PRELOAD_STOPPED = 81;

    /**
     * Built-in event type: cache partition loaded.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @see GridSwapSpaceEvent
     */
    public static final int EVT_CACHE_PRELOAD_PART_LOADED = 82;

    /**
     * Built-in event type: cache partition unloaded.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     */
    public static final int EVT_CACHE_PRELOAD_PART_UNLOADED = 83;

    /**
     * Built-in event type: cache entry preloaded.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     */
    public static final int EVT_CACHE_PRELOAD_OBJECT_LOADED = 84;

    /**
     * Built-in event type: cache entry unloaded.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     */
    public static final int EVT_CACHE_PRELOAD_OBJECT_UNLOADED = 85;

    /**
     * Built-in event type: {@code SQL} query executed.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @deprecated Not used in current version of GridGain (replaced with {@link #EVT_CACHE_QUERY_EXECUTED}).
     */
    @SuppressWarnings("UnusedDeclaration")
    @Deprecated
    public static final int EVT_CACHE_SQL_QUERY_EXECUTED = 86;

    /**
     * Built-in event type: {@code SQL fields} query executed.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @deprecated Not used in current version of GridGain (replaced with {@link #EVT_CACHE_QUERY_EXECUTED}).
     */
    @SuppressWarnings("UnusedDeclaration")
    @Deprecated
    public static final int EVT_CACHE_SQL_FIELDS_QUERY_EXECUTED = 87;

    /**
     * Built-in event type: {@code full text} query executed.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @deprecated Not used in current version of GridGain (replaced with {@link #EVT_CACHE_QUERY_EXECUTED}).
     */
    @SuppressWarnings("UnusedDeclaration")
    @Deprecated
    public static final int EVT_CACHE_FULL_TEXT_QUERY_EXECUTED = 88;

    /**
     * Built-in event type: {@code scan} query executed.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @deprecated Not used in current version of GridGain (replaced with {@link #EVT_CACHE_QUERY_EXECUTED}).
     */
    @SuppressWarnings("UnusedDeclaration")
    @Deprecated
    public static final int EVT_CACHE_SCAN_QUERY_EXECUTED = 89;

    /**
     * Built-in event type: {@code continuous} query executed.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @deprecated Not used in current version of GridGain (replaced with {@link #EVT_CACHE_QUERY_EXECUTED}).
     */
    @SuppressWarnings("UnusedDeclaration")
    @Deprecated
    public static final int EVT_CACHE_CONTINUOUS_QUERY_EXECUTED = 90;

    /**
     * Built-in event type: {@code SQL} query entry read.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @deprecated Not used in current version of GridGain (replaced with {@link #EVT_CACHE_QUERY_OBJECT_READ}).
     */
    @SuppressWarnings("UnusedDeclaration")
    @Deprecated
    public static final int EVT_CACHE_SQL_QUERY_OBJECT_READ = 91;

    /**
     * Built-in event type: {@code SQL fields} query result set row read.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @deprecated Not used in current version of GridGain (replaced with {@link #EVT_CACHE_QUERY_OBJECT_READ}).
     */
    @SuppressWarnings("UnusedDeclaration")
    @Deprecated
    public static final int EVT_CACHE_SQL_FIELDS_QUERY_OBJECT_READ = 92;

    /**
     * Built-in event type: {@code full text} query entry read.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @deprecated Not used in current version of GridGain (replaced with {@link #EVT_CACHE_QUERY_OBJECT_READ}).
     */
    @SuppressWarnings("UnusedDeclaration")
    @Deprecated
    public static final int EVT_CACHE_FULL_TEXT_QUERY_OBJECT_READ = 93;

    /**
     * Built-in event type: {@code scan} query entry read.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @deprecated Not used in current version of GridGain (replaced with {@link #EVT_CACHE_QUERY_OBJECT_READ}).
     */
    @SuppressWarnings("UnusedDeclaration")
    @Deprecated
    public static final int EVT_CACHE_SCAN_QUERY_OBJECT_READ = 94;

    /**
     * Built-in event type: {@code continuous} query entry read.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @deprecated Not used in current version of GridGain (replaced with {@link #EVT_CACHE_QUERY_OBJECT_READ}).
     */
    @SuppressWarnings("UnusedDeclaration")
    @Deprecated
    public static final int EVT_CACHE_CONTINUOUS_QUERY_OBJECT_READ = 95;

    /**
     * Built-in event type: query executed.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     */
    public static final int EVT_CACHE_QUERY_EXECUTED = 96;

    /**
     * Built-in event type: query entry read.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     */
    public static final int EVT_CACHE_QUERY_OBJECT_READ = 97;

    /**
     * Built-in event type: license violation detected.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @see GridLicenseEvent
     */
    public static final int EVT_LIC_VIOLATION = 108;

    /**
     * Built-in event type: license violation cleared.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @see GridLicenseEvent
     */
    public static final int EVT_LIC_CLEARED = 109;

    /**
     * Built-in event type: license violation grace period is expired.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @see GridLicenseEvent
     */
    public static final int EVT_LIC_GRACE_EXPIRED = 110;

    /**
     * Built-in event type: authentication succeed.
     * <p>
     * Authentication procedure succeed. This event is triggered every time
     * an authentication procedure finished without exception.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @see GridAuthenticationEvent
     */
    public static final int EVT_AUTHENTICATION_SUCCEEDED = 111;

    /**
     * Built-in event type: authentication failed.
     * <p>
     * Authentication procedure failed. This means that  there was some error event
     * during authentication procedure and authentication procedure was not successful.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @see GridAuthenticationEvent
     */
    public static final int EVT_AUTHENTICATION_FAILED = 112;

    /**
     * Built-in event type: secure session validation succeed.
     * <p>
     * Secure session validation succeed. This event is triggered every time
     * a validation of secure session procedure finished without exception.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @see GridSecureSessionEvent
     */
    public static final int EVT_SECURE_SESSION_VALIDATION_SUCCEEDED = 113;

    /**
     * Built-in event type: secure session validation failed.
     * <br>
     * Secure session validation failed. This means that  there was some error event
     * during secure session validation procedure and validation was not succeed.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @see GridSecureSessionEvent
     */
    public static final int EVT_SECURE_SESSION_VALIDATION_FAILED = 114;

    /**
     * Built-in event type: Visor detects that some events were evicted from events buffer since last poll.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     */
    public static final int EVT_VISOR_EVENTS_LOST = 115;

    /**
     * Built-in event type: GGFS file created.
     * <p>
     * Fired when GGFS component creates new file.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @see GridGgfsEvent
     */
    public static final int EVT_GGFS_FILE_CREATED = 116;

    /**
     * Built-in event type: GGFS file renamed.
     * <p>
     * Fired when GGFS component renames an existing file.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @see GridGgfsEvent
     */
    public static final int EVT_GGFS_FILE_RENAMED = 117;

    /**
     * Built-in event type: GGFS file deleted.
     * <p>
     * Fired when GGFS component deletes a file.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @see GridGgfsEvent
     */
    public static final int EVT_GGFS_FILE_DELETED = 118;

    /**
     * Built-in event type: GGFS file opened for reading.
     * <p>
     * Fired when GGFS file is opened for reading.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @see GridGgfsEvent
     */
    public static final int EVT_GGFS_FILE_OPENED_READ = 119;

    /**
     * Built-in event type: GGFS file opened for writing.
     * <p>
     * Fired when GGFS file is opened for writing.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @see GridGgfsEvent
     */
    public static final int EVT_GGFS_FILE_OPENED_WRITE = 120;

    /**
     * Built-in event type: GGFS file or directory metadata updated.
     * <p>
     * Fired when GGFS file or directory metadata is updated.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @see GridGgfsEvent
     */
    public static final int EVT_GGFS_META_UPDATED = 121;

    /**
     * Built-in event type: GGFS file closed.
     * <p>
     * Fired when GGFS file is closed.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @see GridGgfsEvent
     */
    public static final int EVT_GGFS_FILE_CLOSED_WRITE = 122;

    /**
     * Built-in event type: GGFS file closed.
     * <p>
     * Fired when GGFS file is closed.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @see GridGgfsEvent
     */
    public static final int EVT_GGFS_FILE_CLOSED_READ = 123;

    /**
     * Built-in event type: GGFS directory created.
     * <p>
     * Fired when GGFS component creates new directory.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @see GridGgfsEvent
     */
    public static final int EVT_GGFS_DIR_CREATED = 124;

    /**
     * Built-in event type: GGFS directory renamed.
     * <p>
     * Fired when GGFS component renames an existing directory.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @see GridGgfsEvent
     */
    public static final int EVT_GGFS_DIR_RENAMED = 125;

    /**
     * Built-in event type: GGFS directory deleted.
     * <p>
     * Fired when GGFS component deletes a directory.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @see GridGgfsEvent
     */
    public static final int EVT_GGFS_DIR_DELETED = 126;

    /**
     * Built-in event type: GGFS file purged.
     * <p>
     * Fired when GGFS file data was actually removed from cache.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @see GridGgfsEvent
     */
    public static final int EVT_GGFS_FILE_PURGED = 127;

    /**
     * Built-in event type: authorization succeed.
     * <p>
     * Authorization procedure succeed. This event is triggered every time
     * an authorization procedure finished without exception.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @see GridAuthorizationEvent
     */
    public static final int EVT_AUTHORIZATION_SUCCEEDED = 128;

    /**
     * Built-in event type: authorization failed.
     * <p>
     * Authorization procedure failed. This means that  there was some error event
     * during authorization procedure and authorization procedure was not successful.
     * <p>
     * NOTE: all types in range <b>from 1 to 1000 are reserved</b> for
     * internal GridGain events and should not be used by user-defined events.
     *
     * @see GridAuthorizationEvent
     */
    public static final int EVT_AUTHORIZATION_FAILED = 129;

    /**
     * All license events. This array can be directly passed into
     * {@link GridEvents#localListen(GridPredicate, int...)} method to
     * subscribe to all license events.
     *
     * @see GridLicenseEvent
     */
    public static final int[] EVTS_LICENSE = {
        EVT_LIC_CLEARED,
        EVT_LIC_VIOLATION,
        EVT_LIC_GRACE_EXPIRED
    };

    /**
     * All checkpoint events. This array can be directly passed into
     * {@link GridEvents#localListen(GridPredicate, int...)} method to
     * subscribe to all checkpoint events.
     *
     * @see GridCheckpointEvent
     */
    public static final int[] EVTS_CHECKPOINT = {
        EVT_CHECKPOINT_SAVED,
        EVT_CHECKPOINT_LOADED,
        EVT_CHECKPOINT_REMOVED
    };

    /**
     * All deployment events. This array can be directly passed into
     * {@link GridEvents#localListen(GridPredicate, int...)} method to
     * subscribe to all deployment events.
     *
     * @see GridDeploymentEvent
     */
    public static final int[] EVTS_DEPLOYMENT = {
        EVT_CLASS_DEPLOYED,
        EVT_CLASS_UNDEPLOYED,
        EVT_CLASS_DEPLOY_FAILED,
        EVT_TASK_DEPLOYED,
        EVT_TASK_UNDEPLOYED,
        EVT_TASK_DEPLOY_FAILED
    };

    /**
     * All events indicating an error or failure condition. It is convenient to use
     * when fetching all events indicating error or failure.
     */
    public static final int[] EVTS_ERROR = {
        EVT_JOB_TIMEDOUT,
        EVT_JOB_FAILED,
        EVT_JOB_FAILED_OVER,
        EVT_JOB_REJECTED,
        EVT_JOB_CANCELLED,
        EVT_TASK_TIMEDOUT,
        EVT_TASK_FAILED,
        EVT_CLASS_DEPLOY_FAILED,
        EVT_TASK_DEPLOY_FAILED,
        EVT_TASK_DEPLOYED,
        EVT_TASK_UNDEPLOYED,
        EVT_LIC_CLEARED,
        EVT_LIC_VIOLATION,
        EVT_LIC_GRACE_EXPIRED,
        EVT_CACHE_PRELOAD_STARTED,
        EVT_CACHE_PRELOAD_STOPPED
    };

    /**
     * All discovery events <b>except</b> for {@link #EVT_NODE_METRICS_UPDATED}. Subscription to
     * {@link #EVT_NODE_METRICS_UPDATED} can generate massive amount of event processing in most cases
     * is not necessary. If this event is indeed required you can subscribe to it individually or use
     * {@link #EVTS_DISCOVERY_ALL} array.
     * <p>
     * This array can be directly passed into
     * {@link GridEvents#localListen(GridPredicate, int...)} method to
     * subscribe to all discovery events <b>except</b> for {@link #EVT_NODE_METRICS_UPDATED}.
     *
     * @see GridDiscoveryEvent
     */
    public static final int[] EVTS_DISCOVERY = {
        EVT_NODE_JOINED,
        EVT_NODE_LEFT,
        EVT_NODE_FAILED,
        EVT_NODE_SEGMENTED,
    };

    /**
     * All discovery events. This array can be directly passed into
     * {@link GridEvents#localListen(GridPredicate, int...)} method to
     * subscribe to all discovery events.
     *
     * @see GridDiscoveryEvent
     */
    public static final int[] EVTS_DISCOVERY_ALL = {
        EVT_NODE_JOINED,
        EVT_NODE_LEFT,
        EVT_NODE_FAILED,
        EVT_NODE_METRICS_UPDATED,
        EVT_NODE_SEGMENTED,
    };

    /**
     * All grid job execution events. This array can be directly passed into
     * {@link GridEvents#localListen(GridPredicate, int...)} method to
     * subscribe to all grid job execution events.
     *
     * @see GridJobEvent
     */
    public static final int[] EVTS_JOB_EXECUTION = {
        EVT_JOB_MAPPED,
        EVT_JOB_RESULTED,
        EVT_JOB_FAILED_OVER,
        EVT_JOB_STARTED,
        EVT_JOB_FINISHED,
        EVT_JOB_TIMEDOUT,
        EVT_JOB_REJECTED,
        EVT_JOB_FAILED,
        EVT_JOB_QUEUED,
        EVT_JOB_CANCELLED
    };

    /**
     * All grid task execution events. This array can be directly passed into
     * {@link GridEvents#localListen(GridPredicate, int...)} method to
     * subscribe to all grid task execution events.
     *
     * @see GridTaskEvent
     */
    public static final int[] EVTS_TASK_EXECUTION = {
        EVT_TASK_STARTED,
        EVT_TASK_FINISHED,
        EVT_TASK_FAILED,
        EVT_TASK_TIMEDOUT,
        EVT_TASK_SESSION_ATTR_SET,
        EVT_TASK_REDUCED
    };

    /**
     * All cache events. This array can be directly passed into
     * {@link GridEvents#localListen(GridPredicate, int...)} method to
     * subscribe to all cache events.
     */
    public static final int[] EVTS_CACHE = {
        EVT_CACHE_ENTRY_CREATED,
        EVT_CACHE_ENTRY_DESTROYED,
        EVT_CACHE_OBJECT_PUT,
        EVT_CACHE_OBJECT_READ,
        EVT_CACHE_OBJECT_REMOVED,
        EVT_CACHE_OBJECT_LOCKED,
        EVT_CACHE_OBJECT_UNLOCKED,
        EVT_CACHE_OBJECT_SWAPPED,
        EVT_CACHE_OBJECT_UNSWAPPED,
        EVT_CACHE_OBJECT_EXPIRED
    };

    /**
     * All cache preload events. This array can be directly passed into
     * {@link GridEvents#localListen(GridPredicate, int...)} method to
     * subscribe to all cache preload events.
     */
    public static final int[] EVTS_CACHE_PRELOAD = {
        EVT_CACHE_PRELOAD_STARTED,
        EVT_CACHE_PRELOAD_STOPPED,
        EVT_CACHE_PRELOAD_PART_LOADED,
        EVT_CACHE_PRELOAD_PART_UNLOADED,
        EVT_CACHE_PRELOAD_OBJECT_LOADED,
        EVT_CACHE_PRELOAD_OBJECT_UNLOADED
    };

    /**
     * All cache query events. This array can be directly passed into
     * {@link GridEvents#localListen(GridPredicate, int...)} method to
     * subscribe to all cache query events.
     */
    public static final int[] EVTS_CACHE_QUERY = {
        EVT_CACHE_QUERY_EXECUTED,
        EVT_CACHE_QUERY_OBJECT_READ
    };

    /**
     * All swap space events. This array can be directly passed into
     * {@link GridEvents#localListen(GridPredicate, int...)} method to
     * subscribe to all cloud events.
     *
     * @see GridSwapSpaceEvent
     */
    public static final int[] EVTS_SWAPSPACE = {
        EVT_SWAP_SPACE_CLEARED,
        EVT_SWAP_SPACE_DATA_REMOVED,
        EVT_SWAP_SPACE_DATA_READ,
        EVT_SWAP_SPACE_DATA_STORED,
        EVT_SWAP_SPACE_DATA_EVICTED
    };

    /**
     * All authentication events. This array can be directly passed into
     * {@link GridEvents#localListen(GridPredicate, int...)} method to
     * subscribe to all cloud events.
     *
     * @see GridAuthenticationEvent
     */
    public static final int[] EVTS_AUTHENTICATION = {
        EVT_AUTHENTICATION_SUCCEEDED,
        EVT_AUTHENTICATION_FAILED
    };

    /**
     * All authorization events. This array can be directly passed into
     * {@link GridEvents#localListen(GridPredicate, int...)} method to
     * subscribe to all cloud events.
     *
     * @see GridAuthenticationEvent
     */
    public static final int[] EVTS_AUTHORIZATION = {
        EVT_AUTHORIZATION_SUCCEEDED,
        EVT_AUTHORIZATION_FAILED
    };

    /**
     * All secure session events. This array can be directly passed into
     * {@link GridEvents#localListen(GridPredicate, int...)} method to
     * subscribe to all GGFS events.
     *
     * @see GridGgfsEvent
     */
    public static final int[] EVTS_SECURE_SESSION = {
        EVT_SECURE_SESSION_VALIDATION_SUCCEEDED,
        EVT_SECURE_SESSION_VALIDATION_FAILED
    };

    /**
     * All GGFS events. This array can be directly passed into
     * {@link GridEvents#localListen(GridPredicate, int...)} method to
     * subscribe to all cloud events.
     *
     * @see GridSecureSessionEvent
     */
    public static final int[] EVTS_GGFS = {
        EVT_GGFS_FILE_CREATED,
        EVT_GGFS_FILE_RENAMED,
        EVT_GGFS_FILE_DELETED,
        EVT_GGFS_FILE_OPENED_READ,
        EVT_GGFS_FILE_OPENED_WRITE,
        EVT_GGFS_FILE_CLOSED_WRITE,
        EVT_GGFS_FILE_CLOSED_READ,
        EVT_GGFS_FILE_PURGED,
        EVT_GGFS_META_UPDATED,
        EVT_GGFS_DIR_CREATED,
        EVT_GGFS_DIR_RENAMED,
        EVT_GGFS_DIR_DELETED,
    };

    /**
     * All GridGain events (<b>including</b> metric update event).
     */
    public static final int[] EVTS_ALL = U.gridEvents();

    /**
     * All GridGain events (<b>excluding</b> metric update event).
     */
    public static final int[] EVTS_ALL_MINUS_METRIC_UPDATE = U.gridEvents(EVT_NODE_METRICS_UPDATED);
}
