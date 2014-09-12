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

import org.gridgain.grid.service.*;
import org.gridgain.grid.util.tostring.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * Service descriptor.
 */
public class GridServiceDescriptorImpl implements GridServiceDescriptor {
    /** */
    private static final long serialVersionUID = 0L;

    /** Configuration. */
    @GridToStringInclude
    private final GridServiceDeployment dep;

    /** Topology snapshot. */
    @GridToStringInclude
    private Map<UUID, Integer> top;

    /**
     * @param dep Deployment.
     */
    public GridServiceDescriptorImpl(GridServiceDeployment dep) {
        this.dep = dep;
    }

    /** {@inheritDoc} */
    @Override public String name() {
        return dep.configuration().getName();
    }

    /** {@inheritDoc} */
    @Override public Class<? extends GridService> serviceClass() {
        return dep.configuration().getService().getClass();
    }

    /** {@inheritDoc} */
    @Override public int totalCount() {
        return dep.configuration().getTotalCount();
    }

    /** {@inheritDoc} */
    @Override public int maxPerNodeCount() {
        return dep.configuration().getMaxPerNodeCount();
    }

    /** {@inheritDoc} */
    @Nullable @Override public String cacheName() {
        return dep.configuration().getCacheName();
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Nullable @Override public <K> K affinityKey() {
        return (K)dep.configuration().getAffinityKey();
    }

    /** {@inheritDoc} */
    @Override public UUID originNodeId() {
        return dep.nodeId();
    }

    /** {@inheritDoc} */
    @Override public Map<UUID, Integer> topologySnapshot() {
        return top;
    }

    /**
     * @param top Topology snapshot.
     */
    void topologySnapshot(Map<UUID, Integer> top) {
        this.top = top;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridServiceDescriptorImpl.class, this);
    }
}
