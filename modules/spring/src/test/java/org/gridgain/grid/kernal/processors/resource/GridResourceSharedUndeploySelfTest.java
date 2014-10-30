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
import org.gridgain.grid.logger.*;
import org.gridgain.grid.resources.*;
import org.gridgain.testframework.*;
import org.gridgain.testframework.junits.common.*;
import org.springframework.context.support.*;

import java.io.*;
import java.util.*;

import static org.gridgain.grid.kernal.processors.resource.GridAbstractUserResource.*;
import static org.gridgain.grid.kernal.processors.resource.GridResourceTestUtils.*;

/**
 * Tests resources injection for the tasks executed in shared class loader undeploy mode.
 */
@GridCommonTest(group = "Resource Self")
public class GridResourceSharedUndeploySelfTest extends GridCommonAbstractTest {
    /** */
    private static Object task1Rsrc1;

    /** */
    private static Object task1Rsrc2;

    /** */
    private static Object task1Rsrc3;

    /** */
    private static Object task1Rsrc4;

    /** */
    private static Object task2Rsrc1;

    /** */
    private static Object task2Rsrc2;

    /** */
    private static Object task2Rsrc3;

    /** */
    private static Object task2Rsrc4;

    /** */
    public GridResourceSharedUndeploySelfTest() {
        super(/*start grid*/false);
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        task1Rsrc1 = null;
        task1Rsrc2 = null;
        task1Rsrc3 = null;
        task1Rsrc4 = null;

        task2Rsrc1 = null;
        task2Rsrc2 = null;
        task2Rsrc3 = null;
        task2Rsrc4 = null;

        resetResourceCounters();
    }


    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        cfg.setDeploymentMode(GridDeploymentMode.SHARED);

