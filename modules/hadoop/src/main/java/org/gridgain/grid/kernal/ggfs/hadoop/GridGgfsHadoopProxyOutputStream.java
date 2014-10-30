/* 
 Copyright (C) GridGain Systems. All Rights Reserved.
 
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0
 
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.kernal.ggfs.hadoop;

import org.apache.hadoop.fs.*;
import org.gridgain.grid.kernal.ggfs.common.*;

import java.io.*;

/**
 * Secondary Hadoop file system output stream wrapper.
 */
public class GridGgfsHadoopProxyOutputStream extends OutputStream {
    /** Actual output stream. */
    private FSDataOutputStream os;

    /** Client logger. */
    private final GridGgfsLogger clientLog;

    /** Log stream ID. */
    private final long logStreamId;

    /** Read time. */
    private long writeTime;

    /** User time. */
    private long userTime;

    /** Last timestamp. */
    private long lastTs;

    /** Amount of written bytes. */
    private long total;

    /** Closed flag. */
    private boolean closed;

    /**
     * Constructor.
     *
     * @param os Actual output stream.
     * @param clientLog Client logger.
     * @param logStreamId Log stream ID.
     */
    public GridGgfsHadoopProxyOutputStream(FSDataOutputStream os, GridGgfsLogger clientLog, long logStreamId) {
        assert os != null;
        assert clientLog != null;

        this.os = os;
        this.clientLog = clientLog;
        this.logStreamId = logStreamId;

        lastTs = System.nanoTime();
    }

    /** {@inheritDoc} */
    @Override public synchronized void write(int b) throws IOException {
        writeStart();

        try {
            os.write(b);
        }
        finally {
            writeEnd();
        }

        total++;
    }

    /** {@inheritDoc} */
    @Override public synchronized void write(byte[] b) throws IOException {
        writeStart();

        try {
            os.write(b);
        }
        finally {
            writeEnd();
        }

        total += b.length;
    }

    /** {@inheritDoc} */
    @Override public synchronized void write(byte[] b, int off, int len) throws IOException {
        writeStart();

        try {
            os.write(b, off, len);
        }
        finally {
            writeEnd();
        }

        total += len;
    }

    /** {@inheritDoc} */
    @Override public synchronized void flush() throws IOException {
        writeStart();

        try {
            os.flush();
        }
        finally {
            writeEnd();
        }
    }

    /** {@inheritDoc} */
    @Override public synchronized void close() throws IOException {
        if (!closed) {
            closed = true;

            writeStart();

            try {
                os.close();
            }
            finally {
                writeEnd();
            }

            if (clientLog.isLogEnabled())
                clientLog.logCloseOut(logStreamId, userTime, writeTime, total);
        }
    }

    /**
     * Read start.
     */
    private void writeStart() {
        long now = System.nanoTime();

        userTime += now - lastTs;

        lastTs = now;
    }

    /**
     * Read end.
     */
    private void writeEnd() {
        long now = System.nanoTime();

        writeTime += now - lastTs;

        lastTs = now;
    }
}
