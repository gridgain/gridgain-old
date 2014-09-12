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
 * Data transfer object for write-behind cache configuration properties.
 */
public class VisorWriteBehindConfig implements Serializable {
    /** */
    private static final long serialVersionUID = 0L;

    /** Flag indicating whether write-behind behaviour should be used for the cache store. */
    private boolean enabled;

    /** Maximum batch size for write-behind cache store operations. */
    private int batchSize;

    /** Frequency with which write-behind cache is flushed to the cache store in milliseconds. */
    private long flushFrequency;

    /** Maximum object count in write-behind cache. */
    private int flushSize;

    /** Number of threads that will perform cache flushing. */
    private int flushThreadCnt;

    /**
     * @param ccfg Cache configuration.
     * @return Data transfer object for write-behind cache configuration properties.
     */
    public static VisorWriteBehindConfig from(GridCacheConfiguration ccfg) {
        VisorWriteBehindConfig cfg = new VisorWriteBehindConfig();

        cfg.enabled(ccfg.isWriteBehindEnabled());
        cfg.batchSize(ccfg.getWriteBehindBatchSize());
        cfg.flushFrequency(ccfg.getWriteBehindFlushFrequency());
        cfg.flushSize(ccfg.getWriteBehindFlushSize());
        cfg.flushThreadCount(ccfg.getWriteBehindFlushThreadCount());

        return cfg;
    }

    /**
     * @return Flag indicating whether write-behind behaviour should be used for the cache store.
     */
    public boolean enabled() {
        return enabled;
    }

    /**
     * @param enabled New flag indicating whether write-behind behaviour should be used for the cache store.
     */
    public void enabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * @return Maximum batch size for write-behind cache store operations.
     */
    public int batchSize() {
        return batchSize;
    }

    /**
     * @param batchSize New maximum batch size for write-behind cache store operations.
     */
    public void batchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    /**
     * @return Frequency with which write-behind cache is flushed to the cache store in milliseconds.
     */
    public long flushFrequency() {
        return flushFrequency;
    }

    /**
     * @param flushFreq New frequency with which write-behind cache is flushed to the cache store in milliseconds.
     */
    public void flushFrequency(long flushFreq) {
        flushFrequency = flushFreq;
    }

    /**
     * @return Maximum object count in write-behind cache.
     */
    public int flushSize() {
        return flushSize;
    }

    /**
     * @param flushSize New maximum object count in write-behind cache.
     */
    public void flushSize(int flushSize) {
        this.flushSize = flushSize;
    }

    /**
     * @return Number of threads that will perform cache flushing.
     */
    public int flushThreadCount() {
        return flushThreadCnt;
    }

    /**
     * @param flushThreadCnt New number of threads that will perform cache flushing.
     */
    public void flushThreadCount(int flushThreadCnt) {
        this.flushThreadCnt = flushThreadCnt;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(VisorWriteBehindConfig.class, this);
    }
}
