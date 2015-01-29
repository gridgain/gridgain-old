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

import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.grid.hadoop.*;

import java.io.*;

/**
 * The wrapper for native hadoop input splits.
 *
 * Warning!! This class must not depend on any Hadoop classes directly or indirectly.
 */
public class GridHadoopSplitWrapper extends GridHadoopInputSplit {
    /** */
    private static final long serialVersionUID = 0L;

    /** Native hadoop input split. */
    private byte[] bytes;

    /** */
    private String clsName;

    /** Internal ID */
    private int id;

    /**
     * Creates new split wrapper.
     */
    public GridHadoopSplitWrapper() {
        // No-op.
    }

    /**
     * Creates new split wrapper.
     *
     * @param id Split ID.
     * @param clsName Class name.
     * @param bytes Serialized class.
     * @param hosts Hosts where split is located.
     * @throws IOException If failed.
     */
    public GridHadoopSplitWrapper(int id, String clsName, byte[] bytes, String[] hosts) throws IOException {
        assert hosts != null;
        assert clsName != null;
        assert bytes != null;

        this.hosts = hosts;
        this.id = id;

        this.clsName = clsName;
        this.bytes = bytes;
    }

    /** {@inheritDoc} */
    @Override public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(id);

        out.writeUTF(clsName);
        U.writeByteArray(out, bytes);
    }

    /**
     * @return Class name.
     */
    public String className() {
        return clsName;
    }

    /**
     * @return Class bytes.
     */
    public byte[] bytes() {
        return bytes;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        id = in.readInt();

        clsName = in.readUTF();
        bytes = U.readByteArray(in);
    }

    /** {@inheritDoc} */
    @Override public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        GridHadoopSplitWrapper that = (GridHadoopSplitWrapper)o;

        return id == that.id;
    }

    /** {@inheritDoc} */
    @Override public int hashCode() {
        return id;
    }
}
