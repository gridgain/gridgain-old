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
import org.gridgain.grid.kernal.executor.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.testframework.junits.common.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Grid distributed executor test.
 */
@GridCommonTest(group = "Thread Tests")
public class GridExecutorServiceTest extends GridCommonAbstractTest {
    /** */
    public GridExecutorServiceTest() {
        super(true);
    }

    /**
     * @throws Exception Thrown in case of test failure.
     */
    public void testExecute() throws Exception {
        Grid grid = G.grid(getTestGridName());

        ExecutorService srvc = createExecutorService(grid);

        srvc.execute(new Runnable() {
            @GridInstanceResource
            private Grid grid;

            @Override public void run() {
                System.out.println("Test message.");

                assert grid != null;
            }
        });

        srvc.execute(new TestRunnable());

        srvc.shutdown();
    }

    /**
     * @throws Exception Thrown in case of test failure.
     */
    public void testSubmit() throws Exception {
        Grid grid = G.grid(getTestGridName());

        ExecutorService srvc = createExecutorService(grid);

        Future<?> fut = srvc.submit(new TestRunnable());

        Object res = fut.get();

        info("Default Runnable result:" + res);

        assert res == null : "Failed to get valid default result for submitted Runnable: " + res;

        String val = "test-value";

        fut = srvc.submit(new TestRunnable(), val);

        res = fut.get();

        info("Defined Runnable result:" + res);

        assert val.equals(res) : "Failed to get valid predefined result for submitted Runnable: " + res;

        fut = srvc.submit(new TestCallable<>(val));

        res = fut.get();

        info("Callable result:" + res);

        assert val.equals(res) : "Failed to get valid result for submitted Callable: " + res;

        srvc.shutdown();
    }

    /**
     * @throws Exception Thrown in case of test failure.
     */
    public void testSubmitWithFutureTimeout() throws Exception {
        Grid grid = G.grid(getTestGridName());

        ExecutorService srvc = createExecutorService(grid);

        Future<Integer> fut = srvc.submit(new TestCallable<>(3000)); // Just sleep for 3 seconds.

        boolean ok = true;

        try {
            fut.get(1, TimeUnit.SECONDS);

            ok = false;
        }
        catch (TimeoutException e) {
            info("Task timeout elapsed: " + e.getMessage());
        }

        assert ok : "Timeout must be thrown.";

        srvc.shutdown();
    }

    /**
     * @throws Exception Thrown in case of test failure.
     */
    @SuppressWarnings("TooBroadScope")
    public void testInvokeAll() throws Exception {
        Grid grid = G.grid(getTestGridName());

        ExecutorService srvc = createExecutorService(grid);

        Collection<Callable<String>> cmds = new ArrayList<>(2);

        String val1 = "test-value-1";
        String val2 = "test-value-2";

        cmds.add(new TestCallable<>(val1));
        cmds.add(new TestCallable<>(val2));

        List<Future<String>> futs = srvc.invokeAll(cmds);

        assert futs != null;
        assert futs.size() == 2;

        String res1 = futs.get(0).get();
        String res2 = futs.get(1).get();

        assert val1.equals(res1) : "Failed to get valid result for first command: " + res1;
        assert val2.equals(res2) : "Failed to get valid result for second command: " + res2;

        srvc.shutdown();
    }

    /**
     * @throws Exception Thrown in case of test failure.
     */
    @SuppressWarnings("TooBroadScope")
    public void testInvokeAllWithTimeout() throws Exception {
        Grid grid = G.grid(getTestGridName());

        ExecutorService srvc = createExecutorService(grid);

        Collection<Callable<Integer>> cmds = new ArrayList<>();

        cmds.add(new TestCallable<>(3000)); // Just sleeps for 3 seconds.
        cmds.add(new TestCallable<>(3000)); // Just sleeps for 3 seconds.

        List<Future<Integer>> fut = srvc.invokeAll(cmds, 1, TimeUnit.SECONDS);

        assert fut != null;
        assert fut.size() == 2;

        boolean ok = true;

        try {
            fut.get(0).get();

            ok = false;
        }
        catch (CancellationException e) {
            info("First timeout task is cancelled: " + e.getMessage());
        }

        assert ok : "First task must be cancelled.";

        try {
            fut.get(1).get();

            ok = false;
        }
        catch (CancellationException e) {
            info("Second timeout task is cancelled: " + e.getMessage());
        }

        assert ok : "Second task must be cancelled.";

        srvc.shutdown();
    }

    /**
     * @throws Exception Thrown in case of test failure.
     */
    @SuppressWarnings("TooBroadScope")
    public void testInvokeAny() throws Exception {
        Grid grid = G.grid(getTestGridName());

        ExecutorService srvc = createExecutorService(grid);

        Collection<Callable<String>> cmds = new ArrayList<>(2);

        String val1 = "test-value-1";
        String val2 = "test-value-2";

        cmds.add(new TestCallable<>(val1));
        cmds.add(new TestCallable<>(val2));

        String res = srvc.invokeAny(cmds);

        info("Result: " + res);

        assert val1.equals(res) : "Failed to get valid result: " + res;

        srvc.shutdown();
    }

    /**
     * @throws Exception Thrown in case of test failure.
     */
    @SuppressWarnings("TooBroadScope")
    public void testInvokeAnyWithTimeout() throws Exception {
        Grid grid = G.grid(getTestGridName());

        ExecutorService srvc = createExecutorService(grid);

        Collection<Callable<Integer>> timeoutCmds = new ArrayList<>(2);

        timeoutCmds.add(new TestCallable<>(5000));
        timeoutCmds.add(new TestCallable<>(5000));

        boolean ok = true;

        try {
            srvc.invokeAny(timeoutCmds, 1, TimeUnit.SECONDS);

            ok = false;
        }
        catch (TimeoutException e) {
            info("Task timeout elapsed: " + e.getMessage());
        }

        assert ok : "Timeout must be thrown.";

        srvc.shutdown();
    }

    /**
     * @param grid Grid instance.
     * @return Thrown in case of test failure.
     */
    private ExecutorService createExecutorService(Grid grid) {
        assert grid != null;

        return new GridExecutorService(grid, log());
    }

    /**
     * @param <T> Type of the {@link Callable} argument.
     */
    private static class TestCallable<T> implements Callable<T>, Serializable {
        /** */
        private T data;

        /** */
        @GridInstanceResource
        private Grid grid;

        /**
         * @param data Data.
         */
        TestCallable(T data) {
            this.data = data;
        }

        /** {@inheritDoc} */
        @Override public T call() throws Exception {
            System.out.println("Test callable message.");

            assert grid != null;

            if (data instanceof Integer)
                Thread.sleep((Integer)data);

            return data;
        }
    }

    /** */
    private static class TestRunnable implements Runnable, Serializable {
        /** */
        @GridInstanceResource
        private Grid grid;

        /** {@inheritDoc} */
        @Override public void run() {
            System.out.println("Test Runnable message.");

            assert grid != null;
        }
    }
}
