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

package org.gridgain.grid.util.ipc;

import org.gridgain.grid.*;

import java.io.*;

/**
 * GGFS IPC endpoint used for point-to-point communication.
 */
public interface GridIpcEndpoint extends Closeable {
    /**
     * Gets input stream associated with this IPC endpoint.
     *
     * @return IPC input stream.
     * @throws GridException If error occurred.
     */
    public InputStream inputStream() throws GridException;

    /**
     * Gets output stream associated with this IPC endpoint.
     *
     * @return IPC output stream.
     * @throws GridException If error occurred.
     */
    public OutputStream outputStream() throws GridException;

    /**
     * Closes endpoint. Note that IPC endpoint may acquire native resources so it must be always closed
     * once it is not needed.
     */
    @Override public void close();
}
