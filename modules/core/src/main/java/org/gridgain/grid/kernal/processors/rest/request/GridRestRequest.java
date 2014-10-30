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

package org.gridgain.grid.kernal.processors.rest.request;

import org.gridgain.grid.kernal.processors.rest.*;
import org.gridgain.grid.util.tostring.*;
import org.gridgain.grid.util.typedef.internal.*;

import java.net.*;
import java.util.*;

/**
 * Grid command request.
 */
public class GridRestRequest {
    /** Destination ID. */
    private UUID destId;

    /** Client ID. */
    private UUID clientId;

    /** Client network address. */
    private InetSocketAddress addr;

    /** Client credentials. */
    @GridToStringExclude
    private Object cred;

    /** Client session token. */
    private byte[] sesTok;

    /** Command. */
    private GridRestCommand cmd;

    /** Portable mode flag. */
    private boolean portableMode;

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
     * @return Command.
     */
    public GridRestCommand command() {
        return cmd;
    }

    /**
     * @param cmd Command.
     */
    public void command(GridRestCommand cmd) {
        this.cmd = cmd;
    }

    /**
     * Gets client ID that performed request.
     *
     * @return Client ID.
     */
    public UUID clientId() {
        return clientId;
    }

    /**
     * Sets client ID that performed request.
     *
     * @param clientId Client ID.
     */
    public void clientId(UUID clientId) {
        this.clientId = clientId;
    }

    /**
     * Gets client credentials for authentication process.
     *
     * @return Credentials.
     */
    public Object credentials() {
        return cred;
    }

    /**
     * Sets client credentials for authentication.
     *
     * @param cred Credentials.
     */
    public void credentials(Object cred) {
        this.cred = cred;
    }

    /**
     * Gets session token for already authenticated client.
     *
     * @return Session token.
     */
    public byte[] sessionToken() {
        return sesTok;
    }

    /**
     * Sets session token for already authenticated client.
     *
     * @param sesTok Session token.
     */
    public void sessionToken(byte[] sesTok) {
        this.sesTok = sesTok;
    }

    /**
     * @return Client address.
     */
    public InetSocketAddress address() {
        return addr;
    }

    /**
     * @param addr Client address.
     */
    public void address(InetSocketAddress addr) {
        this.addr = addr;
    }

    /**
     * @return Portable mode flag.
     */
    public boolean portableMode() {
        return portableMode;
    }

    /**
     * @param portableMode Portable mode flag.
     */
    public void portableMode(boolean portableMode) {
        this.portableMode = portableMode;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridRestRequest.class, this);
    }
}
