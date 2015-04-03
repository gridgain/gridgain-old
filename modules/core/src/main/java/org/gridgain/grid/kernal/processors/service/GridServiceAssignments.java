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

package org.gridgain.grid.kernal.processors.service;

import org.gridgain.grid.*;
import org.gridgain.grid.kernal.processors.cache.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.service.*;
import org.gridgain.grid.util.tostring.*;
import org.gridgain.grid.util.typedef.internal.*;

import java.io.*;
import java.util.*;

/**
 * Service per-node assignment.
 */
public class GridServiceAssignments implements Serializable, GridCacheInternal {
    /** Serialization version. */
    private static final long serialVersionUID = 0L;

    /** Node ID. */
    private final UUID nodeId;

    /** Topology version. */
    private final long topVer;

    /** Service configuration. */
    private final GridServiceConfiguration cfg;

    /** Assignments. */
    @GridToStringInclude
    private Map<UUID, Integer> assigns = Collections.emptyMap();

    /**
     * @param cfg Configuration.
     * @param nodeId Node ID.
     * @param topVer Topology version.
     */
    public GridServiceAssignments(GridServiceConfiguration cfg, UUID nodeId, long topVer) {
        this.cfg = cfg;
        this.nodeId = nodeId;
        this.topVer = topVer;
    }

    /**
     * @return Configuration.
     */
    public GridServiceConfiguration configuration() {
        return cfg;
    }

    /**
     * @return Service name.
     */
    public String name() {
        return cfg.getName();
    }

    /**
     * @return Service.
     */
    public GridService service() {
        return cfg.getService();
    }

    /**
     * @return Topology version.
     */
    public long topologyVersion() {
        return topVer;
    }

    /**
     * @return Cache name.
     */
    public String cacheName() {
        return cfg.getCacheName();
    }

    /**
     * @return Affinity key.
     */
    public Object affinityKey() {
        return cfg.getAffinityKey();
    }

    /**
     * @return Origin node ID.
     */
    public UUID nodeId() {
        return nodeId;
    }

    /**
     * @return Node filter.
     */
    public GridPredicate<GridNode> nodeFilter() {
        return cfg.getNodeFilter();
    }

    /**
     * @return Assignments.
     */
    public Map<UUID, Integer> assigns() {
        return assigns;
    }

    /**
     * @param assigns Assignments.
     */
    public void assigns(Map<UUID, Integer> assigns) {
        this.assigns = assigns;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridServiceAssignments.class, this);
    }
}
