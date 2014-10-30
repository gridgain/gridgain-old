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

package org.gridgain.grid.spi.deployment.local;

import org.gridgain.grid.*;
import org.gridgain.grid.compute.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.deployment.*;
import org.gridgain.testframework.junits.spi.*;

import java.io.*;
import java.util.*;

/**
 * Local deployment SPI test.
 */
@GridSpiTest(spi = GridLocalDeploymentSpi.class, group = "Deployment SPI")
public class GridLocalDeploymentSpiSelfTest extends GridSpiAbstractTest<GridLocalDeploymentSpi> {
    /** */
    private static Map<ClassLoader, Set<Class<? extends GridComputeTask<?, ?>>>> tasks =
        Collections.synchronizedMap(new HashMap<ClassLoader, Set<Class<? extends GridComputeTask<?, ?>>>>());

    /** {@inheritDoc} */
    @Override protected void afterTestsStopped() throws Exception {
        getSpi().setListener(null);
    }

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        getSpi().setListener(new GridDeploymentListener() {
            @Override public void onUnregistered(ClassLoader ldr) { tasks.remove(ldr); }
        });
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        tasks.clear();
    }

    /**
     * @param taskCls Task class.
     * @throws Exception If failed.
     */
    private void deploy(Class<? extends GridComputeTask<?, ?>> taskCls) throws Exception {
        getSpi().register(taskCls.getClassLoader(), taskCls);

        Set<Class<? extends GridComputeTask<?, ?>>> clss = new HashSet<>(1);

        clss.add(taskCls);

        tasks.put(GridLocalDeploymentSpi.class.getClassLoader(), clss);
    }

    /**
     * @param taskCls Task class.
     */
    private void checkUndeployed(Class<? extends GridComputeTask<?, ?>> taskCls) {
        assert !tasks.containsKey(taskCls.getClassLoader());
    }

    /**
     * @throws Exception If failed.
     */
    @SuppressWarnings({"TooBroadScope"})
    public void testDeploy() throws Exception {
        String taskName = "GridDeploymentTestTask";

        Class<? extends GridComputeTask<?, ?>> task = GridDeploymentTestTask.class;

        deploy(task);

        // Note we use task name instead of class name.
        GridDeploymentResource t1 = getSpi().findResource(taskName);

        assert t1 != null;

        assert t1.getResourceClass().equals(task);
        assert t1.getName().equals(taskName);

        getSpi().unregister(taskName);

        checkUndeployed(task);

        assert getSpi().findResource(taskName) == null;
        assert getSpi().findResource(task.getName()) == null;
    }

    /**
     * @throws Exception If failed.
     */
    @SuppressWarnings({"TooBroadScope"})
    public void testRedeploy() throws Exception {
        String taskName = "GridDeploymentTestTask";

        // Test versioned redeploy.
        Class<? extends GridComputeTask<?, ?>> t1 = GridDeploymentTestTask.class;
        Class<? extends GridComputeTask<?, ?>> t2 = GridDeploymentTestTask1.class;

        deploy(t1);

        try {
            deploy(t2);

            assert false : "Exception must be thrown for registering with the same name.";
        }
        catch (GridSpiException e) {
            // No-op.
        }

        getSpi().unregister("GridDeploymentTestTask");

        checkUndeployed(t1);

        assert getSpi().findResource("GridDeploymentTestTask") == null;

        tasks.clear();

        deploy(t1);

        try {
            deploy(t2);

            assert false : "Exception must be thrown for registering with the same name.";
        }
        catch (GridSpiException e) {
            // No-op.
        }

        getSpi().unregister(t1.getName());

        checkUndeployed(t1);

        assert getSpi().findResource(taskName) == null;
        assert getSpi().findResource(t1.getName()) == null;
    }

    /**
     *
     */
    @SuppressWarnings({"PublicInnerClass", "InnerClassMayBeStatic"})
    @GridComputeTaskName(value="GridDeploymentTestTask")
    public class GridDeploymentTestTask extends GridComputeTaskSplitAdapter<Object, Object> {
        /** {@inheritDoc} */
        @Override protected Collection<? extends GridComputeJob> split(int gridSize, Object arg) throws GridException {
            return null;
        }

        /** {@inheritDoc} */
        @Override public Serializable reduce(List<GridComputeJobResult> results) throws GridException {
            return null;
        }
    }

    /**
     *
     */
    @SuppressWarnings({"PublicInnerClass", "InnerClassMayBeStatic"})
    @GridComputeTaskName(value="GridDeploymentTestTask")
    public class GridDeploymentTestTask1 extends GridComputeTaskSplitAdapter<Object, Object> {
        /** {@inheritDoc} */
        @Override protected Collection<? extends GridComputeJob> split(int gridSize, Object arg) throws GridException {
            return null;
        }

        /** {@inheritDoc} */
        @Override public Serializable reduce(List<GridComputeJobResult> results) throws GridException {
            return null;
        }
    }
}
