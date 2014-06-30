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

package org.gridgain.grid.gridify;

import org.gridgain.grid.*;
import org.gridgain.grid.compute.*;
import org.gridgain.grid.compute.gridify.*;
import org.gridgain.testframework.*;
import org.gridgain.testframework.junits.common.*;

import java.util.*;
import java.util.concurrent.*;

/**
 * Tries to execute dummy gridified task. It should fail because grid is not started.
 * <p>
 * The main purpose of this test is to check that AOP is properly configured. It should
 * be included in all suites that require AOP.
 */
public class GridBasicAopSelfTest extends GridCommonAbstractTest {
    /**
     * @throws Exception If failed.
     */
    public void testAop() throws Exception {
        GridTestUtils.assertThrows(
            log,
            new Callable<Object>() {
                @Override public Object call() throws Exception {
                    gridify();

                    return null;
                }
            },
            GridException.class,
            "Grid is not locally started: null"
        );
    }

    /**
     * Gridified method.
     */
    @Gridify(taskClass = TestTask.class)
    private void gridify() {
        // No-op
    }

    /**
     * Test task.
     */
    private static class TestTask extends GridifyTaskSplitAdapter<Void> {
        /** {@inheritDoc} */
        @Override protected Collection<? extends GridComputeJob> split(int gridSize,
            GridifyArgument arg) throws GridException {
            return null;
        }

        /** {@inheritDoc} */
        @Override public Void reduce(List<GridComputeJobResult> results) throws GridException {
            return null;
        }
    }
}
