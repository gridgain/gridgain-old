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

package org.gridgain.grid.kernal.managers.deployment;

import org.gridgain.grid.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.util.direct.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.grid.util.tostring.*;

import java.io.*;
import java.nio.*;
import java.util.*;

/**
 * Deployment info bean.
 */
public class GridDeploymentInfoBean extends GridTcpCommunicationMessageAdapter implements GridDeploymentInfo,
    Externalizable {
    /** */
    private static final long serialVersionUID = 0L;

    /** */
    private GridUuid clsLdrId;

    /** */
    private GridDeploymentMode depMode;

    /** */
    private String userVer;

    /** */
    private boolean locDepOwner;

    /** Node class loader participant map. */
    @GridToStringInclude
    @GridDirectMap(keyType = UUID.class, valueType = GridUuid.class)
    private Map<UUID, GridUuid> participants;

    /**
     * Required by {@link Externalizable}.
     */
    public GridDeploymentInfoBean() {
        /* No-op. */
    }

    /**
     * @param clsLdrId Class loader ID.
     * @param userVer User version.
     * @param depMode Deployment mode.
     * @param participants Participants.
     * @param locDepOwner Local deployment owner flag.
     */
    public GridDeploymentInfoBean(GridUuid clsLdrId, String userVer, GridDeploymentMode depMode,
        Map<UUID, GridUuid> participants, boolean locDepOwner) {
        this.clsLdrId = clsLdrId;
        this.depMode = depMode;
        this.userVer = userVer;
        this.participants = participants;
        this.locDepOwner = locDepOwner;
    }

    /**
     * @param dep Grid deployment.
     */
    public GridDeploymentInfoBean(GridDeploymentInfo dep) {
        clsLdrId = dep.classLoaderId();
        depMode = dep.deployMode();
        userVer = dep.userVersion();
        locDepOwner = dep.localDeploymentOwner();
        participants = dep.participants();
    }

    /** {@inheritDoc} */
    @Override public GridUuid classLoaderId() {
        return clsLdrId;
    }

    /** {@inheritDoc} */
    @Override public GridDeploymentMode deployMode() {
        return depMode;
    }

    /** {@inheritDoc} */
    @Override public String userVersion() {
        return userVer;
    }

    /** {@inheritDoc} */
    @Override public long sequenceNumber() {
        return clsLdrId.localId();
    }

    /** {@inheritDoc} */
    @Override public boolean localDeploymentOwner() {
        return locDepOwner;
    }

    /** {@inheritDoc} */
    @Override public Map<UUID, GridUuid> participants() {
        return participants;
    }

    /**
     * Sets local deployment ownership flag.
     *
     * @param locDepOwner Local deployment ownership flag.
     */
    public void localDeploymentOwner(boolean locDepOwner) {
        this.locDepOwner = locDepOwner;
    }

    /** {@inheritDoc} */
    @Override public int hashCode() {
        return clsLdrId.hashCode();
    }

    /** {@inheritDoc} */
    @Override public boolean equals(Object o) {
        return o == this || o instanceof GridDeploymentInfoBean &&
            clsLdrId.equals(((GridDeploymentInfoBean)o).clsLdrId);
    }

    /** {@inheritDoc} */
    @SuppressWarnings({"CloneDoesntCallSuperClone", "CloneCallsConstructors"})
    @Override public GridTcpCommunicationMessageAdapter clone() {
        GridDeploymentInfoBean _clone = new GridDeploymentInfoBean();

        clone0(_clone);

        return _clone;
    }

    /** {@inheritDoc} */
    @Override protected void clone0(GridTcpCommunicationMessageAdapter _msg) {
        GridDeploymentInfoBean _clone = (GridDeploymentInfoBean)_msg;

        _clone.clsLdrId = clsLdrId;
        _clone.depMode = depMode;
        _clone.userVer = userVer;
        _clone.locDepOwner = locDepOwner;
        _clone.participants = participants;
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
                if (!commState.putGridUuid(clsLdrId))
                    return false;

                commState.idx++;

            case 1:
                if (!commState.putEnum(depMode))
                    return false;

                commState.idx++;

            case 2:
                if (!commState.putBoolean(locDepOwner))
                    return false;

                commState.idx++;

            case 3:
                if (participants != null) {
                    if (commState.it == null) {
                        if (!commState.putInt(participants.size()))
                            return false;

                        commState.it = participants.entrySet().iterator();
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

            case 4:
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
                GridUuid clsLdrId0 = commState.getGridUuid();

                if (clsLdrId0 == GRID_UUID_NOT_READ)
                    return false;

                clsLdrId = clsLdrId0;

                commState.idx++;

            case 1:
                if (buf.remaining() < 1)
                    return false;

                byte depMode0 = commState.getByte();

                depMode = GridDeploymentMode.fromOrdinal(depMode0);

                commState.idx++;

            case 2:
                if (buf.remaining() < 1)
                    return false;

                locDepOwner = commState.getBoolean();

                commState.idx++;

            case 3:
                if (commState.readSize == -1) {
                    if (buf.remaining() < 4)
                        return false;

                    commState.readSize = commState.getInt();
                }

                if (commState.readSize >= 0) {
                    if (participants == null)
                        participants = new HashMap<>(commState.readSize);

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

                        participants.put((UUID)commState.cur, _val);

                        commState.keyDone = false;

                        commState.readItems++;
                    }
                }

                commState.readSize = -1;
                commState.readItems = 0;
                commState.cur = null;

                commState.idx++;

            case 4:
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
        return 10;
    }

    /** {@inheritDoc} */
    @Override public void writeExternal(ObjectOutput out) throws IOException {
        U.writeGridUuid(out, clsLdrId);
        U.writeEnum(out, depMode);
        U.writeString(out, userVer);
        out.writeBoolean(locDepOwner);
        U.writeMap(out, participants);
    }

    /** {@inheritDoc} */
    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        clsLdrId = U.readGridUuid(in);
        depMode = GridDeploymentMode.fromOrdinal(in.readByte());
        userVer = U.readString(in);
        locDepOwner = in.readBoolean();
        participants = U.readMap(in);
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridDeploymentInfoBean.class, this);
    }
}
