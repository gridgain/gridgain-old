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

package org.gridgain.grid.kernal;

import org.gridgain.grid.*;
import org.gridgain.grid.compute.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.testframework.junits.common.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.util.*;

/**
 *
 */
@GridCommonTest(group = "Kernal Self")
public class GridTaskMapAsyncSelfTest extends GridCommonAbstractTest {
    /**
     *
     */
    public GridTaskMapAsyncSelfTest() {
        super(true);
    }

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration c = super.getConfiguration(gridName);

        GridTcpDiscoverySpi discoSpi = new GridTcpDiscoverySpi();

        discoSpi.setIpFinder(new GridTcpDiscoveryVmIpFinder(true));

        c.setDiscoverySpi(discoSpi);

        return c;
    }

    /**
     * @throws Exception If failed.
     */
    public void testTaskMap() throws Exception {
        Grid grid = G.grid(getTestGridName());

        info("Executing sync mapped task.");

        grid.compute().execute(SyncMappedTask.class, null).get();

        info("Executing async mapped task.");

        grid.compute().execute(AsyncMappedTask.class, null).get();
    }

    /**
     *
     */
    @GridComputeTaskMapAsync
    private static class AsyncMappedTask extends BaseTask {
        /** {@inheritDoc} */
        @Override protected Collection<? extends GridComputeJob> split(int gridSize, Object arg) throws GridException {
            Collection<? extends GridComputeJob> res = super.split(gridSize, arg);

            assert mainThread != mapper;

            return res;
        }
    }

    /**
     *
     */
    private static class SyncMappedTask extends BaseTask {
        /** {@inheritDoc} */
        @Override protected Collection<? extends GridComputeJob> split(int gridSize, Object arg) throws GridException {
            Collection<? extends GridComputeJob> res = super.split(gridSize, arg);

            assert mainThread == mapper;

            return res;
        }
    }

    /**
     * Test task.
     */
    private abstract static class BaseTask extends GridComputeTaskSplitAdapter<Object, Void> {
        /** */
        protected static final Thread mainThread = Thread.currentThread();

        /** */
        protected Thread mapper;

        /** */
        protected Thread runner;

        /** */
        @GridLoggerResource
        protected GridLogger log;

        /** {@inheritDoc} */
        @Override protected Collection<? extends GridComputeJob> split(int gridSize, Object arg) throws GridException {
            mapper = Thread.currentThread();

            return Collections.singleton(new GridComputeJobAdapter() {
                @Override public Serializable execute() {
                    runner = Thread.currentThread();

                    log.info("Runner: " + runner);
                    log.info("Main: " + mainThread);
                    log.info("Mapper: " + mapper);

                    return null;
                }
            });
        }

        /** {@inheritDoc} */
        @Nullable @Override public Void reduce(List<GridComputeJobResult> results) throws GridException {
            return null;
        }
    }
}
