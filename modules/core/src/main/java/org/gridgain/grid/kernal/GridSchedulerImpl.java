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
import org.gridgain.grid.scheduler.*;
import org.gridgain.grid.util.typedef.internal.*;

import java.io.*;
import java.util.concurrent.*;

/**
 * {@link GridScheduler} implementation.
 */
public class GridSchedulerImpl implements GridScheduler, Externalizable {
    /** */
    private static final long serialVersionUID = 0L;

    /** */
    private GridKernalContext ctx;

    /**
     * Required by {@link Externalizable}.
     */
    public GridSchedulerImpl() {
        // No-op.
    }

    /**
     * @param ctx Kernal context.
     */
    public GridSchedulerImpl(GridKernalContext ctx) {
        this.ctx = ctx;
    }

    /** {@inheritDoc} */
    @Override public GridFuture<?> runLocal(Runnable r) {
        A.notNull(r, "r");

        guard();

        try {
            return ctx.closure().runLocalSafe(r, false);
        }
        finally {
            unguard();
        }
    }

    /** {@inheritDoc} */
    @Override public <R> GridFuture<R> callLocal(Callable<R> c) {
        A.notNull(c, "c");

        guard();

        try {
            return ctx.closure().callLocalSafe(c, false);
        }
        finally {
            unguard();
        }
    }

    /** {@inheritDoc} */
    @Override public GridSchedulerFuture<?> scheduleLocal(Runnable job, String ptrn) {
        A.notNull(job, "job");

        guard();

        try {
            return ctx.schedule().schedule(job, ptrn);
        }
        finally {
            unguard();
        }
    }

    /** {@inheritDoc} */
    @Override public <R> GridSchedulerFuture<R> scheduleLocal(Callable<R> job, String ptrn) {
        A.notNull(job, "job");

        guard();

        try {
            return ctx.schedule().schedule(job, ptrn);
        }
        finally {
            unguard();
        }
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
        out.writeObject(ctx);
    }

    /** {@inheritDoc} */
    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        ctx = (GridKernalContext)in.readObject();
    }

    /**
     * Reconstructs object on unmarshalling.
     *
     * @return Reconstructed object.
     * @throws ObjectStreamException Thrown in case of unmarshalling error.
     */
    private Object readResolve() throws ObjectStreamException {
        return ctx.grid().scheduler();
    }
}
