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

package org.gridgain.grid.kernal.visor.cmd.dto;

import org.gridgain.grid.util.typedef.internal.*;

import java.io.*;

/**
 * Data transfer object for aggregated cache query metrics.
 */
public class VisorCacheQueryAggregatedMetrics implements Serializable {
    /** */
    private static final long serialVersionUID = 0L;

    /** Minimum execution time of query. */
    private long minTime = Long.MAX_VALUE;

    /** Maximum execution time of query. */
    private long maxTime = Long.MIN_VALUE;

    /** Average execution time of query. */
    private double avgTime;

    /** Total execution time of query. */
    private long totalTime;

    /** Number of executions. */
    private int execs;

    /** Total number of times a query execution failed. */
    private int fails;

    /**
     * @return Minimum execution time of query.
     */
    public long minTime() {
        return minTime;
    }

    /**
     * @param minTime New min time.
     */
    public void minTime(long minTime) {
        this.minTime = Math.min(this.minTime, minTime);
    }

    /**
     * @return Maximum execution time of query.
     */
    public long maxTime() {
        return maxTime;
    }

    /**
     * @param maxTime New max time.
     */
    public void maxTime(long maxTime) {
        this.maxTime = Math.max(this.maxTime, maxTime);
    }

    /**
     * @return Average execution time of query.
     */
    public double avgTime() {
        return avgTime;
    }

    /**
     * @param avgTime New avg time.
     */
    public void avgTime(double avgTime) {
        this.avgTime = avgTime;
    }

    /**
     * @return Total execution time of query.
     */
    public long totalTime() {
        return totalTime;
    }

    /**
     * @param totalTime New total time.
     */
    public void totalTime(long totalTime) {
        this.totalTime = totalTime;
    }

    /**
     * @return Number of executions.
     */
    public int execs() {
        return execs;
    }

    /**
     * @param execs New execs.
     */
    public void execs(int execs) {
        this.execs = execs;
    }

    /**
     * @return Total number of times a query execution failed.
     */
    public int fails() {
        return fails;
    }

    /**
     * @param fails New fails.
     */
    public void fails(int fails) {
        this.fails = fails;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(VisorCacheQueryAggregatedMetrics.class, this);
    }
}
