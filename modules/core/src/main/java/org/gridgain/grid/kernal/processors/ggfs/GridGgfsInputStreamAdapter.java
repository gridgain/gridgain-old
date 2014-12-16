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

import org.gridgain.grid.ggfs.*;

import java.io.*;

/**
 * Implementation adapter providing necessary methods.
 */
public abstract class GridGgfsInputStreamAdapter extends GridGgfsInputStream {
    /** {@inheritDoc} */
    @Override public long length() {
        return fileInfo().length();
    }

    /**
     * Gets file info for opened file.
     *
     * @return File info.
     */
    public abstract GridGgfsFileInfo fileInfo();

    /**
     * Reads bytes from given position.
     *
     * @param pos Position to read from.
     * @param len Number of bytes to read.
     * @return Array of chunks with respect to chunk file representation.
     * @throws IOException If read failed.
     */
    public abstract byte[][] readChunks(long pos, int len) throws IOException;
}
