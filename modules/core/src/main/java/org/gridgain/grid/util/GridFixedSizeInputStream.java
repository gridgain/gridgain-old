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

package org.gridgain.grid.util;

import java.io.*;

/**
 * Input stream wrapper which allows to read exactly expected number of bytes.
 */
public class GridFixedSizeInputStream extends InputStream {
    /** */
    private final InputStream in;

    /** */
    private long size;

    /**
     * @param in Input stream.
     * @param size Size of available data.
     */
    public GridFixedSizeInputStream(InputStream in, long size) {
        this.in = in;
        this.size = size;
    }

    /** {@inheritDoc} */
    @Override public int read(byte[] b, int off, int len) throws IOException {
        assert len <= b.length;

        if (size == 0)
            return -1;

        if (len > size)
            // Assignment is ok because size < len <= Integer.MAX_VALUE
            len = (int)size;

        int res = in.read(b, off, len);

        if (res == -1)
            throw new IOException("Expected " + size + " more bytes to read.");

        assert res >= 0 : res;

        size -= res;

        assert size >= 0 : size;

        return res;
    }

    /** {@inheritDoc} */
    @Override public int available() throws IOException {
        return size > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int)size;
    }

    /** {@inheritDoc} */
    @Override public int read() throws IOException {
        if (size == 0)
            return -1;

        int res = in.read();

        if (res == -1)
            throw new IOException("Expected " + size + " more bytes to read.");

        size--;

        return res;
    }
}
