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
import org.gridgain.grid.logger.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.testframework.*;
import org.gridgain.testframework.junits.common.*;

import java.io.*;
import java.util.*;

/**
 *
 */
@GridCommonTest(group = "Kernal Self")
public class GridExplicitImplicitDeploymentSelfTest extends GridCommonAbstractTest {
    /** */
    public GridExplicitImplicitDeploymentSelfTest() {
        super(/*start grid*/false);
    }

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        // Override P2P configuration to exclude Task and Job classes
        cfg.setPeerClassLoadingLocalClassPathExclude(GridDeploymentResourceTestTask.class.getName(),
            GridDeploymentResourceTestJob.class.getName());

        cfg.setDeploymentMode(GridDeploymentMode.ISOLATED);

        return cfg;
    }

    /**
     * @throws Exception If test failed.
     */
    public void testImplicitDeployLocally() throws Exception {
        execImplicitDeployLocally(true, true, true);
    }

    /**
     * @throws Exception If test failed.
     */
    public void testImplicitDeployP2P() throws Exception {
        execImplicitDeployP2P(true, true, true);
    }

    /**
     * @throws Exception If test failed.
     */
    public void testExplicitDeployLocally() throws Exception {
        execExplicitDeployLocally(true, true, true);
    }

    /**
     * @throws Exception If test failed.
     */
    public void testExplicitDeployP2P() throws Exception {
        execExplicitDeployP2P(true, true, true);
    }

    /**
     * @param grid Grid.
     */
    @SuppressWarnings({"CatchGenericClass"})
    private void stopGrid(Grid grid) {
        try {
            if (grid != null)
                G.stop(grid.name(), true);
        }
        catch (Throwable e) {
            error("Got error when stopping grid.", e);
        }
    }

    /**
     * @param byCls If {@code true} than executes task by Class.
     * @param byTask If {@code true} than executes task instance.
     * @param byName If {@code true} than executes task by class name.
     * @throws Exception If test failed.
     */
    @SuppressWarnings("unchecked")
    private void execExplicitDeployLocally(boolean byCls, boolean byTask, boolean byName) throws Exception {
        Grid grid = null;

        try {
            grid = startGrid();

            // Explicit Deployment. Task execution should return 0.
            // Say resource class loader - different to task one.
            ClassLoader ldr1 = new GridTestClassLoader(
                Collections.singletonMap("testResource", "1"),
                getClass().getClassLoader());

            // Assume that users task and job were loaded with this class loader
            ClassLoader ldr2 = new GridTestClassLoader(
                Collections.singletonMap("testResource", "2"),
                getClass().getClassLoader(),
                GridDeploymentResourceTestTask.class.getName(),
                GridDeploymentResourceTestJob.class.getName()
            );

            info("Loader1: " + ldr1);
            info("Loader2: " + ldr2);

            Class<? extends GridComputeTask<String, Integer>> taskCls = (Class<? extends GridComputeTask<String, Integer>>)
                ldr2.loadClass(GridDeploymentResourceTestTask.class.getName());

            // Check auto-deploy. It should pick up resource class loader.
            if (byCls) {
                grid.compute().localDeployTask(taskCls, ldr1);

                Integer res = grid.compute().execute(taskCls, null).get();

                assert res != null;
                assert res == 2 : "Invalid response: " + res;
            }

            if (byTask) {
                grid.compute().localDeployTask(taskCls, ldr1);

                Integer res = grid.compute().execute(taskCls.newInstance(), null).get();

                assert res != null;
                assert res == 2 : "Invalid response: " + res;
            }

            if (byName) {
                grid.compute().localDeployTask(taskCls, ldr1);

                Integer res = (Integer)grid.compute().execute(taskCls.getName(), null).get();

                assert res != null;
                assert res == 1 : "Invalid response: " + res;
            }
        }
        finally {
            stopGrid(grid);
        }
    }

    /**
     * @param byCls If {@code true} than executes task by Class.
     * @param byTask If {@code true} than executes task instance.
     * @param byName If {@code true} than executes task by class name.
     * @throws Exception If test failed.
     */
   @SuppressWarnings("unchecked")
   private void execImplicitDeployLocally(boolean byCls, boolean byTask, boolean byName) throws Exception {
       Grid grid = null;

       try {
           grid = startGrid();

           // First task class loader.
           ClassLoader ldr1 = new GridTestClassLoader(
               Collections.singletonMap("testResource", "1"),
               getClass().getClassLoader(),
               GridDeploymentResourceTestTask.class.getName(),
               GridDeploymentResourceTestJob.class.getName()
           );

           // Second task class loader
           ClassLoader ldr2 = new GridTestClassLoader(
               Collections.singletonMap("testResource", "2"),
               getClass().getClassLoader(),
               GridDeploymentResourceTestTask.class.getName(),
               GridDeploymentResourceTestJob.class.getName()
           );

           // The same name but different classes/ class loaders.
           Class<? extends GridComputeTask<String, Integer>> taskCls1 = (Class<? extends GridComputeTask<String, Integer>>)
               ldr1.loadClass(GridDeploymentResourceTestTask.class.getName());

           Class<? extends GridComputeTask<String, Integer>> taskCls2 = (Class<? extends GridComputeTask<String, Integer>>)
               ldr2.loadClass(GridDeploymentResourceTestTask.class.getName());

           if (byCls) {
               Integer res1 = grid.compute().execute(taskCls1, null).get();
               Integer res2 = grid.compute().execute(taskCls2, null).get();

               assert res1 != null;
               assert res2 != null;

               assert res1 == 1 : "Invalid res1: " + res1;
               assert res2 == 2 : "Invalid res2: " + res2;
           }

           if (byTask) {
               Integer res1 = grid.compute().execute(taskCls1.newInstance(), null).get();
               Integer res2 = grid.compute().execute(taskCls2.newInstance(), null).get();

               assert res1 != null;
               assert res2 != null;

               assert res1 == 1 : "Invalid res1: " + res1;
               assert res2 == 2 : "Invalid res2: " + res2;
           }

           if (byName) {
               grid.compute().localDeployTask(taskCls1, ldr1);

               Integer res1 = (Integer)grid.compute().execute(taskCls1.getName(), null).get();

               grid.compute().localDeployTask(taskCls2, ldr2);

               Integer res2 = (Integer)grid.compute().execute(taskCls2.getName(), null).get();

               assert res1 != null;
               assert res2 != null;

               assert res1 == 1 : "Invalid res1: " + res1;
               assert res2 == 2 : "Invalid res2: " + res2;
           }
       }
       finally {
           stopGrid(grid);
       }
   }

    /**
     * @param byCls If {@code true} than executes task by Class.
     * @param byTask If {@code true} than executes task instance.
     * @param byName If {@code true} than executes task by class name.
     * @throws Exception If test failed.
     */
    @SuppressWarnings("unchecked")
    private void execExplicitDeployP2P(boolean byCls, boolean byTask, boolean byName) throws Exception {
       Grid grid1 = null;
       Grid grid2 = null;

       try {
           grid1 = startGrid(1);
           grid2 = startGrid(2);

           ClassLoader ldr1 = new GridTestClassLoader(
               Collections.singletonMap("testResource", "1"),
               getClass().getClassLoader(),
               GridDeploymentResourceTestTask.class.getName(),
               GridDeploymentResourceTestJob.class.getName()
           );

           ClassLoader ldr2 = new GridTestClassLoader(
               Collections.singletonMap("testResource", "2"),
               getClass().getClassLoader(),
               GridDeploymentResourceTestTask.class.getName(),
               GridDeploymentResourceTestJob.class.getName()
           );

           Class<? extends GridComputeTask<String, Integer>> taskCls = (Class<? extends GridComputeTask<String, Integer>>)
               ldr2.loadClass(GridDeploymentResourceTestTask.class.getName());

           if (byCls) {
               grid1.compute().localDeployTask(taskCls, ldr1);

               // Even though the task is deployed with resource class loader,
               // when we execute it, it will be redeployed with task class-loader.
               Integer res = grid1.compute().execute(taskCls, null).get();

               assert res != null;
               assert res == 2 : "Invalid response: " + res;
           }


           if (byTask) {
               grid1.compute().localDeployTask(taskCls, ldr1);

               // Even though the task is deployed with resource class loader,
               // when we execute it, it will be redeployed with task class-loader.
               Integer res = grid1.compute().execute(taskCls.newInstance(), null).get();

               assert res != null;
               assert res == 2 : "Invalid response: " + res;
           }

           if (byName) {
               grid1.compute().localDeployTask(taskCls, ldr1);

               // Even though the task is deployed with resource class loader,
               // when we execute it, it will be redeployed with task class-loader.
               Integer res = (Integer)grid1.compute().execute(taskCls.getName(), null).get();

               assert res != null;
               assert res == 1 : "Invalid response: " + res;
           }
       }
       finally {
           stopGrid(grid2);
           stopGrid(grid1);
       }
    }

    /**
     * @param byCls If {@code true} than executes task by Class.
     * @param byTask If {@code true} than executes task instance.
     * @param byName If {@code true} than executes task by class name.
     * @throws Exception If test failed.
     */
   @SuppressWarnings("unchecked")
   private void execImplicitDeployP2P(boolean byCls, boolean byTask, boolean byName) throws Exception {
      Grid grid1 = null;
      Grid grid2 = null;

      try {
          grid1 = startGrid(1);
          grid2 = startGrid(2);

          ClassLoader ldr1 = new GridTestClassLoader(
              Collections.singletonMap("testResource", "1"),
              getClass().getClassLoader(),
              GridDeploymentResourceTestTask.class.getName(),
              GridDeploymentResourceTestJob.class.getName()
          );

          ClassLoader ldr2 = new GridTestClassLoader(
              Collections.singletonMap("testResource", "2"),
              getClass().getClassLoader(),
              GridDeploymentResourceTestTask.class.getName(),
              GridDeploymentResourceTestJob.class.getName()
          );

          Class<? extends GridComputeTask<String, Integer>> taskCls1 = (Class<? extends GridComputeTask<String, Integer>>)
              ldr1.loadClass(GridDeploymentResourceTestTask.class.getName());

          Class<? extends GridComputeTask<String, Integer>> taskCls2 = (Class<? extends GridComputeTask<String, Integer>>)
              ldr2.loadClass(GridDeploymentResourceTestTask.class.getName());

          if (byCls) {
              Integer res1 = grid1.compute().execute(taskCls1, null).get();
              Integer res2 = grid1.compute().execute(taskCls2, null).get();

              assert res1 != null;
              assert res2 != null;

              assert res1 == 1 : "Invalid res1: " + res1;
              assert res2 == 2 : "Invalid res2: " + res2;
          }

          if (byTask) {
              Integer res1 = grid1.compute().execute(taskCls1.newInstance(), null).get();
              Integer res2 = grid1.compute().execute(taskCls2.newInstance(), null).get();

              assert res1 != null;
              assert res2 != null;

              assert res1 == 1 : "Invalid res1: " + res1;
              assert res2 == 2 : "Invalid res2: " + res2;
          }

          if (byName) {
              grid1.compute().localDeployTask(taskCls1, ldr1);

              Integer res1 = (Integer)grid1.compute().execute(taskCls1.getName(), null).get();

              grid1.compute().localDeployTask(taskCls2, ldr2);

              Integer res2 = (Integer)grid1.compute().execute(taskCls2.getName(), null).get();

              assert res1 != null;
              assert res2 != null;

              assert res1 == 1 : "Invalid res1: " + res1;
              assert res2 == 2 : "Invalid res2: " + res2;
          }
      }
      finally {
          stopGrid(grid1);
          stopGrid(grid2);
      }
   }

    /**
     * We use custom name to avoid auto-deployment in the same VM.
     */
    @SuppressWarnings({"PublicInnerClass"})
    @GridComputeTaskName("GridDeploymentResourceTestTask")
    public static class GridDeploymentResourceTestTask extends GridComputeTaskAdapter<String, Integer> {
        /** */
        @GridLocalNodeIdResource
        private UUID locId;

        /** {@inheritDoc} */
        @Override public Map<? extends GridComputeJob, GridNode> map(List<GridNode> subgrid, String arg) throws GridException {
            Map<GridComputeJobAdapter, GridNode> map = new HashMap<>(subgrid.size());

            boolean ignoreLocNode = false;

            if (subgrid.size() == 1)
                assert subgrid.get(0).id().equals(locId) : "Wrong node id.";
            else
                ignoreLocNode = true;

            for (GridNode node : subgrid) {
                // Ignore local node.
                if (ignoreLocNode && node.id().equals(locId))
                    continue;

                map.put(new GridDeploymentResourceTestJob(), node);
            }

            return map;
        }

        /** {@inheritDoc} */
        @Override public Integer reduce(List<GridComputeJobResult> results) throws GridException {
            return results.get(0).getData();
        }
    }

    /**
     * Simple job for this test.
     */
    @SuppressWarnings({"PublicInnerClass"})
    public static final class GridDeploymentResourceTestJob extends GridComputeJobAdapter {
        /** */
        @GridLoggerResource private GridLogger log;

        /** {@inheritDoc} */
        @Override public Serializable execute() throws GridException {
            if (log.isInfoEnabled())
                log.info("Executing grid job: " + this);

            try {
                ClassLoader ldr = Thread.currentThread().getContextClassLoader();

                if (log.isInfoEnabled())
                    log.info("Loader (inside job): " + ldr);

                InputStream in = ldr.getResourceAsStream("testResource");

                if (in != null) {
                    Reader reader = new InputStreamReader(in);

                    try {
                        char res = (char)reader.read();

                        return Integer.parseInt(Character.toString(res));
                    }
                    finally {
                        U.close(in, null);
                    }
                }

                return null;
            }
            catch (IOException e) {
                throw new GridException("Failed to execute job.", e);
            }
        }
    }
}
