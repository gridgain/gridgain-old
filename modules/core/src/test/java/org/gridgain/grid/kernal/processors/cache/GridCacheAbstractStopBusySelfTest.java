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

package org.gridgain.grid.kernal.processors.cache;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.communication.tcp.*;
import org.gridgain.grid.util.direct.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.testframework.*;
import org.gridgain.testframework.junits.common.*;
import org.jetbrains.annotations.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static org.gridgain.grid.cache.GridCacheAtomicityMode.*;
import static org.gridgain.grid.cache.GridCacheDistributionMode.*;
import static org.gridgain.grid.cache.GridCacheMode.*;
import static org.gridgain.grid.cache.GridCachePreloadMode.*;
import static org.gridgain.grid.cache.GridCacheWriteSynchronizationMode.*;

/**
 * Stops node when there are ongoing user operations.
 */
public abstract class GridCacheAbstractStopBusySelfTest extends GridCommonAbstractTest {
    /** */
    public static final int CLN_GRD = 0;

    /** */
    public static final int SRV_GRD = 1;

    /** */
    public static final String CACHE_NAME = "StopTest";

    /** */
    private AtomicBoolean suspended = new AtomicBoolean(false);

    /**
     * @return Cache distribution mode.
     */
    protected GridCacheDistributionMode cacheDistributionMode() {
        return PARTITIONED_ONLY;
    }

    /**
     * @return Cache mode.
     */
    protected GridCacheMode cacheMode(){
        return PARTITIONED;
    }

    /**
     * @return Cache atomicity mode.
     */
    protected GridCacheAtomicityMode atomicityMode(){
        return TRANSACTIONAL;
    }

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        GridCacheConfiguration cacheCfg = cacheConfiguration(CACHE_NAME);

        TestTpcCommunicationSpi commSpi = new TestTpcCommunicationSpi();

        commSpi.setLocalPort(GridTestUtils.getNextCommPort(getClass()));

        commSpi.setTcpNoDelay(true);

        if (gridName.endsWith(String.valueOf(CLN_GRD)))
            cacheCfg.setDistributionMode(CLIENT_ONLY);

        cacheCfg.setPreloadMode(SYNC);

        cacheCfg.setWriteSynchronizationMode(FULL_SYNC);

        cacheCfg.setBackups(1);

        cfg.setCommunicationSpi(commSpi);

        cfg.setCacheConfiguration(cacheCfg);

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        beforeTestsStarted();

        startGrid(SRV_GRD);

        startGrid(CLN_GRD);

        TimeUnit.SECONDS.sleep(1L);
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        suspended.set(false);

        afterTestsStopped();

        stopGrid(SRV_GRD);

        stopGrid(CLN_GRD);

