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

package org.gridgain.testframework.junits;

import junit.framework.*;
import org.apache.log4j.*;
import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.events.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.kernal.processors.license.*;
import org.gridgain.grid.kernal.processors.resource.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.marshaller.*;
import org.gridgain.grid.marshaller.jdk.*;
import org.gridgain.grid.marshaller.optimized.*;
import org.gridgain.grid.spi.checkpoint.sharedfs.*;
import org.gridgain.grid.spi.communication.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.multicast.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.testframework.*;
import org.gridgain.testframework.config.*;
import org.jetbrains.annotations.*;
import org.springframework.beans.*;
import org.springframework.context.*;
import org.springframework.context.support.*;

import java.lang.reflect.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static org.gridgain.grid.cache.GridCacheAtomicWriteOrderMode.*;
import static org.gridgain.grid.cache.GridCacheAtomicityMode.*;
import static org.gridgain.grid.cache.GridCacheDistributionMode.*;
import static org.gridgain.grid.cache.GridCacheWriteSynchronizationMode.*;

/**
 * Common abstract test for GridGain tests.
 */
@SuppressWarnings({
    "TransientFieldInNonSerializableClass",
    "ProhibitedExceptionDeclared",
    "JUnitTestCaseWithNonTrivialConstructors"
})
public abstract class GridAbstractTest extends TestCase {
    /**************************************************************
     * DO NOT REMOVE TRANSIENT - THIS OBJECT MIGHT BE TRANSFERRED *
     *                  TO ANOTHER NODE.                          *
     **************************************************************/
    /** Null name for execution map. */
    private static final String NULL_NAME = UUID.randomUUID().toString();

    /** */
    private static final long DFLT_TEST_TIMEOUT = 5 * 60 * 1000;

    /** */
    private static final transient Map<Class<?>, TestCounters> tests = new ConcurrentHashMap<>();

    /** */
    private transient boolean startGrid;

    /** */
    protected transient GridLogger log;

    /** */
    private transient ClassLoader clsLdr;

    /** */
    private transient boolean stopGridErr;

    /** Timestamp for tests. */
    private static long ts = System.currentTimeMillis();

    static {
        System.setProperty(GridSystemProperties.GG_ATOMIC_CACHE_DELETE_HISTORY_SIZE, "10000");
    }

    /** */
    protected GridAbstractTest() {
        this(false);

        log = getTestCounters().getTestResources().getLogger().getLogger(getClass());
    }

    /**
     * @param startGrid Start grid flag.
     */
    @SuppressWarnings({"OverriddenMethodCallDuringObjectConstruction"})
    protected GridAbstractTest(boolean startGrid) {
        assert isJunitFrameworkClass() : "GridAbstractTest class cannot be extended directly " +
            "(use GridCommonAbstractTest class instead).";

        // Initialize properties. Logger initialized here.
        GridTestProperties.init();

        log = getTestCounters().getTestResources().getLogger().getLogger(getClass());

        this.startGrid = startGrid;
    }

    /**
     * @param cls Class to create.
     * @return Instance of class.
     * @throws Exception If failed.
     */
    protected <T> T allocateInstance(Class<T> cls) throws Exception {
        return (T)GridUnsafe.unsafe().allocateInstance(cls);
    }

    /**
     * @param cls Class to create.
     * @return Instance of class.
     */
    @Nullable protected <T> T allocateInstance0(Class<T> cls) {
        try {
            return (T)GridUnsafe.unsafe().allocateInstance(cls);
        }
        catch (InstantiationException e) {
            e.printStackTrace();

            return null;
        }
    }

    /**
     * @return Flag to check if class is Junit framework class.
     */
    protected boolean isJunitFrameworkClass() {
        return false;
    }

    /**
     * @return Test resources.
     */
    protected GridTestResources getTestResources() {
        return getTestCounters().getTestResources();
    }

    /**
     * @param msg Message to print.
     */
    protected void info(String msg) {
        if (log().isInfoEnabled())
            log().info(msg);
    }

    /**
     * @param msg Message to print.
     */
    protected void error(String msg) {
        log().error(msg);
    }

    /**
     * @param msg Message to print.
     * @param t Error to print.
     */
    protected void error(String msg, Throwable t) {
        log().error(msg, t);
    }

    /**
     * @return logger.
     */
    protected GridLogger log() {
        return log;
    }

