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

package org.gridgain.grid.kernal.visor.cmd;

import org.gridgain.grid.*;
import org.gridgain.grid.compute.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.jetbrains.annotations.*;

import static org.gridgain.grid.kernal.visor.cmd.VisorTaskUtils.*;

/**
 * Base class for Visor jobs.
 */
public abstract class VisorJob<A, R> extends GridComputeJobAdapter {
    @GridInstanceResource
    protected GridEx g;

    @GridLoggerResource
    @Deprecated
    protected GridLogger log;

    /** Job start time. */
    protected transient long start;

    /** Debug flag. */
    protected transient boolean debug;

    /**
     * Create job with specified argument.
     *
     * @param arg Job argument.
     */
    protected VisorJob(@Nullable A arg) {
        super(arg);
    }

    /** {@inheritDoc} */
    @Nullable @Override public Object execute() throws GridException {
        start = U.currentTimeMillis();

        debug = debugState(g);

        try {
            if (debug)
                logStart(g.log(), getClass(), start);

            return run((A)argument(0));
        }
        finally {
            if (debug)
                logFinish(g.log(), getClass(), start);
        }
    }

    /**
     * Execution logic of concrete task.
     *
     * @return Result.
     */
    protected abstract R run(@Nullable A arg) throws GridException;
}
