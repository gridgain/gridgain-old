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

package org.gridgain.grid.util.lang;

import org.gridgain.grid.*;
import org.gridgain.grid.compute.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.jetbrains.annotations.*;

import java.util.concurrent.*;

/**
 * Convenient wrapper for grid job. It allows to create a job clone in cases when the same
 * job needs to be cloned to multiple grid nodes during mapping phase of task execution.
 */
public class GridComputeJobWrapper extends GridMetadataAwareAdapter implements GridComputeJob, Callable<Object>,
    GridPeerDeployAware {
    /** */
    private static final long serialVersionUID = 0L;

    /** */
    private final GridComputeJob job;

    /** Peer deploy aware class. */
    private transient volatile GridPeerDeployAware p;

    /**
     * Creates a wrapper with given grid {@code job}. If {@code job} implements {@link GridMetadataAware}
     * interface and {@code copyMeta} is {@code true} - the metadata information will be
     * copied from given {@code job} to this wrapper.
     *
     * @param job Job to wrap.
     * @param cpMeta Whether or not to copy metadata in case when {@code job}
     *      implements {@link GridMetadataAware} interface.
     */
    public GridComputeJobWrapper(GridComputeJob job, boolean cpMeta) {
        A.notNull(job, "job");

        this.job = job;

        if (cpMeta && job instanceof GridMetadataAware)
            copyMeta((GridMetadataAware)job);
    }

    /**
     * Gets wrapped job.
     *
     * @return Wrapped job.
     */
    public GridComputeJob wrappedJob() {
        return job;
    }

    /** {@inheritDoc} */
    @Nullable
    @Override public final Object call() throws Exception {
        return execute();
    }

    /** {@inheritDoc} */
    @Override public Class<?> deployClass() {
        if (p == null)
            p = U.detectPeerDeployAware(this);

        return p.deployClass();
    }

    /** {@inheritDoc} */
    @Override public ClassLoader classLoader() {
        if (p == null)
            p = U.detectPeerDeployAware(this);

        return p.classLoader();
    }

    /** {@inheritDoc} */
    @Override public void cancel() {
        job.cancel();
    }

    /** {@inheritDoc} */
    @Override public Object execute() throws GridException {
        return job.execute();
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridComputeJobWrapper.class, this);
    }
}