    /**
     * Resets log4j programmatically.
     *
     * @param log4jLevel Level.
     * @param logToFile If {@code true}, then log to file under "work/log" folder.
     * @param cat Category.
     * @param cats Additional categories.
     */
    @SuppressWarnings({"deprecation"})
    protected void resetLog4j(Level log4jLevel, boolean logToFile, String cat, String... cats) {
        for (String c : F.concat(false, cat, F.asList(cats)))
            Logger.getLogger(c).setLevel(log4jLevel);

        if (logToFile) {
            Logger log4j = Logger.getRootLogger();

            log4j.removeAllAppenders();

            // Console appender.
            ConsoleAppender c = new ConsoleAppender();

            c.setName("CONSOLE_ERR");
            c.setTarget("System.err");
            c.setThreshold(Priority.WARN);
            c.setLayout(new PatternLayout("[%d{ABSOLUTE}][%-5p][%t][%c{1}] %m%n"));

            c.activateOptions();

            log4j.addAppender(c);

            // File appender.
            RollingFileAppender file = new RollingFileAppender();

            file.setName("FILE");
            file.setThreshold(log4jLevel);
            file.setFile(home() + "/work/log/gridgain.log");
            file.setAppend(false);
            file.setMaxFileSize("10MB");
            file.setMaxBackupIndex(10);
            file.setLayout(new PatternLayout("[%d{ABSOLUTE}][%-5p][%t][%c{1}] %m%n"));

            file.activateOptions();

            log4j.addAppender(file);
        }
    }

    /**
     * Executes runnable and prints out how long it took.
     *
     * @param name Name of execution.
     * @param r Runnable to execute.
     */
    protected void time(String name, Runnable r) {
        System.gc();

        long start = System.currentTimeMillis();

        r.run();

        long dur = System.currentTimeMillis() - start;

        info(name + " took " + dur + "ms.");
    }

    /**
     * Runs given code in multiple threads and synchronously waits for all threads to complete.
     * If any thread failed, exception will be thrown out of this method.
     *
     * @param r Runnable.
     * @param threadNum Thread number.
     * @throws Exception If failed.
     */
    protected void multithreaded(Runnable r, int threadNum) throws Exception {
        multithreaded(r, threadNum, getTestGridName());
    }

    /**
     * Runs given code in multiple threads and synchronously waits for all
     * threads to complete. If any thread failed, exception will be thrown
     * out of this method.
     *
     * @param r Runnable.
     * @param threadNum Thread number.
     * @param threadName Thread name.
     * @throws Exception If failed.
     */
    protected void multithreaded(Runnable r, int threadNum, String threadName) throws Exception {
        GridTestUtils.runMultiThreaded(r, threadNum, threadName);
    }

    /**
     * Runs given code in multiple threads. Returns future that ends upon
     * threads completion. If any thread failed, exception will be thrown
     * out of this method.
     *
     * @param r Runnable.
     * @param threadNum Thread number.
     * @throws Exception If failed.
     * @return Future.
     */
    protected GridFuture<?> multithreadedAsync(Runnable r, int threadNum) throws Exception {
        return multithreadedAsync(r, threadNum, getTestGridName());
    }

    /**
     * Runs given code in multiple threads. Returns future that ends upon
     * threads completion. If any thread failed, exception will be thrown
     * out of this method.
     *
     * @param r Runnable.
     * @param threadNum Thread number.
     * @param threadName Thread name.
     * @throws Exception If failed.
     * @return Future.
     */
    protected GridFuture<?> multithreadedAsync(Runnable r, int threadNum, String threadName) throws Exception {
        return GridTestUtils.runMultiThreadedAsync(r, threadNum, threadName);
    }

    /**
     * Runs given code in multiple threads and synchronously waits for all threads to complete.
     * If any thread failed, exception will be thrown out of this method.
     *
     * @param c Callable.
     * @param threadNum Thread number.
     * @throws Exception If failed.
     */
    protected void multithreaded(Callable<?> c, int threadNum) throws Exception {
        multithreaded(c, threadNum, getTestGridName());
    }

    /**
     * Runs given code in multiple threads and synchronously waits for all threads to complete.
     * If any thread failed, exception will be thrown out of this method.
     *
     * @param c Callable.
     * @param threadNum Thread number.
     * @param threadName Thread name.
     * @throws Exception If failed.
     */
    protected void multithreaded(Callable<?> c, int threadNum, String threadName) throws Exception {
        GridTestUtils.runMultiThreaded(c, threadNum, threadName);
    }

