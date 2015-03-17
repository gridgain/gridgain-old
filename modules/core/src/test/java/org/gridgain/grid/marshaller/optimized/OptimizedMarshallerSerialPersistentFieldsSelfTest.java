/* @java.file.header */

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.marshaller.optimized;

import org.gridgain.testframework.junits.common.*;

import java.io.*;

/**
 * Test that Optimized Marshaller works with classes with serialPersistentFields.
 */
public class OptimizedMarshallerSerialPersistentFieldsSelfTest extends GridCommonAbstractTest {
    /**
     * @throws Exception If failed.
     */
    public void testOptimizedMarshaller() throws Exception {
        GridOptimizedMarshaller m = new GridOptimizedMarshaller();

        byte[] valueBytes = m.marshal(new TestClass());

        m.unmarshal(valueBytes, TestClass.class.getClassLoader());
    }

    /**
     * Test class with serialPersistentFields fields.
     */
    private static class TestClass implements Serializable {
        private static final long serialVersionUID = 0L;

        /** For serialization compatibility. */
        private static final ObjectStreamField[] serialPersistentFields = {
            new ObjectStreamField("field1", Integer.TYPE),
            new ObjectStreamField("field2", Integer.TYPE)
        };

        /**
         * @param s Object output stream.
         */
        private void writeObject(ObjectOutputStream s) throws IOException {
            s.putFields().put("field1", 1);
            s.putFields().put("field2", 2);
            s.writeFields();

            s.writeObject(null);
        }

        /**
         * @param s Object input stream.
         */
        private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
            s.defaultReadObject();

            s.readObject();
        }
    }
}
