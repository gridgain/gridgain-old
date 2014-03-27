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

import java.io.*;

/**
 * A client handshake response, containing result
 * code.
 */
public class GridClientHandshakeResponse extends GridClientAbstractMessage {
    /** Response, indicating successful handshake. */
    public static final GridClientHandshakeResponse OK = new GridClientHandshakeResponse((byte)0);

    /** Response, indicating that client version check has failed. */
    public static final GridClientHandshakeResponse ERR_VERSION_CHECK_FAILED = new GridClientHandshakeResponse((byte)1);

    /** Response, indicating that protocol ID, specified by the client, is invalid. */
    public static final GridClientHandshakeResponse ERR_UNKNOWN_PROTO_ID = new GridClientHandshakeResponse((byte)2);

    /** */
    private byte resCode;

    /**
     * Constructor for {@link Externalizable}.
     */
    public GridClientHandshakeResponse() {
        // No-op.
    }

    /**
     * Constructor.
     *
     * @param resCode Result code.
     */
    public GridClientHandshakeResponse(byte resCode) {
        this.resCode = resCode;
    }

    /**
     * @return Result code.
     */
    public byte resultCode() {
        return resCode;
    }

    /** {@inheritDoc} */
    @Override public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);

        out.writeByte(resCode);
    }

    /** {@inheritDoc} */
    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);

        resCode = in.readByte();
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return getClass().getSimpleName() + " [resCode=" + resCode + ']';
    }
}
