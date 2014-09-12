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

package org.gridgain.grid.spi.checkpoint;

import org.gridgain.grid.*;
import org.gridgain.grid.compute.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.checkpoint.cache.*;
import org.gridgain.grid.spi.checkpoint.jdbc.*;
import org.gridgain.grid.spi.checkpoint.noop.*;
import org.gridgain.grid.spi.checkpoint.sharedfs.*;
import org.jetbrains.annotations.*;

/**
 * Checkpoint SPI provides an ability to save an intermediate job state. It can
 * be useful when long running jobs need to store some intermediate state to
 * protect from system or application failures. Grid job can save intermediate
 * state in certain points of the execution (e.g., periodically) and upon start
 * check if previously saved state exists. This allows job to restart from the last
 * save checkpoint in case of preemption or other types of failover.
 * <p>
 * Note, that since a job can execute on different nodes, checkpoints need to
 * be accessible by all nodes.
 * <p>
 * To manipulate checkpoints from grid job the following public methods are available
 * on task session (that can be injected into grid job):
 * <ul>
 * <li>{@link GridComputeTaskSession#loadCheckpoint(String)}</li>
 * <li>{@link GridComputeTaskSession#removeCheckpoint(String)}</li>
 * <li>{@link GridComputeTaskSession#saveCheckpoint(String, Object)}</li>
 * <li>{@link GridComputeTaskSession#saveCheckpoint(String, Object, GridComputeTaskSessionScope, long)}</li>
 * <li>{@link GridComputeTaskSession#saveCheckpoint(String, Object, GridComputeTaskSessionScope, long, boolean)}</li>
 * </ul>
 * <p>
 * GridGain provides the following {@code GridCheckpointSpi} implementations:
 * <ul>
 * <li>{@link GridNoopCheckpointSpi} - default</li>
 * <li>{@link GridSharedFsCheckpointSpi}</li>
 * <li>{@gglink org.gridgain.grid.spi.checkpoint.s3.GridS3CheckpointSpi}</li>
 * <li>{@link GridJdbcCheckpointSpi}</li>
 * <li>{@link GridCacheCheckpointSpi}</li>
 * </ul>
 * <b>NOTE:</b> this SPI (i.e. methods in this interface) should never be used directly. SPIs provide
 * internal view on the subsystem and is used internally by GridGain kernal. In rare use cases when
 * access to a specific implementation of this SPI is required - an instance of this SPI can be obtained
 * via {@link Grid#configuration()} method to check its configuration properties or call other non-SPI
 * methods. Note again that calling methods from this interface on the obtained instance can lead
 * to undefined behavior and explicitly not supported.
 */
public interface GridCheckpointSpi extends GridSpi {
    /**
     * Loads checkpoint from storage by its unique key.
     *
     * @param key Checkpoint key.
     * @return Loaded data or {@code null} if there is no data for a given
     *      key.
     * @throws GridSpiException Thrown in case of any error while loading
     *      checkpoint data. Note that in case when given {@code key} is not
     *      found this method will return {@code null}.
     */
    @Nullable public byte[] loadCheckpoint(String key) throws GridSpiException;

    /**
     * Saves checkpoint to the storage.
     *
     * @param key Checkpoint unique key.
     * @param state Saved data.
     * @param timeout Every intermediate data stored by checkpoint provider
     *      should have a timeout. Timeout allows for effective resource
     *      management by checkpoint provider by cleaning saved data that are not
     *      needed anymore. Generally, the user should choose the minimum
     *      possible timeout to avoid long-term resource acquisition by checkpoint
     *      provider. Value {@code 0} means that timeout will never expire.
     * @param overwrite Whether or not overwrite checkpoint if it already exists.
     * @return {@code true} if checkpoint has been actually saved, {@code false} otherwise.
     * @throws GridSpiException Thrown in case of any error while saving
     *    checkpoint data.
     */
    public boolean saveCheckpoint(String key, byte[] state, long timeout, boolean overwrite) throws GridSpiException;

    /**
     * This method instructs the checkpoint provider to clean saved data for a
     * given {@code key}.
     *
     * @param key Key for the checkpoint to remove.
     * @return {@code true} if data has been actually removed, {@code false}
     *      otherwise.
     */
    public boolean removeCheckpoint(String key);

    /**
     * Sets the checkpoint listener.
     *
     * @param lsnr The listener to set or {@code null}.
     */
    public void setCheckpointListener(GridCheckpointListener lsnr);
}
