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

package org.gridgain.grid.util.io;

import java.io.*;

/**
 * Extended data output.
 */
public interface GridDataOutput extends DataOutput {
    /**
     * @param out Underlying stream.
     */
    public void outputStream(OutputStream out);

    /**
     * @return Copy of internal array shrunk to offset.
     */
    public byte[] array();

    /**
     * @return Internal array.
     */
    public byte[] internalArray();

    /**
     * @return Offset.
     */
    public int offset();

    /**
     * @param off Offset.
     */
    public void offset(int off);

    /**
     * Resets data output.
     */
    public void reset();

    /**
     * Writes array of {@code byte}s.
     *
     * @param arr Array.
     * @throws IOException In case of error.
     */
    public void writeByteArray(byte[] arr) throws IOException;

    /**
     * Writes array of {@code short}s.
     *
     * @param arr Array.
     * @throws IOException In case of error.
     */
    public void writeShortArray(short[] arr) throws IOException;

    /**
     * Writes array of {@code int}s.
     *
     * @param arr Array.
     * @throws IOException In case of error.
     */
    public void writeIntArray(int[] arr) throws IOException;

    /**
     * Writes array of {@code long}s.
     *
     * @param arr Array.
     * @throws IOException In case of error.
     */
    public void writeLongArray(long[] arr) throws IOException;

    /**
     * Writes array of {@code float}s.
     *
     * @param arr Array.
     * @throws IOException In case of error.
     */
    public void writeFloatArray(float[] arr) throws IOException;

    /**
     * Writes array of {@code double}s.
     *
     * @param arr Array.
     * @throws IOException In case of error.
     */
    public void writeDoubleArray(double[] arr) throws IOException;

    /**
     * Writes array of {@code boolean}s.
     *
     * @param arr Array.
     * @throws IOException In case of error.
     */
    public void writeBooleanArray(boolean[] arr) throws IOException;

    /**
     * Writes array of {@code char}s.
     *
     * @param arr Array.
     * @throws IOException In case of error.
     */
    public void writeCharArray(char[] arr) throws IOException;
}
