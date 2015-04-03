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

package org.gridgain.grid.kernal.processors.ggfs;

import org.gridgain.grid.*;
import org.gridgain.grid.marshaller.*;
import org.gridgain.grid.util.direct.*;
import org.jetbrains.annotations.*;

import java.nio.*;

/**
 * Base class for all GGFS communication messages sent between nodes.
 */
public abstract class GridGgfsCommunicationMessage extends GridTcpCommunicationMessageAdapter {
    /** */
    private static final long serialVersionUID = 0L;

    /** {@inheritDoc} */
    @Override protected void clone0(GridTcpCommunicationMessageAdapter _msg) {
    }

    /**
     * @param marsh Marshaller.
     * @throws GridException In case of error.
     */
    public void prepareMarshal(GridMarshaller marsh) throws GridException {
        // No-op.
    }

    /**
     * @param marsh Marshaller.
     * @param ldr Class loader.
     * @throws GridException In case of error.
     */
    public void finishUnmarshal(GridMarshaller marsh, @Nullable ClassLoader ldr) throws GridException {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override public boolean writeTo(ByteBuffer buf) {
        commState.setBuffer(buf);

        if (!commState.typeWritten) {
            if (!commState.putByte(directType()))
                return false;

            commState.typeWritten = true;
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override public boolean readFrom(ByteBuffer buf) {
        commState.setBuffer(buf);

        return true;
    }
}
