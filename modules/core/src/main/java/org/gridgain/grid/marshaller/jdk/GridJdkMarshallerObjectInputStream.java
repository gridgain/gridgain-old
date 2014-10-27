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

package org.gridgain.grid.marshaller.jdk;

import java.io.*;

/**
 * This class defines custom JDK object input stream.
 */
class GridJdkMarshallerObjectInputStream extends ObjectInputStream {
    /** */
    private final ClassLoader clsLdr;

    /**
     * @param in Parent input stream.
     * @param clsLdr Custom class loader.
     * @throws IOException If initialization failed.
     */
    GridJdkMarshallerObjectInputStream(InputStream in, ClassLoader clsLdr) throws IOException {
        super(in);

        assert clsLdr != null;

        this.clsLdr = clsLdr;

        enableResolveObject(true);
    }

    /** {@inheritDoc} */
    @Override protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
        // NOTE: DO NOT CHANGE TO 'clsLoader.loadClass()'
        // Must have 'Class.forName()' instead of clsLoader.loadClass()
        // due to weird ClassNotFoundExceptions for arrays of classes
        // in certain cases.
        return Class.forName(desc.getName(), true, clsLdr);
    }

    /** {@inheritDoc} */
    @Override protected Object resolveObject(Object o) throws IOException {
        if (o != null && o.getClass().equals(GridJdkMarshallerDummySerializable.class))
            return new Object();

        return super.resolveObject(o);
    }
}

