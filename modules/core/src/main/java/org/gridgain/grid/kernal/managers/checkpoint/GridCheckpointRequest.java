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

package org.gridgain.grid.kernal.managers.checkpoint;

import org.gridgain.grid.*;
import org.gridgain.grid.util.direct.*;
import org.gridgain.grid.util.typedef.internal.*;

import java.io.*;
import java.nio.*;

/**
 * This class defines checkpoint request.
 */
public class GridCheckpointRequest extends GridTcpCommunicationMessageAdapter {
    /** */
    private static final long serialVersionUID = 0L;

    /** */
    private GridUuid sesId;

    /** */
    private String key;

    /** */
    private String cpSpi;

    /**
     * Empty constructor required by {@link Externalizable}.
     */
    public GridCheckpointRequest() {
        // No-op.
    }

    /**
     * @param sesId Task session ID.
     * @param key Checkpoint key.
     * @param cpSpi Checkpoint SPI.
     */
    public GridCheckpointRequest(GridUuid sesId, String key, String cpSpi) {
        assert sesId != null;
        assert key != null;

        this.sesId = sesId;
        this.key = key;

        this.cpSpi = cpSpi == null || cpSpi.isEmpty() ? null : cpSpi;
    }

    /**
     * @return Session ID.
     */
    public GridUuid getSessionId() {
        return sesId;
    }

    /**
     * @return Checkpoint key.
     */
    public String getKey() {
        return key;
    }

    /**
     * @return Checkpoint SPI.
     */
    public String getCheckpointSpi() {
        return cpSpi;
    }

    /** {@inheritDoc} */
    @SuppressWarnings({"CloneDoesntCallSuperClone", "CloneCallsConstructors"})
    @Override public GridTcpCommunicationMessageAdapter clone() {
        GridCheckpointRequest _clone = new GridCheckpointRequest();

        clone0(_clone);

        return _clone;
    }

    /** {@inheritDoc} */
    @Override protected void clone0(GridTcpCommunicationMessageAdapter _msg) {
        GridCheckpointRequest _clone = (GridCheckpointRequest)_msg;

        _clone.sesId = sesId;
        _clone.key = key;
        _clone.cpSpi = cpSpi;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("all")
    @Override public boolean writeTo(ByteBuffer buf) {
        commState.setBuffer(buf);

        if (!commState.typeWritten) {
            if (!commState.putByte(directType()))
                return false;

            commState.typeWritten = true;
        }

        switch (commState.idx) {
            case 0:
                if (!commState.putString(cpSpi))
                    return false;

                commState.idx++;

            case 1:
                if (!commState.putString(key))
                    return false;

                commState.idx++;

            case 2:
                if (!commState.putGridUuid(sesId))
                    return false;

                commState.idx++;

        }

        return true;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("all")
    @Override public boolean readFrom(ByteBuffer buf) {
        commState.setBuffer(buf);

        switch (commState.idx) {
            case 0:
                String cpSpi0 = commState.getString();

                if (cpSpi0 == STR_NOT_READ)
                    return false;

                cpSpi = cpSpi0;

                commState.idx++;

            case 1:
                String key0 = commState.getString();

                if (key0 == STR_NOT_READ)
                    return false;

                key = key0;

                commState.idx++;

            case 2:
                GridUuid sesId0 = commState.getGridUuid();

                if (sesId0 == GRID_UUID_NOT_READ)
                    return false;

                sesId = sesId0;

                commState.idx++;

        }

        return true;
    }

    /** {@inheritDoc} */
    @Override public byte directType() {
        return 7;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridCheckpointRequest.class, this);
    }
}
