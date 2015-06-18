/* @java.file.header */

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.kernal;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.compute.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.testframework.*;
import org.gridgain.testframework.junits.common.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static org.gridgain.grid.cache.GridCacheAtomicityMode.*;
import static org.gridgain.grid.cache.GridCacheMode.*;
import static org.gridgain.grid.cache.GridCachePreloadMode.*;

/**
 *
 */
public class GridTaskFailoverAffinityRunTest extends GridCommonAbstractTest {
    /** */
    private static GridTcpDiscoveryIpFinder ipFinder = new GridTcpDiscoveryVmIpFinder(true);

    /** */
    private boolean clientCache;

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        ((GridTcpDiscoverySpi)cfg.getDiscoverySpi()).setIpFinder(ipFinder);

        boolean cache = clientCache || !gridName.equals(getTestGridName(0));

        if (cache) {
            GridCacheConfiguration ccfg = new GridCacheConfiguration();

            ccfg.setCacheMode(PARTITIONED);
            ccfg.setBackups(1);
            ccfg.setAtomicityMode(ATOMIC);
            ccfg.setPreloadMode(SYNC);

            cfg.setCacheConfiguration(ccfg);
        }

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        super.afterTest();

        stopAllGrids();
    }

    /**
     * @throws Exception If failed.
     */
    public void testNodeRestart() throws Exception {
        clientCache = true;

        nodeRestart();
    }

    /**
     * @throws Exception If failed.
     */
    public void testNodeRestartNoClientCache() throws Exception {
        clientCache = false;

        nodeRestart();
    }

    /**
     * @throws Exception If failed.
     */
    private void nodeRestart() throws Exception {
        startGridsMultiThreaded(4);

        GridCompute comp = grid(0).compute();

        final AtomicBoolean stop = new AtomicBoolean();

        final AtomicInteger gridIdx = new AtomicInteger(1);

        GridFuture<?> fut = GridTestUtils.runMultiThreadedAsync(new Callable<Object>() {
            @Override public Object call() throws Exception {
                int grid = gridIdx.getAndIncrement();

                while (!stop.get()) {
                    stopGrid(grid);

                    startGrid(grid);
                }

                return null;
            }
        }, 2, "restart-thread");

        try {
            long stopTime = System.currentTimeMillis() + 60_000;

            while (System.currentTimeMillis() < stopTime) {
                Collection<GridFuture<?>> futs = new ArrayList<>(1000);

                for (int i = 0; i < 1000; i++)
                    futs.add(comp.affinityCall(null, i, new TestJob()));

                for (GridFuture<?> fut0 : futs) {
                    try {
                        fut0.get();
                    }
                    catch (GridException ignore) {
                        // No-op.
                    }
                }
            }
        }
        finally {
            stop.set(true);

            fut.get();
        }
    }

    /**
     *
     */
    private static class TestJob implements GridCallable<Object> {
        /** {@inheritDoc} */
        @Override public Object call() throws Exception {
            return null;
        }
    }
}
