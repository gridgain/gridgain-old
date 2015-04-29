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

package org.gridgain.grid.kernal.processors.hadoop.counter;

import org.gridgain.grid.hadoop.*;
import java.io.*;

/**
 * Standard hadoop counter to use via original Hadoop API in Hadoop jobs.
 */
public class GridHadoopLongCounter extends GridHadoopCounterAdapter {
    /** */
    private static final long serialVersionUID = 0L;

    /** The counter value. */
    private long val;

    /**
     * Default constructor required by {@link Externalizable}.
     */
    public GridHadoopLongCounter() {
        // No-op.
    }

    /**
     * Constructor.
     *
     * @param grp Group name.
     * @param name Counter name.
     */
    public GridHadoopLongCounter(String grp, String name) {
        super(grp, name);
    }

    /** {@inheritDoc} */
    @Override protected void writeValue(ObjectOutput out) throws IOException {
        out.writeLong(val);
    }

    /** {@inheritDoc} */
    @Override protected void readValue(ObjectInput in) throws IOException {
        val = in.readLong();
    }

    /** {@inheritDoc} */
    @Override public void merge(GridHadoopCounter cntr) {
        val += ((GridHadoopLongCounter)cntr).val;
    }

    /**
     * Gets current value of this counter.
     *
     * @return Current value.
     */
    public long value() {
        return val;
    }

    /**
     * Sets current value by the given value.
     *
     * @param val Value to set.
     */
    public void value(long val) {
        this.val = val;
    }

    /**
     * Increment this counter by the given value.
     *
     * @param i Value to increase this counter by.
     */
    public void increment(long i) {
        val += i;
    }
}
