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

package org.gridgain.grid.kernal.processors.hadoop.v2;

import org.gridgain.grid.hadoop.*;

import java.io.*;

/**
 * Split serialized in external file.
 */
public class GridHadoopExternalSplit extends GridHadoopInputSplit {
    /** */
    private static final long serialVersionUID = 0L;

    /** */
    private long off;

    /**
     * For {@link Externalizable}.
     */
    public GridHadoopExternalSplit() {
        // No-op.
    }

    /**
     * @param hosts Hosts.
     * @param off Offset of this split in external file.
     */
    public GridHadoopExternalSplit(String[] hosts, long off) {
        assert off >= 0 : off;
        assert hosts != null;

        this.hosts = hosts;
        this.off = off;
    }

    /**
     * @return Offset of this input split in external file.
     */
    public long offset() {
        return off;
    }

    /** {@inheritDoc} */
    @Override public void writeExternal(ObjectOutput out) throws IOException {
        out.writeLong(off);
    }

    /** {@inheritDoc} */
    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        off = in.readLong();
    }

    /** {@inheritDoc} */
    @Override public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        GridHadoopExternalSplit that = (GridHadoopExternalSplit) o;

        return off == that.off;
    }

    /** {@inheritDoc} */
    @Override public int hashCode() {
        return (int)(off ^ (off >>> 32));
    }
}
