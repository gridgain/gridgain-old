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
import org.gridgain.grid.util.typedef.internal.*;

import java.io.*;
import java.util.*;

/**
 * Recovery information for node.
 */
public class GridNioRecoveryData {
    /** Number of acknowledged messages. */
    private long acked;

    /** Unacknowledged message futures. */
    private ArrayDeque<GridNioFuture<?>> msgFuts = new ArrayDeque<>(1024);

    /** Number of messages to resend. */
    private int resendCnt;

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

    /**
     * @param node Node.
     * @param log Logger.
     */
    public GridNioRecoveryData(GridNode node, GridLogger log) {
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
     * @param fut NIO future.
     */
    public void add(GridNioFuture<?> fut) {
        if (!fut.skipRecovery()) {
            if (resendCnt == 0)
                msgFuts.addLast(fut);
            else
                resendCnt--;
        }
    }

    /**
     * @param rcvCnt Number of messages received by remote node.
     */
    public void ackReceived(long rcvCnt) {
        if (log.isDebugEnabled())
            log.debug("Handle acknowledgment, acked=" + acked + ", rcvCnt=" + rcvCnt +
                ", msgFuts=" + msgFuts.size() + ']');

        while (acked < rcvCnt) {
            GridNioFuture<?> fut = msgFuts.pollFirst();

            assert fut != null;

            ((GridNioFutureImpl)fut).onDone();

            acked++;
        }
    }

    /**
     * Completes with error all futures when node fails.
     * @throws InterruptedException If interrupted.
     */
    public void onNodeLeft() throws InterruptedException {
        reserve();

        try {
            for (GridNioFuture<?> msg : msgFuts)
                ((GridNioFutureImpl)msg).onDone(new IOException("Failed to send message, node has left: " + node.id()));
        }
        finally {
            release();
        }
    }

    /**
     * @return Message futures for unacknowledged messages.
     */
    public Deque<GridNioFuture<?>> messagesFutures() {
        return msgFuts;
    }

    /**
     * @throws InterruptedException If interrupted.
     */
    public void reserve() throws InterruptedException {
        synchronized (this) {
            while (reserved)
                wait();

            reserved = true;
        }
    }

    /**
     * @param rcvCnt Number of messages received by remote node.
     */
    public void onHandshake(long rcvCnt) {
        ackReceived(rcvCnt);

        resendCnt = msgFuts.size();
    }

    /**
     *
     */
    public void release() {
        synchronized (this) {
            reserved = false;

            notifyAll();
        }
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridNioRecoveryData.class, this);
    }
}
