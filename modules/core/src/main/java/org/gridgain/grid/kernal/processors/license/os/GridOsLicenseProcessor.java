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

package org.gridgain.grid.kernal.processors.license.os;

import org.gridgain.grid.kernal.*;
import org.gridgain.grid.kernal.processors.*;
import org.gridgain.grid.kernal.processors.license.*;
import org.gridgain.grid.product.*;
import org.jetbrains.annotations.*;

/**
 * No-op implementation for {@link GridLicenseProcessor}.
 */
public class GridOsLicenseProcessor extends GridProcessorAdapter implements GridLicenseProcessor {
    /**
     * @param ctx Kernal context.
     */
    public GridOsLicenseProcessor(GridKernalContext ctx) {
        super(ctx);
    }

    /** {@inheritDoc} */
    @Override public void updateLicense(String licTxt) throws GridProductLicenseException {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override public void ackLicense() {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override public void checkLicense() throws GridProductLicenseException {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override public boolean enabled(GridLicenseSubsystem ed) {
        return true;
    }

    /** {@inheritDoc} */
    @Nullable @Override public GridProductLicense license() {
        return null;
    }

    /** {@inheritDoc} */
    @Override public long gracePeriodLeft() {
        return -1;
    }
}
