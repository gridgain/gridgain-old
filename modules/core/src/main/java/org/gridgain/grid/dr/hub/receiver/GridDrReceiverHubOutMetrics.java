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

package org.gridgain.grid.dr.hub.receiver;

/**
 * Data center replication receiver hub metrics for outgoing data, i.e. data transferred from receiver hub to
 * receiver caches.
 */
public interface GridDrReceiverHubOutMetrics {
    /**
     * Gets amount of batches waiting to be stored in receiver caches.
     *
     * @return Amount of batches waiting to be stored in receiver caches.
     */
    public int batchesSent();

    /**
     * Gets amount of entries waiting to be stored in receiver caches.
     *
     * @return Amount of entries waiting to be stored in receiver caches.
     */
    public long entriesSent();

    /**
     * Gets amount of bytes waiting to be stored.
     *
     * @return Amount of bytes waiting to be stored.
     */
    public long bytesSent();

    /**
     * Gets amount of batches stored in receiver caches.
     *
     * @return Amount of batches stored in receiver caches.
     */
    public int batchesAcked();

    /**
     * Gets amount of cache entries stored in receiver caches.
     *
     * @return Amount of cache entries stored in receiver caches.
     */
    public long entriesAcked();

    /**
     * Gets amount of bytes stored in receiver caches.
     *
     * @return Amount of bytes stored in receiver caches.
     */
    public long bytesAcked();

    /**
     * Gets average time in milliseconds between sending batch to receiver cache nodes and successfully storing it.
     *
     * @return Average time in milliseconds between sending batch to receiver cache nodes and successfully storing it.
     */
    public double averageBatchAckTime();
}
