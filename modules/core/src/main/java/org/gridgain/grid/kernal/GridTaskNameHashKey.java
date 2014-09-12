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

package org.gridgain.grid.kernal;

import org.gridgain.grid.kernal.processors.cache.*;

import java.io.*;

/**
 * Keys for task name hashing.
 */
public class GridTaskNameHashKey implements Externalizable, GridCacheInternal {
    /** Serial version UID. */
    private static final long serialVersionUID = 0L;

    /** Task name hash. */
    private int taskNameHash;

    /**
     * Empty constructor required by {@link Externalizable}.
     */
    public GridTaskNameHashKey() {
        // No-op.
    }

    /**
     * @param taskNameHash Task name hash.
     */
    public GridTaskNameHashKey(int taskNameHash) {
        this.taskNameHash = taskNameHash;
    }

    /** {@inheritDoc} */
    @Override public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(taskNameHash);
    }

    /** {@inheritDoc} */
    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        taskNameHash = in.readInt();
    }

    /** {@inheritDoc} */
    @Override public boolean equals(Object o) {
        if (this == o)
            return true;

        if (!(o instanceof GridTaskNameHashKey))
            return false;

        GridTaskNameHashKey that = (GridTaskNameHashKey)o;

        return taskNameHash == that.taskNameHash;
    }

    /** {@inheritDoc} */
    @Override public int hashCode() {
        return taskNameHash;
    }
}
