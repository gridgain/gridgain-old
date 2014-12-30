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
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.testframework.*;
import org.gridgain.testframework.junits.common.*;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Cancel async cache operations tests.
 */
public class GridCacheStopTest extends GridCommonAbstractTest {
    /** */
    public static final int CLN_GRD = 0;

    /** */
    public static final int SRV_GRD = 1;

    /** */
    public static final String CACHE_NAME = "StopTest";

    /** */
    private AtomicBoolean suspended = new AtomicBoolean(false);

    /** */
    private List<TestTpcCommunicationSpi> commSpis = new ArrayList<>();

    /**
     * Constructs test.
     */
    public GridCacheStopTest() {
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

        commSpis.add(commSpi);

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        super.beforeTestsStarted();

        startGrid(SRV_GRD);

        startGrid(CLN_GRD);

        TimeUnit.SECONDS.sleep(1L);
    }

    /** {@inheritDoc} */
    @Override protected void afterTestsStopped() throws Exception {
        super.afterTestsStopped();

        stopGrid(SRV_GRD);
        stopGrid(CLN_GRD);

        assert G.allGrids().isEmpty();
    }

    /**
     * @throws Exception If failed.
     */
    public void testAsyncStop() throws Exception {
        suspended.set(true);

        GridTestUtils.runAsync(new Callable<Integer>() {
            /** {@inheritDoc} */
            @Override public Integer call() throws Exception {
                info("Start put.");
                Integer put = (Integer) clientCache().put(1, 999);
                info("Stop put.");
                return put;
            }
        });

        U.sleep(300L);

        Thread stopThread = new Thread(new StopRunnable());

        stopThread.start();

        stopThread.join(5000L);

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
}
