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

package org.gridgain.grid.kernal.processors.hadoop;

import org.gridgain.grid.*;
import org.gridgain.grid.logger.*;

/**
 * Abstract class for all hadoop components.
 */
public abstract class GridHadoopComponent {
    /** Hadoop context. */
    protected GridHadoopContext ctx;

    /** Logger. */
    protected GridLogger log;

    /**
     * @param ctx Hadoop context.
     */
    public void start(GridHadoopContext ctx) throws GridException {
        this.ctx = ctx;

        log = ctx.kernalContext().log(getClass());
    }

    /**
     * Stops manager.
     */
    public void stop(boolean cancel) {
        // No-op.
    }

    /**
     * Callback invoked when all grid components are started.
     */
    public void onKernalStart() throws GridException {
        // No-op.
    }

    /**
     * Callback invoked before all grid components are stopped.
     */
    public void onKernalStop(boolean cancel) {
        // No-op.
    }
}
