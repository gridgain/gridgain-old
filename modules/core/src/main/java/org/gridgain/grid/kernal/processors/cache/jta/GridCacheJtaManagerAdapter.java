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

package org.gridgain.grid.kernal.processors.cache.jta;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.kernal.processors.cache.*;
import org.jetbrains.annotations.*;

/**
 * Provides possibility to integrate cache transactions with JTA.
 */
public abstract class GridCacheJtaManagerAdapter<K, V> extends GridCacheManagerAdapter<K, V> {
    /**
     * Creates transaction manager finder.
     *
     * @param ccfg Cache configuration.
     * @throws GridException If failed.
     */
    public abstract void createTmLookup(GridCacheConfiguration ccfg) throws GridException;

    /**
     * Checks if cache is working in JTA transaction and enlist cache as XAResource if necessary.
     *
     * @throws GridException In case of error.
     */
    public abstract void checkJta() throws GridException;

    /**
     * Gets transaction manager finder. Returns Object to avoid dependency on JTA library.
     *
     * @return Transaction manager finder.
     */
    @Nullable public abstract Object tmLookup();
}
