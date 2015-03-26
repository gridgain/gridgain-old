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
import org.gridgain.grid.events.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.kernal.processors.cache.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.util.typedef.internal.*;

import java.util.*;
import java.util.concurrent.*;

import static org.gridgain.grid.events.GridEventType.*;

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
    public static void checkExplicitLock(Grid grid, int keys) throws Exception {
        GridFuture<?> fut = grid.compute().broadcast(new CacheOperationsClosure(testDuration * 1000, keys));

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
    public static class CacheOperationsClosure implements GridRunnable {
        /** */
        private final long duration;

        /** */
        private final int keyCnt;

        @GridInstanceResource
        private Grid grid;

        /**
         * @param duration Duration.
         * @param keyCnt Key count.
         */
        public CacheOperationsClosure(long duration, int keyCnt) {
            this.duration = duration;
            this.keyCnt = keyCnt;
        }

        /** {@inheritDoc} */
        @Override public void run() {
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
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                    finally {
                        cache.unlock(vals.firstKey());
                    }
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            } while (startTime + duration > System.currentTimeMillis());

            System.out.println("Cache operation closure finished");
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
    }


}
