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
import org.gridgain.grid.kernal.processors.task.*;
import org.gridgain.grid.kernal.visor.cmd.*;
import org.gridgain.grid.kernal.visor.gui.dto.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.util.typedef.internal.*;

import java.lang.management.*;

/**
 * Creates thread dump.
 */
@GridInternal
public class VisorDumpThreadTask extends VisorOneNodeTask<Void, GridBiTuple<VisorThreadInfo[], long[]>> {
    /** */
    private static final long serialVersionUID = 0L;

    /** {@inheritDoc} */
    @Override protected VisorDumpThreadJob job(Void arg) {
        return new VisorDumpThreadJob(arg);
    }

    /**
     * Job that take thread dump on node.
     */
    private static class VisorDumpThreadJob extends VisorJob<Void, GridBiTuple<VisorThreadInfo[], long[]>> {
        /** */
        private static final long serialVersionUID = 0L;

        /**
         * @param arg Formal job argument.
         */
        private VisorDumpThreadJob(Void arg) {
            super(arg);
        }

        /** {@inheritDoc} */
        @Override protected GridBiTuple<VisorThreadInfo[], long[]> run(Void arg) throws GridException {
            ThreadMXBean mx = U.getThreadMx();

            ThreadInfo[] info = mx.dumpAllThreads(true, true);

            VisorThreadInfo[] visorInfo = new VisorThreadInfo[info.length];

            for (int i = 0; i < info.length; i++)
                visorInfo[i] = VisorThreadInfo.from(info[i]);

            return new GridBiTuple<>(visorInfo, mx.findDeadlockedThreads());
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(VisorDumpThreadJob.class, this);
        }
    }
}
