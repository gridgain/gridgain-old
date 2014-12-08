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

import static  org.gridgain.grid.kernal.visor.cmd.VisorTaskUtils.*;

/**
 * Change debug level for Visor tasks, jobs.
 */
@GridInternal
public class VisorDebugTask extends VisorOneNodeTask<Boolean, Void> {
    /** */
    private static final long serialVersionUID = 0L;

    /** {@inheritDoc} */
    @Override protected VisorJob<Boolean, Void> job(Boolean arg) {
        return new VisorDebugJob(arg);
    }

    /**
     * Job that change debug level for Visor tasks, jobs.
     */
    private static class VisorDebugJob extends VisorJob<Boolean, Void> {
        /** */
        private static final long serialVersionUID = 0L;

        /**
         * @param arg New debug level.
         */
        protected VisorDebugJob(@Nullable Boolean arg) {
            super(arg);
        }

        /** {@inheritDoc} */
        @Override protected Void run(@Nullable Boolean newVal) throws GridException {
            debugState(g, newVal);

            return null;
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(VisorDebugJob.class, this);
        }
    }
}
