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

import org.apache.hadoop.io.*;
import org.gridgain.grid.*;
import org.gridgain.grid.hadoop.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.jetbrains.annotations.*;

import java.io.*;

/**
 * Optimized serialization for Hadoop {@link Writable} types.
 */
public class GridHadoopWritableSerialization implements GridHadoopSerialization {
    /** */
    private final Class<? extends Writable> cls;

    /**
     * @param cls Class.
     */
    public GridHadoopWritableSerialization(Class<? extends Writable> cls) {
        assert cls != null;

        this.cls = cls;
    }

    /** {@inheritDoc} */
    @Override public void write(DataOutput out, Object obj) throws GridException {
        assert cls.isAssignableFrom(obj.getClass()) : cls + " " + obj.getClass();

        try {
            ((Writable)obj).write(out);
        }
        catch (IOException e) {
            throw new GridException(e);
        }
    }

    /** {@inheritDoc} */
    @Override public Object read(DataInput in, @Nullable Object obj) throws GridException {
        Writable w = obj == null ? U.newInstance(cls) : cls.cast(obj);

        try {
            w.readFields(in);
        }
        catch (IOException e) {
            throw new GridException(e);
        }

        return w;
    }

    /** {@inheritDoc} */
    @Override public void close() {
        // No-op.
    }
}
