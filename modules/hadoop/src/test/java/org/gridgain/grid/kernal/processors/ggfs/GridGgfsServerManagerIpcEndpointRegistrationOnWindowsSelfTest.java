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

package org.gridgain.grid.kernal.processors.ggfs;

import org.gridgain.grid.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.ipc.loopback.*;
import org.gridgain.grid.util.ipc.shmem.*;

import java.util.concurrent.*;

import static org.gridgain.testframework.GridTestUtils.*;

/**
 * Tests for {@link GridGgfsServerManager} that checks shmem IPC endpoint registration
 * forbidden for Windows.
 */
public class GridGgfsServerManagerIpcEndpointRegistrationOnWindowsSelfTest
    extends GridGgfsServerManagerIpcEndpointRegistrationAbstractSelfTest {
    /**
     * @throws Exception If failed.
     */
    public void testShmemEndpointsRegistration() throws Exception {
        Throwable e = assertThrows(log, new Callable<Object>() {
            @Override public Object call() throws Exception {
                GridConfiguration cfg = gridConfiguration();

                cfg.setGgfsConfiguration(gridGgfsConfiguration(
                    "{type:'shmem', port:" + GridIpcSharedMemoryServerEndpoint.DFLT_IPC_PORT + "}"));

                return G.start(cfg);
            }
        }, GridException.class, null);

        assert e.getCause().getMessage().contains(" should not be configured on Windows (configure " +
            GridIpcServerTcpEndpoint.class.getSimpleName() + ")");
    }
}
