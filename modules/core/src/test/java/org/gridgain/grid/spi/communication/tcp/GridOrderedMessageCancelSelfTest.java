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

package org.gridgain.grid.spi.communication.tcp;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.cache.query.*;
import org.gridgain.grid.compute.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.kernal.managers.communication.*;
import org.gridgain.grid.kernal.processors.cache.query.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.marshaller.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.grid.util.direct.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.testframework.junits.common.*;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.concurrent.*;

import static org.gridgain.grid.cache.GridCacheMode.*;
import static org.gridgain.grid.cache.GridCachePreloadMode.*;

/**
 *
 */
public class GridOrderedMessageCancelSelfTest extends GridCommonAbstractTest {
    /** IP finder. */
    private static final GridTcpDiscoveryIpFinder IP_FINDER = new GridTcpDiscoveryVmIpFinder(true);

    /** Cancel latch. */
    private static CountDownLatch cancelLatch;

    /** Process response latch. */
    private static CountDownLatch resLatch;

    /** Finish latch. */
    private static CountDownLatch finishLatch;

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        GridCacheConfiguration cache = defaultCacheConfiguration();

        cache.setCacheMode(PARTITIONED);
        cache.setPreloadMode(NONE);

        cfg.setCacheConfiguration(cache);

        cfg.setCommunicationSpi(new CommunicationSpi());

        GridTcpDiscoverySpi disco = new GridTcpDiscoverySpi();

        disco.setIpFinder(IP_FINDER);

        cfg.setDiscoverySpi(disco);

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        cancelLatch = new CountDownLatch(1);
        resLatch = new CountDownLatch(1);
        finishLatch = new CountDownLatch(1);

        startGridsMultiThreaded(2);
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        stopAllGrids();
    }

    /**
     * @throws Exception If failed.
     */
    public void testQuery() throws Exception {
        GridCacheQueryFuture<Map.Entry<Object, Object>> fut =
            grid(0).cache(null).queries().createSqlQuery(String.class, "_key is not null").execute();

        testMessageSet(fut);
    }

    /**
     * @throws Exception If failed.
     */
    public void testTask() throws Exception {
        GridComputeTaskFuture<Void> fut = grid(0).forRemotes().compute().execute(Task.class, null);

        testMessageSet(fut);
    }

    /**
     * @throws Exception If failed.
     */
    public void testTaskException() throws Exception {
        GridComputeTaskFuture<Void> fut = grid(0).forRemotes().compute().execute(FailTask.class, null);

        testMessageSet(fut);
    }

    /**
     * @param fut Future to cancel.
     * @throws Exception If failed.
     */
    private void testMessageSet(GridFuture<?> fut) throws Exception {
        cancelLatch.await();

        assertTrue(fut.cancel());

        resLatch.countDown();

        finishLatch.await();

        Map map = U.field(((GridKernal)grid(0)).context().io(), "msgSetMap");

        info("Map: " + map);

        assertTrue(map.isEmpty());
    }

    /**
     * Communication SPI.
     */
    private static class CommunicationSpi extends GridTcpCommunicationSpi {
        /** */
        @GridMarshallerResource
        private GridMarshaller marsh;

        /** {@inheritDoc} */
        @Override protected void notifyListener(UUID sndId, GridTcpCommunicationMessageAdapter msg,
            GridRunnable msgC) {
            GridIoMessage ioMsg = (GridIoMessage)msg;

            boolean wait = ioMsg.message() instanceof GridCacheQueryResponse ||
                ioMsg.message() instanceof GridJobExecuteResponse;

            if (wait) {
                cancelLatch.countDown();

                U.awaitQuiet(resLatch);
            }

            super.notifyListener(sndId, msg, msgC);

            if (wait)
                finishLatch.countDown();
        }
    }

    /**
     * Test task.
     */
    @GridComputeTaskSessionFullSupport
    private static class Task extends GridComputeTaskSplitAdapter<Void, Void> {
        /** {@inheritDoc} */
        @Override protected Collection<? extends GridComputeJob> split(int gridSize, Void arg) throws GridException {
            return Collections.singleton(new GridComputeJobAdapter() {
                @Nullable @Override public Object execute() {
                    return null;
                }
            });
        }

        /** {@inheritDoc} */
        @Nullable @Override public Void reduce(List<GridComputeJobResult> results) throws GridException {
            return null;
        }
    }

    /**
     * Test task.
     */
    @GridComputeTaskSessionFullSupport
    private static class FailTask extends GridComputeTaskSplitAdapter<Void, Void> {
        /** {@inheritDoc} */
        @Override protected Collection<? extends GridComputeJob> split(int gridSize, Void arg) throws GridException {
            return Collections.singleton(new GridComputeJobAdapter() {
                @Nullable @Override public Object execute() throws GridException {
                    throw new GridException("Task failed.");
                }
            });
        }

        /** {@inheritDoc} */
        @Nullable @Override public Void reduce(List<GridComputeJobResult> results) throws GridException {
            return null;
        }
    }
}
