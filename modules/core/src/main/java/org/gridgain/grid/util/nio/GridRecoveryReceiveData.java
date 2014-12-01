/* @java.file.header */

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.util.nio;

import org.gridgain.grid.logger.*;
import org.gridgain.grid.util.future.*;
import org.gridgain.grid.util.typedef.internal.*;

/**
 *
 */
public class GridRecoveryReceiveData {
    /** */
    private long rcvCntr;

    /** */
    private boolean reserved;

    /** */
    private final GridLogger log;

    /** */
    private GridFutureAdapter<GridCommunicationClient> fut;

    /** */
    private volatile long lastSent;

    /**
     * @param log Logger.
     */
    public GridRecoveryReceiveData(GridLogger log) {
        this.log = log;
    }

    /**
     * @return Number of received messages.
     */
    public long messageReceived() {
        rcvCntr++;

        //log.info("Recovery received total: " + rcvCntr);

        return rcvCntr;
    }

    /**
     * @param lastSent Last ID sent on idle timeout.
     */
    public void lastSent(long lastSent) {
        this.lastSent = lastSent;
    }

    /**
     * @return
     */
    public long lastSent() {
        return lastSent;
    }

    /**
     * @return Received messages cout.
     */
    public long receivedCount() {
        return rcvCntr;
    }

    /**
     * @param fut
     * @throws InterruptedException If interrupted.
     */
    public void setFuture(GridFutureAdapter<GridCommunicationClient> fut) throws InterruptedException {
        synchronized (this) {
            while (reserved)
                wait();

            assert this.fut == null;

            this.fut = fut;
        }
    }

    /**
     *
     * @return
     * @throws InterruptedException If interrupted.
     */
    public GridFutureAdapter<GridCommunicationClient> reserve() throws InterruptedException {
        synchronized (this) {
            while (fut == null && reserved)
                wait();

            reserved = true;

            return fut;
        }
    }

    /**
     *
     */
    public void release() {
        synchronized (this) {
            reserved = false;

            fut = null;

            notifyAll();
        }
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridRecoveryReceiveData.class, this);
    }
}