    /**
     * Runs given code in multiple threads and asynchronously waits for all threads to complete.
     * If any thread failed, exception will be thrown out of this method.
     *
     * @param c Callable.
     * @param threadNum Thread number.
     * @throws Exception If failed.
     * @return Future.
     */
    protected GridFuture<?> multithreadedAsync(Callable<?> c, int threadNum) throws Exception {
        return multithreadedAsync(c, threadNum, getTestGridName());
    }

    /**
     * Runs given code in multiple threads and asynchronously waits for all threads to complete.
     * If any thread failed, exception will be thrown out of this method.
     *
     * @param c Callable.
     * @param threadNum Thread number.
     * @param threadName Thread name.
     * @throws Exception If failed.
     * @return Future.
     */
    protected GridFuture<?> multithreadedAsync(Callable<?> c, int threadNum, String threadName) throws Exception {
        return GridTestUtils.runMultiThreadedAsync(c, threadNum, threadName);
    }

    /**
     * @return Test kernal context.
     */
    protected GridTestKernalContext newContext() {
        GridTestKernalContext ctx = new GridTestKernalContext();

        ctx.config().setGridLogger(log());

        return ctx;
    }

    /**
     * Called before execution of every test method in class.
     *
     * @throws Exception If failed. {@link #afterTest()} will be called in this case.
     */
    protected void beforeTest() throws Exception {
        // No-op.
    }

    /**
     * Called after execution of every test method in class or
     * if {@link #beforeTest()} failed without test method execution.
     *
     * @throws Exception If failed.
     */
    protected void afterTest() throws Exception {
        // No-op.
    }

    /**
     * Called before execution of all test methods in class.
     *
     * @throws Exception If failed. {@link #afterTestsStopped()} will be called in this case.
     */
    protected void beforeTestsStarted() throws Exception {
        // No-op.
    }

