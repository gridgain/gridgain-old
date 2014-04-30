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

package org.gridgain.grid.kernal.processors.dr.messages.external;

import org.gridgain.grid.util.typedef.internal.*;

/**
 * Request which is sent by sending receiving hub to sender hub on connection in order to
 * identify sender hub data center ID.
 */
public class GridDrExternalHandshakeRequest {
    /** Data center ID. */
    private byte dataCenterId;

    /** Protocol version. */
    private String protoVer;

    /** Marshaller class name. */
    private String marshClsName;

    /** Await acknowledge flag. */
    private boolean awaitAck;

    /**
     * Standard constructor.
     *
     * @param dataCenterId Data center ID.
     * @param protoVer Protocol version.
     * @param marshClsName Marshaller class name.
     * @param awaitAck Await acknowledge flag.
     */
    public GridDrExternalHandshakeRequest(byte dataCenterId, String protoVer, String marshClsName, boolean awaitAck) {
        this.dataCenterId = dataCenterId;
        this.protoVer = protoVer;
        this.marshClsName = marshClsName;
        this.awaitAck = awaitAck;
    }

    /**
     * Get data center ID.
     *
     * @return Data center ID.
     */
    public byte dataCenterId() {
        return dataCenterId;
    }

    /**
     * @return Protocol version.
     */
    public String protocolVersion() {
        return protoVer;
    }

    /**
     * @return Marshaller class name.
     */
    public String marshallerClassName() {
        return marshClsName;
    }

    /**
     * @return Await acknowledge flag.
     */
    public boolean awaitAcknowledge() {
        return awaitAck;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridDrExternalHandshakeRequest.class, this);
    }
}
