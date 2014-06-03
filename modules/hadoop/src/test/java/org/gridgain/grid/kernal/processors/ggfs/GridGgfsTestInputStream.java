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

package org.gridgain.grid.kernal.processors.ggfs;

import java.io.*;

/**
 * Test input stream with predictable data output and zero memory usage.
 */
class GridGgfsTestInputStream extends InputStream {
    /** This stream length. */
    private long size;

    /** Salt for input data generation. */
    private long salt;

    /** Current stream position. */
    private long pos;

    /**
     * Constructs test input stream.
     *
     * @param size This stream length.
     * @param salt Salt for input data generation.
     */
    GridGgfsTestInputStream(long size, long salt) {
        this.size = size;
        this.salt = salt;
    }

    /** {@inheritDoc} */
    @Override public synchronized int read() throws IOException {
        if (pos >= size)
            return -1;

        long next = salt ^ (salt * pos++);

        next ^= next >>> 32;
        next ^= next >>> 16;
        next ^= next >>> 8;

        return (int)(0xFF & next);
    }

    /** {@inheritDoc} */
    @Override public synchronized long skip(long n) throws IOException {
        pos += Math.min(n, size - pos);

        return size - pos;
    }
}
