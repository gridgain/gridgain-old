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

import org.gridgain.testframework.junits.common.*;

import static org.gridgain.grid.GridSystemProperties.*;

/**
 * Test for {@link GridUnsafeDataOutput}.
 */
public class GridUnsafeDataOutputArraySizingSelfTest extends GridCommonAbstractTest {
    /** Small array. */
    private static final byte[] SMALL = new byte[32];

    /** Big array. */
    private static final byte[] BIG = new byte[2048];

    static {
        System.setProperty(GG_MARSHAL_BUFFERS_RECHECK, "1000");
    }

    /**
     * @throws Exception If failed.
     */
    @SuppressWarnings("BusyWait")
    public void testSmall() throws Exception {
        GridUnsafeDataOutput out = new GridUnsafeDataOutput(512);

        for (int i = 0; i < 11; i++) {
            Thread.sleep(100);

            writeSmall(out);
        }

        assertEquals(256, out.internalArray().length);

        for (int i = 0; i < 11; i++) {
            Thread.sleep(100);

            writeSmall(out);
        }

        assertEquals(128, out.internalArray().length);

        for (int i = 0; i < 11; i++) {
            Thread.sleep(100);

            writeSmall(out);
        }

        assertEquals(64, out.internalArray().length);

        for (int i = 0; i < 11; i++) {
            Thread.sleep(100);

            writeSmall(out);
        }

        assertEquals(64, out.internalArray().length);
    }

    /**
     * @throws Exception If failed.
     */
    public void testBig() throws Exception {
        GridUnsafeDataOutput out = new GridUnsafeDataOutput(512);

        writeBig(out);

        assertEquals(4096, out.internalArray().length);
    }

    /**
     * @throws Exception If failed.
     */
    @SuppressWarnings("BusyWait")
    public void testChanged1() throws Exception {
        GridUnsafeDataOutput out = new GridUnsafeDataOutput(512);

        for (int i = 0; i < 100; i++) {
            Thread.sleep(100);

            writeSmall(out);
            writeBig(out);
        }

        assertEquals(4096, out.internalArray().length);
    }

    /**
     * @throws Exception If failed.
     */
    @SuppressWarnings("BusyWait")
    public void testChanged2() throws Exception {
        GridUnsafeDataOutput out = new GridUnsafeDataOutput(512);

        for (int i = 0; i < 11; i++) {
            Thread.sleep(100);

            writeSmall(out);
        }

        assertEquals(256, out.internalArray().length);

        writeBig(out);

        assertEquals(4096, out.internalArray().length);

        for (int i = 0; i < 11; i++) {
            Thread.sleep(100);

            writeSmall(out);
        }

        assertEquals(4096, out.internalArray().length);

        for (int i = 0; i < 11; i++) {
            Thread.sleep(100);

            writeSmall(out);
        }

        assertEquals(2048, out.internalArray().length);
    }

    /**
     * @param out Output.
     * @throws Exception If failed.
     */
    private void writeSmall(GridUnsafeDataOutput out) throws Exception {
        out.write(SMALL);

        out.reset();
    }

    /**
     * @param out Output.
     * @throws Exception If failed.
     */
    private void writeBig(GridUnsafeDataOutput out) throws Exception {
        out.write(BIG);

        out.reset();
    }
}
