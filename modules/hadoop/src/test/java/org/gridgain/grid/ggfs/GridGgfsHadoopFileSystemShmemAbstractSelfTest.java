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

package org.gridgain.grid.ggfs;

import org.apache.commons.lang.exception.*;
import org.gridgain.grid.*;
import org.gridgain.grid.util.ipc.*;
import org.gridgain.testframework.*;

import java.util.*;
import java.util.concurrent.*;

import static org.gridgain.grid.util.ipc.shmem.GridIpcSharedMemoryServerEndpoint.*;

/**
 * GGFS Hadoop file system IPC self test.
 */
public abstract class GridGgfsHadoopFileSystemShmemAbstractSelfTest extends GridGgfsHadoopFileSystemAbstractSelfTest {
    /**
     * Constructor.
     *
     * @param mode GGFS mode.
     */
    protected GridGgfsHadoopFileSystemShmemAbstractSelfTest(GridGgfsMode mode) {
        super(mode);
    }

    /** {@inheritDoc} */
    @Override protected String primaryFileSystemUriPath() {
        return "ggfs://primary/";
    }

    /** {@inheritDoc} */
    @Override protected String primaryFileSystemConfigPath() {
        return "modules/core/src/test/config/hadoop/core-site.xml";
    }

    /** {@inheritDoc} */
    @Override protected String primaryIpcEndpointConfiguration(String gridName) {
        return "{type:'shmem', port:" + (DFLT_IPC_PORT + getTestGridIndex(gridName)) + "}";
    }

    /** {@inheritDoc} */
    @Override protected String primaryFileSystemEndpoint() {
        return "shmem:10500";
    }

    /** {@inheritDoc} */
    @Override protected String secondaryFileSystemUriPath() {
        return "ggfs://secondary/";
    }

    /** {@inheritDoc} */
    @Override protected String secondaryFileSystemConfigPath() {
        return "modules/core/src/test/config/hadoop/core-site-secondary.xml";
    }

    /** {@inheritDoc} */
    @Override protected String secondaryFileSystemEndpoint() {
        return "127.0.0.1:11500";
    }

    /** {@inheritDoc} */
    @Override protected String secondaryIpcEndpointConfiguration() {
        return "{type:'shmem', port:11500}";
    }

    /**
     * Checks correct behaviour in case when we run out of system
     * resources.
     *
     * @throws Exception If error occurred.
     */
    public void testOutOfResources() throws Exception {
        final Collection<GridIpcEndpoint> eps = new LinkedList<>();

        try {
            GridException e = (GridException)GridTestUtils.assertThrows(log, new Callable<Object>() {
                @SuppressWarnings("InfiniteLoopStatement")
                @Override public Object call() throws Exception {
                    while (true) {
                        GridIpcEndpoint ep = GridIpcEndpointFactory.connectEndpoint("shmem:10500", log);

                        eps.add(ep);
                    }
                }
            }, GridException.class, null);

            assertNotNull(e);

            String msg = e.getMessage();

            assertTrue("Invalid exception: " + ExceptionUtils.getFullStackTrace(e),
                msg.contains("(error code: 28)") ||
                msg.contains("(error code: 24)") ||
                msg.contains("(error code: 12)"));
        }
        finally {
            for (GridIpcEndpoint ep : eps)
                ep.close();
        }
    }
}
