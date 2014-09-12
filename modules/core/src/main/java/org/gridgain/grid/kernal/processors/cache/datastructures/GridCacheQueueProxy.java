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
import org.gridgain.grid.cache.datastructures.*;
import org.gridgain.grid.kernal.processors.cache.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Cache queue proxy.
 */
public class GridCacheQueueProxy<T> implements GridCacheQueue<T>, Externalizable {
    /** */
    private static final long serialVersionUID = 0L;

    /** Deserialization stash. */
    private static final ThreadLocal<GridBiTuple<GridCacheContext, String>> stash =
        new ThreadLocal<GridBiTuple<GridCacheContext, String>>() {
            @Override protected GridBiTuple<GridCacheContext, String> initialValue() {
                return F.t2();
            }
        };

    /** Delegate queue. */
    private GridCacheQueueAdapter<T> delegate;

    /** Cache context. */
    private GridCacheContext cctx;

    /** Cache gateway. */
    private GridCacheGateway gate;

    /**
     * Required by {@link Externalizable}.
     */
    public GridCacheQueueProxy() {
        // No-op.
    }

    /**
     * @param cctx Cache context.
     * @param delegate Delegate queue.
     */
    public GridCacheQueueProxy(GridCacheContext cctx, GridCacheQueueAdapter<T> delegate) {
        this.cctx = cctx;
        this.delegate = delegate;

        gate = cctx.gate();
    }

    /**
     * @return Delegate queue.
     */
    public GridCacheQueueAdapter<T> delegate() {
        return delegate;
    }

    /** {@inheritDoc} */
    @Override public boolean add(final T item) {
        gate.enter();

        try {
            if (cctx.transactional())
                return CU.outTx(new Callable<Boolean>() {
                    @Override public Boolean call() throws Exception {
                        return delegate.add(item);
                    }
                }, cctx);

            return delegate.add(item);
        }
        catch (GridException e) {
            throw new GridRuntimeException(e);
        }
        finally {
            gate.leave();
        }
    }

    /** {@inheritDoc} */
    @Override public boolean offer(final T item) {
        gate.enter();

        try {
            if (cctx.transactional())
                return CU.outTx(new Callable<Boolean>() {
                    @Override public Boolean call() throws Exception {
                        return delegate.offer(item);
                    }
                }, cctx);

            return delegate.offer(item);
        }
        catch (GridException e) {
            throw new GridRuntimeException(e);
        }
        finally {
            gate.leave();
        }
    }

