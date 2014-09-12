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

package org.gridgain.testframework;

import org.gridgain.grid.logger.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.jetbrains.annotations.*;

/**
 * Logger which logs to string buffer.
 */
public class GridStringLogger implements GridLogger {
    /** */
    private static final int MAX = 1024 * 11;

    /** */
    private static final int CHAR_CNT = 1024 * 10;

    /** */
    private StringBuilder buf = new StringBuilder(MAX);

    /** */
    private final boolean dbg;

    /** */
    private final GridLogger echo;

    /**
     *
     */
    public GridStringLogger() {
        this(false);
    }

    /**
     * @param dbg Debug flag.
     */
    public GridStringLogger(boolean dbg) {
        this(dbg, null);
    }

    /**
     * @param dbg Debug flag.
     * @param echo Logger to echo all messages.
     */
    public GridStringLogger(boolean dbg, @Nullable GridLogger echo) {
        this.dbg = dbg;
        this.echo = echo;
    }

    /**
     * @param msg Message to log.
     */
    private void log(String msg) {
        buf.append(msg).append(U.nl());

        if (echo != null)
            echo.info("[GridStringLogger echo] " + msg);

        if (buf.length() > CHAR_CNT) {
            if (echo != null)
                echo.warning("Cleaning GridStringLogger history.");

            buf.delete(0, buf.length() - CHAR_CNT);
        }

        assert buf.length() <= CHAR_CNT;
    }

    /** {@inheritDoc} */
    @Override public GridLogger getLogger(Object ctgr) {
        return this;
    }

    /** {@inheritDoc} */
    @Override public void trace(String msg) {
        log(msg);
    }

    /** {@inheritDoc} */
    @Override public void debug(String msg) {
        log(msg);
    }

    /** {@inheritDoc} */
    @Override public void info(String msg) {
        log(msg);
    }

    /** {@inheritDoc} */
    @Override public void warning(String msg) {
        log(msg);
    }

    /** {@inheritDoc} */
    @Override public void warning(String msg, @Nullable Throwable e) {
        log(msg);

        if (e != null)
            log(e.toString());
    }

    /** {@inheritDoc} */
    @Override public void error(String msg) {
        log(msg);
    }

    /** {@inheritDoc} */
    @Override public void error(String msg, @Nullable Throwable e) {
        log(msg);

        if (e != null)
            log(e.toString());
    }

    /** {@inheritDoc} */
    @Override public boolean isTraceEnabled() {
        return dbg;
    }

    /** {@inheritDoc} */
    @Override public boolean isDebugEnabled() {
        return dbg;
    }

    /** {@inheritDoc} */
    @Override public boolean isInfoEnabled() {
        return true;
    }

    /** {@inheritDoc} */
    @Override public boolean isQuiet() {
        return false;
    }

    /** {@inheritDoc} */
    @Nullable @Override public String fileName() {
        return null;
    }

    /**
     * Resets logger.
     */
    public void reset() {
        buf.setLength(0);
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return buf.toString();
    }
}
