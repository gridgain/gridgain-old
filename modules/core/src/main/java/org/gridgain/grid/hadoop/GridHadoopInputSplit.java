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

import java.io.*;

/**
 * Abstract fragment of an input data source.
 */
public abstract class GridHadoopInputSplit implements Externalizable {
    /** */
    protected String[] hosts;

    /**
     * Array of hosts where this input split resides.
     *
     * @return Hosts.
     */
    public String[] hosts() {
        assert hosts != null;

        return hosts;
    }

    /**
     * This method must be implemented for purpose of internal implementation.
     *
     * @param obj Another object.
     * @return {@code true} If objects are equal.
     */
    @Override public abstract boolean equals(Object obj);

    /**
     * This method must be implemented for purpose of internal implementation.
     *
     * @return Hash code of the object.
     */
    @Override public abstract int hashCode();
}
