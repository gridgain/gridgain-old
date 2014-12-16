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

package org.gridgain.grid.spi.swapspace.file;

import org.gridgain.grid.spi.*;
import org.gridgain.grid.util.mbean.*;

/**
 * Management bean for {@link GridFileSwapSpaceSpi}.
 */
@GridMBeanDescription("MBean that provides configuration information on file-based swapspace SPI.")
public interface GridFileSwapSpaceSpiMBean extends GridSpiManagementMBean {
    /**
     * Gets base directory.
     *
     * @return Base directory.
     */
    @GridMBeanDescription("Base directory.")
    public String getBaseDirectory();

    /**
     * Gets maximum sparsity.
     *
     * @return Maximum sparsity.
     */
    @GridMBeanDescription("Maximum sparsity.")
    public float getMaximumSparsity();

    /**
     * Gets write buffer size in bytes.
     *
     * @return Write buffer size in bytes.
     */
    @GridMBeanDescription("Write buffer size in bytes.")
    public int getWriteBufferSize();

    /**
     * Gets max write queue size in bytes.
     *
     * @return Max write queue size in bytes.
     */
    @GridMBeanDescription("Max write queue size in bytes.")
    public int getMaxWriteQueueSize();

    /**
     * Gets read pool size.
     *
     * @return Read pool size.
     */
    @GridMBeanDescription("Read pool size.")
    public int getReadStripesNumber();
}