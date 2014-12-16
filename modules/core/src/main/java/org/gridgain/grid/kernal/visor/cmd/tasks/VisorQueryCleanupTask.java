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

package org.gridgain.grid.kernal.visor.cmd.tasks;

import org.gridgain.grid.*;
import org.gridgain.grid.compute.*;
import org.gridgain.grid.kernal.processors.task.*;
import org.gridgain.grid.kernal.visor.cmd.*;
import org.gridgain.grid.kernal.visor.cmd.tasks.VisorQueryTask.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.jetbrains.annotations.*;

import java.util.*;

import static org.gridgain.grid.kernal.visor.cmd.VisorTaskUtils.*;

/**
 * Task for cleanup not needed SCAN or SQL queries result futures from node local.
 */
@GridInternal
public class VisorQueryCleanupTask extends VisorMultiNodeTask<Map<UUID, Collection<String>>, Void, Void> {
    /** */
    private static final long serialVersionUID = 0L;

    /** {@inheritDoc} */
    @Override protected VisorJob<Map<UUID, Collection<String>>, Void> job(Map<UUID, Collection<String>> arg) {
        return null;
    }

    /** {@inheritDoc} */
    @Nullable @Override public Map<? extends GridComputeJob, GridNode> map(List<GridNode> subgrid,
        @Nullable GridBiTuple<Set<UUID>, Map<UUID, Collection<String>>> arg) throws GridException {
        assert arg != null;
        assert arg.get2() != null;

        start = U.currentTimeMillis();

        boolean debug = debugState(g);

        if (debug)
            logStart(g.log(), getClass(), start);

        Set<UUID> nodeIds = arg.get2().keySet();

        Map<GridComputeJob, GridNode> map = U.newHashMap(nodeIds.size());

        try {
            taskArg = arg.get2();

            for (GridNode node : g.nodes())
                if (nodeIds.contains(node.id()))
                    map.put(new VisorQueryCleanupJob(taskArg.get(node.id())), node);

            return map;
        }
        finally {
            if (debug)
                logMapped(g.log(), getClass(), map.values());
        }
    }

    /** {@inheritDoc} */
    @Nullable @Override protected Void reduce0(List list) throws GridException {
        return null;
    }

    /**
     * Job for cleanup not needed SCAN or SQL queries result futures from node local.
     */
    private static class VisorQueryCleanupJob extends VisorJob<Collection<String>, Void> {
        /** */
        private static final long serialVersionUID = 0L;

        /**
         * Create job with specified argument.
         *
         * @param arg Job argument.
         */
        protected VisorQueryCleanupJob(Collection<String> arg) {
            super(arg);
        }

        /** {@inheritDoc} */
        @Override protected Void run(Collection<String> qryIds) throws GridException {
            GridNodeLocalMap<String, VisorFutureResultSetHolder> locMap = g.nodeLocalMap();

            for (String qryId : qryIds)
                locMap.remove(qryId);

            return null;
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(VisorQueryCleanupJob.class, this);
        }
    }
}
