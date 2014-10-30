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
import org.gridgain.grid.events.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.resources.*;
import org.gridgain.testframework.*;
import org.gridgain.testframework.junits.common.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import static java.util.concurrent.TimeUnit.*;
import static org.gridgain.grid.events.GridEventType.*;

/**
 *
 */
@GridCommonTest(group = "Kernal Self")
public class GridMultipleVersionsDeploymentSelfTest extends GridCommonAbstractTest {
    /** Excluded classes. */
    private static final String[] EXCLUDE_CLASSES = new String[] {
        GridDeploymentTestTask.class.getName(),
        GridDeploymentTestJob.class.getName()
    };

    /** */
    public GridMultipleVersionsDeploymentSelfTest() {
        super(/*start grid*/false);
    }

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        // Override P2P configuration to exclude Task and Job classes
        cfg.setPeerClassLoadingLocalClassPathExclude(GridDeploymentTestJob.class.getName(),
            GridDeploymentTestTask.class.getName());

        // Following tests makes sense in ISOLATED modes (they redeploy tasks
        // and don't change task version. The different tasks with the same version from the same node
        // executed in parallel - this does not work in share mode.)
        cfg.setDeploymentMode(GridDeploymentMode.ISOLATED);

        cfg.setPeerClassLoadingLocalClassPathExclude(
            "org.gridgain.grid.kernal.GridMultipleVersionsDeploymentSelfTest*");

