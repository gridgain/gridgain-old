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

import org.gridgain.grid.streamer.*;
import org.gridgain.grid.util.typedef.internal.*;

/**
 * Streamer window metrics adapter.
 */
public class GridStreamerWindowMetricsAdapter implements GridStreamerWindowMetrics {
    /** Window name. */
    private String name;

    /** Window size. */
    private int size;

    /** Window eviction queue size. */
    private int evictionQueueSize;

    /**
     * @param m Metrics to copy.
     */
    public GridStreamerWindowMetricsAdapter(GridStreamerWindowMetrics m) {
        // Preserve alphabetic order for maintenance.
        evictionQueueSize = m.evictionQueueSize();
        name = m.name();
        size = m.size();
    }

    /** {@inheritDoc} */
    @Override public String name() {
        return name;
    }

    /** {@inheritDoc} */
    @Override public int size() {
        return size;
    }

    /** {@inheritDoc} */
    @Override public int evictionQueueSize() {
        return evictionQueueSize;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridStreamerWindowMetricsAdapter.class, this);
    }
}