    /**
     * Called after execution of all test methods in class or
     * if {@link #beforeTestsStarted()} failed without execution of any test methods.
     *
     * @throws Exception If failed.
     */
    protected void afterTestsStopped() throws Exception {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override protected void setUp() throws Exception {
        stopGridErr = false;

        clsLdr = Thread.currentThread().getContextClassLoader();

        // Change it to the class one.
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

        // Clear log throttle.
        LT.clear();

        // Clear license registry.
        GridLicenseUseRegistry.clear();

        TestCounters cntrs = getTestCounters();

        if (isDebug())
            info("Test counters [numOfTests=" + cntrs.getNumberOfTests() + ", started=" + cntrs.getStarted() +
                ", stopped=" + cntrs.getStopped() + ']');

        if (cntrs.isReset()) {
            info("Resetting test counters.");

            int started = cntrs.getStarted() % cntrs.getNumberOfTests();
            int stopped = cntrs.getStopped() % cntrs.getNumberOfTests();

            cntrs.reset();

            cntrs.setStarted(started);
            cntrs.setStopped(stopped);
        }

        if (isFirstTest()) {
            info(">>> Starting test class: " + getClass().getSimpleName() + " <<<");

            if (startGrid) {
                GridConfiguration cfg = optimize(getConfiguration());

                G.start(cfg);
            }

            try {
                beforeTestsStarted();
            }
            catch (Exception | Error t) {
                t.printStackTrace();

                getTestCounters().setStopped(getTestCounters().getNumberOfTests() - 1);

                try {
                    tearDown();
                }
                catch (Exception e) {
                    log.error("Failed to tear down test after exception was thrown in beforeTestsStarted (will " +
                        "ignore)", e);
                }

                throw t;
            }
        }

        info(">>> Starting test: " + getName() + " <<<");

        try {
            beforeTest();
        }
        catch (Exception | Error t) {
            try {
                tearDown();
            }
            catch (Exception e) {
                log.error("Failed to tear down test after exception was thrown in beforeTest (will ignore)", e);
            }

            throw t;
        }

        ts = System.currentTimeMillis();
    }

    /**
     * @return Started grid.
     * @throws Exception If anything failed.
     */
    protected Grid startGrid() throws Exception {
        return startGrid(getTestGridName());
    }

    /**
     * @param cnt Grid count.
     * @return First started grid.
     * @throws Exception If failed.
     */
    protected final Grid startGrids(int cnt) throws Exception {
        assert cnt > 0;

        Grid grid = null;

        for (int i = 0; i < cnt; i++)
            if (grid == null)
                grid = startGrid(i);
            else
                startGrid(i);

        checkTopology(cnt);

        assert grid != null;

        return grid;
    }

    /**
     * @param cnt Grid count.
     * @return First started grid.
     * @throws Exception If failed.
     */
    protected Grid startGridsMultiThreaded(int cnt) throws Exception {
        if (cnt == 1)
            return startGrids(1);

        Grid grid = startGridsMultiThreaded(0, cnt);

        checkTopology(cnt);

        return grid;
    }

    /**
     * @param init Start grid index.
     * @param cnt Grid count.
     * @return First started grid.
     * @throws Exception If failed.
     */
    protected final Grid startGridsMultiThreaded(int init, int cnt) throws Exception {
        assert init >= 0;
        assert cnt > 0;

        info("Starting grids: " + cnt);

        final AtomicInteger gridIdx = new AtomicInteger(init);

        GridTestUtils.runMultiThreaded(
            new Callable<Object>() {
                @Nullable @Override public Object call() throws Exception {
                    startGrid(gridIdx.getAndIncrement());

                    return null;
                }
            },
            cnt,
            "grid-starter-" + getName()
        );

        assert gridIdx.get() - init == cnt;

        return grid(init);
    }

    /**
     * @param cnt Grid count
     * @throws Exception If an error occurs.
     */
    @SuppressWarnings({"BusyWait"})
    protected void checkTopology(int cnt) throws Exception {
        for (int j = 0; j < 10; j++) {
            boolean topOk = true;

            for (int i = 0; i < cnt; i++) {
                if (cnt != grid(i).nodes().size()) {
                    U.warn(log, "Grid size is incorrect (will re-run check in 1000 ms) " +
                        "[name=" + grid(i).name() + ", size=" + grid(i).nodes().size() + ']');

                    topOk = false;

                    break;
                }
            }

            if (topOk)
                return;
            else
                Thread.sleep(1000);
        }

        throw new Exception("Failed to wait for proper topology.");
    }

    /** */
    protected void stopGrid() {
        stopGrid(getTestGridName());
    }

    /**
     * Starts new grid with given index.
     *
     * @param idx Index of the grid to start.
     * @return Started grid.
     * @throws Exception If anything failed.
     */
    protected Grid startGrid(int idx) throws Exception {
        return startGrid(getTestGridName(idx));
    }

    /**
     * Starts new grid with given index and Spring application context.
     *
     * @param idx Index of the grid to start.
     * @param ctx Spring context.
     * @return Started grid.
     * @throws Exception If anything failed.
     */
    protected Grid startGrid(int idx, GridSpringResourceContext ctx) throws Exception {
        return startGrid(getTestGridName(idx), ctx);
    }

    /**
     * Starts new grid with given name.
     *
     * @param gridName Grid name.
     * @return Started grid.
     * @throws Exception If failed.
     */
    protected Grid startGrid(String gridName) throws Exception {
        return startGrid(gridName, (GridSpringResourceContext)null);
    }

    /**
     * Starts new grid with given name.
     *
     * @param gridName Grid name.
     * @param ctx Spring context.
     * @return Started grid.
     * @throws Exception If failed.
     */
    protected Grid startGrid(String gridName, GridSpringResourceContext ctx) throws Exception {
        return GridGainEx.start(optimize(getConfiguration(gridName)), ctx);
    }

    /**
     * Optimizes configuration to achieve better test performance.
     *
     * @param cfg Configuration.
     * @return Optimized configuration (by modifying passed in one).
     */
    protected GridConfiguration optimize(GridConfiguration cfg) {
        // TODO: GG-4048: propose another way to avoid network overhead in tests.
        if (cfg.getLocalHost() == null) {
            if (cfg.getDiscoverySpi() instanceof GridTcpDiscoverySpi)
                cfg.setLocalHost("127.0.0.1");
            else
                cfg.setLocalHost(getTestResources().getLocalHost());
        }

        // Do not add redundant data if it is not needed.
        if (cfg.getIncludeProperties() == null)
            cfg.setIncludeProperties();

        return cfg;
    }

    /**
     * @param gridName Grid name.
     */
    protected void stopGrid(@Nullable String gridName) {
        stopGrid(gridName, true);
    }

    /**
     * @param gridName Grid name.
     * @param cancel Cancel flag.
     */
    @SuppressWarnings({"deprecation"})
    protected void stopGrid(@Nullable String gridName, boolean cancel) {
        try {
            Grid grid = G.grid(gridName);

            assert grid != null : "GridGain returned null grid for name: " + gridName;

            info(">>> Stopping grid [name=" + grid.name() + ", id=" + grid.localNode().id() + ']');

            G.stop(gridName, cancel);
        }
        catch (IllegalStateException ignored) {
            // Ignore error if grid already stopped.
        }
        catch (Throwable e) {
            error("Failed to stop grid [gridName=" + gridName + ", cancel=" + cancel + ']', e);

            stopGridErr = true;
        }
    }

    /**
     *
     */
    protected void stopAllGrids() {
        stopAllGrids(true);
    }

    /**
     * @param cancel Cancel flag.
     */
    protected void stopAllGrids(boolean cancel) {
        List<Grid> grids = G.allGrids();

        for (Grid g : grids)
            stopGrid(g.name(), cancel);

        assert G.allGrids().isEmpty();
    }

    /**
     * @param grid Grid
     * @param cnt Count
     * @throws GridException If failed.
     */
    @SuppressWarnings({"BusyWait"})
    protected void waitForRemoteNodes(Grid grid, int cnt) throws GridException {
        while (true) {
            Collection<GridNode> nodes = grid.forRemotes().nodes();

            if (nodes != null && nodes.size() >= cnt)
                return;

            try {
                Thread.sleep(100);
            }
            catch (InterruptedException ignored) {
                throw new GridException("Interrupted while waiting for remote nodes [gridName=" + grid.name() +
                    ", count=" + cnt + ']');
            }
        }
    }

    /**
     * @param grids Grids
     * @throws GridException If failed.
     */
    protected void waitForDiscovery(Grid... grids) throws GridException {
        assert grids != null;
        assert grids.length > 1;

        for (Grid grid : grids)
            waitForRemoteNodes(grid, grids.length - 1);
    }

    /**
     * Gets grid for given index.
     *
     * @param idx Index.
     * @return Grid instance.
     */
    protected GridEx grid(int idx) {
        return (GridEx)G.grid(getTestGridName(idx));
    }

    /**
     * Gets grid for given test.
     *
     * @return Grid for given test.
     */
    protected GridEx grid() {
        return (GridEx)G.grid(getTestGridName());
    }

    /**
     * Starts grid using provided grid name and spring config location.
     * <p>
     * Note that grids started this way should be stopped with {@code G.stop(..)} methods.
     *
     * @param gridName Grid name.
     * @param springCfgPath Path to config file.
     * @return Grid Started grid.
     * @throws Exception If failed.
     */
    protected Grid startGrid(String gridName, String springCfgPath) throws Exception {
        return startGrid(gridName, loadConfiguration(springCfgPath));
    }

    /**
     * Starts grid using provided grid name and config.
     * <p>
     * Note that grids started this way should be stopped with {@code G.stop(..)} methods.
     *
     * @param gridName Grid name.
     * @param cfg Config.
     * @return Grid Started grid.
     * @throws Exception If failed.
     */
    protected Grid startGrid(String gridName, GridConfiguration cfg) throws Exception {
        cfg.setGridName(gridName);

        return G.start(cfg);
    }

    /**
     * Loads configuration from the given Spring XML file.
     *
     * @param springCfgPath Path to file.
     * @return Grid configuration.
     * @throws GridException If load failed.
     */
    @SuppressWarnings("deprecation")
    protected GridConfiguration loadConfiguration(String springCfgPath) throws GridException {
        URL cfgLocation = U.resolveGridGainUrl(springCfgPath);

        assert cfgLocation != null;

        ApplicationContext springCtx;

        try {
            springCtx = new FileSystemXmlApplicationContext(cfgLocation.toString());
        }
        catch (BeansException e) {
            throw new GridException("Failed to instantiate Spring XML application context.", e);
        }

        Map cfgMap;

        try {
            // Note: Spring is not generics-friendly.
            cfgMap = springCtx.getBeansOfType(GridConfiguration.class);
        }
        catch (BeansException e) {
            throw new GridException("Failed to instantiate bean [type=" + GridConfiguration.class + ", err=" +
                e.getMessage() + ']', e);
        }

        if (cfgMap == null)
            throw new GridException("Failed to find a single grid factory configuration in: " + springCfgPath);

        if (cfgMap.isEmpty())
            throw new GridException("Can't find grid factory configuration in: " + springCfgPath);
        else if (cfgMap.size() > 1)
            throw new GridException("More than one configuration provided for cache load test: " + cfgMap.values());

        GridConfiguration cfg = (GridConfiguration)cfgMap.values().iterator().next();

        cfg.setNodeId(UUID.randomUUID());

        return cfg;
    }

    /**
     * @param idx Index of the grid to stop.
     */
    protected void stopGrid(int idx) {
        stopGrid(getTestGridName(idx), false);
    }

    /**
     * @param idx Grid index.
     * @param cancel Cancel flag.
     */
    @SuppressWarnings("deprecation")
    protected void stopGrid(int idx, boolean cancel) {
        String gridName = getTestGridName(idx);

        try {
            Grid grid = G.grid(gridName);

            assert grid != null : "GridGain returned null grid for name: " + gridName;

            info(">>> Stopping grid [name=" + grid.name() + ", id=" + grid.localNode().id() + ']');

            G.stop(gridName, cancel);
        }
        catch (IllegalStateException ignored) {
            // Ignore error if grid already stopped.
        }
        catch (Throwable e) {
            error("Failed to stop grid [gridName=" + gridName + ", cancel=" + cancel + ']', e);

            stopGridErr = true;
        }
    }

    /**
     * @param idx Index of the grid to stop.
     */
    protected void stopAndCancelGrid(int idx) {
        stopGrid(getTestGridName(idx), true);
    }

    /**
     * @return Grid test configuration.
     * @throws Exception If failed.
     */
    protected GridConfiguration getConfiguration() throws Exception {
        // Generate unique grid name.
        return getConfiguration(getTestGridName());
    }

    /**
     * This method should be overridden by subclasses to change configuration parameters.
     *
     * @param gridName Grid name.
     * @return Grid configuration used for starting of grid.
     * @throws Exception If failed.
     */
    @SuppressWarnings("deprecation")
    protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = getConfiguration(gridName, getTestResources());

        cfg.setNodeId(null);

        return cfg;
    }

