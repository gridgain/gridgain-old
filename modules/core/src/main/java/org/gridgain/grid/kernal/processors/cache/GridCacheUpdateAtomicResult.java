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

import org.gridgain.grid.kernal.processors.dr.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.grid.util.tostring.*;
import org.jetbrains.annotations.*;

/**
 * Cache entry atomic update result.
 */
public class GridCacheUpdateAtomicResult<K, V> {
    /** Success flag.*/
    private final boolean success;

    /** Old value. */
    @GridToStringInclude
    private final V oldVal;

    /** New value. */
    @GridToStringInclude
    private final V newVal;

    /** New TTL. */
    private final long newTtl;

    /** Explicit DR expire time (if any). */
    private final long drExpireTime;

    /** Version for deferred delete. */
    @GridToStringInclude
    private final GridCacheVersion rmvVer;

    /** DR conflict resolution context. */
    @GridToStringInclude
    private final GridDrReceiverConflictContextImpl<K, V> drConflictCtx;

    /** Whether update should be propagated to DHT node. */
    private final boolean sndToDht;

    /**
     * Constructor.
     *
     * @param success Success flag.
     * @param oldVal Old value.
     * @param newVal New value.
     * @param newTtl New TTL.
     * @param drExpireTime Explict DR expire time (if any).
     * @param rmvVer Version for deferred delete.
     * @param drConflictCtx DR conflict resolution context.
     * @param sndToDht Whether update should be propagated to DHT node.
     */
    public GridCacheUpdateAtomicResult(boolean success, @Nullable V oldVal, @Nullable V newVal, long newTtl,
        long drExpireTime, @Nullable GridCacheVersion rmvVer,
        @Nullable GridDrReceiverConflictContextImpl<K, V> drConflictCtx, boolean sndToDht) {
        this.success = success;
        this.oldVal = oldVal;
        this.newVal = newVal;
        this.newTtl = newTtl;
        this.drExpireTime = drExpireTime;
        this.rmvVer = rmvVer;
        this.drConflictCtx = drConflictCtx;
        this.sndToDht = sndToDht;
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

    /**
     * @return New value.
     */
    @Nullable public V newValue() {
        return newVal;
    }

    /**
     * @return New TTL.
     */
    public long newTtl() {
        return newTtl;
    }

    /**
     * @return Explicit DR expire time (if any).
     */
    public long drExpireTime() {
        return drExpireTime;
    }

    /**
     * @return Version for deferred delete.
     */
    @Nullable public GridCacheVersion removeVersion() {
        return rmvVer;
    }

    /**
     * @return DR conflict resolution context.
     */
    @Nullable public GridDrReceiverConflictContextImpl<K, V> drConflictContext() {
        return drConflictCtx;
    }

    /**
     * @return Whether update should be propagated to DHT node.
     */
    public boolean sendToDht() {
        return sndToDht;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridCacheUpdateAtomicResult.class, this);
    }
}
