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

package org.gridgain.grid.kernal.managers.communication;

import org.gridgain.grid.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.kernal.managers.deployment.*;
import org.gridgain.grid.util.direct.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.grid.util.tostring.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.nio.*;
import java.util.*;

/**
 * User message wrapper.
 */
public class GridIoUserMessage extends GridTcpCommunicationMessageAdapter {
    /** */
    private static final long serialVersionUID = 0L;

    /** Message body. */
    @GridDirectTransient
    private Object body;

    /** Serialized message body. */
    private byte[] bodyBytes;

    /** Class loader ID. */
    private GridUuid clsLdrId;

    /** Message topic. */
    @GridDirectTransient
    private Object topic;

    /** Serialized message topic. */
    private byte[] topicBytes;

    /** Deployment mode. */
    private GridDeploymentMode depMode;

    /** Deployment class name. */
    private String depClsName;

    /** User version. */
    private String userVer;

    /** Node class loader participants. */
    @GridToStringInclude
    @GridDirectMap(keyType = UUID.class, valueType = GridUuid.class)
    private Map<UUID, GridUuid> ldrParties;

    /** Message deployment. */
    @GridDirectTransient
    private GridDeployment dep;

    /**
     * @param body Message body.
     * @param bodyBytes Serialized message body.
     * @param depClsName Message body class name.
     * @param topic Message topic.
     * @param topicBytes Serialized message topic bytes.
     * @param clsLdrId Class loader ID.
     * @param depMode Deployment mode.
     * @param userVer User version.
     * @param ldrParties Node loader participant map.
     */
    GridIoUserMessage(
        Object body,
        @Nullable byte[] bodyBytes,
        @Nullable String depClsName,
        @Nullable Object topic,
        @Nullable byte[] topicBytes,
        @Nullable GridUuid clsLdrId,
        @Nullable GridDeploymentMode depMode,
        @Nullable String userVer,
        @Nullable Map<UUID, GridUuid> ldrParties) {
        this.body = body;
        this.bodyBytes = bodyBytes;
        this.depClsName = depClsName;
        this.topic = topic;
        this.topicBytes = topicBytes;
        this.depMode = depMode;
        this.clsLdrId = clsLdrId;
        this.userVer = userVer;
        this.ldrParties = ldrParties;
    }

    /**
     * Default constructor, required for {@link Externalizable}.
     */
    public GridIoUserMessage() {
        // No-op.
    }

    /**
     * @return Serialized message body.
     */
    @Nullable public byte[] bodyBytes() {
        return bodyBytes;
    }

    /**
     * @return the Class loader ID.
     */
    @Nullable public GridUuid classLoaderId() {
        return clsLdrId;
    }

    /**
     * @return Deployment mode.
     */
    @Nullable public GridDeploymentMode deploymentMode() {
        return depMode;
    }

    /**
     * @return Message body class name.
     */
    @Nullable public String deploymentClassName() {
        return depClsName;
    }

    /**
     * @return User version.
     */
    @Nullable public String userVersion() {
        return userVer;
    }

    /**
     * @return Node class loader participant map.
     */
    @Nullable public Map<UUID, GridUuid> loaderParticipants() {
        return ldrParties != null ? Collections.unmodifiableMap(ldrParties) : null;
    }

    /**
     * @return Serialized message topic.
     */
    @Nullable public byte[] topicBytes() {
        return topicBytes;
    }

    /**
     * @param topic New message topic.
     */
    public void topic(Object topic) {
        this.topic = topic;
    }

    /**
     * @return Message topic.
     */
    @Nullable public Object topic() {
        return topic;
    }

    /**
     * @param body New message body.
     */
    public void body(Object body) {
        this.body = body;
    }

    /**
     * @return Message body.
     */
    @Nullable public Object body() {
        return body;
    }

    /**
     * @param dep New message deployment.
     */
    public void deployment(GridDeployment dep) {
        this.dep = dep;
    }

    /**
     * @return Message deployment.
     */
    @Nullable public GridDeployment deployment() {
        return dep;
    }

    /** {@inheritDoc} */
    @SuppressWarnings({"CloneDoesntCallSuperClone", "CloneCallsConstructors"})
    @Override public GridTcpCommunicationMessageAdapter clone() {
        GridIoUserMessage _clone = new GridIoUserMessage();

        clone0(_clone);

        return _clone;
    }

