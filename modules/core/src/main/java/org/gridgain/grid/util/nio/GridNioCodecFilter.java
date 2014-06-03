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

package org.gridgain.grid.util.nio;

import org.gridgain.grid.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.grid.util.tostring.*;

import java.io.*;
import java.nio.*;

/**
 * Filter that transforms byte buffers to user-defined objects and vice-versa
 * with specified {@link GridNioParser}.
 */
public class GridNioCodecFilter extends GridNioFilterAdapter {
    /** Parser used. */
    private GridNioParser parser;

    /** Grid logger. */
    @GridToStringExclude
    private GridLogger log;

    /** Whether direct mode is used. */
    private boolean directMode;

    /**
     * Creates a codec filter.
     *
     * @param parser Parser to use.
     * @param log Log instance to use.
     * @param directMode Whether direct mode is used.
     */
    public GridNioCodecFilter(GridNioParser parser, GridLogger log, boolean directMode) {
        super("GridNioCodecFilter");

        this.parser = parser;
        this.log = log;
        this.directMode = directMode;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridNioCodecFilter.class, this);
    }

    /** {@inheritDoc} */
    @Override public void onSessionOpened(GridNioSession ses) throws GridException {
        proceedSessionOpened(ses);
    }

    /** {@inheritDoc} */
    @Override public void onSessionClosed(GridNioSession ses) throws GridException {
        proceedSessionClosed(ses);
    }

    /** {@inheritDoc} */
    @Override public void onExceptionCaught(GridNioSession ses, GridException ex) throws GridException {
        proceedExceptionCaught(ses, ex);
    }

    /** {@inheritDoc} */
    @Override public GridNioFuture<?> onSessionWrite(GridNioSession ses, Object msg) throws GridException {
        // No encoding needed in direct mode.
        if (directMode)
            return proceedSessionWrite(ses, msg);

        try {
            ByteBuffer res = parser.encode(ses, msg);

            return proceedSessionWrite(ses, res);
        }
        catch (IOException e) {
            throw new GridNioException(e);
        }
    }

    /** {@inheritDoc} */
    @Override public void onMessageReceived(GridNioSession ses, Object msg) throws GridException {
        if (!(msg instanceof ByteBuffer))
            throw new GridNioException("Failed to decode incoming message (incoming message is not a byte buffer, " +
                "is filter properly placed?): " + msg.getClass());

        try {
            ByteBuffer input = (ByteBuffer)msg;

            while (input.hasRemaining()) {
                Object res = parser.decode(ses, input);

                if (res != null)
                    proceedMessageReceived(ses, res);
                else {
                    if (input.hasRemaining()) {
                        if (directMode)
                            return;

                        LT.warn(log, null, "Parser returned null but there are still unread data in input buffer (bug in " +
                            "parser code?) [parser=" + parser + ", ses=" + ses + ']');

                        input.position(input.limit());
                    }
                }
            }
        }
        catch (IOException e) {
            throw new GridNioException(e);
        }
    }

    /** {@inheritDoc} */
    @Override public GridNioFuture<Boolean> onSessionClose(GridNioSession ses) throws GridException {
        return proceedSessionClose(ses);
    }

    /** {@inheritDoc} */
    @Override public void onSessionIdleTimeout(GridNioSession ses) throws GridException {
        proceedSessionIdleTimeout(ses);
    }

    /** {@inheritDoc} */
    @Override public void onSessionWriteTimeout(GridNioSession ses) throws GridException {
        proceedSessionWriteTimeout(ses);
    }
}
