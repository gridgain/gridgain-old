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

package org.gridgain.hadoop;

import org.apache.hadoop.fs.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapred.*;

import java.io.*;
import java.util.*;

/**
 * Mapper to count characters in a file.
 */
public class GridHadoopTextLineMapper<K> extends MapReduceBase implements Mapper<K, Text, Text, LongWritable> {
    /** */
    private JobConf jobConf;

    /** {@inheritDoc} */
    @Override public void configure(JobConf job) {
        jobConf = job;
    }

    /** {@inheritDoc} */
    @Override public void map(K key, Text val, OutputCollector<Text, LongWritable> output, Reporter reporter)
        throws IOException {
        String text = val.toString();

        output.collect(new Text(text), new LongWritable(text.length()));

        Path outputPath = FileOutputFormat.getOutputPath(jobConf);

        FileSystem fs = outputPath.getFileSystem(jobConf);

        try (FSDataOutputStream out = fs.create(new Path(outputPath, UUID.randomUUID().toString()))) {
            out.write(text.getBytes());
        }
    }
}
