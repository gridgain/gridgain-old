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

package org.gridgain.grid.kernal.visor.gui.tasks;

import org.gridgain.grid.*;
import org.gridgain.grid.compute.*;
import org.gridgain.grid.kernal.processors.task.*;
import org.gridgain.grid.kernal.visor.cmd.*;
import org.gridgain.grid.util.lang.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * Ping other node.
 */
@GridInternal
public class VisorPingTask extends VisorOneNodeTask<UUID, GridTuple3<Boolean, Long, Long>> {
    /** */
    private static final long serialVersionUID = 0L;

    /** {@inheritDoc} */
    @Override protected VisorPingJob job(UUID arg) {
        return new VisorPingJob(arg);
    }

    /** {@inheritDoc} */
    @Nullable @Override public GridTuple3<Boolean, Long, Long> reduce(List<GridComputeJobResult> results) throws GridException {
        try {
            return super.reduce(results);
        } catch (GridEmptyProjectionException ignored) {
            return new GridTuple3<>(false, -1L, -1L);
        } catch (GridTopologyException ignored) {
            return new GridTuple3<>(false, -1L, -1L);
        }
    }

    /**
     * Job that ping node.
     */
    private static class VisorPingJob extends VisorJob<UUID, GridTuple3<Boolean, Long, Long>> {
        /** */
        private static final long serialVersionUID = 0L;

        /**
         * @param arg Node ID to ping.
         */
        protected VisorPingJob(UUID arg) {
            super(arg);
        }

        /** {@inheritDoc} */
        @Override protected GridTuple3<Boolean, Long, Long> run(UUID nodeToPing) throws GridException {
            long start = System.currentTimeMillis();

            return new GridTuple3<>(g.pingNode(nodeToPing), start, System.currentTimeMillis());
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(VisorPingJob.class, this);
        }
    }
}
