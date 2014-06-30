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

package org.gridgain.grid.kernal.processors.dr.messages.internal;

import org.gridgain.grid.util.typedef.internal.*;

import java.io.*;

/**
 * Internal DR request entry.
 */
public class GridDrInternalRequestEntry implements Externalizable {
    /** */
    private static final long serialVersionUID = 0L;

    /** Data center ID. */
    private byte dataCenterId;

    /** Entry count. */
    private int entryCnt;

    /** Data bytes. */
    private byte[] dataBytes;

    /** Data length. */
    private int dataLen;

    /**
     * {@link Externalizable} support.
     */
    public GridDrInternalRequestEntry() {
        // No-op.
    }

    /**
     * Constructor.
     *
     * @param dataCenterId Data center ID.
     * @param entryCnt Entry count.
     * @param dataBytes Data bytes.
     * @param dataLen Data length.
     */
    public GridDrInternalRequestEntry(byte dataCenterId, int entryCnt, byte[] dataBytes, int dataLen) {
        assert dataCenterId >= 0;
        assert entryCnt > 0;
        assert dataBytes != null;
        assert dataLen > 0;

        this.dataCenterId = dataCenterId;
        this.entryCnt = entryCnt;
        this.dataBytes = dataBytes;
        this.dataLen = dataLen;
    }

    /**
     * @return Data center ID..
     */
    public byte dataCenterId() {
        return dataCenterId;
    }

    /**
     * @return Entry count.
     */
    public int entryCount() {
        return entryCnt;
    }

    /**
     * @return Data bytes.
     */
    public byte[] dataBytes() {
        return dataBytes;
    }

    /**
     * @return Data length.
     */
    public int dataLength() {
        return dataLen;
    }

    /** {@inheritDoc} */
    @Override public void writeExternal(ObjectOutput out) throws IOException {
        assert dataBytes != null;

        out.writeByte(dataCenterId);
        out.writeInt(entryCnt);

        out.writeInt(dataLen);
        out.write(dataBytes, 0, dataLen);
    }

    /** {@inheritDoc} */
    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        dataCenterId = in.readByte();
        entryCnt = in.readInt();
        dataBytes = U.readByteArray(in);
        dataLen = dataBytes.length;

        assert dataBytes != null;
    }
}
