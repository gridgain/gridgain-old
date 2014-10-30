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
import org.gridgain.grid.cache.*;
import org.gridgain.grid.cache.affinity.*;
import org.gridgain.grid.cache.datastructures.*;
import org.gridgain.grid.kernal.processors.cache.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.util.tostring.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import static java.util.concurrent.TimeUnit.*;

/**
 * Common code for {@link GridCacheQueue} implementation.
 */
public abstract class GridCacheQueueAdapter<T> extends AbstractCollection<T> implements GridCacheQueue<T> {
    /** Value returned by closure updating queue header indicating that queue was removed. */
    protected static final long QUEUE_REMOVED_IDX = Long.MIN_VALUE;

    /** */
    protected static final int MAX_UPDATE_RETRIES = 100;

    /** */
    protected static final long RETRY_DELAY = 1;

    /** */
    private static final int DFLT_CLEAR_BATCH_SIZE = 100;

    /** Logger. */
    protected final GridLogger log;

    /** Cache context. */
    protected final GridCacheContext<?, ?> cctx;

    /** Cache. */
    protected final GridCacheAdapter cache;

    /** Queue name. */
    protected final String queueName;

    /** Queue header key. */
    protected final GridCacheQueueHeaderKey queueKey;

    /** Queue unique ID. */
    protected final GridUuid id;

    /** Queue capacity. */
    private final int cap;

    /** Collocation flag. */
    private final boolean collocated;

    /** Removed flag. */
    private volatile boolean rmvd;

    /** Read blocking operations semaphore. */
    @GridToStringExclude
    private final Semaphore readSem;

    /** Write blocking operations semaphore. */
    @GridToStringExclude
    private final Semaphore writeSem;

    /**
     * @param queueName Queue name.
     * @param hdr Queue hdr.
     * @param cctx Cache context.
     */
    @SuppressWarnings("unchecked")
    protected GridCacheQueueAdapter(String queueName, GridCacheQueueHeader hdr, GridCacheContext<?, ?> cctx) {
        this.cctx = cctx;
        this.queueName = queueName;
        id = hdr.id();
        cap = hdr.capacity();
        collocated = hdr.collocated();
        queueKey = new GridCacheQueueHeaderKey(queueName);
        cache = cctx.cache();

        log = cctx.logger(getClass());

        readSem = new Semaphore(hdr.size(), true);

        writeSem = bounded() ? new Semaphore(hdr.capacity() - hdr.size(), true) : null;
    }

    /** {@inheritDoc} */
    @Override public String name() {
        return queueName;
    }

    /** {@inheritDoc} */
    @Override public boolean add(T item) {
        A.notNull(item, "item");

        return offer(item);
    }

    /** {@inheritDoc} */
    @Override public boolean collocated() {
        return collocated;
    }

    /** {@inheritDoc} */
    @Override public int capacity() throws GridException {
        return cap;
    }

