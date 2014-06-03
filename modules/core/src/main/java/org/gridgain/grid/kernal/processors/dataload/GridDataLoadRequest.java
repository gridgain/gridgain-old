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

package org.gridgain.grid.kernal.processors.dataload;

import org.gridgain.grid.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.util.direct.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.grid.util.tostring.*;
import org.jetbrains.annotations.*;

import java.nio.*;
import java.util.*;

/**
 *
 */
public class GridDataLoadRequest<K, V> extends GridTcpCommunicationMessageAdapter {
    /** */
    private static final long serialVersionUID = 0L;

    /** */
    private long reqId;

    /** */
    private byte[] resTopicBytes;

    /** Cache name. */
    private String cacheName;

    /** */
    private byte[] updaterBytes;

    /** Entries to put. */
    private byte[] colBytes;

    /** {@code True} to ignore deployment ownership. */
    private boolean ignoreDepOwnership;

    /** */
    private GridDeploymentMode depMode;

    /** */
    private String sampleClsName;

    /** */
    private String userVer;

    /** Node class loader participants. */
    @GridToStringInclude
    @GridDirectMap(keyType = UUID.class, valueType = GridUuid.class)
    private Map<UUID, GridUuid> ldrParticipants;

    /** */
    private GridUuid clsLdrId;

    /** */
    private boolean forceLocDep;

    /**
     * {@code Externalizable} support.
     */
    public GridDataLoadRequest() {
        // No-op.
    }

    /**
     * @param reqId Request ID.
     * @param resTopicBytes Response topic.
     * @param cacheName Cache name.
     * @param updaterBytes Cache updater.
     * @param colBytes Collection bytes.
     * @param ignoreDepOwnership Ignore ownership.
     * @param depMode Deployment mode.
     * @param sampleClsName Sample class name.
     * @param userVer User version.
     * @param ldrParticipants Loader participants.
     * @param clsLdrId Class loader ID.
     * @param forceLocDep Force local deployment.
     */
    public GridDataLoadRequest(long reqId,
        byte[] resTopicBytes,
        @Nullable String cacheName,
        byte[] updaterBytes,
        byte[] colBytes,
        boolean ignoreDepOwnership,
        GridDeploymentMode depMode,
        String sampleClsName,
        String userVer,
        Map<UUID, GridUuid> ldrParticipants,
        GridUuid clsLdrId,
        boolean forceLocDep) {
        this.reqId = reqId;
        this.resTopicBytes = resTopicBytes;
        this.cacheName = cacheName;
        this.updaterBytes = updaterBytes;
        this.colBytes = colBytes;
        this.ignoreDepOwnership = ignoreDepOwnership;
        this.depMode = depMode;
        this.sampleClsName = sampleClsName;
        this.userVer = userVer;
        this.ldrParticipants = ldrParticipants;
        this.clsLdrId = clsLdrId;
        this.forceLocDep = forceLocDep;
    }

    /**
     * @return Request ID.
     */
    public long requestId() {
        return reqId;
    }

    /**
     * @return Response topic.
     */
    public byte[] responseTopicBytes() {
        return resTopicBytes;
    }

    /**
     * @return Cache name.
     */
    public String cacheName() {
        return cacheName;
    }

    /**
     * @return Updater.
     */
    public byte[] updaterBytes() {
        return updaterBytes;
    }

    /**
     * @return Collection bytes.
     */
    public byte[] collectionBytes() {
        return colBytes;
    }

    /**
     * @return {@code True} to ignore ownership.
     */
    public boolean ignoreDeploymentOwnership() {
        return ignoreDepOwnership;
    }

    /**
     * @return Deployment mode.
     */
    public GridDeploymentMode deploymentMode() {
        return depMode;
    }

    /**
     * @return Sample class name.
     */
    public String sampleClassName() {
        return sampleClsName;
    }

    /**
     * @return User version.
     */
    public String userVersion() {
        return userVer;
    }

    /**
     * @return Participants.
     */
    public Map<UUID, GridUuid> participants() {
        return ldrParticipants;
    }

    /**
     * @return Class loader ID.
     */
    public GridUuid classLoaderId() {
        return clsLdrId;
    }

    /**
     * @return {@code True} to force local deployment.
     */
    public boolean forceLocalDeployment() {
        return forceLocDep;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridDataLoadRequest.class, this);
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
                if (!commState.putString(cacheName))
                    return false;

                commState.idx++;

            case 1:
                if (!commState.putGridUuid(clsLdrId))
                    return false;

                commState.idx++;

