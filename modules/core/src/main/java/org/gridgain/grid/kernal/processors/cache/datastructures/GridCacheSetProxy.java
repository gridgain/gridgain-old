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
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Cache set proxy.
 */
public class GridCacheSetProxy<T> implements GridCacheSet<T>, Externalizable {
    /** */
    private static final long serialVersionUID = 0L;

    /** Deserialization stash. */
    private static final ThreadLocal<GridBiTuple<GridCacheContext, String>> stash =
        new ThreadLocal<GridBiTuple<GridCacheContext, String>>() {
            @Override protected GridBiTuple<GridCacheContext, String> initialValue() {
                return F.t2();
            }
        };

    /** Delegate set. */
    private GridCacheSetImpl<T> delegate;

    /** Cache context. */
    private GridCacheContext cctx;

    /** Cache gateway. */
    private GridCacheGateway gate;

    /** Busy lock. */
    private GridSpinBusyLock busyLock;

    /**
     * Required by {@link Externalizable}.
     */
    public GridCacheSetProxy() {
        // No-op.
    }

    /**
     * @param cctx Cache context.
     * @param delegate Delegate set.
     */
    public GridCacheSetProxy(GridCacheContext cctx, GridCacheSetImpl<T> delegate) {
        this.cctx = cctx;
        this.delegate = delegate;

        gate = cctx.gate();

        busyLock = new GridSpinBusyLock();
    }

    /**
     * Remove callback.
     */
    void blockOnRemove() {
        delegate.removed(true);

        busyLock.block();
    }

    /** {@inheritDoc} */
    @Override public int size() {
        enterBusy();

        try {
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
        finally {
            leaveBusy();
        }
    }

    /** {@inheritDoc} */
    @Override public boolean isEmpty() {
        enterBusy();

        try {
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
        finally {
            leaveBusy();
        }
    }

    /** {@inheritDoc} */
    @Override public boolean contains(final Object o) {
        enterBusy();

        try {
            gate.enter();

            try {
                if (cctx.transactional())
                    return CU.outTx(new Callable<Boolean>() {
                        @Override public Boolean call() throws Exception {
                            return delegate.contains(o);
                        }
                    }, cctx);

                return delegate.contains(o);
            }
            catch (GridException e) {
                throw new GridRuntimeException(e);
            }
            finally {
                gate.leave();
            }
        }
        finally {
            leaveBusy();
        }
    }

    /** {@inheritDoc} */
    @NotNull @Override public Object[] toArray() {
        enterBusy();

        try {
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
        finally {
            leaveBusy();
        }
    }

    /** {@inheritDoc} */
    @NotNull @Override public <T1> T1[] toArray(final T1[] a) {
        enterBusy();

        try {
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
        finally {
            leaveBusy();
        }
    }

    /** {@inheritDoc} */
    @Override public boolean add(final T t) {
        enterBusy();

        try {
            gate.enter();

            try {
                if (cctx.transactional())
                    return CU.outTx(new Callable<Boolean>() {
                        @Override public Boolean call() throws Exception {
                            return delegate.add(t);
                        }
                    }, cctx);

                return delegate.add(t);
            }
            catch (GridException e) {
                throw new GridRuntimeException(e);
            }
            finally {
                gate.leave();
            }
        }
        finally {
            leaveBusy();
        }
    }

    /** {@inheritDoc} */
    @Override public boolean remove(final Object o) {
        enterBusy();

        try {
            gate.enter();

            try {
                if (cctx.transactional())
                    return CU.outTx(new Callable<Boolean>() {
                        @Override public Boolean call() throws Exception {
                            return delegate.remove(o);
                        }
                    }, cctx);

                return delegate.remove(o);
            }
            catch (GridException e) {
                throw new GridRuntimeException(e);
            }
            finally {
                gate.leave();
            }
        }
        finally {
            leaveBusy();
        }
    }

    /** {@inheritDoc} */
    @Override public boolean containsAll(final Collection<?> c) {
        enterBusy();

        try {
            gate.enter();

            try {
                if (cctx.transactional())
                    return CU.outTx(new Callable<Boolean>() {
                        @Override public Boolean call() throws Exception {
                            return delegate.containsAll(c);
                        }
                    }, cctx);

                return delegate.containsAll(c);
            }
            catch (GridException e) {
                throw new GridRuntimeException(e);
            }
            finally {
                gate.leave();
            }
        }
        finally {
            leaveBusy();
        }
    }

    /** {@inheritDoc} */
    @Override public boolean addAll(final Collection<? extends T> c) {
        enterBusy();

        try {
            gate.enter();

            try {
                if (cctx.transactional())
                    return CU.outTx(new Callable<Boolean>() {
                        @Override public Boolean call() throws Exception {
                            return delegate.addAll(c);
                        }
                    }, cctx);

                return delegate.addAll(c);
            }
            catch (GridException e) {
                throw new GridRuntimeException(e);
            }
            finally {
                gate.leave();
            }
        }
        finally {
            leaveBusy();
        }
    }

    /** {@inheritDoc} */
    @Override public boolean retainAll(final Collection<?> c) {
        enterBusy();

        try {
            gate.enter();

            try {
                if (cctx.transactional())
                    return CU.outTx(new Callable<Boolean>() {
                        @Override public Boolean call() throws Exception {
                            return delegate.retainAll(c);
                        }
                    }, cctx);

                return delegate.retainAll(c);
            }
            catch (GridException e) {
                throw new GridRuntimeException(e);
            }
            finally {
                gate.leave();
            }
        }
        finally {
            leaveBusy();
        }
    }

    /** {@inheritDoc} */
    @Override public boolean removeAll(final Collection<?> c) {
        enterBusy();

        try {
            gate.enter();

            try {
                if (cctx.transactional())
                    return CU.outTx(new Callable<Boolean>() {
                        @Override public Boolean call() throws Exception {
                            return delegate.removeAll(c);
                        }
                    }, cctx);

                return delegate.removeAll(c);
            }
            catch (GridException e) {
                throw new GridRuntimeException(e);
            }
            finally {
                gate.leave();
            }
        }
        finally {
            leaveBusy();
        }
    }

    /** {@inheritDoc} */
    @Override public void clear() {
        enterBusy();

        try {
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
        finally {
            leaveBusy();
        }
    }

    /** {@inheritDoc} */
    @Override public Iterator<T> iterator() {
        enterBusy();

        try {
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
        finally {
            leaveBusy();
        }
    }

    /** {@inheritDoc} */
    @Override public String name() {
        return delegate.name();
    }

    /** {@inheritDoc} */
    @Override public boolean collocated() throws GridException {
        return delegate.collocated();
    }

    /** {@inheritDoc} */
    @Override public boolean removed() {
        return delegate.removed();
    }

    /**
     * Enters busy state.
     */
    private void enterBusy() {
        if (!busyLock.enterBusy())
            throw new GridCacheDataStructureRemovedRuntimeException("Set has been removed from cache: " + delegate);
    }

    /**
     * Leaves busy state.
     */
    private void leaveBusy() {
        busyLock.leaveBusy();
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

            return t.get1().dataStructures().set(t.get2(), false, false);
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
