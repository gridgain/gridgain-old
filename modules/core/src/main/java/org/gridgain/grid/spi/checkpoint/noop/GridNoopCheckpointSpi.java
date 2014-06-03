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

package org.gridgain.grid.spi.checkpoint.noop;

import org.gridgain.grid.logger.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.checkpoint.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.jetbrains.annotations.*;

/**
 * No-op implementation of {@link GridCheckpointSpi}. This is default implementation
 * since {@code 4.5.0} version.
 */
@GridSpiNoop
@GridSpiMultipleInstancesSupport(true)
public class GridNoopCheckpointSpi extends GridSpiAdapter implements GridCheckpointSpi {
    /** Logger. */
    @GridLoggerResource
    private GridLogger log;

    /** {@inheritDoc} */
    @Override public void spiStart(@Nullable String gridName) throws GridSpiException {
        U.warn(log, "Checkpoints are disabled (to enable configure any GridCheckpointSpi implementation)");
    }

    /** {@inheritDoc} */
    @Override public void spiStop() throws GridSpiException {
        // No-op.
    }

    /** {@inheritDoc} */
    @Nullable @Override public byte[] loadCheckpoint(String key) throws GridSpiException {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean saveCheckpoint(String key, byte[] state, long timeout, boolean overwrite) throws GridSpiException {
        return false;
    }

    /** {@inheritDoc} */
    @Override public boolean removeCheckpoint(String key) {
        return false;
    }

    /** {@inheritDoc} */
    @Override public void setCheckpointListener(GridCheckpointListener lsnr) {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridNoopCheckpointSpi.class, this);
    }
}
