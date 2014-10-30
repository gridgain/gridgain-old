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

package org.gridgain.grid.kernal.processors.cache.distributed.dht;

/**
 * Exception thrown whenever entry is created for invalid partition.
 */
public class GridDhtInvalidPartitionException extends RuntimeException {
    /** */
    private static final long serialVersionUID = 0L;

    /** Partition. */
    private final int part;

    /**
     * @param part Partition.
     * @param msg Message.
     */
    public GridDhtInvalidPartitionException(int part, String msg) {
        super(msg);

        this.part = part;
    }

    /**
     * @return Partition.
     */
    public int partition() {
        return part;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return getClass() + " [part=" + part + ", msg=" + getMessage() + ']';
    }
}
