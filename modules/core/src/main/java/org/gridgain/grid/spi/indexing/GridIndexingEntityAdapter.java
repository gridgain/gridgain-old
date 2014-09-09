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

package org.gridgain.grid.spi.indexing;

import org.gridgain.grid.marshaller.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.grid.util.tostring.*;
import org.jetbrains.annotations.*;

/**
 * Convenience adapter for {@link GridIndexingEntity}.
 */
public class GridIndexingEntityAdapter<T> implements GridIndexingEntity<T> {
    /** */
    @GridToStringInclude
    private final T val;

    /** */
    @GridToStringExclude
    private final byte[] bytes;

    /**
     * @param val Value.
     * @param bytes Value marshalled by {@link GridMarshaller}.
     */
    public GridIndexingEntityAdapter(T val, @Nullable byte[] bytes) {
        this.val = val;
        this.bytes = bytes;
    }

    /** {@inheritDoc} */
    @Override public T value() {
        return val;
    }

    /** {@inheritDoc} */
    @Override public byte[] bytes() {
        return bytes;
    }

    /** {@inheritDoc} */
    @Override public boolean hasValue() {
        return val != null || (val == null && bytes == null);
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridIndexingEntityAdapter.class, this,
            "bytesLength", (bytes == null ? 0 : bytes.length));
    }
}
