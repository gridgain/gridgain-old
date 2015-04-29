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

package org.gridgain.grid.marshaller.optimized;

import org.gridgain.testframework.junits.common.*;

import java.io.*;
import java.net.InetAddress;

/**
 * Test that Optimized Marshaller works with classes with serialPersistentFields.
 */
public class OptimizedMarshallerSerialPersistentFieldsSelfTest extends GridCommonAbstractTest {
    /**
     * @throws Exception If failed.
     */
    public void testOptimizedMarshaller() throws Exception {
        GridOptimizedMarshaller m = new GridOptimizedMarshaller();

        m.unmarshal(m.marshal(new TestClass()), TestClass.class.getClassLoader());

        TestClass2 val = m.unmarshal(m.marshal(new TestClass2()), TestClass2.class.getClassLoader());

        assertNull(val.field3);

        m.unmarshal(m.marshal(new TestClass3()), TestClass3.class.getClassLoader());

        m.unmarshal(m.marshal(InetAddress.getByName("127.0.0.1")), null);
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

    /**
     * Test class with serialPersistentFields fields.
     */
    private static class TestClass2 implements Serializable {
        private static final long serialVersionUID = 0L;

        private Integer field3 = 1;

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

    /**
     * Test class with serialPersistentFields fields.
     */
    private static class TestClass3 implements Serializable {
        private static final long serialVersionUID = 0L;

        final transient InetAddressHolder holder;

        public TestClass3() {
            holder = new InetAddressHolder();
        }

        private static final ObjectStreamField[] serialPersistentFields = {
                new ObjectStreamField("hostName", String.class),
                new ObjectStreamField("address", int.class),
                new ObjectStreamField("family", int.class),
        };

        /**
         * @param s Object output stream.
         */
        private void writeObject(ObjectOutputStream s) throws IOException {
            ObjectOutputStream.PutField pf = s.putFields();
            pf.put("hostName", "Name");
            pf.put("address", 1);
            pf.put("family", 2);
            s.writeFields();
        }

        /**
         * @param s Object input stream.
         */
        private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
            ObjectInputStream.GetField gf = s.readFields();
            String host = (String)gf.get("hostName", null);
            int address= gf.get("address", 0);
            int family= gf.get("family", 0);
            InetAddressHolder h = new InetAddressHolder(host, address, family);
        }

        static class InetAddressHolder {

            InetAddressHolder() {}

            InetAddressHolder(String hostName, int address, int family) {
                this.hostName = hostName;
                this.address = address;
                this.family = family;
            }

            void init(String hostName, int family) {
                this.hostName = hostName;
                if (family != -1) {
                    this.family = family;
                }
            }

            String hostName;

            String getHostName() {
                return hostName;
            }

            /**
             * Holds a 32-bit IPv4 address.
             */
            int address;

            int getAddress() {
                return address;
            }

            /**
             * Specifies the address family type, for instance, '1' for IPv4
             * addresses, and '2' for IPv6 addresses.
             */
            int family;

            int getFamily() {
                return family;
            }
        }
    }
}