        return cfg;
    }

    /**
     * @throws Exception If failed.
     */
    public void testSameTaskLocally() throws Exception {
        Grid grid = startGrid(0, new GridSpringResourceContextImpl(new GenericApplicationContext()));

        try {
            // Execute the same task twice.
            // 1 resource created locally
            grid.compute().execute(SharedResourceTask1.class, null).get();
            grid.compute().execute(SharedResourceTask1.class, null).get();

            checkUsageCount(createClss, UserResource1.class, 2);
            checkUsageCount(createClss, UserResource2.class, 2);

            checkUsageCount(deployClss, UserResource1.class, 2);
            checkUsageCount(deployClss, UserResource2.class, 2);
        }
        finally {
            GridTestUtils.close(grid, log());
        }

        checkUsageCount(undeployClss, UserResource1.class, 2);
        checkUsageCount(undeployClss, UserResource2.class, 2);
    }

    /**
     * @throws Exception If failed.
     */
    public void testDifferentTaskLocally() throws Exception {
        Grid grid = startGrid(0, new GridSpringResourceContextImpl(new GenericApplicationContext()));

        try {
            // Execute the tasks with the same class loaders.
            // 1 resource created locally
            grid.compute().execute(SharedResourceTask1.class, null).get();
            grid.compute().execute(SharedResourceTask2.class, null).get();

            checkUsageCount(createClss, UserResource1.class, 2);
            checkUsageCount(createClss, UserResource2.class, 2);

            checkUsageCount(deployClss, UserResource1.class, 2);
            checkUsageCount(deployClss, UserResource2.class, 2);
        }
        finally {
            GridTestUtils.close(grid, log());
        }

        checkUsageCount(undeployClss, UserResource1.class, 2);
        checkUsageCount(undeployClss, UserResource2.class, 2);
    }

    /**
     * @throws Exception If failed.
     */
    public void testDifferentTaskNameLocally() throws Exception {
        Grid grid = startGrid(0, new GridSpringResourceContextImpl(new GenericApplicationContext()));

        // Versions are different - should not share
        // 2 resource created locally
        try {
            grid.compute().execute(SharedResourceTask1Version1.class, null).get();

            try {
                grid.compute().execute(SharedResourceTask1Version2.class, null).get();

                assert false : "SharedResourceTask4 should not be allowed to deploy.";
            }
            catch (GridException e) {
                info("Received expected exception: " + e);
            }
        }
        finally {
            GridTestUtils.close(grid, log());
        }
    }

    /**
     * @throws Exception If failed.
     */
    @SuppressWarnings({"ObjectEquality"})
    public void testDifferentTasks() throws Exception {
        Grid grid1 = null;
        Grid grid2 = null;

        try {
            grid1 = startGrid(1, new GridSpringResourceContextImpl(new GenericApplicationContext()));
            grid2 = startGrid(2, new GridSpringResourceContextImpl(new GenericApplicationContext()));

            // Execute different tasks.
            grid1.compute().execute(SharedResourceTask1.class, null).get();
            grid1.compute().execute(SharedResourceTask2.class, null).get();

            // In ISOLATED_CLASSLOADER mode tasks should have the class
            // loaders because they have the same CL locally and thus the same
            // resources.
            // So 1 resource locally and 1 remotely
            assert task1Rsrc1 == task2Rsrc1;
            assert task1Rsrc2 == task2Rsrc2;
            assert task1Rsrc3 == task2Rsrc3;
            assert task1Rsrc4 == task2Rsrc4;

            checkUsageCount(createClss, UserResource1.class, 4);
            checkUsageCount(createClss, UserResource2.class, 4);

            checkUsageCount(deployClss, UserResource1.class, 4);
            checkUsageCount(deployClss, UserResource2.class, 4);
        }
        finally {
            GridTestUtils.close(grid1, log());
            GridTestUtils.close(grid2, log());
        }

        checkUsageCount(undeployClss, UserResource1.class, 4);
        checkUsageCount(undeployClss, UserResource2.class, 4);
    }

    /**
     * @throws Exception If failed.
     */
    @SuppressWarnings({"ObjectEquality"})
    public void testUndeployedTask() throws Exception {
        Grid grid1 = null;
        Grid grid2 = null;

        try {
            grid1 = startGrid(1, new GridSpringResourceContextImpl(new GenericApplicationContext()));
            grid2 = startGrid(2, new GridSpringResourceContextImpl(new GenericApplicationContext()));

            // Execute tasks.
            grid1.compute().execute(SharedResourceTask1.class, null).get();
            grid1.compute().execute(SharedResourceTask2.class, null).get();

            grid1.compute().undeployTask(SharedResourceTask1.class.getName());

            // Wait until resources get undeployed remotely
            // because undeploy is asynchronous apply.
            Thread.sleep(3000);

            // 1 local and 1 remote resource instances
            checkUsageCount(createClss, UserResource1.class, 4);
            checkUsageCount(deployClss, UserResource1.class, 4);
            checkUsageCount(createClss, UserResource2.class, 4);
            checkUsageCount(deployClss, UserResource2.class, 4);
            checkUsageCount(undeployClss, UserResource1.class, 4);
            checkUsageCount(undeployClss, UserResource2.class, 4);

            grid1.compute().undeployTask(SharedResourceTask2.class.getName());

            // Wait until resources get undeployed remotely
            // because undeploy is asynchronous apply.
            Thread.sleep(3000);

            // We undeployed last task for this class loader and resources.
            // All resources should be undeployed.
            checkUsageCount(undeployClss, UserResource1.class, 4);
            checkUsageCount(undeployClss, UserResource2.class, 4);

            // Execute the same tasks.
            grid1.compute().execute(SharedResourceTask1.class, null).get();
            grid1.compute().execute(SharedResourceTask2.class, null).get();

            // 2 new resources.
            checkUsageCount(createClss, UserResource1.class, 8);
            checkUsageCount(deployClss, UserResource1.class, 8);
            checkUsageCount(createClss, UserResource2.class, 8);
            checkUsageCount(deployClss, UserResource2.class, 8);
        }
        finally {
            GridTestUtils.close(grid1, log());
            GridTestUtils.close(grid2, log());
        }

        checkUsageCount(undeployClss, UserResource1.class, 8);
        checkUsageCount(undeployClss, UserResource2.class, 8);
    }

    /**
     * @throws Exception If failed.
     */
    @SuppressWarnings("unchecked")
    public void testRedeployedTask() throws Exception {
        Grid grid = startGrid(0, new GridSpringResourceContextImpl(new GenericApplicationContext()));

        try {
            // Execute same task with different class loaders. Second execution should redeploy first one.
            grid.compute().execute(SharedResourceTask1.class, null).get();

            checkUsageCount(createClss, UserResource1.class, 2);
            checkUsageCount(createClss, UserResource2.class, 2);

            checkUsageCount(deployClss, UserResource1.class, 2);
            checkUsageCount(deployClss, UserResource2.class, 2);

            // Change class loader of the task. So it's just implicit redeploy.
            ClassLoader tstClsLdr = new GridTestClassLoader(null, getClass().getClassLoader(),
                SharedResourceTask1.class.getName(),
                GridResourceSharedUndeploySelfTest.SharedResourceTask1.GridSharedJob1.class.getName(),
                GridResourceSharedUndeploySelfTest.class.getName());

            Class<? extends GridComputeTask<Object, Object>> taskCls =
                (Class<? extends GridComputeTask<Object, Object>>)tstClsLdr.loadClass(
                    SharedResourceTask1.class.getName());

            grid.compute().execute(taskCls, null).get();

            // Old resources should be undeployed at this point.
            checkUsageCount(undeployClss, UserResource1.class, 2);
            checkUsageCount(undeployClss, UserResource2.class, 2);

            // We should detect redeployment and create new resources.
            checkUsageCount(createClss, UserResource1.class, 4);
            checkUsageCount(createClss, UserResource2.class, 4);

            checkUsageCount(deployClss, UserResource1.class, 4);
            checkUsageCount(deployClss, UserResource2.class, 4);
        }
        finally {
            GridTestUtils.close(grid, log());
        }

        checkUsageCount(undeployClss, UserResource1.class, 4);
        checkUsageCount(undeployClss, UserResource2.class, 4);
    }

    /**
     * @throws Exception If failed.
     */
    public void testSameTaskFromTwoNodesUndeploy() throws Exception {
        Grid grid1 = null;
        Grid grid2 = null;
        Grid grid3 = null;

        try {
            grid1 = startGrid(1, new GridSpringResourceContextImpl(new GenericApplicationContext()));
            grid2 = startGrid(2, new GridSpringResourceContextImpl(new GenericApplicationContext()));
            grid3 = startGrid(3, new GridSpringResourceContextImpl(new GenericApplicationContext()));

            grid1.compute().execute(SharedResourceTask1.class, null).get();
            grid2.compute().execute(SharedResourceTask1.class, null).get();

            checkUsageCount(createClss, UserResource1.class, 6);
            checkUsageCount(deployClss, UserResource1.class, 6);
            checkUsageCount(createClss, UserResource2.class, 6);
            checkUsageCount(deployClss, UserResource2.class, 6);

            checkUsageCount(undeployClss, UserResource1.class, 0);
            checkUsageCount(undeployClss, UserResource2.class, 0);

            grid1.compute().undeployTask(SharedResourceTask1.class.getName());

            // Wait until resources get undeployed remotely
            // because undeploy is asynchronous apply.
            Thread.sleep(3000);

            checkUsageCount(undeployClss, UserResource1.class, 6);
            checkUsageCount(undeployClss, UserResource2.class, 6);

            grid2.compute().undeployTask(SharedResourceTask1.class.getName());

            // Wait until resources get undeployed remotely
            // because undeploy is asynchronous apply.
            Thread.sleep(3000);

            // All Tasks from originating nodes were undeployed. All resources should be cleaned up.
            checkUsageCount(undeployClss, UserResource1.class, 6);
            checkUsageCount(undeployClss, UserResource2.class, 6);
        }
        finally {
            GridTestUtils.close(grid1, log());
            GridTestUtils.close(grid2, log());
            GridTestUtils.close(grid3, log());
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testSameTaskFromTwoNodesLeft() throws Exception {
        Grid grid1 = null;
        Grid grid2 = null;
        Grid grid3 = null;

        try {
            grid1 = startGrid(1, new GridSpringResourceContextImpl(new GenericApplicationContext()));
            grid2 = startGrid(2, new GridSpringResourceContextImpl(new GenericApplicationContext()));
            grid3 = startGrid(3, new GridSpringResourceContextImpl(new GenericApplicationContext()));

            grid1.compute().execute(SharedResourceTask1.class, null).get();
            grid2.compute().execute(SharedResourceTask1.class, null).get();

            checkUsageCount(createClss, UserResource1.class, 6);
            checkUsageCount(deployClss, UserResource1.class, 6);
            checkUsageCount(createClss, UserResource2.class, 6);
            checkUsageCount(deployClss, UserResource2.class, 6);

            checkUsageCount(undeployClss, UserResource1.class, 0);
            checkUsageCount(undeployClss, UserResource2.class, 0);

            GridTestUtils.close(grid1, log());

            // Wait until other nodes get notified
            // this grid1 left.
            Thread.sleep(1000);

            // Undeployment happened only on Grid1.
            checkUsageCount(undeployClss, UserResource1.class, 2);
            checkUsageCount(undeployClss, UserResource2.class, 2);

            GridTestUtils.close(grid2, log());

            // Wait until resources get undeployed remotely
            // because undeploy is asynchronous apply.
            Thread.sleep(1000);

            // Grid1 and Grid2
            checkUsageCount(undeployClss, UserResource1.class, 4);
            checkUsageCount(undeployClss, UserResource2.class, 4);
        }
        finally {
            GridTestUtils.close(grid1, log());
            GridTestUtils.close(grid2, log());
            GridTestUtils.close(grid3, log());
        }
    }

    /** */
    public static class UserResource1 extends GridAbstractUserResource {
        // No-op.
    }

    /** */
    public static class UserResource2 extends GridAbstractUserResource {
        // No-op.
    }

    /** */
    @GridComputeTaskName("SharedResourceTask1")
    public static class SharedResourceTask1Version1 extends SharedResourceTask1 {
        // No-op.
    }

    /** */
    @GridComputeTaskName("SharedResourceTask1")
    public static class SharedResourceTask1Version2 extends SharedResourceTask1 {
        // No-op.
    }

    /** */
    public static class SharedResourceTask1 extends GridComputeTaskSplitAdapter<Object, Object> {
        /** User resource.  */
        @GridUserResource(resourceClass = UserResource1.class)
        private transient GridAbstractUserResource rsrc1;

        /** User resource. */
        @GridUserResource private transient UserResource2 rsrc2;

        /** User resource.  */
        @GridUserResource(resourceClass = UserResource1.class, resourceName = "rsrc3")
        private transient GridAbstractUserResource rsrc3;

        /** User resource. */
        @GridUserResource(resourceName = "rsrc4")
        private transient UserResource2 rsrc4;

        /** */
        @GridLoggerResource private GridLogger log;

        /** {@inheritDoc} */
        @Override protected Collection<GridComputeJobAdapter> split(int gridSize, Object arg) throws GridException {
            assert rsrc1 != null;
            assert rsrc2 != null;
            assert rsrc3 != null;
            assert rsrc4 != null;
            assert log != null;

            log.info("Injected shared resource1 into task: " + rsrc1);
            log.info("Injected shared resource2 into task: " + rsrc2);
            log.info("Injected shared resource3 into task: " + rsrc3);
            log.info("Injected shared resource4 into task: " + rsrc4);
            log.info("Injected log resource into task: " + log);

            task1Rsrc1 = rsrc1;
            task1Rsrc2 = rsrc2;
            task1Rsrc3 = rsrc3;
            task1Rsrc4 = rsrc4;

            Collection<GridComputeJobAdapter> jobs = new ArrayList<>(gridSize);

            for (int i = 0; i < gridSize; i++)
                jobs.add(new GridSharedJob1());

            return jobs;
        }

        /** {@inheritDoc} */
        @Override public Object reduce(List<GridComputeJobResult> results) throws GridException {
            assert rsrc1 != null;
            assert rsrc2 != null;
            assert rsrc3 != null;
            assert rsrc4 != null;
            assert log != null;

            // Nothing to reduce.
            return null;
        }

        /**
         * Job class for the 1st task. To avoid illegal
         * access when loading class with different class loader.
         */
        public final class GridSharedJob1 extends GridComputeJobAdapter {
            /** User resource. */
            @GridUserResource(resourceClass = UserResource1.class)
            private transient GridAbstractUserResource rsrc5;

            /** Global resource. */
            @GridUserResource private transient UserResource2 rsrc6;

            /** User resource. */
            @GridUserResource(resourceClass = UserResource1.class, resourceName = "rsrc3")
            private transient GridAbstractUserResource rsrc7;

            /** Global resource. */
            @GridUserResource(resourceName = "rsrc4")
            private transient UserResource2 rsrc8;

            /** {@inheritDoc} */
            @SuppressWarnings({"ObjectEquality"})
            @Override public Serializable execute() {
                assert rsrc1 != null;
                assert rsrc2 != null;
                assert rsrc3 != null;
                assert rsrc4 != null;
                assert log != null;

                assert rsrc5 != null;
                assert rsrc6 != null;
                assert rsrc7 != null;
                assert rsrc8 != null;

                // Make sure that neither task nor global scope got
                // created more than once.
                assert rsrc1 == rsrc5;
                assert rsrc2 == rsrc6;
                assert rsrc3 == rsrc7;
                assert rsrc4 == rsrc8;

                log.info("Injected shared resource1 into job: " + rsrc1);
                log.info("Injected shared resource2 into job: " + rsrc2);
                log.info("Injected shared resource3 into job: " + rsrc3);
                log.info("Injected shared resource4 into job: " + rsrc4);
                log.info("Injected shared resource5 into job: " + rsrc5);
                log.info("Injected shared resource6 into job: " + rsrc6);
                log.info("Injected shared resource7 into job: " + rsrc7);
                log.info("Injected shared resource8 into job: " + rsrc8);
                log.info("Injected log resource into job: " + log);

                return null;
            }
        }
    }

    /** */
    public static class SharedResourceTask2 extends GridComputeTaskSplitAdapter<Object, Object> {
        /** User resource.  */
        @GridUserResource(resourceClass = UserResource1.class)
        private transient GridAbstractUserResource rsrc1;

        /** User resource. */
        @GridUserResource private transient UserResource2 rsrc2;

        /** User resource.  */
        @GridUserResource(resourceClass = UserResource1.class, resourceName = "rsrc3")
        private transient GridAbstractUserResource rsrc3;

        /** User resource. */
        @GridUserResource(resourceName = "rsrc4")
        private transient UserResource2 rsrc4;

        /** */
        @GridLoggerResource private GridLogger log;

        /** {@inheritDoc} */
        @Override protected Collection<GridComputeJobAdapter> split(int gridSize, Object arg) throws GridException {
            assert rsrc1 != null;
            assert rsrc2 != null;
            assert rsrc3 != null;
            assert rsrc4 != null;
            assert log != null;

            log.info("Injected shared resource1 into task: " + rsrc1);
            log.info("Injected shared resource2 into task: " + rsrc2);
            log.info("Injected shared resource3 into task: " + rsrc3);
            log.info("Injected shared resource4 into task: " + rsrc4);
            log.info("Injected log resource into task: " + log);

            task2Rsrc1 = rsrc1;
            task2Rsrc2 = rsrc2;
            task2Rsrc3 = rsrc3;
            task2Rsrc4 = rsrc4;

            Collection<GridComputeJobAdapter> jobs = new ArrayList<>(gridSize);

            for (int i = 0; i < gridSize; i++) {
                jobs.add(new GridComputeJobAdapter() {
                    /** User resource. */
                    @GridUserResource(resourceClass = UserResource1.class)
                    private transient GridAbstractUserResource rsrc5;

                    /** User resource */
                    @GridUserResource private transient UserResource2 rsrc6;

                    /** User resource. */
                    @GridUserResource(resourceClass = UserResource1.class, resourceName = "rsrc3")
                    private transient GridAbstractUserResource rsrc7;

                    /** User resource */
                    @GridUserResource(resourceName = "rsrc4")
                    private transient UserResource2 rsrc8;

                    /** {@inheritDoc} */
                    @SuppressWarnings({"ObjectEquality"})
                    @Override public Serializable execute() {
                        assert rsrc1 != null;
                        assert rsrc2 != null;
                        assert rsrc3 != null;
                        assert rsrc4 != null;
                        assert log != null;

                        assert rsrc5 != null;
                        assert rsrc6 != null;
                        assert rsrc7 != null;
                        assert rsrc8 != null;

                        // Make sure that neither task nor global scope got
                        // created more than once.
                        assert rsrc1 == rsrc5;
                        assert rsrc2 == rsrc6;
                        assert rsrc3 == rsrc7;
                        assert rsrc4 == rsrc8;

                        log.info("Injected shared resource1 into job: " + rsrc1);
                        log.info("Injected shared resource2 into job: " + rsrc2);
                        log.info("Injected shared resource3 into job: " + rsrc3);
                        log.info("Injected shared resource4 into job: " + rsrc4);
                        log.info("Injected shared resource5 into job: " + rsrc5);
                        log.info("Injected shared resource6 into job: " + rsrc6);
                        log.info("Injected shared resource7 into job: " + rsrc7);
                        log.info("Injected shared resource8 into job: " + rsrc8);
                        log.info("Injected log resource into job: " + log);

                        return null;
                    }
                });
            }

            return jobs;
        }

        /** {@inheritDoc} */
        @Override public Object reduce(List<GridComputeJobResult> results) throws GridException {
            assert rsrc1 != null;
            assert rsrc2 != null;
            assert rsrc3 != null;
            assert rsrc4 != null;
            assert log != null;

            // Nothing to reduce.
            return null;
        }
    }

}
