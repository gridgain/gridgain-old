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

import org.gridgain.grid.*;
import org.gridgain.grid.events.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.util.*;

/**
 * {@link GridEvents} implementation.
 */
public class GridEventsImpl implements GridEvents, Externalizable {
    /** */
    private static final long serialVersionUID = 0L;

    /** */
    private GridKernalContext ctx;

    /** */
    private GridProjection prj;

    /**
     * Required by {@link Externalizable}.
     */
    public GridEventsImpl() {
        // No-op.
    }

    /**
     * @param ctx Kernal context.
     * @param prj Projection.
     */
    public GridEventsImpl(GridKernalContext ctx, GridProjection prj) {
        this.ctx = ctx;
        this.prj = prj;
    }

    /** {@inheritDoc} */
    @Override public GridProjection projection() {
        return prj;
    }

    /** {@inheritDoc} */
    @Override public <T extends GridEvent> GridFuture<List<T>> remoteQuery(GridPredicate<T> p, long timeout,
        @Nullable int... types) {
        A.notNull(p, "p");

        guard();

        try {
            return ctx.event().remoteEventsAsync(compoundPredicate(p, types), prj.nodes(), timeout);
        }
        finally {
            unguard();
        }
    }

    /** {@inheritDoc} */
    @Override public <T extends GridEvent> GridFuture<UUID> remoteListen(@Nullable GridBiPredicate<UUID, T> locLsnr,
        @Nullable GridPredicate<T> rmtFilter, @Nullable int... types) {
        return remoteListen(1, 0, true, locLsnr, rmtFilter, types);
    }

    /** {@inheritDoc} */
    @Override public <T extends GridEvent> GridFuture<UUID> remoteListen(int bufSize, long interval,
        boolean autoUnsubscribe, @Nullable GridBiPredicate<UUID, T> locLsnr, @Nullable GridPredicate<T> rmtFilter,
        @Nullable int... types) {
        A.ensure(bufSize > 0, "bufSize > 0");
        A.ensure(interval >= 0, "interval >= 0");

        guard();

        try {
            return ctx.continuous().startRoutine(new GridEventConsumeHandler((GridBiPredicate<UUID, GridEvent>) locLsnr,
                (GridPredicate<GridEvent>) rmtFilter, types), bufSize, interval, autoUnsubscribe, prj.predicate());
        }
        finally {
            unguard();
        }
    }

    /** {@inheritDoc} */
    @Override public GridFuture<?> stopRemoteListen(UUID opId) {
        A.notNull(opId, "consumeId");

        guard();

        try {
            return ctx.continuous().stopRoutine(opId);
        }
        finally {
            unguard();
        }
    }

    /** {@inheritDoc} */
    @Override public <T extends GridEvent> GridFuture<T> waitForLocal(@Nullable GridPredicate<T> filter,
        @Nullable int... types) {
        guard();

        try {
            return ctx.event().waitForEvent(filter, types);
        }
        finally {
            unguard();
        }
    }

    /** {@inheritDoc} */
    @Override public <T extends GridEvent> Collection<T> localQuery(GridPredicate<T> p, @Nullable int... types) {
        A.notNull(p, "p");

        guard();

        try {
            return ctx.event().localEvents(compoundPredicate(p, types));
        }
        finally {
            unguard();
        }
    }

    /** {@inheritDoc} */
    @Override public void recordLocal(GridEvent evt) {
        A.notNull(evt, "evt");

        if (evt.type() <= 1000)
            throw new IllegalArgumentException("All types in range from 1 to 1000 are reserved for " +
                "internal GridGain events [evtType=" + evt.type() + ", evt=" + evt + ']');

        guard();

        try {
            ctx.event().record(evt);
        }
        finally {
            unguard();
        }
    }

    /** {@inheritDoc} */
    @Override public void localListen(GridPredicate<? extends GridEvent> lsnr, int[] types) {
        A.notNull(lsnr, "lsnr");
        A.notEmpty(types, "types");

        guard();

        try {
            ctx.event().addLocalEventListener(lsnr, types);
        }
        finally {
            unguard();
        }
    }

    /** {@inheritDoc} */
    @Override public boolean stopLocalListen(GridPredicate<? extends GridEvent> lsnr, @Nullable int... types) {
        A.notNull(lsnr, "lsnr");

        guard();

        try {
            return ctx.event().removeLocalEventListener(lsnr, types);
        }
        finally {
            unguard();
        }
    }

    /** {@inheritDoc} */
    @Override public void enableLocal(int[] types) {
        A.notEmpty(types, "types");

        guard();

        try {
            ctx.event().enableEvents(types);
        }
        finally {
            unguard();
        }
    }

    /** {@inheritDoc} */
    @Override public void disableLocal(int[] types) {
        A.notEmpty(types, "types");

        guard();

        try {
            ctx.event().disableEvents(types);
        }
        finally {
            unguard();
        }
    }

    /** {@inheritDoc} */
    @Override public int[] enabledEvents() {
        return ctx.event().enabledEvents();
    }

    /** {@inheritDoc} */
    @Override public boolean isEnabled(int type) {
        if (type < 0)
            throw new IllegalArgumentException("Invalid event type: " + type);

        return ctx.event().isUserRecordable(type);
    }

    /**
     * <tt>ctx.gateway().readLock()</tt>
     */
    private void guard() {
        ctx.gateway().readLock();
    }

    /**
     * <tt>ctx.gateway().readUnlock()</tt>
     */
    private void unguard() {
        ctx.gateway().readUnlock();
    }

    /**
     * @param p Predicate.
     * @param types Event types.
     * @return Compound predicate.
     */
    private static <T extends GridEvent> GridPredicate<T> compoundPredicate(final GridPredicate<T> p,
        @Nullable final int... types) {

        return F.isEmpty(types) ? p :
            new GridPredicate<T>() {
                @Override public boolean apply(T t) {
                    for (int type : types) {
                        if (type == t.type())
                            return p.apply(t);
                    }

                    return false;
                }
            };
    }

    /** {@inheritDoc} */
    @Override public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(prj);
    }

    /** {@inheritDoc} */
    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        prj = (GridProjection)in.readObject();
    }

    /**
     * Reconstructs object on unmarshalling.
     *
     * @return Reconstructed object.
     * @throws ObjectStreamException Thrown in case of unmarshalling error.
     */
    private Object readResolve() throws ObjectStreamException {
        return prj.events();
    }
}
