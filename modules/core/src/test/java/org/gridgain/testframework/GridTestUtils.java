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

package org.gridgain.testframework;

import junit.framework.*;
import org.gridgain.client.ssl.*;
import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.cache.affinity.consistenthash.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.kernal.processors.cache.*;
import org.gridgain.grid.kernal.processors.cache.distributed.dht.*;
import org.gridgain.grid.kernal.processors.cache.distributed.near.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.future.*;
import org.gridgain.grid.util.lang.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.testframework.config.*;
import org.jetbrains.annotations.*;

import javax.net.ssl.*;
import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.net.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Utility class for tests.
 */
@SuppressWarnings({"UnusedCatchParameter"})
public final class GridTestUtils {
    /** Default busy wait sleep interval in milliseconds.  */
    public static final long DFLT_BUSYWAIT_SLEEP_INTERVAL = 200;

    /** */
    private static final Map<Class<? extends Test>, String> addrs = new HashMap<>();

    /** */
    private static final Map<Class<? extends Test>, Integer> mcastPorts = new HashMap<>();

    /** */
    private static final Map<Class<? extends Test>, Integer> discoPorts = new HashMap<>();

    /** */
    private static final Map<Class<? extends Test>, Integer> commPorts = new HashMap<>();

    /** */
    private static int[] addr;

    /** */
    private static final int default_mcast_port = 50000;

    /** */
    private static final int max_mcast_port = 54999;

    /** */
    private static final int default_comm_port = 45000;

    /** */
    private static final int max_comm_port = 49999;

    /** */
    private static final int default_disco_port = 55000;

    /** */
    private static final int max_disco_port = 59999;

    /** */
    private static int mcastPort = default_mcast_port;

    /** */
    private static int discoPort = default_disco_port;

    /** */
    private static int commPort = default_comm_port;

    /** */
    private static final GridBusyLock busyLock = new GridBusyLock();

    /**
     * Ensure singleton.
     */
    private GridTestUtils() {
        // No-op.
    }

    /**
     * Checks whether callable throws expected exception or not.
     *
     * @param log Logger (optional).
     * @param call Callable.
     * @param cls Exception class.
     * @param msg Exception message (optional). If provided exception message
     *      and this message should be equal.
     * @return Thrown throwable.
     */
    @Nullable public static Throwable assertThrows(@Nullable GridLogger log, Callable<?> call,
        Class<? extends Throwable> cls, @Nullable String msg) {
        assert call != null;
        assert cls != null;

        try {
            call.call();
        }
        catch (Throwable e) {
            if (cls != e.getClass()) {
                U.error(log, "Unexpected exception.", e);

                fail("Exception class is not as expected [expected=" + cls + ", actual=" + e.getClass() + ']', e);
            }

            if (msg != null && (e.getMessage() == null || !e.getMessage().startsWith(msg))) {
                U.error(log, "Unexpected exception message.", e);

                fail("Exception message is not as expected [expected=" + msg + ", actual=" + e.getMessage() + ']', e);
            }

            if (log != null) {
                if (log.isInfoEnabled())
                    log.info("Caught expected exception: " + e.getMessage());
            }
            else
                X.println("Caught expected exception: " + e.getMessage());

            return e;
        }

        throw new AssertionError("Exception has not been thrown.");
    }

    /**
     * Checks whether callable throws expected exception or its child or not.
     *
     * @param log Logger (optional).
     * @param call Callable.
     * @param cls Exception class.
     * @param msg Exception message (optional). If provided exception message
     *      and this message should be equal.
     * @return Thrown throwable.
     */
    @Nullable public static Throwable assertThrowsInherited(@Nullable GridLogger log, Callable<?> call,
        Class<? extends Throwable> cls, @Nullable String msg) {
        assert call != null;
        assert cls != null;

        try {
            call.call();
        }
        catch (Throwable e) {
            if (!cls.isAssignableFrom(e.getClass()))
                fail("Exception class is not as expected [expected=" + cls + ", actual=" + e.getClass() + ']', e);

            if (msg != null && (e.getMessage() == null || !e.getMessage().startsWith(msg)))
                fail("Exception message is not as expected [expected=" + msg + ", actual=" + e.getMessage() + ']', e);

            if (log != null) {
                if (log.isDebugEnabled())
                    log.debug("Caught expected exception: " + e.getMessage());
            }
            else
                X.println("Caught expected exception: " + e.getMessage());

            return e;
        }

        throw new AssertionError("Exception has not been thrown.");
    }

