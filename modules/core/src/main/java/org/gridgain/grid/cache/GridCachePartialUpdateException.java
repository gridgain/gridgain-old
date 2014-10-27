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

package org.gridgain.grid.cache;

import org.gridgain.grid.*;

import java.util.*;

/**
 * Exception thrown from non-transactional cache in case when update succeeded only partially.
 * One can get list of keys for which update failed with method {@link #failedKeys()}.
 */
public class GridCachePartialUpdateException extends GridException {
    /** */
    private static final long serialVersionUID = 0L;

    /** Failed keys. */
    private final Collection<Object> failedKeys = new ArrayList<>();

    /**
     * @param msg Error message.
     */
    public GridCachePartialUpdateException(String msg) {
        super(msg);
    }

    /**
     * Gets collection of failed keys.
     * @return Collection of failed keys.
     */
    public <K> Collection<K> failedKeys() {
        return (Collection<K>)failedKeys;
    }

    /**
     * @param failedKeys Failed keys.
     * @param err Error.
     */
    public void add(Collection<?> failedKeys, Throwable err) {
        this.failedKeys.addAll(failedKeys);

        addSuppressed(err);
    }

    @Override public String getMessage() {
        return super.getMessage() + ": " + failedKeys;
    }
}