        assert G.allGrids().isEmpty();
    }

    /**
     * @throws Exception If failed.
     */
    public void testPut() throws Exception {
        executeTest(new Callable<Integer>() {
            /** {@inheritDoc} */
            @Override public Integer call() throws Exception {
                info("Start operation.");

                Integer val = (Integer)clientCache().put(1, 999);

                info("Stop operation.");

                return val;
            }
        });
    }

    /**
     * @throws Exception If failed.
     */
    public void testRemove() throws Exception {
        executeTest(new Callable<Integer>() {
            /** {@inheritDoc} */
            @Override public Integer call() throws Exception {
                info("Start operation.");

                Integer val = (Integer)clientCache().remove(1);

                info("Stop operation.");

                return val;
            }
        });
    }

    /**
     * @throws Exception If failed.
     */
    public void testPutx() throws Exception {
        executeTest(new Callable<Boolean>() {
            /** {@inheritDoc} */
            @Override public Boolean call() throws Exception {
                info("Start operation.");

                Boolean put = clientCache().putx(1, 1);

                info("Stop operation.");

                return put;
            }
        });
    }

    /**
     * @throws Exception If failed.
     */
    public void testReplace() throws Exception {
        executeTest(new Callable<Boolean>() {
            /** {@inheritDoc} */
            @Override public Boolean call() throws Exception {
                info("Start operation.");

                Boolean put = clientCache().replace(1, 1, 2);

                info("Stop operation.");

                return put;
            }
        });
    }

    /**
     * @throws Exception If failed.
     */
    public void testPutAsync() throws Exception {
        executeTest(new Callable<Object>() {
            /** {@inheritDoc} */
            @Override public Object call() throws Exception {
                info("Start operation.");

                GridFuture<Object> fut = clientCache().putAsync(1, 1);

                info("Stop operation.");

                return fut.get();
            }
        });
    }

    /**
     * @throws Exception If failed.
     */
    public void testExplicitTx() throws Exception {
        if (atomicityMode() != TRANSACTIONAL)
            return;

        executeTest(new Callable<Boolean>() {
            /** {@inheritDoc} */
            @Override public Boolean call() throws Exception {
                info("Start operation.");

                GridCacheTx gridCacheTx = clientCache().txStart();

                clientCache().put(1, 100);

                clientCache().get(1);

                gridCacheTx.commit();

                info("Stop operation.");

                return true;
            }
        });
    }

    /**
     * @throws Exception If failed.
     */
    public void testGet() throws Exception {
        executeTest(new Callable<Integer>() {
            /** {@inheritDoc} */
            @Override public Integer call() throws Exception {
                info("Start operation.");

                Integer put = (Integer) clientCache().get(1);

                info("Stop operation.");

                return put;
            }
        });
    }

    /**
     *
     * @param call Closure executing cache operation.
     * @throws Exception If failed.
     */
    private <T> void executeTest(Callable<T> call) throws Exception {
        suspended.set(true);

        GridFuture<T> fut = GridTestUtils.runAsync(call);

        Thread stopThread = new Thread(new StopRunnable());

        stopThread.start();

        stopThread.join(10000L);

        suspended.set(false);

        assert !stopThread.isAlive();

        Exception e = null;

        try {
            fut.get();
        }
        catch (GridException gridException){
            e = gridException;
        }

        assertNotNull(e);
    }

    /**
     * @throws Exception If failed.
     */
    public void testPutBatch() throws Exception {
        assert !suspended.get();

        GridFuture<Void> fut = GridTestUtils.runAsync(new Callable<Void>() {
            /** {@inheritDoc} */
            @Override public Void call() throws Exception {
                for (int i = 0; i < 1_000_000; i++)
                    clientCache().put(i, i);

                return null;
            }
        });

        Thread stopThread = new Thread(new StopRunnable());

        U.sleep(100);

        stopThread.start();

        stopThread.join(10000L);

        assert !stopThread.isAlive();

        Exception e = null;

        try {
            fut.get();
        }
        catch (GridException gridException){
            e = gridException;
        }

        assertNotNull(e);
    }

    /**
     * @return Client cache.
     */
    private GridCache<Object, Object> clientCache() {
        return grid(CLN_GRD).cache(CACHE_NAME);
    }

    /**
     * @param cacheName Cache name.
     * @return Cache configuration.
     * @throws Exception In case of error.
     */
    private GridCacheConfiguration cacheConfiguration(@Nullable String cacheName) throws Exception {
        GridCacheConfiguration cfg = defaultCacheConfiguration();

        cfg.setCacheMode(cacheMode());

        cfg.setAtomicityMode(atomicityMode());

        cfg.setDistributionMode(cacheDistributionMode());

        cfg.setName(cacheName);

        return cfg;
    }

    /**
     *
     */
    private class TestTpcCommunicationSpi extends GridTcpCommunicationSpi {
        /** {@inheritDoc} */
        @Override public void sendMessage(GridNode node, GridTcpCommunicationMessageAdapter msg) throws GridSpiException {
            if (suspended.get())
                return;

            super.sendMessage(node, msg);
        }
    }

    /**
     *
     */
    private class StopRunnable implements Runnable {
        /** {@inheritDoc} */
        @Override public void run() {
            info("Stopping grid...");

            stopGrid(CLN_GRD, true);

            info("Grid stopped.");
        }
    }
}