    /**
     * Checks whether callable throws exception, which is itself of a specified
     * class, or has a cause of the specified class.
     *
     * @param call Callable.
     * @param cls Expected class.
     * @return Thrown throwable.
     */
    @Nullable public static Throwable assertThrowsWithCause(Callable<?> call, Class<? extends Throwable> cls) {
        assert call != null;
        assert cls != null;

        try {
            call.call();
        }
        catch (Throwable e) {
            if (!X.hasCause(e, cls))
                fail("Exception is neither of a specified class, nor has a cause of the specified class: " + cls, e);

            return e;
        }

        throw new AssertionError("Exception has not been thrown.");
    }

    /**
     * Throw assertion error with specified error message and initialized cause.
     *
     * @param msg Error message.
     * @param cause Error cause.
     * @return Assertion error.
     */
    private static AssertionError fail(String msg, @Nullable Throwable cause) {
        AssertionError e = new AssertionError(msg);

        if (cause != null)
            e.initCause(cause);

        throw e;
    }

    /**
     * Checks whether object's method call throws expected exception or not.
     *
     * @param log Logger (optional).
     * @param cls Exception class.
     * @param msg Exception message (optional). If provided exception message
     *      and this message should be equal.
     * @param obj Object to invoke method for.
     * @param mtd Object's method to invoke.
     * @param params Method parameters.
     * @return Thrown throwable.
     */
    @Nullable public static Throwable assertThrows(@Nullable GridLogger log, Class<? extends Throwable> cls,
        @Nullable String msg, final Object obj, final String mtd, final Object... params) {
        return assertThrows(log, new Callable() {
            @Override public Object call() throws Exception {
                return invoke(obj, mtd, params);
            }
        }, cls, msg);
    }

    /**
     * Asserts that each element in iterable has one-to-one correspondence with a
     * predicate from list.
     *
     * @param it Input iterable of elements.
     * @param ps Array of predicates (by number of elements in iterable).
     */
    @SuppressWarnings("ConstantConditions")
    public static <T> void assertOneToOne(Iterable<T> it, GridPredicate<T> ... ps) {
        Collection<GridPredicate<T>> ps0 = new ArrayList<>(Arrays.asList(ps));
        Collection<T2<GridPredicate<T>, T>> passed = new ArrayList<>();

        for (T elem : it) {
            for (T2<GridPredicate<T>, T> p : passed) {
                if (p.get1().apply(elem))
                    throw new AssertionError("Two elements match one predicate [elem1=" + p.get2() +
                        ", elem2=" + elem + ", pred=" + p.get1() + ']');
            }

            GridPredicate<T> matched = null;

            for (GridPredicate<T> p : ps0) {
                if (p.apply(elem)) {
                    if (matched != null)
                        throw new AssertionError("Element matches more than one predicate [elem=" + elem +
                            ", pred1=" + p + ", pred2=" + matched + ']');

                    matched = p;
                }
            }

            if (matched == null) // None matched.
                throw new AssertionError("The element does not match [elem=" + elem +
                    ", numRemainingPreds=" + ps0.size() + ']');

            ps0.remove(matched);
            passed.add(new T2<>(matched, elem));
        }
    }

    /**
     * Every invocation of this method will never return a
     * repeating multicast port for a different test case.
     *
     * @param cls Class.
     * @return Next multicast port.
     */
    public static synchronized int getNextMulticastPort(Class<? extends Test> cls) {
        Integer portRet = mcastPorts.get(cls);

        if (portRet != null)
            return portRet;

        int startPort = mcastPort;

        while (true) {
            if (mcastPort >= max_mcast_port)
                mcastPort = default_mcast_port;
            else
                mcastPort++;

            if (startPort == mcastPort)
                break;

            portRet = mcastPort;

            MulticastSocket sock = null;

            try {
                sock = new MulticastSocket(portRet);

                break;
            }
            catch (IOException ignored) {
                // No-op.
            }
            finally {
                U.closeQuiet(sock);
            }
        }

        // Cache port to be reused by the same test.
        mcastPorts.put(cls, portRet);

        return portRet;
    }

    /**
     * Every invocation of this method will never return a
     * repeating communication port for a different test case.
     *
     * @param cls Class.
     * @return Next communication port.
     */
    public static synchronized int getNextCommPort(Class<? extends Test> cls) {
        Integer portRet = commPorts.get(cls);

        if (portRet != null)
            return portRet;

        if (commPort >= max_comm_port)
            commPort = default_comm_port;
        else
            // Reserve 10 ports per test.
            commPort += 10;

        portRet = commPort;

        // Cache port to be reused by the same test.
        commPorts.put(cls, portRet);

        return portRet;
    }

