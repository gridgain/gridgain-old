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

package org.gridgain.loadtests.communication;

import org.gridgain.grid.*;
import org.gridgain.grid.util.direct.*;
import org.gridgain.grid.util.typedef.internal.*;

import java.io.*;
import java.nio.*;

/**
 *
 */
class GridTestMessage extends GridTcpCommunicationMessageAdapter implements Externalizable {
    /** */
    private GridUuid id;

    /** */
    private long field1;

    /** */
    private long field2;

    /** */
    private String str;

    /** */
    private byte[] bytes;

    /**
     * @param id Message ID.
     * @param str String.
     */
    GridTestMessage(GridUuid id, String str) {
        this.id = id;
        this.str = str;
    }

    /**
     * @param id Message ID.
     * @param bytes Bytes.
     */
    GridTestMessage(GridUuid id, byte[] bytes) {
        this.id = id;
        this.bytes = bytes;
    }

    /**
     * For Externalizable support.
     */
    public GridTestMessage() {
        // No-op.
    }

    /**
     * @return Message ID.
     */
    public GridUuid id() {
        return id;
    }

    /**
     * @return Bytes.
     */
    public byte[] bytes() {
        return bytes;
    }

    /**
     * @param bytes Bytes.
     */
    public void bytes(byte[] bytes) {
        this.bytes = bytes;
    }

    /** {@inheritDoc} */
    @Override public void writeExternal(ObjectOutput out) throws IOException {
        U.writeGridUuid(out, id);
        out.writeLong(field1);
        out.writeLong(field2);
        U.writeString(out, str);
        U.writeByteArray(out, bytes);
    }

    /** {@inheritDoc} */
    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        id = U.readGridUuid(in);
        field1 = in.readLong();
        field2 = in.readLong();
        str = U.readString(in);
        bytes = U.readByteArray(in);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("CloneDoesntCallSuperClone")
    @Override public GridTcpCommunicationMessageAdapter clone() {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override protected void clone0(GridTcpCommunicationMessageAdapter _msg) {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override public boolean writeTo(ByteBuffer buf) {
        return true;
    }

    /** {@inheritDoc} */
    @Override public boolean readFrom(ByteBuffer buf) {
        return true;
    }

    /** {@inheritDoc} */
    @Override public byte directType() {
        return 0;
    }
}
