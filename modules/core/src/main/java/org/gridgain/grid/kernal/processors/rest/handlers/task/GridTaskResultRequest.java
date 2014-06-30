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

package org.gridgain.grid.kernal.processors.rest.handlers.task;

import org.gridgain.grid.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.util.direct.*;

import java.io.*;
import java.nio.*;

/**
 * Task result request.
 */
public class GridTaskResultRequest extends GridTcpCommunicationMessageAdapter {
    /** */
    private static final long serialVersionUID = 0L;

    /** Task ID. */
    private GridUuid taskId;

    /** Topic. */
    @GridDirectTransient
    private Object topic;

    /** Serialized topic. */
    private byte[] topicBytes;

    /**
     * Public no-arg constructor for {@link Externalizable} support.
     */
    public GridTaskResultRequest() {
        // No-op.
    }

    /**
     * @param taskId Task ID.
     * @param topic Topic.
     * @param topicBytes Serialized topic.
     */
    GridTaskResultRequest(GridUuid taskId, Object topic, byte[] topicBytes) {
        this.taskId = taskId;
        this.topic = topic;
        this.topicBytes = topicBytes;
    }

    /**
     * @return Task ID.
     */
    public GridUuid taskId() {
        return taskId;
    }

    /**
     * @param taskId Task ID.
     */
    public void taskId(GridUuid taskId) {
        assert taskId != null;

        this.taskId = taskId;
    }

    /**
     * @return Topic.
     */
    public Object topic() {
        return topic;
    }

    /**
     * @return Serialized topic.
     */
    public byte[] topicBytes() {
        return topicBytes;
    }

    /**
     * @param topic Topic.
     */
    public void topic(String topic) {
        assert topic != null;

        this.topic = topic;
    }

    /** {@inheritDoc} */
    @SuppressWarnings({"CloneDoesntCallSuperClone", "CloneCallsConstructors"})
    @Override public GridTcpCommunicationMessageAdapter clone() {
        GridTaskResultRequest _clone = new GridTaskResultRequest();

        clone0(_clone);

        return _clone;
    }

    /** {@inheritDoc} */
    @Override protected void clone0(GridTcpCommunicationMessageAdapter _msg) {
        GridTaskResultRequest _clone = (GridTaskResultRequest)_msg;

        _clone.taskId = taskId;
        _clone.topic = topic;
        _clone.topicBytes = topicBytes;
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
                if (!commState.putGridUuid(taskId))
                    return false;

                commState.idx++;

            case 1:
                if (!commState.putByteArray(topicBytes))
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
                GridUuid taskId0 = commState.getGridUuid();

                if (taskId0 == GRID_UUID_NOT_READ)
                    return false;

                taskId = taskId0;

                commState.idx++;

            case 1:
                byte[] topicBytes0 = commState.getByteArray();

                if (topicBytes0 == BYTE_ARR_NOT_READ)
                    return false;

                topicBytes = topicBytes0;

                commState.idx++;

        }

        return true;
    }

    /** {@inheritDoc} */
    @Override public byte directType() {
        return 73;
    }
}
