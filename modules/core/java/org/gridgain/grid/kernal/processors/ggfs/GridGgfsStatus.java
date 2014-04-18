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
 * GGFS response for status request.
 */
public class GridGgfsStatus implements Externalizable {
    /** */
    private static final long serialVersionUID = 0L;

    /** Total space size. */
    private long spaceTotal;

    /** Used space in GGFS. */
    private long spaceUsed;

    /**
     * Empty constructor required by {@link Externalizable}.
     */
    public GridGgfsStatus() {
        // No-op.
    }

    /**
     * @param spaceUsed Used space in GGFS.
     * @param spaceTotal Total space available in GGFS.
     */
    public GridGgfsStatus(long spaceUsed, long spaceTotal) {
        this.spaceUsed = spaceUsed;
        this.spaceTotal = spaceTotal;
    }

    /**
     * @return Total space available in GGFS.
     */
    public long spaceTotal() {
        return spaceTotal;
    }

    /**
     * @return Used space in GGFS.
     */
    public long spaceUsed() {
        return spaceUsed;
    }

    /** {@inheritDoc} */
    @Override public void writeExternal(ObjectOutput out) throws IOException {
        out.writeLong(spaceUsed);
        out.writeLong(spaceTotal);
    }

    /** {@inheritDoc} */
    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        spaceUsed = in.readLong();
        spaceTotal = in.readLong();
    }
}
