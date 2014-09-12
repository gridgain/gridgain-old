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
import org.gridgain.grid.lang.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.marshaller.optimized.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.spi.collision.jobstealing.*;
import org.gridgain.grid.spi.failover.jobstealing.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.testframework.config.*;
import org.gridgain.testframework.junits.common.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Job stealing test.
 */
@GridCommonTest(group = "Kernal Self")
public class GridJobStealingSelfTest extends GridCommonAbstractTest {
    /** Task execution timeout in milliseconds. */
    private static final int TASK_EXEC_TIMEOUT_MS = 50000;

    /** */
    private Grid grid1;

    /** */
    private Grid grid2;

    /** Job distribution map. Records which job has run on which node. */
    private static Map<UUID, Collection<GridComputeJob>> jobDistrMap = new HashMap<>();

    /** */
    public GridJobStealingSelfTest() {
        super(false /* don't start grid*/);
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        jobDistrMap.clear();

        grid1 = startGrid(1);

        grid2 = startGrid(2);
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        stopAllGrids();

        grid1 = null;
        grid2 = null;
    }

    /**
     * Test 2 jobs on 1 node.
     *
     * @throws GridException If test failed.
     */
    public void testTwoJobs() throws GridException {
        grid1.compute().execute(new JobStealingSingleNodeTask(2), null).get(TASK_EXEC_TIMEOUT_MS);

        // Verify that 1 job was stolen by second node.
        assertEquals(2, jobDistrMap.keySet().size());
        assertEquals(1, jobDistrMap.get(grid1.localNode().id()).size());
        assertEquals(1, jobDistrMap.get(grid2.localNode().id()).size());
    }

    /**
     * Test 2 jobs on 1 node with null predicate.
     *
     * @throws GridException If test failed.
     */
    @SuppressWarnings("NullArgumentToVariableArgMethod")
    public void testTwoJobsNullPredicate() throws GridException {
        grid1.compute().execute(new JobStealingSingleNodeTask(2), null).get(TASK_EXEC_TIMEOUT_MS);

        // Verify that 1 job was stolen by second node.
        assertEquals(2, jobDistrMap.keySet().size());
        assertEquals(1, jobDistrMap.get(grid1.localNode().id()).size());
        assertEquals(1, jobDistrMap.get(grid2.localNode().id()).size());
    }

    /**
     * Test 2 jobs on 1 node with null predicate using string task name.
     *
     * @throws GridException If test failed.
     */
    @SuppressWarnings("NullArgumentToVariableArgMethod")
    public void testTwoJobsTaskNameNullPredicate() throws GridException {
        grid1.compute().execute(JobStealingSingleNodeTask.class.getName(), null).get(TASK_EXEC_TIMEOUT_MS);

        // Verify that 1 job was stolen by second node.
        assertEquals(2, jobDistrMap.keySet().size());
        assertEquals(1, jobDistrMap.get(grid1.localNode().id()).size());
        assertEquals(1, jobDistrMap.get(grid2.localNode().id()).size());
    }

    /**
     * Test 2 jobs on 1 node when one of the predicates is null.
     *
     * @throws GridException If test failed.
     */
    @SuppressWarnings("unchecked")
    public void testTwoJobsPartiallyNullPredicate() throws GridException {
        GridPredicate<GridNode> topPred =  new GridPredicate<GridNode>() {
                @Override public boolean apply(GridNode e) {
                    return grid2.localNode().id().equals(e.id()); // Limit projection with only grid2.
                }
            };

        grid1.forPredicate(topPred).compute().withTimeout(TASK_EXEC_TIMEOUT_MS).
            execute(new JobStealingSpreadTask(2), null).get(TASK_EXEC_TIMEOUT_MS);

        assertEquals(1, jobDistrMap.keySet().size());
        assertEquals(2, jobDistrMap.get(grid2.localNode().id()).size());
        assertFalse(jobDistrMap.containsKey(grid1.localNode().id()));
    }

    /**
     * Tests that projection predicate is taken into account by Stealing SPI.
     *
     * @throws Exception If failed.
     */
    public void testProjectionPredicate() throws Exception {
        final Grid grid3 = startGrid(3);

        grid1.forPredicate(new P1<GridNode>() {
            @Override public boolean apply(GridNode e) {
                return grid1.localNode().id().equals(e.id()) ||
                    grid3.localNode().id().equals(e.id()); // Limit projection with only grid1 or grid3 node.
            }
        }).compute().execute(new JobStealingSpreadTask(4), null).get(TASK_EXEC_TIMEOUT_MS);

        // Verify that jobs were run only on grid1 and grid3 (not on grid2)
        assertEquals(2, jobDistrMap.keySet().size());
        assertEquals(2, jobDistrMap.get(grid1.localNode().id()).size());
        assertEquals(2, jobDistrMap.get(grid3.localNode().id()).size());
        assertFalse(jobDistrMap.containsKey(grid2.localNode().id()));
    }

