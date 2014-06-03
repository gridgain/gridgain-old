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

package org.gridgain.grid.streamer.router;

import org.gridgain.grid.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.streamer.*;
import org.gridgain.grid.util.typedef.*;
import org.jdk8.backport.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * Random router. Routes event to random node.
 */
public class GridStreamerRandomEventRouter extends GridStreamerEventRouterAdapter {
    /** Optional predicates to exclude nodes from routing. */
    private GridPredicate<GridNode>[] predicates;

    /**
     * Empty constructor for spring.
     */
    public GridStreamerRandomEventRouter() {
        this((GridPredicate<GridNode>[])null);
    }

    /**
     * Constructs random event router with optional set of filters to apply to streamer projection.
     *
     * @param predicates Node predicates.
     */
    public GridStreamerRandomEventRouter(@Nullable GridPredicate<GridNode>... predicates) {
        this.predicates = predicates;
    }

    /**
     * Constructs random event router with optional set of filters to apply to streamer projection.
     *
     * @param predicates Node predicates.
     */
    @SuppressWarnings("unchecked")
    public GridStreamerRandomEventRouter(Collection<GridPredicate<GridNode>> predicates) {
        if (!F.isEmpty(predicates)) {
            this.predicates = new GridPredicate[predicates.size()];

            predicates.toArray(this.predicates);
        }
    }

    /** {@inheritDoc} */
    @Override public GridNode route(GridStreamerContext ctx, String stageName, Object evt) {
        Collection<GridNode> nodes = F.view(ctx.projection().nodes(), predicates);

        if (F.isEmpty(nodes))
            return null;

        int idx = ThreadLocalRandom8.current().nextInt(nodes.size());

        int i = 0;

        Iterator<GridNode> iter = nodes.iterator();

        while (true) {
            if (!iter.hasNext())
                iter = nodes.iterator();

            GridNode node = iter.next();

            if (idx == i++)
                return node;
        }
    }
}
