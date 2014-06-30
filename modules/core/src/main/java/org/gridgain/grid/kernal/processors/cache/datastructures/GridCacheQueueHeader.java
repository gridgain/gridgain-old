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

package org.gridgain.grid.kernal.processors.cache.datastructures;

import org.gridgain.grid.*;
import org.gridgain.grid.kernal.processors.cache.*;
import org.gridgain.grid.util.tostring.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.util.*;

/**
 * Queue header.
 */
public class GridCacheQueueHeader implements GridCacheInternal, Externalizable {
    /** */
    private static final long serialVersionUID = 0L;

    /** */
    private GridUuid id;

    /** */
    private long head;

    /** */
    private long tail;

    /** */
    private int cap;

    /** */
    private boolean collocated;

    /** */
    @GridToStringInclude
    private Set<Long> rmvIdxs;

    /**
     * Required by {@link Externalizable}.
     */
    public GridCacheQueueHeader() {
        // No-op.
    }

    /**
     * @param id Queue unique ID.
     * @param cap Capacity.
     * @param collocated Collocation flag.
     * @param head Queue head index.
     * @param tail Queue tail index.
     * @param rmvIdxs Indexes of removed items.
     */
    public GridCacheQueueHeader(GridUuid id, int cap, boolean collocated, long head, long tail,
        @Nullable Set<Long> rmvIdxs) {
        assert id != null;
        assert head <= tail;

        this.id = id;
        this.cap = cap;
        this.collocated = collocated;
        this.head = head;
        this.tail = tail;
        this.rmvIdxs = rmvIdxs;
    }

    /**
     * @return Queue unique ID.
     */
    public GridUuid id() {
        return id;
    }

    /**
     * @return Capacity.
     */
    public int capacity() {
        return cap;
    }

    /**
     * @return Queue collocation flag.
     */
    public boolean collocated() {
        return collocated;
    }

    /**
     * @return Head index.
     */
    public long head() {
        return head;
    }

    /**
     * @return Tail index.
     */
    public long tail() {
        return tail;
    }

    /**
     * @return {@code True} if queue is bounded.
     */
    public boolean bounded() {
        return cap < Integer.MAX_VALUE;
    }

    /**
     * @return {@code True} if queue is empty.
     */
    public boolean empty() {
        return head == tail;
    }

    /**
     * @return {@code True} if queue is full.
     */
    public boolean full() {
        return bounded() && size() == capacity();
    }

    /**
     * @return Queue size.
     */
    public int size() {
        int rmvSize = F.isEmpty(removedIndexes()) ? 0 : removedIndexes().size();

        int size = (int)(tail() - head() - rmvSize);

        assert size >= 0 : size;

        return size;
    }

    /**
     * @return Indexes of removed items.
     */
    @Nullable public Set<Long> removedIndexes() {
        return rmvIdxs;
    }

    /** {@inheritDoc} */
    @Override public void writeExternal(ObjectOutput out) throws IOException {
        U.writeGridUuid(out, id);
        out.writeInt(cap);
        out.writeBoolean(collocated);
        out.writeLong(head);
        out.writeLong(tail);
        out.writeBoolean(rmvIdxs != null);

        if (rmvIdxs != null) {
            out.writeInt(rmvIdxs.size());

            for (Long idx : rmvIdxs)
                out.writeLong(idx);
        }
    }

    /** {@inheritDoc} */
    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        id = U.readGridUuid(in);
        cap = in.readInt();
        collocated = in.readBoolean();
        head = in.readLong();
        tail = in.readLong();

        if (in.readBoolean()) {
            int size = in.readInt();

            rmvIdxs = new HashSet<>();

            for (int i = 0; i < size; i++)
                rmvIdxs.add(in.readLong());
        }
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridCacheQueueHeader.class, this);
    }
}