        return cfg;
    }

    /**
     * @param grid Grid.
     * @param taskName Task name.
     * @return {@code true} if task has been deployed on passed grid.
     */
    private boolean checkDeployed(Grid grid, String taskName) {
        Map<String, Class<? extends GridComputeTask<?, ?>>> locTasks = grid.compute().localTasks();

        if (log().isInfoEnabled())
            log().info("Local tasks found: " + locTasks);

        return locTasks.get(taskName) != null;
    }

    /**
     * @throws Exception If test failed.
     */
    @SuppressWarnings("unchecked")
    public void testMultipleVersionsLocalDeploy() throws Exception {
        try {
            Grid grid = startGrid(1);

            ClassLoader ldr1 = new GridTestClassLoader(
                Collections.singletonMap("testResource", "1"),
                getClass().getClassLoader(),
                EXCLUDE_CLASSES);

            ClassLoader ldr2 = new GridTestClassLoader(
                Collections.singletonMap("testResource", "2"),
                getClass().getClassLoader(),
                EXCLUDE_CLASSES
            );

            Class<? extends GridComputeTask<?, ?>> taskCls1 = (Class<? extends GridComputeTask<?, ?>>)ldr1.
                loadClass(GridDeploymentTestTask.class.getName());

            Class<? extends GridComputeTask<?, ?>> taskCls2 = (Class<? extends GridComputeTask<?, ?>>)ldr2.
                loadClass(GridDeploymentTestTask.class.getName());

            grid.compute().localDeployTask(taskCls1, ldr1);

            // Task will wait for the signal.
            GridComputeTaskFuture fut = grid.compute().execute("GridDeploymentTestTask", null);

            // We should wait here when to be sure that job has been started.
            // Since we loader task/job classes with different class loaders we cannot
            // use any kind of mutex because of the illegal state exception.
            // We have to use timer here. DO NOT CHANGE 2 seconds. This should be enough
            // on Bamboo.
            Thread.sleep(2000);

            assert checkDeployed(grid, "GridDeploymentTestTask");

            // Deploy new one - this should move first task to the obsolete list.
            grid.compute().localDeployTask(taskCls2, ldr2);

            boolean deployed = checkDeployed(grid, "GridDeploymentTestTask");

            Object res = fut.get();

            grid.compute().undeployTask("GridDeploymentTestTask");

            // New one should be deployed.
            assert deployed;

            // Wait for the execution.
            assert res.equals(1);
        }
        finally {
            stopGrid(1);
        }
    }

    /**
     * @throws Exception If test failed.
     */
    @SuppressWarnings("unchecked")
    public void testMultipleVersionsP2PDeploy() throws Exception {
        try {
            Grid g1 = startGrid(1);
            Grid g2 = startGrid(2);

            final CountDownLatch latch = new CountDownLatch(2);

            g2.events().localListen(
                new GridPredicate<GridEvent>() {
                    @Override public boolean apply(GridEvent evt) {
                        info("Received event: " + evt);

                        latch.countDown();

                        return true;
                    }
                }, EVT_TASK_UNDEPLOYED
            );

            ClassLoader ldr1 = new GridTestClassLoader(
                Collections.singletonMap("testResource", "1"),
                getClass().getClassLoader(),
                EXCLUDE_CLASSES);

            ClassLoader ldr2 = new GridTestClassLoader(
                Collections.singletonMap("testResource", "2"),
                getClass().getClassLoader(),
                EXCLUDE_CLASSES);

            Class<? extends GridComputeTask<?, ?>> taskCls1 = (Class<? extends GridComputeTask<?, ?>>)ldr1.
                loadClass(GridDeploymentTestTask.class.getName());

            Class<? extends GridComputeTask<?, ?>> taskCls2 = (Class<? extends GridComputeTask<?, ?>>)ldr2.
                loadClass(GridDeploymentTestTask.class.getName());

            g1.compute().localDeployTask(taskCls1, ldr1);

            // Task will wait for the signal.
            GridComputeTaskFuture fut1 = g1.compute().execute("GridDeploymentTestTask", null);

            assert checkDeployed(g1, "GridDeploymentTestTask");

            // We should wait here when to be sure that job has been started.
            // Since we loader task/job classes with different class loaders we cannot
            // use any kind of mutex because of the illegal state exception.
            // We have to use timer here. DO NOT CHANGE 2 seconds here.
            Thread.sleep(2000);

            // Deploy new one - this should move first task to the obsolete list.
            g1.compute().localDeployTask(taskCls2, ldr2);

            // Task will wait for the signal.
            GridComputeTaskFuture fut2 = g1.compute().execute("GridDeploymentTestTask", null);

            boolean deployed = checkDeployed(g1, "GridDeploymentTestTask");

            Object res1 = fut1.get();
            Object res2 = fut2.get();

            g1.compute().undeployTask("GridDeploymentTestTask");

            // New one should be deployed.
            assert deployed;

            // Wait for the execution.
            assert res1.equals(1);
            assert res2.equals(2);

            stopGrid(1);

            assert latch.await(3000, MILLISECONDS);

            assert !checkDeployed(g2, "GridDeploymentTestTask");
        }
        finally {
            stopGrid(2);
            stopGrid(1);
        }
    }

    /**
     * Task that maps {@link GridDeploymentTestJob} either on local node
     * or on remote nodes if there are any. Never on both.
     */
    @SuppressWarnings({"PublicInnerClass"})
    @GridComputeTaskName(value="GridDeploymentTestTask")
    public static class GridDeploymentTestTask extends GridComputeTaskAdapter<Object, Object> {
        /** */
        @GridLocalNodeIdResource private UUID locNodeId;

        /** {@inheritDoc} */
        @Override public Map<? extends GridComputeJob, GridNode> map(List<GridNode> subgrid, Object arg) throws GridException {
            Map<GridComputeJobAdapter, GridNode> map = new HashMap<>(subgrid.size());

            boolean ignoreLocNode = false;

            if (subgrid.size() == 1)
                assert subgrid.get(0).id().equals(locNodeId) : "Wrong node id.";
            else
                ignoreLocNode = true;

            for (GridNode node : subgrid) {
                // Ignore local node.
                if (ignoreLocNode && node.id().equals(locNodeId))
                    continue;

                map.put(new GridDeploymentTestJob(), node);
            }

            return map;
        }

        /** {@inheritDoc} */
        @Override public Integer reduce(List<GridComputeJobResult> results) throws GridException {
            return results.get(0).getData();
        }
    }

    /**
     * Simple job class that requests resource with name "testResource"
     * and expects "0" value.
     */
    @SuppressWarnings({"PublicInnerClass"})
    public static class GridDeploymentTestJob extends GridComputeJobAdapter {
        /** */
        @GridLoggerResource private GridLogger log;

        /** {@inheritDoc} */
        @Override public Integer execute() throws GridException {
            try {
                if (log.isInfoEnabled())
                    log.info("GridDeploymentTestJob job started");

                // Again there is no way to get access to any
                // mutex of the test class because of the different class loaders.
                // we have to wait.
                Thread.sleep(3000);

                // Here we should request some resources. New task
                // has already been deployed and old one should be still available.
                int res = getClass().getClassLoader().getResourceAsStream("testResource").read();

                return res - 48;
            }
            catch (IOException | InterruptedException e) {
                throw new GridException("Failed to execute job.", e);
            }
        }
    }
}
