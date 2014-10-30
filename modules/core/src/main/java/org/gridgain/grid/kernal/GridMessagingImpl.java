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
import org.gridgain.grid.kernal.processors.continuous.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.messaging.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.util.*;

/**
 * {@link GridMessaging} implementation.
 */
public class GridMessagingImpl implements GridMessaging, Externalizable {
    /** */
    private static final long serialVersionUID = 0L;

    /** */
    private GridKernalContext ctx;

    /** */
    private GridProjection prj;

    /**
     * Required by {@link Externalizable}.
     */
    public GridMessagingImpl() {
        // No-op.
    }

    /**
     * @param ctx Kernal context.
     * @param prj Projection.
     */
    public GridMessagingImpl(GridKernalContext ctx, GridProjection prj) {
        this.ctx = ctx;
        this.prj = prj;
    }

    /** {@inheritDoc} */
    @Override public GridProjection projection() {
        return prj;
    }

    /** {@inheritDoc} */
    @Override public void send(@Nullable Object topic, Object msg) throws GridException {
        A.notNull(msg, "msg");

        guard();

        try {
            Collection<GridNode> snapshot = prj.nodes();

            if (snapshot.isEmpty())
                throw U.emptyTopologyException();

            ctx.io().sendUserMessage(snapshot, msg, topic, false, 0);
        }
        finally {
            unguard();
        }
    }

    /** {@inheritDoc} */
    @Override public void send(@Nullable Object topic, Collection<?> msgs) throws GridException {
        A.ensure(!F.isEmpty(msgs), "msgs cannot be null or empty");

        guard();

        try {
            Collection<GridNode> snapshot = prj.nodes();

            if (snapshot.isEmpty())
                throw U.emptyTopologyException();

            for (Object msg : msgs) {
                A.notNull(msg, "msg");

                ctx.io().sendUserMessage(snapshot, msg, topic, false, 0);
            }
        }
        finally {
            unguard();
        }
    }

    /** {@inheritDoc} */
    @Override public void sendOrdered(@Nullable Object topic, Object msg, long timeout) throws GridException {
        A.notNull(msg, "msg");

        guard();

        try {
            Collection<GridNode> snapshot = prj.nodes();

            if (snapshot.isEmpty())
                throw U.emptyTopologyException();

            if (timeout == 0)
                timeout = ctx.config().getNetworkTimeout();

            ctx.io().sendUserMessage(snapshot, msg, topic, true, timeout);
        }
        finally {
            unguard();
        }
    }

    /** {@inheritDoc} */
    @Override public void localListen(@Nullable Object topic, GridBiPredicate<UUID, ?> p) {
        A.notNull(p, "p");

        guard();

        try {
            ctx.io().addUserMessageListener(topic, p);
        }
        finally {
            unguard();
        }
    }

    /** {@inheritDoc} */
    @Override public void stopLocalListen(@Nullable Object topic, GridBiPredicate<UUID, ?> p) {
        A.notNull(p, "p");

        guard();

        try {
            ctx.io().removeUserMessageListener(topic, p);
        }
        finally {
            unguard();
        }
    }

    /** {@inheritDoc} */
    @Override public GridFuture<UUID> remoteListen(@Nullable Object topic, GridBiPredicate<UUID, ?> p) {
        A.notNull(p, "p");

        guard();

        try {
            GridContinuousHandler hnd = new GridMessageListenHandler(topic, (GridBiPredicate<UUID, Object>)p);

            return ctx.continuous().startRoutine(hnd, 1, 0, false, prj.predicate());
        }
        finally {
            unguard();
        }
    }

    /** {@inheritDoc} */
    @Override public GridFuture<?> stopRemoteListen(UUID opId) {
        A.notNull(opId, "opId");

        return ctx.continuous().stopRoutine(opId);
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
        return prj.message();
    }
}
