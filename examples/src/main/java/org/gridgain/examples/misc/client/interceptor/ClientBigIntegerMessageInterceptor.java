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

package org.gridgain.examples.misc.client.interceptor;

import org.gridgain.grid.*;
import org.gridgain.grid.product.*;
import org.jetbrains.annotations.*;

import java.math.*;
import java.util.*;

import static org.gridgain.grid.product.GridProductEdition.*;

/**
 * Example implementation of {@link GridClientMessageInterceptor}.
 * <p>
 * For demonstration purpose it converts received byte arrays to {@link BigInteger} and back.
 */
@GridOnlyAvailableIn(DATA_GRID)
public class ClientBigIntegerMessageInterceptor implements GridClientMessageInterceptor {
    /** {@inheritDoc} */
    @Override public Object onReceive(@Nullable Object obj) {
        if (obj instanceof byte[]) {
            System.out.println(">>> Byte array received over REST: " + Arrays.toString((byte[])obj));

            BigInteger val = new BigInteger((byte[])obj);

            System.out.println(">>> Unpacked a BigInteger from byte array received over REST: " + val);

            return val;
        }
        else
            return obj;
    }

    /** {@inheritDoc} */
    @Override public Object onSend(Object obj) {
        if (obj instanceof BigInteger) {
            System.out.println(">>> Creating byte array from BigInteger to send over REST: " + obj);

            byte[] bytes = ((BigInteger)obj).toByteArray();

            System.out.println(">>> Created byte array from BigInteger to send over REST: " + Arrays.toString(bytes));

            return bytes;
        }
        else
           return  obj;
    }
}
