/* @java.file.header */

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.examples.test;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.compute.*;
import org.gridgain.grid.resources.*;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.concurrent.*;

/**
 * Demonstrates event consume API that allows to register event listeners on remote nodes.
 * Note that grid events are disabled by default and must be specifically enabled,
 * just like in {@code examples/config/example-compute.xml} file.
 * <p>
 * Remote nodes should always be started with configuration: {@code 'ggstart.sh examples/config/example-compute.xml'}.
 * <p>
 * Alternatively you can run {@link org.gridgain.examples.ComputeNodeStartup} in another JVM which will start
 * GridGain node with {@code examples/config/example-compute.xml} configuration.
 */
public class TxLockOneKeyExample {

    public static long testDuration = GridSystemProperties.getLong("duration", 60);

    /**
     * Executes example.
     *
     * @param args Command line arguments, none required.
     * @throws org.gridgain.grid.GridException If example execution failed.
     */
    public static void main(String[] args) throws Exception {
        try (Grid grid = GridGain.start("examples/config/example-cache.xml")) {
            System.out.println();
            System.out.println(">>> TxLock example started.");

            checkExplicitLock(grid, 1);
        }
    }

    /**
     * @throws Exception If failed.
     */
    public static void checkExplicitLock(Grid grid, final int keys) throws Exception {
        GridFuture<?> fut = grid.compute().withNoFailover().execute(new GridComputeTaskAdapter<Object, Object>() {
            @Nullable @Override public Map<? extends GridComputeJob, GridNode> map(List<GridNode> subgrid,
                @Nullable Object arg) {

                Map<GridComputeJob, GridNode> res = new HashMap<>();

                for (GridNode node : subgrid)
                    res.put(new CacheOperationsJob(testDuration * 1000, keys), node);

                return res;
            }

            @Override public GridComputeJobResultPolicy result(GridComputeJobResult res,
                List<GridComputeJobResult> rcvd) {

                System.out.println("Result [node=" + res.getNode().id() + ", err=" + res.getException() + ", res="
                    + res.getData() + "]");

                return GridComputeJobResultPolicy.WAIT;
            }

            @Nullable @Override public Object reduce(List<GridComputeJobResult> results) {
                return null;
            }
        }, null);

        long timeout = testDuration * 2;

        try {
            fut.get(timeout, TimeUnit.SECONDS);

            System.out.println("Cache operations executed successfully!");
        }
        catch (GridFutureTimeoutException ignored) {
            System.out.println("Cache operations hangs! (waiting more then " + timeout + " seconds)");

            fut.get();
        }
    }

    /** */
    public static class CacheOperationsJob implements GridComputeJob {
        /** */
        private long duration;

        /** */
        private final int keyCnt;

        @GridInstanceResource
        private Grid grid;

        /**
         * @param duration Duration.
         * @param keyCnt Key count.
         */
        public CacheOperationsJob(long duration, int keyCnt) {
            this.duration = duration;
            this.keyCnt = keyCnt;
        }

        /**
         * @param cnt Number of keys to generate.
         * @return Map.
         */
        private TreeMap<Integer, String> generateValues(int cnt) {
            TreeMap<Integer, String> res = new TreeMap<>();

            ThreadLocalRandom rnd = ThreadLocalRandom.current();

            while (res.size() < cnt) {
                int key = rnd.nextInt(0, 100);

                res.put(key, String.valueOf(key));
            }

            return res;
        }

        /** {@inheritDoc} */
        @Override public void cancel() {
            System.out.println("Job canceled.");

            duration = 0;
        }

        /** {@inheritDoc} */
        @Nullable @Override public Object execute() throws GridException {
            System.out.println("Cache operation closure started");

            GridCache<Object, Object> cache = grid.cache("part_cache");

            cache.clearAll();

            long startTime = System.currentTimeMillis();

            do {
                TreeMap<Integer, String> vals = generateValues(keyCnt);

                try {
                    // Explicit lock.
                    cache.lock(vals.firstKey(), 0);

                    try {
                        // Put or remove.
                        if (ThreadLocalRandom.current().nextDouble(1) < 0.65)
                            cache.putAll(vals);
                        else
                            cache.removeAll(vals.keySet());
                    }
                    finally {
                        cache.unlock(vals.firstKey());
                    }
                }
                catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } while (startTime + duration > System.currentTimeMillis());

            System.out.println("Cache operation closure finished");

            return "Ok";
        }
    }
}
