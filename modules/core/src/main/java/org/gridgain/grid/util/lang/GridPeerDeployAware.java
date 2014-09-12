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

package org.gridgain.grid.util.lang;

import java.io.*;

/**
 * Represents any class that needs to maintain or carry on peer deployment information.
 * <p>
 * This interface is intended to be used primarily by GridGain's code.
 * User's code can however implement this interface, for example, if it wraps a
 * closure or a predicate and wants to maintain its peer deployment
 * information so that the user class could be peer-deployed as well.
 */
public interface GridPeerDeployAware extends Serializable {
    /**
     * Gets top level user class being deployed.
     *
     * @return Top level user deployed class.
     */
    public Class<?> deployClass();

    /**
     * Gets class loader for the class. This class loader must be able to load
     * the class returned from {@link #deployClass()} as well as all of its
     * dependencies.
     * <p>
     * Note that in most cases the class loader returned from this method
     * and the class loader for the class returned from {@link #deployClass()} method
     * will be the same. If they are not the same, it is required that the class loader
     * returned from this method still has to be able to load the deploy class and all its
     * dependencies.
     *
     * @return Class loader for the class.
     */
    public ClassLoader classLoader();
}
