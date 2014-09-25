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

package org.gridgain.grid.ggfs;

import java.io.*;

/**
 * The simplest data input interface to read from secondary file system in dual modes.
 */
public interface GridGgfsReader extends Closeable {
    /**
     * Read up to the specified number of bytes, from a given position within a file, and return the number of bytes
     * read.
     *
     * @param pos Position in the input stream to seek.
     * @param buf Buffer into which data is read.
     * @param off Offset in the buffer from which stream data should be written.
     * @param len The number of bytes to read.
     * @return Total number of bytes read into the buffer, or -1 if there is no more data (EOF).
     * @throws IOException In case of any exception.
     */
    public int read(long pos, byte[] buf, int off, int len) throws IOException;
}
