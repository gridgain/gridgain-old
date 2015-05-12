/* @java.file.header */

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.kernal.processors.job;

import java.util.*;

/**
 * Job hold listener to notify job processor on {@code hold}
 * state change.
 */
interface GridJobHoldListener extends EventListener {
    /**
     * @param worker Held job worker.
     * @return {@code True} if worker has been h.
     */
    public boolean onHeld(GridJobWorker worker);

    /**
     * @param worker Unheld job worker.
     *
     */
    public boolean onUnheld(GridJobWorker worker);
}
