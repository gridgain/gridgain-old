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

package org.gridgain.grid.tests.p2p;

import org.gridgain.grid.*;
import org.gridgain.grid.compute.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;

import java.util.*;

/**
 * Test P2P task.
 */
public class GridP2PTestTaskExternalPath1 extends GridComputeTaskAdapter<Object, int[]> {
    /** */
    @GridLoggerResource
    private GridLogger log;

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({"unchecked"})
    @Override public Map<? extends GridComputeJob, GridNode> map(List<GridNode> subgrid, Object arg) throws GridException {
        if (log.isInfoEnabled()) {
            log.info("Mapping [task=" + this + ", subgrid=" + F.viewReadOnly(subgrid, F.node2id()) +
                ", arg=" + arg + ']');
        }

        Set<UUID> nodeIds;

        boolean sleep;

        if (arg instanceof Object[]) {
            nodeIds = Collections.singleton((UUID)(((Object[])arg)[0]));

            sleep = (Boolean)((Object[])arg)[1];
        }
        else if (arg instanceof List) {
            nodeIds = new HashSet<>((Collection<UUID>)arg);

            sleep = false;
        }
        else {
            nodeIds = Collections.singleton((UUID)arg);

            sleep = false;
        }

        Map<TestJob, GridNode> jobs = U.newHashMap(subgrid.size());

        for (GridNode node : subgrid) {
            if (nodeIds.contains(node.id()))
                jobs.put(new TestJob(node.id(), sleep), node);
        }

        if (!jobs.isEmpty())
            return jobs;

        throw new GridException("Failed to find target node: " + arg);
    }

    /**
     * {@inheritDoc}
     */
    @Override public int[] reduce(List<GridComputeJobResult> results) throws GridException {
        return results.get(0).getData();
    }

    /**
     * Simple job class
     */
    @SuppressWarnings({"PublicInnerClass"})
    public static class TestJob extends GridComputeJobAdapter {
        /** User resource. */
        @GridUserResource
        private transient GridTestUserResource rsrc;

        /** Local node ID. */
        @GridLocalNodeIdResource
        private UUID locNodeId;

        /** Task session. */
        @GridTaskSessionResource
        private GridComputeTaskSession ses;

        /** */
        @GridLoggerResource
        private GridLogger log;

        /** */
        @GridInstanceResource
        private Grid g;

        /** */
        private boolean sleep;

        /**
         *
         */
        public TestJob() {
            // No-op.
        }

        /**
         * @param nodeId Node ID for node this job is supposed to execute on.
         * @param sleep Sleep flag.
         */
        public TestJob(UUID nodeId, boolean sleep) {
            super(nodeId);

            this.sleep = sleep;
        }

        /** {@inheritDoc} */
        @Override public int[] execute() throws GridException {
            assert locNodeId.equals(argument(0));

            log.info("Running job on node: " + g.localNode().id());

            if (sleep) {
                try {
                    Thread.sleep(Long.MAX_VALUE);
                }
                catch (InterruptedException e) {
                    log.info("Job has been cancelled. Caught exception: " + e);

                    Thread.currentThread().interrupt();
                }
            }

            return new int[] {
                System.identityHashCode(rsrc),
                System.identityHashCode(ses.getClassLoader())
            };
        }
    }
}