    /** {@inheritDoc} */
    @Override public boolean addAll(final Collection<? extends T> items) {
        gate.enter();

        try {
            if (cctx.transactional())
                return CU.outTx(new Callable<Boolean>() {
                    @Override public Boolean call() throws Exception {
                        return delegate.addAll(items);
                    }
                }, cctx);

            return delegate.addAll(items);
        }
        catch (GridException e) {
            throw new GridRuntimeException(e);
        }
        finally {
            gate.leave();
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("SuspiciousMethodCalls")
    @Override public boolean contains(final Object item) {
        gate.enter();

        try {
            if (cctx.transactional())
                return CU.outTx(new Callable<Boolean>() {
                    @Override public Boolean call() throws Exception {
                        return delegate.contains(item);
                    }
                }, cctx);

            return delegate.contains(item);
        }
        catch (GridException e) {
            throw new GridRuntimeException(e);
        }
        finally {
            gate.leave();
        }
    }

    /** {@inheritDoc} */
    @Override public boolean containsAll(final Collection<?> items) {
        gate.enter();

        try {
            if (cctx.transactional())
                return CU.outTx(new Callable<Boolean>() {
                    @Override public Boolean call() throws Exception {
                        return delegate.containsAll(items);
                    }
                }, cctx);

            return delegate.containsAll(items);
        }
        catch (GridException e) {
            throw new GridRuntimeException(e);
        }
        finally {
            gate.leave();
        }
    }

    /** {@inheritDoc} */
    @Override public void clear() {
        gate.enter();

        try {
            if (cctx.transactional()) {
                CU.outTx(new Callable<Void>() {
                    @Override public Void call() throws Exception {
                        delegate.clear();

                        return null;
                    }
                }, cctx);
            }
            else
                delegate.clear();
        }
        catch (GridException e) {
            throw new GridRuntimeException(e);
        }
        finally {
            gate.leave();
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("SuspiciousMethodCalls")
    @Override public boolean remove(final Object item) {
        gate.enter();

        try {
            if (cctx.transactional())
                return CU.outTx(new Callable<Boolean>() {
                    @Override public Boolean call() throws Exception {
                        return delegate.remove(item);
                    }
                }, cctx);

            return delegate.remove(item);
        }
        catch (GridException e) {
            throw new GridRuntimeException(e);
        }
        finally {
            gate.leave();
        }
    }

    /** {@inheritDoc} */
    @Override public boolean removeAll(final Collection<?> items) {
        gate.enter();

        try {
            if (cctx.transactional())
                return CU.outTx(new Callable<Boolean>() {
                    @Override public Boolean call() throws Exception {
                        return delegate.removeAll(items);
                    }
                }, cctx);

            return delegate.removeAll(items);
        }
        catch (GridException e) {
            throw new GridRuntimeException(e);
        }
        finally {
            gate.leave();
        }
    }

    /** {@inheritDoc} */
    @Override public boolean isEmpty() {
        gate.enter();

        try {
            if (cctx.transactional())
                return CU.outTx(new Callable<Boolean>() {
                    @Override public Boolean call() throws Exception {
                        return delegate.isEmpty();
                    }
                }, cctx);

            return delegate.isEmpty();
        }
        catch (GridException e) {
            throw new GridRuntimeException(e);
        }
        finally {
            gate.leave();
        }
    }

    /** {@inheritDoc} */
    @Override public Iterator<T> iterator() {
        gate.enter();

        try {
            if (cctx.transactional())
                return CU.outTx(new Callable<Iterator<T>>() {
                    @Override public Iterator<T> call() throws Exception {
                        return delegate.iterator();
                    }
                }, cctx);

            return delegate.iterator();
        }
        catch (GridException e) {
            throw new GridRuntimeException(e);
        }
        finally {
            gate.leave();
        }
    }

    /** {@inheritDoc} */
    @Override public Object[] toArray() {
        gate.enter();

        try {
            if (cctx.transactional())
                return CU.outTx(new Callable<Object[]>() {
                    @Override public Object[] call() throws Exception {
                        return delegate.toArray();
                    }
                }, cctx);

            return delegate.toArray();
        }
        catch (GridException e) {
            throw new GridRuntimeException(e);
        }
        finally {
            gate.leave();
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("SuspiciousToArrayCall")
    @Override public <T1> T1[] toArray(final T1[] a) {
        gate.enter();

        try {
            if (cctx.transactional())
                return CU.outTx(new Callable<T1[]>() {
                    @Override public T1[] call() throws Exception {
                        return delegate.toArray(a);
                    }
                }, cctx);

            return delegate.toArray(a);
        }
        catch (GridException e) {
            throw new GridRuntimeException(e);
        }
        finally {
            gate.leave();
        }
    }

    /** {@inheritDoc} */
    @Override public boolean retainAll(final Collection<?> items) {
        gate.enter();

        try {
            if (cctx.transactional())
                return CU.outTx(new Callable<Boolean>() {
                    @Override public Boolean call() throws Exception {
                        return delegate.retainAll(items);
                    }
                }, cctx);

            return delegate.retainAll(items);
        }
        catch (GridException e) {
            throw new GridRuntimeException(e);
        }
        finally {
            gate.leave();
        }
    }

    /** {@inheritDoc} */
    @Override public int size() {
        gate.enter();

        try {
            if (cctx.transactional())
                return CU.outTx(new Callable<Integer>() {
                    @Override public Integer call() throws Exception {
                        return delegate.size();
                    }
                }, cctx);

            return delegate.size();
        }
        catch (GridException e) {
            throw new GridRuntimeException(e);
        }
        finally {
            gate.leave();
        }
    }

    /** {@inheritDoc} */
    @Nullable @Override public T poll() {
        gate.enter();

        try {
            if (cctx.transactional())
                return CU.outTx(new Callable<T>() {
                    @Override public T call() throws Exception {
                        return delegate.poll();
                    }
                }, cctx);

            return delegate.poll();
        }
        catch (GridException e) {
            throw new GridRuntimeException(e);
        }
        finally {
            gate.leave();
        }
    }

    /** {@inheritDoc} */
    @Nullable @Override public T peek() {
        gate.enter();

        try {
            if (cctx.transactional())
                return CU.outTx(new Callable<T>() {
                    @Override public T call() throws Exception {
                        return delegate.peek();
                    }
                }, cctx);

            return delegate.peek();
        }
        catch (GridException e) {
            throw new GridRuntimeException(e);
        }
        finally {
            gate.leave();
        }
    }

    /** {@inheritDoc} */
    @Override public void clear(final int batchSize) {
        gate.enter();

        try {
            if (cctx.transactional()) {
                CU.outTx(new Callable<Void>() {
                    @Override public Void call() throws Exception {
                        delegate.clear(batchSize);

                        return null;
                    }
                }, cctx);
            }
            else
                delegate.clear(batchSize);
        }
        catch (GridException e) {
            throw new GridRuntimeException(e);
        }
        finally {
            gate.leave();
        }
    }

    /** {@inheritDoc} */
    @Override public int remainingCapacity() {
        gate.enter();

        try {
            if (cctx.transactional())
                return CU.outTx(new Callable<Integer>() {
                    @Override public Integer call() throws Exception {
                        return delegate.remainingCapacity();
                    }
                }, cctx);

            return delegate.remainingCapacity();
        }
        catch (GridException e) {
            throw new GridRuntimeException(e);
        }
        finally {
            gate.leave();
        }
    }

    /** {@inheritDoc} */
    @Override public int drainTo(final Collection<? super T> c) {
        gate.enter();

        try {
            if (cctx.transactional())
                return CU.outTx(new Callable<Integer>() {
                    @Override public Integer call() throws Exception {
                        return delegate.drainTo(c);
                    }
                }, cctx);

            return delegate.drainTo(c);
        }
        catch (GridException e) {
            throw new GridRuntimeException(e);
        }
        finally {
            gate.leave();
        }
    }

    /** {@inheritDoc} */
    @Override public int drainTo(final Collection<? super T> c, final int maxElements) {
        gate.enter();

        try {
            if (cctx.transactional())
                return CU.outTx(new Callable<Integer>() {
                    @Override public Integer call() throws Exception {
                        return delegate.drainTo(c, maxElements);
                    }
                }, cctx);

            return delegate.drainTo(c, maxElements);
        }
        catch (GridException e) {
            throw new GridRuntimeException(e);
        }
        finally {
            gate.leave();
        }
    }

    /** {@inheritDoc} */
    @Override public T remove() {
        gate.enter();

        try {
            if (cctx.transactional())
                return CU.outTx(new Callable<T>() {
                    @Override public T call() throws Exception {
                        return delegate.remove();
                    }
                }, cctx);

            return delegate.remove();
        }
        catch (GridException e) {
            throw new GridRuntimeException(e);
        }
        finally {
            gate.leave();
        }
    }

    /** {@inheritDoc} */
    @Override public T element() {
        gate.enter();

        try {
            if (cctx.transactional())
                return CU.outTx(new Callable<T>() {
                    @Override public T call() throws Exception {
                        return delegate.element();
                    }
                }, cctx);

            return delegate.element();
        }
        catch (GridException e) {
            throw new GridRuntimeException(e);
        }
        finally {
            gate.leave();
        }
    }

    /** {@inheritDoc} */
    @Override public void put(final T item) {
        gate.enter();

        try {
            if (cctx.transactional()) {
                CU.outTx(new Callable<Void>() {
                    @Override public Void call() throws Exception {
                        delegate.put(item);

                        return null;
                    }
                }, cctx);
            }
            else
                delegate.put(item);
        }
        catch (GridException e) {
            throw new GridRuntimeException(e);
        }
        finally {
            gate.leave();
        }
    }

    /** {@inheritDoc} */
    @Override public boolean offer(final T item, final long timeout, final TimeUnit unit) {
        gate.enter();

        try {
            if (cctx.transactional())
                return CU.outTx(new Callable<Boolean>() {
                    @Override public Boolean call() throws Exception {
                        return delegate.offer(item, timeout, unit);
                    }
                }, cctx);

            return delegate.offer(item, timeout, unit);
        }
        catch (GridException e) {
            throw new GridRuntimeException(e);
        }
        finally {
            gate.leave();
        }
    }

    /** {@inheritDoc} */
    @Nullable @Override public T take() {
        gate.enter();

        try {
            if (cctx.transactional())
                return CU.outTx(new Callable<T>() {
                    @Override public T call() throws Exception {
                        return delegate.take();
                    }
                }, cctx);

            return delegate.take();
        }
        catch (GridException e) {
            throw new GridRuntimeException(e);
        }
        finally {
            gate.leave();
        }
    }

    /** {@inheritDoc} */
    @Nullable @Override public T poll(final long timeout, final TimeUnit unit) {
        gate.enter();

        try {
            if (cctx.transactional())
                return CU.outTx(new Callable<T>() {
                    @Override public T call() throws Exception {
                        return delegate.poll(timeout, unit);
                    }
                }, cctx);

            return delegate.poll(timeout, unit);
        }
        catch (GridException e) {
            throw new GridRuntimeException(e);
        }
        finally {
            gate.leave();
        }
    }

    /** {@inheritDoc} */
    @Override public String name() {
        return delegate.name();
    }

    /** {@inheritDoc} */
    @Override public int capacity() throws GridException {
        return delegate.capacity();
    }

    /** {@inheritDoc} */
    @Override public boolean bounded() throws GridException {
        return delegate.bounded();
    }

    /** {@inheritDoc} */
    @Override public boolean collocated() throws GridException {
        return delegate.collocated();
    }

    /** {@inheritDoc} */
    @Override public boolean removed() {
        return delegate.removed();
    }

    /** {@inheritDoc} */
    @Override public int hashCode() {
        return delegate.hashCode();
    }

    /** {@inheritDoc} */
    @Override public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        GridCacheQueueProxy that = (GridCacheQueueProxy)o;

        return delegate.equals(that.delegate);
    }

    /** {@inheritDoc} */
    @Override public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(cctx);
        U.writeString(out, name());
    }

    /** {@inheritDoc} */
    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        GridBiTuple<GridCacheContext, String> t = stash.get();

        t.set1((GridCacheContext)in.readObject());
        t.set2(U.readString(in));
    }

    /**
     * Reconstructs object on unmarshalling.
     *
     * @return Reconstructed object.
     * @throws ObjectStreamException Thrown in case of unmarshalling error.
     */
    protected Object readResolve() throws ObjectStreamException {
        try {
            GridBiTuple<GridCacheContext, String> t = stash.get();

            return t.get1().dataStructures().queue(t.get2(), 0, false, false);
        }
        catch (GridException e) {
            throw U.withCause(new InvalidObjectException(e.getMessage()), e);
        }
        finally {
            stash.remove();
        }
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return delegate.toString();
    }
}
