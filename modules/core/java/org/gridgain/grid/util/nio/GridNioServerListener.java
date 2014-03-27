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
package org.gridgain.grid.util.nio;

import org.jetbrains.annotations.*;

/**
 * Listener passed in to the {@link GridNioServer} that will be notified on client events.
 */
public interface GridNioServerListener<T> {
    /**
     * This method is called whenever a new client is connected and session is created.
     *
     * @param ses Newly created session for remote client.
     */
    public void onConnected(GridNioSession ses);

    /**
     * This method is called whenever client is disconnected due to correct connection close
     * or due to {@code IOException} during network operations.
     *
     * @param ses Closed session.
     * @param e Exception occurred, if any.
     */
    public void onDisconnected(GridNioSession ses, @Nullable Exception e);

    /**
     * This method is called whenever a {@link GridNioParser} returns non-null value.
     *
     * @param ses Session on which message was received.
     * @param msg Parsed message.
     */
    public void onMessage(GridNioSession ses, T msg);

    /**
     * Called when session has non-empty write queue and server did not send any data
     * within timeout interval.
     *
     * @param ses Session that has timed out writes.
     */
    public void onSessionWriteTimeout(GridNioSession ses);

    /**
     * Called when session did not receive any activity within timeout interval.
     *
     * @param ses Session that is idle.
     */
    public void onSessionIdleTimeout(GridNioSession ses);
}
