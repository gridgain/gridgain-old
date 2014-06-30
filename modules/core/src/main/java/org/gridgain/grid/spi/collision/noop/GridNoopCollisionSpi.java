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

package org.gridgain.grid.spi.collision.noop;

import org.gridgain.grid.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.collision.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.jetbrains.annotations.*;

/**
 * No-op implementation of {@link GridCollisionSpi}. This is default implementation
 * since {@code 4.5.0} version. When grid is started with {@link GridNoopCollisionSpi}
 * jobs are activated immediately on arrival to mapped node. This approach suits well
 * for large amount of small jobs (which is a wide-spread use case). User still can
 * control the number of concurrent jobs by setting maximum thread pool size defined
 * by {@link GridConfiguration#getExecutorService()} configuration property.
 */
@GridSpiNoop
@GridSpiMultipleInstancesSupport(true)
public class GridNoopCollisionSpi extends GridSpiAdapter implements GridCollisionSpi {
    /** {@inheritDoc} */
    @Override public void spiStart(@Nullable String gridName) throws GridSpiException {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override public void spiStop() throws GridSpiException {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override public void onCollision(GridCollisionContext ctx) {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override public void setExternalCollisionListener(@Nullable GridCollisionExternalListener lsnr) {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridNoopCollisionSpi.class, this);
    }
}
