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

package org.gridgain.grid.kernal.ggfs.hadoop;

import org.apache.hadoop.fs.*;
import org.gridgain.grid.ggfs.*;
import org.gridgain.grid.util.typedef.internal.*;

import java.io.*;

/**
 * Secondary file system input stream wrapper which actually opens input stream only in case it is explicitly
 * requested.
 * <p>
 * The class is expected to be used only from synchronized context and therefore is not tread-safe.
 */
public class GridGgfsHadoopReader implements GridGgfsReader {
    /** Secondary file system. */
    private final FileSystem fs;

    /** Path to the file to open. */
    private final Path path;

    /** Buffer size. */
    private final int bufSize;

    /** Actual input stream. */
    private FSDataInputStream in;

    /** Cached error occurred during output stream open. */
    private IOException err;

    /** Flag indicating that the stream was already opened. */
    private boolean opened;

    /**
     * Constructor.
     *
     * @param fs Secondary file system.
     * @param path Path to the file to open.
     * @param bufSize Buffer size.
     */
    GridGgfsHadoopReader(FileSystem fs, Path path, int bufSize) {
        assert fs != null;
        assert path != null;

        this.fs = fs;
        this.path = path;
        this.bufSize = bufSize;
    }

    /** Get input stream. */
    private PositionedReadable in() throws IOException {
        if (opened) {
            if (err != null)
                throw err;
        }
        else {
            opened = true;

            try {
                in = fs.open(path, bufSize);

                if (in == null)
                    throw new IOException("Failed to open input stream (file system returned null): " + path);
            }
            catch (IOException e) {
                err = e;

                throw err;
            }
        }

        return in;
    }

    /**
     * Close wrapped input stream in case it was previously opened.
     */
    @Override public void close() {
        U.closeQuiet(in);
    }

    /** {@inheritDoc} */
    @Override public int read(long pos, byte[] buf, int off, int len) throws IOException {
        return in().read(pos, buf, off, len);
    }
}