    /**
     * This method should be overridden by subclasses to change configuration parameters.
     *
     * @return Grid configuration used for starting of grid.
     * @param rsrcs Resources.
     * @throws Exception If failed.
     */
    protected GridConfiguration getConfiguration(GridTestResources rsrcs) throws Exception {
        return getConfiguration(getTestGridName(), rsrcs);
    }

    /**
     * @return Generated unique test grid name.
     */
    public String getTestGridName() {
        String[] parts = getClass().getName().split("\\.");

        return parts[parts.length - 2] + '.' + parts[parts.length - 1];
    }

    /**
     * @param idx Index of the grid.
     * @return Indexed grid name.
     */
    public String getTestGridName(int idx) {
        return getTestGridName() + idx;
    }

    /**
     * Parses test grid index from test grid name.
     *
     * @param testGridName Test grid name, returned by {@link #getTestGridName(int)}.
     * @return Test grid index.
     */
    public int getTestGridIndex(String testGridName) {
        return Integer.parseInt(testGridName.substring(getTestGridName().length()));
    }

    /**
     * @return {@code True} if system property -DDEBUG is set.
     */
    public boolean isDebug() {
        return System.getProperty("DEBUG") != null;
    }

    /**
     * @param marshaller Marshaller to get checkpoint path for.
     * @return Path for specific marshaller.
     */
    @SuppressWarnings({"IfMayBeConditional", "deprecation"})
    protected String getDefaultCheckpointPath(GridMarshaller marshaller) {
        if (marshaller instanceof GridJdkMarshaller)
            return GridSharedFsCheckpointSpi.DFLT_DIR_PATH + "/jdk/";
        else
            return GridSharedFsCheckpointSpi.DFLT_DIR_PATH + '/' + marshaller.getClass().getSimpleName() + '/';
    }

