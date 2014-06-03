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
import org.gridgain.grid.compute.*;
import org.gridgain.grid.resources.*;
import org.gridgain.testframework.junits.common.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.util.*;

/**
 * Tests instantiation of various task types (defined as private inner class, without default constructor, non-public
 * default constructor).
 */
@GridCommonTest(group = "Kernal Self")
public class GridTaskInstantiationSelfTest extends GridCommonAbstractTest {
    /**
     * Constructor.
     */
    public GridTaskInstantiationSelfTest() {
        super(true);
    }

    /**
     * @throws Exception If an error occurs.
     */
    public void testTasksInstantiation() throws Exception {
        grid().compute().execute(PrivateClassTask.class, null).get();

        grid().compute().execute(NonPublicDefaultConstructorTask.class, null).get();

        try {
            grid().compute().execute(NoDefaultConstructorTask.class, null).get();

            assert false : "Exception should have been thrown.";
        }
        catch (Exception e) {
            info("Caught expected exception: " + e);
        }
    }

    /**
     * Test task defined as private inner class.
     */
    private static class PrivateClassTask extends GridComputeTaskAdapter<String, Object> {
        /** */
        @GridLocalNodeIdResource
        private UUID locId;

        /** {@inheritDoc} */
        @Override public Map<? extends GridComputeJob, GridNode> map(List<GridNode> subgrid,
            @Nullable String arg) throws GridException {
            for (GridNode node : subgrid)
                if (node.id().equals(locId))
                    return Collections.singletonMap(new GridComputeJobAdapter() {
                        @Override public Serializable execute() {
                            return null;
                        }
                    }, node);

            throw new GridException("Local node not found.");
        }

        /** {@inheritDoc} */
        @Override public Object reduce(List<GridComputeJobResult> results) {
            return null;
        }
    }

    /**
     * Test task defined with non-public default constructor.
     */
    @SuppressWarnings({"PublicInnerClass"})
    public static final class NonPublicDefaultConstructorTask extends PrivateClassTask {
        /**
         * No-op constructor.
         */
        private NonPublicDefaultConstructorTask() {
            // No-op.
        }
    }

    /**
     * Test task defined without default constructor.
     */
    @SuppressWarnings({"PublicInnerClass"})
    public static final class NoDefaultConstructorTask extends PrivateClassTask {
        /**
         * No-op constructor.
         *
         * @param param Some parameter.
         */
        @SuppressWarnings({"unused"})
        private NoDefaultConstructorTask(Object param) {
            // No-op.
        }
    }
}
