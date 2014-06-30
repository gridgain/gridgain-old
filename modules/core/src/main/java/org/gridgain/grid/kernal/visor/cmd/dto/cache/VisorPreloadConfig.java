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

package org.gridgain.grid.kernal.visor.cmd.dto.cache;

import org.gridgain.grid.cache.*;
import org.gridgain.grid.util.typedef.internal.*;

import java.io.*;

/**
 * Data transfer object for cache preload configuration properties.
 */
public class VisorPreloadConfig implements Serializable {
    /** */
    private static final long serialVersionUID = 0L;

    /** Cache preload mode. */
    private GridCachePreloadMode mode;

    /** Preload thread pool size. */
    private int threadPoolSize;

    /** Cache preload batch size. */
    private int batchSize;

    /** Preloading partitioned delay. */
    private long partitionedDelay;

    /** Time in milliseconds to wait between preload messages. */
    private long throttle;

    /** Preload timeout. */
    private long timeout;

    /**
     * @param ccfg Cache configuration.
     * @return Data transfer object for preload configuration properties.
     */
    public static VisorPreloadConfig from(GridCacheConfiguration ccfg) {
        VisorPreloadConfig cfg = new VisorPreloadConfig();

        cfg.mode(ccfg.getPreloadMode());
        cfg.batchSize(ccfg.getPreloadBatchSize());
        cfg.threadPoolSize(ccfg.getPreloadThreadPoolSize());
        cfg.partitionedDelay(ccfg.getPreloadPartitionedDelay());
        cfg.throttle(ccfg.getPreloadThrottle());
        cfg.timeout(ccfg.getPreloadTimeout());

        return cfg;
    }

    /**
     * @return Cache preload mode.
     */
    public GridCachePreloadMode mode() {
        return mode;
    }

    /**
     * @param mode New cache preload mode.
     */
    public void mode(GridCachePreloadMode mode) {
        this.mode = mode;
    }

    /**
     * @return Preload thread pool size.
     */
    public int threadPoolSize() {
        return threadPoolSize;
    }

    /**
     * @param threadPoolSize New preload thread pool size.
     */
    public void threadPoolSize(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }

    /**
     * @return Cache preload batch size.
     */
    public int batchSize() {
        return batchSize;
    }

    /**
     * @param batchSize New cache preload batch size.
     */
    public void batchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    /**
     * @return Preloading partitioned delay.
     */
    public long partitionedDelay() {
        return partitionedDelay;
    }

    /**
     * @param partitionedDelay New preloading partitioned delay.
     */
    public void partitionedDelay(long partitionedDelay) {
        this.partitionedDelay = partitionedDelay;
    }

    /**
     * @return Time in milliseconds to wait between preload messages.
     */
    public long throttle() {
        return throttle;
    }

    /**
     * @param throttle New time in milliseconds to wait between preload messages.
     */
    public void throttle(long throttle) {
        this.throttle = throttle;
    }

    /**
     * @return Preload timeout.
     */
    public long timeout() {
        return timeout;
    }

    /**
     * @param timeout New preload timeout.
     */
    public void timeout(long timeout) {
        this.timeout = timeout;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(VisorPreloadConfig.class, this);
    }
}
