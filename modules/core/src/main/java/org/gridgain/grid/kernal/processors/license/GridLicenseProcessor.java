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

import org.gridgain.grid.kernal.processors.*;
import org.gridgain.grid.product.*;

/**
 * License processor.
 */
public interface GridLicenseProcessor extends GridProcessor {
    /**
     * Upload the new license into the current node. Throw the exception if the license is not validated.
     *
     * @param licTxt String - The string representation of the license file.
     * @throws GridProductLicenseException - Throw the exception in the case of failed validation.
     */
    public void updateLicense(String licTxt) throws GridProductLicenseException;

    /**
     * Acks the license to the log.
     */
    public void ackLicense();

    /**
     * This method is called periodically by the GridGain to check the license
     * conformance.
     *
     * @throws GridProductLicenseException Thrown in case of any license violation.
     */
    public void checkLicense() throws GridProductLicenseException;

    /**
     * Checks if edition is enabled.
     *
     * @param ed Edition to check.
     * @return {@code True} if enabled.
     */
    public boolean enabled(GridProductEdition ed);

    /**
     * Gets license descriptor.
     *
     * @return License descriptor.
     */
    public GridProductLicense license();

    /**
     * @return Grace period left in minutes if bursting or {@code -1} otherwise.
     */
    public long gracePeriodLeft();
}
