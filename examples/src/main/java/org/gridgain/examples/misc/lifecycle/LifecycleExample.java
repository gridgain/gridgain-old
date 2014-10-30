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

package org.gridgain.examples.misc.lifecycle;

import org.gridgain.grid.*;
import org.gridgain.grid.resources.*;

import static org.gridgain.grid.GridLifecycleEventType.*;

/**
 * This example shows how to provide your own {@link GridLifecycleBean} implementation
 * to be able to hook into GridGain lifecycle. The {@link LifecycleExampleBean} bean
 * will output occurred lifecycle events to the console.
 * <p>
 * This example does not require remote nodes to be started.
 */
public final class LifecycleExample {
    /**
     * Executes example.
     *
     * @param args Command line arguments, none required.
     * @throws GridException If example execution failed.
     */
    public static void main(String[] args) throws GridException {
        System.out.println();
        System.out.println(">>> Lifecycle example started.");

        // Create new configuration.
        GridConfiguration cfg = new GridConfiguration();

        LifecycleExampleBean bean = new LifecycleExampleBean();

        // Provide lifecycle bean to configuration.
        cfg.setLifecycleBeans(bean);

        try (Grid g  = GridGain.start(cfg)) {
            // Make sure that lifecycle bean was notified about grid startup.
            assert bean.isStarted();
        }

        // Make sure that lifecycle bean was notified about grid stop.
        assert !bean.isStarted();
    }

    /**
     * Simple {@link GridLifecycleBean} implementation that outputs event type when it is occurred.
     */
    public static class LifecycleExampleBean implements GridLifecycleBean {
        /** Auto-inject grid instance. */
        @GridInstanceResource
        private Grid grid;

        /** Started flag. */
        private boolean isStarted;

        /** {@inheritDoc} */
        @Override public void onLifecycleEvent(GridLifecycleEventType evt) {
            System.out.println();
            System.out.println(">>> Grid lifecycle event occurred: " + evt);
            System.out.println(">>> Grid name: " + grid.name());

            if (evt == AFTER_GRID_START)
                isStarted = true;
            else if (evt == AFTER_GRID_STOP)
                isStarted = false;
        }

        /**
         * @return {@code True} if grid has been started.
         */
        public boolean isStarted() {
            return isStarted;
        }
    }
}
