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

package org.gridgain.client.marshaller;

import java.io.*;
import java.nio.*;

/**
 * Marshaller for binary protocol messages.
 */
public interface GridClientMarshaller {
    /**
     * Marshals object to byte array.
     *
     * @param obj Object to marshal.
     * @param off Start offset.
     * @return Byte buffer.
     * @throws IOException If marshalling failed.
     */
    public ByteBuffer marshal(Object obj, int off) throws IOException;

    /**
     * Unmarshals object from byte array.
     *
     * @param bytes Byte array.
     * @return Unmarshalled object.
     * @throws IOException If unmarshalling failed.
     */
    public <T> T unmarshal(byte[] bytes) throws IOException;
}
