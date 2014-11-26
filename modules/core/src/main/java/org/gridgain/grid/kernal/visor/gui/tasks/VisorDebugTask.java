/* @java.file.header */

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.kernal.visor.gui.tasks;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.datastructures.*;
import org.gridgain.grid.kernal.processors.task.*;
import org.gridgain.grid.kernal.visor.cmd.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.jetbrains.annotations.*;

/**
 * Change debug level for Visor task.
 */
@GridInternal
public class VisorDebugTask extends VisorOneNodeTask<Boolean, Boolean> {
    /** */
    private static final long serialVersionUID = 0L;

    public static final String VISOR_DEBUG_KEY = "VISOR_DEBUG_KEY";

    /** {@inheritDoc} */
    @Override protected VisorJob<Boolean, Boolean> job(Boolean arg) {
        return new VisorDebugJob(arg);
    }

    private static class VisorDebugJob extends VisorJob<Boolean, Boolean> {
        /** */
        private static final long serialVersionUID = 0L;

        /**
         * @param arg New debug level.
         */
        protected VisorDebugJob(@Nullable Boolean arg) {
            super(arg);
        }

        /** {@inheritDoc} */
        @Override protected Boolean run(@Nullable Boolean newVal) throws GridException {
            GridCacheAtomicReference<Boolean> debug = g.cachex(CU.UTILITY_CACHE_NAME).dataStructures().
                atomicReference(VISOR_DEBUG_KEY, false, false);

            boolean expVal = !newVal;

            return debug.compareAndSet(expVal, newVal) ? newVal : expVal;
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(VisorDebugJob.class, this);
        }
    }
}
