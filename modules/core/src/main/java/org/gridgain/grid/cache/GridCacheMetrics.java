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

package org.gridgain.grid.cache;

import org.gridgain.grid.dr.cache.receiver.*;
import org.gridgain.grid.dr.cache.sender.*;

import java.io.*;

/**
 * Cache metrics used to obtain statistics on cache itself.
 * Use {@link GridCache#metrics()} to obtain metrics for a cache.
 */
public interface GridCacheMetrics extends Serializable {
    /**
     * Gets create time of the owning entity (either cache or entry).
     *
     * @return Create time.
     */
    public long createTime();

    /**
     * Gets last write time of the owning entity (either cache or entry).
     *
     * @return Last write time.
     */
    public long writeTime();

    /**
     * Gets last read time of the owning entity (either cache or entry).
     *
     * @return Last read time.
     */
    public long readTime();

    /**
     * Gets last time transaction was committed.
     *
     * @return Last commit time.
     */
    public long commitTime();

    /**
     * Gets last time transaction was rollback.
     *
     * @return Last rollback time.
     */
    public long rollbackTime();

    /**
     * Gets total number of reads of the owning entity (either cache or entry).
     *
     * @return Total number of reads.
     */
    public int reads();

    /**
     * Gets total number of writes of the owning entity (either cache or entry).
     *
     * @return Total number of writes.
     */
    public int writes();

    /**
     * Gets total number of hits for the owning entity (either cache or entry).
     *
     * @return Number of hits.
     */
    public int hits();

    /**
     * Gets total number of misses for the owning entity (either cache or entry).
     *
     * @return Number of misses.
     */
    public int misses();

    /**
     * Gets total number of transaction commits.
     *
     * @return Number of transaction commits.
     */
    public int txCommits();

    /**
     * Gets total number of transaction rollbacks.
     *
     * @return Number of transaction rollbacks.
     */
    public int txRollbacks();

    /**
     * Gets metrics for data sent during data center replication, if data center replication
     * is not configured then {@link IllegalStateException} will be thrown.
     *
     * @return Metrics for data sent during data center replication.
     */
    public GridDrSenderCacheMetrics drSendMetrics();

    /**
     * Gets metrics for data received during data center replication, if data center replication
     * is not configured then {@link IllegalStateException} will be thrown.
     *
     * @return Metrics for data received during data center replication.
     */
    public GridDrReceiverCacheMetrics drReceiveMetrics();
}
