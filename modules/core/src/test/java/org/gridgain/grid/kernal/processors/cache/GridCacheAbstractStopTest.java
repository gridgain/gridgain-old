/* @java.file.header */

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
import org.gridgain.testframework.*;
import org.gridgain.testframework.junits.common.*;
import org.jetbrains.annotations.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Stopped node when client operations are executing.
 */
public abstract class GridCacheAbstractStopTest extends GridCommonAbstractTest {
    /** */
    public static final int CLN_GRD = 0;

    /** */
    public static final int SRV_GRD = 1;

    /** */
    public static final String CACHE_NAME = "StopTest";

    /** */
    private AtomicBoolean suspended = new AtomicBoolean(false);

    /**
     * Constructs test.
     */
    public GridCacheAbstractStopTest() {
        super(/* don't start grid */ false);
    }

    /**
     *
     */
    protected GridCacheDistributionMode cacheDistributionMode() {
        return GridCacheDistributionMode.PARTITIONED_ONLY;
    }

    /**
     *
     */
    protected GridCacheMode cacheMode(){
        return GridCacheMode.PARTITIONED;
    }

    /**
     *
     */
    protected GridCacheAtomicityMode atomicityMode(){
        return GridCacheAtomicityMode.TRANSACTIONAL;
    }

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        GridCacheConfiguration cacheCfg = cacheConfiguration(CACHE_NAME);
        TestTpcCommunicationSpi commSpi = new TestTpcCommunicationSpi();

        commSpi.setLocalPort(GridTestUtils.getNextCommPort(getClass()));
        commSpi.setTcpNoDelay(true);

        if (gridName.endsWith(String.valueOf(CLN_GRD)))
            cacheCfg.setDistributionMode(GridCacheDistributionMode.CLIENT_ONLY);

        cacheCfg.setPreloadMode(GridCachePreloadMode.SYNC);
        cacheCfg.setWriteSynchronizationMode(GridCacheWriteSynchronizationMode.FULL_SYNC);
        cacheCfg.setBackups(1);


        cfg.setCommunicationSpi(commSpi);

        cfg.setCacheConfiguration(cacheCfg);

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        super.beforeTestsStarted();

        startGrid(SRV_GRD);

        startGrid(CLN_GRD);

        TimeUnit.SECONDS.sleep(1L);
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        suspended.set(false);

        super.afterTestsStopped();

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
                Integer val = (Integer) clientCache().put(1, 999);
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
                Integer val = (Integer) clientCache().remove(1);
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
        executeTest(new Callable<Boolean>() {
            /** {@inheritDoc} */
            @Override public Boolean call() throws Exception {
                info("Start operation.");
                clientCache().putAsync(1, 1);
                info("Stop operation.");
                return true;
            }
        });
    }

    /**
     * @throws Exception If failed.
     */
    public void testExplicitTx() throws Exception {
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

    private <T> void executeTest(Callable<T> callable) throws Exception {
        suspended.set(true);

        GridTestUtils.runAsync(callable);

        Thread stopThread = new Thread(new StopRunnable());

        stopThread.start();

        stopThread.join(10000L);

        suspended.set(false);

        assert !stopThread.isAlive();
    }

    /**
     *
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

        cfg.setBackups(0);

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

        /**
         *
         */
        public TestTpcCommunicationSpi() {
            super();
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

    /** {@inheritDoc} */
    @Override protected long getTestTimeout() {
        return TimeUnit.DAYS.toMillis(1L);
    }
}
