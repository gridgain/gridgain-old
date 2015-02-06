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

/**
 * Streamer stage MBean adapter.
 */
@SuppressWarnings("ConstantConditions")
public class GridStreamerStageMBeanAdapter implements GridStreamerStageMBean {
    /** Stage name. */
    private String stageName;

    /** Stage class name. */
    private String stageClsName;

    /** */
    private GridStreamerImpl streamer;

    /**
     * @param stageName Stage name.
     * @param stageClsName Stage class name.
     * @param streamer Streamer implementation.
     */
    public GridStreamerStageMBeanAdapter(String stageName, String stageClsName, GridStreamerImpl streamer) {
        this.stageName = stageName;
        this.stageClsName = stageClsName;
        this.streamer = streamer;
    }

    /** {@inheritDoc} */
    @Override public String getName() {
        return stageName;
    }

    /** {@inheritDoc} */
    @Override public String getStageClassName() {
        return stageClsName;
    }

    /** {@inheritDoc} */
    @Override public long getMinimumExecutionTime() {
        return streamer.metrics().stageMetrics(stageName).minimumExecutionTime();
    }

    /** {@inheritDoc} */
    @Override public long getMaximumExecutionTime() {
        return streamer.metrics().stageMetrics(stageName).maximumExecutionTime();
    }

    /** {@inheritDoc} */
    @Override public long getAverageExecutionTime() {
        return streamer.metrics().stageMetrics(stageName).averageExecutionTime();
    }

    /** {@inheritDoc} */
    @Override public long getMinimumWaitingTime() {
        return streamer.metrics().stageMetrics(stageName).minimumWaitingTime();
    }

    /** {@inheritDoc} */
    @Override public long getMaximumWaitingTime() {
        return streamer.metrics().stageMetrics(stageName).maximumWaitingTime();
    }

    /** {@inheritDoc} */
    @Override public long getAverageWaitingTime() {
        return streamer.metrics().stageMetrics(stageName).averageWaitingTime();
    }

    /** {@inheritDoc} */
    @Override public long getTotalExecutionCount() {
        return streamer.metrics().stageMetrics(stageName).totalExecutionCount();
    }

    /** {@inheritDoc} */
    @Override public int getFailuresCount() {
        return streamer.metrics().stageMetrics(stageName).failuresCount();
    }

    /** {@inheritDoc} */
    @Override public boolean isExecuting() {
        return streamer.metrics().stageMetrics(stageName).executing();
    }
}
