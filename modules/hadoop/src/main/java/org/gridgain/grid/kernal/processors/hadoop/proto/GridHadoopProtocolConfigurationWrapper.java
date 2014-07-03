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

package org.gridgain.grid.kernal.processors.hadoop.proto;

import org.apache.hadoop.conf.*;

import java.io.*;

/**
 * Configuration wrapper.
 */
public class GridHadoopProtocolConfigurationWrapper implements Externalizable {
    /** Configuration. */
    private Configuration conf;

    /**
     * {@link Externalizable} support.
     */
    public GridHadoopProtocolConfigurationWrapper() {
        // No-op.
    }

    /**
     * Constructor.
     *
     * @param conf Configuration.
     */
    public GridHadoopProtocolConfigurationWrapper(Configuration conf) {
        this.conf = conf;
    }

    /**
     * @return Underlying configuration.
     */
    public Configuration get() {
        return conf;
    }

    /** {@inheritDoc} */
    @Override public void writeExternal(ObjectOutput out) throws IOException {
        conf.write(out);
    }

    /** {@inheritDoc} */
    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        conf = new Configuration();

        conf.readFields(in);
    }
}
