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

import org.apache.hadoop.mapreduce.*;
import org.gridgain.grid.hadoop.*;

import java.io.*;

/**
 * Adapter from own counter implementation into Hadoop API Counter od version 2.0.
 */
public class GridHadoopV2Counter implements Counter {
    /** Delegate. */
    private final GridHadoopCounter counter;

    /**
     * Creates new instance with given delegate.
     *
     * @param counter Internal counter.
     */
    public GridHadoopV2Counter(GridHadoopCounter counter) {
        assert counter != null : "counter must be non-null";

        this.counter = counter;
    }

    /** {@inheritDoc} */
    @Override public void setDisplayName(String displayName) {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override public String getName() {
        return counter.name();
    }

    /** {@inheritDoc} */
    @Override public String getDisplayName() {
        return getName();
    }

    /** {@inheritDoc} */
    @Override public long getValue() {
        return counter.value();
    }

    /** {@inheritDoc} */
    @Override public void setValue(long value) {
        counter.value(value);
    }

    /** {@inheritDoc} */
    @Override public void increment(long incr) {
        counter.increment(incr);
    }

    /** {@inheritDoc} */
    @Override public Counter getUnderlyingCounter() {
        return this;
    }

    /** {@inheritDoc} */
    @Override public void write(DataOutput out) throws IOException {
        throw new UnsupportedOperationException("not implemented");
    }

    /** {@inheritDoc} */
    @Override public void readFields(DataInput in) throws IOException {
        throw new UnsupportedOperationException("not implemented");
    }
}
