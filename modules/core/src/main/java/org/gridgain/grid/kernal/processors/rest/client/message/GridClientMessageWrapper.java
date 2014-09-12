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
import java.util.*;

/**
 * Client message wrapper for direct marshalling.
 */
public class GridClientMessageWrapper extends GridTcpCommunicationMessageAdapter {
    /** */
    private static final long serialVersionUID = 5284375300887454697L;

    /** Client request header. */
    public static final byte REQ_HEADER = (byte)0x90;

    /** */
    private int msgSize;

    /** */
    private long reqId;

    /** */
    private UUID clientId;

    /** */
    private UUID destId;

    /** */
    private ByteBuffer msg;

    /**
     * @return Request ID.
     */
    public long requestId() {
        return reqId;
    }

    /**
     * @param reqId Request ID.
     */
    public void requestId(long reqId) {
        this.reqId = reqId;
    }

    /**
     * @return Message size.
     */
    public int messageSize() {
        return msgSize;
    }

    /**
     * @param msgSize Message size.
     */
    public void messageSize(int msgSize) {
        this.msgSize = msgSize;
    }

    /**
     * @return Client ID.
     */
    public UUID clientId() {
        return clientId;
    }

    /**
     * @param clientId Client ID.
     */
    public void clientId(UUID clientId) {
        this.clientId = clientId;
    }

    /**
     * @return Destination ID.
     */
    public UUID destinationId() {
        return destId;
    }

    /**
     * @param destId Destination ID.
     */
    public void destinationId(UUID destId) {
        this.destId = destId;
    }

    /**
     * @return Message buffer.
     */
    public ByteBuffer message() {
        return msg;
    }

    /**
     * @return Message bytes.
     */
    public byte[] messageArray() {
        assert msg.hasArray();
        assert msg.position() == 0 && msg.remaining() == msg.capacity();

        return msg.array();
    }

    /**
     * @param msg Message bytes.
     */
    public void message(ByteBuffer msg) {
        this.msg = msg;
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
                if (!commState.putIntClient(msgSize))
                    return false;

                commState.idx++;

            case 1:
                if (!commState.putLongClient(reqId))
                    return false;

                commState.idx++;

            case 2:
                if (!commState.putUuidClient(clientId))
                    return false;

                commState.idx++;

            case 3:
                if (!commState.putUuidClient(destId))
                    return false;

                commState.idx++;

            case 4:
                if (!commState.putByteBufferClient(msg))
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
                if (buf.remaining() < 4)
                    return false;

                msgSize = commState.getIntClient();

                if (msgSize == 0) // Ping message.
                    return true;

                commState.idx++;

            case 1:
                if (buf.remaining() < 8)
                    return false;

                reqId = commState.getLongClient();

                commState.idx++;

            case 2:
                UUID clientId0 = commState.getUuidClient();

                if (clientId0 == UUID_NOT_READ)
                    return false;

                clientId = clientId0;

                commState.idx++;

            case 3:
                UUID destId0 = commState.getUuidClient();

                if (destId0 == UUID_NOT_READ)
                    return false;

                destId = destId0;

                commState.idx++;

            case 4:
                byte[] msg0 = commState.getByteArrayClient(msgSize - 40);

                if (msg0 == BYTE_ARR_NOT_READ)
                    return false;

                msg = ByteBuffer.wrap(msg0);

                commState.idx++;
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override public byte directType() {
        return REQ_HEADER;
    }

    /** {@inheritDoc} */
    @SuppressWarnings({"CloneDoesntCallSuperClone", "CloneCallsConstructors"})
    @Override public GridTcpCommunicationMessageAdapter clone() {
        GridClientMessageWrapper _clone = new GridClientMessageWrapper();

        clone0(_clone);

        return _clone;
    }

    /** {@inheritDoc} */
    @Override protected void clone0(GridTcpCommunicationMessageAdapter _msg) {
        GridClientMessageWrapper _clone = (GridClientMessageWrapper)_msg;

        _clone.reqId = reqId;
        _clone.msgSize = msgSize;
        _clone.clientId = clientId;
        _clone.destId = destId;
        _clone.msg = msg;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridClientMessageWrapper.class, this);
    }
}
