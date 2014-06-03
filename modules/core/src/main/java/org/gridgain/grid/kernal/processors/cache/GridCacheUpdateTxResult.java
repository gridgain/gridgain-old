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

import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.grid.util.tostring.*;
import org.jetbrains.annotations.*;

/**
 * Cache entry transactional update result.
 */
public class GridCacheUpdateTxResult<V> {
    /** Success flag.*/
    private final boolean success;

    /** Old value. */
    @GridToStringInclude
    private final V oldVal;

    /**
     * Constructor.
     *
     * @param success Success flag.
     * @param oldVal Old value (if any),
     */
    GridCacheUpdateTxResult(boolean success, @Nullable V oldVal) {
        this.success = success;
        this.oldVal = oldVal;
    }

    /**
     * @return Success flag.
     */
    public boolean success() {
        return success;
    }

    /**
     * @return Old value.
     */
    @Nullable public V oldValue() {
        return oldVal;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridCacheUpdateTxResult.class, this);
    }
}
