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

package org.gridgain.examples.compute.failover;

import org.gridgain.examples.*;
import org.gridgain.grid.*;
import org.gridgain.grid.compute.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.util.lang.*;

import java.util.*;

/**
 * Demonstrates the usage of checkpoints in GridGain.
 * <p>
 * The example tries to compute phrase length. In order to mitigate possible node failures, intermediate
 * result is saved as as checkpoint after each job step.
 * <p>
 * Remote nodes must be started using {@link ComputeFailoverNodeStartup}.
 */
public class ComputeFailoverExample {
    /**
     * Executes example.
     *
     * @param args Command line arguments, none required.
     * @throws GridException If example execution failed.
     */
    public static void main(String[] args) throws GridException {
        try (Grid g = GridGain.start(ComputeFailoverNodeStartup.configuration())) {
            if (!ExamplesUtils.checkMinTopologySize(g, 2))
                return;

            System.out.println();
            System.out.println("Compute failover example started.");

            GridFuture<Integer> f = g.compute().apply(new CheckPointJob(), "Stage1 Stage2");

            // Number of letters.
            int charCnt = f.get();

            System.out.println();
            System.out.println(">>> Finished executing fail-over example with checkpoints.");
            System.out.println(">>> Total number of characters in the phrase is '" + charCnt + "'.");
            System.out.println(">>> You should see exception stack trace from failed job on some node.");
            System.out.println(">>> Failed job will be failed over to another node.");
        }
    }

    @GridComputeTaskSessionFullSupport
    private static final class CheckPointJob implements GridClosure<String, Integer> {
        /** Injected distributed task session. */
        @GridTaskSessionResource
        private GridComputeTaskSession jobSes;

        /** Injected grid logger. */
        @GridLoggerResource
        private GridLogger log;

        /** */
        private GridBiTuple<Integer, Integer> state;

        /** */
        private String phrase;

        /**
         * The job will check the checkpoint with key '{@code fail}' and if
         * it's {@code true} it will throw exception to simulate a failure.
         * Otherwise, it will execute the grid-enabled method.
         */
        @Override public Integer apply(String phrase) {
            System.out.println();
            System.out.println(">>> Executing fail-over example job.");

            this.phrase = phrase;

            List<String> words = Arrays.asList(phrase.split(" "));

            final String cpKey = checkpointKey();

            try {
                GridBiTuple<Integer, Integer> state = jobSes.loadCheckpoint(cpKey);

                int idx = 0;
                int sum = 0;

                if (state != null) {
                    this.state = state;

                    // Last processed word index and total length.
                    idx = state.get1();
                    sum = state.get2();
                }

                for (int i = idx; i < words.size(); i++) {
                    sum += words.get(i).length();

                    this.state = new GridBiTuple<>(i + 1, sum);

                    // Save checkpoint with scope of task execution.
                    // It will be automatically removed when task completes.
                    jobSes.saveCheckpoint(cpKey, this.state);

                    // For example purposes, we fail on purpose after first stage.
                    // This exception will cause job to be failed over to another node.
                    if (i == 0) {
                        System.out.println();
                        System.out.println(">>> Job will be failed over to another node.");

                        throw new GridComputeJobFailoverException("Expected example job exception.");
                    }
                }

                return sum;
            }
            catch (GridException e) {
                throw new GridClosureException(e);
            }
        }

        /**
         * Make reasonably unique checkpoint key.
         *
         * @return Checkpoint key.
         */
        private String checkpointKey() {
            return getClass().getName() + '-' + phrase;
        }
    }
}
