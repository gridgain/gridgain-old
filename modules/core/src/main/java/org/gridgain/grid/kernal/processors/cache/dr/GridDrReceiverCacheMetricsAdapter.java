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

package org.gridgain.grid.kernal.processors.cache.dr;

import org.gridgain.grid.dr.cache.receiver.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.jdk8.backport.*;
import org.jetbrains.annotations.*;

import java.io.*;

/**
 * Adapter for DR receive data node metrics.
 */
public class GridDrReceiverCacheMetricsAdapter implements GridDrReceiverCacheMetrics, Externalizable {
    /** */
    private static final long serialVersionUID = 0L;

    /** Total amount of received cache entries. */
    private LongAdder entriesReceived = new LongAdder();

    /** Total amount of conflicts resolved by using new value. */
    private LongAdder conflictNew = new LongAdder();

    /** Total amount of conflicts resolved by using old value. */
    private LongAdder conflictOld = new LongAdder();

    /** Total amount of conflicts resolved by merging values. */
    private LongAdder conflictMerge = new LongAdder();

    /**
     * No-args constructor.
     */
    public GridDrReceiverCacheMetricsAdapter() {
        // No-op.
    }

    /**
     * @param m Metrics to copy from.
     */
    GridDrReceiverCacheMetricsAdapter(GridDrReceiverCacheMetrics m) {
        entriesReceived.add(m.entriesReceived());
        conflictNew.add(m.conflictNew());
        conflictOld.add(m.conflictOld());
        conflictMerge.add(m.conflictMerge());
    }

    /** {@inheritDoc} */
    @Override public long entriesReceived() {
        return entriesReceived.longValue();
    }

    /** {@inheritDoc} */
    @Override public long conflictNew() {
        return conflictNew.longValue();
    }

    /** {@inheritDoc} */
    @Override public long conflictOld() {
        return conflictOld.longValue();
    }

    /** {@inheritDoc} */
    @Override public long conflictMerge() {
        return conflictMerge.longValue();
    }

    /**
     * Callback for conflict resolver on receiver cache side.
     *
     * @param usedNew New conflict status flag.
     * @param usedOld Old conflict status flag.
     * @param usedMerge Merge conflict status flag.
     */
    public void onReceiveCacheConflictResolved(boolean usedNew, boolean usedOld, boolean usedMerge) {
        if (usedNew)
            conflictNew.increment();
        else if (usedOld)
            conflictOld.increment();
        else if (usedMerge)
            conflictMerge.increment();
    }

    /**
     * Callback for received entries from receiver hub.
     *
     * @param entriesCnt Number of received entries.
     */
    public void onReceiveCacheEntriesReceived(int entriesCnt) {
        entriesReceived.add(entriesCnt);
    }

    /**
     * Create a copy of given metrics object.
     *
     * @param m Metrics to copy from.
     * @return Copy of given metrics.
     */
    @Nullable public static GridDrReceiverCacheMetricsAdapter copyOf(@Nullable GridDrReceiverCacheMetrics m) {
        if (m == null)
            return null;

        return new GridDrReceiverCacheMetricsAdapter(m);
    }

    /** {@inheritDoc} */
    @Override public void writeExternal(ObjectOutput out) throws IOException {
        out.writeLong(entriesReceived.longValue());
        out.writeLong(conflictNew.longValue());
        out.writeLong(conflictOld.longValue());
        out.writeLong(conflictMerge.longValue());
    }

    /** {@inheritDoc} */
    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        entriesReceived.add(in.readInt());
        conflictNew.add(in.readInt());
        conflictOld.add(in.readLong());
        conflictMerge.add(in.readInt());
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridDrReceiverCacheMetricsAdapter.class, this);
    }
}