    /** {@inheritDoc} */
    @Override protected void clone0(GridTcpCommunicationMessageAdapter _msg) {
        GridIoUserMessage _clone = (GridIoUserMessage)_msg;

        _clone.body = body;
        _clone.bodyBytes = bodyBytes;
        _clone.clsLdrId = clsLdrId;
        _clone.topic = topic;
        _clone.topicBytes = topicBytes;
        _clone.depMode = depMode;
        _clone.depClsName = depClsName;
        _clone.userVer = userVer;
        _clone.ldrParties = ldrParties;
        _clone.dep = dep;
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
                if (!commState.putByteArray(bodyBytes))
                    return false;

                commState.idx++;

            case 1:
                if (!commState.putGridUuid(clsLdrId))
                    return false;

                commState.idx++;

            case 2:
                if (!commState.putString(depClsName))
                    return false;

                commState.idx++;

            case 3:
                if (!commState.putEnum(depMode))
                    return false;

                commState.idx++;

            case 4:
                if (ldrParties != null) {
                    if (commState.it == null) {
                        if (!commState.putInt(ldrParties.size()))
                            return false;

                        commState.it = ldrParties.entrySet().iterator();
                    }

                    while (commState.it.hasNext() || commState.cur != NULL) {
                        if (commState.cur == NULL)
                            commState.cur = commState.it.next();

                        Map.Entry<UUID, GridUuid> e = (Map.Entry<UUID, GridUuid>)commState.cur;

                        if (!commState.keyDone) {
                            if (!commState.putUuid(e.getKey()))
                                return false;

                            commState.keyDone = true;
                        }

                        if (!commState.putGridUuid(e.getValue()))
                            return false;

                        commState.keyDone = false;

                        commState.cur = NULL;
                    }

                    commState.it = null;
                } else {
                    if (!commState.putInt(-1))
                        return false;
                }

                commState.idx++;

            case 5:
                if (!commState.putByteArray(topicBytes))
                    return false;

                commState.idx++;

            case 6:
                if (!commState.putString(userVer))
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
                byte[] bodyBytes0 = commState.getByteArray();

                if (bodyBytes0 == BYTE_ARR_NOT_READ)
                    return false;

                bodyBytes = bodyBytes0;

                commState.idx++;

            case 1:
                GridUuid clsLdrId0 = commState.getGridUuid();

                if (clsLdrId0 == GRID_UUID_NOT_READ)
                    return false;

                clsLdrId = clsLdrId0;

                commState.idx++;

            case 2:
                String depClsName0 = commState.getString();

                if (depClsName0 == STR_NOT_READ)
                    return false;

                depClsName = depClsName0;

                commState.idx++;

            case 3:
                if (buf.remaining() < 1)
                    return false;

                byte depMode0 = commState.getByte();

                depMode = GridDeploymentMode.fromOrdinal(depMode0);

                commState.idx++;

            case 4:
                if (commState.readSize == -1) {
                    if (buf.remaining() < 4)
                        return false;

                    commState.readSize = commState.getInt();
                }

                if (commState.readSize >= 0) {
                    if (ldrParties == null)
                        ldrParties = new HashMap<>(commState.readSize);

                    for (int i = commState.readItems; i < commState.readSize; i++) {
                        if (!commState.keyDone) {
                            UUID _val = commState.getUuid();

                            if (_val == UUID_NOT_READ)
                                return false;

                            commState.cur = _val;
                            commState.keyDone = true;
                        }

                        GridUuid _val = commState.getGridUuid();

                        if (_val == GRID_UUID_NOT_READ)
                            return false;

                        ldrParties.put((UUID)commState.cur, _val);

                        commState.keyDone = false;

                        commState.readItems++;
                    }
                }

                commState.readSize = -1;
                commState.readItems = 0;
                commState.cur = null;

                commState.idx++;

            case 5:
                byte[] topicBytes0 = commState.getByteArray();

                if (topicBytes0 == BYTE_ARR_NOT_READ)
                    return false;

                topicBytes = topicBytes0;

                commState.idx++;

            case 6:
                String userVer0 = commState.getString();

                if (userVer0 == STR_NOT_READ)
                    return false;

                userVer = userVer0;

                commState.idx++;

        }

        return true;
    }

    /** {@inheritDoc} */
    @Override public byte directType() {
        return 9;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridIoUserMessage.class, this);
    }
}
