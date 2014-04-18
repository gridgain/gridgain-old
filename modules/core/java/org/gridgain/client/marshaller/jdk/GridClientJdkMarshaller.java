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
package org.gridgain.client.marshaller.jdk;

import org.gridgain.client.marshaller.*;

import java.io.*;

/**
 * Simple marshaller that utilize JDK serialization features.
 */
public class GridClientJdkMarshaller implements GridClientMarshaller {
    /** Unique marshaller ID. */
    public static final Byte PROTOCOL_ID = 3;

    /** {@inheritDoc} */
    @Override public byte[] marshal(Object obj) throws IOException {
        ByteArrayOutputStream tmp = new ByteArrayOutputStream();

        ObjectOutputStream out = new ObjectOutputStream(tmp);

        out.writeObject(obj);

        out.flush();

        return tmp.toByteArray();
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override public <T> T unmarshal(byte[] bytes) throws IOException {
        ByteArrayInputStream tmp = new ByteArrayInputStream(bytes);

        ObjectInputStream in = new ObjectInputStream(tmp);

        try {
            return (T)in.readObject();
        }
        catch (ClassNotFoundException e) {
            throw new IOException("Failed to unmarshal target object: " + e.getMessage(), e);
        }
    }

    /** {@inheritDoc} */
    @Override public byte getProtocolId() {
        return PROTOCOL_ID;
    }
}
