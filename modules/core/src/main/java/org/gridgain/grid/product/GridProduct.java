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

package org.gridgain.grid.product;

import org.jetbrains.annotations.*;

/**
 * Provides information about current release. Note that enterprise users are also
 * able to renew license. Instance of {@code GridProduct} is obtained from grid as follows:
 * <pre name="code" class="java">
 * GridProduct p = GridGain.grid().product();
 * </pre>
 */
public interface GridProduct {
    /**
     * Gets license descriptor for enterprise edition or {@code null} for open source edition.
     *
     * @return License descriptor.
     */
    @Nullable public GridProductLicense license();

    /**
     * Updates to a new license in enterprise edition. This method is no-op in open source edition.
     *
     * @param lic The content of the license.
     * @throws GridProductLicenseException If license could not be updated.
     */
    public void updateLicense(String lic) throws GridProductLicenseException;

    /**
     * Gets product version for this release.
     *
     * @return Product version for this release.
     */
    public GridProductVersion version();

    /**
     * Copyright statement for GridGain code.
     *
     * @return Legal copyright statement for GridGain code.
     */
    public String copyright();

    /**
     * Gets latest version available for download or
     * {@code null} if information is not available.
     *
     * @return Latest version string or {@code null} if information is not available.
     */
    @Nullable public String latestVersion();
}
