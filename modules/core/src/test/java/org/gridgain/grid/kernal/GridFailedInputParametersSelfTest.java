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
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.testframework.junits.common.*;

import static org.gridgain.grid.events.GridEventType.*;

/**
 * Test for invalid input parameters.
 */
@GridCommonTest(group = "Kernal Self")
public class GridFailedInputParametersSelfTest extends GridCommonAbstractTest {
    /** */
    private static Grid grid;

    /** */
    public GridFailedInputParametersSelfTest() {
        super(/*start grid*/true);
    }

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        grid = G.grid(getTestGridName());
    }

    /**
     * @throws Exception Thrown in case of any errors.
     */
    public void testAddEventLocalListener() throws Exception {
        try {
            grid.events().localListen(null, EVTS_ALL);

            assert false : "Null listener can't be added.";
        }
        catch (NullPointerException ignored) {
            // No-op.
        }
    }

    /**
     * @throws Exception Thrown in case of any errors.
     */
    public void testRemoveEventLocalListener() throws Exception {
        try {
            grid.events().stopLocalListen(null);

            assert false : "Null listener can't be removed.";
        }
        catch (NullPointerException ignored) {
            // No-op.
        }
    }

    /**
     * @throws Exception Thrown in case of any errors.
     */
    public void testAddDiscoveryListener() throws Exception {
        try {
            grid.events().localListen(null, EVTS_ALL);

            assert false : "Null listener can't be added.";
        }
        catch (NullPointerException ignored) {
            // No-op.
        }
    }

    /**
     * @throws Exception Thrown in case of any errors.
     */
    public void testRemoveDiscoveryListener() throws Exception {
        try {
            grid.events().stopLocalListen(null);

            assert false : "Null listener can't be removed.";
        }
        catch (NullPointerException ignored) {
            // No-op.
        }
    }

    /**
     * @throws Exception Thrown in case of any errors.
     */
    public void testGetNode() throws Exception {
        try {
            grid.node(null);

            assert false : "Null nodeId can't be entered.";
        }
        catch (NullPointerException ignored) {
            // No-op.
        }
    }

    /**
     * @throws Exception Thrown in case of any errors.
     */
    public void testPingNode() throws Exception {
        try {
            grid.pingNode(null);

            assert false : "Null nodeId can't be entered.";
        }
        catch (NullPointerException ignored) {
            // No-op.
        }
    }

    /**
     * @throws Exception Thrown in case of any errors.
     */
    public void testDeployTask() throws Exception {
        try {
            grid.compute().localDeployTask(null, null);

            assert false : "Null task can't be entered.";
        }
        catch (NullPointerException ignored) {
            // No-op.
        }

        try {
            grid.compute().localDeployTask(null, null);

            assert false : "Null task can't be entered.";
        }
        catch (NullPointerException ignored) {
            // No-op.
        }

        // Check for exceptions.
        grid.compute().localDeployTask(GridTestTask.class, U.detectClassLoader(GridTestTask.class));
    }
}
