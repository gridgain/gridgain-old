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

package org.gridgain.client.balancer;

import org.gridgain.client.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.util.typedef.*;

import java.util.*;

/**
 * Base class for balancers. Contains common direct connection handling logic.
 */
public abstract class GridClientBalancerAdapter implements GridClientLoadBalancer {
    /** Selects connectable nodes. */
    private static final GridPredicate<GridClientNode> CONNECTABLE =
        new GridPredicate<GridClientNode>() {
            @Override public boolean apply(GridClientNode e) {
                return e.connectable();
            }
        };

    /** Prefer direct nodes. */
    private boolean preferDirectNodes;

    /**
     * If set to {@code true} balancer should prefer directly connectable
     * nodes over others.
     * <p>
     * In other words when working in router connection mode
     * client will prefer send requests to router nodes
     * if operation projection contains some of them.
     * <p>
     * Default value is {@code false}.
     *
     * @see GridClientNode#connectable()
     * @return Prefer direct nodes.
     */
    public boolean isPreferDirectNodes() {
        return preferDirectNodes;
    }

    /**
     * Sets prefer direct nodes.
     *
     * @param preferDirectNodes Prefer direct nodes.
     */
    public void setPreferDirectNodes(boolean preferDirectNodes) {
        this.preferDirectNodes = preferDirectNodes;
    }

    /**
     * Returns only directly available nodes from given collection.
     *
     * @param nodes Nodes.
     * @return Directly available subset.
     */
    protected static Collection<GridClientNode> selectDirectNodes(Collection<? extends GridClientNode> nodes) {
        return F.viewReadOnly(nodes, F.<GridClientNode>identity(), CONNECTABLE);
    }
}