    /** {@inheritDoc} */
    @Override public boolean bounded() {
        return cap < Integer.MAX_VALUE;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override public int size() {
        try {
            GridCacheQueueHeader hdr = (GridCacheQueueHeader)cache.get(queueKey);

            checkRemoved(hdr);

            return hdr.size();
        }
        catch (GridException e) {
            throw new GridRuntimeException(e);
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Nullable @Override public T peek() throws GridRuntimeException {
        try {
            GridCacheQueueHeader hdr = (GridCacheQueueHeader)cache.get(queueKey);

            checkRemoved(hdr);

            if (hdr.empty())
                return null;

            return (T)cache.get(itemKey(hdr.head()));
        }
        catch (GridException e) {
            throw new GridRuntimeException(e);
        }
    }

    /** {@inheritDoc} */
    @Override public T remove() {
        T res = poll();

        if (res == null)
            throw new NoSuchElementException();

        return res;
    }

    /** {@inheritDoc} */
    @Override public T element() {
        T el = peek();

        if (el == null)
            throw new NoSuchElementException();

        return el;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override public Iterator<T> iterator() {
        try {
            GridCacheQueueHeader hdr = (GridCacheQueueHeader)cache.get(queueKey);

            checkRemoved(hdr);

            return new QueueIterator(hdr);
        }
        catch (GridException e) {
            throw new GridRuntimeException(e);
        }
    }

    /** {@inheritDoc} */
    @Override public void put(T item) throws GridRuntimeException {
        A.notNull(item, "item");

        if (!bounded()) {
            boolean offer = offer(item);

            assert offer;

            return;
        }

        while (true) {
            try {
                writeSem.acquire();
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();

                throw new GridRuntimeException("Queue put interrupted.", e);
            }

            checkStopping();

            if (offer(item))
                return;
        }
    }

    /** {@inheritDoc} */
    @Override public boolean offer(T item, long timeout, TimeUnit unit) throws GridRuntimeException {
        A.notNull(item, "item");
        A.ensure(timeout >= 0, "Timeout cannot be negative: " + timeout);

        if (!bounded()) {
            boolean offer = offer(item);

            assert offer;

            return true;
        }

        long end = U.currentTimeMillis() + MILLISECONDS.convert(timeout, unit);

        while (U.currentTimeMillis() < end) {
            boolean retVal = false;

            try {
                if (writeSem.tryAcquire(end - U.currentTimeMillis(), MILLISECONDS)) {
                    checkStopping();

                    retVal = offer(item);
                }
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();

                throw new GridRuntimeException("Queue put interrupted.", e);
            }

            if (retVal)
                return true;
        }

        return false;
    }

    /** {@inheritDoc} */
    @Nullable @Override public T take() throws GridRuntimeException {
        while (true) {
            try {
                readSem.acquire();
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();

                throw new GridRuntimeException("Queue take interrupted.", e);
            }

            checkStopping();

            T e = poll();

            if (e != null)
                return e;
        }
    }

    /** {@inheritDoc} */
    @Nullable @Override public T poll(long timeout, TimeUnit unit) throws GridRuntimeException {
        A.ensure(timeout >= 0, "Timeout cannot be negative: " + timeout);

        long end = U.currentTimeMillis() + MILLISECONDS.convert(timeout, unit);

        while (U.currentTimeMillis() < end) {
            T retVal = null;

            try {
                if (readSem.tryAcquire(end - U.currentTimeMillis(), MILLISECONDS)) {
                    checkStopping();

                    retVal = poll();
                }
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();

                throw new GridRuntimeException("Queue poll interrupted.", e);
            }

            if (retVal != null)
                return retVal;
        }

        return null;
    }

    /** {@inheritDoc} */
    @Override public int remainingCapacity() {
        if (!bounded())
            return Integer.MAX_VALUE;

        int remaining = cap - size();

        return remaining > 0 ? remaining : 0;
    }

    /** {@inheritDoc} */
    @Override public void clear() {
        clear(DFLT_CLEAR_BATCH_SIZE);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override public void clear(int batchSize) throws GridRuntimeException {
        A.ensure(batchSize >= 0, "Batch size cannot be negative: " + batchSize);

        try {
            GridBiTuple<Long, Long> t = (GridBiTuple<Long, Long>)cache.transformAndCompute(queueKey,
                new ClearClosure(id));

            if (t == null)
                return;

            checkRemoved(t.get1());

            removeKeys(cache, id, queueName, collocated, t.get1(), t.get2(), batchSize);
        }
        catch (GridException e) {
            throw new GridRuntimeException(e);
        }
    }

    /** {@inheritDoc} */
    @Override public int drainTo(Collection<? super T> c) {
        return drainTo(c, Integer.MAX_VALUE);
    }

    /** {@inheritDoc} */
    @Override public int drainTo(Collection<? super T> c, int maxElements) {
        int max = Math.min(maxElements, size());

        for (int i = 0; i < max; i++) {
            T el = poll();

            if (el == null)
                return i;

            c.add(el);
        }

        return max;
    }

    /** {@inheritDoc} */
    @Override public boolean removed() {
        return rmvd;
    }

    /**
     * @param cache Cache.
     * @param id Queue unique ID.
     * @param name Queue name.
     * @param collocated Collocation flag.
     * @param startIdx Start item index.
     * @param endIdx End item index.
     * @param batchSize Batch size.
     * @throws GridException If failed.
     */
    @SuppressWarnings("unchecked")
    static void removeKeys(GridCacheProjection cache, GridUuid id, String name, boolean collocated, long startIdx,
        long endIdx, int batchSize) throws GridException {
        Collection<GridCacheQueueItemKey> keys = new ArrayList<>(batchSize > 0 ? batchSize : 10);

        for (long idx = startIdx; idx < endIdx; idx++) {
            keys.add(itemKey(id, name, collocated, idx));

            if (batchSize > 0 && keys.size() == batchSize) {
                cache.removeAll(keys);

                keys.clear();
            }
        }

        if (!keys.isEmpty())
            cache.removeAll(keys);
    }

    /**
     * Checks result of closure modifying queue header, throws {@link GridCacheDataStructureRemovedRuntimeException}
     * if queue was removed.
     *
     * @param idx Result of closure execution.
     */
    protected final void checkRemoved(Long idx) {
        if (idx == QUEUE_REMOVED_IDX)
            onRemoved(true);
    }

    /**
     * Checks queue state, throws {@link GridCacheDataStructureRemovedRuntimeException} if queue was removed.
     *
     * @param hdr Queue hdr.
     */
    protected final void checkRemoved(@Nullable GridCacheQueueHeader hdr) {
        if (queueRemoved(hdr, id))
            onRemoved(true);
    }

    /**
     * Marks queue as removed.
     *
     * @param throw0 If {@code true} then throws {@link GridCacheDataStructureRemovedRuntimeException}.
     */
    void onRemoved(boolean throw0) {
        rmvd = true;

        releaseSemaphores();

        if (throw0)
            throw new GridCacheDataStructureRemovedRuntimeException("Queue has been removed from cache: " + this);
    }

    /**
     * Release all semaphores used in blocking operations (used in case queue was removed or grid is stopping).
     */
    private void releaseSemaphores() {
        if (bounded()) {
            writeSem.drainPermits();
            writeSem.release(1_000_000); // Let all blocked threads to proceed (operation will fail with exception).
        }

        readSem.drainPermits();
        readSem.release(1_000_000); // Let all blocked threads to proceed (operation will fail with exception).
    }

    /**
     * @param hdr Queue header.
     */
    void onHeaderChanged(GridCacheQueueHeader hdr) {
        if (!hdr.empty()) {
            readSem.drainPermits();
            readSem.release(hdr.size());
        }

        if (bounded()) {
            writeSem.drainPermits();

            if (!hdr.full())
                writeSem.release(hdr.capacity() - hdr.size());
        }
    }

    /**
     * Grid stop callback.
     */
    void onKernalStop() {
        releaseSemaphores();
    }

    /**
     * Throws {@link GridRuntimeException} in case if grid is stopping.
     */
    private void checkStopping() {
        if (cctx.kernalContext().isStopping())
            throw new GridRuntimeException("Grid is stopping");
    }

    /**
     * @return Queue unique ID.
     */
    GridUuid id() {
        return id;
    }

    /**
     * Removes item with given index from queue.
     *
     * @param rmvIdx Index of item to be removed.
     * @throws GridException If failed.
     */
    protected abstract void removeItem(long rmvIdx) throws GridException;


    /**
     * @param idx Item index.
     * @return Item key.
     */
    protected GridCacheQueueItemKey itemKey(Long idx) {
        return itemKey(id, queueName, collocated(), idx);
    }

    /**
     * @param id Queue unique ID.
     * @param queueName Queue name.
     * @param collocated Collocation flag.
     * @param idx Item index.
     * @return Item key.
     */
    private static GridCacheQueueItemKey itemKey(GridUuid id, String queueName, boolean collocated, long idx) {
        return collocated ? new CollocatedItemKey(id, queueName, idx) :
            new GridCacheQueueItemKey(id, queueName, idx);
    }

    /**
     * @param hdr Queue header.
     * @param id Expected queue unique ID.
     * @return {@code True} if queue was removed.
     */
    private static boolean queueRemoved(@Nullable GridCacheQueueHeader hdr, GridUuid id) {
        return hdr == null || !id.equals(hdr.id());
    }

    /**
     */
    private class QueueIterator implements Iterator<T> {
        /** */
        private T next;

        /** */
        private T cur;

        /** */
        private long curIdx;

        /** */
        private long idx;

        /** */
        private long endIdx;

        /** */
        private Set<Long> rmvIdxs;

        /**
         * @param hdr Queue header.
         * @throws GridException If failed.
         */
        @SuppressWarnings("unchecked")
        private QueueIterator(GridCacheQueueHeader hdr) throws GridException {
            idx = hdr.head();
            endIdx = hdr.tail();
            rmvIdxs = hdr.removedIndexes();

            assert !F.contains(rmvIdxs, idx) : idx;

            if (idx < endIdx)
                next = (T)cache.get(itemKey(idx));
        }

        /** {@inheritDoc} */
        @Override public boolean hasNext() {
            return next != null;
        }

        /** {@inheritDoc} */
        @SuppressWarnings("unchecked")
        @Override public T next() {
            if (next == null)
                throw new NoSuchElementException();

            try {
                cur = next;
                curIdx = idx;

                idx++;

                if (rmvIdxs != null) {
                    while (F.contains(rmvIdxs, idx) && idx < endIdx)
                        idx++;
                }

                next = idx < endIdx ? (T)cache.get(itemKey(idx)) : null;

                return cur;
            }
            catch (GridException e) {
                throw new GridRuntimeException(e);
            }
        }

        /** {@inheritDoc} */
        @Override public void remove() {
            if (cur == null)
                throw new IllegalStateException();

            try {
                removeItem(curIdx);

                cur = null;
            }
            catch (GridException e) {
                throw new GridRuntimeException(e);
            }
        }
    }

    /**
     * Item key for collocated queue.
     */
    private static class CollocatedItemKey extends GridCacheQueueItemKey {
        /** */
        private static final long serialVersionUID = 0L;

        /**
         * Required by {@link Externalizable}.
         */
        public CollocatedItemKey() {
            // No-op.
        }

        /**
         * @param id Queue unique ID.
         * @param queueName Queue name.
         * @param idx Item index.
         */
        private CollocatedItemKey(GridUuid id, String queueName, long idx) {
            super(id, queueName, idx);
        }

        /**
         * @return Item affinity key.
         */
        @GridCacheAffinityKeyMapped
        public Object affinityKey() {
            return queueName();
        }
    }

    /**
     */
    protected static class ClearClosure implements GridClosure<GridCacheQueueHeader,
        GridBiTuple<GridCacheQueueHeader, GridBiTuple<Long, Long>>>,
        Externalizable {
        /** */
        private static final long serialVersionUID = 0L;

        /** */
        private GridUuid id;

        /**
         * Required by {@link Externalizable}.
         */
        public ClearClosure() {
            // No-op.
        }

        /**
         * @param id Queue unique ID.
         */
        public ClearClosure(GridUuid id) {
            this.id = id;
        }

        /** {@inheritDoc} */
        @Override public GridBiTuple<GridCacheQueueHeader, GridBiTuple<Long, Long>> apply(GridCacheQueueHeader hdr) {
            boolean rmvd = queueRemoved(hdr, id);

            if (rmvd)
                return new GridBiTuple<>(hdr, new GridBiTuple<>(QUEUE_REMOVED_IDX, QUEUE_REMOVED_IDX));
            else if (hdr.empty())
                return new GridBiTuple<>(hdr, null);

            GridCacheQueueHeader newHdr = new GridCacheQueueHeader(hdr.id(), hdr.capacity(), hdr.collocated(),
                hdr.tail(), hdr.tail(), null);

            return new GridBiTuple<>(newHdr, new GridBiTuple<>(hdr.head(), hdr.tail()));
        }

        /** {@inheritDoc} */
        @Override public void writeExternal(ObjectOutput out) throws IOException {
            U.writeGridUuid(out, id);
        }

        /** {@inheritDoc} */
        @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            id = U.readGridUuid(in);
        }
    }

    /**
     */
    protected static class PollClosure implements
        GridClosure<GridCacheQueueHeader, GridBiTuple<GridCacheQueueHeader, Long>>, Externalizable {
        /** */
        private static final long serialVersionUID = 0L;

        /** */
        private GridUuid id;

        /**
         * Required by {@link Externalizable}.
         */
        public PollClosure() {
            // No-op.
        }

        /**
         * @param id Queue unique ID.
         */
        public PollClosure(GridUuid id) {
            this.id = id;
        }

        /** {@inheritDoc} */
        @Override public GridBiTuple<GridCacheQueueHeader, Long> apply(GridCacheQueueHeader hdr) {
            boolean rmvd = queueRemoved(hdr, id);

            if (rmvd || hdr.empty())
                return new GridBiTuple<>(hdr, rmvd ? QUEUE_REMOVED_IDX : null);

            Set<Long> rmvdIdxs = hdr.removedIndexes();

            if (rmvdIdxs == null) {
                GridCacheQueueHeader newHdr = new GridCacheQueueHeader(hdr.id(), hdr.capacity(), hdr.collocated(),
                    hdr.head() + 1, hdr.tail(), rmvdIdxs);

                return new GridBiTuple<>(newHdr, hdr.head());
            }

            long next = hdr.head() + 1;

            rmvdIdxs = new HashSet<>(rmvdIdxs);

            do {
                if (!rmvdIdxs.remove(next)) {
                    GridCacheQueueHeader newHdr = new GridCacheQueueHeader(hdr.id(), hdr.capacity(), hdr.collocated(),
                        next + 1, hdr.tail(), rmvdIdxs.isEmpty() ? null : rmvdIdxs);

                    return new GridBiTuple<>(newHdr, next);
                }

                next++;
            } while (next != hdr.tail());

            GridCacheQueueHeader newHdr = new GridCacheQueueHeader(hdr.id(), hdr.capacity(), hdr.collocated(),
                next, hdr.tail(), rmvdIdxs.isEmpty() ? null : rmvdIdxs);

            return new GridBiTuple<>(newHdr, null);
        }

        /** {@inheritDoc} */
        @Override public void writeExternal(ObjectOutput out) throws IOException {
            U.writeGridUuid(out, id);
        }

        /** {@inheritDoc} */
        @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            id = U.readGridUuid(in);
        }
    }

    /**
     */
    protected static class AddClosure implements
        GridClosure<GridCacheQueueHeader, GridBiTuple<GridCacheQueueHeader, Long>>, Externalizable {
        /** */
        private static final long serialVersionUID = 0L;

        /** */
        private GridUuid id;

        /** */
        private int size;

        /**
         * Required by {@link Externalizable}.
         */
        public AddClosure() {
            // No-op.
        }

        /**
         * @param id Queue unique ID.
         * @param size Number of elements to add.
         */
        public AddClosure(GridUuid id, int size) {
            this.id = id;
            this.size = size;
        }

        /** {@inheritDoc} */
        @Override public GridBiTuple<GridCacheQueueHeader, Long> apply(GridCacheQueueHeader hdr) {
            boolean rmvd = queueRemoved(hdr, id);

            if (rmvd || !spaceAvailable(hdr, size))
                return new GridBiTuple<>(hdr, rmvd ? QUEUE_REMOVED_IDX : null);

            GridCacheQueueHeader newHdr = new GridCacheQueueHeader(hdr.id(), hdr.capacity(), hdr.collocated(),
                hdr.head(), hdr.tail() + size, hdr.removedIndexes());

            return new GridBiTuple<>(newHdr, hdr.tail());
        }

        /**
         * @param hdr Queue header.
         * @param size Number of elements to add.
         * @return {@code True} if new elements can be added.
         */
        private boolean spaceAvailable(GridCacheQueueHeader hdr, int size) {
            return !hdr.bounded() || (hdr.size() + size) <= hdr.capacity();
        }

        /** {@inheritDoc} */
        @Override public void writeExternal(ObjectOutput out) throws IOException {
            U.writeGridUuid(out, id);
            out.writeInt(size);
        }

        /** {@inheritDoc} */
        @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            id = U.readGridUuid(in);
            size = in.readInt();
        }
    }

    /**
     */
    protected static class RemoveClosure implements
        GridClosure<GridCacheQueueHeader, GridBiTuple<GridCacheQueueHeader, Long>>, Externalizable {
        /** */
        private static final long serialVersionUID = 0L;

        /** */
        private GridUuid id;

        /** */
        private Long idx;

        /**
         * Required by {@link Externalizable}.
         */
        public RemoveClosure() {
            // No-op.
        }

        /**
         * @param id Queue UUID.
         * @param idx Index of item to be removed.
         */
        public RemoveClosure(GridUuid id, Long idx) {
            this.id = id;
            this.idx = idx;
        }

        /** {@inheritDoc} */
        @Override public GridBiTuple<GridCacheQueueHeader, Long> apply(GridCacheQueueHeader hdr) {
            boolean rmvd = queueRemoved(hdr, id);

            if (rmvd || hdr.empty() || idx < hdr.head())
                return new GridBiTuple<>(hdr, rmvd ? QUEUE_REMOVED_IDX : null);

            if (idx == hdr.head()) {
                Set<Long> rmvIdxs = hdr.removedIndexes();

                long head = hdr.head() + 1;

                if (!F.contains(rmvIdxs, head)) {
                    GridCacheQueueHeader newHdr = new GridCacheQueueHeader(hdr.id(), hdr.capacity(), hdr.collocated(),
                        head, hdr.tail(), hdr.removedIndexes());

                    return new GridBiTuple<>(newHdr, idx);
                }

                rmvIdxs = new HashSet<>(rmvIdxs);

                while (rmvIdxs.remove(head))
                    head++;

                GridCacheQueueHeader newHdr = new GridCacheQueueHeader(hdr.id(), hdr.capacity(), hdr.collocated(),
                    head, hdr.tail(), rmvIdxs.isEmpty() ? null : rmvIdxs);

                return new GridBiTuple<>(newHdr, null);
            }

            Set<Long> rmvIdxs = hdr.removedIndexes();

            if (rmvIdxs == null) {
                rmvIdxs = new HashSet<>();

                rmvIdxs.add(idx);
            }
            else {
                if (!rmvIdxs.contains(idx)) {
                    rmvIdxs = new HashSet<>(rmvIdxs);

                    rmvIdxs.add(idx);
                }
                else
                    idx = null;
            }

            GridCacheQueueHeader newHdr = new GridCacheQueueHeader(hdr.id(), hdr.capacity(), hdr.collocated(),
                hdr.head(), hdr.tail(), rmvIdxs);

            return new GridBiTuple<>(newHdr, idx);
        }

        /** {@inheritDoc} */
        @Override public void writeExternal(ObjectOutput out) throws IOException {
            U.writeGridUuid(out, id);
            out.writeLong(idx);
        }

        /** {@inheritDoc} */
        @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            id = U.readGridUuid(in);
            idx = in.readLong();
        }
    }

    /** {@inheritDoc} */
    @Override public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        GridCacheQueueAdapter that = (GridCacheQueueAdapter) o;

        return id.equals(that.id);

    }

    /** {@inheritDoc} */
    @Override public int hashCode() {
        return id.hashCode();
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridCacheQueueAdapter.class, this);
    }
}
