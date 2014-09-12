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

package org.gridgain.grid.kernal.processors.hadoop;

import org.apache.hadoop.io.*;
import org.apache.hadoop.io.serializer.*;
import org.apache.hadoop.io.serializer.avro.AvroReflectSerialization;
import org.gridgain.grid.hadoop.*;
import org.gridgain.grid.kernal.processors.hadoop.v2.*;
import org.gridgain.testframework.junits.common.*;

import java.io.*;
import java.util.*;

/**
 * Test of wrapper of the native serialization.
 */
public class GridHadoopSerializationWrapperSelfTest extends GridCommonAbstractTest {
    /**
     * Tests read/write of IntWritable via native WritableSerialization.
     * @throws Exception If fails.
     */
    public void testIntWritableSerialization() throws Exception {
        GridHadoopSerialization ser = new GridHadoopSerializationWrapper(new WritableSerialization(), IntWritable.class);

        ByteArrayOutputStream buf = new ByteArrayOutputStream();

        DataOutput out = new DataOutputStream(buf);

        ser.write(out, new IntWritable(3));
        ser.write(out, new IntWritable(-5));

        assertEquals("[0, 0, 0, 3, -1, -1, -1, -5]", Arrays.toString(buf.toByteArray()));

        DataInput in = new DataInputStream(new ByteArrayInputStream(buf.toByteArray()));

        assertEquals(3, ((IntWritable)ser.read(in, null)).get());
        assertEquals(-5, ((IntWritable)ser.read(in, null)).get());
    }

    /**
     * Tests read/write of Integer via native JavaleSerialization.
     * @throws Exception If fails.
     */
    public void testIntJavaSerialization() throws Exception {
        GridHadoopSerialization ser = new GridHadoopSerializationWrapper(new JavaSerialization(), Integer.class);

        ByteArrayOutputStream buf = new ByteArrayOutputStream();

        DataOutput out = new DataOutputStream(buf);

        ser.write(out, 3);
        ser.write(out, -5);
        ser.close();

        DataInput in = new DataInputStream(new ByteArrayInputStream(buf.toByteArray()));

        assertEquals(3, ((Integer)ser.read(in, null)).intValue());
        assertEquals(-5, ((Integer)ser.read(in, null)).intValue());
    }
}