    /**
     * Every invocation of this method will never return a
     * repeating discovery port for a different test case.
     *
     * @param cls Class.
     * @return Next discovery port.
     */
    public static synchronized int getNextDiscoPort(Class<? extends Test> cls) {
        Integer portRet = discoPorts.get(cls);

        if (portRet != null)
            return portRet;

        if (discoPort >= max_disco_port)
            discoPort = default_disco_port;
        else
            discoPort += 10;

        portRet = discoPort;

        // Cache port to be reused by the same test.
        discoPorts.put(cls, portRet);

        return portRet;
    }

    /**
     * Every invocation of this method will never return a
     * repeating multicast group for a different test case.
     *
     * @param cls Class.
     * @return Next multicast group.
     */
    public static synchronized String getNextMulticastGroup(Class<? extends Test> cls) {
        String addrStr = addrs.get(cls);

        if (addrStr != null)
            return addrStr;

        // Increment address.
        if (addr[3] == 255) {
            if (addr[2] == 255)
                assert false;
            else {
                addr[2] += 1;

                addr[3] = 1;
            }
        }
        else
            addr[3] += 1;

        // Convert address to string.
        StringBuilder b = new StringBuilder(15);

        for (int i = 0; i < addr.length; i++) {
            b.append(addr[i]);

            if (i < addr.length - 1)
                b.append('.');
        }

        addrStr = b.toString();

        // Cache address to be reused by the same test.
        addrs.put(cls, addrStr);

        return addrStr;
    }

    /**
     * Runs runnable object in specified number of threads.
     *
     * @param run Target runnable.
     * @param threadNum Number of threads.
     * @param threadName Thread name.
     * @return Execution time in milliseconds.
     * @throws Exception Thrown if at least one runnable execution failed.
     */
    public static long runMultiThreaded(Runnable run, int threadNum, String threadName) throws Exception {
        return runMultiThreaded(makeCallable(run, null), threadNum, threadName);
    }

    /**
     * Runs runnable object in specified number of threads.
     *
     * @param run Target runnable.
     * @param threadNum Number of threads.
     * @param threadName Thread name.
     * @return Future for the run. Future returns execution time in milliseconds.
     */
    public static GridFuture<Long> runMultiThreadedAsync(Runnable run, int threadNum, String threadName) {
        return runMultiThreadedAsync(makeCallable(run, null), threadNum, threadName);
    }

    /**
     * Runs callable object in specified number of threads.
     *
     * @param call Callable.
     * @param threadNum Number of threads.
     * @param threadName Thread names.
     * @return Execution time in milliseconds.
     * @throws Exception If failed.
     */
    public static long runMultiThreaded(Callable<?> call, int threadNum, String threadName) throws Exception {
        List<Callable<?>> calls = Collections.<Callable<?>>nCopies(threadNum, call);

        return runMultiThreaded(calls, threadName);
    }

    /**
     * Runs callable object in specified number of threads.
     *
     * @param call Callable.
     * @param threadNum Number of threads.
     * @param threadName Thread names.
     * @return Future for the run. Future returns execution time in milliseconds.
     */
    @SuppressWarnings("ExternalizableWithoutPublicNoArgConstructor")
    public static GridFuture<Long> runMultiThreadedAsync(Callable<?> call, int threadNum, final String threadName) {
        final List<Callable<?>> calls = Collections.<Callable<?>>nCopies(threadNum, call);
        final GridTestSafeThreadFactory threadFactory = new GridTestSafeThreadFactory(threadName);

        // Future that supports cancel() operation.
        GridFutureAdapter<Long> cancelFut = new GridFutureAdapter<Long>() {
            @Override public boolean cancel() {
                if (onCancelled()) {
                    threadFactory.interruptAllThreads();

                    onCancelled();

                    return true;
                }

                return false;
            }
        };

        // Async execution future (doesn't support cancel()).
        GridFuture<Long> runFut = runAsync(new Callable<Long>() {
            @Override public Long call() throws Exception {
                return runMultiThreaded(calls, threadFactory);
            }
        });

        // Compound future, that adds cancel() support to execution future.
        GridCompoundFuture<Long, Long> compFut = new GridCompoundFuture<>();

        compFut.addAll(cancelFut, runFut);
        compFut.reducer(F.sumLongReducer());
        compFut.markInitialized();

        cancelFut.onDone();

        return compFut;
    }

    /**
     * Runs callable tasks each in separate threads.
     *
     * @param calls Callable tasks.
     * @param threadName Thread name.
     * @return Execution time in milliseconds.
     * @throws Exception If failed.
     */
    public static long runMultiThreaded(Iterable<Callable<?>> calls, String threadName) throws Exception {
        return runMultiThreaded(calls, new GridTestSafeThreadFactory(threadName));
    }

