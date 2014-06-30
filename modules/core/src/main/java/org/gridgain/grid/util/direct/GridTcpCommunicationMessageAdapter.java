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

package org.gridgain.grid.util.direct;

import org.gridgain.grid.*;
import org.gridgain.grid.kernal.processors.cache.*;
import org.gridgain.grid.kernal.processors.cache.distributed.dht.preloader.*;
import org.gridgain.grid.kernal.processors.clock.*;
import org.gridgain.grid.kernal.processors.dr.messages.internal.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.nio.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.nio.*;
import java.util.*;

import static org.gridgain.grid.events.GridEventType.*;

/**
 * Communication message adapter.
 */
public abstract class GridTcpCommunicationMessageAdapter implements Serializable, Cloneable {
    /** */
    private static final long serialVersionUID = 0L;

    /** */
    public static final byte[] BYTE_ARR_NOT_READ = new byte[0];

    /** */
    public static final short[] SHORT_ARR_NOT_READ = new short[0];

    /** */
    public static final int[] INT_ARR_NOT_READ = new int[0];

    /** */
    public static final long[] LONG_ARR_NOT_READ = new long[0];

    /** */
    public static final float[] FLOAT_ARR_NOT_READ = new float[0];

    /** */
    public static final double[] DOUBLE_ARR_NOT_READ = new double[0];

    /** */
    public static final char[] CHAR_ARR_NOT_READ = new char[0];

    /** */
    public static final boolean[] BOOLEAN_ARR_NOT_READ = new boolean[0];

    /** */
    public static final UUID UUID_NOT_READ = new UUID(0, 0);

    /** */
    public static final GridUuid GRID_UUID_NOT_READ = new GridUuid(new UUID(0, 0), 0);

    /** */
    public static final GridClockDeltaVersion CLOCK_DELTA_VER_NOT_READ = new GridClockDeltaVersion(0, 0);

    /** */
    public static final GridByteArrayList BYTE_ARR_LIST_NOT_READ = new GridByteArrayList(new byte[0]);

    /** */
    public static final GridLongList LONG_LIST_NOT_READ = new GridLongList(0);

    /** */
    public static final GridCacheVersion CACHE_VER_NOT_READ = new GridCacheVersion(0, 0, 0, 0);

    /** */
    public static final GridDhtPartitionExchangeId DHT_PART_EXCHANGE_ID_NOT_READ =
        new GridDhtPartitionExchangeId(new UUID(0, 0), EVT_NODE_LEFT, 1);

    /** */
    public static final GridCacheValueBytes VAL_BYTES_NOT_READ = new GridCacheValueBytes();

    /** */
    public static final GridDrInternalRequestEntry DR_INT_REQ_ENTRY_NOT_READ = new GridDrInternalRequestEntry();

    /** */
    @SuppressWarnings("RedundantStringConstructorCall")
    public static final String STR_NOT_READ = new String();

    /** */
    public static final BitSet BIT_SET_NOT_READ = new BitSet();

    /** */
    public static final Enum<?> ENUM_NOT_READ = DummyEnum.DUMMY;

    /** */
    public static final GridTcpCommunicationMessageAdapter MSG_NOT_READ = new GridTcpCommunicationMessageAdapter() {
        @SuppressWarnings("CloneDoesntCallSuperClone")
        @Override public GridTcpCommunicationMessageAdapter clone() {
            throw new UnsupportedOperationException();
        }

        @Override protected void clone0(GridTcpCommunicationMessageAdapter _msg) {
            throw new UnsupportedOperationException();
        }

        @Override public boolean writeTo(ByteBuffer buf) {
            throw new UnsupportedOperationException();
        }

        @Override public boolean readFrom(ByteBuffer buf) {
            throw new UnsupportedOperationException();
        }

        @Override public byte directType() {
            throw new UnsupportedOperationException();
        }
    };

    /** */
    protected static final Object NULL = new Object();

    /** */
    protected final GridTcpCommunicationMessageState commState = new GridTcpCommunicationMessageState();

    /**
     * @param msgWriter Message writer.
     * @param nodeId Node ID (provided only if versions are different).
     */
    public void messageWriter(GridNioMessageWriter msgWriter, @Nullable UUID nodeId) {
        assert msgWriter != null;

        commState.messageWriter(msgWriter, nodeId);
    }

    /**
     * @param msgReader Message reader.
     * @param nodeId Node ID (provided only if versions are different).
     */
    public void messageReader(GridNioMessageReader msgReader, @Nullable UUID nodeId) {
        assert msgReader != null;

        commState.messageReader(msgReader, nodeId);
    }

    /**
     * @param buf Byte buffer.
     * @return Whether message was fully written.
     */
    public abstract boolean writeTo(ByteBuffer buf);

    /**
     * @param buf Byte buffer.
     * @return Whether message was fully read.
     */
    public abstract boolean readFrom(ByteBuffer buf);

    /**
     * @return Message type.
     */
    public abstract byte directType();

    /** {@inheritDoc} */
    @SuppressWarnings("CloneDoesntDeclareCloneNotSupportedException")
    @Override public abstract GridTcpCommunicationMessageAdapter clone();

    /**
     * Clones all fields of the provided message to {@code this}.
     *
     * @param _msg Message to clone from.
     */
    protected abstract void clone0(GridTcpCommunicationMessageAdapter _msg);

    /**
     * @param arr Array.
     * @return Array iterator.
     */
    protected final Iterator<?> arrayIterator(final Object[] arr) {
        return new Iterator<Object>() {
            private int idx;

            @Override public boolean hasNext() {
                return idx < arr.length;
            }

            @Override public Object next() {
                if (!hasNext())
                    throw new NoSuchElementException();

                return arr[idx++];
            }

            @Override public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * Dummy enum.
     */
    private enum DummyEnum {
        /** */
        DUMMY
    }
}
