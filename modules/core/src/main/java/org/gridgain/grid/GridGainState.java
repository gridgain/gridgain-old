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

package org.gridgain.grid;

import org.gridgain.grid.segmentation.*;
import org.jetbrains.annotations.*;

/**
 * Possible states of {@link GridGain}. You can register a listener for
 * state change notifications via {@link GridGain#addListener(GridGainListener)}
 * method.
 */
public enum GridGainState {
    /**
     * Grid factory started.
     */
    STARTED,

    /**
     * Grid factory stopped.
     */
    STOPPED,

    /**
     * Grid factory stopped due to network segmentation issues.
     * <p>
     * Notification on this state will be fired only when segmentation policy is
     * set to {@link GridSegmentationPolicy#STOP} or {@link GridSegmentationPolicy#RESTART_JVM}
     * and node is stopped from internals of GridGain after segment becomes invalid.
     */
    STOPPED_ON_SEGMENTATION;

    /** Enumerated values. */
    private static final GridGainState[] VALS = values();

    /**
     * Efficiently gets enumerated value from its ordinal.
     *
     * @param ord Ordinal value.
     * @return Enumerated value.
     */
    @Nullable public static GridGainState fromOrdinal(byte ord) {
        return ord >= 0 && ord < VALS.length ? VALS[ord] : null;
    }
}
