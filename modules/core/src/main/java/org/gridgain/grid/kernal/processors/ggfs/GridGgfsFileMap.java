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

package org.gridgain.grid.kernal.processors.ggfs;

import org.gridgain.grid.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.grid.util.tostring.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.util.*;

import static org.gridgain.grid.kernal.processors.ggfs.GridGgfsFileAffinityRange.*;

/**
 * Auxiliary class that is responsible for managing file affinity keys allocation by ranges.
 */
public class GridGgfsFileMap implements Externalizable {
    /** */
    private static final long serialVersionUID = 0L;

    @GridToStringInclude
    /** Sorted list of ranges in ascending order. */
    private List<GridGgfsFileAffinityRange> ranges;

    /**
     * Empty constructor.
     */
    public GridGgfsFileMap() {
        // No-op.
    }

    /**
     * Constructs same file map as passed in.
     *
     * @param old Old map.
     */
    public GridGgfsFileMap(@Nullable GridGgfsFileMap old) {
        if (old != null && old.ranges != null) {
            ranges = new ArrayList<>(old.ranges.size());

            ranges.addAll(old.ranges);
        }
    }

    /**
     * Gets affinity key from file map based on block start offset.
     *
     * @param blockOff Block start offset (divisible by block size).
     * @param includeMoved If {@code true} then will return affinity key for ranges marked as moved.
     *      Otherwise will return null for such ranges.
     * @return Affinity key.
     */
    public GridUuid affinityKey(long blockOff, boolean includeMoved) {
        if (ranges == null)
            return null;

        assert !ranges.isEmpty();

        // Range binary search.
        int leftIdx = 0, rightIdx = ranges.size() - 1;

        GridGgfsFileAffinityRange leftRange = ranges.get(leftIdx);
        GridGgfsFileAffinityRange rightRange = ranges.get(rightIdx);

        // If block offset is less than start of first range, we don't have affinity key.
        if (leftRange.less(blockOff))
            return null;

        if (leftRange.belongs(blockOff))
            return leftRange.status() != RANGE_STATUS_MOVED ? leftRange.affinityKey() :
                includeMoved ? leftRange.affinityKey() : null;

        if (rightRange.greater(blockOff))
            return null;

        if (rightRange.belongs(blockOff))
            return rightRange.status() != RANGE_STATUS_MOVED ? rightRange.affinityKey() :
                includeMoved ? leftRange.affinityKey() : null;

        while (rightIdx - leftIdx > 1) {
            int midIdx = (leftIdx + rightIdx) / 2;

            GridGgfsFileAffinityRange midRange = ranges.get(midIdx);

            if (midRange.belongs(blockOff))
                return midRange.status() != RANGE_STATUS_MOVED ? midRange.affinityKey() :
                    includeMoved ? leftRange.affinityKey() : null;

            // If offset is less then block start, update right index.
            if (midRange.less(blockOff))
                rightIdx = midIdx;
            else {
                assert midRange.greater(blockOff);

                leftIdx = midIdx;
            }
        }

        // Range was not found.
        return null;
    }

    /**
     * Updates range status in file map. Will split range into two ranges if given range is a sub-range starting
     * from the same offset.
     *
     * @param range Range to update status.
     * @param status New range status.
     * @throws GridException If range was not found.
     */
    public void updateRangeStatus(GridGgfsFileAffinityRange range, int status) throws GridException {
        if (ranges == null)
            throw new GridGgfsInvalidRangeException("Failed to update range status (file map is empty) " +
                "[range=" + range + ", ranges=" + ranges + ']');

        assert !ranges.isEmpty();

        // Check last.
        int lastIdx = ranges.size() - 1;

        GridGgfsFileAffinityRange last = ranges.get(lastIdx);

        if (last.startOffset() == range.startOffset()) {
            updateRangeStatus0(lastIdx, last, range, status);

            return;
        }

        // Check first.
        int firstIdx = 0;

        GridGgfsFileAffinityRange first = ranges.get(firstIdx);

        if (first.startOffset() == range.startOffset()) {
            updateRangeStatus0(firstIdx, first, range, status);

            return;
        }

        // Binary search.
        while (lastIdx - firstIdx > 1) {
            int midIdx = (firstIdx + lastIdx) / 2;

            GridGgfsFileAffinityRange midRange = ranges.get(midIdx);

            if (midRange.startOffset() == range.startOffset()) {
                updateRangeStatus0(midIdx, midRange, range, status);

                return;
            }

            // If range we are looking for is less
            if (midRange.less(range.startOffset()))
                lastIdx = midIdx;
            else {
                assert midRange.greater(range.startOffset());

                firstIdx = midIdx;
            }
        }

        throw new GridGgfsInvalidRangeException("Failed to update map for range (corresponding map range " +
            "was not found) [range=" + range + ", status=" + status + ", ranges=" + ranges + ']');
    }

