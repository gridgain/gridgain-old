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

package org.gridgain.grid.kernal.processors.rest.client.message;

import org.gridgain.grid.util.direct.*;
import org.gridgain.grid.util.typedef.internal.*;

import java.nio.*;

/**
 * Client handshake wrapper for direct marshalling.
 */
public class GridClientHandshakeRequestWrapper extends GridTcpCommunicationMessageAdapter {
    /** */
    private static final long serialVersionUID = -5705048094821942662L;

    /** Signal char. */
    public static final byte HANDSHAKE_HEADER = (byte)0x91;

    /** Handshake bytes. */
    private byte[] bytes;

    /**
     *
     */
    public GridClientHandshakeRequestWrapper() {
        // No-op.
    }

    /**
     *
     * @param req Handshake request.
     */
    public GridClientHandshakeRequestWrapper(GridClientHandshakeRequest req) {
        bytes = req.rawBytes();
    }

    /**
     * @return Handshake bytes.
     */
    public byte[] bytes() {
        return bytes;
    }

    /** {@inheritDoc} */
    @Override public boolean writeTo(ByteBuffer buf) {
        commState.setBuffer(buf);

        if (!commState.typeWritten) {
            if (!commState.putByte(directType()))
                return false;

            commState.typeWritten = true;
        }

        switch (commState.idx) {
            case 0:
                if (!commState.putByteArrayClient(bytes))
                    return false;

                commState.idx++;

        }

        return true;
    }

    /** {@inheritDoc} */
    @Override public boolean readFrom(ByteBuffer buf) {
        commState.setBuffer(buf);

        switch (commState.idx) {
            case 0:
                byte[] bytes0 = commState.getByteArrayClient(GridClientHandshakeRequest.PACKET_SIZE);

                if (bytes0 == BYTE_ARR_NOT_READ)
                    return false;

                bytes = bytes0;

                commState.idx++;

        }

        return true;
    }

    /** {@inheritDoc} */
    @Override public byte directType() {
        return HANDSHAKE_HEADER;
    }

    /** {@inheritDoc} */
    @SuppressWarnings({"CloneDoesntCallSuperClone", "CloneCallsConstructors"})
    @Override public GridTcpCommunicationMessageAdapter clone() {
        GridClientHandshakeRequestWrapper _clone = new GridClientHandshakeRequestWrapper();

        clone0(_clone);

        return _clone;
    }

    /** {@inheritDoc} */
    @Override protected void clone0(GridTcpCommunicationMessageAdapter _msg) {
        GridClientHandshakeRequestWrapper _clone = (GridClientHandshakeRequestWrapper)_msg;

        _clone.bytes = bytes;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridClientHandshakeRequestWrapper.class, this);
    }
}