    /**
     * Runs callable tasks each in separate threads.
     *
     * @param calls Callable tasks.
     * @param threadFactory Thread factory.
     * @return Execution time in milliseconds.
     * @throws Exception If failed.
     */
    public static long runMultiThreaded(Iterable<Callable<?>> calls, GridTestSafeThreadFactory threadFactory)
        throws Exception {
        if (!busyLock.enterBusy())
            throw new IllegalStateException("Failed to start new threads (test is being stopped).");

        Collection<Thread> threads = new ArrayList<>();
        long time;

        try {
            for (Callable<?> call : calls)
                threads.add(threadFactory.newThread(call));

            time = System.currentTimeMillis();

            for (Thread t : threads)
                t.start();
        }
        finally {
            busyLock.leaveBusy();
        }

        // Wait threads finish their job.
        for (Thread t : threads)
            t.join();

        time = System.currentTimeMillis() - time;

        // Validate errors happens
        threadFactory.checkError();

        return time;
    }

    /**
     * Runs callable task asyncronously.
     *
     * @param task Callable.
     * @return Future with task result.
     */
    @SuppressWarnings("ExternalizableWithoutPublicNoArgConstructor")
    public static <T> GridFuture<T> runAsync(final Callable<T> task) {
        if (!busyLock.enterBusy())
            throw new IllegalStateException("Failed to start new threads (test is being stopped).");

        try {
            final GridTestSafeThreadFactory thrFactory = new GridTestSafeThreadFactory("async-runner");

            final GridFutureAdapter<T> fut = new GridFutureAdapter<T>() {
                @Override public boolean cancel() throws GridException {
                    super.cancel();

                    thrFactory.interruptAllThreads();

                    onCancelled();

                    return true;
                }
            };

            thrFactory.newThread(new Runnable() {
                @Override public void run() {
                    try {
                        // Execute task.
                        T res = task.call();

                        fut.onDone(res);
                    }
                    catch (Throwable e) {
                        fut.onDone(e);
                    }
                }
            }).start();

            return fut;
        }
        finally {
            busyLock.leaveBusy();
        }
    }

    /**
     * Interrupts and waits for termination of all the threads started
     * so far by current test.
     *
     * @param log Logger.
     */
    public static void stopThreads(GridLogger log) {
        busyLock.block();

        try {
            GridTestSafeThreadFactory.stopAllThreads(log);
        }
        finally {
            busyLock.unblock();
        }
    }

    /**
     * @return GridGain home.
     * @throws Exception If failed.
     */
    @SuppressWarnings({"ProhibitedExceptionThrown"})
    public static String getGridGainHome() throws Exception {
        String ggHome = System.getProperty("GRIDGAIN_HOME");

        if (ggHome == null)
            ggHome = System.getenv("GRIDGAIN_HOME");

        if (ggHome == null)
            throw new Exception("GRIDGAIN_HOME parameter must be set either as system or environment variable.");

        File dir = new File(ggHome);

        if (!dir.exists())
            throw new Exception("Gridgain home does not exist [girdgain-home=" + dir.getAbsolutePath() + ']');

        if (!dir.isDirectory())
            throw new Exception("Gridgain home is not a directory [gridgain-home=" + dir.getAbsolutePath() + ']');

        return ggHome;
    }

    /**
     * @param <T> Type.
     * @param cls Class.
     * @param annCls Annotation class.
     * @return Annotation.
     */
    @Nullable public static <T extends Annotation> T getAnnotation(Class<?> cls, Class<T> annCls) {
        for (Class<?> cls0 = cls; cls0 != null; cls0 = cls0.getSuperclass()) {
            T ann = cls0.getAnnotation(annCls);

            if (ann != null)
                return ann;
        }

        return null;
    }

    /**
     * Initializes address.
     */
    static {
        InetAddress locHost = null;

        try {
            locHost = U.getLocalHost();
        }
        catch (IOException e) {
            assert false : "Unable to get local address. This leads to the same multicast addresses " +
                "in the local network.";
        }

        if (locHost != null) {
            int thirdByte = locHost.getAddress()[3];

            if (thirdByte < 0)
                thirdByte += 256;

            // To get different addresses for different machines.
            addr = new int[] {229, thirdByte, 1, 1};
        }
        else
            addr = new int[] {229, 1, 1, 1};
    }

