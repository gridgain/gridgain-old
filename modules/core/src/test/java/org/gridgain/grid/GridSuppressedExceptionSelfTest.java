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

package org.gridgain.grid;

import junit.framework.*;
import org.gridgain.grid.util.typedef.*;

import java.io.*;

/**
 *
 */
public class GridSuppressedExceptionSelfTest extends TestCase {
    /**
     * @throws Exception If failed.
     */
    public void testHasCause() throws Exception {
        GridException me = prepareMultiException();

        assertFalse(me.hasCause(IOException.class));
        assertTrue(me.hasCause(GridException.class));
        assertTrue(me.hasCause(IllegalArgumentException.class));
    }

    /**
     * @throws Exception If failed.
     */
    public void testGetCause() throws Exception {
        GridException me = prepareMultiException();

        assertNull(me.getCause(IOException.class));

        assertNotNull(me.getCause(GridException.class));
        assertTrue(me.getCause(GridException.class) instanceof GridException);

        assertNotNull(me.getCause(IllegalArgumentException.class));
        assertTrue(me.getCause(IllegalArgumentException.class) instanceof IllegalArgumentException);
    }

    /**
     * @throws Exception If failed.
     */
    public void testXHasCause() throws Exception {
        GridException me = prepareMultiException();

        try {
            throw new RuntimeException("Test.", me);
        }
        catch (RuntimeException e) {
            assertFalse(X.hasCause(e, IOException.class));
            assertTrue(X.hasCause(e, GridException.class));
            assertTrue(X.hasCause(e, IllegalArgumentException.class));
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testXCause() throws Exception {
        GridException me = prepareMultiException();

        try {
            throw new RuntimeException("Test.", me);
        }
        catch (RuntimeException e) {
            assertNull(X.cause(e, IOException.class));

            assertNotNull(X.cause(e, GridException.class));
            assertTrue(X.cause(e, GridException.class) instanceof GridException);

            assertNotNull(X.cause(e, IllegalArgumentException.class));
            assertTrue(X.cause(e, IllegalArgumentException.class) instanceof IllegalArgumentException);
        }
    }

    /**
     * Made to demonstrate stack printing for {@link GridException}. Do not enable.
     *
     * @throws Exception If failed.
     */
    public void _testStackTrace() throws Exception {
        GridException me = new GridException("Test message.");

        for (int i = 5; i < 20; i++) {
            try {
                generateException(i, null);
            }
            catch (GridException e) {
                me.addSuppressed(e);
            }
        }

        me.printStackTrace();
    }

    /**
     * @return A multi exception with few nested causes and
     *  {@link IllegalAccessException} in hierarchy.
     */
    private GridException prepareMultiException() {
        GridException me = new GridException("Test message.");

        for (int i = 0; i < 3; i++) {
            try {
                generateException(3, new IllegalArgumentException());
            }
            catch (GridException e) {
                me.addSuppressed(e);
            }
        }
        return me;
    }

    /**
     * @param calls Stack depth to throw exception.
     * @param cause Cause for the generated exception.
     * @throws GridException Exception.
     */
    private void generateException(int calls, Throwable cause) throws GridException {
        if (calls == 1)
            throw new GridException("Demo exception.", cause);
        else
            generateException(calls - 1, cause);
    }
}