    /**
     * Deletes range from map.
     *
     * @param range Range to delete.
     */
    public void deleteRange(GridGgfsFileAffinityRange range) throws GridException {
        if (ranges == null)
            throw new GridGgfsInvalidRangeException("Failed to remove range (file map is empty) " +
                "[range=" + range + ", ranges=" + ranges + ']');

        assert !ranges.isEmpty();

        try {
            // Check last.
            int lastIdx = ranges.size() - 1;

            GridGgfsFileAffinityRange last = ranges.get(lastIdx);

            if (last.regionEqual(range)) {
                assert last.status() == RANGE_STATUS_MOVED;

                ranges.remove(last);

                return;
            }

            // Check first.
            int firstIdx = 0;

            GridGgfsFileAffinityRange first = ranges.get(firstIdx);

            if (first.regionEqual(range)) {
                assert first.status() == RANGE_STATUS_MOVED;

                ranges.remove(first);

                return;
            }

            // Binary search.
            while (lastIdx - firstIdx > 1) {
                int midIdx = (firstIdx + lastIdx) / 2;

                GridGgfsFileAffinityRange midRange = ranges.get(midIdx);

                if (midRange.regionEqual(range)) {
                    assert midRange.status() == RANGE_STATUS_MOVED;

                    ranges.remove(midIdx);

                    return;
                }

                // If range we are looking for is less
                if (midRange.less(range.startOffset()))
                    lastIdx = midIdx;
                else {
                    assert midRange.greater(range.startOffset());

                    firstIdx = midIdx;
                }
            }
        }
        finally {
            if (ranges.isEmpty())
                ranges = null;
        }

        throw new GridGgfsInvalidRangeException("Failed to remove range from file map (corresponding map range " +
            "was not found) [range=" + range + ", ranges=" + ranges + ']');
    }

    /**
     * Updates range status at given position (will split range into two if necessary).
     *
     * @param origIdx Original range index.
     * @param orig Original range at index.
     * @param update Range being updated.
     * @param status New status for range.
     */
    private void updateRangeStatus0(int origIdx, GridGgfsFileAffinityRange orig, GridGgfsFileAffinityRange update,
        int status) {
        assert F.eq(orig.affinityKey(), update.affinityKey());
        assert ranges.get(origIdx) == orig;

        if (orig.regionEqual(update))
            ranges.set(origIdx, new GridGgfsFileAffinityRange(update, status));
        else {
            // If range was expanded, new one should be larger.
            assert orig.endOffset() > update.endOffset();

            ranges.set(origIdx, new GridGgfsFileAffinityRange(update, status));
            ranges.add(origIdx + 1, new GridGgfsFileAffinityRange(update.endOffset() + 1, orig.endOffset(),
                orig.affinityKey()));
        }
    }

    /**
     * Gets full list of ranges present in this map.
     *
     * @return Unmodifiable list of ranges.
     */
    public List<GridGgfsFileAffinityRange> ranges() {
        if (ranges == null)
            return Collections.emptyList();

        return Collections.unmodifiableList(ranges);
    }

    /**
     * Adds range to the list of already existing ranges. Added range must be located after
     * the last range in this map. If added range is adjacent to the last range in the map,
     * added range will be concatenated to the last one.
     *
     * @param range Range to add.
     */
    public void addRange(GridGgfsFileAffinityRange range) {
        if (range == null || range.empty())
            return;

        // We cannot add range in the middle of the file.
        if (ranges == null) {
            ranges = new ArrayList<>();

            ranges.add(range);

            return;
        }

        assert !ranges.isEmpty();

        GridGgfsFileAffinityRange last = ranges.get(ranges.size() - 1);

        // Ensure that range being added is located to the right of last range in list.
        assert last.greater(range.startOffset()) : "Cannot add range to middle of map [last=" + last +
            ", range=" + range + ']';

        // Try to concat last and new range.
        GridGgfsFileAffinityRange concat = last.concat(range);

        // Simply add range to the end of the list if they are not adjacent.
        if (concat == null)
            ranges.add(range);
        else
            ranges.set(ranges.size() - 1, concat);
    }

    /** {@inheritDoc} */
    @Override public void writeExternal(ObjectOutput out) throws IOException {
        if (ranges == null)
            out.writeInt(-1);
        else {
            assert !ranges.isEmpty();

            out.writeInt(ranges.size());

            for (GridGgfsFileAffinityRange range : ranges)
                out.writeObject(range);
        }
    }

    /** {@inheritDoc} */
    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        int size = in.readInt();

        if (size > 0) {
            ranges = new ArrayList<>(size);

            for (int i = 0; i < size; i++)
                ranges.add((GridGgfsFileAffinityRange)in.readObject());
        }
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridGgfsFileMap.class, this);
    }
}
