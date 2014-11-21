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
import org.jetbrains.annotations.*;

/**
 * Streamer MBean implementation.
 */
public class GridStreamerMBeanAdapter implements GridStreamerMBean {
    /** Streamer. */
    private GridStreamerImpl streamer;

    /**
     * @param streamer Streamer.
     */
    public GridStreamerMBeanAdapter(GridStreamerImpl streamer) {
        this.streamer = streamer;
    }

    /** {@inheritDoc} */
    @Nullable @Override public String getName() {
        return streamer.name();
    }

    /** {@inheritDoc} */
    @Override public boolean isAtLeastOnce() {
        return streamer.atLeastOnce();
    }

    /** {@inheritDoc} */
    @Override public int getStageFutureMapSize() {
        return streamer.stageFutureMapSize();
    }

    /** {@inheritDoc} */
    @Override public int getBatchFutureMapSize() {
        return streamer.batchFutureMapSize();
    }

    /** {@inheritDoc} */
    @Override public int getStageActiveExecutionCount() {
        return streamer.metrics().stageActiveExecutionCount();
    }

    /** {@inheritDoc} */
    @Override public int getStageWaitingExecutionCount() {
        return streamer.metrics().stageWaitingExecutionCount();
    }

    /** {@inheritDoc} */
    @Override public long getStageTotalExecutionCount() {
        return streamer.metrics().stageTotalExecutionCount();
    }

    /** {@inheritDoc} */
    @Override public long getPipelineMaximumExecutionTime() {
        return streamer.metrics().pipelineMaximumExecutionTime();
    }

    /** {@inheritDoc} */
    @Override public long getPipelineMinimumExecutionTime() {
        return streamer.metrics().pipelineMinimumExecutionTime();
    }

    /** {@inheritDoc} */
    @Override public long getPipelineAverageExecutionTime() {
        return streamer.metrics().pipelineAverageExecutionTime();
    }

    /** {@inheritDoc} */
    @Override public int getPipelineMaximumExecutionNodes() {
        return streamer.metrics().pipelineMaximumExecutionNodes();
    }

    /** {@inheritDoc} */
    @Override public int getPipelineMinimumExecutionNodes() {
        return streamer.metrics().pipelineMinimumExecutionNodes();
    }

    /** {@inheritDoc} */
    @Override public int getPipelineAverageExecutionNodes() {
        return streamer.metrics().pipelineAverageExecutionNodes();
    }

    /** {@inheritDoc} */
    @Override public int getCurrentActiveSessions() {
        return streamer.metrics().currentActiveSessions();
    }

    /** {@inheritDoc} */
    @Override public int getMaximumActiveSessions() {
        return streamer.metrics().maximumActiveSessions();
    }

    /** {@inheritDoc} */
    @Override public int getFailuresCount() {
        return streamer.metrics().failuresCount();
    }
}
