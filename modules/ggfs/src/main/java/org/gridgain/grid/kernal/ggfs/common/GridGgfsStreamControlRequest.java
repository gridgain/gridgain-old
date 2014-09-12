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

package org.gridgain.grid.kernal.ggfs.common;

import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.grid.util.tostring.*;

/**
 * Read block request.
 */
public class GridGgfsStreamControlRequest extends GridGgfsMessage {
    /** Stream id. */
    private long streamId;

    /** Data. */
    @GridToStringExclude
    private byte[] data;

    /** Read position. */
    private long pos;

    /** Length to read. */
    private int len;

    /**
     * @return Stream ID.
     */
    public long streamId() {
        return streamId;
    }

    /**
     * @param streamId Stream ID.
     */
    public void streamId(long streamId) {
        this.streamId = streamId;
    }

    /**
     * @return Data.
     */
    public byte[] data() {
        return data;
    }

    /**
     * @param data Data.
     */
    public void data(byte[] data) {
        this.data = data;
    }

    /**
     * @return Position.
     */
    public long position() {
        return pos;
    }

    /**
     * @param pos Position.
     */
    public void position(long pos) {
        this.pos = pos;
    }

    /**
     * @return Length.
     */
    public int length() {
        return len;
    }

    /**
     * @param len Length.
     */
    public void length(int len) {
        this.len = len;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridGgfsStreamControlRequest.class, this, "cmd", command(),
            "dataLen", data == null ? 0 : data.length);
    }
}
