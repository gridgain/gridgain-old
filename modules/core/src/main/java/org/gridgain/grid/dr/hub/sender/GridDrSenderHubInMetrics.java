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

package org.gridgain.grid.dr.hub.sender;

/**
 * Data center replication sender hub metrics for incoming data, i.e. data transferred from sender caches to
 * sender hub.
 */
public interface GridDrSenderHubInMetrics {
    /**
     * Gets amount of batches received from sender caches.
     *
     * @return Amount of batches received from sender caches.
     */
    public int batchesReceived();

    /**
     * Gets amount of cache entries received from sender caches.
     *
     * @return Amount of cache entries received from sender caches.
     */
    public long entriesReceived();

    /**
     * Gets amount of bytes received from sender caches.
     *
     * @return Amount of bytes received from sender caches.
     */
    public long bytesReceived();
}
