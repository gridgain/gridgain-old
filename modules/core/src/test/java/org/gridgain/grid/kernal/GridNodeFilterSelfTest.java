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
import org.gridgain.testframework.junits.common.*;

import java.util.*;

/**
 * Node filter test.
 */
@GridCommonTest(group = "Kernal Self")
public class GridNodeFilterSelfTest extends GridCommonAbstractTest {
    /** Grid instance. */
    private Grid grid;

    /** Remote instance. */
    private Grid rmtGrid;

    /** */
    public GridNodeFilterSelfTest() {
        super(false);
    }

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        grid = startGrid(1);

        rmtGrid = startGrid(2);
        startGrid(3);
    }

    /** {@inheritDoc} */
    @Override protected void afterTestsStopped() throws Exception {
        stopGrid(1);
        stopGrid(2);
        stopGrid(3);

        grid = null;
        rmtGrid = null;
    }

    /**
     * @throws Exception If failed.
     */
    public void testSynchronousExecute() throws Exception {
        UUID nodeId = grid.localNode().id();

        UUID rmtNodeId = rmtGrid.localNode().id();

        Collection<GridNode> locNodes = grid.forNodeId(nodeId).nodes();

        assert locNodes.size() == 1;
        assert locNodes.iterator().next().id().equals(nodeId);

        Collection<GridNode> rmtNodes = grid.forNodeId(rmtNodeId).nodes();

        assert rmtNodes.size() == 1;
        assert rmtNodes.iterator().next().id().equals(rmtNodeId);
    }
}
