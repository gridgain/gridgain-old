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

package org.gridgain.grid.util.ipc.shmem;

import org.gridgain.grid.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.ipc.*;
import org.gridgain.testframework.junits.*;

import java.io.*;

/**
 * Test-purposed app launching {@link GridIpcSharedMemoryServerEndpoint} and designed
 * to be used with conjunction to {@link GridJavaProcess}.
 */
public class GridGgfsSharedMemoryTestServer {
    @SuppressWarnings({"BusyWait", "InfiniteLoopStatement"})
    public static void main(String[] args) throws GridException {
        System.out.println("Starting server ...");

        U.setWorkDirectory(null, U.getGridGainHome());

        // Tell our process PID to the wrapper.
        X.println(GridJavaProcess.PID_MSG_PREFIX + U.jvmPid());

        InputStream is = null;

        try {
            GridIpcServerEndpoint srv = new GridIpcSharedMemoryServerEndpoint();

            new GridTestResources().inject(srv);

            srv.start();

            GridIpcEndpoint clientEndpoint = srv.accept();

            is = clientEndpoint.inputStream();

            for (;;) {
                X.println("Before read.");

                is.read();

                Thread.sleep(GridIpcSharedMemoryCrashDetectionSelfTest.RW_SLEEP_TIMEOUT);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            U.closeQuiet(is);
        }
    }
}
