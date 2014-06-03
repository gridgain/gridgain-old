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

package org.gridgain.grid.kernal.processors.streamer;

import org.gridgain.grid.*;
import org.gridgain.grid.util.direct.*;

import java.io.*;
import java.nio.*;

/**
 * Streamer cancel request.
 */
public class GridStreamerCancelRequest extends GridTcpCommunicationMessageAdapter {
    /** */
    private static final long serialVersionUID = 0L;

    /** Cancelled future ID. */
    private GridUuid cancelledFutId;

    /**
     * Empty constructor required by {@link Externalizable}.
     */
    public GridStreamerCancelRequest() {
        // No-op.
    }

    /**
     * @param cancelledFutId Cancelled future ID.
     */
    public GridStreamerCancelRequest(GridUuid cancelledFutId) {
        this.cancelledFutId = cancelledFutId;
    }

    /**
     * @return Cancelled future ID.
     */
    public GridUuid cancelledFutureId() {
        return cancelledFutId;
    }

    /** {@inheritDoc} */
    @SuppressWarnings({"CloneDoesntCallSuperClone", "CloneCallsConstructors"})
    @Override public GridTcpCommunicationMessageAdapter clone() {
        GridStreamerCancelRequest _clone = new GridStreamerCancelRequest();

        clone0(_clone);

        return _clone;
    }

    /** {@inheritDoc} */
    @Override protected void clone0(GridTcpCommunicationMessageAdapter _msg) {
        GridStreamerCancelRequest _clone = (GridStreamerCancelRequest)_msg;

        _clone.cancelledFutId = cancelledFutId;
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
                if (!commState.putGridUuid(cancelledFutId))
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
                GridUuid cancelledFutId0 = commState.getGridUuid();

                if (cancelledFutId0 == GRID_UUID_NOT_READ)
                    return false;

                cancelledFutId = cancelledFutId0;

                commState.idx++;

        }

        return true;
    }

    /** {@inheritDoc} */
    @Override public byte directType() {
        return 75;
    }
}