    /**
     * Tests that projection predicate is taken into account by Stealing SPI,
     * and that jobs in projection can steal tasks from each other.
     *
     * @throws Exception If failed.
     */
    public void testProjectionPredicateInternalStealing() throws Exception {
        final Grid grid3 = startGrid(3);

        P1<GridNode> p = new P1<GridNode>() {
            @Override public boolean apply(GridNode e) {
                return grid1.localNode().id().equals(e.id()) ||
                    grid3.localNode().id().equals(e.id()); // Limit projection with only grid1 or grid3 node.
            }
        };

        grid1.forPredicate(p).compute().
            execute(new JobStealingSingleNodeTask(4), null).get(TASK_EXEC_TIMEOUT_MS);

        // Verify that jobs were run only on grid1 and grid3 (not on grid2)
        assertEquals(2, jobDistrMap.keySet().size());
        assertFalse(jobDistrMap.containsKey(grid2.localNode().id()));
    }

    /**
     * Tests that a job is not cancelled if there are no
     * available thief nodes in topology.
     *
     * @throws Exception If failed.
     */
    public void testSingleNodeTopology() throws Exception {
        GridPredicate<GridNode> p = new GridPredicate<GridNode>() {
            @Override public boolean apply(GridNode e) {
                return grid1.localNode().id().equals(e.id()); // Limit projection with only grid1 node.
            }
        };

        grid1.forPredicate(p).compute().
            execute(new JobStealingSpreadTask(2), null).get(TASK_EXEC_TIMEOUT_MS);

        assertEquals(1, jobDistrMap.keySet().size());
        assertEquals(2, jobDistrMap.get(grid1.localNode().id()).size());
    }

    /**
     * Tests that a job is not cancelled if there are no
     * available thief nodes in projection.
     *
     * @throws Exception If failed.
     */
    public void testSingleNodeProjection() throws Exception {
        GridProjection prj = grid1.forNodeIds(Collections.singleton(grid1.localNode().id()));

        prj.compute().execute(new JobStealingSpreadTask(2), null).get(TASK_EXEC_TIMEOUT_MS);

        assertEquals(1, jobDistrMap.keySet().size());
        assertEquals(2, jobDistrMap.get(grid1.localNode().id()).size());
    }

    /**
     * Tests that a job is not cancelled if there are no
     * available thief nodes in projection. Uses null predicate.
     *
     * @throws Exception If failed.
     */
    @SuppressWarnings("NullArgumentToVariableArgMethod")
    public void testSingleNodeProjectionNullPredicate() throws Exception {
        GridProjection prj = grid1.forNodeIds(Collections.singleton(grid1.localNode().id()));

        prj.compute().withTimeout(TASK_EXEC_TIMEOUT_MS).execute(new JobStealingSpreadTask(2), null).
            get(TASK_EXEC_TIMEOUT_MS);

        assertEquals(1, jobDistrMap.keySet().size());
        assertEquals(2, jobDistrMap.get(grid1.localNode().id()).size());
    }

    /**
     * Tests job stealing with peer deployment and different class loaders.
     *
     * @throws Exception If failed.
     */
    @SuppressWarnings("unchecked")
    public void testProjectionPredicateDifferentClassLoaders() throws Exception {
        final Grid grid3 = startGrid(3);

        URL[] clsLdrUrls;
        try {
            clsLdrUrls = new URL[] {new URL(GridTestProperties.getProperty("p2p.uri.cls"))};
        }
        catch (MalformedURLException e) {
            throw new RuntimeException("Define property p2p.uri.cls", e);
        }

        ClassLoader ldr1 = new URLClassLoader(clsLdrUrls, getClass().getClassLoader());

        Class taskCls = ldr1.loadClass("org.gridgain.grid.tests.p2p.JobStealingTask");
        Class nodeFilterCls = ldr1.loadClass("org.gridgain.grid.tests.p2p.GridExcludeNodeFilter");

        GridPredicate<GridNode> nodeFilter = (GridPredicate<GridNode>)nodeFilterCls
            .getConstructor(UUID.class).newInstance(grid2.localNode().id());

        Map<UUID, Integer> ret = (Map<UUID, Integer>)grid1.forPredicate(nodeFilter)
            .compute().execute(taskCls, null).get(TASK_EXEC_TIMEOUT_MS);

        assert ret != null;
        assert ret.get(grid1.localNode().id()) != null && ret.get(grid1.localNode().id()) == 2 :
            ret.get(grid1.localNode().id());
        assert ret.get(grid3.localNode().id()) != null && ret.get(grid3.localNode().id()) == 2 :
            ret.get(grid3.localNode().id());
    }

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        GridJobStealingCollisionSpi colSpi = new GridJobStealingCollisionSpi();

