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

package org.gridgain.examples.compute;

import org.gridgain.grid.*;
import org.gridgain.grid.compute.*;
import org.gridgain.grid.resources.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Demonstrates usage of continuous mapper. With continuous mapper
 * it is possible to continue mapping jobs asynchronously even after
 * initial {@link GridComputeTask#map(List, Object)} method completes.
 * <p>
 * String "Hello Continuous Mapper" is passed as an argument for execution
 * of {@link GridContinuousMapperTask}. As an outcome, participating
 * nodes will print out a single word from the passed in string and return
 * number of characters in that word. However, to demonstrate continuous
 * mapping, next word will be mapped to a node only after the result from
 * previous word has been received.
 * <p>
 * Remote nodes should always be started with special configuration file which
 * enables P2P class loading: {@code 'ggstart.{sh|bat} examples/config/example-compute.xml'}.
 * <p>
 * Alternatively you can run {@link ComputeNodeStartup} in another JVM which will start GridGain node
 * with {@code examples/config/example-compute.xml} configuration.
 */
public class ComputeContinuousMapperExample {
    /**
     * Executes example.
     *
     * @param args Command line arguments, none required.
     * @throws GridException If example execution failed.
     */
    public static void main(String[] args) throws GridException {
        System.out.println();
        System.out.println(">>> Compute continuous mapper example started.");

        try (Grid g = GridGain.start("examples/config/example-compute.xml")) {
            GridComputeTaskFuture<Integer> fut = g.compute().execute(
                GridContinuousMapperTask.class, "Hello Continuous Mapper");

            // Wait for task completion.
            int phraseLen = fut.get();

            System.out.println();
            System.out.println(">>> Total number of characters in the phrase is '" + phraseLen + "'.");
        }
    }

    /**
     * Counts number of characters in the given word.
     *
     * @param word Word to count characters in.
     * @return Number of characters in the given word.
     */
    static int charCount(String word) {
        System.out.println();
        System.out.println(">>> Printing '" + word + "' from grid job at time: " + new Date());

        return word.length();
    }

    /**
     * This task demonstrates how continuous mapper is used. The passed in phrase
     * is split into multiple words and next word is sent out for processing only
     * when the result for the previous word was received.
     * <p>
     * Note that annotation {@link GridComputeTaskNoResultCache} is optional and tells GridGain
     * not to accumulate results from individual jobs. In this example we increment
     * total character count directly in {@link #result(GridComputeJobResult, List)} method,
     * and therefore don't need to accumulate them be be processed at reduction step.
     */
    @GridComputeTaskNoResultCache
    private static class GridContinuousMapperTask extends GridComputeTaskAdapter<String, Integer> {
        /** This field will be injected with task continuous mapper. */
        @GridTaskContinuousMapperResource
        private GridComputeTaskContinuousMapper mapper;

        /** Word queue. */
        private final Queue<String> words = new ConcurrentLinkedQueue<>();

        /** Total character count. */
        private final AtomicInteger totalChrCnt = new AtomicInteger(0);

        /** {@inheritDoc} */
        @Override public Map<? extends GridComputeJob, GridNode> map(List<GridNode> grid, String phrase) throws GridException {
            if (phrase == null || phrase.isEmpty())
                throw new GridException("Phrase is empty.");

            // Populate word queue.
            Collections.addAll(words, phrase.split(" "));

            // Sends first word.
            sendWord();

            // Since we have sent at least one job, we are allowed to return
            // 'null' from map method.
            return null;
        }

        /** {@inheritDoc} */
        @Override public GridComputeJobResultPolicy result(GridComputeJobResult res, List<GridComputeJobResult> rcvd) throws GridException {
            // If there is an error, fail-over to another node.
            if (res.getException() != null)
                return super.result(res, rcvd);

            // Add result to total character count.
            totalChrCnt.addAndGet(res.<Integer>getData());

            sendWord();

            // If next word was sent, keep waiting, otherwise work queue is empty and we reduce.
            return GridComputeJobResultPolicy.WAIT;
        }

        /** {@inheritDoc} */
        @Override public Integer reduce(List<GridComputeJobResult> results) throws GridException {
            return totalChrCnt.get();
        }

        /**
         * Sends next queued word to the next node implicitly selected by load balancer.
         *
         * @throws GridException If sending of a word failed.
         */
        private void sendWord() throws GridException {
            // Remove first word from the queue.
            String word = words.poll();

            if (word != null) {
                // Map next word.
                mapper.send(new GridComputeJobAdapter(word) {
                    @Override public Object execute() {
                        String word = argument(0);

                        int cnt = word.length();

                        // Sleep for some time so it will be visually noticeable that
                        // jobs are executed sequentially.
                        try {
                            Thread.sleep(1000);
                        }
                        catch (InterruptedException ignored) {
                            // No-op.
                        }

                        return cnt;
                    }
                });
            }
        }
    }
}
