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
import org.gridgain.grid.util.nio.*;

import java.util.*;

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

    public GridRecoveryReceiveData(GridLogger log) {
        this.log = log;
    }

    /**
     *
     */
    public long messageReceived() {
        rcvCntr++;

        log.info("Recovery received: " + rcvCntr);

        return rcvCntr;
    }

    public long lastReceived() {
        return rcvCntr;
    }

    public void reserve() throws InterruptedException {
        synchronized (this) {
            while (reserved)
                wait();

            reserved = true;
        }
    }

    public void await() throws InterruptedException {
        synchronized (this) {
            while (reserved)
                wait();
        }
    }

    public void release() {
        synchronized (this) {
            assert reserved;

            reserved = false;

            notifyAll();
        }
    }
}
