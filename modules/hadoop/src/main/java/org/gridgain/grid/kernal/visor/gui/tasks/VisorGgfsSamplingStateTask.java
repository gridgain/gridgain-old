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
import org.gridgain.grid.kernal.processors.ggfs.*;
import org.gridgain.grid.kernal.processors.task.*;
import org.gridgain.grid.kernal.visor.cmd.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.util.typedef.internal.*;

/**
 * Task to set GGFS instance sampling state.
 */
@GridInternal
public class VisorGgfsSamplingStateTask extends VisorOneNodeTask<GridBiTuple<String, Boolean>, Void> {
    /** */
    private static final long serialVersionUID = 0L;

    /**
     * Job that perform parsing of GGFS profiler logs.
     */
    private static class VisorGgfsSamplingStateJob extends VisorJob<GridBiTuple<String, Boolean>, Void> {
        /** */
        private static final long serialVersionUID = 0L;

        /**
         * Create job with given argument.
         *
         * @param arg Job argument.
         */
        public VisorGgfsSamplingStateJob(GridBiTuple<String, Boolean> arg) {
            super(arg);
        }

        /** {@inheritDoc} */
        @Override protected Void run(GridBiTuple<String, Boolean> arg) throws GridException {
            try {
                ((GridGgfsEx) g.ggfs(arg.get1())).globalSampling(arg.get2());

                return null;
            }
            catch (IllegalArgumentException iae) {
                throw new GridException("Failed to set sampling state for GGFS: " + arg.get1(), iae);
            }
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(VisorGgfsSamplingStateJob.class, this);
        }
    }

    /** {@inheritDoc} */
    @Override protected VisorGgfsSamplingStateJob job(GridBiTuple<String, Boolean> arg) {
        return new VisorGgfsSamplingStateJob(arg);
    }
}
