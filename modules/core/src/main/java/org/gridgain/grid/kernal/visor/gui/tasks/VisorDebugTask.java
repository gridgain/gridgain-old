/* @java.file.header */

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
import org.gridgain.grid.util.typedef.internal.*;
import org.jetbrains.annotations.*;

/**
 * Change debug level for Visor task.
 */
@GridInternal
public class VisorDebugTask extends VisorOneNodeTask<Long, Void> {
    /** */
    private static final long serialVersionUID = 0L;

    /** {@inheritDoc} */
    @Override protected VisorJob<Long, Void> job(Long arg) {
        return new VisorDebugJob(arg);
    }

    private static class VisorDebugJob extends VisorJob<Long, Void> {
        /** */
        private static final long serialVersionUID = 0L;

        /**
         * @param arg New debug level.
         */
        protected VisorDebugJob(@Nullable Long arg) {
            super(arg);
        }

        /** {@inheritDoc} */
        @Override protected Void run(@Nullable Long arg) throws GridException {
            g.cachex(CU.UTILITY_CACHE_NAME).putx("VISOR_DEBUG", arg);

            return null;
        }
        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(VisorDebugJob.class, this);
        }
    }
}
