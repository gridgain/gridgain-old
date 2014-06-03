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

import junit.framework.*;

/**
 *
 */
public class GridOptimizedMarshallerEnumSelfTest extends TestCase {
    /**
     * @throws Exception If failed.
     */
    public void testEnumSerialisation() throws Exception {
        GridOptimizedMarshaller marsh = new GridOptimizedMarshaller();

        byte[] bytes = marsh.marshal(TestEnum.Bond);

        TestEnum unmarshalled = marsh.unmarshal(bytes, Thread.currentThread().getContextClassLoader());

        assertEquals(TestEnum.Bond, unmarshalled);
        assertEquals(TestEnum.Bond.desc, unmarshalled.desc);
    }

    private enum TestEnum {
        Equity("Equity") {
            @Override public String getTestString() {
                return "eee";
            }
        },

        Bond("Bond") {
            @Override public String getTestString() {
                return "qqq";
            }
        };

        public final String desc;

        TestEnum(String desc) {
            this.desc = desc;
        }

        public abstract String getTestString();
    }

}
