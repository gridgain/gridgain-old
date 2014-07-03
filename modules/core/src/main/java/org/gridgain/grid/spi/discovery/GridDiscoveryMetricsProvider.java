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

package org.gridgain.grid.spi.discovery;

import org.gridgain.grid.*;
import org.gridgain.grid.util.tostring.*;

/**
 * Provides metrics to discovery SPI. It is responsibility of discovery SPI
 * to make sure that all nodes have updated metrics data about each other.
 * <p>
 * GridGain implementation will supply discovery SPI with metrics provider
 * via {@link GridDiscoverySpi#setMetricsProvider(GridDiscoveryMetricsProvider)}
 * method.
 */
@GridToStringExclude
public interface GridDiscoveryMetricsProvider {
    /**
     * This method always returns up-to-date metrics data about local node.
     *
     * @return Up to date metrics data about local node.
     */
    public GridNodeMetrics getMetrics();
}
