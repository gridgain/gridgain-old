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

package org.gridgain.examples.events;

import org.gridgain.examples.*;
import org.gridgain.grid.*;
import org.gridgain.grid.compute.*;
import org.gridgain.grid.events.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.resources.*;

import java.util.*;

import static org.gridgain.grid.events.GridEventType.*;

/**
 * Demonstrates event consume API that allows to register event listeners on remote nodes.
 * Note that grid events are disabled by default and must be specifically enabled,
 * just like in {@code examples/config/example-compute.xml} file.
 * <p>
 * Remote nodes should always be started with configuration: {@code 'ggstart.sh examples/config/example-compute.xml'}.
 * <p>
 * Alternatively you can run {@link ComputeNodeStartup} in another JVM which will start
 * GridGain node with {@code examples/config/example-compute.xml} configuration.
 */
public class EventsExample {
    /**
     * Executes example.
     *
     * @param args Command line arguments, none required.
     * @throws GridException If example execution failed.
     */
    public static void main(String[] args) throws Exception {
        try (Grid grid = GridGain.start("examples/config/example-compute.xml")) {
            System.out.println();
            System.out.println(">>> Events API example started.");

            // Listen to events happening on local node.
            localListen();

            // Listen to events happening on all grid nodes.
            remoteListen();

            // Wait for a while while callback is notified about remaining puts.
            Thread.sleep(1000);
        }
    }

    /**
     * Listen to events that happen only on local node.
     *
     * @throws GridException If failed.
     */
    private static void localListen() throws Exception {
        System.out.println();
        System.out.println(">>> Local event listener example.");

        Grid g = GridGain.grid();

        GridPredicate<GridTaskEvent> lsnr = new GridPredicate<GridTaskEvent>() {
            @Override public boolean apply(GridTaskEvent evt) {
                System.out.println("Received task event [evt=" + evt.name() + ", taskName=" + evt.taskName() + ']');

                return true; // Return true to continue listening.
            }
        };

        // Register event listener for all local task execution events.
        g.events().localListen(lsnr, EVTS_TASK_EXECUTION);

        // Generate task events.
        g.compute().withName("example-event-task").run(new GridRunnable() {
            @Override public void run() {
                System.out.println("Executing sample job.");
            }
        }).get();

        // Unsubscribe local task event listener.
        g.events().stopLocalListen(lsnr);
    }

    /**
     * Listen to events coming from all grid nodes.
     *
     * @throws GridException If failed.
     */
    private static void remoteListen() throws GridException {
        System.out.println();
        System.out.println(">>> Remote event listener example.");

        // This optional local callback is called for each event notification
        // that passed remote predicate listener.
        GridBiPredicate<UUID, GridTaskEvent> locLsnr = new GridBiPredicate<UUID, GridTaskEvent>() {
            @Override public boolean apply(UUID nodeId, GridTaskEvent evt) {
                // Remote filter only accepts tasks whose name being with "good-task" prefix.
                assert evt.taskName().startsWith("good-task");

                System.out.println("Received task event [evt=" + evt.name() + ", taskName=" + evt.taskName());

                return true; // Return true to continue listening.
            }
        };

        // Remote filter which only accepts tasks whose name begins with "good-task" prefix.
        GridPredicate<GridTaskEvent> rmtLsnr = new GridPredicate<GridTaskEvent>() {
            @Override public boolean apply(GridTaskEvent evt) {
                return evt.taskName().startsWith("good-task");
            }
        };

        Grid g = GridGain.grid();

        // Register event listeners on all nodes to listen for task events.
        GridFuture<?> fut = g.events().remoteListen(locLsnr, rmtLsnr, EVTS_TASK_EXECUTION);

        // Wait until event listeners are subscribed on all nodes.
        fut.get();

        // Generate task events.
        for (int i = 0; i < 10; i++) {
            g.compute().withName(i < 5 ? "good-task-" + i : "bad-task-" + i).run(new GridRunnable() {
                // Auto-inject task session.
                @GridTaskSessionResource
                private GridComputeTaskSession ses;

                @Override public void run() {
                    System.out.println("Executing sample job for task: " + ses.getTaskName());
                }
            }).get();
        }
    }
}
