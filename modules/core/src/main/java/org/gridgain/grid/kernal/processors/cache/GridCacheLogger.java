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

import org.gridgain.grid.lang.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.grid.util.lang.*;
import org.gridgain.grid.util.tostring.*;
import org.jetbrains.annotations.*;

import java.io.*;

/**
 * Logger which automatically attaches {@code [cacheName]} to every log statement.
 */
@GridToStringExclude
class GridCacheLogger extends GridMetadataAwareAdapter implements GridLogger, Externalizable {
    /** */
    private static final long serialVersionUID = 0L;

    /** */
    private static ThreadLocal<GridBiTuple<String, GridCacheContext>> stash =
        new ThreadLocal<GridBiTuple<String, GridCacheContext>>() {
            @Override protected GridBiTuple<String, GridCacheContext> initialValue() {
                return F.t2();
            }
        };

    /** Logger. */
    private GridLogger log;

    /** Cache name. */
    private GridCacheContext<?, ?> cctx;

    /** Cache name. */
    private String cacheName;

    /** Category. */
    private String ctgr;

    /**
     * @param cctx Cache context.
     * @param ctgr Category.
     */
    GridCacheLogger(GridCacheContext<?, ?> cctx, String ctgr) {
        assert cctx != null;
        assert ctgr != null;

        this.cctx = cctx;
        this.ctgr = ctgr;

        cacheName = '<' + cctx.namexx() + "> ";

        log = cctx.kernalContext().log().getLogger(ctgr);
    }

    /**
     * Empty constructor required for {@link Externalizable}.
     */
    public GridCacheLogger() {
        // No-op.
    }

    /**
     * @param msg Message.
     * @return Formatted log message.
     */
    private String format(String msg) {
        return cacheName + msg;
    }

    /** {@inheritDoc} */
    @Override public void debug(String msg) {
        log.debug(format(msg));
    }

    /** {@inheritDoc} */
    @Override public GridLogger getLogger(Object ctgr) {
        return new GridCacheLogger(cctx, ctgr.toString());
    }

    /** {@inheritDoc} */
    @Override public void trace(String msg) {
        log.trace(format(msg));
    }

    /** {@inheritDoc} */
    @Override public void info(String msg) {
        log.info(format(msg));
    }

    /** {@inheritDoc} */
    @Override public void warning(String msg) {
        log.warning(format(msg));
    }

    /** {@inheritDoc} */
    @Override public void warning(String msg, @Nullable Throwable e) {
        log.warning(format(msg), e);
    }

    /** {@inheritDoc} */
    @Override public void error(String msg) {
        log.error(format(msg));
    }

    /** {@inheritDoc} */
    @Override public void error(String msg, @Nullable Throwable e) {
        log.error(format(msg), e);
    }

    /** {@inheritDoc} */
    @Override public boolean isTraceEnabled() {
        return log.isTraceEnabled();
    }

    /** {@inheritDoc} */
    @Override public boolean isDebugEnabled() {
        return log.isDebugEnabled();
    }

    /** {@inheritDoc} */
    @Override public boolean isInfoEnabled() {
        return log.isInfoEnabled();
    }

    /** {@inheritDoc} */
    @Override public boolean isQuiet() {
        return log.isQuiet();
    }

    /** {@inheritDoc} */
    @Nullable @Override public String fileName() {
        return log.fileName();
    }

    /** {@inheritDoc} */
    @Override public void writeExternal(ObjectOutput out) throws IOException {
        U.writeString(out, ctgr);
        out.writeObject(cctx);
    }

    /** {@inheritDoc} */
    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        GridBiTuple<String, GridCacheContext> t = stash.get();

        t.set1(U.readString(in));
        t.set2((GridCacheContext)in.readObject());
    }

    /**
     * Reconstructs object on unmarshalling.
     *
     * @return Reconstructed object.
     * @throws ObjectStreamException Thrown in case of unmarshalling error.
     */
    protected Object readResolve() throws ObjectStreamException {
        try {
            GridBiTuple<String, GridCacheContext> t = stash.get();

            return t.get2().logger(t.get1());
        }
        catch (IllegalStateException e) {
            throw U.withCause(new InvalidObjectException(e.getMessage()), e);
        }
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridCacheLogger.class, this);
    }
}
