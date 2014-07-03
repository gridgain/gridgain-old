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

package org.gridgain.grid.dr.cache.sender;

import org.jetbrains.annotations.*;

/**
 * Metrics for data center replication sender cache.
 */
public interface GridDrSenderCacheMetrics {
    /**
     * Gets total amount of batches sent to sender hubs.
     *
     * @return Total amount of batches sent to sender hubs.
     */
    public int batchesSent();

    /**
     * Gets total amount of entries sent to sender hubs.
     *
     * @return Total amount of entries sent to sender hubs.
     */
    public long entriesSent();

    /**
     * Gets total amount of sent batches acknowledged by sender hub.
     *
     * @return Total amount of sent batches acknowledged by sender hub.
     */
    public int batchesAcked();

    /**
     * Gets total amount of sent entries acknowledged by sender hub.
     *
     * @return Total amount of sent entries acknowledged by sender hub.
     */
    public long entriesAcked();

    /**
     * Gets total amount of failed batches. Failure may occur because there was no available sender hubs during
     * batch send or sender hub replied with an error.
     *
     * @return Total amount of failed batches.
     */
    public int batchesFailed();

    /**
     * Gets total amount of filtered cache entries. Data center replication provides ability to filter entries
     * before being replicated to another data center (see {@link GridDrSenderCacheConfiguration#getEntryFilter()}).
     * This metric returns the number of entries that didn't pass that filter.
     *
     * @return Total amount of filtered cache entries.
     */
    public long entriesFiltered();

    /**
     * Gets current amount of cache entries in backup queue.
     *
     * @return Current amount of cache entries in backup queue.
     */
    public long backupQueueSize();

    /**
     * Gets data center replication status.
     *
     * @return Data center replication status.
     */
    public GridDrStatus status();
}
