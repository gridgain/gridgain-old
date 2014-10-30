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
import org.gridgain.grid.util.direct.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.nio.*;
import java.util.*;

/**
 * Parser for direct messages.
 */
public class GridDirectParser implements GridNioParser {
    /** Message metadata key. */
    private static final int MSG_META_KEY = GridNioSessionMetaKey.nextUniqueKey();

    /** Message reader. */
    private final GridNioMessageReader msgReader;

    /**
     * @param msgReader Message reader.
     */
    public GridDirectParser(GridNioMessageReader msgReader) {
        this.msgReader = msgReader;
    }

    /** {@inheritDoc} */
    @Nullable @Override public Object decode(GridNioSession ses, ByteBuffer buf) throws IOException, GridException {
        GridTcpCommunicationMessageAdapter msg = ses.removeMeta(MSG_META_KEY);
        UUID nodeId = ses.meta(GridNioServer.DIFF_VER_NODE_ID_META_KEY);

        if (msg == null && buf.hasRemaining())
            msg = GridTcpCommunicationMessageFactory.create(buf.get());

        boolean finished = false;

        if (buf.hasRemaining())
            finished = msgReader.read(nodeId, msg, buf);

        if (finished)
            return msg;
        else {
            ses.addMeta(MSG_META_KEY, msg);

            return null;
        }
    }

    /** {@inheritDoc} */
    @Override public ByteBuffer encode(GridNioSession ses, Object msg) throws IOException, GridException {
        // No encoding needed for direct messages.
        throw new UnsupportedEncodingException();
    }
}