            case 2:
                if (!commState.putByteArray(colBytes))
                    return false;

                commState.idx++;

            case 3:
                if (!commState.putEnum(depMode))
                    return false;

                commState.idx++;

            case 4:
                if (!commState.putBoolean(forceLocDep))
                    return false;

                commState.idx++;

            case 5:
                if (!commState.putBoolean(ignoreDepOwnership))
                    return false;

                commState.idx++;

            case 6:
                if (ldrParticipants != null) {
                    if (commState.it == null) {
                        if (!commState.putInt(ldrParticipants.size()))
                            return false;

                        commState.it = ldrParticipants.entrySet().iterator();
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

            case 7:
                if (!commState.putLong(reqId))
                    return false;

                commState.idx++;

            case 8:
                if (!commState.putByteArray(resTopicBytes))
                    return false;

                commState.idx++;

            case 9:
                if (!commState.putString(sampleClsName))
                    return false;

                commState.idx++;

            case 10:
                if (!commState.putByteArray(updaterBytes))
                    return false;

                commState.idx++;

            case 11:
                if (!commState.putString(userVer))
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
                String cacheName0 = commState.getString();

                if (cacheName0 == STR_NOT_READ)
                    return false;

                cacheName = cacheName0;

                commState.idx++;

            case 1:
                GridUuid clsLdrId0 = commState.getGridUuid();

                if (clsLdrId0 == GRID_UUID_NOT_READ)
                    return false;

                clsLdrId = clsLdrId0;

                commState.idx++;

            case 2:
                byte[] colBytes0 = commState.getByteArray();

                if (colBytes0 == BYTE_ARR_NOT_READ)
                    return false;

                colBytes = colBytes0;

                commState.idx++;

            case 3:
                if (buf.remaining() < 1)
                    return false;

                byte depMode0 = commState.getByte();

                depMode = GridDeploymentMode.fromOrdinal(depMode0);

                commState.idx++;

            case 4:
                if (buf.remaining() < 1)
                    return false;

                forceLocDep = commState.getBoolean();

                commState.idx++;

            case 5:
                if (buf.remaining() < 1)
                    return false;

                ignoreDepOwnership = commState.getBoolean();

                commState.idx++;

            case 6:
                if (commState.readSize == -1) {
                    if (buf.remaining() < 4)
                        return false;

                    commState.readSize = commState.getInt();
                }

                if (commState.readSize >= 0) {
                    if (ldrParticipants == null)
                        ldrParticipants = new HashMap<>(commState.readSize);

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

                        ldrParticipants.put((UUID)commState.cur, _val);

                        commState.keyDone = false;

                        commState.readItems++;
                    }
                }

                commState.readSize = -1;
                commState.readItems = 0;
                commState.cur = null;

                commState.idx++;

            case 7:
                if (buf.remaining() < 8)
                    return false;

                reqId = commState.getLong();

                commState.idx++;

            case 8:
                byte[] resTopicBytes0 = commState.getByteArray();

                if (resTopicBytes0 == BYTE_ARR_NOT_READ)
                    return false;

                resTopicBytes = resTopicBytes0;

                commState.idx++;

            case 9:
                String sampleClsName0 = commState.getString();

                if (sampleClsName0 == STR_NOT_READ)
                    return false;

                sampleClsName = sampleClsName0;

                commState.idx++;

            case 10:
                byte[] updaterBytes0 = commState.getByteArray();

                if (updaterBytes0 == BYTE_ARR_NOT_READ)
                    return false;

                updaterBytes = updaterBytes0;

                commState.idx++;

            case 11:
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
        return 61;
    }

    /** {@inheritDoc} */
    @Override public GridTcpCommunicationMessageAdapter clone() {
        GridDataLoadRequest _clone = new GridDataLoadRequest();

        clone0(_clone);

        return _clone;
    }

    /** {@inheritDoc} */
    @Override protected void clone0(GridTcpCommunicationMessageAdapter _msg) {
        GridDataLoadRequest _clone = (GridDataLoadRequest)_msg;

        _clone.reqId = reqId;
        _clone.resTopicBytes = resTopicBytes;
        _clone.cacheName = cacheName;
        _clone.updaterBytes = updaterBytes;
        _clone.colBytes = colBytes;
        _clone.ignoreDepOwnership = ignoreDepOwnership;
        _clone.depMode = depMode;
        _clone.sampleClsName = sampleClsName;
        _clone.userVer = userVer;
        _clone.ldrParticipants = ldrParticipants;
        _clone.clsLdrId = clsLdrId;
        _clone.forceLocDep = forceLocDep;
    }
}
