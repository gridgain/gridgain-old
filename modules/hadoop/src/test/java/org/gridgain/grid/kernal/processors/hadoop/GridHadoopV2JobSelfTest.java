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

import org.apache.hadoop.fs.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.io.serializer.*;
import org.apache.hadoop.mapred.*;
import org.gridgain.grid.*;
import org.gridgain.grid.hadoop.*;
import org.gridgain.grid.kernal.processors.hadoop.v2.*;

import java.io.*;
import java.util.*;

import static org.gridgain.grid.kernal.processors.hadoop.GridHadoopUtils.*;

/**
 * Self test of {@link GridHadoopV2Job}.
 */
public class GridHadoopV2JobSelfTest extends GridHadoopAbstractSelfTest {
    /** */
    private static final String TEST_SERIALIZED_VALUE = "Test serialized value";

    /**
     * Custom serialization class that accepts {@link Writable}.
     */
    private static class CustomSerialization extends WritableSerialization {
        /** {@inheritDoc} */
        @Override public Deserializer<Writable> getDeserializer(Class<Writable> c) {
            return new Deserializer<Writable>() {
                @Override public void open(InputStream in) { }

                @Override public Writable deserialize(Writable writable) {
                    return new Text(TEST_SERIALIZED_VALUE);
                }

                @Override public void close() { }
            };
        }
    }

    /**
     * Tests that {@link GridHadoopJob} provides wrapped serializer if it's set in configuration.
     *
     * @throws GridException If fails.
     */
    public void testCustomSerializationApplying() throws GridException {
        JobConf cfg = new JobConf();

        cfg.setMapOutputKeyClass(IntWritable.class);
        cfg.setMapOutputValueClass(Text.class);
        cfg.set(CommonConfigurationKeys.IO_SERIALIZATIONS_KEY, CustomSerialization.class.getName());

        GridHadoopJob job = new GridHadoopV2Job(new GridHadoopJobId(UUID.randomUUID(), 1), createJobInfo(cfg), log);

        GridHadoopTaskContext taskCtx = job.getTaskContext(new GridHadoopTaskInfo(GridHadoopTaskType.MAP, null, 0, 0,
            null));

        GridHadoopSerialization ser = taskCtx.keySerialization();

        assertEquals(GridHadoopSerializationWrapper.class.getName(), ser.getClass().getName());

        DataInput in = new DataInputStream(new ByteArrayInputStream(new byte[0]));

        assertEquals(TEST_SERIALIZED_VALUE, ser.read(in, null).toString());

        ser = taskCtx.valueSerialization();

        assertEquals(GridHadoopSerializationWrapper.class.getName(), ser.getClass().getName());

        assertEquals(TEST_SERIALIZED_VALUE, ser.read(in, null).toString());
    }
}