    /**
     * @param path Path.
     * @param startFilter Start filter.
     * @param endFilter End filter.
     * @return List of JARs that corresponds to the filters.
     * @throws IOException If failed.
     */
    private static Collection<String> getFiles(String path, @Nullable final String startFilter,
        @Nullable final String endFilter) throws IOException {
        Collection<String> res = new ArrayList<>();

        File file = new File(path);

        assert file.isDirectory();

        File[] jars = file.listFiles(new FilenameFilter() {
            /**
             * @see FilenameFilter#accept(File, String)
             */
            @SuppressWarnings({"UnnecessaryJavaDocLink"})
            @Override public boolean accept(File dir, String name) {
                // Exclude spring.jar because it tries to load META-INF/spring-handlers.xml from
                // all available JARs and create instances of classes from there for example.
                // Exclude logging as it is used by spring and casted to Log interface.
                // Exclude log4j because of the design - 1 per VM.
                if (name.startsWith("spring") || name.startsWith("log4j") ||
                    name.startsWith("commons-logging") || name.startsWith("junit") ||
                    name.startsWith("gridgain-tests"))
                    return false;

                boolean ret = true;

                if (startFilter != null)
                    ret = name.startsWith(startFilter);

                if (ret && endFilter != null)
                    ret = name.endsWith(endFilter);

                return ret;
            }
        });

        for (File jar : jars)
            res.add(jar.getCanonicalPath());

        return res;
    }

    /**
     * Silent stop grid.
     * Method doesn't throw any exception.
     *
     * @param grid Grid to stop.
     * @param log Logger.
     */
    @SuppressWarnings({"CatchGenericClass"})
    public static void close(Grid grid, GridLogger log) {
        if (grid != null)
            try {
                G.stop(grid.name(), false);
            }
            catch (Throwable e) {
                U.error(log, "Failed to stop grid: " + grid.name(), e);
            }
    }

    /**
     * Silent stop grid.
     * Method doesn't throw any exception.
     *
     * @param gridName Grid name.
     * @param log Logger.
     */
    @SuppressWarnings({"CatchGenericClass"})
    public static void stopGrid(String gridName, GridLogger log) {
        try {
            G.stop(gridName, false);
        }
        catch (Throwable e) {
            U.error(log, "Failed to stop grid: " + gridName, e);
        }
    }

    /**
     * Gets file representing the path passed in. First the check is made if path is absolute.
     * If not, then the check is made if path is relative to ${GRIDGAIN_HOME}. If both checks fail,
     * then {@code null} is returned, otherwise file representing path is returned.
     * <p>
     * See {@link #getGridGainHome()} for information on how {@code GRIDGAIN_HOME} is retrieved.
     *
     * @param path Path to resolve.
     * @return Resolved path, or {@code null} if file cannot be resolved.
     * @see #getGridGainHome()
     */
    @Nullable public static File resolveGridGainPath(String path) {
        return resolveGridGainPath(null, path);
    }

    /**
     * @param ggHome Optional gridgain home path.
     * @param path Path to resolve.
     * @return Resolved path, or {@code null} if file cannot be resolved.
     */
    @Nullable public static File resolveGridGainPath(@Nullable String ggHome, String path) {
        File file = resolvePath(ggHome, path);

        return file != null ? file : resolvePath(ggHome, "os/" + path);
    }

    /**
     * @param ggHome Optional gridgain home path.
     * @param path Path to resolve.
     * @return Resolved path, or {@code null} if file cannot be resolved.
     */
    @Nullable private static File resolvePath(@Nullable String ggHome, String path) {
        File file = new File(path).getAbsoluteFile();

        if (!file.exists()) {
            String home = ggHome != null ? ggHome : U.getGridGainHome();

            if (home == null)
                return null;

            file = new File(home, path);

            return file.exists() ? file : null;
        }

        return file;
    }

    /**
     * @param cache Cache.
     * @return Cache context.
     */
    public static <K, V> GridCacheContext<K, V> cacheContext(GridCacheProjection<K, V> cache) {
        return ((GridKernal)cache.gridProjection().grid()).<K, V>internalCache().context();
    }

    /**
     * @param cache Cache.
     * @return Near cache.
     */
    public static <K, V> GridNearCacheAdapter<K, V> near(GridCacheProjection<K, V> cache) {
        return cacheContext(cache).near();
    }

    /**
     * @param cache Cache.
     * @return DHT cache.
     */
    public static <K, V> GridDhtCacheAdapter<K, V> dht(GridCacheProjection<K, V> cache) {
        return near(cache).dht();
    }

