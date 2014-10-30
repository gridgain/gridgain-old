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

import org.apache.commons.io.*;
import org.gridgain.grid.marshaller.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.util.*;

import static junit.framework.Assert.*;

/**
 * IO test utilities.
 */
public final class GridTestIoUtils {

    /**
     * Serializes a given object into byte array.
     *
     * @param obj Object to serialize.
     * @return Byte array containing serialized object.
     * @throws IOException If serialization failed.
     */
    public static byte[] serializeJdk(Object obj) throws IOException {
        ByteArrayOutputStream byteOut = null;

        ObjectOutputStream objOut = null;

        try {
            // Allocate half of kilobyte to avoid very small resizings.
            byteOut = new ByteArrayOutputStream(512);

            objOut = new ObjectOutputStream(byteOut);

            // Make sure that we serialize only task, without
            // class loader.
            objOut.writeObject(obj);

            objOut.flush();

            return byteOut.toByteArray();
        }
        finally {
            close(objOut);
            close(byteOut);
        }
    }

    /**
     * Deserializes passed in bytes using provided class loader.
     *
     * @param <T> Type of result object.
     * @param bytes Object bytes to deserialize.
     * @return Deserialized object.
     * @throws IOException If deserialization failed.
     * @throws ClassNotFoundException If deserialization failed.
     */
    @SuppressWarnings({"unchecked"})
    public static <T extends Serializable> T deserializeJdk(byte[] bytes) throws IOException, ClassNotFoundException {
        ObjectInputStream in = null;

        try {
            in = new ObjectInputStream(new ByteArrayInputStream(bytes));

            return (T)in.readObject();
        }
        finally {
            close(in);
        }
    }

    /**
     * Deserializes passed in bytes using provided class loader.
     *
     * @param <T> Type of result object.
     * @param bytes Object bytes to deserialize.
     * @param clsLdr Class loader.
     * @return Deserialized object.
     * @throws IOException If deserialization failed.
     * @throws ClassNotFoundException If deserialization failed.
     */
    @SuppressWarnings({"unchecked"})
    public static <T> T deserializeJdk(byte[] bytes, final ClassLoader clsLdr)
        throws IOException, ClassNotFoundException {
        ObjectInputStream in = null;

        try {
            in = new ObjectInputStream(new ByteArrayInputStream(bytes)) {
                @Override protected Class<?> resolveClass(ObjectStreamClass desc) throws ClassNotFoundException {
                    return clsLdr.loadClass(desc.getName());
                }
            };

            return (T)in.readObject();
        }
        finally {
            close(in);
        }
    }

    /**
     * @param <T> Type of result object.
     * @param obj Object to marshal/unmarshal.
     * @param marshaller Marshaller to use for serialization.
     * @return The same object, but passed through marshaller: obj->marshal->buf->unmarshal->copy.
     * @throws Exception If failed.
     */
    @SuppressWarnings("unchecked")
    public static <T> T externalize(Externalizable obj, GridMarshaller marshaller) throws Exception {
        assert marshaller != null;

        byte[] buf = marshaller.marshal(obj);

        // Sleep to make sure that clock advances (necessary for some tests)
        Thread.sleep(10);

        return (T)marshaller.unmarshal(buf, obj.getClass().getClassLoader());
    }

    /**
     * Validate streams generate the same output.
     *
     * @param expIn Expected input stream.
     * @param actIn Actual input stream.
     * @param expSize Expected size of the streams.
     * @throws IOException In case of any IO exception.
     */
    public static void assertEqualStreams(InputStream expIn, InputStream actIn,
        @Nullable Long expSize) throws IOException {
        int bufSize = 2345;
        byte buf1[] = new byte[bufSize];
        byte buf2[] = new byte[bufSize];
        long pos = 0;

        while (true) {
            int i1 = actIn.read(buf1, 0, bufSize);

            int i2;

            if (i1 == -1) // Expects EOF?
                i2 = expIn.read(buf2, 0, 1); // Try to read at least 1 byte guaranted by stream's API.
            else
                IOUtils.readFully(expIn, buf2, 0, i2 = i1); // Read the same bytes count as from actual stream.

            if (i1 != i2)
                fail("Expects the same data [pos=" + pos + ", i1=" + i1 + ", i2=" + i2 + ']');

            if (i1 == -1)
                break; // EOF

            // i1 == bufSize => compare buffers.
            // i1 <  bufSize => Compare part of buffers, rest of buffers are equal from previous iteration.
            assertTrue("Expects the same data [pos=" + pos +  ", i1=" + i1 + ", i2=" + i2 + ']',
                Arrays.equals(buf1, buf2));

            pos += i1;
        }

        if (expSize != null)
            assertEquals(expSize.longValue(), pos);
    }


    /**
     * Closes resource.
     *
     * @param c Resource
     * @throws IOException If failed to close resource.
     */
    private static void close(Closeable c) throws IOException {
        if (c != null)
            c.close();
    }

    /**
     * Ensure singleton.
     */
    private GridTestIoUtils() {
        // No-op.
    }
}
