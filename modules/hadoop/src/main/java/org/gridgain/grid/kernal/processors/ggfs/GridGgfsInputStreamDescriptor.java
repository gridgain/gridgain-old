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

import java.io.*;

/**
 * GGFS input stream descriptor - includes stream id and length.
 */
public class GridGgfsInputStreamDescriptor implements Externalizable {
    /** */
    private static final long serialVersionUID = 0L;

    /** Stream id. */
    private long streamId;

    /** Available length. */
    private long len;

    /**
     * Empty constructor required by {@link Externalizable}.
     */
    public GridGgfsInputStreamDescriptor() {
        // No-op.
    }

    /**
     * Input stream descriptor constructor.
     *
     * @param streamId Stream id.
     * @param len Available length.
     */
    public GridGgfsInputStreamDescriptor(long streamId, long len) {
        this.streamId = streamId;
        this.len = len;
    }

    /**
     * @return Stream ID.
     */
    public long streamId() {
        return streamId;
    }

    /**
     * @return Available length.
     */
    public long length() {
        return len;
    }

    /** {@inheritDoc} */
    @Override public void writeExternal(ObjectOutput out) throws IOException {
        out.writeLong(streamId);
        out.writeLong(len);
    }

    /** {@inheritDoc} */
    @Override public void readExternal(ObjectInput in) throws IOException {
        streamId = in.readLong();
        len = in.readLong();
    }
}