    /**
     * @param cache Cache.
     * @return Affinity.
     */
    static <K, V> GridCacheConsistentHashAffinityFunction affinity(GridCacheProjection<K, V> cache) {
        return (GridCacheConsistentHashAffinityFunction)cache.cache().configuration().getAffinity();
    }

    /**
     * @param cacheName Cache name.
     * @param backups Number of backups.
     * @param log Logger.
     * @throws Exception If failed.
     */
    @SuppressWarnings("BusyWait")
    public static <K, V> void waitTopologyUpdate(@Nullable String cacheName, int backups, GridLogger log)
        throws Exception {
        for (Grid g : GridGain.allGrids()) {
            GridCache<K, V> cache = ((GridEx)g).cachex(cacheName);

            GridDhtPartitionTopology<?, ?> top = dht(cache).topology();

            while (true) {
                boolean wait = false;

                for (int p = 0; p < affinity(cache).partitions(); p++) {
                    Collection<GridNode> nodes = top.nodes(p, -1);

                    if (nodes.size() > backups + 1) {
                        LT.warn(log, null, "Partition map was not updated yet (will wait) [grid=" + g.name() +
                            ", p=" + p + ", nodes=" + F.nodeIds(nodes) + ']');

                        wait = true;

                        break;
                    }
                }

                if (wait)
                    Thread.sleep(20);
                else
                    break; // While.
            }
        }
    }

    /**
     * Convert runnable tasks with callable.
     *
     * @param run Runnable task to convert into callable one.
     * @param res Callable result.
     * @param <T> The result type of method <tt>call</tt>, always {@code null}.
     * @return Callable task around the specified runnable one.
     */
    public static <T> Callable<T> makeCallable(final Runnable run, @Nullable final T res) {
        return new Callable<T>() {
            @Override public T call() throws Exception {
                run.run();
                return res;
            }
        };
    }

    /**
     * Get object field value via reflection.
     *
     * @param obj Object or class to get field value from.
     * @param cls Class.
     * @param fieldName Field names to get value for.
     * @param <T> Expected field class.
     * @return Field value.
     * @throws GridRuntimeException In case of error.
     */
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public static <T> T getFieldValue(Object obj, Class cls, String fieldName) throws GridRuntimeException {
        assert obj != null;
        assert fieldName != null;

        try {
            // Resolve inner field.
            Field field = cls.getDeclaredField(fieldName);

            synchronized (field) {
                // Backup accessible field state.
                boolean accessible = field.isAccessible();

                try {
                    if (!accessible)
                        field.setAccessible(true);

                    obj = field.get(obj);
                }
                finally {
                    // Recover accessible field state.
                    if (!accessible)
                        field.setAccessible(false);
                }
            }

            return (T)obj;
        }
        catch (NoSuchFieldException | IllegalAccessException e) {
            throw new GridRuntimeException("Failed to get object field [obj=" + obj +
                ", fieldName=" + fieldName + ']', e);
        }
    }

    /**
     * Get object field value via reflection.
     *
     * @param obj Object or class to get field value from.
     * @param fieldNames Field names to get value for: obj->field1->field2->...->fieldN.
     * @param <T> Expected field class.
     * @return Field value.
     * @throws GridRuntimeException In case of error.
     */
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public static <T> T getFieldValue(Object obj, String... fieldNames) throws GridRuntimeException {
        assert obj != null;
        assert fieldNames != null;
        assert fieldNames.length >= 1;

        try {
            for (String fieldName : fieldNames) {
                Class<?> cls = obj instanceof Class ? (Class)obj : obj.getClass();

                try {
                    // Resolve inner field.
                    Field field = cls.getDeclaredField(fieldName);

                    synchronized (field) {
                        // Backup accessible field state.
                        boolean accessible = field.isAccessible();

                        try {
                            if (!accessible)
                                field.setAccessible(true);

                            obj = field.get(obj);
                        }
                        finally {
                            // Recover accessible field state.
                            if (!accessible)
                                field.setAccessible(false);
                        }
                    }
                }
                catch (NoSuchFieldException e) {
                    // Resolve inner class, if not an inner field.
                    Class<?> innerCls = getInnerClass(cls, fieldName);

                    if (innerCls == null)
                        throw new GridRuntimeException("Failed to get object field [obj=" + obj +
                            ", fieldNames=" + Arrays.toString(fieldNames) + ']', e);

                    obj = innerCls;
                }
            }

            return (T)obj;
        }
        catch (IllegalAccessException e) {
            throw new GridRuntimeException("Failed to get object field [obj=" + obj +
                ", fieldNames=" + Arrays.toString(fieldNames) + ']', e);
        }
    }

