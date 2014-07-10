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

package org.gridgain.grid.spi.loadbalancing.weightedrandom;

import org.gridgain.grid.spi.*;
import org.gridgain.grid.util.mbean.*;

/**
 * Management MBean for {@link GridWeightedRandomLoadBalancingSpi} SPI.
 */
@GridMBeanDescription("MBean that provides access to weighted random load balancing SPI configuration.")
public interface GridWeightedRandomLoadBalancingSpiMBean extends GridSpiManagementMBean {
    /**
     * Checks whether node weights are considered when doing
     * random load balancing.
     *
     * @return If {@code true} then random load is distributed according
     *      to node weights.
     */
    @GridMBeanDescription("Whether node weights are considered when doing random load balancing.")
    public boolean isUseWeights();

    /**
     * Gets weight of this node.
     *
     * @return Weight of this node.
     */
    @GridMBeanDescription("Weight of this node.")
    public int getNodeWeight();
}
