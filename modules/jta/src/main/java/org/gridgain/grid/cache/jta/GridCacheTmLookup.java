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

package org.gridgain.grid.cache.jta;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.cache.jta.jndi.*;
import org.gridgain.grid.cache.jta.reflect.*;
import org.jetbrains.annotations.*;

import javax.transaction.*;

/**
 * Allows grid to use different transactional systems. Implement this interface
 * to look up native transaction manager within your environment. Transaction
 * manager lookup is configured via {@link GridCacheConfiguration#getTransactionManagerLookupClassName()}
 * method.
 * <p>
 * The following implementations are provided out of the box:
 * <ul>
 * <li>
 *  {@link GridCacheJndiTmLookup} utilizes a configured JNDI name to look up a transaction manager.
 * </li>
 * <li>
 *  {@link GridCacheReflectionTmLookup} uses reflection to call a method on a given class
 *  to get to transaction manager.
 * </li>
 * </ul>
 */
public interface GridCacheTmLookup {
    /**
     * Gets Transaction Manager (TM).
     *
     * @return TM or {@code null} if TM cannot be looked up. 
     * @throws GridException In case of error.
     */
    @Nullable public TransactionManager getTm() throws GridException;
}