    /**
     * Get inner class by its name from the enclosing class.
     *
     * @param parentCls Parent class to resolve inner class for.
     * @param innerClsName Name of the inner class.
     * @return Inner class.
     */
    @Nullable public static <T> Class<T> getInnerClass(Class<?> parentCls, String innerClsName) {
        for (Class<?> cls : parentCls.getDeclaredClasses())
            if (innerClsName.equals(cls.getSimpleName()))
                return (Class<T>)cls;

        return null;
    }

    /**
     * Set object field value via reflection.
     *
     * @param obj Object to set field value to.
     * @param fieldName Field name to set value for.
     * @param val New field value.
     * @throws GridRuntimeException In case of error.
     */
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public static void setFieldValue(Object obj, String fieldName, Object val) throws GridRuntimeException {
        assert obj != null;
        assert fieldName != null;

        try {
            Class<?> cls = obj instanceof Class ? (Class)obj : obj.getClass();

            Field field = cls.getDeclaredField(fieldName);

            synchronized (field) {
                // Backup accessible field state.
                boolean accessible = field.isAccessible();

                try {
                    if (!accessible)
                        field.setAccessible(true);

                    field.set(obj, val);
                }
                finally {
                    // Recover accessible field state.
                    if (!accessible)
                        field.setAccessible(false);
                }
            }
        }
        catch (NoSuchFieldException | IllegalAccessException e) {
            throw new GridRuntimeException("Failed to set object field [obj=" + obj + ", field=" + fieldName + ']', e);
        }
    }

