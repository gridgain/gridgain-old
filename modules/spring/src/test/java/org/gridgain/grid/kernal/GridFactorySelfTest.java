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
import org.gridgain.grid.events.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.marshaller.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.collision.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.util.lang.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.testframework.*;
import org.gridgain.testframework.config.*;
import org.gridgain.testframework.http.*;
import org.gridgain.testframework.junits.common.*;
import org.jetbrains.annotations.*;
import org.springframework.beans.*;
import org.springframework.beans.factory.config.*;
import org.springframework.beans.factory.support.*;
import org.springframework.beans.factory.xml.*;
import org.springframework.context.*;
import org.springframework.context.support.*;
import org.springframework.core.io.*;

import javax.management.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static org.gridgain.grid.GridGainState.*;
import static org.gridgain.grid.GridSystemProperties.*;

/**
 * Tests for {@link GridGain}.
 * @see GridFactoryVmShutdownTest
 */
@SuppressWarnings("UnusedDeclaration")
@GridCommonTest(group = "NonDistributed Kernal Self")
public class GridFactorySelfTest extends GridCommonAbstractTest {
    /** */
    private static final AtomicInteger cnt = new AtomicInteger();

    /** */
    private static final String CUSTOM_CFG_PATH = "modules/core/src/test/config/factory/custom-grid-name-spring-test.xml";

    /**
     *
     */
    public GridFactorySelfTest() {
        super(false);

        System.setProperty(GG_OVERRIDE_MCAST_GRP,
            GridTestUtils.getNextMulticastGroup(GridFactorySelfTest.class));
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        cnt.set(0);
    }