        // One job at a time.
        colSpi.setActiveJobsThreshold(1);
        colSpi.setWaitJobsThreshold(0);

        GridJobStealingFailoverSpi failSpi = new GridJobStealingFailoverSpi();

        // Verify defaults.
        assert failSpi.getMaximumFailoverAttempts() == GridJobStealingFailoverSpi.DFLT_MAX_FAILOVER_ATTEMPTS;

        cfg.setCollisionSpi(colSpi);
        cfg.setFailoverSpi(failSpi);

        cfg.setMarshaller(new GridOptimizedMarshaller(false));

        return cfg;
    }

    /**
     * Job stealing task, that spreads jobs equally over the grid.
     */
    private static class JobStealingSpreadTask extends GridComputeTaskAdapter<Object, Object> {
        /** Grid. */
        @GridInstanceResource
        private Grid grid;

        /** Logger. */
        @GridLoggerResource
        private GridLogger log;

        /** Number of jobs to spawn from task. */
        protected final int nJobs;

        /**
         * Constructs a new task instance.
         *
         * @param nJobs Number of jobs to spawn from this task.
         */
        JobStealingSpreadTask(int nJobs) {
            this.nJobs = nJobs;
        }

        /** {@inheritDoc} */
        @SuppressWarnings("ForLoopReplaceableByForEach")
        @Override public Map<? extends GridComputeJob, GridNode> map(List<GridNode> subgrid,
            @Nullable Object arg) throws GridException {
            //assert subgrid.size() == 2 : "Invalid subgrid size: " + subgrid.size();

            Map<GridComputeJobAdapter, GridNode> map = new HashMap<>(subgrid.size());

            Iterator<GridNode> subIter = subgrid.iterator();

            // Spread jobs over subgrid.
            for (int i = 0; i < nJobs; i++) {
                if (!subIter.hasNext())
                    subIter = subgrid.iterator(); // wrap around

                map.put(new GridJobStealingJob(5000L), subIter.next());
            }

            return map;
        }

        /** {@inheritDoc} */
        @SuppressWarnings("SuspiciousMethodCalls")
        @Override public Object reduce(List<GridComputeJobResult> results) throws GridException {
            for (GridComputeJobResult res : results) {
                log.info("Job result: " + res.getData());
            }

            return null;
        }
    }

    /**
     * Job stealing task, that puts all jobs onto one node.
     */
    private static class JobStealingSingleNodeTask extends JobStealingSpreadTask {
        /** {@inheritDoc} */
        JobStealingSingleNodeTask(int nJobs) {
            super(nJobs);
        }

        /**
         * Default constructor.
         *
         * Uses 2 jobs.
         */
        JobStealingSingleNodeTask() {
            super(2);
        }

        /** {@inheritDoc} */
        @SuppressWarnings("ForLoopReplaceableByForEach")
        @Override public Map<? extends GridComputeJob, GridNode> map(List<GridNode> subgrid,
            @Nullable Object arg) throws GridException {
            assert subgrid.size() > 1 : "Invalid subgrid size: " + subgrid.size();

            Map<GridComputeJobAdapter, GridNode> map = new HashMap<>(subgrid.size());

            // Put all jobs onto one node.
            for (int i = 0; i < nJobs; i++)
                map.put(new GridJobStealingJob(5000L), subgrid.get(0));

            return map;
        }
    }

    /**
     * Job stealing job.
     */
    private static final class GridJobStealingJob extends GridComputeJobAdapter {
        /** Injected grid. */
        @GridInstanceResource
        private Grid grid;

        /** Logger. */
        @GridLoggerResource
        private GridLogger log;

        /**
         * @param arg Job argument.
         */
        GridJobStealingJob(Long arg) {
            super(arg);
        }

        /** {@inheritDoc} */
        @Override public Serializable execute() throws GridException {
            log.info("Started job on node: " + grid.localNode().id());

            if (!jobDistrMap.containsKey(grid.localNode().id())) {
                Collection<GridComputeJob> jobs = new ArrayList<>();
                jobs.add(this);

                jobDistrMap.put(grid.localNode().id(), jobs);
            }
            else
                jobDistrMap.get(grid.localNode().id()).add(this);

            try {
                Long sleep = argument(0);

                assert sleep != null;

                Thread.sleep(sleep);
            }
            catch (InterruptedException e) {
                log.info("Job got interrupted on node: " + grid.localNode().id());

                throw new GridException("Job got interrupted.", e);
            }
            finally {
                log.info("Job finished on node: " + grid.localNode().id());
            }

            return grid.localNode().id();
        }
    }
}
