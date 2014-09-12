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

package org.gridgain.grid.kernal.processors.version;

import org.gridgain.grid.util.direct.*;

import java.nio.*;

/**
 * Version converter.
 */
public abstract class GridVersionConverter {
    /** State. */
    protected final GridTcpCommunicationMessageState commState = new GridTcpCommunicationMessageState();

    /**
     * Writes delta between two versions.
     *
     * @param buf Buffer to write to.
     * @return Whether delta was fully written.
     */
    public abstract boolean writeTo(ByteBuffer buf);

    /**
     * Reads delta between two versions.
     *
     * @param buf Buffer to read from.
     * @return Whether delta was fully read.
     */
    public abstract boolean readFrom(ByteBuffer buf);
}
