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

package org.gridgain.grid.spi.communication;

import org.gridgain.grid.lang.*;

import java.io.*;
import java.util.*;

/**
 * Listener SPI notifies IO manager with.
 * <p>
 * {@link GridCommunicationSpi} should ignore very first 4 bytes received from
 * sender node and pass the rest of the message to the listener.
 */
public interface GridCommunicationListener<T extends Serializable> {
    /**
     * <b>NOTE:</b> {@link GridCommunicationSpi} should ignore very first 4 bytes received from
     * sender node and pass the rest of the received message to the listener.
     *
     * @param nodeId Node ID.
     * @param msg Message.
     * @param msgC Runnable to call when message processing finished.
     */
    public void onMessage(UUID nodeId, T msg, GridRunnable msgC);

    /**
     * Callback invoked when connection with remote node is lost.
     *
     * @param nodeId Node ID.
     */
    public void onDisconnected(UUID nodeId);
}
