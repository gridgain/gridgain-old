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

package org.gridgain.grid.spi.deployment;

import org.gridgain.grid.compute.*;

/**
 * Generic abstraction over deployed resource containing resource's name, class and corresponding class loader.
 */
public interface GridDeploymentResource {
    /**
     * Gets resource name, either class name or alias name, such as alias
     * specified by {@link GridComputeTaskName} annotation.
     *
     * @return Resource name.
     */
    public String getName();

    /**
     * Gets resource class.
     *
     * @return Resource class.
     */
    public Class<?> getResourceClass();

    /**
     * Gets resource class loader.
     *
     * @return Resource class loader.
     */
    public ClassLoader getClassLoader();
}
