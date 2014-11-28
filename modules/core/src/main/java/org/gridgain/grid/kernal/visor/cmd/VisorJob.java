/* @java.file.header */

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
import org.gridgain.grid.kernal.visor.gui.tasks.*;
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

    protected long start;

    protected boolean debug;

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
        A arg = argument(0);

        debug = g.cachex(CU.UTILITY_CACHE_NAME).dataStructures().
            atomicReference(VisorDebugTask.VISOR_DEBUG_KEY, false, true).get();

        start = U.currentTimeMillis();

        if (debug)
            logStartJob(g.log(), getClass(), start);

        R result = run(arg);

        if (debug)
            logFinishJob(g.log(), getClass(), start);

        return result;
    }

    /**
     * Execution logic of concrete task.
     *
     * @return Result.
     */
    protected abstract R run(@Nullable A arg) throws GridException;
}
