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
import org.gridgain.grid.dr.*;
import org.gridgain.grid.dr.cache.sender.*;
import org.gridgain.grid.kernal.processors.task.*;
import org.gridgain.grid.kernal.visor.cmd.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.util.typedef.internal.*;

/**
 * Task for suspend or resume DR replication state.
 */
@GridInternal
public class VisorDrSenderCacheChangeReplicationStateTask
    extends VisorOneNodeTask<GridBiTuple<String, Boolean>, GridDrStatus> {
    /** */
    private static final long serialVersionUID = 0L;

    /** {@inheritDoc} */
    @Override protected VisorDrSenderCacheChangeReplicationStateJob job(GridBiTuple<String, Boolean> arg) {
        return new VisorDrSenderCacheChangeReplicationStateJob(arg);
    }

    /**
     * Job that change replication state.
     */
    private static class VisorDrSenderCacheChangeReplicationStateJob extends VisorJob<GridBiTuple<String, Boolean>,
        GridDrStatus> {
        /** */
        private static final long serialVersionUID = 0L;

        /**
         * @param arg Cache name and flag whether to resume or suspend replication.
         */
        private VisorDrSenderCacheChangeReplicationStateJob(GridBiTuple<String, Boolean> arg) {
            super(arg);
        }

        /** {@inheritDoc} */
        @Override protected GridDrStatus run(GridBiTuple<String, Boolean> arg) throws GridException {
            GridDr dr = g.dr();

            String cacheName = arg.get1();

            boolean resume = arg.get2() != null ? arg.get2() : false;

            if (resume)
                dr.senderCacheDrResume(cacheName);
            else
                dr.senderCacheDrPause(cacheName);

            return dr.senderCacheMetrics(cacheName).status();
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(VisorDrSenderCacheChangeReplicationStateJob.class, this);
        }
    }
}
