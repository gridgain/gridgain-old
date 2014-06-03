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

package org.gridgain.grid.kernal.processors.resource;

import org.gridgain.grid.*;
import org.gridgain.grid.compute.*;
import org.gridgain.grid.external.resource.*;
import org.gridgain.testframework.*;
import org.gridgain.testframework.junits.common.*;
import org.springframework.context.support.*;

/**
 *
 */
@GridCommonTest(group = "Resource Self")
public class GridResourceUserExternalTest extends GridCommonAbstractTest {
    /** */
    public GridResourceUserExternalTest() {
        super(/*start grid*/false);
    }

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration c = super.getConfiguration(gridName);

        // Override P2P configuration to exclude Task and Job classes
        c.setPeerClassLoadingLocalClassPathExclude(
            GridUserExternalResourceTask1.class.getName(),
            GridUserExternalResourceTask2.class.getName(),
            GridUserExternalResourceTask1.GridUserExternalResourceJob1.class.getName(),
            GridUserExternalResourceTask2.GridUserExternalResourceJob2.class.getName(),
            GridAbstractUserExternalResource.class.getName(),
            GridUserExternalResource1.class.getName(),
            GridUserExternalResource2.class.getName()
        );

        return c;
    }

    /**
     * @throws Exception If failed.
     */
    @SuppressWarnings("unchecked")
    public void testExternalResources() throws Exception {
        Grid grid1 = null;
        Grid grid2 = null;

        try {
            grid1 = startGrid(1, new GridSpringResourceContextImpl(new GenericApplicationContext()));
            grid2 = startGrid(2, new GridSpringResourceContextImpl(new GenericApplicationContext()));

            GridTestClassLoader tstClsLdr = new GridTestClassLoader(null, getClass().getClassLoader(),
                GridUserExternalResourceTask1.class.getName(),
                GridUserExternalResourceTask2.class.getName(),
                GridUserExternalResourceTask1.GridUserExternalResourceJob1.class.getName(),
                GridUserExternalResourceTask2.GridUserExternalResourceJob2.class.getName(),
                GridAbstractUserExternalResource.class.getName(),
                GridUserExternalResource1.class.getName(),
                GridUserExternalResource2.class.getName());

            Class<? extends GridComputeTask<Object, Object>> taskCls1 =
                (Class<? extends GridComputeTask<Object, Object>>)tstClsLdr.loadClass(
                GridUserExternalResourceTask1.class.getName());

            Class<? extends GridComputeTask<Object, Object>> taskCls2 =
                (Class<? extends GridComputeTask<Object, Object>>)tstClsLdr.loadClass(
                GridUserExternalResourceTask2.class.getName());

            // Execute the same task twice.
            grid1.compute().execute(taskCls1, null).get();
            grid1.compute().execute(taskCls2, null).get();
        }
        finally {
            GridTestUtils.close(grid1, log());
            GridTestUtils.close(grid2, log());
        }
    }
}