    /**
     * @param name Name to mask.
     * @return Masked name.
     */
    private String maskNull(String name) {
        return name == null ? NULL_NAME : name;
    }

    /**
     * @return GridGain home.
     */
    protected String home() {
        return getTestResources().getGridgainHome();
    }

    /**
     * This method should be overridden by subclasses to change configuration parameters.
     *
     * @return Grid configuration used for starting of grid.
     * @param gridName Grid name.
     * @param rsrcs Resources.
     * @throws Exception If failed.
     */
    @SuppressWarnings("deprecation")
    protected GridConfiguration getConfiguration(String gridName, GridTestResources rsrcs) throws Exception {
        GridConfiguration cfg = new GridConfiguration();

        cfg.setGridName(gridName);
        cfg.setGridLogger(rsrcs.getLogger());
        cfg.setMarshaller(rsrcs.getMarshaller());
        cfg.setNodeId(rsrcs.getNodeId());
        cfg.setGridGainHome(rsrcs.getGridgainHome());
        cfg.setMBeanServer(rsrcs.getMBeanServer());
        cfg.setPeerClassLoadingEnabled(true);
        cfg.setMetricsLogFrequency(0);

        GridTcpCommunicationSpi commSpi = new GridTcpCommunicationSpi();

        commSpi.setLocalPort(GridTestUtils.getNextCommPort(getClass()));
        commSpi.setTcpNoDelay(true);

        cfg.setCommunicationSpi(commSpi);

        GridTcpDiscoverySpi discoSpi = new GridTcpDiscoverySpi();

        if (isDebug()) {
            discoSpi.setMaxMissedHeartbeats(Integer.MAX_VALUE);
            cfg.setNetworkTimeout(Long.MAX_VALUE);
        }
        else {
            // Set network timeout to 10 sec to avoid unexpected p2p class loading errors.
            cfg.setNetworkTimeout(10000);

            // Increase max missed heartbeats to avoid unexpected node fails.
            discoSpi.setMaxMissedHeartbeats(30);
        }

        // Set heartbeat interval to 1 second to speed up tests.
        discoSpi.setHeartbeatFrequency(1000);

        String mcastAddr = GridTestUtils.getNextMulticastGroup(getClass());

        if (!F.isEmpty(mcastAddr)) {
            GridTcpDiscoveryMulticastIpFinder ipFinder = new GridTcpDiscoveryMulticastIpFinder();

            ipFinder.setMulticastGroup(mcastAddr);
            ipFinder.setMulticastPort(GridTestUtils.getNextMulticastPort(getClass()));

            discoSpi.setIpFinder(ipFinder);
        }

        cfg.setDiscoverySpi(discoSpi);

        GridSharedFsCheckpointSpi cpSpi = new GridSharedFsCheckpointSpi();

        Collection<String> paths = new ArrayList<>();

        paths.add(getDefaultCheckpointPath(cfg.getMarshaller()));

        cpSpi.setDirectoryPaths(paths);

        cfg.setCheckpointSpi(cpSpi);

        cfg.setRestEnabled(false);

        cfg.setIncludeEventTypes(GridEventType.EVTS_ALL);

        return cfg;
    }

