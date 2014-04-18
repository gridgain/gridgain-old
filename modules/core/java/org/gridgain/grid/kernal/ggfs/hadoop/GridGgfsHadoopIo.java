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

package org.gridgain.grid.kernal.ggfs.hadoop;

import org.gridgain.grid.*;
import org.gridgain.grid.kernal.ggfs.common.*;
import org.gridgain.grid.util.lang.*;
import org.jetbrains.annotations.*;

/**
 * IO abstraction layer for GGFS client. Two kind of messages are expected to be sent: requests with response
 * and request without response.
 */
public interface GridGgfsHadoopIo {
    /**
     * Sends given GGFS client message and asynchronously awaits for response.
     *
     * @param msg Message to send.
     * @return Future that will be completed.
     * @throws GridException If a message cannot be sent (connection is broken or client was closed).
     */
    public GridPlainFuture<GridGgfsMessage> send(GridGgfsMessage msg) throws GridException;

    /**
     * Sends given GGFS client message and asynchronously awaits for response. When IO detects response
     * beginning for given message it stops reading data and passes input stream to closure which can read
     * response in a specific way.
     *
     * @param msg Message to send.
     * @param outBuf Output buffer. If {@code null}, the output buffer is not used.
     * @param outOff Output buffer offset.
     * @param outLen Output buffer length.
     * @return Future that will be completed when response is returned from closure.
     * @throws GridException If a message cannot be sent (connection is broken or client was closed).
     */
    public <T> GridPlainFuture<T> send(GridGgfsMessage msg, @Nullable byte[] outBuf, int outOff, int outLen)
        throws GridException;

    /**
     * Sends given message and does not wait for response.
     *
     * @param msg Message to send.
     * @throws GridException If send failed.
     */
    public void sendPlain(GridGgfsMessage msg) throws GridException;

    /**
     * Adds event listener that will be invoked when connection with server is lost or remote error has occurred.
     * If connection is closed already, callback will be invoked synchronously inside this method.
     *
     * @param lsnr Event listener.
     */
    public void addEventListener(GridGgfsHadoopIpcIoListener lsnr);

    /**
     * Removes event listener that will be invoked when connection with server is lost or remote error has occurred.
     *
     * @param lsnr Event listener.
     */
    public void removeEventListener(GridGgfsHadoopIpcIoListener lsnr);
}
