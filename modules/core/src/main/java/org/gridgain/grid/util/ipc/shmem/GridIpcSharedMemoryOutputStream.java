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

package org.gridgain.grid.util.ipc.shmem;

import org.gridgain.grid.*;
import org.gridgain.grid.util.typedef.internal.*;

import java.io.*;

/**
 *
 */
public class GridIpcSharedMemoryOutputStream extends OutputStream {
    /** */
    private final GridIpcSharedMemorySpace out;

    /**
     * @param out Space.
     */
    public GridIpcSharedMemoryOutputStream(GridIpcSharedMemorySpace out) {
        assert out != null;

        this.out = out;
    }

    /** {@inheritDoc} */
    @Override public void write(int b) throws IOException {
        byte[] buf = new byte[1];

        buf[0] = (byte)b;

        write(buf, 0, 1);
    }

    /** {@inheritDoc} */
    @Override public void write(byte[] b, int off, int len) throws IOException {
        try {
            out.write(b, off, len, 0);
        }
        catch (GridException e) {
            throw new IOException(e);
        }
    }

    /** {@inheritDoc} */
    @Override public void close() throws IOException {
        out.close();
    }

    /**
     * Forcibly closes spaces and frees all system resources.
     * <p>
     * This method should be called with caution as it may result to the other-party
     * process crash. It is intended to call when there was an IO error during handshake
     * and other party has not yet attached to the space.
     */
    public void forceClose() {
        out.forceClose();
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridIpcSharedMemoryOutputStream.class, this);
    }
}