    /**
     * @return New cache configuration with modified defaults.
     */
    public static GridCacheConfiguration defaultCacheConfiguration() {
        GridCacheConfiguration cfg = new GridCacheConfiguration();

        cfg.setStartSize(1024);
        cfg.setQueryIndexEnabled(true);
        cfg.setAtomicWriteOrderMode(PRIMARY);
        cfg.setAtomicityMode(TRANSACTIONAL);
        cfg.setDistributionMode(NEAR_PARTITIONED);
        cfg.setWriteSynchronizationMode(FULL_SYNC);
        cfg.setEvictionPolicy(null);

        return cfg;
    }

    /**
     * Gets external class loader.
     *
     * @return External class loader.
     */
    protected static ClassLoader getExternalClassLoader() {
        String path = GridTestProperties.getProperty("p2p.uri.cls");

        try {
            return new URLClassLoader(new URL[] {new URL(path)}, U.gridClassLoader());
        }
        catch (MalformedURLException e) {
            throw new RuntimeException("Failed to create URL: " + path, e);
        }
    }

    /** {@inheritDoc} */
    @Override protected void tearDown() throws Exception {
        long dur = System.currentTimeMillis() - ts;

        info(">>> Stopping test: " + getName() + " in " + dur + " ms <<<");

        TestCounters cntrs = getTestCounters();

        if (isDebug())
            info("Test counters [numOfTests=" + cntrs.getNumberOfTests() + ", started=" + cntrs.getStarted() +
                ", stopped=" + cntrs.getStopped() + ']');

        try {
            afterTest();
        }
        finally {
            if (isLastTest()) {
                info(">>> Stopping test class: " + getClass().getSimpleName() + " <<<");

                TestCounters counters = getTestCounters();

                // Stop all threads started by runMultithreaded() methods.
                GridTestUtils.stopThreads(log);

                // Safety.
                getTestResources().stopThreads();

                // Set reset flags, so counters will be reset on the next setUp.
                counters.setReset(true);

                afterTestsStopped();

                if (startGrid)
                    G.stop(getTestGridName(), true);

                // Remove counters.
                tests.remove(getClass());

                // Remove resources cached in static, if any.
                GridClassLoaderCache.clear();
                GridOptimizedMarshaller.clearCache();
                GridMarshallerExclusions.clearCache();
                GridEnumCache.clear();
            }

            Thread.currentThread().setContextClassLoader(clsLdr);

            clsLdr = null;
        }
    }

    /**
     * @return First test flag.
     */
    protected boolean isFirstTest() {
        TestCounters cntrs = getTestCounters();

        return cntrs.getStarted() == 1 && cntrs.getStopped() == 0;
    }