    /**
     * Invoke method on an object.
     *
     * @param obj Object to call method on.
     * @param mtd Method to invoke.
     * @param params Parameters of the method.
     * @return Method invocation result.
     * @throws Exception If failed.
     */
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    @Nullable public static <T> T invoke(Object obj, String mtd, Object... params) throws Exception {
        // We cannot resolve method by parameter classes due to some of parameters can be null.
        // Search correct method among all methods collection.
        for (Method m : obj.getClass().getDeclaredMethods()) {
            // Filter methods by name.
            if (!m.getName().equals(mtd))
                continue;

            if (!areCompatible(params, m.getParameterTypes()))
                continue;

            try {
                synchronized (m) {
                    // Backup accessible field state.
                    boolean accessible = m.isAccessible();

                    try {
                        if (!accessible)
                            m.setAccessible(true);

                        return (T)m.invoke(obj, params);
                    }
                    finally {
                        // Recover accessible field state.
                        if (!accessible)
                            m.setAccessible(false);
                    }
                }
            }
            catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to access method" +
                    " [obj=" + obj + ", mtd=" + mtd + ", params=" + Arrays.toString(params) + ']', e);
            }
            catch (InvocationTargetException e) {
                Throwable cause = e.getCause();

                if (cause instanceof Error)
                    throw (Error) cause;

                if (cause instanceof Exception)
                    throw (Exception) cause;

                throw new RuntimeException("Failed to invoke method)" +
                    " [obj=" + obj + ", mtd=" + mtd + ", params=" + Arrays.toString(params) + ']', e);
            }
        }

        throw new RuntimeException("Failed to find method" +
            " [obj=" + obj + ", mtd=" + mtd + ", params=" + Arrays.toString(params) + ']');
    }

    /**
     * Check objects and corresponding types are compatible.
     *
     * @param objs Objects array.
     * @param types Classes array.
     * @return Objects in array can be casted to corresponding types.
     */
    private static boolean areCompatible(Object[] objs, Class[] types) {
        if (objs.length != types.length)
            return false;

        for (int i = 0, size = objs.length; i < size; i++) {
            Object o = objs[i];

            if (o != null && !types[i].isInstance(o))
                return false;
        }

        return true;
    }

    /**
     * Tries few times to perform some assertion. In the worst case
     * {@code assertion} closure will be executed {@code retries} + 1 times and
     * thread will spend approximately {@code retries} * {@code retryInterval} sleeping.
     *
     * @param log Log.
     * @param retries Number of retries.
     * @param retryInterval Interval between retries in milliseconds.
     * @param c Closure with assertion. All {@link AssertionError}s thrown
     *      from this closure will be ignored {@code retries} times.
     * @throws GridInterruptedException If interrupted.
     */
    @SuppressWarnings("ErrorNotRethrown")
    public static void retryAssert(@Nullable GridLogger log, int retries, long retryInterval, GridAbsClosure c)
        throws GridInterruptedException {
        for (int i = 0; i < retries; i++) {
            try {
                c.apply();

                return;
            }
            catch (AssertionError e) {
                U.warn(log, "Check failed (will retry in " + retryInterval + "ms).", e);

                U.sleep(retryInterval);
            }
        }

        // Apply the last time without guarding try.
        c.apply();
    }

    /**
     * Reads entire file into byte array.
     *
     * @param file File to read.
     * @return Content of file in byte array.
     * @throws IOException If failed.
     */
    public static byte[] readFile(File file) throws IOException {
        assert file.exists();
        assert file.length() < Integer.MAX_VALUE;

        byte[] bytes = new byte[(int) file.length()];

        try (FileInputStream fis = new FileInputStream(file)) {
            int readBytesCnt = fis.read(bytes);
            assert readBytesCnt == bytes.length;
        }

        return bytes;
    }

    /**
     * Sleeps and increments an integer.
     * <p>
     * Allows for loops like the following:
     * <pre>{@code
     *     for (int i = 0; i < 20 && !condition; i = sleepAndIncrement(200, i)) {
     *         ...
     *     }
     * }</pre>
     * for busy-waiting limited number of iterations.
     *
     * @param sleepDur Sleep duration in milliseconds.
     * @param i Integer to increment.
     * @return Incremented value.
     * @throws GridInterruptedException If sleep was interrupted.
     */
    public static int sleepAndIncrement(int sleepDur, int i) throws GridInterruptedException {
        U.sleep(sleepDur);

        return i + 1;
    }

    /**
     * Waits for condition, polling in busy wait loop.
     *
     * @param cond Condition to wait for.
     * @param timeout Max time to wait in milliseconds.
     * @return {@code true} if condition was achieved, {@code false} otherwise.
     * @throws GridInterruptedException If interrupted.
     */
    public static boolean waitForCondition(GridAbsPredicate cond, long timeout) throws GridInterruptedException {
        long curTime = U.currentTimeMillis();
        long endTime = curTime + timeout;

        if (endTime < 0)
            endTime = Long.MAX_VALUE;

        while (curTime < endTime) {
            if (cond.apply())
                return true;

            U.sleep(DFLT_BUSYWAIT_SLEEP_INTERVAL);

            curTime = U.currentTimeMillis();
        }

        return false;
    }

    /**
     * Creates an SSL context from test key store with disabled trust manager.
     *
     * @return Initialized context.
     * @throws GeneralSecurityException In case if context could not be initialized.
     * @throws IOException If keystore cannot be accessed.
     */
    public static SSLContext sslContext() throws GeneralSecurityException, IOException {
        SSLContext ctx = SSLContext.getInstance("TLS");

        char[] storePass = GridTestProperties.getProperty("ssl.keystore.password").toCharArray();

        KeyManagerFactory keyMgrFactory = KeyManagerFactory.getInstance("SunX509");

        KeyStore keyStore = KeyStore.getInstance("JKS");

        keyStore.load(new FileInputStream(U.resolveGridGainPath(GridTestProperties.getProperty("ssl.keystore.path"))),
            storePass);

        keyMgrFactory.init(keyStore, storePass);

        ctx.init(keyMgrFactory.getKeyManagers(),
            new TrustManager[]{GridSslBasicContextFactory.getDisabledTrustManager()}, null);

        return ctx;
    }

    /**
     * Creates test-purposed SSL context factory from test key store with disabled trust manager.
     *
     * @return SSL context factory used in test.
     */
    public static GridSslContextFactory sslContextFactory() {
        GridSslBasicContextFactory factory = new GridSslBasicContextFactory();

        factory.setKeyStoreFilePath(GridTestProperties.getProperty("ssl.keystore.path"));
        factory.setKeyStorePassword(GridTestProperties.getProperty("ssl.keystore.password").toCharArray());

        factory.setTrustManagers(GridSslBasicContextFactory.getDisabledTrustManager());

        return factory;
    }

    /**
     * @param o1 Object 1.
     * @param o2 Object 2.
     * @return Equals or not.
     */
    public static boolean deepEquals(@Nullable Object o1, @Nullable Object o2) {
        if (o1 == o2)
            return true;
        else if (o1 == null || o2 == null)
            return false;
        else if (o1.getClass() != o2.getClass())
            return false;
        else {
            Class<?> cls = o1.getClass();

            assert o2.getClass() == cls;

            for (Field f : cls.getDeclaredFields()) {
                f.setAccessible(true);

                Object v1;
                Object v2;

                try {
                    v1 = f.get(o1);
                    v2 = f.get(o2);
                }
                catch (IllegalAccessException e) {
                    throw new AssertionError(e);
                }

                if (!Objects.deepEquals(v1, v2))
                    return false;
            }

            return true;
        }
    }
}
