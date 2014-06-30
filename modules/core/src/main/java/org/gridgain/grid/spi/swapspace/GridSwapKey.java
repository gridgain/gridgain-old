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

package org.gridgain.grid.spi.swapspace;

import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.grid.util.tostring.*;
import org.jetbrains.annotations.*;

/**
 * Utility wrapper class that represents swap key.
 * <p>
 * This class also holds information about partition this key belongs to
 * (if needed for caches).
 */
public class GridSwapKey {
    /** */
    @GridToStringInclude
    private final Object key;

    /** */
    private final int part;

    /** Serialized key. */
    @GridToStringExclude
    private byte[] keyBytes;

    /**
     * @param key Key.
     */
    public GridSwapKey(Object key) {
        this(key, Integer.MAX_VALUE, null);
    }

    /**
     * @param key Key.
     * @param part Partition.
     */
    public GridSwapKey(Object key, int part) {
        this(key, part, null);
    }

    /**
     * @param key Key.
     * @param part Part.
     * @param keyBytes Key bytes.
     */
    public GridSwapKey(Object key, int part, @Nullable byte[] keyBytes) {
        assert key != null;
        assert part >= 0;

        this.key = key;
        this.part = part;
        this.keyBytes = keyBytes;
    }

    /**
     * @return Key.
     */
    public Object key() {
        return key;
    }

    /**
     * @return Partition this key belongs to.
     */
    public int partition() {
        return part;
    }

    /**
     * @return Serialized key.
     */
    @Nullable public byte[] keyBytes() {
        return keyBytes;
    }

    /**
     * @param keyBytes Serialized key.
     */
    public void keyBytes(byte[] keyBytes) {
        assert keyBytes != null;

        this.keyBytes = keyBytes;
    }

    /** {@inheritDoc} */
    @Override public boolean equals(Object obj) {
        if (obj == this)
            return true;

        if (obj instanceof GridSwapKey) {
            GridSwapKey other = (GridSwapKey)obj;

            return part == other.part && key.equals(other.key);
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override public int hashCode() {
        return key.hashCode();
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridSwapKey.class, this);
    }
}
