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
 * Recovery information for sent messages.
 */
public class GridRecoverySendData {
    /** Number of acknowledged messages. */
    private long acked;

    /** Unacknowledged message futures. */
    private ArrayDeque<GridNioFuture<?>> msgFuts = new ArrayDeque<>(1024);

    /** Reserved flag. */
    private boolean reserved;

    /** Number of messages to resend. */
    private int resendCnt;

    /** Target node. */
    private final GridNode node;

    /** Logger. */
    private final GridLogger log;

    /**
     * @param node Node.
     * @param log Logger.
     */
    public GridRecoverySendData(GridNode node, GridLogger log) {
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
        reserve(-1);

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
     * @param rcvCnt Number of messages received by remote node.
     * @throws InterruptedException If interrupted.
     */
    public void reserve(long rcvCnt) throws InterruptedException {
        synchronized (this) {
            while (reserved)
                wait();

            reserved = true;
        }

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
        return S.toString(GridRecoverySendData.class, this);
    }
}
