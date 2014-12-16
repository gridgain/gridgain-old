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
import org.gridgain.grid.kernal.visor.cmd.dto.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * Collect license from nodes task.
 */
@GridInternal
public class VisorLicenseCollectTask extends
    VisorMultiNodeTask<Void, Iterable<GridBiTuple<UUID, VisorLicense>>, VisorLicense> {
    /** */
    private static final long serialVersionUID = 0L;

    /** {@inheritDoc} */
    @Override protected VisorLicenseCollectJob job(Void arg) {
        return new VisorLicenseCollectJob(arg);
    }

    /** {@inheritDoc} */
    @Nullable @Override protected Iterable<GridBiTuple<UUID, VisorLicense>> reduce0(List<GridComputeJobResult> results)
        throws GridException {
        Collection<GridBiTuple<UUID, VisorLicense>> licenses = new ArrayList<>(results.size());

        for (GridComputeJobResult r : results) {
            VisorLicense license = r.getException() != null ? null : (VisorLicense) r.getData();

            licenses.add(new GridBiTuple<>(r.getNode().id(), license));
        }

        return licenses;
    }

    /**
     * Job that collect license from nodes.
     */
    private static class VisorLicenseCollectJob extends VisorJob<Void, VisorLicense> {
        /** */
        private static final long serialVersionUID = 0L;

        /**
         * Create job with given argument.
         *
         * @param arg Formal job argument.
         */
        private VisorLicenseCollectJob(Void arg) {
            super(arg);
        }

        /** {@inheritDoc} */
        @Nullable @Override protected VisorLicense run(Void arg) throws GridException {
            return VisorLicense.from(g);
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(VisorLicenseCollectJob.class, this);
        }
    }
}
