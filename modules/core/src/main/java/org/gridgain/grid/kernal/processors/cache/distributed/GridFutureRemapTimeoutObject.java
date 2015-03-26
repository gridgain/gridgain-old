/* @java.file.header */

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.kernal.processors.cache.distributed;

import org.gridgain.grid.GridException;
import org.gridgain.grid.kernal.processors.timeout.GridTimeoutObjectAdapter;
import org.gridgain.grid.util.future.GridFutureAdapter;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Future remap timeout object.
 */
public class GridFutureRemapTimeoutObject extends GridTimeoutObjectAdapter {
    /** */
    private final GridFutureAdapter<?> fut;

    /** Finished flag. */
    private final AtomicBoolean finished = new AtomicBoolean();

    /** Topology version to wait. */
    private final long topVer;

    /** Exception cause. */
    private final GridException e;

    /**
     * @param fut Future.
     * @param timeout Timeout.
     * @param topVer Topology version timeout was created on.
     * @param e Exception cause.
     */
    public GridFutureRemapTimeoutObject(
        GridFutureAdapter<?> fut,
        long timeout,
        long topVer,
        GridException e) {
        super(timeout);

        this.fut = fut;
        this.topVer = topVer;
        this.e = e;
    }

    /** {@inheritDoc} */
    @Override public void onTimeout() {
        if (finish()) // Fail the whole get future, else remap happened concurrently.
            fut.onDone(new GridException("Failed to wait for topology version to change: " + topVer, e));
    }

    /**
     * @return Guard against concurrent completion.
     */
    public boolean finish() {
        return finished.compareAndSet(false, true);
    }
}