    /**
     * @return Last test flag.
     */
    protected boolean isLastTest() {
        TestCounters cntrs = getTestCounters();

        return cntrs.getStopped() == cntrs.getNumberOfTests();
    }

    /**
     * @return Test counters.
     */
    protected synchronized TestCounters getTestCounters() {
        TestCounters tc = tests.get(getClass());

        if (tc == null)
            tests.put(getClass(), tc = new TestCounters());

        return tc;
    }

    /** {@inheritDoc} */
    @SuppressWarnings({"ProhibitedExceptionDeclared"})
    @Override protected void runTest() throws Throwable {
        final AtomicReference<Throwable> ex = new AtomicReference<>();

        Thread runner = new Thread("test-runner") {
            @Override public void run() {
                try {
                    runTestInternal();
                }
                catch (Throwable e) {
                    GridClosure<Throwable, Throwable> hnd = errorHandler();

                    ex.set(hnd != null ? hnd.apply(e) : e);
                }
            }
        };

        runner.start();

        runner.join(isDebug() ? 0 : getTestTimeout());

        if (runner.isAlive()) {
            U.error(log,
                "Test has been timed out and will be interrupted (threads dump will be taken before interruption) [" +
                "test=" + getName() + ", timeout=" + getTestTimeout() + ']');

            // We dump threads to stdout, because we can loose logs in case
            // the build is cancelled on TeamCity.
            U.dumpThreads(null);

            U.dumpThreads(log);

            U.interrupt(runner);

            U.join(runner, log);

            throw new TimeoutException("Test has been timed out [test=" + getName() + ", timeout=" +
                getTestTimeout() + ']' );
        }

        Throwable t = ex.get();

        if (t != null) {
            U.error(log, "Test failed.", t);

            throw t;
        }

        assert !stopGridErr : "Error occurred on grid stop (see log for more details).";
    }

    /**
     * @return Error handler to process all uncaught exceptions of the test run
     *      ({@code null} by default).
     */
    protected GridClosure<Throwable, Throwable> errorHandler() {
        return null;
    }

    /**
     * @throws Throwable If failed.
     */
    @SuppressWarnings({"ProhibitedExceptionDeclared"})
    private void runTestInternal() throws Throwable {
        super.runTest();
    }

    /**
     * @return Test case timeout.
     */
    protected long getTestTimeout() {
        return getDefaultTestTimeout();
    }

    /**
     * @return Default test case timeout.
     */
    private long getDefaultTestTimeout() {
        String timeout = GridTestProperties.getProperty("test.timeout");

        if (timeout != null)
            return Long.parseLong(timeout);

        return DFLT_TEST_TIMEOUT;
    }

    /**
     * Test counters.
     */
    protected class TestCounters {
        /** */
        private int numOfTests = -1;

        /** */
        private int started;

        /** */
        private int stopped;

        /** */
        private boolean reset;

        /** */
        private GridTestResources rsrcs = new GridTestResources();

        /**
         * @return Reset flag.
         */
        public boolean isReset() {
            return reset;
        }

        /**
         * @return Test resources.
         */
        public GridTestResources getTestResources() {
            return rsrcs;
        }

        /**
         * @param reset Reset flag.
         */
        public void setReset(boolean reset) {
            this.reset = reset;
        }

        /** */
        public void reset() {
            numOfTests = -1;
            started = 0;
            stopped = 0;
            reset = false;
        }

        /**
         * @return Started flag.
         */
        public int getStarted() {
            return started;
        }

        /**
         * @param started Started flag.
         */
        public void setStarted(int started) {
            this.started = started;
        }

        /**
         * @return Stopped flag.
         */
        public int getStopped() {
            return stopped;
        }

        /**
         * @param stopped Stopped flag.
         */
        public void setStopped(int stopped) {
            this.stopped = stopped;
        }

        /** */
        public void incrementStarted() {
            if (isDebug())
                info("Incrementing started tests counter.");

            started++;
        }

        /** */
        public void incrementStopped() {
            if (isDebug())
                info("Incrementing stopped tests counter.");

            stopped++;
        }

        /**
         * @return Number of tests
         */
        public int getNumberOfTests() {
            if (numOfTests == -1) {
                int cnt = 0;

                for (Method m : GridAbstractTest.this.getClass().getMethods())
                    if (m.getDeclaringClass().getName().startsWith("org.gridgain")) {
                        if (m.getName().startsWith("test") && Modifier.isPublic(m.getModifiers()))
                            cnt++;
                    }

                numOfTests = cnt;
            }

            countTestCases();

            return numOfTests;
        }
    }
}
