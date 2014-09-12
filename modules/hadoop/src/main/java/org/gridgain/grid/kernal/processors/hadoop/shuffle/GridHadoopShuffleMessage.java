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

package org.gridgain.grid.kernal.processors.hadoop.shuffle;

import org.gridgain.grid.*;
import org.gridgain.grid.hadoop.*;
import org.gridgain.grid.kernal.processors.hadoop.message.*;
import org.gridgain.grid.util.tostring.*;
import org.gridgain.grid.util.typedef.internal.*;

import java.io.*;
import java.util.concurrent.atomic.*;

import static org.gridgain.grid.util.offheap.unsafe.GridUnsafeMemory.*;

/**
 * Shuffle message.
 */
public class GridHadoopShuffleMessage implements GridHadoopMessage {
    /** */
    private static final long serialVersionUID = 0L;

    /** */
    private static final AtomicLong ids = new AtomicLong();

    /** */
    private static final byte MARKER_KEY = (byte)17;

    /** */
    private static final byte MARKER_VALUE = (byte)31;

    /** */
    @GridToStringInclude
    private long msgId;

    /** */
    @GridToStringInclude
    private GridHadoopJobId jobId;

    /** */
    @GridToStringInclude
    private int reducer;

    /** */
    private byte[] buf;

    /** */
    @GridToStringInclude
    private int off;

    /**
     *
     */
    public GridHadoopShuffleMessage() {
        // No-op.
    }

    /**
     * @param size Size.
     */
    public GridHadoopShuffleMessage(GridHadoopJobId jobId, int reducer, int size) {
        assert jobId != null;

        buf = new byte[size];

        this.jobId = jobId;
        this.reducer = reducer;

        msgId = ids.incrementAndGet();
    }

    /**
     * @return Message ID.
     */
    public long id() {
        return msgId;
    }

    /**
     * @return Job ID.
     */
    public GridHadoopJobId jobId() {
        return jobId;
    }

    /**
     * @return Reducer.
     */
    public int reducer() {
        return reducer;
    }

    /**
     * @return Buffer.
     */
    public byte[] buffer() {
        return buf;
    }

    /**
     * @return Offset.
     */
    public int offset() {
        return off;
    }

    /**
     * @param size Size.
     * @param valOnly Only value wll be added.
     * @return {@code true} If this message can fit additional data of this size
     */
    public boolean available(int size, boolean valOnly) {
        size += valOnly ? 5 : 10;

        if (off + size > buf.length) {
            if (off == 0) { // Resize if requested size is too big.
                buf = new byte[size];

                return true;
            }

            return false;
        }

        return true;
    }

    /**
     * @param keyPtr Key pointer.
     * @param keySize Key size.
     */
    public void addKey(long keyPtr, int keySize) {
        add(MARKER_KEY, keyPtr, keySize);
    }

    /**
     * @param valPtr Value pointer.
     * @param valSize Value size.
     */
    public void addValue(long valPtr, int valSize) {
        add(MARKER_VALUE, valPtr, valSize);
    }

    /**
     * @param marker Marker.
     * @param ptr Pointer.
     * @param size Size.
     */
    private void add(byte marker, long ptr, int size) {
        buf[off++] = marker;

        UNSAFE.putInt(buf, BYTE_ARR_OFF + off, size);

        off += 4;

        UNSAFE.copyMemory(null, ptr, buf, BYTE_ARR_OFF + off, size);

        off += size;
    }

    /**
     * @param v Visitor.
     */
    public void visit(Visitor v) throws GridException {
        for (int i = 0; i < off;) {
            byte marker = buf[i++];

            int size = UNSAFE.getInt(buf, BYTE_ARR_OFF + i);

            i += 4;

            if (marker == MARKER_VALUE)
                v.onValue(buf, i, size);
            else if (marker == MARKER_KEY)
                v.onKey(buf, i, size);
            else
                throw new IllegalStateException();

            i += size;
        }
    }

    /** {@inheritDoc} */
    @Override public void writeExternal(ObjectOutput out) throws IOException {
        jobId.writeExternal(out);
        out.writeLong(msgId);
        out.writeInt(reducer);
        out.writeInt(off);
        U.writeByteArray(out, buf);
    }

    /** {@inheritDoc} */
    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        jobId = new GridHadoopJobId();

        jobId.readExternal(in);
        msgId = in.readLong();
        reducer = in.readInt();
        off = in.readInt();
        buf = U.readByteArray(in);
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridHadoopShuffleMessage.class, this);
    }

    /**
     * Visitor.
     */
    public static interface Visitor {
        /**
         * @param buf Buffer.
         * @param off Offset.
         * @param len Length.
         */
        public void onKey(byte[] buf, int off, int len) throws GridException;

        /**
         * @param buf Buffer.
         * @param off Offset.
         * @param len Length.
         */
        public void onValue(byte[] buf, int off, int len) throws GridException;
    }
}
