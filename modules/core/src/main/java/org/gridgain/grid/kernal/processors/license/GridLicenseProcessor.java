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

package org.gridgain.grid.kernal.processors.license;

import org.gridgain.grid.*;
import org.gridgain.grid.kernal.processors.*;
import org.gridgain.grid.product.*;
import org.jetbrains.annotations.*;

/**
 * License processor.
 */
public interface GridLicenseProcessor extends GridProcessor {
    /**
     * Upload the new license into the current node.
     *
     * @param licTxt The string representation of the license file.
     * @throws GridProductLicenseException Thrown if validation check failed for specified license or
     *      license can not be updated because configured license URL use non-file scheme.
     * @see GridConfiguration#getLicenseUrl()
     */
    public void updateLicense(String licTxt) throws GridProductLicenseException;

    /**
     * Acks the license to the log.
     */
    public void ackLicense();

    /**
     * This method is called periodically by the GridGain to check the license conformance.
     *
     * @throws GridProductLicenseException Thrown in case of any license violation.
     */
    public void checkLicense() throws GridProductLicenseException;

    /**
     * Checks if subsystem is enabled.
     *
     * @param ed Subsystem to check.
     * @return {@code True} if enabled.
     */
    public boolean enabled(GridLicenseSubsystem ed);

    /**
     * Gets license descriptor.
     *
     * @return License descriptor or {@code null} for open-source edition.
     */
    @Nullable public GridProductLicense license();

    /**
     * @return Grace period left in minutes if bursting or {@code -1} otherwise.
     */
    public long gracePeriodLeft();
}
