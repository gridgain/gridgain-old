/* @java.file.header */

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.util.nio;

import org.gridgain.grid.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.util.typedef.internal.*;

import java.io.*;
import java.util.*;

/**
 * Recovery information for single node.
 */
public class GridNioRecoveryDescriptor {
    /** Number of acknowledged messages. */
    private long acked;

    /** Unacknowledged message futures. */
    private final ArrayDeque<GridNioFuture<?>> msgFuts;

    /** Number of messages to resend. */
    private int resendCnt;

    /** Number of received messages. */
    private long rcvCnt;

    /** Reserved flag. */
    private boolean reserved;

    /** Last acknowledged message. */
    private volatile long lastAck;

    /** */
    private boolean nodeLeft;

    /** Target node. */
    private final GridNode node;

    /** Logger. */
    private final GridLogger log;

    /** */
    private GridBiTuple<Long, GridInClosure<Boolean>> handshakeReq;

    /** */
    private boolean connected;

    /** Number of outgoing connect attempts. */
    private long connectCnt;

    /**
     * @param queueSize Expected message queue size.
     * @param node Node.
     * @param log Logger.
     */
    public GridNioRecoveryDescriptor(int queueSize, GridNode node, GridLogger log) {
        assert !node.isLocal() : node;
        assert queueSize > 0 : queueSize;

        msgFuts = new ArrayDeque<>(queueSize);

        this.node = node;
        this.log = log;
    }

    /**
     * @return Connect count.
     */
    public long incrementConnectCount() {
        return connectCnt++;
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
     * @param lastAck Last acknowledged message.
     */
    public void lastAcknowledged(long lastAck) {
        this.lastAck = lastAck;
    }

    /**
     * @return Last acknowledged message.
     */
    public long lastAcknowledged() {
        return lastAck;
    }

    /**
     * @return Received messages count.
     */
    public long receivedCount() {
        return rcvCnt;
    }

    /**
     * @param fut NIO future.
     */
    public void add(GridNioFuture<?> fut) {
        assert fut != null;

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
     * Node left callback.
     */
    public void onNodeLeft() {
        synchronized (this) {
            if (reserved)
                nodeLeft = true;
            else
                completeOnNodeLeft();
        }
    }

    /**
     * @return Message futures for unacknowledged messages.
     */
    public Deque<GridNioFuture<?>> messagesFutures() {
        return msgFuts;
    }

    /**
     * @param ctx SPI context.
     * @return {@code True} if node still exists with the same order.
     */
    public boolean nodeAlive(GridSpiContext ctx) {
        GridNode node0 = ctx.node(node.id());

        return node0 != null && node0.order() == node.order();
    }

    /**
     * @throws InterruptedException If interrupted.
     * @return {@code True} if reserved.
     */
    public boolean reserve() throws InterruptedException {
        synchronized (this) {
            while (!connected && reserved)
                wait();

            if (!connected)
                reserved = true;

            return !connected;
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
    public void connected() {
        synchronized (this) {
            assert reserved;
            assert !connected;

            connected = true;

            if (handshakeReq != null) {
                GridInClosure<Boolean> c = handshakeReq.get2();

                assert c != null;

                c.apply(false);

                handshakeReq = null;
            }

            notifyAll();
        }
    }

    /**
     *
     */
    public void release() {
        synchronized (this) {
            connected = false;

            if (handshakeReq != null) {
                GridInClosure<Boolean> c = handshakeReq.get2();

                assert c != null;

                handshakeReq = null;

                c.apply(true);
            }
            else {
                reserved = false;

                notifyAll();
            }

            if (nodeLeft)
                completeOnNodeLeft();
        }
    }

    /**
     * @param id Handshake ID.
     * @param c Closure to run on reserve.
     * @return {@code True} if reserved.
     */
    public boolean tryReserve(long id, GridInClosure<Boolean> c) {
        synchronized (this) {
            if (connected) {
                c.apply(false);

                return false;
            }

            if (reserved) {
                if (handshakeReq != null) {
                    assert handshakeReq.get1() != null;

                    long id0 = handshakeReq.get1();

                    assert id0 != id : id0;

                    if (id > id0) {
                        GridInClosure<Boolean> c0 = handshakeReq.get2();

                        assert c0 != null;

                        c0.apply(false);

                        handshakeReq = new GridBiTuple<>(id, c);
                    }
                    else
                        c.apply(false);
                }
                else
                    handshakeReq = new GridBiTuple<>(id, c);

                return false;
            }
            else {
                reserved = true;

                return true;
            }
        }
    }

    /**
     *
     */
    private void completeOnNodeLeft() {
        for (GridNioFuture<?> msg : msgFuts)
            ((GridNioFutureImpl)msg).onDone(new IOException("Failed to send message, node has left: " + node.id()));
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridNioRecoveryDescriptor.class, this);
    }
}
