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

package org.gridgain.grid.kernal.processors.cache;

import java.io.*;

/**
 * Key for system utility cache.
 */
public abstract class GridCacheUtilityKey<K extends GridCacheUtilityKey> implements GridCacheInternal, Serializable {
    /** */
    private static final long serialVersionUID = 0L;

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked") @Override
    public final boolean equals(Object obj) {
        return obj == this || obj != null && obj.getClass() == getClass() && equalsx((K)obj);
    }

    /**
     * Child-specific equals method.
     *
     * @param key Key.
     * @return {@code True} if equals.
     */
    protected abstract boolean equalsx(K key);

    /** {@inheritDoc} */
    public abstract int hashCode();
}