    /**
     * @throws Exception If failed.
     */
    public void testStartGridWithConfigUrlString() throws Exception {
        GridEmbeddedHttpServer srv = null;
        String gridName = "grid_with_url_config";

        try {
            srv = GridEmbeddedHttpServer.startHttpServer().withFileDownloadingHandler(null,
                GridTestUtils.resolveGridGainPath("/modules/core/src/test/config/default-spring-url-testing.xml"));

            Grid grid = G.start(srv.getBaseUrl());

            assert gridName.equals(grid.name()) : "Unexpected grid name: " + grid.name();
        }
        finally {
            if (srv != null)
                srv.stop(1);

            G.stop(gridName, false);
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testStartGridWithConfigUrl() throws Exception {
        GridEmbeddedHttpServer srv = null;
        String gridName = "grid_with_url_config";

        try {
            srv = GridEmbeddedHttpServer.startHttpServer().withFileDownloadingHandler(null,
                GridTestUtils.resolveGridGainPath("modules/core/src/test/config/default-spring-url-testing.xml"));

            Grid grid = G.start(new URL(srv.getBaseUrl()));

            assert gridName.equals(grid.name()) : "Unexpected grid name: " + grid.name();
        }
        finally {
            if (srv != null)
                srv.stop(1);

            G.stop(gridName, false);
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testLifecycleBeansNullGridName() throws Exception {
        checkLifecycleBeans(null);
    }

    /**
     * @throws Exception If failed.
     */
    public void testLifecycleBeansNotNullGridName() throws Exception {
        checkLifecycleBeans("testGrid");
    }

    /**
     * @param gridName Grid name.
     * @throws Exception If test failed.
     */
    private void checkLifecycleBeans(@Nullable String gridName) throws Exception {
        TestLifecycleBean bean1 = new TestLifecycleBean();
        TestLifecycleBean bean2 = new TestLifecycleBean();

        GridConfiguration cfg = new GridConfiguration();

        cfg.setLifecycleBeans(bean1, bean2);
        cfg.setGridName(gridName);

        cfg.setRestEnabled(false);

        try (Grid g = GridGainSpring.start(cfg, new GenericApplicationContext())) {
            bean1.checkState(gridName, true);
            bean2.checkState(gridName, true);
        }

        bean1.checkState(gridName, false);
        bean2.checkState(gridName, false);

        checkLifecycleBean(bean1, gridName);
        checkLifecycleBean(bean2, gridName);
    }

    /**
     * @param bean Bean to check.
     * @param gridName Grid name to check for.
     */
    private void checkLifecycleBean(TestLifecycleBean bean, String gridName) {
        bean.checkErrors();

        List<GridLifecycleEventType> evts = bean.getLifecycleEvents();

        List<String> gridNames = bean.getGridNames();

        assert evts.get(0) == GridLifecycleEventType.BEFORE_GRID_START : "Invalid lifecycle event: " + evts.get(0);
        assert evts.get(1) == GridLifecycleEventType.AFTER_GRID_START : "Invalid lifecycle event: " + evts.get(1);
        assert evts.get(2) == GridLifecycleEventType.BEFORE_GRID_STOP : "Invalid lifecycle event: " + evts.get(2);
        assert evts.get(3) == GridLifecycleEventType.AFTER_GRID_STOP : "Invalid lifecycle event: " + evts.get(3);

        checkGridNameEquals(gridNames.get(0), gridName);
        checkGridNameEquals(gridNames.get(1), gridName);
        checkGridNameEquals(gridNames.get(2), gridName);
        checkGridNameEquals(gridNames.get(3), gridName);
    }

    /**
     * @param n1 First name.
     * @param n2 Second name.
     */
    private void checkGridNameEquals(String n1, String n2) {
        if (n1 == null) {
            assert n2 == null;

            return;
        }

        assert n1.equals(n2) : "Invalid grid names [name1=" + n1 + ", name2=" + n2 + ']';
    }

    /**
     * @throws Exception If failed.
     */
    @SuppressWarnings({"SynchronizationOnLocalVariableOrMethodParameter"})
    public void testStartMultipleGridsFromSpring() throws Exception {
        File cfgFile =
            GridTestUtils.resolveGridGainPath(GridTestProperties.getProperty("loader.self.multipletest.config"));

        assert cfgFile != null;

        String path = cfgFile.getAbsolutePath();

        info("Loading Grid from configuration file: " + path);

        final GridTuple<GridGainState> gridState1 = F.t(null);
        final GridTuple<GridGainState> gridState2 = F.t(null);

        final Object mux = new Object();

        GridGainListener factoryLsnr = new GridGainListener() {
            @Override public void onStateChange(String name, GridGainState state) {
                synchronized (mux) {
                    if ("grid-factory-test-1".equals(name))
                        gridState1.set(state);
                    else if ("grid-factory-test-2".equals(name))
                        gridState2.set(state);
                }
            }
        };

        G.addListener(factoryLsnr);

        G.start(path);

        assert G.grid("grid-factory-test-1") != null;
        assert G.grid("grid-factory-test-2") != null;

        synchronized (mux) {
            assert gridState1.get() == STARTED :
                "Invalid grid state [expected=" + STARTED + ", returned=" + gridState1 + ']';
            assert gridState2.get() == STARTED :
                "Invalid grid state [expected=" + STARTED + ", returned=" + gridState2 + ']';
        }

        G.stop("grid-factory-test-1", true);
        G.stop("grid-factory-test-2", true);

        synchronized (mux) {
            assert gridState1.get() == STOPPED :
                "Invalid grid state [expected=" + STOPPED + ", returned=" + gridState1 + ']';
            assert gridState2.get() == STOPPED :
                "Invalid grid state [expected=" + STOPPED + ", returned=" + gridState2 + ']';
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testStartMultipleDefaultGrids() throws Exception {
        try {
            multithreaded(
                new Callable<Object>() {
                    @Nullable @Override public Object call() throws Exception {
                        try {
                            GridConfiguration cfg = new GridConfiguration();

                            cfg.setRestEnabled(false);

                            G.start(cfg);
                        }
                        catch (Throwable t) {
                            error("Caught exception while starting grid.", t);
                        }

                        info("Thread finished.");

                        return null;
                    }
                },
                5,
                "grid-starter"
            );

            assert G.allGrids().size() == 1;

            assert G.grid() != null;
        }
        finally {
            G.stopAll(true);
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testStartMultipleNonDefaultGrids() throws Exception {
        try {
            multithreaded(
                new Callable<Object>() {
                    @Nullable @Override public Object call() throws Exception {
                        try {
                            GridConfiguration cfg = new GridConfiguration();

                            cfg.setGridName("TEST_NAME");
                            cfg.setRestEnabled(false);

                            G.start(cfg);
                        }
                        catch (Throwable t) {
                            error("Caught exception while starting grid.", t);
                        }

                        info("Thread finished.");

                        return null;
                    }
                },
                5,
                "grid-starter"
            );

            assert G.allGrids().size() == 1;

            assert G.grid("TEST_NAME") != null;
        }
        finally {
            G.stopAll(true);
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testConcurrentStartStop() throws Exception {
        checkConcurrentStartStop("TEST_NAME");
    }

    /**
     * @throws Exception If failed.
     */
    public void testConcurrentStartStopDefaultGrid() throws Exception {
        checkConcurrentStartStop(null);
    }

    /**
     * @param gridName Grid name ({@code null} for default grid).
     * @throws Exception If failed.
     */
    private void checkConcurrentStartStop(@Nullable final String gridName) throws Exception {
        final AtomicInteger startedCnt = new AtomicInteger();
        final AtomicInteger stoppedCnt = new AtomicInteger();

        GridGainListener lsnr = new GridGainListener() {
            @SuppressWarnings("StringEquality")
            @Override public void onStateChange(@Nullable String name, GridGainState state) {
                assert name == gridName;

                info("On state change fired: " + state);

                if (state == STARTED)
                    startedCnt.incrementAndGet();
                else {
                    assert state == STOPPED : "Unexpected state: " + state;

                    stoppedCnt.incrementAndGet();
                }
            }
        };

        G.addListener(lsnr);

        try {
            final int iterCnt = 3;

            multithreaded(
                new Callable<Object>() {
                    @Nullable @Override public Object call() throws Exception {
                        for (int i = 0; i < iterCnt; i++) {
                            try {
                                GridConfiguration cfg = getConfiguration(gridName);

                                G.start(cfg);
                            }
                            catch (Exception e) {
                                String msg = e.getMessage();

                                if (msg != null &&
                                    (msg.contains("Default grid instance has already been started.") ||
                                    msg.contains("Grid instance with this name has already been started:")))
                                    info("Caught expected exception: " + msg);
                                else
                                    throw e; // Unexpected exception.
                            }
                            finally {
                                stopGrid(gridName);
                            }
                        }

                        info("Thread finished.");

                        return null;
                    }
                },
                5,
                "tester"
            );

            assert G.allGrids().isEmpty();

            assert startedCnt.get() == iterCnt;
            assert stoppedCnt.get() == iterCnt;
        }
        finally {
            G.removeListener(lsnr);

            G.stopAll(true);
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testGridStartRollback() throws Exception {
        GridTestUtils.assertThrows(
            log,
            new Callable<Object>() {
                @Nullable @Override public Object call() throws Exception {
                    GridConfiguration cfg = new GridConfiguration();

                    cfg.setRestEnabled(false);

                    cfg.setDiscoverySpi(new GridTcpDiscoverySpi() {
                        @Override public void spiStart(String gridName) throws GridSpiException {
                            throw new GridSpiException("This SPI will never start.");
                        }
                    });

                    G.start(cfg);

                    info("Thread finished.");

                    return null;
                }
            },
            GridException.class,
            null
        );
    }

    /**
     * @throws Exception If failed.
     */
    public void disabledTestStartSingleInstanceSpi() throws Exception {
        GridConfiguration cfg1 = getConfiguration();
        GridConfiguration cfg2 = getConfiguration();

        cfg1.setCollisionSpi(new TestSingleInstancesCollisionSpi());
        cfg2.setCollisionSpi(new TestSingleInstancesCollisionSpi());

        G.start(cfg1);

        assert G.state(cfg1.getGridName()) == STARTED;
        assert G.state(getTestGridName() + '1') == STOPPED;

        G.stop(cfg1.getGridName(), false);

        assert G.state(cfg1.getGridName()) == STOPPED;
        assert G.state(getTestGridName() + '1') == STOPPED;

        cfg2.setGridName(getTestGridName() + '1');

        G.start(cfg2);

        assert G.state(cfg1.getGridName()) == STOPPED;
        assert G.state(getTestGridName() + '1') == STARTED;

        G.stop(getTestGridName() + '1', false);

        assert G.state(cfg1.getGridName()) == STOPPED;
        assert G.state(getTestGridName() + '1') == STOPPED;

        cfg2.setGridName(getTestGridName() + '1');

        G.start(cfg2);

        assert G.state(getTestGridName() + '1') == STARTED;
        assert G.state(getTestGridName()) == STOPPED;

        G.stop(getTestGridName() + '1', false);
        G.stop(getTestGridName(), false);

        assert G.state(getTestGridName() + '1') == STOPPED;
        assert G.state(getTestGridName()) == STOPPED;
    }

    /**
     * @throws Exception If failed.
     */
    public void testStartMultipleInstanceSpi() throws Exception {
        GridConfiguration cfg1 = getConfiguration();
        GridConfiguration cfg2 = getConfiguration();
        GridConfiguration cfg3 = getConfiguration();

        cfg1.setCollisionSpi(new TestMultipleInstancesCollisionSpi());
        cfg2.setCollisionSpi(new TestMultipleInstancesCollisionSpi());
        cfg3.setCollisionSpi(new TestMultipleInstancesCollisionSpi());

        cfg2.setGridName(getTestGridName() + '1');

        G.start(cfg2);

        G.start(cfg1);

        cfg3.setGridName(getTestGridName() + '2');

        G.start(cfg3);

        assert G.state(cfg1.getGridName()) == STARTED;
        assert G.state(getTestGridName() + '1') == STARTED;
        assert G.state(getTestGridName() + '2') == STARTED;

        G.stop(getTestGridName() + '2', false);
        G.stop(cfg1.getGridName(), false);
        G.stop(getTestGridName() + '1', false);

        assert G.state(cfg1.getGridName()) == STOPPED;
        assert G.state(getTestGridName() + '1') == STOPPED;
        assert G.state(getTestGridName() + '2') == STOPPED;
    }

    /**
     * @throws Exception If failed.
     */
    @Override protected void afterTest() throws Exception {
        G.stopAll(false);
    }

    /** */
    @GridSpiMultipleInstancesSupport(true)
    private static class TestMultipleInstancesCollisionSpi extends GridSpiAdapter implements GridCollisionSpi {
        /** Grid logger. */
        @GridLoggerResource private GridLogger log;

        /** {@inheritDoc} */
        @Override public void onCollision(GridCollisionContext ctx) {
            // No-op.
        }

        /** {@inheritDoc} */
        @Override public void spiStart(String gridName) throws GridSpiException {
            // Start SPI start stopwatch.
            startStopwatch();

            // Ack start.
            if (log.isInfoEnabled())
                log.info(startInfo());
        }

        /** {@inheritDoc} */
        @Override public void spiStop() throws GridSpiException {
            // Ack stop.
            if (log.isInfoEnabled())
                log.info(stopInfo());
        }

        /** {@inheritDoc} */
        @Override public void setExternalCollisionListener(GridCollisionExternalListener lsnr) {
            // No-op.
        }
    }

    /**
     * DO NOT CHANGE MULTIPLE INSTANCES SUPPORT.
     * This test might be working on distributed environment.
     */
    @GridSpiMultipleInstancesSupport(true)
    private static class TestSingleInstancesCollisionSpi extends GridSpiAdapter implements GridCollisionSpi {
        /** Grid logger. */
        @GridLoggerResource private GridLogger log;

        /** {@inheritDoc} */
        @Override public void onCollision(GridCollisionContext ctx) {
            // No-op.
        }

        /** {@inheritDoc} */
        @Override public void spiStart(String gridName) throws GridSpiException {
            // Start SPI start stopwatch.
            startStopwatch();

            // Ack start.
            if (log.isInfoEnabled())
                log.info(startInfo());
        }

        /** {@inheritDoc} */
        @Override public void spiStop() throws GridSpiException {
            // Ack stop.
            if (log.isInfoEnabled())
                log.info(stopInfo());
        }

        /** {@inheritDoc} */
        @Override public void setExternalCollisionListener(GridCollisionExternalListener lsnr) {
            // No-op.
        }
    }

    /**
     * Lifecycle bean for testing.
     */
    private static class TestLifecycleBean implements GridLifecycleBean {
        /** Grid logger. */
        @GridLoggerResource
        private GridLogger log;

        /** Marshaller. */
        @GridMarshallerResource
        private GridMarshaller marshaller;

        /** Executor. */
        @GridExecutorServiceResource
        private Executor exec;

        /** MBean server. */
        @GridMBeanServerResource
        private MBeanServer mbeanSrv;

        /** Grid home. */
        @GridHomeResource
        private String gridHome;

        /** Grid name. */
        @GridNameResource
        private String gridName;

        /** Local node ID. */
        @GridLocalNodeIdResource
        private UUID nodeId;

        /** */
        @GridSpringApplicationContextResource
        private ApplicationContext appCtx;

        /** */
        @GridInstanceResource
        private Grid grid;

        /** Lifecycle events. */
        private final List<GridLifecycleEventType> evts = new ArrayList<>();

        /** Grid names. */
        private final List<String> gridNames = new ArrayList<>();

        /** */
        private final AtomicReference<Throwable> err = new AtomicReference<>();

        /** {@inheritDoc} */
        @Override public void onLifecycleEvent(GridLifecycleEventType evt) {
            evts.add(evt);

            gridNames.add(grid.name());

            try {
                checkState(grid.name(),
                    evt == GridLifecycleEventType.AFTER_GRID_START || evt == GridLifecycleEventType.BEFORE_GRID_STOP);
            }
            catch (Throwable e) {
                log.error("Lifecycle bean failed state check: " + this, e);

                err.compareAndSet(null, e);
            }
        }

        /**
         * Checks state of the bean.
         *
         * @param gridName Grid name.
         * @param exec Try to execute something on the grid.
         */
        void checkState(String gridName, boolean exec) {
            assert log != null;
            assert marshaller != null;
            assert this.exec != null;
            assert mbeanSrv != null;
            assert gridHome != null;
            assert nodeId != null;
            assert appCtx != null;

            assert F.eq(gridName, this.gridName);

            if (exec)
                // Execute any grid method.
                G.grid(gridName).events().localQuery(F.<GridEvent>alwaysTrue());
        }

        /**
         * Gets ordered list of lifecycle events.
         *
         * @return Ordered list of lifecycle events.
         */
        List<GridLifecycleEventType> getLifecycleEvents() {
            return evts;
        }

        /**
         * Gets ordered list of grid names.
         *
         * @return Ordered list of grid names.
         */
        List<String> getGridNames() {
            return gridNames;
        }

        /**
         *
         */
        void checkErrors() {
            if (err.get() != null)
                fail("Exception has been caught by listener: " + err.get().getMessage());
        }
    }

    /**
     * Gets Spring application context by given path.
     *
     * @param path Spring application context configuration path.
     * @return Spring application context.
     * @throws GridException If given path or xml-configuration at this path is invalid.
     */
    private GenericApplicationContext getSpringContext(String path) throws GridException {
        try {
            GenericApplicationContext ctx = new GenericApplicationContext();

            new XmlBeanDefinitionReader(ctx).loadBeanDefinitions(new UrlResource(U.resolveGridGainUrl(path)));

            ctx.refresh();

            return ctx;
        }
        catch (BeansException e) {
            throw new GridException("Failed to instantiate Spring XML application context: " + e.getMessage(), e);
        }
    }

    /**
     * Gets test Spring application context with single {@link StringBuilder} bean
     * with name "myBean" and value "Test string".
     *
     * @return Spring application context.
     */
    private ApplicationContext getTestApplicationContext() {
        AbstractBeanDefinition def = new GenericBeanDefinition();

        def.setBeanClass(StringBuilder.class);

        ConstructorArgumentValues args = new ConstructorArgumentValues();
        args.addGenericArgumentValue("Test string");

        def.setConstructorArgumentValues(args);

        GenericApplicationContext ctx = new GenericApplicationContext();

        ctx.registerBeanDefinition("myBean", def);

        return ctx;
    }

    /**
     * @throws Exception If failed.
     */
    public void testStopCancel() throws Exception {
        GridConfiguration cfg = new GridConfiguration();

        cfg.setRestEnabled(false);

        Grid grid = G.start(cfg);

        grid.compute().execute(TestTask.class, null);

        G.stop(true);
    }

    /**
     * Test task.
     */
    private static class TestTask extends GridComputeTaskSplitAdapter<Void, Void> {
        /** {@inheritDoc} */
        @Override protected Collection<? extends GridComputeJob> split(int gridSize, Void arg) throws GridException {
            return F.asSet(new TestJob());
        }

        /** {@inheritDoc} */
        @Nullable @Override public Void reduce(List<GridComputeJobResult> results) throws GridException {
            return null;
        }
    }

    /**
     * Test job.
     */
    private static class TestJob extends GridComputeJobAdapter {
        /** {@inheritDoc} */
        @SuppressWarnings("StatementWithEmptyBody")
        @Override public Object execute() throws GridException {
            long start = System.currentTimeMillis();

            while (System.currentTimeMillis() - start < 3000);

            return null;
        }
    }
}
