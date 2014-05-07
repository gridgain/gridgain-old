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

package org.gridgain.grid.dr.hub.sender.store;

import org.gridgain.grid.*;
import org.gridgain.grid.dr.hub.sender.store.fs.*;
import org.gridgain.grid.dr.hub.sender.store.memory.*;

/**
 * Data center replication sender hub store. Persists batches received from data nodes until they are successfully
 * sent to receiver hub.
 * <p>
 * Gridgain provides the following {@code GridDrStore} implementations:
 * <ul>
 * <li>{@link GridDrSenderHubInMemoryStore}</li>
 * <li>{@link GridDrSenderHubFsStore}</li>
 * </ul>
 */
public interface GridDrSenderHubStore {
    /**
     * Store data.
     *
     * @param dataCenterIds Target data center ids.
     * @param data Data center replication store entry.
     * @throws GridException In case of failure.
     * @throws GridDrSenderHubStoreOverflowException If overflowed.
     */
    public void store(byte[] dataCenterIds, byte[] data) throws GridException, GridDrSenderHubStoreOverflowException;

    /**
     * Gets cursor for the given data center ID.
     *
     * @param dataCenterId Data center ID.
     * @return Cursor over stored entries which belongs to that data center ID.
     * @throws GridException If failed.
     */
    public GridDrSenderHubStoreCursor cursor(byte dataCenterId) throws GridException;
}
