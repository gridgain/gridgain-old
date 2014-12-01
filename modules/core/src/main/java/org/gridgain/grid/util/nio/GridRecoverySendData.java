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

    /** */
    private final GridLogger log;

    /**
     * @param log Logger.
     */
    public GridRecoverySendData(GridLogger log) {
        this.log = log;
    }

    /**
     * @param fut NIO future.
     */
    public void add(GridNioFuture<?> fut) {
        if (!fut.skipRecovery()) {
            if (resend == 0)
                msgs.addLast(fut);
            else {
                //log.info("Add skip: " + resend + " " + fut);

                resend--;
            }
        }
    }

    /**
     * @param last ID.
     */
    public void ackReceived(long last) {
        //log.info("Handle ack, cur=" + lastAcked + ", rcvd=" + last + ", msgs=" + msgs.size());

        while (lastAcked < last) {
            GridNioFuture<?> fut = msgs.pollFirst();

            assert fut != null;

            ((GridNioFutureImpl)fut).onDone();

            lastAcked++;
        }

        //log.info("After Handle ack: " + msgs.size());

        /*
        for (GridNioFuture fut : msgs)
            log.info("Msg: " + fut);
            */
    }

    /**
     * @return Messages.
     */
    public Deque<GridNioFuture<?>> messages() {
        return msgs;
    }

    /**
     * @param last
     * @throws InterruptedException If interrupted.
     */
    public void reserve(long last) throws InterruptedException {
        //log.info("Reserve send data: " + last);

        synchronized (this) {
            while (reserved)
                wait();

            reserved = true;
        }

        ackReceived(last);

        resend = msgs.size();
    }

    /**
     *
     */
    public void release() {
        synchronized (this) {
            assert reserved;

            reserved = false;

            notifyAll();
        }
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridRecoverySendData.class, this);
    }
}
