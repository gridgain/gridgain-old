/* @java.file.header */

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.util.nio;

import org.gridgain.grid.*;

import java.util.*;

/**
 *
 */
public class GridRecoverySendData {
    /** */
    private long lastAcked;

    /** */
    private ArrayDeque<GridNioFuture<?>> msgs = new ArrayDeque<>(1024);

    /** */
    private boolean reserved;

    /** */
    private int resend;

    /**
     * @param fut NIO future.
     */
    public void add(GridNioFuture<?> fut) {
        if (resend == 0)
            msgs.addLast(fut);
        else
            resend--;
    }

    /**
     * @param last ID.
     */
    public void ackReceived(long last) {
        while (lastAcked < last) {
            GridNioFuture<?> fut = msgs.removeFirst();

            assert fut != null;

            ((GridNioFutureImpl)fut).onDone();

            lastAcked++;
        }
    }

    public void onNodeLeft() {
        for (GridNioFuture<?> fut : msgs)
            ((GridNioFutureImpl)fut).onDone(new GridException("Node left."));
    }

    /**
     * @return Messages.
     */
    public Deque<GridNioFuture<?>> messages() {
        return msgs;
    }

    public void reserve(long last) throws InterruptedException {
        synchronized (this) {
            while (reserved)
                wait();

            reserved = true;
        }

        ackReceived(last);

        resend = msgs.size();
    }

    public void release() {
        synchronized (this) {
            assert reserved;

            reserved = false;

            notifyAll();
        }
    }
}
