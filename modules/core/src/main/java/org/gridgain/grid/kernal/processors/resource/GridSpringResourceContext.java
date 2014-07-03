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

package org.gridgain.grid.kernal.processors.resource;

import org.gridgain.grid.*;
import org.gridgain.grid.kernal.GridGainEx;

/**
 * Interface was introduced to avoid compile-time dependency on spring framework. Spring resource context
 * provides optional spring resource injectors, it can be passed to factory method
 * starting GridGain {@link GridGainEx#start(GridSpringResourceContext)}.
 */
public interface GridSpringResourceContext {
    /**
     * @return Spring bean injector.
     */
    public GridResourceInjector springBeanInjector();

    /**
     * @return Spring context injector.
     */
    public GridResourceInjector springContextInjector();

    /**
     * Return original object if AOP used with proxy objects.
     *
     * @param target Target object.
     * @return Original object wrapped by proxy.
     * @throws org.gridgain.grid.GridException If unwrap failed.
     */
    public Object unwrapTarget(Object target) throws GridException;
}

