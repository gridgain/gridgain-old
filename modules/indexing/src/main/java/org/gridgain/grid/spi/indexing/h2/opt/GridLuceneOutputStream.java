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

package org.gridgain.grid.spi.indexing.h2.opt;

import org.apache.lucene.store.*;
import org.gridgain.grid.util.offheap.unsafe.*;

import java.io.IOException;

/**
 * A memory-resident {@link IndexOutput} implementation.
 */
public class GridLuceneOutputStream extends IndexOutput {
    /** Off-heap page size. */
    static final int BUFFER_SIZE = 32 * 1024;

    /** */
    private GridLuceneFile file;

    /** */
    private long currBuf;

    /** */
    private int currBufIdx;

    /** */
    private int bufPosition;

    /** */
    private long bufStart;

    /** */
    private int bufLength;

    /** */
    private final GridUnsafeMemory mem;

    /**
     * Constructor.
     *
     * @param f File.
     */
    public GridLuceneOutputStream(GridLuceneFile f) {
        file = f;

        mem = f.getDirectory().memory();

        // make sure that we switch to the
        // first needed buffer lazily
        currBufIdx = -1;
        currBuf = 0;
    }

    /**
     * Resets this to an empty file.
     */
    public void reset() {
        currBuf = 0;
        currBufIdx = -1;
        bufPosition = 0;
        bufStart = 0;
        bufLength = 0;

        file.setLength(0);
    }

    /** {@inheritDoc} */
    @Override public void close() throws IOException {
        flush();
    }

    /** {@inheritDoc} */
    @Override public void seek(long pos) throws IOException {
        // set the file length in case we seek back
        // and flush() has not been called yet
        setFileLength();

        if (pos < bufStart || pos >= bufStart + bufLength) {
            currBufIdx = (int)(pos / BUFFER_SIZE);

            switchCurrentBuffer();
        }

        bufPosition = (int)(pos % BUFFER_SIZE);
    }

    /** {@inheritDoc} */
    @Override public long length() {
        return file.getLength();
    }

    /** {@inheritDoc} */
    @Override public void writeByte(byte b) throws IOException {
        if (bufPosition == bufLength) {
            currBufIdx++;

            switchCurrentBuffer();
        }

        mem.writeByte(currBuf + bufPosition++, b);
    }

    /** {@inheritDoc} */
    @Override public void writeBytes(byte[] b, int offset, int len) throws IOException {
        assert b != null;

        while (len > 0) {
            if (bufPosition == bufLength) {
                currBufIdx++;

                switchCurrentBuffer();
            }

            int remainInBuf = BUFFER_SIZE - bufPosition;
            int bytesToCp = len < remainInBuf ? len : remainInBuf;

            mem.writeBytes(currBuf + bufPosition, b, offset, bytesToCp);

            offset += bytesToCp;
            len -= bytesToCp;

            bufPosition += bytesToCp;
        }
    }

    /**
     * Switch buffer to next.
     */
    private void switchCurrentBuffer() {
        currBuf = currBufIdx == file.numBuffers() ? file.addBuffer() : file.getBuffer(currBufIdx);

        bufPosition = 0;
        bufStart = (long)BUFFER_SIZE * (long)currBufIdx;
        bufLength = BUFFER_SIZE;
    }

    /**
     * Sets file length.
     */
    private void setFileLength() {
        long pointer = bufStart + bufPosition;

        if (pointer > file.getLength())
            file.setLength(pointer);
    }

    /** {@inheritDoc} */
    @Override public void flush() throws IOException {
        setFileLength();
    }

    /** {@inheritDoc} */
    @Override public long getFilePointer() {
        return currBufIdx < 0 ? 0 : bufStart + bufPosition;
    }

    /**
     * Returns byte usage of all buffers.
     *
     * @return Bytes used.
     */
    public long sizeInBytes() {
        return (long)file.numBuffers() * (long)BUFFER_SIZE;
    }

    /** {@inheritDoc} */
    @Override public void copyBytes(DataInput input, long numBytes) throws IOException {
        assert numBytes >= 0 : "numBytes=" + numBytes;

        GridLuceneInputStream gridInput = input instanceof GridLuceneInputStream ? (GridLuceneInputStream)input : null;

        while (numBytes > 0) {
            if (bufPosition == bufLength) {
                currBufIdx++;

                switchCurrentBuffer();
            }

            int toCp = BUFFER_SIZE - bufPosition;

            if (numBytes < toCp)
                toCp = (int)numBytes;

            if (gridInput != null)
                gridInput.readBytes(currBuf + bufPosition, toCp);
            else {
                byte[] buff = new byte[toCp];

                input.readBytes(buff, 0, toCp, false);

                mem.writeBytes(currBuf + bufPosition, buff);
            }

            numBytes -= toCp;
            bufPosition += toCp;
        }
    }

    /**
     * For direct usage by {@link GridLuceneInputStream}.
     *
     * @param ptr Pointer.
     * @param len Length.
     * @throws IOException If failed.
     */
    void writeBytes(long ptr, int len) throws IOException {
        while (len > 0) {
            if (bufPosition == bufLength) {
                currBufIdx++;
                switchCurrentBuffer();
            }

            int remainInBuf = BUFFER_SIZE - bufPosition;
            int bytesToCp = len < remainInBuf ? len : remainInBuf;

            mem.copyMemory(ptr, currBuf + bufPosition, bytesToCp);

            ptr += bytesToCp;
            len -= bytesToCp;
            bufPosition += bytesToCp;
        }
    }
}
