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

package org.gridgain.grid.kernal.processors.hadoop.examples;

import java.io.*;

import org.apache.hadoop.conf.Configurable;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.*;

/**
 * Combiner and Reducer phase of WordCount job.
 */
public class GridHadoopWordCount2Reducer extends Reducer<Text, IntWritable, Text, IntWritable> implements Configurable {
    /** Writable container for writing sum of word counts. */
    private IntWritable totalWordCnt = new IntWritable();

    /** Flag is to check that mapper was configured before run. */
    private boolean wasConfigured;

    /** Flag is to check that mapper was set up before run. */
    private boolean wasSetUp;

    /** {@inheritDoc} */
    @Override public void reduce(Text key, Iterable<IntWritable> values, Context ctx) throws IOException, InterruptedException {
        assert wasConfigured : "Reducer should be configured";
        assert wasSetUp : "Reducer should be set up";

        int wordCnt = 0;

        for (IntWritable value : values)
            wordCnt += value.get();

        totalWordCnt.set(wordCnt);

        ctx.write(key, totalWordCnt);
    }

    /** {@inheritDoc} */
    @Override protected void setup(Context context) throws IOException, InterruptedException {
        super.setup(context);
        wasSetUp = true;
    }

    /** {@inheritDoc} */
    @Override public void setConf(Configuration conf) {
        wasConfigured = true;
    }

    /** {@inheritDoc} */
    @Override public Configuration getConf() {
        return null;
    }

}
