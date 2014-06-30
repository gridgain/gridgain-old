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
import org.gridgain.grid.events.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.spi.deployment.uri.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.testframework.junits.common.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

import static java.util.concurrent.TimeUnit.*;
import static org.gridgain.grid.events.GridEventType.*;

/**
 * Test to reproduce gg-2852.
 */
public class GridTaskUriDeploymentDeadlockSelfTest extends GridCommonAbstractTest {
    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration c = super.getConfiguration(gridName);

        GridUriDeploymentSpi deploymentSpi = new GridUriDeploymentSpi();

        deploymentSpi.setUriList(
            Arrays.asList(U.resolveGridGainUrl("modules/core/src/test/resources/").toURI().toString()));

        if (gridName.endsWith("2")) {
            // Delay deployment for 2nd grid only.
            Field f = deploymentSpi.getClass().getDeclaredField("delayOnNewOrUpdatedFile");

            f.setAccessible(true);

            f.set(deploymentSpi, true);
        }

        c.setDeploymentSpi(deploymentSpi);

        return c;
    }

    /**
     * @throws Exception If failed.
     */
    public void testDeadlock() throws Exception {
        try {
            Grid g = startGrid(1);

            final CountDownLatch latch = new CountDownLatch(1);

            g.events().localListen(new GridPredicate<GridEvent>() {
                @Override public boolean apply(GridEvent evt) {
                    assert evt.type() == EVT_NODE_JOINED;

                    latch.countDown();

                    return true;
                }
            }, EVT_NODE_JOINED);

            GridFuture<?> f = multithreadedAsync(new Callable<Object>() {
                @Override public Object call() throws Exception {
                    startGrid(2);

                    return null;
                }
            }, 1);

            assert latch.await(5, SECONDS);

            info(">>> Starting task.");

            g.forPredicate(F.equalTo(F.first(g.forRemotes().nodes()))).compute().
                execute("GridGarHelloWorldTask", "HELLOWORLD.MSG").get(60000);

            f.get();
        }
        catch (Exception e) {
            error("Test failed.", e);

            // With former version of GridDeploymentLocalStore test hangs forever.
            // So, we need to forcibly exit.
            // System.exit(1);
        }
        finally {
            stopAllGrids();
        }
    }
}
