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
import java.util.*;

/**
 * Interface for all client messages.
 */
public interface GridClientMessage extends Serializable {
    /**
     * This method is used to match request and response messages.
     *
     * @return request ID.
     */
    public long requestId();

    /**
     * Sets request id for outgoing packets.
     *
     * @param reqId request ID.
     */
    public void requestId(long reqId);

    /**
     * Gets client identifier from which this request comes.
     *
     * @return Client identifier.
     */
    public UUID clientId();

    /**
     * Sets client identifier from which this request comes.
     *
     * @param id Client identifier.
     */
    public void clientId(UUID id);

    /**
     * Gets identifier of the node where this message should be processed.
     *
     * @return Client identifier.
     */
    public UUID destinationId();

    /**
     * Sets identifier of the node where this message should be eventually delivered.
     *
     * @param id Client identifier.
     */
    public void destinationId(UUID id);

    /**
     * Gets client session token.
     *
     * @return Session token.
     */
    public byte[] sessionToken();

    /**
     * Sets client session token.
     *
     * @param sesTok Session token.
     */
    public void sessionToken(byte[] sesTok);
}
