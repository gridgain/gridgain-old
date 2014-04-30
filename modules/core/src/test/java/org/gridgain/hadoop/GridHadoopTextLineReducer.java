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

import org.apache.hadoop.io.*;
import org.apache.hadoop.mapred.*;

import java.io.*;
import java.util.*;

/**
 * Reducer and combiner.
 */
public class GridHadoopTextLineReducer extends MapReduceBase implements Reducer<Text, LongWritable, Text, LongWritable> {
    /** {@inheritDoc} */
    @Override public void reduce(Text key, Iterator<LongWritable> vals, OutputCollector<Text, LongWritable> output,
        Reporter reporter) throws IOException {
        // Sum all values for this key.
        long sum = 0;

        while (vals.hasNext())
            sum += vals.next().get();

        // Output sum.
        output.collect(key, new LongWritable(sum));
    }
}
