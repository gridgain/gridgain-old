/* @java.file.header */

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.util.nio;

import org.gridgain.grid.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.util.future.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.jetbrains.annotations.*;

/**
 * Recovery information for received messages.
 */
public class GridRecoveryReceiveData {
    /** Number of received messages. */
    private long rcvCnt;

    /** Reserved flag. */
    private boolean reserved;

    /** Last value sent on idle timeout. */
    private volatile long lastAck;

    /** Target node. */
    private final GridNode node;

    /** Logger. */
    private final GridLogger log;

    /** */
    private GridFutureAdapter<GridCommunicationClient> fut;

    /**
     * @param node Node.
     * @param log Logger.
     */
    public GridRecoveryReceiveData(GridNode node, GridLogger log) {
        assert !node.isLocal() : node;

        this.node = node;
        this.log = log;
    }

    /**
     * @return Node.
     */
    public GridNode node() {
        return node;
    }

    /**
     * Increments received messages counter.
     *
     * @return Number of received messages.
     */
    public long onReceived() {
        rcvCnt++;

        return rcvCnt;
    }

    /**
     * @return Number of received messages.
     */
    public long received() {
        return rcvCnt;
    }

    /**
     * @param lastAck Last sent.
     */
    public void lastAcknowledged(long lastAck) {
        this.lastAck = lastAck;
    }

    /**
     * @return
     */
    public long lastAcknowledged() {
        return lastAck;
    }

    /**
     * @return Received messages cout.
     */
    public long receivedCount() {
        return rcvCnt;
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
    @Nullable public GridFutureAdapter<GridCommunicationClient> reserve() throws InterruptedException {
        synchronized (this) {
            while (fut == null && reserved)
                wait();

            reserved = true;

            return fut;
        }
    }

    /**
     * Releases receive data.
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
