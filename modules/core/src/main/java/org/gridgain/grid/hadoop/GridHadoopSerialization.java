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

package org.gridgain.grid.hadoop;

import org.gridgain.grid.*;
import org.jetbrains.annotations.*;

import java.io.*;

/**
 * Hadoop serialization. Not thread safe object, must be created for each thread or correctly synchronized.
 */
public interface GridHadoopSerialization extends AutoCloseable {
    /**
     * Writes the given object to output.
     *
     * @param out Output.
     * @param obj Object to serialize.
     * @throws GridException If failed.
     */
    public void write(DataOutput out, Object obj) throws GridException;

    /**
     * Reads object from the given input optionally reusing given instance.
     *
     * @param in Input.
     * @param obj Object.
     * @return New object or reused instance.
     * @throws GridException If failed.
     */
    public Object read(DataInput in, @Nullable Object obj) throws GridException;

    /**
     * Finalise the internal objects.
     * 
     * @throws GridException If failed.
     */
    @Override public void close() throws GridException;
}
