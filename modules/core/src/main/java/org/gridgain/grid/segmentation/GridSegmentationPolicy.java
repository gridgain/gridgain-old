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

package org.gridgain.grid.segmentation;

import org.gridgain.grid.*;
import org.gridgain.grid.events.*;
import org.gridgain.grid.startup.cmdline.*;
import org.gridgain.grid.spi.discovery.*;

/**
 * Policy that defines how node will react on topology segmentation. Note that default
 * segmentation policy is defined by {@link GridConfiguration#DFLT_SEG_PLC} property.
 * @see GridSegmentationResolver
 */
public enum GridSegmentationPolicy {
    /**
     * When segmentation policy is {@code RESTART_JVM}, all listeners will receive
     * {@link GridEventType#EVT_NODE_SEGMENTED} event and then JVM will be restarted.
     * Note, that this will work <b>only</b> if GridGain is started with {@link GridCommandLineStartup}
     * via standard {@code ggstart.{sh|bat}} shell script.
     */
    RESTART_JVM,

    /**
     * When segmentation policy is {@code STOP}, all listeners will receive
     * {@link GridEventType#EVT_NODE_SEGMENTED} event and then particular grid node
     * will be stopped via call to {@link GridGain#stop(String, boolean)}.
     */
    STOP,

    /**
     * When segmentation policy is {@code RECONNECT}, all listeners will receive
     * {@link GridEventType#EVT_NODE_SEGMENTED} and then discovery manager will
     * try to reconnect discovery SPI to topology issuing
     * {@link GridEventType#EVT_NODE_RECONNECTED} event on reconnect.
     * <p>
     * Note, that this policy is not allowed when in-memory data grid is enabled.
     * <p>
     * This policy can be used only with {@link GridDiscoverySpi} implementation that
     * has support for reconnect (i.e. annotated with {@link GridDiscoverySpiReconnectSupport}
     * annotation).
     */
    RECONNECT,

    /**
     * When segmentation policy is {@code NOOP}, all listeners will receive
     * {@link GridEventType#EVT_NODE_SEGMENTED} event and it is up to user to
     * implement logic to handle this event.
     */
    NOOP
}

