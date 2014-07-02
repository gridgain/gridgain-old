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

package org.gridgain.grid.spi.checkpoint;

import java.io.*;

/**
 * Grid checkpoint test state
 */
public class GridCheckpointTestState implements Serializable {
    /** */
    private String data;

    /**
     * @param data Data.
     */
    public GridCheckpointTestState(String data) {
        this.data = data;
    }

    /**
     * Gets data.
     *
     * @return data.
     */
    public String getData() {
        return data;
    }

    /** {@inheritDoc} */
    @Override public boolean equals(Object obj) {
        return !(obj == null || !obj.getClass().equals(getClass())) && ((GridCheckpointTestState)obj).data.equals(data);
    }

    /** {@inheritDoc} */
    @Override public int hashCode() {
        return data.hashCode();
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        StringBuilder buf = new StringBuilder();

        buf.append(getClass().getSimpleName());
        buf.append(" [data='").append(data).append('\'');
        buf.append(']');

        return buf.toString();
    }
}
